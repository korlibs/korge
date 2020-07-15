package com.soywiz.korag

expect object AGOpenglFactory {
	fun create(nativeComponent: Any?): AGFactory
	val isTouchDevice: Boolean
}
