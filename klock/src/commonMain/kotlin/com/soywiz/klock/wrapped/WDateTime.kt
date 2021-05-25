package com.soywiz.klock.wrapped

import com.soywiz.klock.*
import com.soywiz.klock.annotations.*
import com.soywiz.klock.internal.*

@KlockExperimental
val DateTime.wrapped get() = WDateTime(this)

/**
 * Wrapped Version, that is not inline. You can use [value] to get the wrapped value class.
 *
 * Represents a Date in UTC (GMT+00) with millisecond precision.
 *
 * It is internally represented as an inlined double, thus doesn't allocate in any target including JS.
 * It can represent without loss dates between (-(2 ** 52) and (2 ** 52)):
 * - Thu Aug 10 -140744 07:15:45 GMT-0014 (Central European Summer Time)
 * - Wed May 23 144683 18:29:30 GMT+0200 (Central European Summer Time)
 */
@KlockExperimental
data class WDateTime(val value: DateTime) : Comparable<WDateTime>, Serializable {
    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /** It is a [WDateTime] instance representing 00:00:00 UTC, Thursday, 1 January 1970. */
        val EPOCH get() = DateTime.EPOCH.wrapped

        /**
         * Constructs a new [WDateTime] from date and time information.
         *
         * This might throw a [DateException] on invalid dates.
         */
        operator fun invoke(
            year: WYear,
            month: Month,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
            milliseconds: Int = 0
        ) = DateTime(year.value, month, day, hour, minute, second, milliseconds).wrapped

        /**
         * Constructs a new [WDateTime] from date and time information.
         *
         * This might throw a [DateException] on invalid dates.
         */
        operator fun invoke(
            date: WDate,
            time: Time = Time(0.milliseconds)
        ) = DateTime(date.value, time).wrapped

        /**
         * Constructs a new [WDateTime] from date and time information.
         *
         * This might throw a [DateException] on invalid dates.
         */
        operator fun invoke(
            year: Int,
            month: WMonth,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
            milliseconds: Int = 0
        ) = DateTime(year, month, day, hour, minute, second, milliseconds).wrapped

        /**
         * Constructs a new [WDateTime] from date and time information.
         *
         * This might throw a [DateException] on invalid dates.
         */
        operator fun invoke(
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
            milliseconds: Int = 0
        ) = DateTime(year, month, day, hour, minute, second, milliseconds).wrapped

        /**
         * Constructs a new [WDateTime] from date and time information.
         *
         * On invalid dates, this function will try to adjust the specified invalid date to a valid one by clamping components.
         */
        fun createClamped(
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
            milliseconds: Int = 0
        ) = DateTime.createClamped(year, month, day, hour, minute, second, milliseconds).wrapped

        /**
         * Constructs a new [WDateTime] from date and time information.
         *
         * On invalid dates, this function will try to adjust the specified invalid date to a valid one by adjusting other components.
         */
        fun createAdjusted(
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
            milliseconds: Int = 0
        ) = DateTime.createAdjusted(year, month, day, hour, minute, second, milliseconds).wrapped

        /**
         * Constructs a new [WDateTime] from date and time information.
         *
         * On invalid dates, this function will have an undefined behaviour.
         */
        fun createUnchecked(
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
            milliseconds: Int = 0
        ) = DateTime.createUnchecked(year, month, day, hour, minute, second, milliseconds).wrapped

        /** Constructs a new [WDateTime] from a [unix] timestamp. */
        operator fun invoke(unix: Long) = DateTime(unix).wrapped
        /** Constructs a new [WDateTime] from a [unix] timestamp. */
        operator fun invoke(unix: Double) = DateTime(unix).wrapped

        /** Constructs a new [WDateTime] from a [unix] timestamp. */
        fun fromUnix(unix: Double) = DateTime.fromUnix(unix).wrapped
        /** Constructs a new [WDateTime] from a [unix] timestamp. */
        fun fromUnix(unix: Long) = DateTime.fromUnix(unix).wrapped

        /** Constructs a new [WDateTime] by parsing the [str] using standard date formats. */
        fun fromString(str: String) = DateTime.fromString(str).wrapped
        /** Constructs a new [WDateTime] by parsing the [str] using standard date formats. */
        fun parse(str: String) = DateTime.parse(str).wrapped

        /** Returns the current time as [WDateTime]. Note that since [WDateTime] is inline, this property doesn't allocate on JavaScript. */
        fun now() = DateTime.now().wrapped
        /** Returns the current local time as [WDateTimeTz]. */
        fun nowLocal() = DateTime.nowLocal().wrapped

        /** Returns the total milliseconds since unix epoch. The same as [nowUnixLong] but as double. To prevent allocation on targets without Long support. */
        fun nowUnix(): Double = DateTime.nowUnix()
        /** Returns the total milliseconds since unix epoch. */
        fun nowUnixLong(): Long = DateTime.nowUnixLong()
    }

    /** Number of milliseconds since the 00:00:00 UTC, Monday, 1 January 1 */
    val yearOneMillis: Double get() = value.yearOneMillis

    /** The local offset for this date for the timezone of the device */
    val localOffset: WTimezoneOffset get() = value.localOffset.wrapped

    /** Number of milliseconds since UNIX [EPOCH] as [Double] */
    val unixMillisDouble: Double get() = value.unixMillisDouble

    /** Number of milliseconds since UNIX [EPOCH] as [Long] */
    val unixMillisLong: Long get() = value.unixMillisLong

    /** The [WYear] part */
    val year: WYear get() = value.year.wrapped
    /** The [WYear] part as [Int] */
    val yearInt: Int get() = value.yearInt

    /** The [WMonth] part */
    val month: WMonth get() = value.month.wrapped
    /** The [WMonth] part as [Int] where January is represented as 0 */
    val month0: Int get() = value.month0
    /** The [WMonth] part as [Int] where January is represented as 1 */
    val month1: Int get() = value.month1

    /** Represents a couple of [WYear] and [WMonth] that has leap information and thus allows to get the number of days of that month */
    val yearMonth: WYearMonth get() = value.yearMonth.wrapped

    /** The [dayOfMonth] part */
    val dayOfMonth: Int get() = value.dayOfMonth

    /** The [dayOfWeek] part */
    val dayOfWeek: WDayOfWeek get() = value.dayOfWeek.wrapped
    /** The [dayOfWeek] part as [Int] */
    val dayOfWeekInt: Int get() = value.dayOfWeekInt

    /** The [dayOfYear] part */
    val dayOfYear: Int get() = value.dayOfYear

    /** The [hours] part */
    val hours: Int get() = value.hours
    /** The [minutes] part */
    val minutes: Int get() = value.minutes
    /** The [seconds] part */
    val seconds: Int get() = value.seconds
    /** The [milliseconds] part */
    val milliseconds: Int get() = value.milliseconds

    /** Returns a new local date that will match these components. */
    val localUnadjusted: WDateTimeTz get() = value.localUnadjusted.wrapped
    /** Returns a new local date that will match these components but with a different [offset]. */
    fun toOffsetUnadjusted(offset: WTimeSpan) = value.toOffsetUnadjusted(offset.value).wrapped
    /** Returns a new local date that will match these components but with a different [offset]. */
    fun toOffsetUnadjusted(offset: WTimezoneOffset) = value.toOffsetUnadjusted(offset.value).wrapped

    /** Returns this date with the local offset of this device. Components might change because of the offset. */
    val local: WDateTimeTz get() = value.local.wrapped
    /** Returns this date with a local offset. Components might change because of the [offset]. */
    fun toOffset(offset: WTimeSpan) = value.toOffset(offset.value).wrapped
    /** Returns this date with a local offset. Components might change because of the [offset]. */
    fun toOffset(offset: WTimezoneOffset) = value.toOffset(offset.value).wrapped
    /** Returns this date with a 0 offset. Components are equal. */
    val utc: WDateTimeTz get() = value.utc.wrapped

    /** Returns a [WDateTime] of [this] day with the hour at 00:00:00 */
    val dateDayStart get() = value.dateDayStart.wrapped
    /** Returns a [WDateTime] of [this] day with the hour at 23:59:59.999 */
    val dateDayEnd get() = value.dateDayEnd.wrapped

    /** Returns the quarter 1, 2, 3 or 4 */
    val quarter get() = value.quarter

    // startOf

    val startOfYear get() = value.startOfYear.wrapped
    val startOfMonth get() = value.startOfMonth.wrapped
    val startOfQuarter get() = value.startOfQuarter.wrapped
    fun startOfDayOfWeek(day: WDayOfWeek) = value.startOfDayOfWeek(day).wrapped
    val startOfWeek get() = value.startOfWeek.wrapped
    val startOfIsoWeek get() = value.startOfIsoWeek.wrapped
    val startOfDay get() = value.startOfDay.wrapped
    val startOfHour get() = value.startOfHour.wrapped
    val startOfMinute get() = value.startOfMinute.wrapped
    val startOfSecond get() = value.startOfSecond.wrapped

    // endOf

    val endOfYear get() = value.endOfYear.wrapped
    val endOfMonth get() = value.endOfMonth.wrapped
    val endOfQuarter get() = value.endOfQuarter.wrapped
    fun endOfDayOfWeek(day: WDayOfWeek) = value.endOfDayOfWeek(day).wrapped
    val endOfWeek get() = value.endOfWeek.wrapped
    val endOfIsoWeek get() = value.endOfIsoWeek.wrapped
    val endOfDay get() = value.endOfDay.wrapped
    val endOfHour get() = value.endOfHour.wrapped
    val endOfMinute get() = value.endOfMinute.wrapped
    val endOfSecond get() = value.endOfSecond.wrapped

    val date get() = value.date.wrapped
    val time get() = value.time.wrapped

    operator fun plus(delta: WMonthSpan): WDateTime = (this.value + delta.value).wrapped
    operator fun plus(delta: WDateTimeSpan): WDateTime = (this.value + delta.value).wrapped
    operator fun plus(delta: WTimeSpan): WDateTime = (this.value + delta.value).wrapped

    operator fun minus(delta: WMonthSpan): WDateTime = (this.value - delta.value).wrapped
    operator fun minus(delta: WDateTimeSpan): WDateTime = (this.value - delta.value).wrapped
    operator fun minus(delta: WTimeSpan): WDateTime = (this.value - delta.value).wrapped

    operator fun minus(other: WDateTime): WTimeSpan = (this.value - other.value).wrapped

    override fun compareTo(other: WDateTime): Int = this.value.compareTo(other.value)

    /** Constructs a new [WDateTime] after adding [deltaMonths] and [deltaMilliseconds] */
    fun add(deltaMonths: Int, deltaMilliseconds: Double): WDateTime = value.add(deltaMonths, deltaMilliseconds).wrapped

    /** Constructs a new [WDateTime] after adding [dateSpan] and [timeSpan] */
    fun add(dateSpan: WMonthSpan, timeSpan: WTimeSpan): WDateTime = value.add(dateSpan.value, timeSpan.value).wrapped

    fun copyDayOfMonth(
        year: WYear = this.year,
        month: WMonth = this.month,
        dayOfMonth: Int = this.dayOfMonth,
        hours: Int = this.hours,
        minutes: Int = this.minutes,
        seconds: Int = this.seconds,
        milliseconds: Int = this.milliseconds
    ) = DateTime(year.value, month, dayOfMonth, hours, minutes, seconds, milliseconds).wrapped

    /** Converts this date to String using [format] for representing it */
    fun format(format: DateFormat): String = value.format(format)
    /** Converts this date to String using [format] for representing it */
    fun format(format: String): String = value.format(format)

    /** Converts this date to String using [format] for representing it */
    fun toString(format: String): String = value.format(format)
    /** Converts this date to String using [format] for representing it */
    fun toString(format: DateFormat): String = value.format(format)

    /** Converts this date to String using the [DateFormat.DEFAULT_FORMAT] for representing it */
    override fun toString(): String = value.toString()
}

@KlockExperimental
fun max(a: WDateTime, b: WDateTime): WDateTime = max(a.value, b.value).wrapped
@KlockExperimental
fun min(a: WDateTime, b: WDateTime): WDateTime = min(a.value, b.value).wrapped
@KlockExperimental
fun WDateTime.clamp(min: WDateTime, max: WDateTime): WDateTime = this.value.clamp(min.value, max.value).wrapped
