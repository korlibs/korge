package com.soywiz.korge.billing

import android.app.Activity
import com.android.billingclient.api.*
import com.google.android.gms.tasks.Task
import com.soywiz.kds.extraProperty
import com.soywiz.korge.service.android
import com.soywiz.korge.view.Views
import com.soywiz.korio.android.androidContext
import com.soywiz.korio.async.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.*

actual fun CreateInAppPurchases(views: Views): InAppPurchases = object : InAppPurchases(views) {
    val context get() = views.coroutineContext.androidContext()
    val activity get() = (context as? Activity?) ?: error("Android context is not an activity ($context)")

    val billingClient by lazy { BillingClient.newBuilder(context)
        .setListener { billingResult, purchases ->
            if (purchases != null) {
                onPurchases(purchases.map { it.toInfo() })
            }
        }
        .enablePendingPurchases()
        .build()
    }
    private var connectedJob: Job? = null

    suspend fun connectOnce() {
        if (connectedJob == null) {
            connectedJob = launch(coroutineContext) {
                val deferred = CompletableDeferred<Unit>()
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            // The BillingClient is ready. You can query purchases here.
                            deferred.complete(Unit)
                        } else {
                            deferred.completeExceptionally(billingResult.toException())
                        }
                    }
                    override fun onBillingServiceDisconnected() {
                        // Try to restart the connection on the next request to
                        // Google Play by calling the startConnection() method.
                        deferred.completeExceptionally(Exception("Billing disconnected"))
                    }
                })
                deferred.await()
            }
        }
        connectedJob?.join()
    }

    override suspend fun consume(purchase: PurchaseInfo): ConsumeInfo {
        connectOnce()
        val deferred = CompletableDeferred<String>()
        billingClient.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchase.token).build()) { billingResult, outToken ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                deferred.complete(outToken)
            } else {
                deferred.completeExceptionally(billingResult.toException())
            }
        }
        return ConsumeInfo(deferred.await())
    }

    override suspend fun listProducts(type: ProductType, items: List<ProductId>): Flow<ProductInfo> {
        connectOnce()
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(items.map { it.android() })
            .setType(type.toSkuType())
            .build()
        val deferred = CompletableDeferred<List<ProductInfo>>()
        billingClient.querySkuDetailsAsync(params) { billingResult, details ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                deferred.complete((details ?: listOf()).map { it.toInfo() })
            } else {
                deferred.completeExceptionally(billingResult.toException())
            }
        }
        return deferred.await().asFlow()
    }

    override suspend fun purchase(product: ProductInfo) {
        connectOnce()
        val responseCode = billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder()
            .setSkuDetails(product.skuDetails!!)
            .build()).responseCode
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            error("Error requesting purchasing product")
        }
    }

    override suspend fun listPurchases(type: ProductType): Flow<PurchaseInfo> {
        connectOnce()
        val query = billingClient.queryPurchases(type.toSkuType())
        if (query.responseCode != BillingClient.BillingResponseCode.OK) {
            error("Error consuming: ${query.responseCode} : ${query.billingResult.responseCode}, ${query.billingResult.debugMessage}")
        }
        return (query.purchasesList?.map { it.toInfo() } ?: listOf()).asFlow()
    }

    fun BillingResult.toException() = Exception("Error consuming: $responseCode : $debugMessage")

    fun ProductType.toSkuType() = when (this) {
        ProductType.PRODUCT -> BillingClient.SkuType.INAPP
        ProductType.SUBSCRIPTION -> BillingClient.SkuType.SUBS
    }

    fun Purchase.toInfo() = PurchaseInfo(
        productId = ProductId().android(this.sku),
        orderId = this.orderId,
        token = this.purchaseToken,
        time = this.purchaseTime,
        pending = when (this.purchaseState) {
            Purchase.PurchaseState.PENDING -> true
            Purchase.PurchaseState.PURCHASED -> false
            else -> null
        },
        packageName = this.packageName,
        originalJson = this.originalJson,
        signature = this.signature,
        developerPayload = this.developerPayload,
        accountId = this.accountIdentifiers?.obfuscatedAccountId,
        profileId = this.accountIdentifiers?.obfuscatedProfileId,
        acknowledged = this.isAcknowledged,
        autoRenewing = this.isAutoRenewing,
    ).also {
        it.purchase = this
    }

    fun SkuDetails.toInfo() = ProductInfo(
        id = ProductId().android(this.sku),
        title = this.title,
        description = this.description,
        freeTrialPeriod = this.freeTrialPeriod,
        iconUrl = this.iconUrl,
        introductoryPrice = this.introductoryPrice,
        introductoryPriceAmountMicros = this.introductoryPriceAmountMicros,
        introductoryPriceCycles = this.introductoryPriceCycles,
        introductoryPricePeriod = this.introductoryPricePeriod,
        originalJson = this.originalJson,

        originalPrice = this.originalPrice,
        originalPriceAmountMicros = this.originalPriceAmountMicros,

        price = this.price,
        priceAmountMicros = this.priceAmountMicros,

        priceCurrencyCode = this.priceCurrencyCode,
        subscriptionPeriod = this.subscriptionPeriod,
        type = when (this.type) {
            BillingClient.SkuType.SUBS -> ProductType.SUBSCRIPTION
            BillingClient.SkuType.INAPP -> ProductType.PRODUCT
            else -> ProductType.PRODUCT
        },

    ).also {
        it.skuDetails = this
    }
}

var PurchaseInfo.purchase: Purchase? by extraProperty { null }
var ProductInfo.skuDetails: SkuDetails? by extraProperty { null }

private suspend fun <T> Task<T>.await(): T {
    val deferred = CompletableDeferred<T>()
    this.addOnCanceledListener { deferred.cancel() }
    this.addOnFailureListener { deferred.completeExceptionally(it) }
    this.addOnSuccessListener { deferred.complete(it) }
    return deferred.await()
}
