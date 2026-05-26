package com.livescore.football.livescores.footballscores.data.local

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val limitManager: RequestLimitManager
) : PurchasesUpdatedListener, BillingClientStateListener {

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    private val _productDetailsList = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsList: StateFlow<List<ProductDetails>> = _productDetailsList.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_MONTHLY = "premium_monthly"
        const val PRODUCT_YEARLY = "premium_yearly"
    }

    init {
        startConnection()
    }

    fun startConnection() {
        try {
            billingClient.startConnection(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting billing client connection", e)
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.d(TAG, "Billing service disconnected. Reconnecting...")
        startConnection()
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Log.d(TAG, "Billing setup finished: Code = ${billingResult.responseCode}, Msg = ${billingResult.debugMessage}")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            queryProductDetails()
            queryPurchases()
        }
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            Log.d(TAG, "Query product details finished: Code = ${billingResult.responseCode}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetailsList.value = queryProductDetailsResult.productDetailsList ?: emptyList()
            }
        }
    }

    fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            Log.d(TAG, "Query purchases finished: Code = ${billingResult.responseCode}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var hasActiveSubscription = false
                for (purchase in purchasesList) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        hasActiveSubscription = true
                        handlePurchase(purchase)
                    }
                }
                
                // Only reset premium state if the user was on active Play Billing premium
                // (Avoiding resetting in case they are manually testing with local mock switch)
                if (hasActiveSubscription) {
                    limitManager.setPremium(true)
                }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val offerDetails = productDetails.subscriptionOfferDetails?.firstOrNull()
        val offerToken = offerDetails?.offerToken ?: ""
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Log.d(TAG, "Purchases updated: Code = ${billingResult.responseCode}")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
            limitManager.setPremium(true)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User cancelled purchase flow.")
        } else {
            Log.e(TAG, "Error in purchase flow: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                Log.d(TAG, "Purchase acknowledgement finished: Code = ${billingResult.responseCode}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    coroutineScope.launch {
                        limitManager.setPremium(true)
                    }
                }
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            limitManager.setPremium(true)
        }
    }
}
