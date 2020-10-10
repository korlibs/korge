package com.soywiz.korge.admob

import com.soywiz.klock.milliseconds
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.delay
import kotlin.jvm.JvmOverloads

abstract class Admob(val views: Views) {
	enum class Size {
		BANNER, IAB_BANNER, IAB_LEADERBOARD, IAB_MRECT, LARGE_BANNER, SMART_BANNER, FLUID
	}

	data class Reward(val type: String, val amount: Int)
	data class Config @JvmOverloads constructor(
		val id: String,
		val userId: String? = null,
		val size: Size = Size.SMART_BANNER,
		val bannerAtTop: Boolean = false,
		val overlap: Boolean = true,
		val offsetTopBar: Boolean = false,
		val forChild: Boolean? = null,
		val keywords: List<String>? = null,
		val immersiveMode: Boolean? = null
	)

	open suspend fun available() = false

	open suspend fun bannerPrepare(config: Config) = Unit
	open suspend fun bannerShow() = Unit
	open suspend fun bannerHide() = Unit

	open suspend fun interstitialPrepare(config: Config) = Unit
	open suspend fun interstitialIsLoaded(): Boolean = false
	open suspend fun interstitialShowAndWait() = Unit

	open suspend fun rewardvideolPrepare(config: Config) = Unit
	open suspend fun rewardvideolIsLoaded(): Boolean = false
	open suspend fun rewardvideoShowAndWait() = Unit

	suspend fun interstitialWaitLoaded() {
		while (!interstitialIsLoaded()) delay(100.milliseconds)
	}

	suspend fun rewardvideolWaitLoaded() {
		while (!rewardvideolIsLoaded()) delay(100.milliseconds)
	}

	// Utility methods

	suspend fun bannerPrepareAndShow(config: Config) {
		bannerPrepare(config)
		bannerShow()
	}

	suspend fun interstitialWaitAndShow(config: Config) {
		interstitialPrepare(config)
		interstitialWaitLoaded()
		interstitialShowAndWait()
	}

	suspend fun rewardvideolWaitAndShow(config: Config) {
		rewardvideolPrepare(config)
		rewardvideolWaitLoaded()
		rewardvideoShowAndWait()
	}
}

expect suspend fun AdmobCreate(views: Views, testing: Boolean): Admob
