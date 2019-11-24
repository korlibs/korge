package com.soywiz.korge.service.vibration

import com.soywiz.klock.*
import com.soywiz.korge.view.*

actual class NativeVibration actual constructor(actual val views: Views) {
    actual fun vibrate(pattern: Array<TimeSpan>) {
    }
}
