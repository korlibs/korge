package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocale

val KlockLocale.Companion.dutch get() = DutchKlockLocale

open class DutchKlockLocale : KlockLocale() {
	companion object : DutchKlockLocale()

	override val ISO639_1 = "nl"

	override val h12Marker = listOf("a.m.", "p.m.")

	override val firstDayOfWeek: DayOfWeek = DayOfWeek.Monday

	override val daysOfWeek = listOf(
		"zondag", "maandag", "dinsdag", "woensdag", "donderdag", "vrijdag", "zaterdag"
	)
	override val months = listOf(
		"januari", "februari", "maart", "april", "mei", "juni",
		"juli", "augustus", "september", "oktober", "november", "december"
	)

	override val formatDateTimeMedium = format("d MMM y HH:mm:ss")
	override val formatDateTimeShort = format("dd-MM-yy HH:mm")

	override val formatDateFull = format("EEEE d MMMM y")
	override val formatDateLong = format("d MMMM y")
	override val formatDateMedium = format("d MMM y")
	override val formatDateShort = format("dd-MM-y")

	override val formatTimeMedium = format("HH:mm:ss")
	override val formatTimeShort = format("HH:mm")
}
