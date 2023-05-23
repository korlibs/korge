package korlibs.time.internal

import korlibs.time.DateTime
import korlibs.time.TimeSpan
import korlibs.time.hr.HRTimeSpan
import korlibs.time.minutes
import kotlinx.browser.window

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

    actual val hrNow: HRTimeSpan get() = when {
        isNodeJs() -> {
            val result: HRTime = process_hrtime(initialHrTime)
            HRTimeSpan.fromSeconds(HRTime_get(result, 0)) + HRTimeSpan.fromNanoseconds(HRTime_get(result, 1))
        }
        else -> {
            HRTimeSpan.fromMilliseconds(window.performance.now())
        }
    }

    actual fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan {
        return Date_localTimezoneOffsetMinutes(time.unixMillisDouble).minutes
    }

    actual fun sleep(time: HRTimeSpan) {
        spinlock(time)
    }
}

actual interface Serializable
