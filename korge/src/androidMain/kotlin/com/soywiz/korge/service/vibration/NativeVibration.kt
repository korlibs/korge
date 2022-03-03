package com.soywiz.korge.service.vibration

import android.annotation.*
import android.os.*
import com.soywiz.klock.*
import com.soywiz.korge.android.*
import com.soywiz.korge.view.*
import kotlin.math.*

actual class NativeVibration actual constructor(views: Views) {

    companion object {
        private const val NO_REPEAT = -1
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 23) {
        views.androidActivity.getSystemService(Vibrator::class.java)
    } else {
        null
    }

    /**
     * @param timings list of alternating ON-OFF durations in milliseconds. Staring with ON.
     * @param amplitudes list of intensities of the vibration. A `0.2` results in 20% vibration power.
     */
    @ExperimentalUnsignedTypes
    @SuppressLint("MissingPermission")
    actual fun vibratePattern(timings: Array<TimeSpan>, amplitudes: Array<Double>) {
        val onOffTimings = (listOf(TimeSpan.NIL) + timings).map { it.millisecondsLong }.toLongArray()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (amplitudes.size != onOffTimings.size) {
                vibrator?.vibrate(VibrationEffect.createWaveform(onOffTimings, NO_REPEAT))
            } else {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(onOffTimings, amplitudes.map {it.toAndroidAmplitude()}.toIntArray(), NO_REPEAT)
                )
            }
        }
    }

    /**
     * @param time vibration duration in milliseconds
     * @param amplitude percentage intensity of the vibration. A `0.2` results in 20% vibration power.
     */
    @ExperimentalUnsignedTypes
    @SuppressLint("MissingPermission")
    actual fun vibrate(time: TimeSpan, amplitude: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(time.millisecondsLong, amplitude.toAndroidAmplitude()))
        }
    }

    /**
     * @return amplitude value between [0 - 255]
     */
    private fun Double.toAndroidAmplitude() : Int{
        val amplitude = 255 * abs(this)
        return min(amplitude, 255.0).toInt()
    }
}
