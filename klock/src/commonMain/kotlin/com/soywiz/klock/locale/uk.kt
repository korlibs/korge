package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocale

val KlockLocale.Companion.ukrainian get() = UkrainianKlockLocale

open class UkrainianKlockLocale : KlockLocale() {
	companion object : UkrainianKlockLocale()

	override val ISO639_1 = "uk"
	override val h12Marker = listOf("ДП", "ПП")

	override val firstDayOfWeek: DayOfWeek = DayOfWeek.Monday

    override val daysOfWeek = listOf(
        "неділя", "понеділок", "вівторок", "середа", "четвер", "п'ятниця", "субота"
    )

    override val daysOfWeekShort = listOf(
        "нд", "пн", "вт", "ср", "чт", "пт", "сб"
    )

	override val months = listOf(
		"січня", "лютого", "березня", "квітня", "травня", "червня",
		"липня", "серпня", "вересня", "жовтня", "листопада", "грудня"
	)

	override val formatDateTimeMedium = format("d MMM y р. H:mm:ss")
	override val formatDateTimeShort = format("dd.MM.y H:mm")

	override val formatDateFull = format("EEEE, d MMMM y р.")
	override val formatDateLong = format("d MMMM y р.")
	override val formatDateMedium = format("d MMM y р.")
	override val formatDateShort = format("dd.MM.y")

	override val formatTimeMedium = format("H:mm:ss")
	override val formatTimeShort = format("H:mm")
}
