package com.soywiz.korag.gl

import com.soywiz.korag.AGFactory

expect object AGOpenglFactory {
	fun create(nativeComponent: Any?): AGFactory
	val isTouchDevice: Boolean
}
