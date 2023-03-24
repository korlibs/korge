package korlibs.time.wrapped

import korlibs.time.Date
import korlibs.time.DateFormat
import korlibs.time.Month
import korlibs.time.Year
import korlibs.time.annotations.KlockExperimental
import korlibs.time.internal.Serializable
import korlibs.time.minus
import korlibs.time.plus

@KlockExperimental
val Date.wrapped get() = WDate(this)

/**
 * Wrapped Version, that is not inline. You can use [value] to get the wrapped value class.
 *
 * Represents a triple of [year], [month] and [day].
 *
 * It is packed in a value class wrapping an Int to prevent allocations.
 */
@KlockExperimental
data class WDate(val value: Date) : Comparable<WDate>, Serializable {
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

    operator fun minus(time: WTimeSpan) = (this.value - time.value).wrapped
    operator fun minus(time: WMonthSpan) = (this.value - time.value).wrapped
    operator fun minus(time: WDateTimeSpan) = (this.value - time.value).wrapped
    operator fun minus(time: WTime) = (this.value - time.value).wrapped

    /** Converts this date to String formatting it like "2020-01-01", "2020-12-31" or "-2020-12-31" if the [year] is negative */
    override fun toString(): String = value.toString()
}