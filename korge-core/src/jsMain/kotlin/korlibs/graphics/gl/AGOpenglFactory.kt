package korlibs.graphics.gl

import korlibs.graphics.*

actual object AGOpenglFactory {
	actual fun create(nativeComponent: Any?): AGFactory = AGFactoryWebgl
	actual val isTouchDevice: Boolean get() {
		return js("('ontouchstart' in window || navigator.maxTouchPoints)").unsafeCast<Boolean>()
	}
}
