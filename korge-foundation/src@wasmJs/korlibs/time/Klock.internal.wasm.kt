@file:Suppress("PackageDirectoryMismatch")

package korlibs.time.internal

import korlibs.time.*
import kotlinx.browser.*

private external class HRTime

@JsFun("(time, index) => { return time[index] }")
private external fun HRTime_get(time: HRTime, index: Int): Double

@JsFun("() => { return process.hrtime(); }")
private external fun process_hrtime(): HRTime

@JsFun("(relative) => { return process.hrtime(relative); }")
private external fun process_hrtime(relative: HRTime): HRTime

@JsFun("() => { return (typeof process === 'object' && typeof require === 'function'); }")
private external fun isNodeJs(): Boolean

@JsFun("() => { return Date.now(); }")
private external fun Date_now(): Double

@JsFun("(rtime) => { return -(new Date(rtime)).getTimezoneOffset(); }")
private external fun Date_localTimezoneOffsetMinutes(rtime: Double): Int

private val initialHrTime by lazy { process_hrtime() }

internal actual object KlockInternal {
    actual val currentTime: Double get() = Date_now()

    actual val now: TimeSpan get() = when {
        isNodeJs() -> {
            val result: HRTime = process_hrtime(initialHrTime)
            TimeSpan.fromSeconds(HRTime_get(result, 0)) + TimeSpan.fromNanoseconds(HRTime_get(result, 1))
        }
        else -> {
            TimeSpan.fromMilliseconds(window.performance.now())
        }
    }

    actual fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan {
        return Date_localTimezoneOffsetMinutes(time.unixMillisDouble).minutes
    }

    actual fun sleep(time: TimeSpan) {
        spinlock(time)
    }
}

actual interface Serializable
