package korlibs.time.wrapped

import korlibs.time.Month
import korlibs.time.Year
import korlibs.time.YearMonth
import korlibs.time.annotations.KlockExperimental
import korlibs.time.internal.Serializable
import korlibs.time.withMonth
import korlibs.time.withYear

@KlockExperimental
val YearMonth.wrapped get() = WYearMonth(this)

/**
 * Wrapped Version, that is not inline. You can use [value] to get the wrapped value class.
 *
 * Represents a couple of [year] and [month].
 */
@KlockExperimental
data class WYearMonth(val value: YearMonth) : Serializable {
    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /** Constructs a new [WYearMonth] from the [year] and [month] components. */
        operator fun invoke(year: WYear, month: WMonth) = YearMonth(year.value, month).wrapped
        /** Constructs a new [WYearMonth] from the [year] and [month] components. */
        operator fun invoke(year: Int, month: WMonth) = YearMonth(year, month).wrapped
        /** Constructs a new [WYearMonth] from the [year] and [month] components. */
        operator fun invoke(year: Int, month1: Int) = YearMonth(year, month1).wrapped
    }

    /** The [year] part. */
    val year: Year get() = value.year
    /** The [year] part as [Int]. */
    val yearInt: Int get() = value.yearInt

    /** The [month] part. */
    val month: WMonth get() = value.month
    /** The [month] part as [Int] where [Month.January] is 1. */
    val month1: Int get() = value.month1

    /** Days in this [month] of this [year]. */
    val days: Int get() = value.days
    /** Number of days since the start of the [year] to reach this [month]. */
    val daysToStart: Int get() = value.daysToStart
    /** Number of days since the start of the [year] to reach next [month]. */
    val daysToEnd: Int get() = value.daysToEnd

    operator fun plus(span: WMonthSpan): WYearMonth  = (this.value + span.value).wrapped
    operator fun minus(span: WMonthSpan): WYearMonth = (this.value - span.value).wrapped

    override fun toString(): String = "$month $yearInt"
}

/**
 * Creates a [WYearMonth] representing [this] year and this [month].
 */
@KlockExperimental
fun WYear.withMonth(month: WMonth) = this.value.withMonth(month).wrapped

/**
 * Creates a [WYearMonth] representing this [year] and [this] month.
 */
@KlockExperimental
fun WMonth.withYear(year: WYear) = this.withYear(year.value).wrapped