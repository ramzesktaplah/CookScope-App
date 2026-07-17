package cookscopeai.com.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cookscopeai.com.ui.screens.ChatbotScreen
import cookscopeai.com.ui.screens.CookingGuideScreen
import cookscopeai.com.ui.screens.CreateRecipeScreen
import cookscopeai.com.ui.screens.DishScanScreen
import cookscopeai.com.ui.screens.EditProfileScreen
import cookscopeai.com.ui.screens.ExploreScreen
import cookscopeai.com.ui.screens.HomeScreen
import cookscopeai.com.ui.screens.PaywallScreen
import cookscopeai.com.ui.screens.PreferencesScreen
import cookscopeai.com.ui.screens.ProfileScreen
import cookscopeai.com.ui.screens.RecipeDetailScreen
import cookscopeai.com.ui.screens.SavedRecipesScreen
import cookscopeai.com.ui.screens.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf("home", "explore", "chat", "create", "profile")

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
            startDestination = "home",
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
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
                )
            }
            composable("paywall") {
                PaywallScreen(
                    onBack = { navController.popBackStack() },
                    onPurchased = { navController.popBackStack() },
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
