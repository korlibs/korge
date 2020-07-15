package com.soywiz.klock

import com.soywiz.klock.internal.Serializable

/** [DateTime] with an associated [TimezoneOffset] */
class DateTimeTz private constructor(
    /** The [adjusted] part */
    private val adjusted: DateTime,
    /** The [offset] part */
    val offset: TimezoneOffset
) : Comparable<DateTimeTz>, Serializable {
    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /** Creates a new [DateTimeTz] with the [utc] date and an [offset]. The [utc] components will be the same as this independently on the [offset]. */
        fun local(local: DateTime, offset: TimezoneOffset) = DateTimeTz(local, offset)

        /** Creates a new [DateTimeTz] with the [utc] date and an [offset]. The [utc] components might be different depending on the [offset]. */
        fun utc(utc: DateTime, offset: TimezoneOffset) = DateTimeTz(utc + offset.time, offset)

        /** Creates a new local [DateTimeTz] from a [unix] time */
        fun fromUnixLocal(unix: Long): DateTimeTz = fromUnixLocal(unix.toDouble())
        /** Creates a new local [DateTimeTz] from a [unix] time */
        fun fromUnixLocal(unix: Double): DateTimeTz = DateTime(unix).localUnadjusted

        /** Returns the current local [DateTimeTz] */
        fun nowLocal(): DateTimeTz = DateTime.now().local
    }

    /** Returns a new UTC date that will match these components without being the same time */
    val local: DateTime get() = adjusted

    /** Returns a new UTC date that might not match these components but it is the same time as UTC */
    val utc: DateTime get() = (adjusted - offset.time)

    /** The [Year] part */
    val year: Year get() = adjusted.year
    /** The [Year] part as [Int] */
    val yearInt: Int get() = adjusted.yearInt

    /** The [Month] part */
    val month: Month get() = adjusted.month
    /** The [Month] part as [Int] where January is represented as 0 */
    val month0: Int get() = adjusted.month0
    /** The [Month] part as [Int] where January is represented as 1 */
    val month1: Int get() = adjusted.month1

    /** Represents a couple of [Year] and [Month] that has leap information and thus allows to get the number of days of that month */
    val yearMonth: YearMonth get() = adjusted.yearMonth

    /** The [dayOfMonth] part */
    val dayOfMonth: Int get() = adjusted.dayOfMonth

    /** The [dayOfWeek] part */
    val dayOfWeek: DayOfWeek get() = adjusted.dayOfWeek
    /** The [dayOfWeek] part as [Int] */
    val dayOfWeekInt: Int get() = adjusted.dayOfWeekInt

    /** The [dayOfYear] part */
    val dayOfYear: Int get() = adjusted.dayOfYear

    /** The [hours] part */
    val hours: Int get() = adjusted.hours
    /** The [minutes] part */
    val minutes: Int get() = adjusted.minutes
    /** The [seconds] part */
    val seconds: Int get() = adjusted.seconds
    /** The [milliseconds] part */
    val milliseconds: Int get() = adjusted.milliseconds

    /** Constructs this local date with a new [offset] without changing its components */
    fun toOffsetUnadjusted(offset: TimeSpan) = toOffsetUnadjusted(offset.offset)
    /** Constructs this local date with a new [offset] without changing its components */
    fun toOffsetUnadjusted(offset: TimezoneOffset) = DateTimeTz.local(this.local, offset)

    /** Constructs this local date by adding an additional [offset] without changing its components */
    fun addOffsetUnadjusted(offset: TimeSpan) = addOffsetUnadjusted(offset.offset)
    /** Constructs this local date by adding an additional [offset] without changing its components */
    fun addOffsetUnadjusted(offset: TimezoneOffset) = DateTimeTz.local(this.local, (this.offset.time + offset.time).offset)

    /** Constructs the UTC part of this date with a new [offset] */
    fun toOffset(offset: TimeSpan) = toOffset(offset.offset)
    /** Constructs the UTC part of this date with a new [offset] */
    fun toOffset(offset: TimezoneOffset) = DateTimeTz.utc(this.utc, offset)

    /** Constructs the UTC part of this date by adding an additional [offset] */
    fun addOffset(offset: TimeSpan) = addOffset(offset.offset)
    /** Constructs the UTC part of this date by adding an additional [offset] */
    fun addOffset(offset: TimezoneOffset) = DateTimeTz.utc(this.utc, (this.offset.time + offset.time).offset)

    /** Constructs a new [DateTimeTz] after adding [dateSpan] and [timeSpan] */
    fun add(dateSpan: MonthSpan, timeSpan: TimeSpan): DateTimeTz = DateTimeTz(adjusted.add(dateSpan, timeSpan), offset)

    operator fun plus(delta: MonthSpan) = add(delta, 0.milliseconds)
    operator fun plus(delta: DateTimeSpan) = add(delta.monthSpan, delta.timeSpan)
    operator fun plus(delta: TimeSpan) = add(0.months, delta)

    operator fun minus(delta: MonthSpan) = this + (-delta)
    operator fun minus(delta: DateTimeSpan) = this + (-delta)
    operator fun minus(delta: TimeSpan) = this + (-delta)

    operator fun minus(other: DateTimeTz) = (this.utc.unixMillisDouble - other.utc.unixMillisDouble).milliseconds

    override fun hashCode(): Int = this.local.hashCode() + offset.totalMinutesInt
    override fun equals(other: Any?): Boolean = other is DateTimeTz && this.utc.unixMillisDouble == other.utc.unixMillisDouble
    override fun compareTo(other: DateTimeTz): Int = this.utc.unixMillis.compareTo(other.utc.unixMillis)

    /** Converts this date to String using [format] for representing it */
    fun format(format: DateFormat): String = format.format(this)
    /** Converts this date to String using [format] for representing it */
    fun format(format: String): String = DateFormat(format).format(this)
    /** Converts this date to String using [format] for representing it */
    fun toString(format: DateFormat): String = format.format(this)
    /** Converts this date to String using [format] for representing it */
    fun toString(format: String): String = DateFormat(format).format(this)

    /** Converts this date to String using the [DateFormat.DEFAULT_FORMAT] for representing it */
    override fun toString(): String = DateFormat.DEFAULT_FORMAT.format(this)
}
