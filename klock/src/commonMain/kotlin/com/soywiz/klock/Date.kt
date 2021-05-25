package com.soywiz.klock

import com.soywiz.klock.internal.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.abs

/**
 * Represents a triple of [year], [month] and [day].
 *
 * It is packed in a value class wrapping an Int to prevent allocations.
 */
@JvmInline
value class Date(val encoded: Int) : Comparable<Date>, Serializable {
	companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /** Constructs a new [Date] from the [year], [month] and [day] components. */
		operator fun invoke(year: Int, month: Int, day: Int) = Date((year shl 16) or (month shl 8) or (day shl 0))
        /** Constructs a new [Date] from the [year], [month] and [day] components. */
		operator fun invoke(year: Int, month: Month, day: Int) = Date(year, month.index1, day)
        /** Constructs a new [Date] from the [year], [month] and [day] components. */
		operator fun invoke(year: Year, month: Month, day: Int) = Date(year.year, month.index1, day)
        /** Constructs a new [Date] from the [yearMonth] and [day] components. */
		operator fun invoke(yearMonth: YearMonth, day: Int) = Date(yearMonth.yearInt, yearMonth.month1, day)
	}

    /** The [year] part as [Int]. */
	val year: Int get() = encoded shr 16
    /** The [month] part as [Int] where [Month.January] is 1. */
	val month1: Int get() = (encoded ushr 8) and 0xFF
    /** The [month] part. */
	val month: Month get() = Month[month1]
    /** The [day] part. */
	val day: Int get() = (encoded ushr 0) and 0xFF
    /** The [year] part as [Year]. */
	val yearYear: Year get() = Year(year)

    /** A [DateTime] instance representing this date and time from the beginning of the [day]. */
	val dateTimeDayStart get() = DateTime(year, month, day)

    /** The [dayOfYear] part. */
	val dayOfYear get() = dateTimeDayStart.dayOfYear
    /** The [dayOfWeek] part. */
	val dayOfWeek get() = dateTimeDayStart.dayOfWeek
    /** The [dayOfWeek] part as [Int]. */
	val dayOfWeekInt get() = dateTimeDayStart.dayOfWeekInt

    /** Converts this date to String using [format] for representing it. */
	fun format(format: String) = dateTimeDayStart.format(format)
    /** Converts this date to String using [format] for representing it. */
	fun format(format: DateFormat) = dateTimeDayStart.format(format)

    /** Converts this date to String formatting it like "2020-01-01", "2020-12-31" or "-2020-12-31" if the [year] is negative */
	override fun toString(): String = "${if (year < 0) "-" else ""}${abs(year).toString()}-${abs(month1).toString().padStart(2, '0')}-${abs(day).toString().padStart(2, '0')}"

	override fun compareTo(other: Date): Int = this.encoded.compareTo(other.encoded)
}

operator fun Date.plus(time: TimeSpan) = (this.dateTimeDayStart + time).date
operator fun Date.plus(time: MonthSpan) = (this.dateTimeDayStart + time).date
operator fun Date.plus(time: DateTimeSpan) = (this.dateTimeDayStart + time).date
operator fun Date.plus(time: Time) = DateTime.createAdjusted(year, month1, day, time.hour, time.minute, time.second, time.millisecond)

operator fun Date.minus(time: TimeSpan) = (this.dateTimeDayStart - time).date
operator fun Date.minus(time: MonthSpan) = (this.dateTimeDayStart - time).date
operator fun Date.minus(time: DateTimeSpan) = (this.dateTimeDayStart - time).date
operator fun Date.minus(time: Time) = DateTime.createAdjusted(year, month1, day, -time.hour, -time.minute, -time.second, -time.millisecond)
