package com.soywiz.korge.billing

import com.soywiz.kds.Extra
import com.soywiz.kds.linkedHashMapOf
import com.soywiz.korge.service.ServiceBaseId
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.Signal
import kotlinx.coroutines.flow.*

abstract class InAppPurchases(val views: Views) {
    val onPurchases = Signal<List<PurchaseInfo>>()
    open suspend fun listProducts(type: ProductType, items: List<ProductId>): Flow<ProductInfo> = flow {
        println("WARNING: Not implemented InAppPurchases.listProducts($type, $items)")
    }
    open suspend fun purchase(product: ProductInfo) {
        println("WARNING: Not implemented InAppPurchases.purchase($product)")
        if (views.confirm("Perform fake purchase?")) {
            onPurchases(listOf(PurchaseInfo(productId = product.id)))
        }
    }
    open suspend fun listPurchases(type: ProductType): Flow<PurchaseInfo> = flow {
        println("WARNING: Not implemented InAppPurchases.listPurchases($type)")
    }
    open suspend fun consume(purchase: PurchaseInfo): ConsumeInfo {
        println("WARNING: Not implemented InAppPurchases.consume($purchase)")
        return ConsumeInfo("invalid")
    }
}

data class ConsumeInfo(val token: String)

data class PurchaseInfo(
    val productId: ProductId,
    val orderId: String = "",
    val token: String = "",
    val time: Long = 0L,
    val pending: Boolean? = null,
    val packageName: String = "",
    val originalJson: String = "",
    val signature: String = "",
    val developerPayload: String = "",
    val accountId: String? = null,
    val profileId: String? = null,
    val acknowledged: Boolean = false,
    val autoRenewing: Boolean = false,
) : Extra by Extra.Mixin() {
}

data class ProductInfo(
    val id: ProductId,
    val title: String,
    val description: String,
    val freeTrialPeriod: String,
    val iconUrl: String,
    val introductoryPrice: String,
    val introductoryPriceAmountMicros: Long,
    val introductoryPriceCycles: Int,
    val introductoryPricePeriod: String,
    val originalJson: String,
    val originalPrice: String,
    val originalPriceAmountMicros: Long,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val subscriptionPeriod: String,
    val type: ProductType
) : Extra by Extra.Mixin() {
}

enum class ProductType { SUBSCRIPTION, PRODUCT }

class ProductId() : ServiceBaseId()
class PurchaseId() : ServiceBaseId()

expect fun CreateInAppPurchases(views: Views): InAppPurchases
