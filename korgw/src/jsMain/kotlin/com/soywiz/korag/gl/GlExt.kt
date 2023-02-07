package com.soywiz.korag.gl

import com.soywiz.kgl.*
import com.soywiz.korag.*
import org.w3c.dom.*
import kotlinx.browser.*

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

fun AGDefaultCanvas(): HTMLCanvasElement {
    return (korgwCanvasQuery?.let { document.querySelector(it) as HTMLCanvasElement })
        ?: (document.createElement("canvas") as HTMLCanvasElement)
}

fun AGWebgl(config: AGConfig, canvas: HTMLCanvasElement = AGDefaultCanvas()): AGOpengl = AGOpengl(
    KmlGlJsCanvas(
        canvas, jsObject(
            "premultipliedAlpha" to false, // To be like the other targets
            "alpha" to false,
            "stencil" to true,
            "antialias" to config.antialiasHint
        )
    )
).also { ag ->
    (window.asDynamic()).ag = ag

    // https://www.khronos.org/webgl/wiki/HandlingContextLost
    // https://gist.github.com/mattdesl/9995467

    canvas.addEventListener("webglcontextlost", { e ->
        //contextVersion++
        e.preventDefault()
    }, false)

    canvas.addEventListener("webglcontextrestored", { e ->
        ag.contextLost()
        //e.preventDefault()
    }, false)
}
