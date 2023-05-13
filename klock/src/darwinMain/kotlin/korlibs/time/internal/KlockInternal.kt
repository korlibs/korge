package korlibs.time.internal

import korlibs.time.*
import korlibs.time.darwin.*
import korlibs.time.hr.*
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.posix.*

internal actual object KlockInternal {
    actual val currentTime: Double get() = memScoped {
        val timeVal = alloc<timeval>()
        gettimeofday(timeVal.ptr, null)
        val sec = timeVal.tv_sec
        val usec = timeVal.tv_usec
        ((sec * 1_000L) + (usec / 1_000L)).toDouble()
    }

    actual val hrNow: HRTimeSpan get() = memScoped {
        val timeVal = alloc<timeval>()
        gettimeofday(timeVal.ptr, null)
        val sec = timeVal.tv_sec
        val usec = timeVal.tv_usec
        HRTimeSpan.fromSeconds(sec.toInt()) + HRTimeSpan.fromMicroseconds(usec.toInt())
    }

    actual fun sleep(time: HRTimeSpan) {
        val micros = time.microsecondsDouble.toLong()
        val s = micros / 1_000_000
        val u = micros % 1_000_000
        if (s > 0) platform.posix.sleep(s.convert())
        if (u > 0) platform.posix.usleep(u.convert())
    }

    actual fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan = autoreleasepool {
        CFAbsoluteTimeGetCurrent()
        return getLocalTimezoneOffsetDarwin(CFTimeZoneCopySystem(), time)
    }
}

internal fun getLocalTimezoneOffsetDarwin(tz: CFTimeZoneRef?, time: DateTime): TimeSpan {
    val secondsSince2001 = time.cfAbsoluteTime()
    return (CFTimeZoneGetSecondsFromGMT(tz, secondsSince2001.toDouble()) / 60.0).minutes
}
