package korlibs.korge.service.vibration

import korlibs.time.*
import kotlinx.browser.*
import kotlin.coroutines.*

actual class NativeVibration actual constructor(val coroutineContext: CoroutineContext) {

    /**
     * @param timings list of alternating ON-OFF durations in milliseconds. Staring with ON.
     * @param amplitudes has no effect on JS backend
     */
    @ExperimentalUnsignedTypes
    actual fun vibratePattern(timings: Array<TimeSpan>, amplitudes: Array<Double>) {
        window.navigator.vibrate(timings.map { it.milliseconds }.toTypedArray())
    }

    /**
     * @param time vibration duration in milliseconds
     * @param amplitude has no effect on JS backend
     */
    @ExperimentalUnsignedTypes
    actual fun vibrate(time: TimeSpan, amplitude: Double) {
        window.navigator.vibrate(time.milliseconds)
    }
}
