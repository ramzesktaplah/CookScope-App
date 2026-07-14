package com.rork.cookscope

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rork.cookscope.data.SubscriptionManager
import com.rork.cookscope.data.ThemeManager
import com.rork.cookscope.data.UserManager
import com.rork.cookscope.ui.navigation.AppNavigation
import com.rork.cookscope.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize managers
        ThemeManager.initialize(this)
        UserManager.initialize(this)
        SubscriptionManager.initialize(
            app = application,
            isDebug = BuildConfig.DEBUG,
        )

        setContent {
            AppTheme {
                AppNavigation()
            }
        }
    }
}
