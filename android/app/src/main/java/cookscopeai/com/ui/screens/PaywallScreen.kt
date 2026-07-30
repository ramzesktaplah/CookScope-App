package cookscopeai.com.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revenuecat.purchases.Package
import cookscopeai.com.R
import cookscopeai.com.data.SubscriptionManager
import cookscopeai.com.ui.theme.GoldPrimary
import kotlinx.coroutines.launch

/**
 * Paywall screen — shown as the last step of onboarding and from the
 * Settings upgrade path. Offers monthly and yearly subscriptions.
 * Uses the regular app theme (light or dark).
 *
 * The pricing options are built dynamically from RevenueCat's current offering
 * so they match the configured dashboard packages and prices exactly.
 */
@Composable
fun PaywallScreen(
    onBack: () -> Unit,
    onPurchased: () -> Unit,
    onSkip: () -> Unit = onBack,
) {
    PaywallContent(
        onBack = onBack,
        onPurchased = onPurchased,
        onSkip = onSkip,
    )
}

@Composable
private fun PaywallContent(
    onBack: () -> Unit,
    onPurchased: () -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPremium by SubscriptionManager.isPremium.collectAsState()
    val isLoading by SubscriptionManager.isLoading.collectAsState()
    val error by SubscriptionManager.error.collectAsState()
    val availablePackages by SubscriptionManager.availablePackages.collectAsState()
    val hasStoreProducts by SubscriptionManager.hasStoreProducts.collectAsState()

    var selectedIndex by remember { mutableStateOf(0) }
    var purchaseAttempted by remember { mutableStateOf(false) }

    // Navigate away when premium becomes active after a purchase
    LaunchedEffect(isPremium, purchaseAttempted) {
        if (isPremium && purchaseAttempted) {
            onPurchased()
        }
    }

    // Default to the annual package if available, otherwise the first package.
    LaunchedEffect(availablePackages) {
        if (availablePackages.isNotEmpty()) {
            selectedIndex = availablePackages.indexOfFirst {
                it.packageType == com.revenuecat.purchases.PackageType.ANNUAL
            }.coerceAtLeast(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar with back button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldPrimary,
                        )
                    }
                }
            }

            // Hero badge
            item {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_brand_logo),
                        contentDescription = "CookScope logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(64.dp),
                    )
                }
            }

            // Title
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "CookScope Premium",
                    style = MaterialTheme.typography.headlineLarge,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Unlock the full culinary experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // Pricing options — built from RevenueCat packages
            item {
                Spacer(Modifier.height(28.dp))
                if (availablePackages.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        availablePackages.forEachIndexed { index, pkg ->
                            val isAnnual = pkg.packageType == com.revenuecat.purchases.PackageType.ANNUAL
                            PricingOption(
                                title = pkg.product.title.replace(" (CookScope)", "").replace("CookScope", "").trim(),
                                price = pkg.product.price.formatted,
                                period = if (isAnnual) "/year" else "/month",
                                badge = if (isAnnual) "BEST VALUE" else null,
                                subtitle = if (isAnnual) "Save with yearly billing" else "Cancel anytime",
                                isSelected = selectedIndex == index,
                                onClick = { selectedIndex = index },
                            )
                        }
                    }
                } else if (hasStoreProducts) {
                    // Loading state while packages resolve
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = GoldPrimary, strokeWidth = 2.dp)
                    }
                }
            }

            // Feature list
            item {
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PremiumFeature(
                        icon = Icons.Default.Star,
                        title = "Unlimited AI Recipe Generation",
                        description = "Create premium restaurant-quality recipes with any ingredients",
                    )
                    PremiumFeature(
                        icon = Icons.Default.Check,
                        title = "Premium Dish Scanner",
                        description = "Unlimited dish photo analysis with detailed nutrition breakdown",
                    )
                    PremiumFeature(
                        icon = Icons.Default.Star,
                        title = "Advanced Cooking Guide",
                        description = "Step-by-step AI guidance with timing, temps, and pro techniques",
                    )
                    PremiumFeature(
                        icon = Icons.Default.Check,
                        title = "Priority AI Chatbot",
                        description = "Unlimited messages with CookScope AI culinary assistant",
                    )
                    PremiumFeature(
                        icon = Icons.Default.Star,
                        title = "Ad-Free Experience",
                        description = "Enjoy CookScope without any interruptions",
                    )
                }
            }

            // CTA buttons
            item {
                Spacer(Modifier.height(32.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val selectedPackage = availablePackages.getOrNull(selectedIndex)
                    Button(
                        onClick = {
                            purchaseAttempted = true
                            scope.launch {
                                val activity = context as? android.app.Activity
                                if (activity != null) {
                                    SubscriptionManager.purchaseSubscription(activity, selectedIndex)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color.White,
                        ),
                        enabled = !isLoading && !isPremium && selectedPackage != null,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp),
                            )
                        } else if (isPremium) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Premium Active", fontWeight = FontWeight.Bold)
                        } else if (selectedPackage == null) {
                            Text("Store unavailable", fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                "Subscribe — ${selectedPackage.product.price.formatted}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Refresh offerings if products failed to load
                    if (availablePackages.isEmpty() && !isLoading) {
                        TextButton(
                            onClick = { SubscriptionManager.fetchOfferings() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "Reload store products",
                                color = GoldPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    // Restore purchases
                    TextButton(
                        onClick = {
                            scope.launch { SubscriptionManager.restorePurchases() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    ) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Restore Purchases",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Continue with Free plan — lets onboarding proceed
                    // without subscribing.
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    ) {
                        Text(
                            "Continue with Free plan",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Error message
                    AnimatedVisibility(
                        visible = error != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Text(
                            error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE53935),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Terms
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Payment will be charged to your Google Play account. " +
                        "Subscription auto-renews unless cancelled at least 24 hours " +
                        "before the end of the current period. " +
                        "Manage or cancel anytime in Google Play Store settings.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun PricingOption(
    title: String,
    price: String,
    period: String,
    badge: String?,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) GoldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldPrimary)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    price,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    period,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PremiumFeature(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(GoldPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
