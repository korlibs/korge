package com.soywiz.korge.admob

import com.soywiz.korio.globalDynamic
import kotlinx.coroutines.await
import kotlin.js.Promise

actual suspend fun AdmobCreate(testing: Boolean): Admob {
	val admob = globalDynamic.admob ?: return object : Admob() {}
	val admobBanner = admob.banner
	val admobInterstitial = admob.interstitial
	val admobRewardvideo = admob.rewardvideo

	return object : Admob() {
		override fun bannerPrepare(config: Config) {
			@Suppress("unused")
			admobBanner.config(config.convert())
			admobBanner.prepare()
		}

		override fun bannerShow(): Unit = run { admobBanner.show() }
		override fun bannerHide(): Unit = run { admobBanner.hide() }

		fun Config.convert(): Any {
			val config = this
			return object {
				@JsName("id") val id = config.id
				@JsName("isTesting") val isTesting = testing
				@JsName("autoShow") val autoShow: Boolean? = null
				@JsName("forChild") val forChild: Boolean? = config.forChild
				@JsName("forFamily") val forFamily: Boolean? = null
				@JsName("location") val location: Boolean? = null
				@JsName("bannerAtTop") val bannerAtTop = config.bannerAtTop
				@JsName("overlap") val overlap = config.overlap
				@JsName("offsetTopBar") val offsetTopBar = config.offsetTopBar
				@JsName("size") val size = config.size.name
				//@JsName("location") val location = if (latitude != null && longitude != null) arrayOf(latitude, longitude) else null
			}
		}

		override fun interstitialPrepare(config: Config) {
			admobInterstitial.config(config.convert())
			admobInterstitial.prepare()
		}

		override fun interstitialIsLoaded(): Boolean = admobInterstitial.isReady()

		override suspend fun interstitialShowAndWait(): Unit = run {
			awaitDynamic(admobInterstitial.show())
		}

		/////////////////

		override fun rewardvideolPrepare(config: Config) {
			admobRewardvideo.config(config.convert())
			admobRewardvideo.prepare()
		}

		override fun rewardvideolIsLoaded(): Boolean = admobRewardvideo.isReady()
		override suspend fun rewardvideoShowAndWait(): Unit = run {
			awaitDynamic(admobRewardvideo.show())
		}
	}
}

suspend fun awaitDynamic(value: dynamic): dynamic {
	if (value != null && value.then !== undefined) {
		return value.unsafeCast<Promise<*>>().await()
	}
	return value
}
