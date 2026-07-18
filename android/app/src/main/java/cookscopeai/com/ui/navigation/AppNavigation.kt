package cookscopeai.com.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cookscopeai.com.data.AuthManager
import cookscopeai.com.data.UserManager
import cookscopeai.com.ui.screens.AccountTypeSelectionScreen
import cookscopeai.com.ui.screens.ChatbotScreen
import cookscopeai.com.ui.screens.CookingGuideScreen
import cookscopeai.com.ui.screens.CreateRecipeScreen
import cookscopeai.com.ui.screens.DishScanScreen
import cookscopeai.com.ui.screens.EditProfileScreen
import cookscopeai.com.ui.screens.ExploreScreen
import cookscopeai.com.ui.screens.HomeScreen
import cookscopeai.com.ui.screens.LoginScreen
import cookscopeai.com.ui.screens.OnboardingPreferencesScreen
import cookscopeai.com.ui.screens.PaywallScreen
import cookscopeai.com.ui.screens.PreferencesScreen
import cookscopeai.com.ui.screens.ProfileScreen
import cookscopeai.com.ui.screens.RecipeDetailScreen
import cookscopeai.com.ui.screens.SavedRecipesScreen
import cookscopeai.com.ui.screens.SettingsScreen
import cookscopeai.com.ui.screens.SignUpScreen
import cookscopeai.com.ui.screens.VerifyEmailScreen
import cookscopeai.com.ui.screens.WelcomeScreen
import cookscopeai.com.ui.theme.BlackBg
import cookscopeai.com.ui.theme.GoldPrimary
import cookscopeai.com.utilities.openUrl
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

/**
 * Root navigation graph. Decides between the onboarding/auth flow and
 * the main app based on the Supabase Auth session status:
 *
 *  - Initializing      → splash spinner
 *  - NotAuthenticated  → welcome → signup → verify → (login) →
 *                        account type → preferences → paywall → main
 *  - Authenticated     → main app (Home, Explore, Chat, Create, Profile)
 *
 * On an authenticated session, onboarding completion is checked:
 *  - If role not yet locked  → account type screen
 *  - If onboarding not done  → onboarding preferences
 *  - Otherwise               → straight to Home.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val sessionStatus by AuthManager.sessionStatus.collectAsState()
    val isOnboardingComplete by UserManager.isOnboardingComplete.collectAsState()
    val isRoleLocked by UserManager.isRoleLocked.collectAsState()
    val scope = rememberCoroutineScope()

    val mainRoutes = listOf("home", "explore", "chat", "create", "profile")
    val showBottomBar = currentRoute in mainRoutes
    val context = androidx.compose.ui.platform.LocalContext.current

    // Determine the start destination based on auth + onboarding state.
    val startDestination: String = when (val s = sessionStatus) {
        is SessionStatus.Initializing -> "splash"
        is SessionStatus.NotAuthenticated -> "welcome"
        is SessionStatus.Authenticated -> when {
            !isRoleLocked -> "account_type"
            !isOnboardingComplete -> "onboarding_preferences"
            else -> "home"
        }
        else -> "splash" // RefreshFailure or unknown → show splash briefly
    }

    // React to session changes during the lifetime of the app.
    LaunchedEffect(sessionStatus) {
        val s = sessionStatus
        val route = currentRoute
        if (s is SessionStatus.Authenticated) {
            // If we're on an auth screen when the session becomes
            // authenticated, jump forward to the next onboarding step
            // (or home if already onboarded).
            if (route in listOf("welcome", "signup", "verify", "login")) {
                val target = when {
                    !isRoleLocked -> "account_type"
                    !isOnboardingComplete -> "onboarding_preferences"
                    else -> "home"
                }
                navController.navigate(target) {
                    popUpTo("welcome") { inclusive = true }
                }
            }
        } else if (s is SessionStatus.NotAuthenticated) {
            // Signed out — clear back stack and go to welcome.
            if (route != null && route !in listOf("welcome", "splash")) {
                navController.navigate("welcome") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    if (sessionStatus is SessionStatus.Initializing) {
        Box(
            modifier = Modifier.fillMaxSize().background(BlackBg),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = GoldPrimary)
        }
        return
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                CookScopeBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            composable("splash") { /* handled above */ }

            // ---- Onboarding / Auth flow ----
            composable("welcome") {
                WelcomeScreen(
                    onGetStarted = { navController.navigate("signup") },
                    onSignIn = { navController.navigate("login") },
                )
            }
            composable("signup") {
                SignUpScreen(
                    onBack = { navController.popBackStack() },
                    onSignUpSuccess = { username, email ->
                        UserManager.updateProfile(username = username, displayName = username)
                        navController.navigate("verify/$email")
                    },
                    onGoogleSuccess = {
                        // Session becomes Authenticated → LaunchedEffect navigates.
                    },
                )
            }
            composable("verify/{email}") { entry ->
                val email = entry.arguments?.getString("email") ?: ""
                VerifyEmailScreen(
                    email = email,
                    onBack = { navController.popBackStack() },
                    onContinue = {
                        // If session is Authenticated, LaunchedEffect moves forward.
                        // Otherwise, prompt login.
                        if (!AuthManager.isAuthenticated) {
                            navController.navigate("login") {
                                popUpTo("welcome") { inclusive = false }
                            }
                        }
                    },
                )
            }
            composable("login") {
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    onLoginSuccess = {
                        // Session becomes Authenticated → LaunchedEffect navigates.
                    },
                    onSignUp = { navController.navigate("signup") },
                )
            }
            composable("account_type") {
                AccountTypeSelectionScreen(
                    onContinue = { navController.navigate("onboarding_preferences") },
                )
            }
            composable("onboarding_preferences") {
                OnboardingPreferencesScreen(
                    onContinue = { navController.navigate("paywall_onboarding") },
                )
            }
            composable("paywall_onboarding") {
                PaywallScreen(
                    onBack = { navController.popBackStack() },
                    onPurchased = {
                        UserManager.setOnboardingComplete()
                        navController.navigate("home") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    },
                    onSkip = {
                        UserManager.setOnboardingComplete()
                        navController.navigate("home") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    },
                    forceLightMode = true,
                )
            }

            // ---- Main app ----
            composable("home") {
                HomeScreen(
                    onRecipeClick = { id -> navController.navigate("recipe/$id") },
                    onExploreClick = { navController.navigate("explore") },
                    onScanClick = { navController.navigate("scan") },
                )
            }
            composable("explore") {
                ExploreScreen(
                    onRecipeClick = { id -> navController.navigate("recipe/$id") }
                )
            }
            composable("chat") {
                ChatbotScreen()
            }
            composable("create") {
                CreateRecipeScreen(
                    onRecipeGenerated = { id -> navController.navigate("recipe/$id") }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onUpgradeClick = { navController.navigate("paywall") },
                    onEditProfile = { navController.navigate("edit_profile") },
                    onSettingsClick = { navController.navigate("settings") },
                    onSavedRecipes = { navController.navigate("saved_recipes") },
                )
            }
            composable("recipe/{recipeId}") { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                RecipeDetailScreen(
                    recipeId = recipeId,
                    onBack = { navController.popBackStack() },
                    onStartCooking = { id -> navController.navigate("cook/$id") },
                )
            }
            composable("cook/{recipeId}") { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                CookingGuideScreen(
                    recipeId = recipeId,
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.popBackStack("home", false) },
                )
            }
            composable("scan") {
                DishScanScreen(
                    onBack = { navController.popBackStack() },
                    onUpgrade = { navController.navigate("paywall") },
                )
            }
            composable("paywall") {
                PaywallScreen(
                    onBack = { navController.popBackStack() },
                    onPurchased = { navController.popBackStack() },
                    onSkip = { navController.popBackStack() },
                )
            }
            composable("edit_profile") {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onPreferences = { navController.navigate("preferences") },
                    onSavedRecipes = { navController.navigate("saved_recipes") },
                    onUpgrade = { navController.navigate("paywall") },
                    onSignOut = {
                        scope.launch { AuthManager.signOut() }
                        // LaunchedEffect above routes to welcome.
                    },
                    onTerms = { openUrl(context, "https://cookscope.ai/terms") },
                    onPrivacy = { openUrl(context, "https://cookscope.ai/privacy") },
                )
            }
            composable("preferences") {
                PreferencesScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable("saved_recipes") {
                SavedRecipesScreen(
                    onBack = { navController.popBackStack() },
                    onRecipeClick = { id -> navController.navigate("recipe/$id") },
                )
            }
        }
    }
}
