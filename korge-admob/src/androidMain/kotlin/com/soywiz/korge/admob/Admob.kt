package com.soywiz.korge.admob

import android.app.*
import android.view.*
import android.widget.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.reward.*
import com.soywiz.korio.android.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.close
import com.soywiz.korge.view.Views
import kotlinx.coroutines.*


actual suspend fun AdmobCreate(views: Views, testing: Boolean): Admob {
	val activity = androidContext() as Activity
	return AndroidAdmob(views, activity, testing)
}

private class AndroidAdmob(views: Views, val activity: Activity, val testing: Boolean) : Admob(views) {
	val initialRootView = activity.window.decorView.findViewById<android.view.View>(android.R.id.content).let {
		if (it is FrameLayout) it.getChildAt(0) else it
	}
	// Let's convert the rootView into a suitable format
	val rootView = if (initialRootView !is RelativeLayout) {
		//if (rootView !is LinearLayout) {
		RelativeLayout(activity).apply {
			initialRootView.removeFromParent()
			addView(
				initialRootView,
				RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
			)
			activity.setContentView(this)
		}
	} else {
		initialRootView
	}

	class RewardedVideoAdSignals {
		val onRewardedVideoAdClosed = Signal<Unit>()
		val onRewardedVideoAdLeftApplication = Signal<Unit>()
		val onRewardedVideoAdLoaded = Signal<Unit>()
		val onRewardedVideoAdOpened = Signal<Unit>()
		val onRewardedVideoCompleted = Signal<Unit>()
		val onRewarded = Signal<Reward>()
		val onRewardedVideoStarted = Signal<Unit>()
		val onRewardedVideoAdFailedToLoad = Signal<Int>()

		val listener = object : RewardedVideoAdListener {
			override fun onRewardedVideoAdClosed() = onRewardedVideoAdClosed(Unit)
			override fun onRewardedVideoAdLeftApplication() = onRewardedVideoAdLeftApplication(Unit)
			override fun onRewardedVideoAdLoaded() = onRewardedVideoAdLoaded(Unit)
			override fun onRewardedVideoAdOpened() = onRewardedVideoAdOpened(Unit)
			override fun onRewardedVideoCompleted() = onRewardedVideoCompleted(Unit)
			override fun onRewarded(rewardItem: RewardItem) = onRewarded.invoke(rewardItem.let { Admob.Reward(it.type, it.amount) })
			override fun onRewardedVideoStarted() = onRewardedVideoStarted(Unit)
			override fun onRewardedVideoAdFailedToLoad(p0: Int) = onRewardedVideoAdFailedToLoad.invoke(p0)
		}
	}


	class AdListenerSignals {
		val onInterstitialAdImpression = Signal<Unit>()
		val onInterstitialAdLeftApplication = Signal<Unit>()
		val onAdClicked = Signal<Unit>()
		val onAdFailedToLoad = Signal<Int>()
		val onAdClosed = Signal<Unit>()
		val onAdOpened = Signal<Unit>()
		val onAdLoaded = Signal<Unit>()

		val listener = object : AdListener() {
			override fun onAdImpression() = onInterstitialAdImpression.invoke(Unit)
			override fun onAdLeftApplication() = onInterstitialAdLeftApplication.invoke(Unit)
			override fun onAdClicked() = onAdClicked.invoke(Unit)
			override fun onAdFailedToLoad(p0: Int) = onAdFailedToLoad.invoke(p0)
			override fun onAdClosed() = onAdClosed.invoke(Unit)
			override fun onAdOpened() = onAdOpened.invoke(Unit)
			override fun onAdLoaded() = onAdLoaded.invoke(Unit)
		}
	}


	val interstitialSignals by lazy { AdListenerSignals() }
	val interstitial by lazy { InterstitialAd(activity).apply {
		adListener = interstitialSignals.listener
	} }

	val rewardVideoSignals by lazy { RewardedVideoAdSignals() }
	val rewardVideo by lazy { MobileAds.getRewardedVideoAdInstance(activity).apply {
		rewardedVideoAdListener = rewardVideoSignals.listener
	} }
	private val adViewSignals by lazy { AdListenerSignals() }
	private var adView = AdView(activity).apply {
		adListener = adViewSignals.listener
	}
	private var bannerAtTop = false

	fun Admob.Config.toAdRequest(): AdRequest {
		val builder = AdRequest.Builder()
		if (this.keywords != null) {
			for (keyword in keywords) builder.addKeyword(keyword)
		}
		if (this.forChild != null) builder.tagForChildDirectedTreatment(forChild)
		return builder.build()
	}

	override suspend fun available() = true

	override suspend fun bannerPrepare(config: Admob.Config) {
		this.bannerAtTop = config.bannerAtTop
		activity.runOnUiThread {
			adView.adUnitId = if (testing) "ca-app-pub-3940256099942544/6300978111" else config.id
			adView.adSize = when (config.size) {
				Size.BANNER -> AdSize.BANNER
				Size.IAB_BANNER -> AdSize.BANNER
				Size.IAB_LEADERBOARD -> AdSize.BANNER
				Size.IAB_MRECT -> AdSize.BANNER
				Size.LARGE_BANNER -> AdSize.LARGE_BANNER
				Size.SMART_BANNER -> AdSize.SMART_BANNER
				Size.FLUID -> AdSize.FLUID
			}
		}
		activity.runOnUiThread {
			adView.loadAd(config.toAdRequest())
		}
	}

	override suspend fun bannerShow() {
		activity.runOnUiThread {
			if (adView.parent != rootView) {
				adView.removeFromParent()
				rootView.addView(
					adView,
					RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
						addRule(RelativeLayout.CENTER_HORIZONTAL)
						addRule(if (bannerAtTop) RelativeLayout.ALIGN_PARENT_TOP else RelativeLayout.ALIGN_PARENT_BOTTOM)
					}
				)
			}
			adView.visibility = View.VISIBLE
		}
		//activity.view
	}

	override suspend fun bannerHide() {
		activity.runOnUiThread {
			adView.visibility = View.INVISIBLE
		}
	}

	override suspend fun interstitialPrepare(config: Admob.Config) {
		activity.runOnUiThread {
			interstitial.adUnitId = if (testing) "ca-app-pub-3940256099942544/1033173712" else config.id
			interstitial.loadAd(config.toAdRequest())
		}
	}

	override suspend fun interstitialIsLoaded(): Boolean = interstitial.isLoaded

	override suspend fun interstitialShowAndWait() {
		val result = mapOf(interstitialSignals.onAdClosed to "closed", interstitialSignals.onAdClicked to "clicked").executeAndWaitAnySignal {
			activity.runOnUiThread {
				interstitial.show()
			}
		}
	}

	override suspend fun rewardvideolPrepare(config: Admob.Config) {
		activity.runOnUiThread {
			rewardVideo.userId = config.userId
			if (config.immersiveMode != null) {
				rewardVideo.setImmersiveMode(config.immersiveMode)
			}
			rewardVideo.loadAd(
				if (testing) "ca-app-pub-3940256099942544/5224354917" else config.id,
				config.toAdRequest()
			)
		}
	}

	override suspend fun rewardvideolIsLoaded(): Boolean = rewardVideo.isLoaded

	override suspend fun rewardvideoShowAndWait() {
		rewardVideoSignals.onRewardedVideoAdClosed.executeAndWaitSignal {
			activity.runOnUiThread {
				rewardVideo.show()
			}
		}
	}
}

private suspend inline fun <T> Map<Signal<Unit>, T>.executeAndWaitAnySignal(callback: () -> Unit): T {
	val deferred = CompletableDeferred<T>()
	val closeables = this.map { pair -> pair.key.once { deferred.complete(pair.value) } }
	try {
		callback()
		return deferred.await()
	} finally {
		closeables.close()
	}
}

private suspend inline fun <T> Iterable<Signal<T>>.executeAndWaitAnySignal(callback: () -> Unit): Pair<Signal<T>, T> {
	val deferred = CompletableDeferred<Pair<Signal<T>, T>>()
	val closeables = this.map { signal -> signal.once { deferred.complete(signal to it) } }
	try {
		callback()
		return deferred.await()
	} finally {
		closeables.close()
	}
}

private suspend inline fun <T> Signal<T>.executeAndWaitSignal(callback: () -> Unit): T {
	val deferred = CompletableDeferred<T>()
	val closeable = this.once { deferred.complete(it) }
	try {
		callback()
		return deferred.await()
	} finally {
		closeable.close()
	}
}

private fun View.removeFromParent() {
	(parent as? ViewGroup?)?.removeView(this)
}

