package com.soywiz.korge.service.vibration

import android.annotation.SuppressLint
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.content.ContextCompat.getSystemService
import com.soywiz.klock.TimeSpan
import com.soywiz.korge.android.androidActivity
import com.soywiz.korge.view.Views
import kotlin.math.abs
import kotlin.math.min

actual class NativeVibration actual constructor(views: Views) {

    companion object {
        private const val NO_REPEAT = -1
    }

    private val vibrator = getSystemService(views.androidActivity, Vibrator::class.java)

    /**
     * @param timings list of alternating ON-OFF durations in milliseconds. Staring with ON.
     * @param amplitudes list of intensities of the vibration. A `0.2` results in 20% vibration power.
     */
    @ExperimentalUnsignedTypes
    @SuppressLint("MissingPermission")
    actual fun vibratePattern(timings: Array<TimeSpan>, amplitudes: Array<Double>) {
        val onOffTimings = (listOf(TimeSpan.NULL) + timings).map { it.millisecondsLong }.toLongArray()
        if (amplitudes.size != onOffTimings.size) {
            vibrator?.vibrate(VibrationEffect.createWaveform(onOffTimings, NO_REPEAT))
        } else {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(onOffTimings, amplitudes.map {it.toAndroidAmplitude()}.toIntArray(), NO_REPEAT)
            )
        }
    }

    /**
     * @param time vibration duration in milliseconds
     * @param amplitude percentage intensity of the vibration. A `0.2` results in 20% vibration power.
     */
    @ExperimentalUnsignedTypes
    @SuppressLint("MissingPermission")
    actual fun vibrate(time: TimeSpan, amplitude: Double) {
        vibrator?.vibrate(VibrationEffect.createOneShot(time.millisecondsLong, amplitude.toAndroidAmplitude()))
    }

    /**
     * @return amplitude value between [0 - 255]
     */
    private fun Double.toAndroidAmplitude() : Int{
        val amplitude = 255 * abs(this)
        return min(amplitude, 1.0).toInt()
    }

}
