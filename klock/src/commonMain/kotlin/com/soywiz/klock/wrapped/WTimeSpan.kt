package com.soywiz.klock.wrapped

import com.soywiz.klock.*
import com.soywiz.klock.annotations.*
import com.soywiz.klock.internal.*

@KlockExperimental
val TimeSpan.wrapped get() = WTimeSpan(this)

/**
 * Wrapped Version, that is not inline. You can use [value] to get the wrapped inline class.
 *
 * Represents a span of time, with [milliseconds] precision.
 *
 * It is an inline class wrapping [Double] instead of [Long] to work on JavaScript without allocations.
 */
@KlockExperimental
data class WTimeSpan(val value: TimeSpan) : Comparable<WTimeSpan>, Serializable {
    val milliseconds: Double get() = value.milliseconds
    /** Returns the total number of [nanoseconds] for this [WTimeSpan] (1 / 1_000_000_000 [seconds]) */
    val nanoseconds: Double get() = value.nanoseconds
    /** Returns the total number of [microseconds] for this [WTimeSpan] (1 / 1_000_000 [seconds]) */
    val microseconds: Double get() = value.microseconds
    /** Returns the total number of [seconds] for this [WTimeSpan] */
    val seconds: Double get() = value.seconds
    /** Returns the total number of [minutes] for this [WTimeSpan] (60 [seconds]) */
    val minutes: Double get() = value.minutes
    /** Returns the total number of [hours] for this [WTimeSpan] (3_600 [seconds]) */
    val hours: Double get() = value.hours
    /** Returns the total number of [days] for this [WTimeSpan] (86_400 [seconds]) */
    val days: Double get() = value.days
    /** Returns the total number of [weeks] for this [WTimeSpan] (604_800 [seconds]) */
    val weeks: Double get() = value.weeks

    /** Returns the total number of [milliseconds] as a [Long] */
    val millisecondsLong: Long get() = value.millisecondsLong
    /** Returns the total number of [milliseconds] as an [Int] */
    val millisecondsInt: Int get() = value.millisecondsInt

    override fun compareTo(other: WTimeSpan): Int = value.compareTo(other.value)

    operator fun unaryMinus() = (-value).wrapped
    operator fun unaryPlus() = (+value).wrapped

    operator fun plus(other: WTimeSpan): WTimeSpan = (value + other.value).wrapped
    operator fun plus(other: WMonthSpan): WDateTimeSpan = (value + other.value).wrapped
    operator fun plus(other: WDateTimeSpan): WDateTimeSpan = (value + other).wrapped

    operator fun minus(other: WTimeSpan): WTimeSpan = (this.value - other.value).wrapped
    operator fun minus(other: WMonthSpan): WDateTimeSpan = (this.value - other.value).wrapped
    operator fun minus(other: WDateTimeSpan): WDateTimeSpan = (this.value - other.value).wrapped

    operator fun times(scale: Int): WTimeSpan = (value * scale).wrapped
    operator fun times(scale: Double): WTimeSpan = (value * scale).wrapped

    operator fun div(scale: Int): WTimeSpan = (value / scale).wrapped
    operator fun div(scale: Double): WTimeSpan = (value / scale).wrapped

    operator fun div(other: WTimeSpan): Double = this.value / other.value
    operator fun rem(other: WTimeSpan): WTimeSpan = (this.value % other.value).wrapped

    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /**
         * Zero time.
         */
        val ZERO get() = TimeSpan.ZERO.wrapped

        /**
         * Represents an invalid TimeSpan.
         * Useful to represent an alternative "null" time lapse
         * avoiding the boxing of a nullable type.
         */
        val NIL get() = TimeSpan.NIL.wrapped
    }

    /**
     * Formats this [WTimeSpan] into something like `12:30:40.100`.
     *
     * For 3 hour, 20 minutes and 15 seconds
     *
     * 1 [components] (seconds): 12015
     * 2 [components] (minutes): 200:15
     * 3 [components] (hours)  : 03:20:15
     * 4 [components] (days)   : 00:03:20:15
     *
     * With milliseconds would add decimals to the seconds part.
     */
    fun toTimeString(components: Int = 3, addMilliseconds: Boolean = false): String = value.toTimeString(components, addMilliseconds)

    override fun toString(): String = value.toString()
}

@KlockExperimental
fun max(a: WTimeSpan, b: WTimeSpan): WTimeSpan = max(a.value, b.value).wrapped
@KlockExperimental
fun min(a: WTimeSpan, b: WTimeSpan): WTimeSpan = min(a.value, b.value).wrapped
@KlockExperimental
fun WTimeSpan.clamp(min: WTimeSpan, max: WTimeSpan) = value.clamp(min.value, max.value).wrapped
