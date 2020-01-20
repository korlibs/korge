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
     * @param amplitudes list of intensities of the vibration. A `0.2` results in 20% vibration power.
     *        Only supported on Android target. Ignored if the size is not equal with the timings.
     */
    @ExperimentalUnsignedTypes
    fun vibratePattern(timings: Array<TimeSpan>, amplitudes: Array<Double> = emptyArray())

    /**
     * @param time vibration duration in milliseconds
     * @param amplitude percentage intensity of the vibration. A `0.2` results in 20% vibration power.
     *        Only supported on Android target.
     */
    @ExperimentalUnsignedTypes
    fun vibrate(time: TimeSpan, amplitude: Double = 1.0)
}
