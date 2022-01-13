package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocale

val KlockLocale.Companion.french get() = FrenchKlockLocale

open class FrenchKlockLocale : KlockLocale() {
	companion object : FrenchKlockLocale()

	override val ISO639_1 = "fr"

	override val h12Marker = listOf("AM", "PM")

	override val firstDayOfWeek: DayOfWeek = DayOfWeek.Monday

	override val daysOfWeek = listOf(
		"dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"
	)
	override val months = listOf(
		"janvier",
		"février",
		"mars",
		"avril",
		"mai",
		"juin",
		"juillet",
		"août",
		"septembre",
		"octobre",
		"novembre",
		"décembre"
	)

	override val formatDateTimeMedium = format("d MMM y HH:mm:ss")
	override val formatDateTimeShort = format("dd/MM/y HH:mm")

	override val formatDateFull = format("EEEE d MMMM y")
	override val formatDateLong = format("d MMMM y")
	override val formatDateMedium = format("d MMM y")
	override val formatDateShort = format("dd/MM/y")

	override val formatTimeMedium = format("HH:mm:ss")
	override val formatTimeShort = format("HH:mm")
}
