@file:Suppress("PackageDirectoryMismatch")

package korlibs.time.internal

import korlibs.time.*
import java.util.*

internal actual object KlockInternal {
    actual val currentTime: Double get() = CurrentKlockInternalJvm.currentTime
    actual val now: TimeSpan get() = CurrentKlockInternalJvm.hrNow
    actual fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan = CurrentKlockInternalJvm.localTimezoneOffsetMinutes(time)
    actual fun sleep(time: TimeSpan) {
        val nanos = time.nanoseconds.toLong()
        Thread.sleep(nanos / 1_000_000, (nanos % 1_000_000).toInt())
    }
}

inline fun <T> TemporalKlockInternalJvm(impl: KlockInternalJvm, callback: () -> T): T {
    val old = CurrentKlockInternalJvm
    CurrentKlockInternalJvm = impl
    try {
        return callback()
    } finally {
        CurrentKlockInternalJvm = old
    }
}

var CurrentKlockInternalJvm = object : KlockInternalJvm {
}

interface KlockInternalJvm {
    val currentTime: Double get() = (System.currentTimeMillis()).toDouble()
    val microClock: Double get() = hrNow.microseconds
    val hrNow: TimeSpan get() = TimeSpan.fromNanoseconds(System.nanoTime().toDouble())
    fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan = TimeZone.getDefault().getOffset(time.unixMillisLong).milliseconds
}

actual typealias Serializable = java.io.Serializable
