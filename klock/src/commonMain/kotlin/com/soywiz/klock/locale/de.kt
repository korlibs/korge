package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocale

val KlockLocale.Companion.german get() = GermanKlockLocale

open class GermanKlockLocale : KlockLocale() {
	companion object : GermanKlockLocale()

	override val ISO639_1 = "de"

	override val h12Marker = listOf("vorm.", "nachm.")

	override val firstDayOfWeek: DayOfWeek = DayOfWeek.Monday

	override val daysOfWeek = listOf(
		"sonntag", "montag", "dienstag", "mittwoch", "donnerstag", "freitag", "samstag"
	)
	override val months = listOf(
		"januar", "februar", "märz", "april", "mai", "juni",
		"juli", "august", "september", "oktober", "november", "dezember"
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
