package cookscopeai.com

import android.app.Application
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.coroutines.resume

/**
 * Wraps the Standard Play Integrity API for app authenticity verification.
 *
 * Call [prepare] once early in the app lifecycle (e.g. [MainActivity.onCreate])
 * on a background coroutine. The prepared [StandardIntegrityTokenProvider] is
 * held in memory and reused for all subsequent [requestToken] calls.
 *
 * If the token provider isn't ready yet (cold start) or the Play Integrity
 * call fails (e.g. emulator without Play Services), [requestToken] returns
 * null. Callers should proceed without the token — the backend will fail-open
 * if integrity isn't configured, fail-closed if it is.
 */
object IntegrityChecker {

    @Volatile
    private var tokenProvider: StandardIntegrityTokenProvider? = null

    @Volatile
    private var isPreparing = false

    /**
     * Prepare the integrity token provider. Should be called once on a
     * background coroutine early in the app lifecycle.
     *
     * @param cloudProjectNumber the Google Cloud project number linked to
     *   this app in Play Console > Protected with Play. Pass an empty string
     *   to skip preparation (integrity checks will be no-ops).
     */
    suspend fun prepare(cloudProjectNumber: String) {
        if (cloudProjectNumber.isBlank()) return
        if (tokenProvider != null || isPreparing) return
        isPreparing = true
        try {
            val number = cloudProjectNumber.trim().toLong()
            val provider = withContext(Dispatchers.IO) {
                prepareTokenProvider(number)
            }
            tokenProvider = provider
        } catch (_: Exception) {
            // Graceful degradation — the app still works, integrity checks
            // will just return null tokens until the next launch.
        } finally {
            isPreparing = false
        }
    }

    /**
     * Request an integrity token for the given request hash.
     *
     * @param requestHash a SHA-256 hash of the request payload being
     *   protected. The backend will verify that the hash in the decoded
     *   token matches the hash it computes from the received request.
     * @return the signed integrity token string, or null if the provider
     *   isn't ready or the request failed.
     */
    suspend fun requestToken(requestHash: String): String? {
        val provider = tokenProvider ?: return null
        return try {
            withContext(Dispatchers.IO) {
                requestTokenFromProvider(provider, requestHash)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Compute a SHA-256 hex hash of the given input string. Used to generate
     * the request hash for integrity tokens.
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ---- Internals ----

    private suspend fun prepareTokenProvider(
        cloudProjectNumber: Long,
    ): StandardIntegrityTokenProvider {
        val manager = IntegrityManagerFactory.createStandard(appContext)
        val request = StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
            .setCloudProjectNumber(cloudProjectNumber)
            .build()
        return suspendCancellableCoroutine { cont ->
            manager.prepareIntegrityToken(request)
                .addOnSuccessListener { provider -> cont.resume(provider) }
                .addOnFailureListener { cont.resume(null) }
        } ?: throw IllegalStateException("Token provider preparation failed")
    }

    private suspend fun requestTokenFromProvider(
        provider: StandardIntegrityTokenProvider,
        requestHash: String,
    ): String? {
        val request = StandardIntegrityTokenRequest.builder()
            .setRequestHash(requestHash)
            .build()
        return suspendCancellableCoroutine { cont ->
            provider.request(request)
                .addOnSuccessListener { token -> cont.resume(token.token()) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    // ---- Application context holder ----

    private lateinit var appContext: Application

    /**
     * Set the application context. Must be called before [prepare].
     */
    fun init(application: Application) {
        appContext = application
    }
}
