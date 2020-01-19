package com.soywiz.korge.service.vibration

import com.soywiz.klock.TimeSpan
import com.soywiz.korge.view.Views

/**
 * Support for device vibrations. Currently only works in Browser and Android target.
 * The `amplitude` is only available on android.
 */
expect class NativeVibration constructor(views: Views) {

    /**
     * @param timings list of alternating ON-OFF durations in milliseconds. Staring with ON.
     * @param amplitudes list of intensities of the vibration (0-255). Only supported on Android target.
     *        Ignored if the size is not equal with the timings.
     */
    @ExperimentalUnsignedTypes
    fun vibrate(timings: Array<TimeSpan>, amplitudes: Array<UInt> = emptyArray())

    /**
     * @param time vibration duration in milliseconds
     * @param amplitude intensity of the vibration (0-255). Only supported on Android target.
     */
    @ExperimentalUnsignedTypes
    fun vibrate(time: TimeSpan, amplitude: UInt = 255U)
}
