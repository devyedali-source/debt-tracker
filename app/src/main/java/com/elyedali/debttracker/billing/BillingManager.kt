package com.elyedali.debttracker.billing

import android.app.Activity
import android.content.Context
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
import com.elyedali.debttracker.BuildConfig
import com.elyedali.debttracker.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Google Play monthly subscription "premium_monthly".
 * Everything is checked on the phone through Google Play: no server needed.
 */
class BillingManager(context: Context, private val prefs: Prefs) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID = "premium_monthly"
    }

    private val _isPremium = MutableStateFlow(prefs.premiumCached || debugOverride())
    val isPremium: StateFlow<Boolean> = _isPremium

    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price

    @Volatile
    private var details: ProductDetails? = null

    @Volatile
    private var connecting = false

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private fun debugOverride(): Boolean = BuildConfig.DEBUG && prefs.debugPremium

    fun start() {
        if (client.isReady) {
            queryProduct()
            refresh()
            return
        }
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    refresh()
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
            }
        })
    }

    /** Re-reads the active subscription from Google Play. */
    fun refresh() {
        if (!client.isReady) {
            start()
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                setPremium(purchases.any { isActive(it) })
                purchases.forEach { acknowledge(it) }
            }
        }
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        client.queryProductDetailsAsync(params) { result, productResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val d = productResult.productDetailsList.firstOrNull()
                details = d
                _price.value = d?.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.lastOrNull()
                    ?.formattedPrice
            }
        }
    }

    /** Opens the Google Play purchase sheet. Returns false if not possible right now. */
    fun launch(activity: Activity): Boolean {
        val d = details
        if (d == null) {
            start()
            return false
        }
        val offer = d.subscriptionOfferDetails?.firstOrNull() ?: return false
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(d)
            .setOfferToken(offer.offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        return client.launchBillingFlow(activity, flowParams).responseCode ==
            BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    if (purchases.any { isActive(it) }) setPremium(true)
                    purchases.forEach { acknowledge(it) }
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> Unit
        }
    }

    private fun isActive(p: Purchase): Boolean =
        p.purchaseState == Purchase.PurchaseState.PURCHASED && p.products.contains(PRODUCT_ID)

    private fun acknowledge(p: Purchase) {
        if (isActive(p) && !p.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
            client.acknowledgePurchase(params) { }
        }
    }

    private fun setPremium(active: Boolean) {
        prefs.premiumCached = active
        _isPremium.value = active || debugOverride()
    }

    fun setDebugPremium(on: Boolean) {
        if (!BuildConfig.DEBUG) return
        prefs.debugPremium = on
        _isPremium.value = prefs.premiumCached || on
    }
}
