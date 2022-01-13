package com.soywiz.klock.internal

import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import java.util.*

internal actual object KlockInternal {
    actual val currentTime: Double get() = (System.currentTimeMillis()).toDouble()
    actual val hrNow: HRTimeSpan get() = HRTimeSpan.fromNanoseconds(System.nanoTime().toDouble())
    actual fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan = TimeZone.getDefault().getOffset(time.unixMillisLong).milliseconds
    actual fun sleep(time: HRTimeSpan) {
        val nanos = time.nanosecondsDouble.toLong()
        Thread.sleep(nanos / 1_000_000, (nanos % 1_000_000).toInt())
    }
}

actual typealias Serializable = java.io.Serializable
