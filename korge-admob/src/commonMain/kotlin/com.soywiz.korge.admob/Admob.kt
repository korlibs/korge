package com.soywiz.korge.admob

import com.soywiz.klock.milliseconds
import com.soywiz.korio.async.delay
import kotlin.jvm.JvmOverloads

abstract class Admob {
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

	open fun bannerPrepare(config: Config) = Unit
	open fun bannerShow() = Unit
	open fun bannerHide() = Unit

	open fun interstitialPrepare(config: Config) = Unit
	open fun interstitialIsLoaded(): Boolean = false
	open suspend fun interstitialShowAndWait() = Unit

	open fun rewardvideolPrepare(config: Config) = Unit
	open fun rewardvideolIsLoaded(): Boolean = false
	open suspend fun rewardvideoShowAndWait() = Unit

	suspend fun interstitialWaitLoaded() = run { while (!interstitialIsLoaded()) delay(100.milliseconds) }
	suspend fun rewardvideolWaitLoaded() = run { while (!rewardvideolIsLoaded()) delay(100.milliseconds) }

	// Utility methods

	fun bannerPrepareAndShow(config: Config) {
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

expect suspend fun AdmobCreate(testing: Boolean): Admob
