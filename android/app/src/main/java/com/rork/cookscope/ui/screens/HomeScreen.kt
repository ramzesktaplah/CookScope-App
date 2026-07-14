package com.rork.cookscope.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rork.cookscope.data.RecipeData
import com.rork.cookscope.data.RecipeDiscoveryService
import com.rork.cookscope.data.model.Cuisine
import com.rork.cookscope.data.model.Recipe
import com.rork.cookscope.ui.components.RecipeCard
import com.rork.cookscope.ui.theme.BlackBg
import com.rork.cookscope.ui.theme.GoldDeep
import com.rork.cookscope.ui.theme.GoldLight
import com.rork.cookscope.ui.theme.GoldPrimary
import com.rork.cookscope.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onRecipeClick: (String) -> Unit,
    onExploreClick: () -> Unit,
    onScanClick: () -> Unit,
) {
    val discoveryService = remember { RecipeDiscoveryService() }
    var isLoading by remember { mutableStateOf(true) }
    var featuredRecipes by remember { mutableStateOf(RecipeData.featured) }
    var trendingRecipes by remember { mutableStateOf(RecipeData.trending.take(4)) }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = discoveryService.loadRandomRecipes(10)
        result.fold(
            onSuccess = { recipes ->
                if (recipes.isNotEmpty()) {
                    featuredRecipes = recipes.take(3)
                    trendingRecipes = recipes.take(4)
                }
            },
            onFailure = {
                // Fall back to seed data — already set as defaults
            }
        )
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // Header with user greeting + search bar
        item { HeaderSection(onExploreClick) }

        // Hero CTA card — Scan Your Dish (full width)
        item { HeroCtaSection(onScanClick) }

        // Featured Recipes
        item {
            SectionHeader("Featured Tonight", "Hand-picked by CookScope AI")
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(featuredRecipes) { recipe ->
                    Box(modifier = Modifier.width(280.dp)) {
                        RecipeCard(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe.id) },
                            cardHeight = 260,
                        )
                    }
                }
            }
        }

        // Cuisine explorer
        item {
            Spacer(Modifier.height(28.dp))
            SectionHeader("Explore by Cuisine", "Thousands of recipes await")
        }
        item { CuisineRow(RecipeData.cuisines, onExploreClick) }

        // Trending
        item {
            Spacer(Modifier.height(28.dp))
            SectionHeader("Trending This Week", "Loved by 100K+ home chefs")
        }
        items(trendingRecipes) { recipe ->
            TrendingRecipeRow(recipe = recipe, onClick = { onRecipeClick(recipe.id) })
        }
    }
}

@Composable
private fun HeaderSection(onExploreClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 56.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
        // User greeting
        Text(
            "Good Evening, Chef",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "What are we cooking tonight?",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(20.dp))
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .clickable { onExploreClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Search recipes, cuisines, ingredients...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun HeroCtaSection(onCreateClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(GoldLight, GoldPrimary, GoldDeep)
                )
            )
            .clickable { onCreateClick() }
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Scan Your Dish",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "AI analyzes nutrition, calories & health score",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.7f),
                )
            }
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.25f),
                modifier = Modifier.size(80.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CuisineRow(cuisines: List<Cuisine>, onExploreClick: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cuisines) { cuisine ->
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onExploreClick() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(cuisine.emoji, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    cuisine.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    cuisine.tagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TrendingRecipeRow(recipe: Recipe, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = recipe.imageUrl,
            contentDescription = recipe.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                recipe.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    recipe.cuisine,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    " • ${recipe.cookTimeMin} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    "${String.format("%.1f", recipe.rating)} (${recipe.reviewCount})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
