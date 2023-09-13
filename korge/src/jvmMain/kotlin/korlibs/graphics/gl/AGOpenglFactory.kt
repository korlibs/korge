package korlibs.graphics.gl

import korlibs.graphics.*
import korlibs.render.*

actual object AGOpenglFactory {
    actual fun create(nativeComponent: Any?): AGFactory = JvmAGFactory
    actual val isTouchDevice: Boolean = false
}
