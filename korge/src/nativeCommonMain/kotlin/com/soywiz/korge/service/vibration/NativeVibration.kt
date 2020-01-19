package com.soywiz.korge.service.vibration

import com.soywiz.klock.TimeSpan
import com.soywiz.korge.view.Views

actual class NativeVibration actual constructor(val views: Views) {
    /**
     * @param timings list of alternating ON-OFF durations in milliseconds. Staring with ON.
     * @param amplitudes list of intensities of the vibration (0-255)
     */
    @ExperimentalUnsignedTypes
    actual fun vibrate(timings: Array<TimeSpan>, amplitudes: Array<UInt>) {
    }

    /**
     * @param time vibration duration in milliseconds
     * @param amplitude intensity of the vibration (0-255)
     */
    @ExperimentalUnsignedTypes
    actual fun vibrate(time: TimeSpan, amplitude: UInt) {
    }
}
