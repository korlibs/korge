package com.soywiz.klock

import com.soywiz.klock.internal.Serializable
import com.soywiz.klock.internal.umod

/** Represents the day of the week. [Sunday], [Monday], [Tuesday], [Wednesday], [Thursday], [Friday], [Saturday]. */
enum class DayOfWeek(
    /** 0: [Sunday], 1: [Monday], 2: [Tuesday], 3: [Wednesday], 4: [Thursday], 5: [Friday], 6: [Saturday] */
    val index0: Int
) : Serializable {
    Sunday(0),
    Monday(1),
    Tuesday(2),
    Wednesday(3),
    Thursday(4),
    Friday(5),
    Saturday(6);

    /**
     * 1: [Sunday], 2: [Monday], 3: [Tuesday], 4: [Wednesday], 5: [Thursday], 6: [Friday], 7: [Saturday]
     */
    val index1 get() = index0 + 1

    val index0Sunday get() = index0
    val index1Sunday get() = index1

    /** 0: [Monday], 1: [Tuesday], 2: [Wednesday], 3: [Thursday], 4: [Friday], 5: [Saturday], 6: [Sunday] */
    val index0Monday get() = (index0 - 1) umod 7

    /** 1: [Monday], 2: [Tuesday], 3: [Wednesday], 4: [Thursday], 5: [Friday], 6: [Saturday], 7: [Sunday] */
    val index1Monday get() = index0Monday + 1

    /** Returns if this day of the week is weekend for a specific [locale]  */
    fun isWeekend(locale: KlockLocale = KlockLocale.default) = locale.isWeekend(this)

    val localName get() = localName(KlockLocale.default)
    fun localName(locale: KlockLocale) = locale.daysOfWeek[index0]

    val localShortName get() = localShortName(KlockLocale.default)
    fun localShortName(locale: KlockLocale) = locale.daysOfWeekShort[index0]

    val prev get() = DayOfWeek[index0 - 1]
    val next get() = DayOfWeek[index0 + 1]

    fun prev(offset: Int = 1) = DayOfWeek[index0 - offset]
    fun next(offset: Int = 1) = DayOfWeek[index0 + offset]

    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /**
         * Number of days in a wekk.
         */
        const val Count = 7

        private val BY_INDEX0 = values()

        /**
         * 0: [Sunday], 1: [Monday], 2: [Tuesday], 3: [Wednesday], 4: [Thursday], 5: [Friday], 6: [Saturday]
         */
        operator fun get(index0: Int) = BY_INDEX0[index0 umod 7]

        /**
         * Returns the first day of the week for a specific [locale].
         */
        fun firstDayOfWeek(locale: KlockLocale = KlockLocale.default) = locale.firstDayOfWeek
    }
}
