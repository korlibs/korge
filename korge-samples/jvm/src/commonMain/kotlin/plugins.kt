import com.soywiz.klock.*
import com.soywiz.korge.dynamic.KgDynamic.dynamicInvoke
import com.soywiz.korge.dynamic.KgDynamic.get
import com.soywiz.korge.dynamic.KgDynamic.global
import kotlinx.coroutines.*
import kotlin.js.*

fun alert(error: String) {
    kotlin.runCatching {
        println(error)
        global.dynamicInvoke("alert", error)
    }
}

// https://www.npmjs.com/package/cordova-launch-review
object LaunchReview {
    val _LaunchReview get() = global["LaunchReview"]
    val LaunchReview get() = _LaunchReview ?: error("LaunchReview not available. Execute 'cordova plugin add cordova-launch-review'")
    val supported get() = _LaunchReview.dynamicInvoke("isRatingSupported") as? Boolean? ?: false

    suspend fun launch(appId: String) {
        val deferred = CompletableDeferred<Unit>()
        LaunchReview.dynamicInvoke("launch", {
            deferred.complete(Unit)
        }, { err: Any? ->
            deferred.completeExceptionally(RuntimeException("$err"))
        }, appId)
        return deferred.await()
    }

    suspend fun rating(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        LaunchReview.dynamicInvoke("rating", { result: String ->
            when (result) {
                "requested" -> println("Requested display of rating dialog")
                "shown" -> println("Rating dialog displayed")
                "dismissed" -> {
                    println("Rating dialog dismissed")
                    deferred.complete(false)
                }
                else -> Unit
            }
        }, { err: Any? ->
            deferred.completeExceptionally(RuntimeException("$err"))
        })
        return deferred.await()
    }
}

// https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-camera/index.html
object Camera {
    class Info(
            @JsName("quality") val quality: Int = 50,
            @JsName("allowEdit") val allowEdit: Boolean = false,
            @JsName("targetWidth") val targetWidth: Int? = null,
            @JsName("targetHeight") val targetHeight: Int? = null
    )

    suspend fun getPicture(
            info: Info = Info()
    ): String {
        val deferred = CompletableDeferred<String>()
        val camera = global["navigator"]["camera"] ?: error("Camera not available. Execute 'cordova plugin add cordova-plugin-camera'")
        camera.dynamicInvoke("getPicture", { res: Any? ->
            deferred.complete("$res")
        }, { err: Any? ->
            deferred.completeExceptionally(RuntimeException("$err"))
        }, info)
        return deferred.await()
    }
}

// https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-vibration/index.html
object Vibrate {
    suspend fun vibrate(time: TimeSpan) {
        global["navigator"].dynamicInvoke("vibrate", time.milliseconds)
        com.soywiz.korio.async.delay(time)
    }
}

// https://www.npmjs.com/package/cordova-plugin-admob-free
object AdMob {
    private val admob get() = global["admob"]

    val banner get() = Banner
    val intestitial get() = Interstitial
    val rewardvideo get() = RewardVideo

    object Banner {
        enum class Size {
            BANNER, IAB_BANNER, IAB_LEADERBOARD, IAB_MRECT, LARGE_BANNER, SMART_BANNER, FLUID
        }

        private val instance = admob["banner"]

        fun config(id: String, bannerAtTop: Boolean = false, overlap: Boolean = true, offsetTopBar: Boolean = false, size: Size = Size.SMART_BANNER) {
            instance.dynamicInvoke("config", object {
                @JsName("id")
                val id = id
                @JsName("bannerAtTop")
                val bannerAtTop = bannerAtTop
                @JsName("overlap")
                val overlap = overlap
                @JsName("offsetTopBar")
                val offsetTopBar = offsetTopBar
                @JsName("size")
                val size = size.name
            })
        }

        suspend fun prepare(): String = instance.dynamicInvoke("prepare").promiseAwait()?.toString() ?: "error"
        suspend fun show(): String = instance.dynamicInvoke("show").promiseAwait()?.toString() ?: "error"
        suspend fun hide(): String = instance.dynamicInvoke("hide").promiseAwait()?.toString() ?: "error"
        suspend fun remove(): String = instance.dynamicInvoke("remove").promiseAwait()?.toString() ?: "error"
    }

    abstract class InterstitialCommon {
        protected abstract val instance: Any?
        fun config(id: String, isTesting: Boolean = false, autoShow: Boolean = true, forChild: Boolean? = null, forFamily: Boolean? = null, latitude: Double? = null, longitude: Double? = null) {
            instance.dynamicInvoke("config", object {
                @JsName("id")
                val id = id
                @JsName("isTesting")
                val isTesting = isTesting
                @JsName("autoShow")
                val autoShow = autoShow
                @JsName("forChild")
                val forChild = forChild
                @JsName("forFamily")
                val forFamily = forFamily
                @JsName("location")
                val location = if (latitude != null && longitude != null) arrayOf(latitude, longitude) else null
            })
        }

        suspend fun prepare(): String = instance.dynamicInvoke("prepare").promiseAwait()?.toString() ?: "error"
        suspend fun show(): String = instance.dynamicInvoke("show").promiseAwait()?.toString() ?: "error"
    }

    object Interstitial : InterstitialCommon() {
        override val instance = admob["interstitial"]
    }

    object RewardVideo : InterstitialCommon() {
        override val instance = admob["rewardvideo"]
    }

    private suspend fun Any?.promiseAwait(): Any? {
        val instance = this ?: return null
        val result = CompletableDeferred<Any?>()
        if (instance["then"] != null) return instance
        instance.dynamicInvoke("then", { value: Any? ->
            result.complete(value)
        }, { err: Any? ->
            result.completeExceptionally(RuntimeException("$err"))
        })
        return result.await()
    }
}
