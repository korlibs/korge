---
permalink: /monetization/
group: reference
layout: default
title: Monetization
title_prefix: KorGE
fa-icon: fa-dollar-sign
priority: 100
---

In order to monetize your game, KorGE provices some out of the box plugins to do so:



You can also create or use any of your own multiplatform libraries.

## AdMob

KorGE supports admob out of the box on Android.
On the rest of the targets this API is mocked and does nothing.

To use it you have to include the KorGE plugin korge-admob:

### build.gradle

In your `build.gradle`:

```kotlin
korge {
    admob(ADMOB_APP_ID) // Shortcut for admob
}
```

This is a shortcut for:

```kotlin
korge {
    plugin("com.soywiz:korge-admob:$korgeVersion", mapOf("ADMOB_APP_ID" to ADMOB_APP_ID))
}
```

Any of these add the `korge-admob` dependency but also does additional configuration in Android XML files.

### The Admob instance

You can instantiate admob directly or register it as a service/singleton in your module:

With the injector:

```kotlin
injector.mapSingleton { AdmobCreate(testing = true) }
```

Without the injector:

```kotlin
AdmobCreate(testing = true)
```

The class looks like this:

```kotlin
class Admob {
    // Checks if this API is available, or it is mocked
    suspend fun available()
    
    // Banner-related APIs
    suspend fun bannerPrepare(config: Config)
    suspend fun bannerShow()
    suspend fun bannerHide()
    suspend fun bannerPrepareAndShow(config: Config)
    
    // Interstitial-related APIs
    suspend fun interstitialPrepare(config: Config)
    suspend fun interstitialIsLoaded(): Boolean
    suspend fun interstitialShowAndWait()
    suspend fun interstitialWaitLoaded()
    suspend fun interstitialWaitAndShow(config: Config)
    
    // RewardVideo-related APIs
    suspend fun rewardvideolPrepare(config: Config)
    suspend fun rewardvideolIsLoaded(): Boolean
    suspend fun rewardvideoShowAndWait()
    suspend fun rewardvideolWaitLoaded()
    suspend fun rewardvideolWaitAndShow(config: Config)
    
    enum class Size { BANNER, IAB_BANNER, IAB_LEADERBOARD, IAB_MRECT, LARGE_BANNER, SMART_BANNER, FLUID }
    data class Reward(val type: String, val amount: Int)
    data class Config(
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
}
```
