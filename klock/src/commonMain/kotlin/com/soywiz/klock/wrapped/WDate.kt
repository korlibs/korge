package com.soywiz.klock.wrapped

import com.soywiz.klock.*
import com.soywiz.klock.annotations.*
import com.soywiz.klock.internal.Serializable

@KlockExperimental
val Date.wrapped get() = WDate(this)

/**
 * Wrapped Version, that is not inline. You can use [value] to get the wrapped inline class.
 *
 * Represents a triple of [year], [month] and [day].
 *
 * It is packed in an inline class wrapping an Int to prevent allocations.
 */
@KlockExperimental
class WDate(val value: Date) : Comparable<WDate>, Serializable {
    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /** Constructs a new [WDate] from the [year], [month] and [day] components. */
        operator fun invoke(year: Int, month: Int, day: Int) = Date(year, month, day).wrapped
        /** Constructs a new [WDate] from the [year], [month] and [day] components. */
        operator fun invoke(year: Int, month: WMonth, day: Int) = Date(year, month, day).wrapped
        /** Constructs a new [WDate] from the [year], [month] and [day] components. */
        operator fun invoke(year: WYear, month: WMonth, day: Int) = Date(year.value, month, day).wrapped
        /** Constructs a new [WDate] from the [yearMonth] and [day] components. */
        operator fun invoke(yearMonth: WYearMonth, day: Int) = Date(yearMonth.value, day).wrapped
    }

    /** The [year] part as [Int]. */
    val year: Int get() = value.year
    /** The [month] part as [Int] where [Month.January] is 1. */
    val month1: Int get() = value.month1
    /** The [month] part. */
    val month: WMonth get() = value.month
    /** The [day] part. */
    val day: Int get() = value.day
    /** The [year] part as [Year]. */
    val yearYear: WYear get() = value.yearYear.wrapped

    /** A [WDateTime] instance representing this date and time from the beginning of the [day]. */
    val dateTimeDayStart get() = value.dateTimeDayStart.wrapped

    /** The [dayOfYear] part. */
    val dayOfYear get() = value.dayOfYear
    /** The [dayOfWeek] part. */
    val dayOfWeek get() = value.dayOfWeek
    /** The [dayOfWeek] part as [Int]. */
    val dayOfWeekInt get() = value.dayOfWeekInt

    /** Converts this date to String using [format] for representing it. */
    fun format(format: String) = value.format(format)
    /** Converts this date to String using [format] for representing it. */
    fun format(format: DateFormat) = value.format(format)

    override fun compareTo(other: WDate): Int = this.value.compareTo(other.value)

    operator fun plus(time: WTimeSpan) = (this.value + time.value).wrapped
    operator fun plus(time: WMonthSpan) = (this.value + time.value).wrapped
    operator fun plus(time: WDateTimeSpan) = (this.value + time.value).wrapped
    operator fun plus(time: WTime) = (this.value + time.value).wrapped

    override fun equals(other: Any?): Boolean = (other is WDate) && this.value == other.value
    override fun hashCode(): Int = value.hashCode()
    /** Converts this date to String formatting it like "2020-01-01", "2020-12-31" or "-2020-12-31" if the [year] is negative */
    override fun toString(): String = value.toString()
}
