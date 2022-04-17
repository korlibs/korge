package com.soywiz.korag.gl

import com.soywiz.korag.*
import com.soywiz.korgw.*

actual object AGOpenglFactory {
    actual fun create(nativeComponent: Any?): AGFactory = JvmAGFactory
    actual val isTouchDevice: Boolean = false
}
