package com.soywiz.korag

import com.soywiz.korgw.JvmAGFactory

actual object AGOpenglFactory {
    actual fun create(nativeComponent: Any?): AGFactory = JvmAGFactory
    actual val isTouchDevice: Boolean = false
}
