package cookscopeai.com

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cookscopeai.com.data.AuthManager
import cookscopeai.com.data.ChatHistoryManager
import cookscopeai.com.data.ChatUsageManager
import cookscopeai.com.data.ScanUsageManager
import cookscopeai.com.data.SubscriptionManager
import cookscopeai.com.data.SupabaseClient
import cookscopeai.com.data.ThemeManager
import cookscopeai.com.data.UserManager
import cookscopeai.com.ui.navigation.AppNavigation
import cookscopeai.com.ui.screens.SplashScreen
import cookscopeai.com.ui.theme.AppTheme
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle Supabase Auth deep link (email confirmation / OAuth callback)
        // before initializing UI. This must run on every launch, including
        // when the app is opened via a confirmation link.
        SupabaseClient.client.handleDeeplinks(intent)

        // Initialize managers in dependency order.
        ThemeManager.initialize(this)
        UserManager.initialize(this)
        AuthManager.initialize(this)
        SubscriptionManager.initialize(
            app = application,
            isDebug = BuildConfig.DEBUG,
        )
        ChatHistoryManager.initialize(this)
        ChatUsageManager.initialize(this)
        ScanUsageManager.initialize(this)

        setContent {
            AppTheme {
                val sessionStatus by AuthManager.sessionStatus.collectAsState()
                val isProfileLoading by UserManager.isProfileLoading.collectAsState()
                val isReady = remember(sessionStatus, isProfileLoading) {
                    sessionStatus != SessionStatus.Initializing && !isProfileLoading
                }
                var showSplash by remember { mutableStateOf(true) }
                var minimumMet by remember { mutableStateOf(false) }
                var maximumMet by remember { mutableStateOf(false) }

                // Minimum splash duration: 5 seconds
                LaunchedEffect(Unit) {
                    delay(5000)
                    minimumMet = true
                }

                // Maximum splash duration: 8 seconds
                LaunchedEffect(Unit) {
                    delay(8000)
                    maximumMet = true
                }

                LaunchedEffect(isReady, minimumMet, maximumMet) {
                    showSplash = !(isReady && minimumMet) && !maximumMet
                }

                AnimatedVisibility(
                    visible = showSplash,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    SplashScreen()
                }

                AnimatedVisibility(
                    visible = !showSplash,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
