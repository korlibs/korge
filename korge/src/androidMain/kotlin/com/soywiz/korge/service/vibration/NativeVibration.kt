package com.soywiz.korge.service.vibration

import android.annotation.SuppressLint
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.content.ContextCompat.getSystemService
import com.soywiz.klock.TimeSpan
import com.soywiz.korge.android.androidActivity
import com.soywiz.korge.view.Views

actual class NativeVibration actual constructor(views: Views) {

    companion object {
        private const val NO_REPEAT = -1
    }

    private val vibrator = getSystemService(views.androidActivity, Vibrator::class.java)

    /**
     * @param timings list of alternating ON-OFF durations in milliseconds. Staring with ON.
     * @param amplitudes list of intensities of the vibration (0-255)
     */
    @ExperimentalUnsignedTypes
    @SuppressLint("MissingPermission")
    actual fun vibrate(timings: Array<TimeSpan>, amplitudes: Array<UInt>) {
        val onOffTimings = (listOf(TimeSpan.NULL) + timings).map { it.millisecondsLong }.toLongArray()
        if (amplitudes.size != onOffTimings.size) {
            vibrator?.vibrate(VibrationEffect.createWaveform(onOffTimings, NO_REPEAT))
        } else {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    onOffTimings,
                    amplitudes.toUIntArray().toIntArray(),
                    NO_REPEAT
                )
            )
        }

    }

    /**
     * @param time vibration duration in milliseconds
     * @param amplitude intensity of the vibration (0-255)
     */
    @ExperimentalUnsignedTypes
    @SuppressLint("MissingPermission")
    actual fun vibrate(time: TimeSpan, amplitude: UInt) {
        vibrator?.vibrate(VibrationEffect.createOneShot(time.millisecondsLong, amplitude.toInt()))
    }
}
