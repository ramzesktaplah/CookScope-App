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
import cookscopeai.com.data.AuthEntryPoint
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
 * Root navigation graph. Routes based on the Supabase Auth session status
 * and the auth entry point (sign-up vs sign-in):
 *
 *  - Initializing      → splash spinner
 *  - NotAuthenticated  → welcome → signup/login → …
 *  - Authenticated     → main app (Home, Explore, Chat, Create, Profile)
 *
 * Post-auth routing depends on how the session was established:
 *  - SIGNUP  (new user via the sign-up screen)   → full onboarding:
 *      account type → preferences → paywall → home
 *  - LOGIN   (returning user via the sign-in screen) → straight to Home
 *      (account type and preferences are never re-shown — the role is
 *      already locked and onboarding already complete remotely)
 *  - UNKNOWN (session restored on app launch) → decide from remote flags:
 *      role not locked → account type; onboarding not done → preferences;
 *      otherwise → home.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val sessionStatus by AuthManager.sessionStatus.collectAsState()
    val authEntryPoint by AuthManager.authEntryPoint.collectAsState()
    val isOnboardingComplete by UserManager.isOnboardingComplete.collectAsState()
    val isRoleLocked by UserManager.isRoleLocked.collectAsState()
    val isProfileLoading by UserManager.isProfileLoading.collectAsState()
    val scope = rememberCoroutineScope()

    val mainRoutes = listOf("home", "explore", "chat", "create", "profile")
    val showBottomBar = currentRoute in mainRoutes
    val context = androidx.compose.ui.platform.LocalContext.current

    // Determine the start destination based on auth + entry point + onboarding state.
    val startDestination: String = when (val s = sessionStatus) {
        is SessionStatus.Initializing -> "splash"
        is SessionStatus.NotAuthenticated -> "welcome"
        is SessionStatus.Authenticated -> postAuthDestination(
            entryPoint = authEntryPoint,
            isRoleLocked = isRoleLocked,
            isOnboardingComplete = isOnboardingComplete,
        )
        else -> "splash" // RefreshFailure or unknown → show splash briefly
    }

    // React to session changes during the lifetime of the app.
    // Wait until the remote profile has finished loading before navigating
    // forward from an auth screen — otherwise returning users would be sent
    // back through onboarding because the flags are still false while the
    // profile is loading.
    LaunchedEffect(isProfileLoading, sessionStatus, authEntryPoint) {
        val s = sessionStatus
        val route = currentRoute
        if (s is SessionStatus.Authenticated && !isProfileLoading) {
            // If we're on an auth screen when the session becomes
            // authenticated (and profile is loaded), jump forward to the
            // correct destination based on how the session was established.
            if (route in listOf("welcome", "signup", "verify", "login")) {
                val target = postAuthDestination(
                    entryPoint = authEntryPoint,
                    isRoleLocked = isRoleLocked,
                    isOnboardingComplete = isOnboardingComplete,
                )
                navController.navigate(target) {
                    popUpTo("welcome") { inclusive = true }
                }
            }
        }
    }
    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.NotAuthenticated) {
            // Signed out — clear back stack and go to welcome.
            val route = currentRoute
            if (route != null && route !in listOf("welcome", "splash")) {
                navController.navigate("welcome") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    if (sessionStatus is SessionStatus.Initializing ||
        (sessionStatus is SessionStatus.Authenticated && isProfileLoading)
    ) {
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

            // Helper: compute the destination after an authenticated session
            // is established. LOGIN always goes straight to Home; SIGNUP walks
            // through onboarding based on the remote flags; UNKNOWN (restored
            // session) falls back to the remote flags.
            // (Defined as a top-level helper below.)

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

/**
 * Compute the destination route after an authenticated session is
 * established and the remote profile has finished loading.
 *
 *  - LOGIN   → returning user → straight to Home (never re-show account
 *              type or preferences, even if remote flags are somehow false)
 *  - SIGNUP  → new user → walk through onboarding based on remote flags:
 *              role not locked → account type; onboarding not done →
 *              preferences; otherwise → home (already onboarded somehow)
 *  - UNKNOWN → session restored on app launch → decide from remote flags:
 *              role not locked → account type; onboarding not done →
 *              preferences; otherwise → home.
 */
private fun postAuthDestination(
    entryPoint: AuthEntryPoint,
    isRoleLocked: Boolean,
    isOnboardingComplete: Boolean,
): String = when (entryPoint) {
    AuthEntryPoint.LOGIN -> "home"
    AuthEntryPoint.SIGNUP, AuthEntryPoint.UNKNOWN -> when {
        !isRoleLocked -> "account_type"
        !isOnboardingComplete -> "onboarding_preferences"
        else -> "home"
    }
}
