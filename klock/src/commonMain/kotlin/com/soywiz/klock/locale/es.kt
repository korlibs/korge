package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocale

val KlockLocale.Companion.spanish get() = SpanishKlockLocale

open class SpanishKlockLocale : KlockLocale() {
	companion object : SpanishKlockLocale()

	override val ISO639_1 = "es"

	override val h12Marker = listOf("a.m.", "p.m.")

	override val firstDayOfWeek: DayOfWeek = DayOfWeek.Monday

	override val daysOfWeek = listOf(
		"domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"
	)
	override val months = listOf(
		"enero", "febrero", "marzo", "abril", "mayo", "junio",
		"julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
	)

	override val formatDateTimeMedium = format("dd/MM/yyyy HH:mm:ss")
	override val formatDateTimeShort = format("dd/MM/yy HH:mm")

	override val formatDateFull = format("EEEE, d 'de' MMMM 'de' y")
	override val formatDateLong = format("d 'de' MMMM 'de' y")
	override val formatDateMedium = format("dd/MM/yyyy")
	override val formatDateShort = format("dd/MM/yy")

	override val formatTimeMedium = format("HH:mm:ss")
	override val formatTimeShort = format("HH:mm")
}
