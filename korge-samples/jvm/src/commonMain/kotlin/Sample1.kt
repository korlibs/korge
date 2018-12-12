import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.dynamic.*
import com.soywiz.korge.dynamic.Dynamic.get
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import kotlin.js.*
import kotlin.jvm.*

object Sample1 {
    @JvmStatic
    fun main(args: Array<String>) = Korge(title = "Sample1") {
        //waveEffectView {
        //colorMatrixEffectView(ColorMatrixEffectView.GRAYSCALE_MATRIX) {
        //convolute3EffectView(Convolute3EffectView.KERNEL_EDGE_DETECTION) {
        /*
        blurEffectView(radius = 1.0) {
            convolute3EffectView(Convolute3EffectView.KERNEL_GAUSSIAN_BLUR) {
                //convolute3EffectView(Convolute3EffectView.KERNEL_BOX_BLUR) {
                swizzleColorsEffectView("bgra") {
                    x = 100.0
                    y = 100.0
                    image(Bitmap32(100, 100) { x, y -> RGBA(156 + x, 156 + y, 0, 255) })
                    //solidRect(100, 100, Colors.RED)
                }
                //}
            }
        }.apply {
            tween(this::radius[10.0], time = 5.seconds)
        }
        */

        //val mfilter = ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX, 0.0)
        //val mfilter = WaveFilter()
        //val mfilter = Convolute3Filter(Convolute3Filter.KERNEL_GAUSSIAN_BLUR)
        solidRect(640, 480, Colors.ALICEBLUE)
        image(resourcesVfs["a.png"].readBitmap()) {
            position(50, 50)
        }
        image(Bitmap32(100, 100) { x, y -> RGBA(156 + x, 156 + y, 0, 255) }) {
            x = 100.0
            y = 100.0
            //filter = ComposedFilter(SwizzleColorsFilter("bgra"), SwizzleColorsFilter("bgra"))
            //filter = ComposedFilter(
            //	SwizzleColorsFilter("bgra"),
            //	Convolute3Filter(Convolute3Filter.KERNEL_GAUSSIAN_BLUR),
            //	Convolute3Filter(Convolute3Filter.KERNEL_EDGE_DETECTION)
            //)
            //filter = ComposedFilter(mfilter, Convolute3Filter(Convolute3Filter.KERNEL_GAUSSIAN_BLUR))
            alpha = 1.0
            //filter = mfilter
            //filter = WaveFilter()
        }.apply {
            //mfilter.amplitudeY = 6
            //mfilter.amplitudeX = 0
            //mfilter.time = 0.5
            //tween(mfilter::time[0.0, 10.0], time = 10.seconds)
            //tween(mfilter::blendRatio[0.0, 1.0], time = 4.seconds)
            onClick {
                try {
                    //LaunchReview.launch("com.google.android.apps.maps")
                    println(Camera.getPicture(Camera.Info()))
                } catch (e: Throwable) {
                    alert("$e")
                }
            }
        }
        //val bmp = SolidRect(100, 100, Colors.RED).renderToBitmap(views)
        //val bmp = view.renderToBitmap(views)
        //bmp.writeTo("/tmp/demo.png".uniVfs, defaultImageFormats)
        //println(bmp)
    }
}

fun alert(error: String) {
    Dynamic {
        kotlin.runCatching {
            println(error)
            global.dynamicInvoke("alert", error)
        }
    }
}

// https://www.npmjs.com/package/cordova-launch-review
object LaunchReview {
    val _LaunchReview get() = Dynamic.global["LaunchReview"]
    val LaunchReview get() = _LaunchReview ?: error("LaunchReview not available. Execute 'cordova plugin add cordova-launch-review'")
    val supported get() = Dynamic { _LaunchReview.dynamicInvoke("isRatingSupported") as? Boolean? ?: false }

    suspend fun launch(appId: String) {
        Dynamic {
            val deferred = CompletableDeferred<Unit>()
            LaunchReview.dynamicInvoke("launch", {
                deferred.complete(Unit)
            }, { err: Any? ->
                deferred.completeExceptionally(RuntimeException("$err"))
            }, appId)
            deferred.await()
        }
    }

    suspend fun rating(): Boolean {
        return Dynamic {
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
            deferred.await()
        }
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
        return Dynamic {
            val deferred = CompletableDeferred<String>()
            val camera = global["navigator"]["camera"] ?: error("Camera not available. Execute 'cordova plugin add cordova-plugin-camera'")
            camera.dynamicInvoke("getPicture", { res: Any? ->
                deferred.complete("$res")
            }, { err: Any? ->
                deferred.completeExceptionally(RuntimeException("$err"))
            }, info)
            deferred.await()
        }
    }
}

// https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-vibration/index.html
object Vibrate {
    suspend fun vibrate(time: TimeSpan) {
        Dynamic { global["navigator"].dynamicInvoke("vibrate", time.milliseconds) }
        com.soywiz.korio.async.delay(time)
    }
}