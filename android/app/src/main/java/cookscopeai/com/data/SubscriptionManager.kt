package cookscopeai.com.data

import android.app.Activity
import android.app.Application
import cookscopeai.com.Config
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages RevenueCat subscription state for CookScope Premium.
 * Tracks entitlement status, purchase flow, and restoration.
 *
 * RevenueCat packages are discovered dynamically from the current offering
 * rather than hardcoded by package key. This avoids breakage when the
 * dashboard uses custom package keys (e.g. "monthly"/"yearly" instead of
 * "$rc_monthly"/"$rc_annual").
 */
object SubscriptionManager {

    private const val ENTITLEMENT_ID = "CookScope Premium"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** All packages in the current offering, ordered by the dashboard. */
    private val _availablePackages = MutableStateFlow<List<Package>>(emptyList())
    val availablePackages: StateFlow<List<Package>> = _availablePackages.asStateFlow()

    /** Whether RevenueCat returned an offering with at least one package. */
    private val _hasStoreProducts = MutableStateFlow(false)
    val hasStoreProducts: StateFlow<Boolean> = _hasStoreProducts.asStateFlow()

    /**
     * Initialize RevenueCat SDK. Call from Activity.onCreate.
     * Uses the Test Store key for debug builds, Play Store key for release.
     *
     * Keys come from the auto-generated Config.kt (populated by the Rork
     * build system from `EXPO_PUBLIC_REVENUECAT_*` env vars) — no longer
     * hardcoded as Kotlin literals.
     */
    fun initialize(app: Application, isDebug: Boolean) {
        val apiKey = if (isDebug) {
            Config.EXPO_PUBLIC_REVENUECAT_TEST_API_KEY
        } else {
            Config.EXPO_PUBLIC_REVENUECAT_ANDROID_API_KEY
        }
        Purchases.configure(
            PurchasesConfiguration.Builder(app, apiKey)
                .purchasesAreCompletedBy(PurchasesAreCompletedBy.REVENUECAT)
                .appUserID(null)
                .diagnosticsEnabled(true)
                .build()
        )
        refreshEntitlementStatus()
        fetchOfferings()
    }

    /**
     * Check current entitlement status from RevenueCat.
     */
    fun refreshEntitlementStatus() {
        scope.launch {
            try {
                val info = Purchases.sharedInstance.awaitCustomerInfo()
                val wasPremium = _isPremium.value
                _isPremium.value = info.entitlements[ENTITLEMENT_ID]?.isActive == true
                // Reset all usage managers when premium status changes so the
                // user gets their new limits immediately.
                if (_isPremium.value && !wasPremium) {
                    ChatUsageManager.reset()
                    ScanUsageManager.reset()
                    RecipeGenerationUsageManager.reset()
                    SearchUsageManager.reset()
                }
            } catch (_: Exception) {
                // Silently fail — user might be offline
            }
        }
    }

    /**
     * Fetch the current offering and cache the available packages.
     * Call this on startup and again before a purchase if products are missing.
     */
    fun fetchOfferings() {
        scope.launch {
            try {
                val offerings = Purchases.sharedInstance.awaitOfferings()
                val current = offerings.current
                val packages = current?.availablePackages ?: emptyList()
                _availablePackages.value = packages
                _hasStoreProducts.value = packages.isNotEmpty()
                if (packages.isEmpty()) {
                    _error.value = "No subscription products found. Pull-to-refresh or try again."
                }
            } catch (e: Exception) {
                _availablePackages.value = emptyList()
                _hasStoreProducts.value = false
                _error.value = "Store not available: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    /**
     * Purchase a subscription by selecting the package at the given index.
     * The index maps to the order returned by RevenueCat's current offering.
     * Typically 0 = monthly, 1 = annual, but the dashboard controls ordering.
     */
    suspend fun purchaseSubscription(activity: Activity, packageIndex: Int): Boolean {
        _isLoading.value = true
        _error.value = null
        return try {
            var packages = _availablePackages.value
            if (packages.isEmpty()) {
                // Try to refresh offerings once before giving up.
                val offerings = Purchases.sharedInstance.awaitOfferings()
                val refreshed = offerings.current?.availablePackages ?: emptyList()
                _availablePackages.value = refreshed
                _hasStoreProducts.value = refreshed.isNotEmpty()
                packages = refreshed
            }

            val pkg = packages.getOrNull(packageIndex)
                ?: packages.firstOrNull()
                ?: throw IllegalStateException("No subscription products available")

            val params = PurchaseParams.Builder(activity, pkg).build()
            val result = Purchases.sharedInstance.awaitPurchase(params)
            _isPremium.value = result.customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
            if (_isPremium.value) {
                ChatUsageManager.reset()
                ScanUsageManager.reset()
                RecipeGenerationUsageManager.reset()
                SearchUsageManager.reset()
            }
            _isLoading.value = false
            _isPremium.value
        } catch (e: Exception) {
            _error.value = e.localizedMessage ?: "Purchase failed. Please try again."
            _isLoading.value = false
            false
        }
    }

    /**
     * Restore previous purchases.
     */
    suspend fun restorePurchases(): Boolean {
        _isLoading.value = true
        _error.value = null
        return try {
            val info = Purchases.sharedInstance.awaitRestore()
            _isPremium.value = info.entitlements[ENTITLEMENT_ID]?.isActive == true
            if (_isPremium.value) {
                ChatUsageManager.reset()
                ScanUsageManager.reset()
                RecipeGenerationUsageManager.reset()
                SearchUsageManager.reset()
            }
            _isLoading.value = false
            _isPremium.value
        } catch (e: Exception) {
            _error.value = e.localizedMessage ?: "Restore failed. Please try again."
            _isLoading.value = false
            false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
