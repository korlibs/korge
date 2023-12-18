@file:OptIn(ExperimentalForeignApi::class)
@file:Suppress("PackageDirectoryMismatch")

package korlibs.time.internal

import korlibs.time.*
import kotlinx.cinterop.*
import platform.posix.mingw_gettimeofday
import platform.posix.timeval
import platform.windows.FILETIME
import platform.windows.FileTimeToSystemTime
import platform.windows.GetTimeZoneInformation
import platform.windows.SYSTEMTIME
import platform.windows.SystemTimeToFileTime
import platform.windows.SystemTimeToTzSpecificLocalTime
import platform.windows.TIME_ZONE_INFORMATION
import platform.windows.TzSpecificLocalTimeToSystemTime

internal actual object KlockInternal {
    actual val currentTime: Double
        get() = memScoped {
            val timeVal = alloc<timeval>()
            mingw_gettimeofday(timeVal.ptr, null) // mingw: doesn't expose gettimeofday, but mingw_gettimeofday
            val sec = timeVal.tv_sec
            val usec = timeVal.tv_usec
            ((sec * 1_000L) + (usec / 1_000L)).toDouble()
        }

    actual val now: TimeSpan
        get() = memScoped {
            val timeVal = alloc<timeval>()
            mingw_gettimeofday(timeVal.ptr, null)
            val sec = timeVal.tv_sec
            val usec = timeVal.tv_usec
            TimeSpan.fromSeconds(sec) + TimeSpan.fromMicroseconds(usec)
        }

    actual fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan = memScoped {
        val timeAsFileTime = UnixMillisecondsToWindowsTicks(time.unixMillisLong)
        val utcFtime = FILETIME_fromWindowsTicks(this, timeAsFileTime)
        val timezone = getTimeZoneInformation(this)
        val utcStime = utcFtime.toSystemTime(this)
        val localStime = utcStime.toTimezone(this, timezone)
        val localUnix = localStime.toFiletime(this).toUnix()
        val utcUnix = utcStime.toFiletime(this).toUnix()
        return (localUnix - utcUnix).milliseconds
    }

    actual fun sleep(time: TimeSpan) {
        val micros = time.inWholeMicroseconds
        val s = micros / 1_000_000
        val u = micros % 1_000_000
        if (s > 0) platform.posix.sleep(s.convert())
        if (u > 0) platform.posix.usleep(u.convert())
    }

    fun FILETIME_fromWindowsTicks(scope: NativePlacement, ticks: Long): FILETIME = scope.run { alloc<FILETIME>().apply { dwHighDateTime = (ticks ushr 32).toUInt(); dwLowDateTime = ticks.toUInt() } }
    fun getTimeZoneInformation(scope: NativePlacement) = scope.run { alloc<TIME_ZONE_INFORMATION>().apply { GetTimeZoneInformation(this.ptr) } }
    fun FILETIME.toSystemTime(scope: NativePlacement): SYSTEMTIME = scope.run { alloc<SYSTEMTIME>().apply { FileTimeToSystemTime(this@toSystemTime.ptr, this.ptr) } }
    fun SYSTEMTIME.toTimezone(scope: NativePlacement, tzi: TIME_ZONE_INFORMATION): SYSTEMTIME = scope.run { alloc<SYSTEMTIME>().apply { SystemTimeToTzSpecificLocalTime(tzi.ptr, this@toTimezone.ptr, this.ptr) } }
    fun SYSTEMTIME.toUtc(scope: NativePlacement, tzi: TIME_ZONE_INFORMATION): SYSTEMTIME = scope.run { alloc<SYSTEMTIME>().apply { TzSpecificLocalTimeToSystemTime(tzi.ptr, this@toUtc.ptr, this.ptr) } }
    fun SYSTEMTIME.toFiletime(scope: NativePlacement): FILETIME = scope.run { alloc<FILETIME>().apply { SystemTimeToFileTime(this@toFiletime.ptr, this.ptr) } }
    fun FILETIME.toWindowsTicks() = ((dwHighDateTime.toULong() shl 32) or (dwLowDateTime.toULong())).toLong()
    fun FILETIME.toUnix() = WindowsTickToUnixMilliseconds(toWindowsTicks())
    fun FILETIME_fromUnix(scope: NativePlacement, unix: Long): FILETIME = FILETIME_fromWindowsTicks(scope, UnixMillisecondsToWindowsTicks(unix))
    const val WINDOWS_TICK = 10_000L
    const val MS_TO_UNIX_EPOCH = 11644473600_000L
    fun WindowsTickToUnixMilliseconds(windowsTicks: Long) = (windowsTicks / WINDOWS_TICK - MS_TO_UNIX_EPOCH)
    fun UnixMillisecondsToWindowsTicks(unix: Long) = ((unix + MS_TO_UNIX_EPOCH) * WINDOWS_TICK)
}
