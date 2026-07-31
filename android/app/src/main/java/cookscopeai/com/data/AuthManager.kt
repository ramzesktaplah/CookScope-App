package cookscopeai.com.data

import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import java.security.MessageDigest
import java.util.UUID

/**
 * Result of an auth action — either success with an optional message,
 * or failure with a user-friendly error message.
 */
sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Tracks which auth screen the current session originated from, so root
 * navigation can apply the correct post-auth routing:
 *  - SIGNUP  → new user → walk through account type + preferences + paywall
 *  - LOGIN   → returning user → go straight to Home (skip onboarding)
 *  - UNKNOWN → restored session on app launch → decide from remote flags
 */
enum class AuthEntryPoint {
    UNKNOWN,
    SIGNUP,
    LOGIN,
}

/**
 * Manages Supabase Auth session state for CookScope.
 *
 * Exposes a [sessionStatus] StateFlow that drives the root navigation graph:
 * - SessionStatus.Initializing     → show a splash/loading state
 * - SessionStatus.NotAuthenticated → show onboarding/login flow
 * - SessionStatus.Authenticated    → show main app
 *
 * Auth methods:
 * - [signUp]            — email/password + username stored as user_metadata
 * - [signIn]            — email/password
 * - [signInWithGoogle]  — Android Credential Manager + Supabase IDToken
 * - [resendConfirmation] — resend email verification link
 * - [signOut]           — clears the session
 */
object AuthManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var credentialManager: CredentialManager

    private val _sessionStatus = MutableStateFlow<SessionStatus>(SessionStatus.Initializing)
    val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    /**
     * Records which auth screen produced the current session, so root
     * navigation can apply the correct post-auth routing:
     *  - SIGNUP  → new user, walk through the full onboarding flow
     *  - LOGIN   → returning user, skip straight to Home
     *  - UNKNOWN → restored session on app launch, decide from remote flags
     *
     * Reset to UNKNOWN on signOut.
     */
    private val _authEntryPoint = MutableStateFlow(AuthEntryPoint.UNKNOWN)
    val authEntryPoint: StateFlow<AuthEntryPoint> = _authEntryPoint.asStateFlow()

    /**
     * Initialize the AuthManager. Call once from MainActivity.onCreate
     * after SupabaseClient is initialized. Wires the Supabase session
     * status into our local StateFlow.
     */
    fun initialize(context: Context) {
        credentialManager = CredentialManager.create(context)
        // Mirror Supabase's session status into our local flow.
        _sessionStatus.value = SupabaseClient.client.auth.sessionStatus.value
        scope.launch {
            SupabaseClient.client.auth.sessionStatus.collect { status ->
                _sessionStatus.value = status
            }
        }
    }

    /** Currently authenticated user, or null. */
    val currentUser: UserInfo?
        get() = (_sessionStatus.value as? SessionStatus.Authenticated)?.session?.user

    /** Convenience: true when an authenticated session exists. */
    val isAuthenticated: Boolean
        get() = _sessionStatus.value is SessionStatus.Authenticated

    /** Observable convenience flow for UI that needs to react to auth state. */
    val isAuthenticatedFlow: StateFlow<Boolean> = _sessionStatus
        .map { it is SessionStatus.Authenticated }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), isAuthenticated)

    /**
     * Sign up with email/password. The username is stored as user_metadata
     * so it can be read on the profile screen after confirmation.
     *
     * Supabase sends a confirmation email by default (link flow).
     * Returns AuthResult.Success on successful signup (before confirmation).
     */
    suspend fun signUp(username: String, email: String, password: String): AuthResult {
        return try {
            SupabaseClient.client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                data = buildJsonObject {
                    put("username", username.trim())
                    put("display_name", username.trim())
                }
            }
            _authEntryPoint.value = AuthEntryPoint.SIGNUP
            AuthResult.Success
        } catch (e: RestException) {
            AuthResult.Error(mapRestError(e))
        } catch (e: Exception) {
            AuthResult.Error("Sign up failed. Please check your details and try again.")
        }
    }

    /**
     * Sign in with email/password.
     */
    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            SupabaseClient.client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            _authEntryPoint.value = AuthEntryPoint.LOGIN
            AuthResult.Success
        } catch (e: RestException) {
            AuthResult.Error(mapRestError(e))
        } catch (e: Exception) {
            AuthResult.Error("Sign in failed. Please check your email and password.")
        }
    }

    /**
     * Native Google Sign-In via Android Credential Manager.
     *
     * 1. Generate a nonce, hash it (SHA-256), base64-encode.
     * 2. Build a GetCredentialRequest with GetGoogleIdOption using the
     *    Web OAuth Client ID from Config + the hashed nonce.
     * 3. Call CredentialManager.getCredential → extract GoogleIdTokenCredential.
     * 4. Send the ID token + raw nonce to Supabase via signInWith(IDToken).
     *
     * Requires Google Web Client ID configured in build config.
     * On the cloud emulator, will throw "No credentials available" if no
     * Google account is present on the device — this is expected.
     */
    suspend fun signInWithGoogle(context: Context, isSignUp: Boolean): AuthResult {
        val serverClientId = cookscopeai.com.Config.GOOGLE_WEB_CLIENT_ID
        if (serverClientId.startsWith("REPLACE_WITH_") || serverClientId.isBlank()) {
            return AuthResult.Error(
                "Google Sign-In is not available right now. Please use email/password instead."
            )
        }

        // Generate and hash a nonce for ID token validation.
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = hashNonce(rawNonce)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )
            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(result.credential.data)
            val googleIdToken = googleIdTokenCredential.idToken

            SupabaseClient.client.auth.signInWith(IDToken) {
                idToken = googleIdToken
                provider = Google
                nonce = rawNonce
            }
            _authEntryPoint.value = if (isSignUp) AuthEntryPoint.SIGNUP else AuthEntryPoint.LOGIN
            AuthResult.Success
        } catch (e: GetCredentialException) {
            AuthResult.Error(
                if (e.message?.contains("No credentials available", ignoreCase = true) == true) {
                    "No Google account found on this device. Add a Google account " +
                        "in Settings → Accounts, or use email/password."
                } else {
                    "Google sign-in was cancelled or failed."
                }
            )
        } catch (e: GoogleIdTokenParsingException) {
            AuthResult.Error("Could not parse Google sign-in response.")
        } catch (e: RestException) {
            AuthResult.Error(mapRestError(e))
        } catch (e: Exception) {
            AuthResult.Error("Google sign-in failed. Please try again.")
        }
    }

    /**
     * Resend the email confirmation link for the given address.
     * Uses OtpType.Email.SIGNUP for signup confirmation resends.
     */
    suspend fun resendConfirmation(email: String): AuthResult {
        return try {
            SupabaseClient.client.auth.resendEmail(
                type = OtpType.Email.SIGNUP,
                email = email.trim(),
            )
            AuthResult.Success
        } catch (e: RestException) {
            AuthResult.Error(mapRestError(e))
        } catch (e: Exception) {
            AuthResult.Error("Could not resend email. Please try again.")
        }
    }

    /**
     * Sign out and clear the current session.
     * Also clears locally-stored chat history and usage counters so the
     * next user starts with a clean slate.
     */
    suspend fun signOut() {
        _authEntryPoint.value = AuthEntryPoint.UNKNOWN
        try {
            SupabaseClient.client.auth.signOut()
        } catch (_: Exception) {
            // Even if signOut fails (e.g., network), the local session is cleared
            // by supabase-kt, so we ignore.
        }
        ChatHistoryManager.clearAll()
    }

    // ---- Helpers ----

    private fun hashNonce(nonce: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(nonce.toByteArray())
        // URL-safe, no-wrap base64 per Google's requirement.
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    private fun mapRestError(e: RestException): String {
        val msg = e.message.orEmpty().lowercase()
        return when {
            msg.contains("weak_password") ||
                msg.contains("password should be") ||
                msg.contains("password should contain") ->
                "Password is too weak. Use at least 12 characters with a mix of letters and numbers."

            msg.contains("invalid login credentials") ||
                msg.contains("invalid credentials") ->
                "Incorrect email or password."

            msg.contains("user already registered") ||
                msg.contains("already been registered") ->
                "An account with this email already exists. Try signing in."

            msg.contains("email not confirmed") ->
                "Please confirm your email before signing in. Check your inbox."

            msg.contains("rate limit") ->
                "Too many attempts. Please wait a moment and try again."

            msg.contains("network") ||
                msg.contains("timeout") ||
                msg.contains("unable to resolve") ->
                "Network error. Check your connection and try again."

            msg.contains("invalid email") ||
                msg.contains("email format") ->
                "Please enter a valid email address."

            msg.contains("user not found") ->
                "No account found with that email. Try signing up."

            // Never return the raw Supabase error text — it may contain URLs,
            // headers, or the anon key. Fall back to a generic safe message.
            else -> "Something went wrong. Please try again."
        }
    }
}
