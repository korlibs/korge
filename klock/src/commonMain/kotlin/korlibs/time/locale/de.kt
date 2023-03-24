package korlibs.time.locale

import korlibs.time.DayOfWeek
import korlibs.time.KlockLocale

val KlockLocale.Companion.german get() = GermanKlockLocale

open class GermanKlockLocale : KlockLocale() {
	companion object : GermanKlockLocale()

	override val ISO639_1 = "de"

	override val h12Marker = listOf("vorm.", "nachm.")

	override val firstDayOfWeek: DayOfWeek = DayOfWeek.Monday

	override val daysOfWeek = listOf(
		"Sonntag", "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag"
	)
	override val months = listOf(
		"Januar", "Februar", "März", "April", "Mai", "Juni",
		"Juli", "August", "September", "Oktober", "November", "Dezember"
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