package com.soywiz.korge.service.vibration

import com.soywiz.klock.TimeSpan
import com.soywiz.korge.view.Views

actual class NativeVibration actual constructor(val views: Views) {
    /**
     * @param timings list of alternating ON-OFF durations in milliseconds. Staring with ON.
     * @param amplitudes list of intensities of the vibration. A `0.2` results in 20% vibration power.
     */
    @ExperimentalUnsignedTypes
    actual fun vibratePattern(timings: Array<TimeSpan>, amplitudes: Array<Double>) {
    }

    /**
     * @param time vibration duration in milliseconds
     * @param amplitude percentage intensity of the vibration. A `0.2` results in 20% vibration power.
     */
    @ExperimentalUnsignedTypes
    actual fun vibrate(time: TimeSpan, amplitude: Double) {
    }
}
