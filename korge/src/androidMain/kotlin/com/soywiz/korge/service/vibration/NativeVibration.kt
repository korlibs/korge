package com.soywiz.korge.service.vibration

import android.annotation.*
import android.os.*
import android.support.v4.content.ContextCompat.*
import com.soywiz.klock.*
import com.soywiz.korge.android.*
import com.soywiz.korge.view.*

actual class NativeVibration actual constructor(val views: Views) {
    @SuppressLint("MissingPermission")
    actual fun vibrate(pattern: Array<TimeSpan>) {
        val v = getSystemService(views.androidActivity, Vibrator::class.java)
        v?.vibrate(VibrationEffect.createWaveform(pattern.map { it.millisecondsLong }.toLongArray(), -1))
    }
}
