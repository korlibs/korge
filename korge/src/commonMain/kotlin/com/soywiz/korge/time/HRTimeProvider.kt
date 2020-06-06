package com.soywiz.korge.time

import com.soywiz.klock.DateTime
import com.soywiz.klock.PerformanceCounter
import com.soywiz.klock.TimeProvider
import com.soywiz.korge.internal.KorgeInternal

/** Class to provide time that can be overridden to mock or change its behaviour. */
interface HRTimeProvider {
    /** Returns a [HRTimeSpan] for this provider. */
    fun now(): HRTimeSpan

    companion object : HRTimeProvider {
        override fun now(): HRTimeSpan = HRTimeSpan.fromNanoseconds(PerformanceCounter.nanoseconds)

        /** Constructs a [HRTimeProvider] from a [callback] producing a [HRTimeSpan]. */
        operator fun invoke(callback: () -> HRTimeSpan) = object : HRTimeProvider {
            override fun now(): HRTimeSpan = callback()
        }
    }
}

@Deprecated("")
@KorgeInternal
fun HRTimeProvider.toTimeProvider(): TimeProvider = TimeProvider { DateTime.fromUnix(this.now().millisecondsDouble) }
