package korlibs.time.locale

import korlibs.time.KlockLocale
import korlibs.time.KlockLocaleContext
import korlibs.time.DayOfWeek

val KlockLocale.Companion.german get() = GermanKlockLocale

open class GermanKlockLocale : KlockLocale() {

    companion object {
        private val NUM_TO_ORDINAL: Array<String> = arrayOf(
            "index zero is invalid",
            "erste",
            "zweite",
            "dritte",
            "vierte",
            "fünfte",
            "sechste",
            "siebte",
            "achte",
            "neunte",
            "zehnte",
            "elfte",
            "zwölfte",
            "dreizehnte",
            "vierzehnte",
            "fünfzehnte",
            "sechzehnte",
            "siebzehnte",
            "achtzehnte",
            "neunzehnte",
            "zwanzigste",
            "einundzwanzigste",
            "zweiundzwanzigste",
            "dreiundzwanzigste",
            "vierundzwanzigste",
            "fünfundzwanzigste",
            "sechsundzwanzigste",
            "siebenundzwanzigste",
            "achtundzwanzigste",
            "neunundzwanzigste",
            "dreißigste",
            "einunddreißigste"
        )
        private val ORDINAL_TO_NUM: Map<String, Int> = NUM_TO_ORDINAL.mapIndexed { index, s -> s to index }.toMap()
    }

    override fun getOrdinalByDay(day: Int, context: KlockLocaleContext): String {
        return NUM_TO_ORDINAL.getOrNull(day) ?: error("Invalid numeral")
    }

    override fun getDayByOrdinal(ordinal: String): Int {
        return ORDINAL_TO_NUM[ordinal.lowercase()] ?: -1
    }

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

    enum class GermanOrdinal(val value: Int) {
        ERSTE(1),
        ZWEITE(2),
        DRITTE(3),
        VIERTE(4),
        FUENFTE(5),
        SECHSTE(6),
        SIEBTE(7),
        ACHTE(8),
        NEUNTE(9),
        ZEHNTE(10),
        ELFTE(11),
        ZWOELFTE(12),
        DREIZEHNTE(13),
        VIERZEHNTE(14),
        FUENFZEHNTE(15),
        SECHZEHNTE(16),
        SIEBZEHNTE(17),
        ACHTZEHNTE(18),
        NEUNZEHNTE(19),
        ZWANZIGSTE(20),
        EINUNDZWANZIGSTE(21),
        ZWEIUNDZWANZIGSTE(22),
        DREIUNDZWANZIGSTE(23),
        VIERUNDZWANZIGSTE(24),
        FUENFUNDZWANZIGSTE(25),
        SECHSUNDZWANZIGSTE(26),
        SIEBENUNDZWANZIGSTE(27),
        ACHTUNDZWANZIGSTE(28),
        NEUNUNDZWANZIGSTE(29),
        DREISSIGSTE(30),
        EINUNDDREISSIGSTE(31);

        fun getOrdinalInGerman(): String {
            var ordinal = this.name.lowercase()
            ordinal = ordinal.replace("ae", "ä")
            ordinal = ordinal.replace("oe", "ö")
            ordinal = ordinal.replace("ue", "ü")
            return ordinal
        }
    }
}
