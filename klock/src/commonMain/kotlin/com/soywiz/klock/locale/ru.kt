package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocaleContext
import com.soywiz.klock.KlockLocale
import com.soywiz.klock.KlockLocaleGender

val KlockLocale.Companion.russian get() = RussianKlockLocale

open class RussianKlockLocale : KlockLocale() {
	companion object : RussianKlockLocale()

    override fun getOrdinalByDay(day: Int, context: KlockLocaleContext): String = when (context.gender) {
        KlockLocaleGender.Masculine -> "$day-й"
        // if feminine is ever added to KlockLocaleGender, don't forget to add implementation here: "$day-я"
        else -> "$day-е"
    }

    override fun getDayByOrdinal(ordinal: String): Int = ordinal.substringBeforeLast('-').toInt()

	override val ISO639_1 = "ru"

	override val h12Marker = listOf("ДП", "ПП")

	override val firstDayOfWeek: DayOfWeek = DayOfWeek.Monday

	override val daysOfWeek = listOf(
		"воскресенье", "понедельник", "вторник", "среда", "четверг", "пятница", "суббота"
	)

	override val daysOfWeekShort = listOf(
		"вс", "пн", "вт", "ср", "чт", "пт", "сб"
	)

	override val months = listOf(
		"января", "февраля", "марта", "апреля", "мая", "июня",
		"июля", "августа", "сентября", "октября", "ноября", "декабря"
	)

	override val formatDateTimeMedium = format("d MMM y г. H:mm:ss")
	override val formatDateTimeShort = format("dd.MM.y H:mm")

	override val formatDateFull = format("EEEE, d MMMM y г.")
	override val formatDateLong = format("d MMMM y г.")
	override val formatDateMedium = format("d MMM y г.")
	override val formatDateShort = format("dd.MM.y")

	override val formatTimeMedium = format("H:mm:ss")
	override val formatTimeShort = format("H:mm")
}
