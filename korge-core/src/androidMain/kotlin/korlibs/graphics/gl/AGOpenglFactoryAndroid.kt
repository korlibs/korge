package korlibs.graphics.gl

import korlibs.graphics.*

actual object AGOpenglFactory {
    actual fun create(nativeComponent: Any?): AGFactory = AGFactoryAndroid
    actual val isTouchDevice: Boolean = true
}
