package com.soywiz.klock.wrapped

import com.soywiz.klock.*
import com.soywiz.klock.annotations.*
import com.soywiz.klock.internal.Serializable

@KlockExperimental
val Time.wrapped get() = WTime(this)

/**
 * Wrapped Version, that is not inline. You can use [value] to get the wrapped value class.
 *
 * Represents a union of [millisecond], [second], [minute] and [hour].
 */
@KlockExperimental
data class WTime(val value: Time) : Comparable<WTime>, Serializable {
    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /** Constructs a new [WTime] from the [hour], [minute], [second] and [millisecond] components. */
        operator fun invoke(hour: Int, minute: Int = 0, second: Int = 0, millisecond: Int = 0): WTime =
            Time(hour, minute, second, millisecond).wrapped
    }
    /** The [millisecond] part. */
    val millisecond: Int get() = value.millisecond
    /** The [second] part. */
    val second: Int get() = value.second
    /** The [minute] part. */
    val minute: Int get() = value.minute
    /** The [hour] part. */
    val hour: Int get() = value.hour
    /** The [hour] part adjusted to 24-hour format. */
    val hourAdjusted: Int get() = value.hourAdjusted

    /** Returns new [WTime] instance adjusted to 24-hour format. */
    fun adjust(): WTime = value.adjust().wrapped

    /** Converts this date to String using [format] for representing it. */
    fun format(format: String) = value.format(format)
    /** Converts this date to String using [format] for representing it. */
    fun format(format: TimeFormat) = value.format(format)

    /** Converts this time to String formatting it like "00:00:00.000", "23:59:59.999" or "-23:59:59.999" if the [hour] is negative */
    override fun toString(): String = value.toString()

    override fun compareTo(other: WTime): Int = this.value.compareTo(other.value)
}

@KlockExperimental
operator fun WTime.plus(span: WTimeSpan) = (this.value + span.value).wrapped
