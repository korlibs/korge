package com.soywiz.korag

import com.soywiz.kgl.*
import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.util.*
import org.w3c.dom.*
import kotlinx.browser.*
import org.khronos.webgl.WebGLRenderingContext as GL

object AGFactoryWebgl : AGFactory {
	override val supportsNativeFrame: Boolean = true
	override fun create(nativeControl: Any?, config: AGConfig): AG = AGWebgl(config)
	override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
		TODO()
	}
}

fun jsEmptyObject(): dynamic = js("({})")

fun jsObject(vararg pairs: Pair<String, Any?>): dynamic {
	val out = jsEmptyObject()
	for ((k, v) in pairs) if (v != null) out[k] = v
	//for ((k, v) in pairs) out[k] = v
	return out
}

val korgwCanvasQuery: String? by lazy { window.asDynamic().korgwCanvasQuery.unsafeCast<String?>() }
val isCanvasCreatedAndHandled get() = korgwCanvasQuery == null

open class AGWebgl(val config: AGConfig, val glDecorator: (KmlGl) -> KmlGl = { it }) : AGOpengl(), AGContainer {
    companion object {
		//var UNPACK_PREMULTIPLY_ALPHA_WEBGL = document.createElement('canvas').getContext('webgl').UNPACK_PREMULTIPLY_ALPHA_WEBGL
		const val UNPACK_PREMULTIPLY_ALPHA_WEBGL = 37441
	}

	override val ag: AG = this

    open fun getCanvas(): HTMLCanvasElement {
        return (korgwCanvasQuery?.let { document.querySelector(it) as HTMLCanvasElement })
            ?: (document.createElement("canvas") as HTMLCanvasElement)
    }

	val canvas by lazy { getCanvas() }

	val glOpts = jsObject(
		"premultipliedAlpha" to true,
		"alpha" to false,
		"stencil" to true,
        "antialias" to config.antialiasHint
	)
	//val gl: GL = (canvas.getContext("webgl", glOpts) ?: canvas.getContext("experimental-webgl", glOpts)) as GL
	//override val gl = KmlGlCached(KmlGlJsCanvas(canvas, glOpts))
    val baseGl = KmlGlJsCanvas(canvas, glOpts)
    override val gl = glDecorator(baseGl)

    override val webgl: Boolean get() = true
    override val webgl2: Boolean get() = baseGl.webglVersion >= 2

    init {
		(window.asDynamic()).ag = this
		//(window.asDynamic()).gl = gl
	}

	override val nativeComponent: Any = canvas
	val tDevicePixelRatio get() = window.devicePixelRatio.toDouble()
	override var devicePixelRatio = 1.0; get() = when {
		tDevicePixelRatio <= 0.0 -> 1.0
		tDevicePixelRatio.isNaN() -> 1.0
		tDevicePixelRatio.isInfinite() -> 1.0
		else -> tDevicePixelRatio
	}
    // @TODO: Improve this: https://gist.github.com/scryptonite/5242987
    override val pixelsPerInch: Double get() = 96.0 * devicePixelRatio

    init {
		canvas.addEventListener("webglcontextlost", { e ->
			//contextVersion++
			e.preventDefault()
		}, false)

		canvas.addEventListener("webglcontextrestored", { e ->
			contextVersion++
			//e.preventDefault()
		}, false)

		//fun handleOnResized() {
		//	ag.resized(canvas.width, canvas.height)
		//}
//
		//window.addEventListener("resize", { e ->
		//	handleOnResized()
		//	//e.preventDefault()
		//}, false)
//
		//handleOnResized()
	}

	override fun repaint() {
	}

	override fun dispose() {
		// https://www.khronos.org/webgl/wiki/HandlingContextLost
		// https://gist.github.com/mattdesl/9995467
	}

	override fun prepareUploadNativeTexture(bmp: NativeImage) {
		gl.pixelStorei(UNPACK_PREMULTIPLY_ALPHA_WEBGL, bmp.premultiplied.toInt())
	}
}
