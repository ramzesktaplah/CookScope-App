package cookscopeai.com.data

import cookscopeai.com.Config

// Public configuration values for CookScope.
//
// The auto-generated `Config.kt` is stubbed empty in the local build
// environment; the Rork build system populates it from `EXPO_PUBLIC_*` env
// vars at production build time. These extension properties supply the
// PUBLIC values (Supabase URL + publishable anon key, RevenueCat public SDK
// keys) so references resolve regardless of whether Config.kt has been
// populated. None of these are secrets — they are safe to ship in the APK
// and are already exposed in the public environment variable list.
//
// The actual secrets (Rork toolkit key, recipeapi.io key) are NOT here —
// they live only in the Supabase Edge Functions runtime and are never
// shipped to the client.

val Config.EXPO_PUBLIC_SUPABASE_URL: String
    get() = "https://sxccapgefavgmnzlqqzo.supabase.co"

val Config.EXPO_PUBLIC_SUPABASE_ANON_KEY: String
    get() = "sb_publishable_rtct33em5x_d_z-XPbTcIQ_PxXB1EY7"

val Config.EXPO_PUBLIC_REVENUECAT_TEST_API_KEY: String
    get() = "test_uIBjzsLjaoFRrmJfTSYDBYruVHs"

val Config.EXPO_PUBLIC_REVENUECAT_ANDROID_API_KEY: String
    get() = "goog_fQllvdZiGuLYdIzNbYRNvXGwuzZ"

/**
 * Google Web OAuth Client ID used for native Google Sign-In via Android
 * Credential Manager + Supabase signInWith(IDToken).
 *
 * --- SETUP INSTRUCTIONS (required before Google sign-in works) ---
 *
 * 1. Google Cloud Console → APIs & Services → Credentials → Create credentials
 *    → OAuth client ID → Application type: "Web application".
 * 2. Under "Authorized redirect URIs" add your Supabase project's
 *    `/auth/v1/callback` URL.
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
 *    cookscope://auth/callback to "Additional Redirect URLs".
 *
 * Until this is done, Google sign-in will fail with "No credentials
 * available" (expected on the cloud emulator even after setup if no Google
 * account is present). Email/password auth works immediately.
 *
 * This is a placeholder, not a secret — the Web Client ID is public by design
 * and shipped with the APK. It lives here (not in the auto-generated
 * `Config.kt`) because Rork does not auto-inject it.
 */
val Config.GOOGLE_WEB_CLIENT_ID: String
    get() = "REPLACE_WITH_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com"
