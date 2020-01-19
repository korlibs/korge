package com.soywiz.korge.service.vibration

import com.soywiz.klock.TimeSpan
import com.soywiz.korge.view.Views
import kotlin.browser.window

actual class NativeVibration actual constructor(val views: Views) {

    /**
     * @param timings list of alternating ON-OFF durations in milliseconds. Staring with ON.
     * @param amplitudes has no effect on JS backend
     */
    @ExperimentalUnsignedTypes
    actual fun vibrate(timings: Array<TimeSpan>, amplitudes: Array<UInt>) {
        window.navigator.vibrate(timings.map { it.milliseconds }.toTypedArray())
    }

    /**
     * @param time vibration duration in milliseconds
     * @param amplitude has no effect on JS backend
     */
    @ExperimentalUnsignedTypes
    actual fun vibrate(time: TimeSpan, amplitude: UInt) {
        window.navigator.vibrate(time.milliseconds)
    }
}
