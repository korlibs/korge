package com.soywiz.klock

import com.soywiz.klock.internal.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents a couple of [year] and [month].
 *
 * It is packed in a value class wrapping an Int to prevent allocations.
 */
@JvmInline
value class YearMonth(internal val internalPackedInfo: Int) : Serializable {
    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /** Constructs a new [YearMonth] from the [year] and [month] components. */
        operator fun invoke(year: Year, month: Month) = YearMonth(year.year, month.index1)
        /** Constructs a new [YearMonth] from the [year] and [month] components. */
        operator fun invoke(year: Int, month: Month) = YearMonth(year, month.index1)
        /** Constructs a new [YearMonth] from the [year] and [month] components. */
        operator fun invoke(year: Int, month1: Int) = YearMonth((year shl 4) or (month1 and 15))
    }

    /** The [year] part. */
    val year: Year get() = Year(yearInt)
    /** The [year] part as [Int]. */
    val yearInt: Int get() = internalPackedInfo ushr 4

    /** The [month] part. */
    val month: Month get() = Month[month1]
    /** The [month] part as [Int] where [Month.January] is 1. */
    val month1: Int get() = internalPackedInfo and 15

    /** Days in this [month] of this [year]. */
    val days: Int get() = month.days(year)
    /** Number of days since the start of the [year] to reach this [month]. */
    val daysToStart: Int get() = month.daysToStart(year)
    /** Number of days since the start of the [year] to reach next [month]. */
    val daysToEnd: Int get() = month.daysToEnd(year)

    operator fun plus(span: MonthSpan): YearMonth {
        val newMonth = this.month1 + span.months
        val yearAdjust = when {
            newMonth > 12 -> +1
            newMonth < 1 -> -1
            else -> 0
        }
        return YearMonth(Year(this.yearInt + span.years + yearAdjust), Month[newMonth])
    }

    operator fun minus(span: MonthSpan): YearMonth = this + (-span)

    override fun toString(): String = "$month $yearInt"
}

/**
 * Creates a [YearMonth] representing [this] year and this [month].
 */
fun Year.withMonth(month: Month) = YearMonth(this, month)

/**
 * Creates a [YearMonth] representing this [year] and [this] month.
 */
fun Month.withYear(year: Year) = YearMonth(year, this)
