package com.rork.cookscope.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rork.cookscope.data.SubscriptionManager
import com.rork.cookscope.data.UserManager
import com.rork.cookscope.data.model.UserRole
import com.rork.cookscope.ui.theme.BlackBg
import com.rork.cookscope.ui.theme.BlackCard
import com.rork.cookscope.ui.theme.BlackSurface
import com.rork.cookscope.ui.theme.GoldPrimary
import com.rork.cookscope.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    onUpgradeClick: () -> Unit,
    onEditProfile: () -> Unit,
    onSettingsClick: () -> Unit,
    onSavedRecipes: () -> Unit,
) {
    val profile by UserManager.profile.collectAsState()
    val isPremium by SubscriptionManager.isPremium.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val isChef = profile.role == UserRole.CHEF_TEACHER

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        // Top bar with settings icon (three-line bar)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, start = 20.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    profile.username,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPremium) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GoldPrimary)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "PREMIUM",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    // Three-line bar / settings icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onSettingsClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }

        // Profile header — Instagram style
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Profile photo
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.15f))
                        .border(2.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (profile.profilePhotoUri != null) {
                        AsyncImage(
                            model = profile.profilePhotoUri,
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                Spacer(Modifier.width(24.dp))

                // Stats — Stars (instead of followers) and Stars Given (instead of following)
                // For chefs: Posts, Stars, stars given
                // For learners: Stars Given only (no posts, no stars received)
                if (isChef) {
                    StatColumn(count = profile.posts.size.toString(), label = "Posts")
                    Spacer(Modifier.width(24.dp))
                    StatColumn(count = profile.starsReceived.toString(), label = "Stars")
                    Spacer(Modifier.width(24.dp))
                    StatColumn(count = profile.starsGiven.toString(), label = "Given")
                } else {
                    StatColumn(count = profile.starsGiven.toString(), label = "Stars Given")
                    Spacer(Modifier.width(32.dp))
                    StatColumn(count = if (isPremium) "Premium" else "Free", label = "Plan")
                }
            }
        }

        // Display name + bio
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    profile.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
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
                        profile.role.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Action buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Edit Profile button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable { onEditProfile() },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Edit Profile",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Saved recipes button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable { onSavedRecipes() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = "Saved",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Upgrade button for free users
                if (!isPremium) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoldPrimary)
                            .clickable { onUpgradeClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Upgrade",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        // Upgrade card for free users
        if (!isPremium) {
            item {
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(GoldPrimary.copy(alpha = 0.12f), BlackCard)
                            )
                        )
                        .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { onUpgradeClick() }
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Upgrade to Premium",
                                style = MaterialTheme.typography.titleSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "$4.99/mo or $49.99/yr — save 17%",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }
        }

        // Tab bar — only for chefs (Posts, Saved). Learners only see Saved.
        if (isChef) {
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = GoldPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GoldPrimary,
                        )
                    },
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                if (selectedTab == 0) Icons.Default.GridOn else Icons.Outlined.GridOn,
                                contentDescription = "Posts",
                                tint = if (selectedTab == 0) GoldPrimary else TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                if (selectedTab == 1) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Saved",
                                tint = if (selectedTab == 1) GoldPrimary else TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                    )
                }
            }
        } else {
            item {
                // Learner: just a Saved header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Saved Recipes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Content area
        if (isChef && selectedTab == 0) {
            // Chef Posts grid
            if (profile.posts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Add,
                        title = "No Posts Yet",
                        subtitle = "Share your culinary creations with the community",
                    )
                }
            } else {
                item {
                    PostsGrid(posts = profile.posts)
                }
            }
        } else {
            // Saved recipes (for both chef tab 2 and learner)
            item {
                SavedRecipesList(onSavedRecipes = onSavedRecipes)
            }
        }
    }
}

@Composable
private fun StatColumn(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = GoldPrimary.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PostsGrid(posts: List<com.rork.cookscope.data.model.ChefPost>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(((posts.size + 2) / 3 * 120).dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(posts) { post ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BlackCard),
            ) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = post.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Star count overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            post.stars.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedRecipesList(onSavedRecipes: () -> Unit) {
    val savedIds by UserManager.savedRecipeIds.collectAsState()
    if (savedIds.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.BookmarkBorder,
            title = "No Saved Recipes",
            subtitle = "Bookmark recipes you love to find them here",
        )
    } else {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "${savedIds.size} saved recipe${if (savedIds.size > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onSavedRecipes() }
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "View all saved recipes",
                        style = MaterialTheme.typography.titleSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
