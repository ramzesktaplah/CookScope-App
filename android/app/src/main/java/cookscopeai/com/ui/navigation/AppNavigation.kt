package cookscopeai.com.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import cookscopeai.com.ui.screens.ChatbotScreen
import cookscopeai.com.ui.screens.CookingGuideScreen
import cookscopeai.com.ui.screens.CreateRecipeScreen
import cookscopeai.com.ui.screens.DishScanScreen
import cookscopeai.com.ui.screens.HomeScreen
import cookscopeai.com.ui.screens.IngredientScanScreen
import cookscopeai.com.ui.screens.LoginScreen
import cookscopeai.com.ui.screens.OnboardingGreetingScreen
import cookscopeai.com.ui.screens.OnboardingNotificationScreen
import cookscopeai.com.ui.screens.OnboardingPreferenceAllergiesScreen
import cookscopeai.com.ui.screens.OnboardingPreferenceCuisinesScreen
import cookscopeai.com.ui.screens.OnboardingPreferenceDietScreen
import cookscopeai.com.ui.screens.ScanSelectionScreen
import cookscopeai.com.ui.screens.OnboardingPreferenceSkillScreen
import cookscopeai.com.ui.screens.OnboardingSettingPreferencesScreen
import cookscopeai.com.ui.screens.PaywallScreen
import cookscopeai.com.ui.screens.PreferencesScreen
import cookscopeai.com.ui.screens.RecipeDetailScreen
import cookscopeai.com.ui.screens.SavedRecipesScreen
import cookscopeai.com.ui.screens.SettingsScreen
import cookscopeai.com.ui.screens.SignUpScreen
import cookscopeai.com.ui.screens.VerifyEmailScreen
import cookscopeai.com.ui.screens.WelcomeScreen
import cookscopeai.com.ui.theme.GoldPrimary
import cookscopeai.com.utilities.openUrl
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

/**
 * Root navigation graph. Routes based on the Supabase Auth session status
 * and the auth entry point (sign-up vs sign-in):
 *
 *  - Initializing      → splash spinner
 *  - NotAuthenticated  → welcome → onboarding preferences → signup/login → …
 *  - Authenticated     → main app (Home, Scan, Chat, Create, Saved)
 *
 * Post-auth routing depends on how the session was established:
 *  - SIGNUP  (new user via the sign-up screen)   → paywall → home
 *  - LOGIN   (returning user via the sign-in screen) → straight to Home
 *  - UNKNOWN (session restored on app launch) → decide from remote flag:
 *      onboarding not done → paywall; otherwise → home.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val sessionStatus by AuthManager.sessionStatus.collectAsState()
    val authEntryPoint by AuthManager.authEntryPoint.collectAsState()
    val isOnboardingComplete by UserManager.isOnboardingComplete.collectAsState()
    val hasRemoteProfile by UserManager.hasRemoteProfile.collectAsState()
    val isProfileLoading by UserManager.isProfileLoading.collectAsState()
    val scope = rememberCoroutineScope()

    val mainRoutes = listOf("home", "scan", "chat", "create", "saved")
    val showBottomBar = currentRoute in mainRoutes
    val context = androidx.compose.ui.platform.LocalContext.current

    // Determine the start destination based on auth + entry point + onboarding state.
    val startDestination: String = when (val s = sessionStatus) {
        is SessionStatus.Initializing -> "splash"
        is SessionStatus.NotAuthenticated -> "welcome"
        is SessionStatus.Authenticated -> postAuthDestination(
            entryPoint = authEntryPoint,
            isOnboardingComplete = isOnboardingComplete,
            hasRemoteProfile = hasRemoteProfile,
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
            if (route in listOf("welcome", "signup", "verify", "login", "notification_permission")) {
                val target = postAuthDestination(
                    entryPoint = authEntryPoint,
                    isOnboardingComplete = isOnboardingComplete,
                    hasRemoteProfile = hasRemoteProfile,
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
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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
                    onGetStarted = { navController.navigate("greeting") },
                    onSignIn = { navController.navigate("login") },
                )
            }
            composable("greeting") {
                OnboardingGreetingScreen(
                    onContinue = { navController.navigate("preference_skill") },
                    onBack = { navController.popBackStack("welcome", false) },
                )
            }
            composable("preference_skill") {
                OnboardingPreferenceSkillScreen(
                    onContinue = { navController.navigate("preference_diet") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("preference_diet") {
                OnboardingPreferenceDietScreen(
                    onContinue = { navController.navigate("preference_allergies") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("preference_allergies") {
                OnboardingPreferenceAllergiesScreen(
                    onContinue = { navController.navigate("preference_cuisines") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("preference_cuisines") {
                OnboardingPreferenceCuisinesScreen(
                    onContinue = { navController.navigate("setting_preferences") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("setting_preferences") {
                OnboardingSettingPreferencesScreen(
                    onContinue = { navController.navigate("notification_permission") },
                )
            }
            composable("notification_permission") {
                OnboardingNotificationScreen(
                    onContinue = { navController.navigate("signup") },
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
                        // Session becomes Authenticated → LaunchedEffect navigates to paywall.
                    },
                    onSignIn = {
                        navController.navigate("welcome") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    },
                )
            }
            composable("verify/{email}") { entry ->
                val email = entry.arguments?.getString("email") ?: ""
                VerifyEmailScreen(
                    email = email,
                    onBack = { navController.popBackStack() },
                    onContinue = {
                        navController.navigate("paywall_onboarding") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    },
                    onCancelSession = {
                        navController.navigate("welcome") {
                            popUpTo("welcome") { inclusive = true }
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
                    onSignUp = {
                        // Don't skip preferences — restart the full onboarding
                        // from Welcome so the user goes through the preference
                        // flow before reaching sign up again.
                        navController.popBackStack("welcome", false)
                    },
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
                )
            }

            // ---- Main app ----
            composable("home") {
                HomeScreen(
                    onRecipeClick = { id -> navController.navigate("recipe/$id") },
                    onExploreClick = { navController.navigate("scan") },
                    onScanClick = { navController.navigate("scan_dish") },
                )
            }
            composable("scan") {
                ScanSelectionScreen(
                    onScanDish = { navController.navigate("scan_dish") },
                    onScanIngredients = { navController.navigate("scan_ingredients") },
                )
            }
            composable("scan_ingredients") {
                IngredientScanScreen(
                    onBack = { navController.popBackStack() },
                    onRecipeClick = { id -> navController.navigate("recipe/$id") },
                    onUpgrade = { navController.navigate("paywall") },
                )
            }
            composable("chat") {
                ChatbotScreen(
                    onUpgrade = { navController.navigate("paywall") },
                )
            }
            composable("create") {
                CreateRecipeScreen(
                    onRecipeGenerated = { id -> navController.navigate("recipe/$id") }
                )
            }
            composable("saved") {
                SavedRecipesScreen(
                    onBack = null,
                    onRecipeClick = { id -> navController.navigate("recipe/$id") },
                    onSettings = { navController.navigate("settings") },
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
                    onUpgrade = { navController.navigate("paywall") },
                )
            }
            composable("scan_dish") {
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
 *  - LOGIN   → returning user → straight to Home (never re-show
 *              onboarding, even if the remote flag is somehow false)
 *  - SIGNUP  → new user → walk through paywall only if no remote profile
 *              exists and onboarding hasn't been completed. Existing users
 *              who sign in via the signup screen (e.g., Google) skip paywall.
 *  - UNKNOWN → session restored on app launch → go Home if the user has a
 *              remote profile or has completed onboarding; otherwise show
 *              the paywall so a new user can finish setup.
 */
private fun postAuthDestination(
    entryPoint: AuthEntryPoint,
    isOnboardingComplete: Boolean,
    hasRemoteProfile: Boolean,
): String = when (entryPoint) {
    AuthEntryPoint.LOGIN -> "home"
    AuthEntryPoint.SIGNUP, AuthEntryPoint.UNKNOWN -> {
        if (isOnboardingComplete || hasRemoteProfile) "home" else "paywall_onboarding"
    }
}
