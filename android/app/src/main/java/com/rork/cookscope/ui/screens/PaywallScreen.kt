package com.rork.cookscope.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.cookscope.data.SubscriptionManager
import com.rork.cookscope.ui.theme.BlackBg
import com.rork.cookscope.ui.theme.BlackCard
import com.rork.cookscope.ui.theme.BlackSurface
import com.rork.cookscope.ui.theme.GoldLight
import com.rork.cookscope.ui.theme.GoldPrimary
import com.rork.cookscope.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun PaywallScreen(onBack: () -> Unit, onPurchased: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPremium by SubscriptionManager.isPremium.collectAsState()
    val isLoading by SubscriptionManager.isLoading.collectAsState()
    val error by SubscriptionManager.error.collectAsState()
    val monthlyPrice by SubscriptionManager.monthlyPrice.collectAsState()
    val yearlyPrice by SubscriptionManager.yearlyPrice.collectAsState()
    var selectedAnnual by remember { mutableStateOf(true) }
    var purchaseAttempted by remember { mutableStateOf(false) }

    // Navigate away when premium becomes active after a purchase
    LaunchedEffect(isPremium, purchaseAttempted) {
        if (isPremium && purchaseAttempted) {
            onPurchased()
        }
    }

    val monthlyDisplay = monthlyPrice ?: "$4.99"
    val yearlyDisplay = yearlyPrice ?: "$49.99"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BlackSurface, BlackBg)
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
                        .background(
                            Brush.radialGradient(
                                colors = listOf(GoldPrimary, GoldPrimary.copy(alpha = 0.3f))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(44.dp),
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
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            // Pricing options — Monthly vs Yearly
            item {
                Spacer(Modifier.height(28.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Yearly option — selected by default
                    PricingOption(
                        title = "Yearly",
                        price = yearlyDisplay,
                        period = "/year",
                        badge = "SAVE 17%",
                        subtitle = "~$4.17/month • 10% off",
                        isSelected = selectedAnnual,
                        onClick = { selectedAnnual = true },
                    )
                    // Monthly option
                    PricingOption(
                        title = "Monthly",
                        price = monthlyDisplay,
                        period = "/month",
                        badge = null,
                        subtitle = "Cancel anytime",
                        isSelected = !selectedAnnual,
                        onClick = { selectedAnnual = false },
                    )
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
                    Button(
                        onClick = {
                            purchaseAttempted = true
                            scope.launch {
                                val activity = context as? android.app.Activity
                                if (activity != null) {
                                    SubscriptionManager.purchaseSubscription(activity, isAnnual = selectedAnnual)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color.Black,
                        ),
                        enabled = !isLoading && !isPremium,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp),
                            )
                        } else if (isPremium) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Premium Active", fontWeight = FontWeight.Bold)
                        } else {
                            val planLabel = if (selectedAnnual) "Yearly" else "Monthly"
                            val priceLabel = if (selectedAnnual) yearlyDisplay else monthlyDisplay
                            Text(
                                "Subscribe $planLabel — $priceLabel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
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
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Restore Purchases",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
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
                    color = TextSecondary.copy(alpha = 0.6f),
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
                if (isSelected) GoldPrimary.copy(alpha = 0.12f) else BlackCard
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) GoldPrimary else Color(0xFF2A2A2A),
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
                        color = Color.White,
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
                    color = TextSecondary,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    price,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isSelected) GoldPrimary else Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    period,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
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
                .size(32.dp)
                .clip(CircleShape)
                .background(GoldPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp,
            )
        }
    }
}
