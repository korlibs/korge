package com.soywiz.klock.hr

import com.soywiz.klock.PerformanceCounter
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.internal.KlockInternal
import com.soywiz.klock.microseconds
import com.soywiz.klock.nanoseconds
import kotlin.math.round

/** Converts a [TimeSpan] into a high-resolution [HRTimeSpan] */
val TimeSpan.hr get() = HRTimeSpan.fromMilliseconds(this.milliseconds)

/** Converts a [HRTimeSpan] into a low-resolution [TimeSpan] */
val HRTimeSpan.timeSpan get() = nanosecondsRaw.nanoseconds

val Int.hrSeconds get() = HRTimeSpan.fromSeconds(this)
val Int.hrMilliseconds get() = HRTimeSpan.fromMilliseconds(this)
val Int.hrMicroseconds get() = HRTimeSpan.fromMicroseconds(this)
val Int.hrNanoseconds get() = HRTimeSpan.fromNanoseconds(this)

val Double.hrSeconds get() = HRTimeSpan.fromSeconds(this)
val Double.hrMilliseconds get() = HRTimeSpan.fromMilliseconds(this)
val Double.hrMicroseconds get() = HRTimeSpan.fromMicroseconds(this)
val Double.hrNanoseconds get() = HRTimeSpan.fromNanoseconds(this)

// @TODO: Ensure nanosecondsRaw has no decimals
/** A High-Resolution TimeSpan that stores its values as nanoseconds. Just uses 52-bits and won't store decimals */
inline class HRTimeSpan constructor(val nanosecondsRaw: Double) : Comparable<HRTimeSpan> {
    companion object {
        val ZERO = HRTimeSpan(0.0)
        val NULL = HRTimeSpan(Double.NaN)

        fun now() = PerformanceCounter.hr

        fun fromSeconds(value: Double) = HRTimeSpan(round(value * 1_000_000_000))
        fun fromMilliseconds(value: Double) = HRTimeSpan(round(value * 1_000_000))
        fun fromMicroseconds(value: Double) = HRTimeSpan(round(value * 1_000))
        fun fromNanoseconds(value: Double) = HRTimeSpan(round(value))

        fun fromSeconds(value: Int) = fromSeconds(value.toDouble())
        fun fromMilliseconds(value: Int) = fromMilliseconds(value.toDouble())
        fun fromMicroseconds(value: Int) = fromMicroseconds(value.toDouble())
        fun fromNanoseconds(value: Int) = fromNanoseconds(value.toDouble())
    }

    val nanosecondsDouble get() = (nanosecondsRaw)
    val microsecondsDouble get() = (nanosecondsRaw / 1_000)
    val millisecondsDouble get() = (nanosecondsRaw / 1_000_000)
    val secondsDouble get() = (nanosecondsRaw / 1_000_000_000)

    val nanosecondsInt get() = nanosecondsRaw.toInt()
    val microsecondsInt get() = microsecondsDouble.toInt()
    val millisecondsInt get() = millisecondsDouble.toInt()
    val secondsInt get() = secondsDouble.toInt()

    operator fun plus(other: HRTimeSpan): HRTimeSpan = fromNanoseconds(nanosecondsRaw + other.nanosecondsRaw)
    operator fun minus(other: HRTimeSpan): HRTimeSpan = fromNanoseconds(nanosecondsRaw - other.nanosecondsRaw)
    operator fun rem(other: HRTimeSpan): HRTimeSpan = fromNanoseconds(nanosecondsRaw % other.nanosecondsRaw)
    operator fun times(other: Double): HRTimeSpan = fromNanoseconds(nanosecondsRaw * other)
    operator fun times(other: Int): HRTimeSpan = fromNanoseconds(nanosecondsRaw * other)
    operator fun div(other: HRTimeSpan): Double = (nanosecondsRaw / other.nanosecondsRaw)
    override fun compareTo(other: HRTimeSpan): Int = this.nanosecondsRaw.compareTo(other.nanosecondsRaw)

    override fun toString(): String = "$nanosecondsRaw".removeSuffix(".0") + " ns"
}

fun max(a: HRTimeSpan, b: HRTimeSpan): HRTimeSpan = kotlin.math.max(a.nanosecondsRaw, b.nanosecondsRaw).hrNanoseconds
fun min(a: HRTimeSpan, b: HRTimeSpan): HRTimeSpan = kotlin.math.min(a.nanosecondsRaw, b.nanosecondsRaw).hrNanoseconds
fun HRTimeSpan.clamp(min: HRTimeSpan, max: HRTimeSpan): HRTimeSpan = when {
    this < min -> min
    this > max -> max
    else -> this
}
inline fun HRTimeSpan.coalesce(block: () -> HRTimeSpan): HRTimeSpan = if (this != HRTimeSpan.NULL) this else block()
