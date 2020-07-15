package com.soywiz.klock.wrapped

import com.soywiz.klock.*
import com.soywiz.klock.annotations.*
import com.soywiz.klock.internal.Serializable

@KlockExperimental
val MonthSpan.wrapped get() = WMonthSpan(this)

/**
 * Wrapped Version, that is not inline. You can use [value] to get the wrapped inline class.
 *
 * Represents a number of years and months temporal distance.
 */
@KlockExperimental
class WMonthSpan(val value: MonthSpan) : Comparable<WMonthSpan>, Serializable {
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
    operator fun div(times: Double) = (value / times).wrapped

    operator fun times(times: Int) = this * times.toDouble()
    operator fun div(times: Int) = this / times.toDouble()

    @Deprecated("Boxing on Kotlin/Native", ReplaceWith("this * times.toDouble()"))
    inline operator fun times(times: Number) = this * times.toDouble()
    @Deprecated("Boxing on Kotlin/Native", ReplaceWith("this / times.toDouble()"))
    inline operator fun div(times: Number) = this / times.toDouble()

    override fun compareTo(other: WMonthSpan): Int = this.value.compareTo(other.value)

    /** Converts this time to String formatting it like "20Y", "20Y 1M", "1M" or "0M". */
    override fun toString(): String = value.toString()
}
