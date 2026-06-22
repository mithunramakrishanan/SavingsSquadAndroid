package com.android.savingssquad.SquadSubscription

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.android.savingssquad.singleton.Plan
import kotlinx.coroutines.suspendCancellableCoroutine
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams

object BillingHelper {

    private const val BASIC_PRODUCT_ID = "squad_basic"
    private const val BUSINESS_PRODUCT_ID = "squad_business"

    private lateinit var billingClient: BillingClient

    // INIT
    fun init(context: Context) {

        billingClient = BillingClient.newBuilder(context)
            .setListener { _, _ -> }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient.startConnection(
            object : BillingClientStateListener {

                override fun onBillingSetupFinished(result: BillingResult) {
                    println("Billing setup: ${result.responseCode}")
                }

                override fun onBillingServiceDisconnected() {
                    println("Billing disconnected")
                }
            }
        )
    }

    // MARK: - GET CURRENT PLAN (iOS equivalent)
    suspend fun getCurrentPlan(): Plan {

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        return suspendCancellableCoroutine { cont ->

            billingClient.queryPurchasesAsync(params) { result, purchases ->

                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    cont.resume(Plan.FREE, null)
                    return@queryPurchasesAsync
                }

                var plan = Plan.FREE

                for (purchase in purchases) {

                    if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue

                    when {
                        purchase.products.contains(BUSINESS_PRODUCT_ID) -> {
                            cont.resume(Plan.BUSINESS, null)
                            return@queryPurchasesAsync
                        }

                        purchase.products.contains(BASIC_PRODUCT_ID) -> {
                            plan = Plan.BASIC
                        }
                    }
                }

                cont.resume(plan, null)
            }
        }
    }

    // MARK: - QUERY PRODUCT DETAILS
    fun queryProduct(
        productId: String,
        callback: (ProductDetails?) -> Unit
    ) {

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, list ->

            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                callback(list.productDetailsList.firstOrNull())
            } else {
                callback(null)
            }
        }
    }

    // MARK: - LAUNCH PURCHASE
    fun launchPurchase(
        activity: Activity,
        productId: String,
        callback: (Boolean, String?) -> Unit
    ) {

        queryProduct(productId) { productDetails ->

            if (productDetails == null) {
                callback(false, "Product not available")
                return@queryProduct
            }

            val productParams = BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(productDetails)
                .build()

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()

            val result = billingClient.launchBillingFlow(activity, flowParams)

            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                callback(false, result.debugMessage)
            } else {
                callback(true, null)
            }
        }
    }

    // MARK: - MAIN FLOW (iOS equivalent startPurchaseFlow)
    fun startPurchaseFlow(
        activity: Activity,
        squadID: String,
        selectedPlan: Plan,
        enableLoanAddon: Boolean,
        onLoading: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {

        val productId = getProductId(selectedPlan)

        if (productId.isEmpty()) {
            onError("Invalid product selection")
            return
        }

        onLoading(true)

        launchPurchase(
            activity = activity,
            productId = productId
        ) { success, error ->

            if (!success) {
                onLoading(false)
                onError(error ?: "Purchase failed")
                return@launchPurchase
            }

            // AFTER PURCHASE → UPDATE FIRESTORE (same as iOS)
            SubscriptionFirebaseManager.shared.updateSubscription(
                squadID = squadID,
                plan = selectedPlan,
                loanAddon = enableLoanAddon
            ) { updateSuccess, updateError ->

                onLoading(false)

                if (updateSuccess) {

                    SubscriptionManager.shared.loadSubscription(squadID) { _, _ -> }

                } else {
                    onError(updateError ?: "Update failed")
                }
            }
        }
    }

    // MARK: - PRODUCT MAPPING
    fun getProductId(plan: Plan): String {
        return when (plan) {
            Plan.BASIC -> BASIC_PRODUCT_ID
            Plan.BUSINESS -> BUSINESS_PRODUCT_ID
            Plan.FREE -> ""
        }
    }
}