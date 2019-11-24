package com.soywiz.korge.service.vibration

import com.soywiz.klock.*
import com.soywiz.korge.view.*
import kotlin.browser.*

actual class NativeVibration actual constructor(val views: Views) {
    actual fun vibrate(pattern: Array<TimeSpan>) {
        window.navigator.vibrate(pattern.map { it.milliseconds }.toTypedArray())
    }
}
