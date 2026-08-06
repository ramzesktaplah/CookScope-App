package cookscopeai.com.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import cookscopeai.com.data.RecipeData
import cookscopeai.com.data.RecipeDiscoveryService
import cookscopeai.com.data.SearchUsageManager
import cookscopeai.com.data.SubscriptionManager
import cookscopeai.com.data.model.Cuisine
import cookscopeai.com.data.model.Recipe
import cookscopeai.com.ui.theme.GoldPrimary
import cookscopeai.com.ui.theme.GoldDeep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
// Home dashboard — restored classic layout

private const val HOME_PREFS = "discover_prefs"
private const val KEY_DAILY_RECIPES_LOADED_AT = "daily_recipes_loaded_at"
private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val HOME_BANNER_AD_UNIT_ID = "ca-app-pub-5940074004186936/9848909749"

/**
 * Home screen — CookScope's cooking-focused dashboard.
 *
 * Shows a time-based greeting, a search bar, a "Scan Your Dish" hero card,
 * a horizontal Featured Recipe carousel, an Explore by Cuisine section,
 * and a Trending This Week list. Search rate limits, search persistence,
 * daily recipe loading, and the local fallback chain are all preserved.
 */
@Composable
fun HomeScreen(
    onRecipeClick: (String) -> Unit,
    onScanClick: () -> Unit,
    onUpgrade: () -> Unit = {},
) {
    val context = LocalContext.current
    val discoveryService = remember { RecipeDiscoveryService() }
    val scope = rememberCoroutineScope()
    val isPremium by SubscriptionManager.isPremium.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var dailyRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    // Use rememberSaveable so search state survives navigation to recipe detail and back.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var hasSearched by rememberSaveable { mutableStateOf(false) }
    var searchResultIds by rememberSaveable {
        mutableStateOf<List<String>>(emptyList())
    }
    var searchResults by remember(searchResultIds) {
        mutableStateOf(searchResultIds.mapNotNull { RecipeData.byId(it) })
    }
    var showSearchLimitDialog by remember { mutableStateOf(false) }

    suspend fun loadDailyRecipes(forceRefresh: Boolean) {
        isLoading = true
        loadError = null
        val prefs = homePrefs(context)
        val loadedAt = prefs.getLong(KEY_DAILY_RECIPES_LOADED_AT, 0L)
        val isFresh = System.currentTimeMillis() - loadedAt < ONE_DAY_MILLIS

        val cachedDaily = RecipeData.dailyRecipes
        val result = if (!forceRefresh && isFresh && cachedDaily.isNotEmpty()) {
            Result.success(cachedDaily)
        } else {
            discoveryService.loadDailyRecipes(20)
        }

        result.fold(
            onSuccess = { recipes ->
                val recipesWithBranding = recipes.map { it.copy(authorName = "CookScopeAI") }
                dailyRecipes = recipesWithBranding
                RecipeData.setDailyRecipes(recipesWithBranding)
                RecipeData.setApiRecipes(recipesWithBranding)
                prefs.edit().putLong(KEY_DAILY_RECIPES_LOADED_AT, System.currentTimeMillis()).apply()
                loadError = null
            },
            onFailure = {
                dailyRecipes = emptyList()
                loadError = "Could not load recipes. Pull down to try again."
            }
        )
        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadDailyRecipes(forceRefresh = false)
    }

    fun performSearch() {
        if (searchQuery.isBlank()) return
        if (!SearchUsageManager.canSearch()) {
            showSearchLimitDialog = true
            return
        }
        isSearching = true
        hasSearched = true
        scope.launch {
            val result = discoveryService.searchRecipes(searchQuery)
            isSearching = false
            result.fold(
                onSuccess = { recipes ->
                    val recipesWithBranding = recipes.map { it.copy(authorName = "CookScopeAI") }
                    searchResultIds = recipesWithBranding.map { it.id }
                    searchResults = recipesWithBranding
                    RecipeData.setApiRecipes(recipesWithBranding)
                    SearchUsageManager.increment()
                },
                onFailure = { searchResults = emptyList() }
            )
        }
    }

    fun clearSearch() {
        searchQuery = ""
        searchResults = emptyList()
        searchResultIds = emptyList()
        hasSearched = false
    }

    val featuredRecipes = remember(dailyRecipes) {
        dailyRecipes.take(6).ifEmpty { RecipeData.featured }
    }
    val trendingRecipes = remember(dailyRecipes) {
        dailyRecipes.take(10).ifEmpty { RecipeData.trending.take(10) }
    }
    val cuisines = RecipeData.cuisines

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (hasSearched) {
            // Search results view — full screen
            Column(modifier = Modifier.fillMaxSize()) {
                HomeSearchBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onSearch = { performSearch() },
                    onClearSearch = { clearSearch() },
                    modifier = Modifier.padding(top = 56.dp, start = 20.dp, end = 20.dp, bottom = 8.dp),
                )
                SearchResultsSection(
                    isSearching = isSearching,
                    searchResults = searchResults,
                    searchQuery = searchQuery,
                    onClearSearch = { clearSearch() },
                    onRecipeClick = onRecipeClick,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                // Greeting + search
                item {
                    HomeGreeting(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onSearch = { performSearch() },
                        onClearSearch = { clearSearch() },
                    )
                }

                // Scan Your Dish hero card
                item {
                    ScanHeroCard(onScanClick = onScanClick)
                }

                // Featured Recipes carousel
                item {
                    SectionHeader(title = "Featured Recipes")
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(featuredRecipes) { recipe ->
                            FeaturedRecipeCard(
                                recipe = recipe,
                                onClick = { onRecipeClick(recipe.id) },
                            )
                        }
                    }
                }

                // Explore by Cuisine
                item {
                    SectionHeader(title = "Explore by Cuisine")
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(cuisines) { cuisine ->
                            CuisineChip(
                                cuisine = cuisine,
                                onClick = {
                                    searchQuery = cuisine.name
                                    performSearch()
                                },
                            )
                        }
                    }
                }

                // Trending This Week
                item {
                    SectionHeader(title = "Trending This Week")
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
                        }
                    }
                } else if (loadError != null) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                loadError.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GoldPrimary)
                                    .clickable {
                                        scope.launch { loadDailyRecipes(forceRefresh = true) }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    "Try Again",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                } else {
                    items(trendingRecipes) { recipe ->
                        TrendingRecipeRow(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe.id) },
                        )
                    }
                }

                // Banner ad at the bottom
                item {
                    Spacer(Modifier.height(12.dp))
                    HomeBannerAd()
                }
            }
        }
    }

    // Search rate limit dialog
    if (showSearchLimitDialog) {
        var searchDialogTick by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                searchDialogTick++
            }
        }
        val searchTimeRemaining = remember(searchDialogTick) {
            SearchUsageManager.formatTimeRemaining()
        }
        RateLimitDialog(
            featureName = "searches",
            freeLimit = SearchUsageManager.FREE_LIMIT,
            premiumLimit = SearchUsageManager.PREMIUM_LIMIT,
            windowDescription = "24-hour window",
            timeRemaining = searchTimeRemaining,
            isPremium = isPremium,
            onUpgrade = {
                showSearchLimitDialog = false
                onUpgrade()
            },
            onDismiss = { showSearchLimitDialog = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Greeting + Search
// ---------------------------------------------------------------------------

@Composable
private fun HomeGreeting(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 56.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)
    ) {
        val greeting = remember {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..21 -> "Good evening"
                else -> "Cooking late?"
            }
        }
        Text(
            greeting,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "What are we cooking?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        HomeSearchBar(
            searchQuery = searchQuery,
            onSearchChange = onSearchChange,
            onSearch = onSearch,
            onClearSearch = onClearSearch,
        )
    }
}

@Composable
private fun HomeSearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = {
            Text(
                "Search recipes, ingredients, cuisines...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = onClearSearch) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = GoldPrimary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------------------
// Scan Your Dish hero card
// ---------------------------------------------------------------------------

@Composable
private fun ScanHeroCard(onScanClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        GoldPrimary,
                        GoldDeep,
                    )
                )
            )
            .clickable { onScanClick() }
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Scan Your Dish",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Snap a photo and let AI analyze your meal",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterEnd),
        )
    }
}

// ---------------------------------------------------------------------------
// Section header
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
    )
}

// ---------------------------------------------------------------------------
// Featured Recipe card (horizontal carousel)
// ---------------------------------------------------------------------------

@Composable
private fun FeaturedRecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = recipe.imageUrl,
            contentDescription = recipe.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.85f),
                        ),
                        startY = 60f,
                        endY = 200f,
                    )
                )
        )
        if (recipe.isPremium) {
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldPrimary)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    "PREMIUM",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Row(
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "★",
                color = GoldPrimary,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.width(2.dp))
            Text(
                String.format("%.1f", recipe.rating),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                recipe.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetaChip(Icons.Default.Schedule, "${recipe.cookTimeMin} min")
                MetaChip(Icons.Default.Restaurant, recipe.difficulty.label)
                MetaChip(Icons.Default.LocalFireDepartment, "${recipe.caloriesPerServing} kcal")
            }
        }
    }
}

@Composable
private fun MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f),
        )
    }
}

// ---------------------------------------------------------------------------
// Cuisine chip
// ---------------------------------------------------------------------------

@Composable
private fun CuisineChip(
    cuisine: Cuisine,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            cuisine.emoji,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            cuisine.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// Trending recipe row
// ---------------------------------------------------------------------------

@Composable
private fun TrendingRecipeRow(
    recipe: Recipe,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                recipe.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "By ${recipe.authorName}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        String.format("%.1f", recipe.rating),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${recipe.cookTimeMin} min",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Search results section
// ---------------------------------------------------------------------------

@Composable
private fun SearchResultsSection(
    isSearching: Boolean,
    searchResults: List<Recipe>,
    searchQuery: String,
    onClearSearch: () -> Unit,
    onRecipeClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
            }
        } else if (searchResults.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${searchResults.size} results for \"$searchQuery\"",
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Clear",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onClearSearch() },
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(searchResults) { recipe ->
                    SearchRecipeCard(
                        recipe = recipe,
                        onClick = { onRecipeClick(recipe.id) },
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No recipes found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Try a different search term",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Clear search",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onClearSearch() },
                )
            }
        }
    }
}

@Composable
private fun SearchRecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(14.dp)),
        ) {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            recipe.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "By ${recipe.authorName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                "${String.format("%.1f", recipe.rating)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${recipe.cookTimeMin} mins",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Banner ad
// ---------------------------------------------------------------------------

@Composable
private fun HomeBannerAd() {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    if (isPreview) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Ad Banner",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val adView = remember {
        AdView(context).apply {
            val adSize = AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, 360)
            val adRequest = BannerAdRequest.Builder(HOME_BANNER_AD_UNIT_ID, adSize).build()
            loadAd(
                adRequest,
                object : AdLoadCallback<BannerAd> {
                    override fun onAdLoaded(ad: BannerAd) {
                        Log.d("CookScopeAd", "Home banner ad loaded")
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.w("CookScopeAd", "Home banner ad failed to load: $adError")
                    }
                },
            )
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { adView },
    )

    DisposableEffect(Unit) {
        onDispose { adView.unregisterBannerAd() }
    }
}

private fun homePrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(HOME_PREFS, Context.MODE_PRIVATE)
}
