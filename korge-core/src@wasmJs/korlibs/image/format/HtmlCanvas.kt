package korlibs.image.format

import korlibs.platform.*
import kotlinx.browser.*
import org.w3c.dom.*
import org.w3c.files.*

external interface RenderingContextJs : RenderingContext, JsAny

external interface HTMLCanvasElementLike : TexImageSourceJs {
    val width: Int
    val height: Int
    fun getContext(contextId: String, vararg arguments: JsAny?): RenderingContextJs?
    fun toDataURL(type: String = definedExternally, quality: JsAny? = definedExternally): String
    fun toBlob(_callback: (Blob?) -> Unit, type: String = definedExternally, quality: JsAny? = definedExternally): Unit
}

external interface HTMLImageElementLike : TexImageSourceJs {
    val width: Int
    val height: Int
    val src: JsString?
}

object HtmlCanvas {
	fun createCanvas(width: Int, height: Int): HTMLCanvasElementLike {
		if (Platform.isJsNodeJs) error("Canvas not available on Node.JS")
        val out = document.createElement("canvas").unsafeCast<HTMLCanvasElement>()
        out.width = width
        out.height = height
        return out.toLike()
	}
}

fun HTMLCanvasElement.toLike() = this.unsafeCast<HTMLCanvasElementLike>()
