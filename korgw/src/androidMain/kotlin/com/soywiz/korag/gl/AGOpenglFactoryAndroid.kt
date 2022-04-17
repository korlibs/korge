package com.soywiz.korag.gl

import com.soywiz.korag.*

actual object AGOpenglFactory {
    actual fun create(nativeComponent: Any?): AGFactory = AGFactoryAndroid
    actual val isTouchDevice: Boolean = true
}
