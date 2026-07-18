package cookscopeai.com

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cookscopeai.com.data.AuthManager
import cookscopeai.com.data.ChatHistoryManager
import cookscopeai.com.data.ChatUsageManager
import cookscopeai.com.data.SubscriptionManager
import cookscopeai.com.data.SupabaseClient
import cookscopeai.com.data.ThemeManager
import cookscopeai.com.data.UserManager
import cookscopeai.com.ui.navigation.AppNavigation
import cookscopeai.com.ui.theme.AppTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle Supabase Auth deep link (email confirmation / OAuth callback)
        // before initializing UI. This must run on every launch, including
        // when the app is opened via a confirmation link.
        SupabaseClient.client.handleDeeplinks(intent)

        // Initialize managers
        ThemeManager.initialize(this)
        UserManager.initialize(this)
        AuthManager.initialize(this)
        SubscriptionManager.initialize(
            app = application,
            isDebug = BuildConfig.DEBUG,
        )
        ChatHistoryManager.initialize(this)
        ChatUsageManager.initialize(this)

        setContent {
            AppTheme {
                AppNavigation()
            }
        }
    }
}
