package korlibs.time.locale

import korlibs.time.*

val KlockLocale.Companion.turkish get() = TurkishKlockLocale

open class TurkishKlockLocale : KlockLocale() {
    companion object : TurkishKlockLocale()

    override val ISO639_1 = "tr"

    override val h12Marker = listOf("ÖÖ", "ÖS")

    override val firstDayOfWeek: DayOfWeek = DayOfWeek.Monday

    override val daysOfWeek = listOf(
        "Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi"
    )

    override val daysOfWeekShort = listOf("Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")

    override val months = listOf(
        "Ocak",
        "Şubat",
        "Mart",
        "Nisan",
        "Mayıs",
        "Haziran",
        "Temmuz",
        "Ağustos",
        "Eylül",
        "Ekim",
        "Kasım",
        "Aralık"
    )

    override val monthsShort = listOf(
        "Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"
    )

    override val formatDateTimeMedium = format("dd MMM yyyy HH:mm:ss")
    override val formatDateTimeShort = format("dd.MM.yyyy HH:mm")

    override val formatDateFull = format("dd MMMM yyyy EEEE")
    override val formatDateLong = format("d MMMM yyyy")
    override val formatDateMedium = format("dd MMM yyyy")
    override val formatDateShort = format("dd.MM.yyyy")

    override val formatTimeMedium = format("HH:mm:ss")
    override val formatTimeShort = format("HH:mm")
}
