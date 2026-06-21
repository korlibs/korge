package korlibs.korge.service.vibration

import kotlin.coroutines.*
import kotlin.time.*

/**
 * Support for device vibrations. Currently only works in Browser and Android target.
 * The `amplitude` is only available on android.
 */
expect class NativeVibration constructor(coroutineContext: CoroutineContext) {

    /**
     * @param timings list of alternating ON-OFF durations in milliseconds. Staring with ON.
     * @param amplitudes list of intensities of the vibration. A `0.2` results in 20% vibration power.
     *        Only supported on Android target. Ignored if the size is not equal with the timings.
     */
    fun vibratePattern(timings: Array<Duration>, amplitudes: Array<Double> = emptyArray())

    /**
     * @param time vibration duration in milliseconds
     * @param amplitude percentage intensity of the vibration. A `0.2` results in 20% vibration power.
     *        Only supported on Android target.
     */
    fun vibrate(time: Duration, amplitude: Double = 1.0)
}
