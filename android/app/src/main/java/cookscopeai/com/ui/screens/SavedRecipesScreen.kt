package cookscopeai.com.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cookscopeai.com.data.RecipeData
import cookscopeai.com.data.UserManager
import cookscopeai.com.ui.theme.GoldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedRecipesScreen(
    onBack: (() -> Unit)?,
    onRecipeClick: (String) -> Unit,
    onSettings: () -> Unit = {},
) {
    val savedIds by UserManager.savedRecipeIds.collectAsState()
    val allSavedRecipes = savedIds.mapNotNull { RecipeData.byId(it) }

    // Diet filter state. "All" is always available; the rest are toggleable.
    var activeDietFilter by remember { mutableStateOf<String?>(null) }

    // Compute which diet filters actually have matching saved recipes so we
    // don't show chips the user can never use.
    val availableDietFilters = remember(allSavedRecipes) {
        DIET_FILTERS.filter { diet ->
            allSavedRecipes.any { recipe ->
                recipe.tags.any { tag -> tag.matchesDietFilter(diet) } ||
                    recipe.title.matchesDietFilter(diet)
            }
        }
    }

    val savedRecipes = remember(allSavedRecipes, activeDietFilter) {
        if (activeDietFilter == null) {
            allSavedRecipes
        } else {
            allSavedRecipes.filter { recipe ->
                recipe.tags.any { tag -> tag.matchesDietFilter(activeDietFilter) } ||
                    recipe.title.matchesDietFilter(activeDietFilter)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        // Top bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 4.dp, end = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
                Text(
                    "Saved Recipes",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${allSavedRecipes.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        // Diet filter chips — only show when there are saved recipes with
        // at least one matching diet tag.
        if (allSavedRecipes.isNotEmpty() && availableDietFilters.isNotEmpty()) {
            item {
                DietFilterRow(
                    availableFilters = availableDietFilters,
                    activeFilter = activeDietFilter,
                    onFilterSelected = { diet ->
                        activeDietFilter = if (activeDietFilter == diet) null else diet
                    },
                    onClear = { activeDietFilter = null },
                )
            }
        }

        if (allSavedRecipes.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = GoldPrimary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No Saved Recipes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Bookmark recipes you love to find them here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Swipe a recipe left to remove it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        } else if (savedRecipes.isEmpty() && activeDietFilter != null) {
            // Saved recipes exist but none match the active diet filter.
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.FilterAlt,
                        contentDescription = null,
                        tint = GoldPrimary.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No $activeDietFilter recipes saved",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Try a different filter or save a matching recipe",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(GoldPrimary.copy(alpha = 0.12f))
                            .clickable { activeDietFilter = null }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Clear filter",
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        } else {
            items(savedRecipes, key = { it.id }) { recipe ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            UserManager.toggleSavedRecipe(recipe.id)
                            true
                        } else {
                            false
                        }
                    },
                    positionalThreshold = { it * 0.5f },
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        SwipeDeleteBackground(progress = dismissState.dismissDirection)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onRecipeClick(recipe.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface),
                        ) {
                            AsyncImage(
                                model = recipe.imageUrl,
                                contentDescription = recipe.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                recipe.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${recipe.cuisine} • ${recipe.cookTimeMin} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${recipe.rating} • ${recipe.authorName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = "Saved",
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Diet filter chips shown above the saved recipes list. Tapping an active
 * filter again clears it. Only diets that have at least one matching saved
 * recipe are shown.
 */
@Composable
private fun DietFilterRow(
    availableFilters: List<String>,
    activeFilter: String?,
    onFilterSelected: (String) -> Unit,
    onClear: () -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
 item {
            FilterChip(
                selected = activeFilter == null,
                onClick = onClear,
                label = {
                    Text(
                        "All",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activeFilter == null) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldPrimary,
                    selectedLabelColor = Color.Black,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
        items(availableFilters, key = { it }) { diet ->
            FilterChip(
                selected = activeFilter == diet,
                onClick = { onFilterSelected(diet) },
                label = {
                    Text(
                        diet,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activeFilter == diet) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldPrimary,
                    selectedLabelColor = Color.Black,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Supported diet filters. Keep in sync with [matchesDietFilter].
 */
private val DIET_FILTERS = listOf(
    "Vegan",
    "Vegetarian",
    "Keto",
    "Gluten-Free",
    "Dairy-Free",
    "Pescatarian",
    "High Protein",
)

/**
 * Returns true when the receiver tag or title text matches the given diet
 * filter. Matching is deliberately fuzzy so seed-data tags like "Vegan",
 * "Gluten-Free", "Pescatarian", "High Protein" match cleanly while also
 * catching title mentions (e.g. "Vegan Brownie").
 */
private fun String.matchesDietFilter(diet: String?): Boolean {
    if (diet.isNullOrBlank()) return false
    val text = this.lowercase().replace("-", " ").replace("_", " ")
    val key = diet.lowercase().replace("-", " ").replace("_", " ")
    return text.contains(key)
}

@Composable
private fun SwipeDeleteBackground(progress: SwipeToDismissBoxValue) {
    val color = Color(0xFFE53935)
    val alignment = if (progress == SwipeToDismissBoxValue.EndToStart) {
        Alignment.CenterEnd
    } else {
        Alignment.CenterStart
    }
    val padEnd = if (progress == SwipeToDismissBoxValue.EndToStart) 20.dp else 0.dp
    val padStart = if (progress == SwipeToDismissBoxValue.StartToEnd) 20.dp else 0.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .padding(start = padStart, end = padEnd),
        contentAlignment = alignment,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
            if (progress == SwipeToDismissBoxValue.StartToEnd) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "Remove",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
