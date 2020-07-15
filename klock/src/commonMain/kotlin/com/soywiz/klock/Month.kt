package com.soywiz.klock

import com.soywiz.klock.internal.*
import kotlin.math.*

/** Represents one of the twelve months of the year. */
enum class Month(
    /** 1: [January], 2: [February], 3: [March], 4: [April], 5: [May], 6: [June], 7: [July], 8: [August], 9: [September], 10: [October], 11: [November], 12: [December] */
    val index1: Int,
    /** Number of days of this month in a common year */
    val daysCommon: Int,
    /** Number of days of this month in a leap year */
    val daysLeap: Int = daysCommon
) : Serializable {
    January(1, daysCommon = 31),
    February(2, daysCommon = 28, daysLeap = 29),
    March(3, daysCommon = 31),
    April(4, daysCommon = 30),
    May(5, daysCommon = 31),
    June(6, daysCommon = 30),
    July(7, daysCommon = 31),
    August(8, daysCommon = 31),
    September(9, daysCommon = 30),
    October(10, daysCommon = 31),
    November(11, daysCommon = 30),
    December(12, daysCommon = 31);

    /** 0: [January], 1: [February], 2: [March], 3: [April], 4: [May], 5: [June], 6: [July], 7: [August], 8: [September], 9: [October], 10: [November], 11: [December] */
    val index0: Int get() = index1 - 1

    /** Number of days in a specific month (28-31) depending whether the year is [leap] or not. */
    fun days(leap: Boolean): Int = if (leap) daysLeap else daysCommon
    /** Number of days in a specific month (28-31) depending whether the [year] or not. */
    fun days(year: Int): Int = days(Year(year).isLeap)
    /** Number of days in a specific month (28-31) depending whether the [year] or not. */
    fun days(year: Year): Int = days(year.isLeap)

    /** Number of days since the start of the [leap] year to reach this month. */
    fun daysToStart(leap: Boolean): Int = YEAR_DAYS(leap)[index0]
    /** Number of days since the start of the [year] to reach this month. */
    fun daysToStart(year: Int): Int = daysToStart(Year(year).isLeap)
    /** Number of days since the start of the [year] to reach this month. */
    fun daysToStart(year: Year): Int = daysToStart(year.isLeap)

    /** Number of days since the start of the [leap] year to reach next month. */
    fun daysToEnd(leap: Boolean): Int = YEAR_DAYS(leap)[index1]
    /** Number of days since the start of the [year] to reach next month. */
    fun daysToEnd(year: Int): Int = daysToEnd(Year(year).isLeap)
    /** Number of days since the start of the [year] to reach next month. */
    fun daysToEnd(year: Year): Int = daysToEnd(year.isLeap)

    /** Previous [Month]. */
    val previous: Month get() = this - 1
    /** Next [Month]. */
    val next: Month get() = this + 1

    operator fun plus(delta: Int): Month = Month[index1 + delta]
    operator fun minus(delta: Int): Month = Month[index1 - delta]

    operator fun minus(other: Month): Int = abs(this.index0 - other.index0)

    val localName get() = localName(KlockLocale.default)
    fun localName(locale: KlockLocale) = locale.months[index0]

    val localShortName get() = localShortName(KlockLocale.default)
    fun localShortName(locale: KlockLocale) = locale.monthsShort[index0]

    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /**
         * Number of months in a year (12).
         */
        const val Count = 12

        /** 1: [January], 2: [February], 3: [March], 4: [April], 5: [May], 6: [June], 7: [July], 8: [August], 9: [September], 10: [October], 11: [November], 12: [December] */
        operator fun invoke(index1: Int) = adjusted(index1)
        /** 1: [January], 2: [February], 3: [March], 4: [April], 5: [May], 6: [June], 7: [July], 8: [August], 9: [September], 10: [October], 11: [November], 12: [December] */
        operator fun get(index1: Int) = adjusted(index1)

        /**
         * Gets the [Month] from a month index where [January]=1 wrapping the index to valid values.
         *
         * For example 0 and 12=[December], 1 and 13=[January], -1 and 11=[November].
         */
        fun adjusted(index1: Int) = BY_INDEX0[(index1 - 1) umod 12]

        /**
         * Gets the [Month] from a month index where [January]=1 checking that the provided [index1] is valid between 1..12.
         */
        fun checked(index1: Int) = BY_INDEX0[index1.also { if (index1 !in 1..12) throw DateException("Month $index1 not in 1..12") } - 1]

        /**
         * Gets the [Month] of a [dayOfYear] in a [leap] year.
         *
         * Returns null if the year doesn't contain that [dayOfYear].
         */
        fun fromDayOfYear(dayOfYear: Int, leap: Boolean): Month? {
            val days = YEAR_DAYS(leap)
            val day0 = dayOfYear - 1
            val guess = day0 / 32

            if (guess in 0..11 && day0 in days[guess] until days[guess + 1]) return Month[guess + 1]
            if (guess in 0..10 && day0 in days[guess + 1] until days[guess + 2]) return Month[guess + 2]

            return null
        }

        /**
         * Gets the [Month] of a [dayOfYear] in the specified [year].
         *
         * Returns null if the year doesn't contain that [dayOfYear].
         */
        fun fromDayOfYear(dayOfYear: Int, year: Year): Month? = fromDayOfYear(dayOfYear, year.isLeap)

        private val BY_INDEX0 = values()
        private fun YEAR_DAYS(isLeap: Boolean): IntArray = if (isLeap) YEAR_DAYS_LEAP else YEAR_DAYS_COMMON
        private val YEAR_DAYS_LEAP = generateDaysToStart(leap = true)
        private val YEAR_DAYS_COMMON = generateDaysToStart(leap = false)

        private fun generateDaysToStart(leap: Boolean): IntArray {
            var total = 0
            return IntArray(13) {
                total += if (it == 0) 0 else BY_INDEX0[it - 1].days(leap)
                total
            }
        }
    }
}
