package korlibs.graphics.gl

import korlibs.graphics.AG
import korlibs.graphics.AGConfig
import korlibs.graphics.AGFactory
import korlibs.graphics.AGWindow

actual object AGOpenglFactory {
    actual fun create(nativeComponent: Any?): AGFactory = AGFactoryWatchosUnsupported
    actual val isTouchDevice: Boolean = true
}

object AGFactoryWatchosUnsupported : AGFactory {
    override val supportsNativeFrame: Boolean = false

    override fun create(nativeControl: Any?, config: AGConfig): AG {
        error("OpenGL is not supported on watchOS")
    }

    override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
        error("Fast OpenGL windows are not supported on watchOS")
    }
}
