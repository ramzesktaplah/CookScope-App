package cookscopeai.com.data

import cookscopeai.com.Config

// Temporary extensions to satisfy the current SupabaseClient references while the
// auto-generated Config.kt is empty in this environment. The values are public
// Supabase credentials already present in android/.env.

val Config.EXPO_PUBLIC_SUPABASE_URL: String
    get() = "https://sxccapgefavgmnzlqqzo.supabase.co"

val Config.EXPO_PUBLIC_SUPABASE_ANON_KEY: String
    get() = "sb_publishable_rtct33em5x_d_z-XPbTcIQ_PxXB1EY7"
