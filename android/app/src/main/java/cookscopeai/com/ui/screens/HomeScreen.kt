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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import cookscopeai.com.data.model.Recipe
import cookscopeai.com.ui.theme.GoldPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DISCOVER_PREFS = "discover_prefs"
private const val KEY_DAILY_RECIPES_LOADED_AT = "daily_recipes_loaded_at"
private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val DISCOVER_BANNER_AD_UNIT_ID = "ca-app-pub-5940074004186936/9848909749"

/**
 * Discover screen — renamed from Home. Shows a search bar, category chips
 * (All, Breakfast, Lunch, Dinner, Desserts), and a two-column grid of popular
 * recipes. Search rate limits and search persistence are preserved.
 * Loads 20 fresh recipes daily from recipeapi.io, falling back to USDA food cards.
 */
@Composable
fun HomeScreen(
    onRecipeClick: (String) -> Unit,
    onExploreClick: () -> Unit,
    onScanClick: () -> Unit,
    onUpgrade: () -> Unit = {},
) {
    val context = LocalContext.current
    val discoveryService = remember { RecipeDiscoveryService() }
    val scope = rememberCoroutineScope()
    val isPremium by SubscriptionManager.isPremium.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var popularRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    // Use rememberSaveable so search state survives navigation to recipe detail and back.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var hasSearched by rememberSaveable { mutableStateOf(false) }
    // Store search result IDs in rememberSaveable, re-resolve to Recipe objects on recompose.
    var searchResultIds by rememberSaveable {
        mutableStateOf<List<String>>(emptyList())
    }
    var searchResults by remember(searchResultIds) {
        mutableStateOf(searchResultIds.mapNotNull { RecipeData.byId(it) })
    }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var showSearchLimitDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "Breakfast", "Lunch", "Dinner", "Desserts")

    // Load 20 recipes daily. If the cached batch is fresh, use it; otherwise fetch.
    LaunchedEffect(Unit) {
        isLoading = true
        val prefs = discoverPrefs(context)
        val loadedAt = prefs.getLong(KEY_DAILY_RECIPES_LOADED_AT, 0L)
        val isFresh = System.currentTimeMillis() - loadedAt < ONE_DAY_MILLIS

        val cachedDaily = RecipeData.dailyRecipes
        val result = if (isFresh && cachedDaily.isNotEmpty()) {
            Result.success(cachedDaily)
        } else {
            discoveryService.loadRandomRecipes(20)
        }

        result.fold(
            onSuccess = { recipes ->
                val recipesWithBranding = recipes.map { it.copy(authorName = "CookScopeAI") }
                popularRecipes = recipesWithBranding
                RecipeData.setDailyRecipes(recipesWithBranding)
                prefs.edit().putLong(KEY_DAILY_RECIPES_LOADED_AT, System.currentTimeMillis()).apply()
            },
            onFailure = {
                popularRecipes = RecipeData.featured.map { it.copy(authorName = "CookScopeAI") }
            }
        )
        isLoading = false
    }

    fun performSearch() {
        if (searchQuery.isBlank()) return
        if (!SearchUsageManager.canSearch()) {
            showSearchLimitDialog = true
            return
        }
        isSearching = true
        hasSearched = true
        selectedCategory = "All"
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

    val filteredPopular = remember(selectedCategory, popularRecipes) {
        if (selectedCategory == "All") {
            popularRecipes
        } else {
            popularRecipes.filter { recipe ->
                recipe.tags.any { it.equals(selectedCategory, ignoreCase = true) } ||
                    recipe.cuisine.equals(selectedCategory, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Static header + search + chips (not inside the scrollable grid)
        DiscoverHeader(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            onSearch = { performSearch() },
            onClearSearch = { clearSearch() },
        )

        if (hasSearched) {
            SearchResultsSection(
                isSearching = isSearching,
                searchResults = searchResults,
                searchQuery = searchQuery,
                onClearSearch = { clearSearch() },
                onRecipeClick = onRecipeClick,
            )
        } else {
            CategoryChips(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Popular Recipes",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "View all",
                                style = MaterialTheme.typography.labelLarge,
                                color = GoldPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onExploreClick() },
                            )
                        }
                    }

                    items(filteredPopular) { recipe ->
                        DiscoverRecipeCard(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe.id) },
                        )
                    }
                }
            }
        }

        // Anchored adaptive banner at the bottom of the Discover tab.
        DiscoverBannerAd()
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

@Composable
private fun DiscoverBannerAd() {
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
            val adRequest = BannerAdRequest.Builder(DISCOVER_BANNER_AD_UNIT_ID, adSize).build()
            loadAd(
                adRequest,
                object : AdLoadCallback<BannerAd> {
                    override fun onAdLoaded(ad: BannerAd) {
                        Log.d("CookScopeAd", "Discover banner ad loaded")
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.w("CookScopeAd", "Discover banner ad failed to load: $adError")
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

@Composable
private fun DiscoverHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 56.dp, start = 20.dp, end = 20.dp, bottom = 12.dp)
    ) {
        Text(
            "Discover Recipes",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Find your next favorite meal",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CategoryChips(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) GoldPrimary else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    category,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(searchResults) { recipe ->
                    DiscoverRecipeCard(
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
private fun DiscoverRecipeCard(
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
            // Bookmark icon placeholder
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Save",
                    tint = GoldPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
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

private fun discoverPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(DISCOVER_PREFS, Context.MODE_PRIVATE)
}
