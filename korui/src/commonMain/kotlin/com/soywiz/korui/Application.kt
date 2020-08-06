package com.soywiz.korui

import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korui.geom.len.*
import com.soywiz.korui.light.*
import com.soywiz.korui.ui.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.*

interface ApplicationAware {
	val app: Application
}

class Application(val coroutineContext: CoroutineContext, val light: LightComponents) : Closeable, ApplicationAware {
	companion object {
		suspend operator fun invoke() = Application(defaultLight(coroutineContext))
		suspend operator fun invoke(light: LightComponents) = Application(coroutineContext, light)

		suspend operator fun invoke(light: LightComponents, callback: suspend Application.() -> Unit) {
			val app = Application(coroutineContext, light)
			try {
				callback(app)
			} finally {
				//app.loop.close()
			}
		}
	}

	override val app = this
	val frames = arrayListOf<Frame>()
	val lengthContext = Length.Context().apply {
		pixelsPerInch = light.getDpi()
	}
	val devicePixelRatio: Double get() = light.getDevicePixelRatio()

	override fun close() {
	}
}

fun Application(callback: suspend Application.() -> Unit) =
	Korui { Application(defaultLightFactory.create(coroutineContext, null)) { callback() } }

@PublishedApi
internal fun Application.framePre(
	title: String,
	width: Int = 640,
	height: Int = 480,
	icon: Bitmap? = null
): Frame {
	val frame = Frame(this, title).apply {
		setBoundsInternal(0, 0, width, height)
	}
	frame.icon = icon
	//light.setBounds(frame.handle, 0, 0, frame.actualBounds.width, frame.actualBounds.height)
	//koruiApplicationLog.info { "Application.frame: ${frame.actualBounds}" }
	var resizing = false
	frame.addEventListener<ReshapeEvent> { e ->
		if (!resizing) {
			resizing = true
			try {
				//koruiApplicationLog.info { "Application.frame.ResizedEvent: ${e.width},${e.height}" }
				frame.invalidate()
				frame.setBoundsAndRelayout(0, 0, e.width, e.height)
				light.repaint(frame.handle)
			} finally {
				resizing = false
			}
		}
	}
	return frame
}

@PublishedApi
internal fun Application.framePost(frame: Frame) {
	frames += frame
	frame.setBoundsAndRelayout(0, 0, frame.actualBounds.width, frame.actualBounds.height)
	frame.invalidate()
	frame.visible = true
	light.configuredFrame(frame.handle)
}

inline fun Application.frame(
	title: String,
	width: Int = 640,
	height: Int = 480,
	icon: Bitmap? = null,
	callback: Frame.() -> Unit = {}
): Frame {
	return framePre(title, width, height, icon).apply(callback).also { framePost(it) }
}

//suspend fun CanvasApplication(
//	title: String,
//	width: Int = 640,
//	height: Int = 480,
//	icon: Bitmap? = null,
//	light: LightComponents = defaultLight,
//	callback: suspend (AGContainer) -> Unit = {}
//) = CanvasApplicationEx(title, width, height, icon, light) { canvas, _ -> callback(canvas) }

suspend fun CanvasApplicationEx(
	title: String,
	width: Int = 640,
	height: Int = 480,
	icon: Bitmap? = null,
	light: LightComponents? = null,
	quality: LightQuality = LightQuality.PERFORMANCE,
	koruiContext: KoruiContext,
    agConfig: AGConfig = AGConfig(),
	callback: suspend (AgCanvas, Frame) -> Unit = { _, _ -> }
) {
	if (OS.isNative) println("CanvasApplicationEx[0]")
	val llight = light ?: defaultLight(coroutineContext, koruiContext)
	if (OS.isNative) println("CanvasApplicationEx[1]")
	llight.quality = quality
	val application = Application(coroutineContext, llight)
	if (OS.isNative) println("CanvasApplicationEx[2]")

	//val loop = coroutineContext.animationFrameLoop {
	//	var n = 0
	//	while (n < application.frames.size) {
	//		val frame = application.frames[n++]
	//		if (frame.valid) continue
	//		frame.setBoundsAndRelayout(frame.actualBounds)
	//		application.light.repaint(frame.handle)
	//		println("frame")
	//	}
	//}
	lateinit var canvas: AgCanvas
	if (OS.isNative) println("CanvasApplicationEx[3]")
	val frame = application.frame(title, width, height, icon) {
		canvas = agCanvas(agConfig).apply { focus() }
	}
	if (OS.isNative) println("CanvasApplicationEx[4] - canvas.waitReady()")
    while (true) {
        try {
            withTimeout(5000L) {
                canvas.waitReady()
            }
            break
        } catch (e: TimeoutCancellationException) {
            println("canvas.waitReady() was not called after 5 seconds. Retrying...")
            continue
        }
    }
	if (OS.isNative) println("CanvasApplicationEx[5]")
	callback(canvas, frame)
	if (OS.isNative) println("CanvasApplicationEx[6]")
}

