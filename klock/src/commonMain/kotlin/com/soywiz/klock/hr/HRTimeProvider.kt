package com.soywiz.klock.hr

import com.soywiz.klock.PerformanceCounter

/** Class to provide time that can be overridden to mock or change its behaviour. */
interface HRTimeProvider {
    /** Returns a [HRTimeSpan] for this provider. */
    fun now(): HRTimeSpan

    companion object : HRTimeProvider {
        override fun now(): HRTimeSpan = PerformanceCounter.hr

        /** Constructs a [HRTimeProvider] from a [callback] producing a [HRTimeSpan]. */
        operator fun invoke(callback: () -> HRTimeSpan) = object : HRTimeProvider {
            override fun now(): HRTimeSpan = callback()
        }
    }
}
