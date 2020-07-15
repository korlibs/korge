package com.soywiz.klock

import com.soywiz.klock.internal.Serializable

/**
 * Creates a [MonthSpan] representing these years.
 */
inline val Int.years get() = MonthSpan(12 * this)

/**
 * Creates a [MonthSpan] representing these months.
 */
inline val Int.months get() = MonthSpan(this)

/**
 * Represents a number of years and months temporal distance.
 */
inline class MonthSpan(
    /** Total months of this [MonthSpan] as integer */
    val totalMonths: Int
) : Comparable<MonthSpan>, Serializable {
    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L
    }

    operator fun unaryMinus() = MonthSpan(-totalMonths)
    operator fun unaryPlus() = MonthSpan(+totalMonths)

    operator fun plus(other: TimeSpan) = DateTimeSpan(this, other)
    operator fun plus(other: MonthSpan) = MonthSpan(totalMonths + other.totalMonths)
    operator fun plus(other: DateTimeSpan) = DateTimeSpan(other.monthSpan + this, other.timeSpan)

    operator fun minus(other: TimeSpan) = this + -other
    operator fun minus(other: MonthSpan) = this + -other
    operator fun minus(other: DateTimeSpan) = this + -other

    operator fun times(times: Double) = MonthSpan((totalMonths * times).toInt())
    operator fun div(times: Double) = MonthSpan((totalMonths / times).toInt())

    operator fun times(times: Int) = this * times.toDouble()
    operator fun div(times: Int) = this / times.toDouble()

    // @TODO: Kotlin/Native is not removing boxes on inline + Number
    @Deprecated("Boxing on Kotlin/Native", ReplaceWith("this * times.toDouble()"))
    inline operator fun times(times: Number) = this * times.toDouble()
    @Deprecated("Boxing on Kotlin/Native", ReplaceWith("this / times.toDouble()"))
    inline operator fun div(times: Number) = this / times.toDouble()

    override fun compareTo(other: MonthSpan): Int = this.totalMonths.compareTo(other.totalMonths)

    /** Converts this time to String formatting it like "20Y", "20Y 1M", "1M" or "0M". */
    override fun toString(): String {
        val list = arrayListOf<String>()
        if (years != 0) list.add("${years}Y")
        if (months != 0 || years == 0) list.add("${months}M")
        return list.joinToString(" ")
    }
}

/** Total years of this [MonthSpan] as double (might contain decimals) */
val MonthSpan.totalYears: Double get() = totalMonths.toDouble() / 12.0

/** Years part of this [MonthSpan] as integer */
val MonthSpan.years: Int get() = totalMonths / 12

/** Months part of this [MonthSpan] as integer */
val MonthSpan.months: Int get() = totalMonths % 12
