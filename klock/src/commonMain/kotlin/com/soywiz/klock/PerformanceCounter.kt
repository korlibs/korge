package com.soywiz.klock

import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.timeSpan
import com.soywiz.klock.internal.*

/**
 * Class for measuring relative times with as much precision as possible.
 */
object PerformanceCounter {
    /**
     * Returns a performance counter measure in nanoseconds.
     */
    val nanoseconds: Double get() = KlockInternal.hrNow.nanosecondsDouble

    /**
     * Returns a performance counter measure in microseconds.
     */
    val microseconds: Double get() = KlockInternal.hrNow.microsecondsDouble

    /**
     * Returns a performance counter measure in milliseconds.
     */
    val milliseconds: Double get() = KlockInternal.hrNow.millisecondsDouble

    /**
     * Returns a performance counter as a [TimeSpan].
     */
    val reference: TimeSpan get() = KlockInternal.hrNow.timeSpan

    /**
     * Returns a performance counter as a [TimeSpan].
     */
    val hr: HRTimeSpan get() = KlockInternal.hrNow
}
