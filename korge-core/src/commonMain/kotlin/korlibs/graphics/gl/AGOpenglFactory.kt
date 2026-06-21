package korlibs.graphics.gl

import korlibs.graphics.AGFactory

expect object AGOpenglFactory {
	fun create(nativeComponent: Any?): AGFactory
	val isTouchDevice: Boolean
}
