package com.soywiz.korge.billing

import com.soywiz.kds.Extra
import com.soywiz.kds.linkedHashMapOf
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.Signal

abstract class InAppPurchases(val views: Views) {
    val onPurchases = Signal<List<PurchaseInfo>>()
    open suspend fun listProducts(type: ProductType, items: List<ProductId>): List<ProductInfo> = listOf()
    open suspend fun purchase(product: ProductInfo) = Unit
    open suspend fun listPurchases(type: ProductType): List<PurchaseInfo> = listOf()
    open suspend fun consume(purchase: PurchaseInfo): ConsumeInfo = TODO()
}

class ConsumeInfo(val token: String)

class PurchaseInfo(
    val productId: String,
    val orderId: String,
    val token: String,
    val time: Long,
    val pending: Boolean?,
    val packageName: String,
    val originalJson: String,
    val signature: String,
    val developerPayload: String,
    val accountId: String?,
    val profileId: String?,
    val acknowledged: Boolean,
    val autoRenewing: Boolean,
) : Extra by Extra.Mixin() {
}

class ProductInfo(
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

class ProductId() : BillingBaseId()
class PurchaseId() : BillingBaseId()

open class BillingBaseId() {
    private val map: LinkedHashMap<String, String> = linkedHashMapOf()
    operator fun set(platform: String, id: String) { map[platform] = id }
    operator fun get(platform: String) = map[platform] ?: error("Id not set for platform '$platform'")
    fun platform(platform: String) = this[platform]
    fun android() = platform("android")
    fun ios() = platform("ios")
}

fun <T : BillingBaseId> T.platform(platform: String, id: String): T = this.apply { this[platform] = id }
fun <T : BillingBaseId> T.android(id: String): T = platform("android", id)
fun <T : BillingBaseId> T.ios(id: String): T = platform("ios", id)


expect fun CreateInAppPurchases(views: Views): InAppPurchases
