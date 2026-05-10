package korlibs.graphics.gl

import korlibs.kgl.*
import korlibs.graphics.*
import korlibs.io.wasm.*
import korlibs.wasm.*
import org.w3c.dom.*
import kotlinx.browser.*

object AGFactoryWebgl : AGFactory {
	override val supportsNativeFrame: Boolean = true
	override fun create(nativeControl: Any?, config: AGConfig): AG = AGWebgl(config)
	override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
		TODO()
	}
}

fun jsEmptyObject(): JsAny = jsEmptyObj()

fun jsObject(vararg pairs: Pair<String, Any?>): JsAny {
	val out = jsEmptyObject()
	for ((k, v) in pairs) if (v != null) out.setAny(k.toJsString(), v.toJsReference())
	//for ((k, v) in pairs) out[k] = v
	return out
}

val korgwCanvasQuery: String? by lazy { window.getAny("korgwCanvasQuery")?.unsafeCast<JsString>()?.toString() }
val isCanvasCreatedAndHandled get() = korgwCanvasQuery == null

fun AGDefaultCanvas(): HTMLCanvasElement {
    return (korgwCanvasQuery?.let { document.querySelector(it) as HTMLCanvasElement })
        ?: (document.createElement("canvas") as HTMLCanvasElement)
}

fun AGWebgl(config: AGConfig, canvas: HTMLCanvasElement = AGDefaultCanvas()): AGOpengl {
    val kmlGl: KmlGl = KmlGlWasmCanvas(
        canvas, jsObject(
            "premultipliedAlpha" to false, // To be like the other targets
            "alpha" to false,
            "stencil" to true,
            "antialias" to config.antialiasHint
        )
    )
    return AGOpengl(kmlGl).also { ag ->
        //window.setAny("ag".toJsString(), ag.toJsReference())

        // https://www.khronos.org/webgl/wiki/HandlingContextLost
        // https://gist.github.com/mattdesl/9995467

        canvas.addEventListener("webglcontextlost", { e ->
            //contextVersion++
            e.preventDefault()
            null
        }, false)

        canvas.addEventListener("webglcontextrestored", { e ->
            ag.contextLost()
            //e.preventDefault()
            null
        }, false)
    }
}
