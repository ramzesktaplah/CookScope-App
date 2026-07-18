package cookscopeai.com.data

import cookscopeai.com.Config

// Temporary extensions to satisfy the current SupabaseClient references while the
// auto-generated Config.kt is empty in this environment. The values are public
// Supabase credentials already present in android/.env.

val Config.EXPO_PUBLIC_SUPABASE_URL: String
    get() = "https://sxccapgefavgmnzlqqzo.supabase.co"

val Config.EXPO_PUBLIC_SUPABASE_ANON_KEY: String
    get() = "sb_publishable_rtct33em5x_d_z-XPbTcIQ_PxXB1EY7"

/**
 * Google Web OAuth Client ID used for native Google Sign-In via Android
 * Credential Manager + Supabase signInWith(IDToken).
 *
 * --- SETUP INSTRUCTIONS (required before Google sign-in works) ---
 *
 * 1. Google Cloud Console → APIs & Services → Credentials → Create credentials
 *    → OAuth client ID → Application type: "Web application".
 * 2. Under "Authorized redirect URIs" add:
 *    https://sxccapgefavgmnzlqqzo.supabase.co/auth/v1/callback
 * 3. Copy the Web application Client ID and paste it below, replacing the
 *    placeholder.
 * 4. In the same Console, create a second OAuth client ID of type "Android":
 *    - Package name: cookscopeai.com
 *    - SHA-1 certificate fingerprint: run `./gradlew signingReport` and copy
 *      the SHA-1 of the signing key (debug + the rorkPlayUpload key for
 *      release). Add one Android client per SHA-1.
 * 5. Supabase Dashboard → Authentication → Providers → Google → Enable.
 *    - Paste the Web Client ID into "Client ID (Web)".
 *    - Paste the Web Client Secret into "Client Secret".
 *    - Add both the Web and Android Client IDs to "Authorized Client IDs".
 * 6. Supabase Dashboard → Authentication → URL Configuration → add
 *    cookscope://login-callback to "Additional Redirect URLs".
 *
 * Until this is done, Google sign-in will fail with "No credentials
 * available" (expected on the cloud emulator even after setup if no Google
 * account is present). Email/password auth works immediately.
 */
val Config.GOOGLE_WEB_CLIENT_ID: String
    get() = "REPLACE_WITH_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com"
