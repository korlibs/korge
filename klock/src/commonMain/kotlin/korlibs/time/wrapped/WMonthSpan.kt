package korlibs.time.wrapped

import korlibs.time.MonthSpan
import korlibs.time.annotations.KlockExperimental
import korlibs.time.internal.Serializable
import korlibs.time.months
import korlibs.time.totalYears
import korlibs.time.years

@KlockExperimental
val MonthSpan.wrapped get() = WMonthSpan(this)

/**
 * Wrapped Version, that is not inline. You can use [value] to get the wrapped value class.
 *
 * Represents a number of years and months temporal distance.
 */
@KlockExperimental
data class WMonthSpan(val value: MonthSpan) : Comparable<WMonthSpan>, Serializable {
    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L
    }

    val totalMonths: Int get() = value.totalMonths
    /** Total years of this [WMonthSpan] as double (might contain decimals) */
    val totalYears: Double get() = value.totalYears
    /** Years part of this [WMonthSpan] as integer */
    val years: Int get() = value.years
    /** Months part of this [WMonthSpan] as integer */
    val months: Int get() = value.months

    operator fun unaryMinus() = (-value).wrapped
    operator fun unaryPlus() = (+value).wrapped

    operator fun plus(other: WTimeSpan) = (this.value + other.value).wrapped
    operator fun plus(other: WMonthSpan) = (this.value + other.value).wrapped
    operator fun plus(other: WDateTimeSpan) = (this.value + other.value).wrapped

    operator fun minus(other: WTimeSpan) = (this.value - other.value).wrapped
    operator fun minus(other: WMonthSpan) = (this.value - other.value).wrapped
    operator fun minus(other: WDateTimeSpan) = (this.value - other.value).wrapped

    operator fun times(times: Double) = (value * times).wrapped
    operator fun times(times: Float) = this * times.toDouble()
    operator fun times(times: Int) = this * times.toDouble()

    operator fun div(times: Double) = (value / times).wrapped
    operator fun div(times: Float) = this / times.toDouble()
    operator fun div(times: Int) = this / times.toDouble()

    override fun compareTo(other: WMonthSpan): Int = this.value.compareTo(other.value)

    /** Converts this time to String formatting it like "20Y", "20Y 1M", "1M" or "0M". */
    override fun toString(): String = value.toString()
}