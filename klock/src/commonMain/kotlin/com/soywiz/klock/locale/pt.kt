package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocale

val KlockLocale.Companion.portuguese get() = PortugueseKlockLocale

open class PortugueseKlockLocale : KlockLocale() {
	companion object : PortugueseKlockLocale()

	override val ISO639_1 = "pt"

	override val h12Marker = listOf("AM", "PM")

	override val firstDayOfWeek: DayOfWeek = DayOfWeek.Monday

	override val daysOfWeek = listOf(
		"domingo",
		"segunda-feira",
		"terça-feira",
		"quarta-feira",
		"quinta-feira",
		"sexta-feira",
		"sábado"
	)
	override val months = listOf(
		"janeiro",
		"fevereiro",
		"março",
		"abril",
		"maio",
		"junho",
		"julho",
		"agosto",
		"setembro",
		"outubro",
		"novembro",
		"dezembro"
	)

	override val formatDateTimeMedium = format("d 'de' MMM 'de' y HH:mm:ss")
	override val formatDateTimeShort = format("dd/MM/y HH:mm")

	override val formatDateFull = format("EEEE, d 'de' MMMM 'de' y")
	override val formatDateLong = format("d 'de' MMMM 'de' y")
	override val formatDateMedium = format("d 'de' MMM 'de' y")
	override val formatDateShort = format("dd/MM/y")
}
