package com.soywiz.klock.internal

import com.soywiz.klock.*
import com.soywiz.klock.hr.HRTimeSpan
import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.*

internal actual object KlockInternal {
    actual val currentTime: Double
        get() = memScoped {
            val timeVal = alloc<timeval>()
            mingw_gettimeofday(timeVal.ptr, null) // mingw: doesn't expose gettimeofday, but mingw_gettimeofday
            val sec = timeVal.tv_sec
            val usec = timeVal.tv_usec
            ((sec * 1_000L) + (usec / 1_000L)).toDouble()
        }

    actual val hrNow: HRTimeSpan
        get() = memScoped {
            val timeVal = alloc<timeval>()
            mingw_gettimeofday(timeVal.ptr, null)
            val sec = timeVal.tv_sec
            val usec = timeVal.tv_usec
            HRTimeSpan.fromSeconds(sec.toInt()) + HRTimeSpan.fromMicroseconds(usec.toInt())
        }

    actual fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan = memScoped {
        val timeAsFileTime = UnixMillisecondsToWindowsTicks(time.unixMillisLong)
        val utcFtime = FILETIME_fromWindowsTicks(timeAsFileTime)
        val timezone = getTimeZoneInformation()
        val utcStime = utcFtime.toSystemTime()
        val localStime = utcStime.toTimezone(timezone)
        val localUnix = localStime.toFiletime().toUnix()
        val utcUnix = utcStime.toFiletime().toUnix()
        return (localUnix - utcUnix).milliseconds
    }

    actual fun sleep(time: HRTimeSpan) {
        val micros = time.microsecondsDouble.toLong()
        val s = micros / 1_000_000
        val u = micros % 1_000_000
        if (s > 0) platform.posix.sleep(s.convert())
        if (u > 0) platform.posix.usleep(u.convert())
    }

    fun SYSTEMTIME.toTimezone(tzi: TIME_ZONE_INFORMATION): SYSTEMTIME = memScoped { alloc<SYSTEMTIME>().apply { SystemTimeToTzSpecificLocalTime(tzi.ptr, this@toTimezone.ptr, this.ptr) } }
    fun SYSTEMTIME.toUtc(tzi: TIME_ZONE_INFORMATION): SYSTEMTIME = memScoped { alloc<SYSTEMTIME>().apply { TzSpecificLocalTimeToSystemTime(tzi.ptr, this@toUtc.ptr, this.ptr) } }
    fun getTimeZoneInformation() = memScoped { alloc<TIME_ZONE_INFORMATION>().apply { GetTimeZoneInformation(this.ptr) } }
    fun SYSTEMTIME.toFiletime(): FILETIME = memScoped { alloc<FILETIME>().apply { SystemTimeToFileTime(this@toFiletime.ptr, this.ptr) } }
    fun FILETIME.toSystemTime(): SYSTEMTIME = memScoped { alloc<SYSTEMTIME>().apply { FileTimeToSystemTime(this@toSystemTime.ptr, this.ptr) } }
    fun FILETIME.toWindowsTicks() = ((dwHighDateTime.toULong() shl 32) or (dwLowDateTime.toULong())).toLong()
    fun FILETIME.toUnix() = WindowsTickToUnixMilliseconds(toWindowsTicks())
    fun FILETIME_fromUnix(unix: Long): FILETIME = FILETIME_fromWindowsTicks(UnixMillisecondsToWindowsTicks(unix))
    fun FILETIME_fromWindowsTicks(ticks: Long): FILETIME = memScoped { return alloc<FILETIME>().apply { dwHighDateTime = (ticks ushr 32).toUInt(); dwLowDateTime = ticks.toUInt()} }
    const val WINDOWS_TICK = 10_000L
    const val MS_TO_UNIX_EPOCH = 11644473600_000L

    fun WindowsTickToUnixMilliseconds(windowsTicks: Long) = (windowsTicks / WINDOWS_TICK - MS_TO_UNIX_EPOCH)
    fun UnixMillisecondsToWindowsTicks(unix: Long) = ((unix + MS_TO_UNIX_EPOCH) * WINDOWS_TICK)
}
