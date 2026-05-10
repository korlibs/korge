package korlibs.graphics.gl

import korlibs.graphics.*

@JsFun("() => { return ('ontouchstart' in window || navigator.maxTouchPoints); }")
private external fun _isTouchDevice(): Boolean

actual object AGOpenglFactory {
	actual fun create(nativeComponent: Any?): AGFactory = AGFactoryWebgl
	actual val isTouchDevice: Boolean get() = _isTouchDevice()
}
