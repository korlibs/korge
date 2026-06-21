package korlibs.graphics.gl

import korlibs.graphics.*
import korlibs.kgl.*

actual object AGOpenglFactory {
    actual fun create(nativeComponent: Any?): AGFactory = AGFactoryMacos
    actual val isTouchDevice: Boolean = false
}

object AGFactoryMacos : AGFactory {
    override val supportsNativeFrame: Boolean = false

    override fun create(nativeControl: Any?, config: AGConfig): AG {
        return AGOpengl(KmlGlNative())
    }

    override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
        error("Fast OpenGL windows are not implemented for macOS native yet")
    }
}

fun AGNative(gl: KmlGl = KmlGlNative()): AGOpengl = AGOpengl(gl)
