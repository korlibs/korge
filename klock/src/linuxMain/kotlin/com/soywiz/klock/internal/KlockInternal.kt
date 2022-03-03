package com.soywiz.klock.internal

import com.soywiz.klock.*
import com.soywiz.klock.hr.HRTimeSpan
import kotlinx.cinterop.*
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

    // @TODO: kotlin-native bug: https://github.com/JetBrains/kotlin-native/pull/1901
    //private val microStart = kotlin.system.getTimeMicros()
    //actual fun currentTimeMillis(): Long = kotlin.system.getTimeMillis()
    //actual fun microClock(): Double = (kotlin.system.getTimeMicros() - microStart).toDouble()

    actual fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan = memScoped {
        val t = alloc<time_tVar>()
        val tm = alloc<tm>()
        t.value = (time.unixMillisLong / 1000L).convert()
        localtime_r(t.ptr, tm.ptr)
        tm.tm_gmtoff.toInt().seconds
    }
}
