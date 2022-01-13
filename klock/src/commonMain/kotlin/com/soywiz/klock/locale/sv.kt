package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocale

val KlockLocale.Companion.swedish get() = SwedishKlockLocale

open class SwedishKlockLocale : KlockLocale() {

    companion object : SwedishKlockLocale()

    override val ISO639_1: String = "sv"

    override val firstDayOfWeek: DayOfWeek = DayOfWeek.Monday

    override val daysOfWeek: List<String> = listOf(
        "söndag", "måndag", "tisdag", "onsdag", "torsdag", "fredag", "lördag"
    )

    override val months: List<String> = listOf(
        "januari",
        "februari",
        "mars",
        "april",
        "maj",
        "juni",
        "juli",
        "augusti",
        "september",
        "oktober",
        "november",
        "december"
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
