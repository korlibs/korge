package korlibs.korge.service.vibration

import korlibs.io.wasm.*
import korlibs.time.TimeSpan
import korlibs.korge.view.Views
import kotlinx.browser.window

actual class NativeVibration actual constructor(val views: Views) {

    /**
     * @param timings list of alternating ON-OFF durations in milliseconds. Staring with ON.
     * @param amplitudes has no effect on JS backend
     */
    @ExperimentalUnsignedTypes
    actual fun vibratePattern(timings: Array<TimeSpan>, amplitudes: Array<Double>) {
        window.navigator.vibrate(jsArrayOf(*timings.map { it.milliseconds.toJsNumber() }.toTypedArray()))
    }

    /**
     * @param time vibration duration in milliseconds
     * @param amplitude has no effect on JS backend
     */
    @ExperimentalUnsignedTypes
    actual fun vibrate(time: TimeSpan, amplitude: Double) {
        window.navigator.vibrate(time.milliseconds.toInt())
    }
}
