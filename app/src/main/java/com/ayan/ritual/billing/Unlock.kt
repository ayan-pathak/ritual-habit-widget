package com.ayan.ritual.billing

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * The lifetime unlock: one ritual is free, the rest are paid for once.
 *
 * The wall sits on *creating* a second ritual and nowhere else. Marking a day,
 * the widget, the archive and the share card keep working forever, for
 * everyone, on whatever rituals they already have — a refunded or unverifiable
 * purchase must never leave someone unable to fill today's square, and must
 * never hide a year they already earned. That is why [canCreate] is the only
 * question this object answers.
 *
 * There is no server behind Ritual, so this is a one-time purchase rather than
 * a subscription: nothing recurring to justify, nothing to cancel. Play is the
 * source of truth; the flag cached in prefs only keeps a cold start honest
 * while the connection comes up.
 */
object Unlock {

    const val PRODUCT_ID = "ritual_unlimited"

    /** How many rituals someone can keep before the wall. */
    const val FREE_LIMIT = 1

    private const val PREFS = "ritual_billing"
    private const val KEY_UNLOCKED = "unlocked_v1"

    private val _unlocked = mutableStateOf(false)
    val unlocked: Boolean get() = _unlocked.value

    /** Compose reads this to recompose when a purchase lands. */
    val unlockedState get() = _unlocked

    private val _price = mutableStateOf<String?>(null)
    val price: String? get() = _price.value

    private var client: BillingClient? = null
    private var details: ProductDetails? = null
    private var started = false
    private var appContext: Context? = null

    /** True when another ritual can be created without paying. */
    fun canCreate(existing: Int): Boolean = unlocked || existing < FREE_LIMIT

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Connects to Play. Safe to call on every launch; the first call wins.
     * Everything it learns is optional — with no connection the app simply
     * behaves as it did before there was a paywall, minus the second ritual.
     */
    fun start(context: Context) {
        val app = context.applicationContext
        appContext = app
        if (!started) {
            _unlocked.value = prefs(app).getBoolean(KEY_UNLOCKED, false)
            started = true
        }
        if (client?.isReady == true) {
            refresh()
            return
        }

        val listener = PurchasesUpdatedListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                purchases.forEach { record(app, it) }
            }
        }

        val billing = BillingClient.newBuilder(app)
            .setListener(listener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        client = billing

        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                loadProduct()
                refresh()
            }

            override fun onBillingServiceDisconnected() {
                // Play will be back. Nothing here is load-bearing enough to retry
                // in a loop; the next launch reconnects.
            }
        })
    }

    private fun loadProduct() {
        val billing = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        billing.queryProductDetailsAsync(params) { result, products ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            details = products.firstOrNull { it.productId == PRODUCT_ID }
            _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    /** Re-asks Play what this account owns. */
    fun refresh() {
        val billing = client ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billing.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val owned = purchases.any {
                it.products.contains(PRODUCT_ID) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (owned) {
                purchases.forEach { purchase ->
                    if (purchase.products.contains(PRODUCT_ID)) acknowledge(purchase)
                }
            }
            _unlocked.value = owned
            cache(owned)
        }
    }

    private fun cache(owned: Boolean) {
        appContext?.let { prefs(it).edit().putBoolean(KEY_UNLOCKED, owned).apply() }
    }

    private fun record(context: Context, purchase: Purchase) {
        if (!purchase.products.contains(PRODUCT_ID)) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        acknowledge(purchase)
        _unlocked.value = true
        prefs(context).edit().putBoolean(KEY_UNLOCKED, true).apply()
    }

    /** Play refunds an unacknowledged purchase after three days. */
    private fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val billing = client ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billing.acknowledgePurchase(params) { }
    }

    /** Opens Play's sheet. Returns false when the product isn't loaded yet. */
    fun purchase(activity: Activity): Boolean {
        val billing = client ?: return false
        val product = details ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .build()
                )
            )
            .build()
        val result = billing.launchBillingFlow(activity, params)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }
}
