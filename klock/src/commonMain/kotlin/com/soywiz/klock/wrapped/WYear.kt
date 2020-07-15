package com.soywiz.klock.wrapped

import com.soywiz.klock.*
import com.soywiz.klock.annotations.*
import com.soywiz.klock.internal.Serializable

@KlockExperimental
val Year.wrapped get() = WYear(this)

/**
 * Wrapped Version, that is not inline. You can use [value] to get the wrapped inline class.
 *
 * Represents a Year in a typed way.
 *
 * A year is a set of 365 days or 366 for leap years.
 * It is the time it takes the earth to fully orbit the sun.
 *
 * The integrated model is capable of determine if a year is leap for years 1 until 9999 inclusive.
 */
@KlockExperimental
data class WYear(val value: Year) : Comparable<WYear>, Serializable {
    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /**
         * Creates a Year instance checking that the year is between 1 and 9999 inclusive.
         *
         * It throws a [DateException] if the year is not in the 1..9999 range.
         */
        fun checked(year: Int) = Year(year).wrapped

        /**
         * Determines if a year is leap checking that the year is between 1..9999 or throwing a [DateException] when outside that range.
         */
        fun isLeapChecked(year: Int): Boolean = Year.isLeapChecked(year)

        /**
         * Determines if a year is leap. The model works for years between 1..9999. Outside this range, the result might be invalid.
         */
        fun isLeap(year: Int): Boolean = Year.isLeap(year)

        /**
         * Computes the year from the number of days since 0001-01-01.
         */
        fun fromDays(days: Int): WYear = Year.fromDays(days).wrapped

        /**
         * Get the number of days of a year depending on being leap or not.
         * Normal, non leap years contain 365 days, while leap ones 366.
         */
        fun days(isLeap: Boolean) = Year.days(isLeap)

        /**
         * Return the number of leap years that happened between 1 and the specified [year].
         */
        fun leapCountSinceOne(year: Int): Int = Year.leapCountSinceOne(year)

        /**
         * Number of days since 1 and the beginning of the specified [year].
         */
        fun daysSinceOne(year: Int): Int = Year.daysSinceOne(year)

        /**
         * Number of days in a normal year.
         */
        const val DAYS_COMMON = Year.DAYS_COMMON

        /**
         * Number of days in a leap year.
         */
        const val DAYS_LEAP = Year.DAYS_LEAP
    }

    /**
     * Determines if this year is leap checking that the year is between 1..9999 or throwing a [DateException] when outside that range.
     */
    val isLeapChecked get() = value.isLeapChecked

    /**
     * Determines if this year is leap. The model works for years between 1..9999. Outside this range, the result might be invalid.
     */
    val isLeap get() = value.isLeap

    /**
     * Total days of this year, 365 (non leap) [DAYS_COMMON] or 366 (leap) [DAYS_LEAP].
     */
    val days: Int get() = value.days

    /**
     * Number of leap years since the year 1 (without including this one)
     */
    val leapCountSinceOne: Int get() = value.leapCountSinceOne

    /**
     * Number of days since year 1 to reach this year
     */
    val daysSinceOne: Int get() = value.daysSinceOne

    /**
     * Compares two years.
     */
    override fun compareTo(other: WYear): Int = this.value.compareTo(other.value)

    operator fun plus(delta: Int): WYear = (value + delta).wrapped
    operator fun minus(delta: Int): WYear = (value - delta).wrapped
    operator fun minus(other: WYear): Int = value - other.value
}
