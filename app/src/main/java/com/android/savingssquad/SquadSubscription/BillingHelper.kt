package com.android.savingssquad.SquadSubscription

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.suspendCancellableCoroutine
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import android.util.Log
import com.android.billingclient.api.PurchasesResponseListener
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("StaticFieldLeak")
object BillingHelper : PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private var currentActivity: Activity? = null

    private var pendingSquadId: String? = null
    private var pendingPlan: SubscriptionModel.Plan? = null
    private var pendingLoanAddon = false

    private val pendingProducts = mutableListOf<String>()
    private var currentPurchaseIndex = 0

    private var loadingCallback: ((Boolean) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    private var successCallback: (() -> Unit)? = null

    private var isConnected = false
    private val pendingActions = mutableListOf<() -> Unit>()
    private var isConnecting = false
    private var purchaseInProgress = false

    const val BASIC_PRODUCT_ID = "basic_monthly"
    const val BUSINESS_PRODUCT_ID = "business_monthly"
    const val LOAN_ADDON_PRODUCT_ID = "loan_addon_monthly"

    // ─────────────────────────────────────────────
    // INIT & CONNECTION
    // ─────────────────────────────────────────────

    fun init(context: Context) {
        if (billingClient != null) return

        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .build()

        connectBillingClient()
    }

    private fun connectBillingClient(onConnected: (() -> Unit)? = null) {
        val client = billingClient ?: return

        if (isConnected) {
            onConnected?.invoke()
            return
        }

        onConnected?.let { pendingActions.add(it) }

        // Already attempting connection — don't start a second one
        if (isConnecting) return
        isConnecting = true

        client.startConnection(object : BillingClientStateListener {

            override fun onBillingSetupFinished(result: BillingResult) {
                isConnecting = false

                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingHelper", "Billing connected")
                    isConnected = true

                    val actions = pendingActions.toList()
                    pendingActions.clear()
                    actions.forEach { it.invoke() }

                } else {
                    isConnected = false
                    pendingActions.clear()

                    val message = result.debugMessage.ifEmpty {
                        "Unable to connect to Google Play Billing."
                    }

                    Log.e("BillingHelper", message)
                    loadingCallback?.invoke(false)
                    errorCallback?.invoke(message)
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
                isConnecting = false
                Log.w("BillingHelper", "Billing service disconnected")
                // Will reconnect automatically on next ensureConnectedThen() call
            }
        })
    }

    private fun ensureConnectedThen(action: () -> Unit) {
        if (isConnected) {
            action()
        } else {
            connectBillingClient(onConnected = action)
        }
    }

    // ─────────────────────────────────────────────
    // PUBLIC: START PURCHASE FLOW
    // ─────────────────────────────────────────────

    fun startPurchaseFlow(
        activity: Activity,
        squadID: String,
        selectedPlan: SubscriptionModel.Plan,
        enableLoanAddon: Boolean,
        onLoading: (Boolean) -> Unit,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        val productIds = buildProductList(
            selectedPlan = selectedPlan,
            enableLoanAddon = enableLoanAddon
        )

        if (productIds.isEmpty()) {
            onError("Invalid product selection.")
            return
        }

        if (purchaseInProgress) {
            onError("A purchase is already in progress.")
            return
        }

        purchaseInProgress = true

        currentActivity = activity
        pendingSquadId = squadID
        pendingPlan = selectedPlan
        pendingLoanAddon = enableLoanAddon

        pendingProducts.clear()
        pendingProducts.addAll(productIds)
        currentPurchaseIndex = 0

        loadingCallback = onLoading
        errorCallback = onError
        successCallback = onSuccess

        onLoading(true)

        ensureConnectedThen { launchNextPurchase() }
    }

    // ─────────────────────────────────────────────
    // PURCHASE FLOW
    // ─────────────────────────────────────────────

    private fun buildProductList(selectedPlan: SubscriptionModel.Plan, enableLoanAddon: Boolean): List<String> {
        return when (selectedPlan) {
            SubscriptionModel.Plan.BASIC -> buildList {
                add(BASIC_PRODUCT_ID)
                if (enableLoanAddon) add(LOAN_ADDON_PRODUCT_ID)
            }
            SubscriptionModel.Plan.BUSINESS -> listOf(BUSINESS_PRODUCT_ID)
            SubscriptionModel.Plan.FREE -> emptyList()
        }
    }

    private fun launchNextPurchase() {
        val activity = currentActivity

        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            failWith("Activity is no longer available.")
            return
        }

        if (currentPurchaseIndex >= pendingProducts.size) {
            updateFirestore()
            return
        }

        val client = billingClient ?: run {
            failWith("Billing client is not initialized.")
            return
        }

        val productId = pendingProducts[currentPurchaseIndex]

        queryProduct(productId) { productDetails ->

            if (productDetails == null) {
                failWith("Product not found: $productId")
                return@queryProduct
            }

            val offer = productDetails.subscriptionOfferDetails
                ?.minByOrNull {
                    it.pricingPhases.pricingPhaseList
                        .firstOrNull()
                        ?.priceAmountMicros ?: Long.MAX_VALUE
                }

            if (offer == null) {
                failWith("No offer available for: $productId")
                return@queryProduct
            }

            val productParams = BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offer.offerToken)
                .build()

            val billingParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()

            val result = client.launchBillingFlow(activity, billingParams)

            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                failWith(
                    result.debugMessage.ifEmpty { "Unable to launch purchase." }
                )
            }
        }
    }

    // ─────────────────────────────────────────────
    // PURCHASE UPDATES
    // ─────────────────────────────────────────────

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {

            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    failWith("Purchase completed but no purchases returned.")
                    return
                }

                val expectedProductId = pendingProducts.getOrNull(currentPurchaseIndex)
                val matchedPurchase = purchases.firstOrNull { purchase ->
                    expectedProductId != null && purchase.products.contains(expectedProductId)
                }

                if (matchedPurchase == null) {
                    failWith("Purchased product does not match the expected subscription.")
                    return
                }

                handlePurchase(matchedPurchase)
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                failWith("Purchase cancelled.")
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                val productId = pendingProducts.getOrNull(currentPurchaseIndex) ?: run {
                    failWith("No pending product to verify.")
                    return
                }

                billingClient?.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                ) { result, ownedPurchases ->

                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        failWith("Unable to verify existing purchase.")
                        return@queryPurchasesAsync
                    }

                    val ownedPurchase = ownedPurchases.firstOrNull {
                        it.products.contains(productId)
                    }

                    if (ownedPurchase == null) {
                        failWith("Owned subscription not found.")
                        return@queryPurchasesAsync
                    }

                    if (ownedPurchase.isAcknowledged) {
                        onPurchaseAcknowledged()
                    } else {
                        acknowledgePurchase(ownedPurchase)
                    }

                } ?: failWith("Billing client not initialized.")
            }

            else -> {
                failWith(
                    billingResult.debugMessage.ifEmpty {
                        "Purchase failed (code: ${billingResult.responseCode})."
                    }
                )
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> acknowledgePurchase(purchase)
            Purchase.PurchaseState.PENDING -> failWith("Purchase is pending approval. It will activate once payment is confirmed.")
            else -> failWith("Unexpected purchase state.")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) {
            onPurchaseAcknowledged()
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                onPurchaseAcknowledged()
            } else {
                failWith(result.debugMessage.ifEmpty { "Failed to acknowledge purchase." })
            }
        } ?: failWith("Billing client not initialized.")
    }

    private fun onPurchaseAcknowledged() {
        currentPurchaseIndex++
        if (currentPurchaseIndex < pendingProducts.size) {
            launchNextPurchase()
        } else {
            updateFirestore()
        }
    }

    // ─────────────────────────────────────────────
    // FIRESTORE UPDATE
    // ─────────────────────────────────────────────

    private fun updateFirestore() {
        val squadId = pendingSquadId
        val plan = pendingPlan

        if (squadId == null || plan == null) {
            failWith("Purchase info missing — cannot update subscription.")
            return
        }

        SubscriptionFirebaseManager.shared.updateSubscription(
            squadID = squadId,
            plan = plan,
            loanAddon = pendingLoanAddon
        ) { success, error ->

            if (!success) {
                failWith(error ?: "Unable to update subscription.")
                return@updateSubscription
            }

            SubscriptionManager.shared.loadSubscription(squadId) { _, loadError ->
                loadingCallback?.invoke(false)

                if (loadError != null) {
                    errorCallback?.invoke(loadError)  // loadError is already a String
                } else {
                    successCallback?.invoke()
                }

                clearPurchaseState()
            }
        }
    }

    // ─────────────────────────────────────────────
    // QUERY PRODUCT DETAILS
    // ─────────────────────────────────────────────

    fun queryProduct(productId: String, callback: (ProductDetails?) -> Unit) {
        val client = billingClient ?: run {
            callback(null)
            return
        }

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        ensureConnectedThen {
            client.queryProductDetailsAsync(params) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    callback(list.firstOrNull())
                } else {
                    Log.e("BillingHelper", "queryProduct failed: ${result.debugMessage}")
                    callback(null)
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // GET CURRENT PLAN
    // ─────────────────────────────────────────────

    suspend fun getCurrentPlan(): SubscriptionModel.Plan {

        val client = billingClient ?: return SubscriptionModel.Plan.FREE

        return withTimeoutOrNull(5000.milliseconds) { // 🔥 prevents infinite hang

            suspendCancellableCoroutine { cont ->

                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()

                val listener = PurchasesResponseListener { result, purchases ->

                    if (!cont.isActive) return@PurchasesResponseListener

                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        cont.resume(SubscriptionModel.Plan.FREE)
                        return@PurchasesResponseListener
                    }

                    var plan = SubscriptionModel.Plan.FREE

                    for (purchase in purchases) {

                        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED)
                            continue

                        when {
                            purchase.products.contains(BUSINESS_PRODUCT_ID) -> {
                                cont.resume(SubscriptionModel.Plan.BUSINESS)
                                return@PurchasesResponseListener
                            }

                            purchase.products.contains(BASIC_PRODUCT_ID) -> {
                                plan = SubscriptionModel.Plan.BASIC
                            }
                        }
                    }

                    cont.resume(plan)
                }

                try {
                    client.queryPurchasesAsync(params, listener)
                } catch (e: Exception) {
                    if (cont.isActive) {
                        cont.resume(SubscriptionModel.Plan.FREE)
                    }
                }

                cont.invokeOnCancellation {
                    // no-op (safe cleanup if needed)
                }
            }

        } ?: SubscriptionModel.Plan.FREE
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    fun getProductId(plan: SubscriptionModel.Plan): String {
        return when (plan) {
            SubscriptionModel.Plan.BASIC -> BASIC_PRODUCT_ID
            SubscriptionModel.Plan.BUSINESS -> BUSINESS_PRODUCT_ID
            SubscriptionModel.Plan.FREE -> ""
        }
    }

    private fun failWith(message: String) {
        Log.e("BillingHelper", "Error: $message")
        loadingCallback?.invoke(false)
        errorCallback?.invoke(message)
        clearPurchaseState()
    }

    private fun clearPurchaseState() {
        currentActivity = null
        pendingSquadId = null
        pendingPlan = null
        pendingLoanAddon = false
        pendingProducts.clear()
        currentPurchaseIndex = 0
        loadingCallback = null
        errorCallback = null
        successCallback = null
        purchaseInProgress = false
        pendingActions.clear()
    }
}