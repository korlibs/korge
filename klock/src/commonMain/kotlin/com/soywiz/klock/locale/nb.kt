package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocale

val KlockLocale.Companion.norwegian get() = NorwegianKlockLocale

open class NorwegianKlockLocale : KlockLocale() {

    companion object : NorwegianKlockLocale()

    override val ISO639_1 = "nb"

    override val firstDayOfWeek = DayOfWeek.Monday

    override val daysOfWeek = listOf(
        "søndag", "mandag", "tirsdag", "onsdag", "torsdag", "fredag", "lørdag"
    )

    override val months = listOf(
        "januar",
        "februar",
        "mars",
        "april",
        "mai",
        "juni",
        "juli",
        "august",
        "september",
        "oktober",
        "november",
        "desember"
    )

    override val formatDateTimeMedium = format("dd.MM.y HH:mm:ss")
    override val formatDateTimeShort = format("dd.MM.yy HH:mm")

    override val formatDateFull = format("EEEE, d. MMMM y")
    override val formatDateLong = format("d. MMMM y")
    override val formatDateMedium = format("dd.MM.y")
    override val formatDateShort = format("dd.MM.yy")

    override val formatTimeMedium = format("HH:mm:ss")
    override val formatTimeShort = format("HH:mm")

}
