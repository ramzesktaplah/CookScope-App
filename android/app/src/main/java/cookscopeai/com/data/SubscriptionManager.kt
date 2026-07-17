package cookscopeai.com.data

import android.app.Activity
import android.app.Application
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
 */
object SubscriptionManager {

    private const val ENTITLEMENT_ID = "CookScope Premium"
    private const val PACKAGE_MONTHLY = "\$rc_monthly"
    private const val PACKAGE_ANNUAL = "\$rc_annual"
    private const val TEST_API_KEY = "test_uIBjzsLjaoFRrmJfTSYDBYruVHs"
    private const val ANDROID_API_KEY = "goog_fQllvdZiGuLYdIzNbYRNvXGwuzZ"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _monthlyPrice = MutableStateFlow<String?>(null)
    val monthlyPrice: StateFlow<String?> = _monthlyPrice.asStateFlow()

    private val _yearlyPrice = MutableStateFlow<String?>(null)
    val yearlyPrice: StateFlow<String?> = _yearlyPrice.asStateFlow()

    /**
     * Initialize RevenueCat SDK. Call from Activity.onCreate.
     * Uses Test Store key for debug builds, Play Store key for release.
     */
    fun initialize(app: Application, isDebug: Boolean) {
        val apiKey = if (isDebug) TEST_API_KEY else ANDROID_API_KEY
        Purchases.configure(
            PurchasesConfiguration.Builder(app, apiKey)
                .purchasesAreCompletedBy(PurchasesAreCompletedBy.REVENUECAT)
                .appUserID(null)
                .diagnosticsEnabled(true)
                .build()
        )
        refreshEntitlementStatus()
        fetchPrices()
    }

    /**
     * Check current entitlement status from RevenueCat.
     */
    fun refreshEntitlementStatus() {
        scope.launch {
            try {
                val info = Purchases.sharedInstance.awaitCustomerInfo()
                _isPremium.value = info.entitlements[ENTITLEMENT_ID]?.isActive == true
            } catch (_: Exception) {
                // Silently fail — user might be offline
            }
        }
    }

    /**
     * Fetch formatted prices from RevenueCat offerings.
     */
    private fun fetchPrices() {
        scope.launch {
            try {
                val offerings = Purchases.sharedInstance.awaitOfferings()
                _monthlyPrice.value = offerings.current
                    ?.getPackage(PACKAGE_MONTHLY)?.product?.price?.formatted
                _yearlyPrice.value = offerings.current
                    ?.getPackage(PACKAGE_ANNUAL)?.product?.price?.formatted
            } catch (_: Exception) {
                // Fallback to hardcoded display prices
            }
        }
    }

    /**
     * Purchase a subscription by package type.
     */
    suspend fun purchaseSubscription(activity: Activity, isAnnual: Boolean): Boolean {
        _isLoading.value = true
        _error.value = null
        return try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val packageKey = if (isAnnual) PACKAGE_ANNUAL else PACKAGE_MONTHLY
            val pkg = offerings.current?.getPackage(packageKey)
                ?: offerings.current?.availablePackages?.firstOrNull()
                ?: throw IllegalStateException("No packages available")

            val params = PurchaseParams.Builder(activity, pkg).build()
            val result = Purchases.sharedInstance.awaitPurchase(params)
            _isPremium.value = result.customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
            _isLoading.value = false
            _isPremium.value
        } catch (e: Exception) {
            _error.value = e.message ?: "Purchase failed"
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
            _isLoading.value = false
            _isPremium.value
        } catch (e: Exception) {
            _error.value = e.message ?: "Restore failed"
            _isLoading.value = false
            false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
