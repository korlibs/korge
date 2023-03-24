package korlibs.time.internal

import korlibs.time.DateTime
import korlibs.time.TimeSpan
import korlibs.time.hr.HRTimeSpan
import korlibs.time.minutes
import kotlinx.browser.window

private external val process: dynamic

private val isNode = jsTypeOf(window) == "undefined"
private val initialHrTime by klockAtomicLazy { process.hrtime() }

internal actual object KlockInternal {
    actual val currentTime: Double get() = (js("Date.now()").unsafeCast<Double>())

    actual val hrNow: HRTimeSpan get() = when {
        isNode -> {
            val result: Array<Double> = process.hrtime(initialHrTime).unsafeCast<Array<Double>>()
            HRTimeSpan.fromSeconds(result[0]) + HRTimeSpan.fromNanoseconds(result[1])
        }
        else -> {
            HRTimeSpan.fromMilliseconds(window.performance.now())
        }
    }

    actual fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan {
        @Suppress("UNUSED_VARIABLE")
        val rtime = time.unixMillisDouble
        return js("-(new Date(rtime)).getTimezoneOffset()").unsafeCast<Int>().minutes
    }

    actual fun sleep(time: HRTimeSpan) {
        spinlock(time)
    }
}

actual interface Serializable