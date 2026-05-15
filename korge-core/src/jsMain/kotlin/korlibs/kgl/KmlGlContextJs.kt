package korlibs.kgl

import korlibs.memory.*
import korlibs.graphics.gl.*
import korlibs.platform.*
import kotlinx.browser.*
import org.w3c.dom.*

actual fun KmlGlContextDefault(window: Any?, parent: KmlGlContext?): KmlGlContext {
    return WebGLContext(window, parent)
}

class WebGLContext(window: Any?, parent: KmlGlContext?) : KmlGlContext(window, createJsCanvas(), parent) {
    override fun set() {
    }

    override fun unset() {
    }

    override fun swap() {
    }

    override fun close() {
    }

    companion object {
        fun createJsCanvas(): KmlGlJsCanvas {
            if (!Platform.isJsBrowser) error("Can't run WebGL outside a browser")
            return KmlGlJsCanvas(document.createElement("canvas").unsafeCast<HTMLCanvasElement>(), jsObject())
        }
    }
}
