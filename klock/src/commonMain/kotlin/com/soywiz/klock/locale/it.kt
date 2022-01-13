package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocale

val KlockLocale.Companion.italian get() = ItalianKlockLocale

open class ItalianKlockLocale : KlockLocale() {
	companion object : ItalianKlockLocale()

	override val ISO639_1 = "it"

	override val h12Marker = listOf("AM", "PM")

	override val firstDayOfWeek: DayOfWeek = DayOfWeek.Monday

	override val daysOfWeek = listOf(
		"domenica", "lunedì", "martedì", "mercoledì", "giovedì", "venerdì", "sabato"
	)
	override val months = listOf(
		"gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
		"luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre"
	)

	override val formatDateTimeMedium = format("dd MMM y HH:mm:ss")
	override val formatDateTimeShort = format("dd/MM/yy HH:mm")

	override val formatDateFull = format("EEEE d MMMM y")
	override val formatDateLong = format("d MMMM y")
	override val formatDateMedium = format("dd MMM y")
	override val formatDateShort = format("dd/MM/yy")

	override val formatTimeMedium = format("HH:mm:ss")
	override val formatTimeShort = format("HH:mm")
}
