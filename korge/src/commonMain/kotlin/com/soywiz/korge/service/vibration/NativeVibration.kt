package com.soywiz.korge.service.vibration

import com.soywiz.klock.*
import com.soywiz.korge.view.*

expect class NativeVibration(val views: Views) {
    fun vibrate(pattern: Array<TimeSpan>)
}
