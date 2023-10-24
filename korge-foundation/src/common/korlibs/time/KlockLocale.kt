package korlibs.time

import korlibs.time.internal.substr
import kotlin.native.concurrent.ThreadLocal

private var KlockLocale_default: KlockLocale? = null

abstract class KlockLocale {
	abstract val ISO639_1: String
	abstract val daysOfWeek: List<String>
	abstract val months: List<String>
	abstract val firstDayOfWeek: DayOfWeek
    open val monthsShort: List<String> get() = months.map { it.substr(0, 3) }
    open val daysOfWeekShort: List<String> get() = daysOfWeek.map { it.substr(0, 3) }

    //private val daysOfWeekWithLocaleList: Array<DayOfWeekWithLocale> = Array(7) { DayOfWeekWithLocale(DayOfWeek[it], this) }

    //fun localizedDayOfWeek(dayOfWeek: DayOfWeek) = daysOfWeekWithLocaleList[dayOfWeek.index0]
    fun localizedDayOfWeek(dayOfWeek: DayOfWeek) = DayOfWeekWithLocale(DayOfWeek[dayOfWeek.index0], this)

    val daysOfWeekComparator get() = Comparator<DayOfWeek> { a, b ->
        a.index0Locale(this).compareTo(b.index0Locale(this))
    }

    open val ordinals get() = Array(32) {
        if (it in 11..13) {
            "${it}th"
        } else {
            when (it % 10) {
                1 -> "${it}st"
                2 -> "${it}nd"
                3 -> "${it}rd"
                else -> "${it}th"
            }
        }
    }

    open fun getOrdinalByDay(day: Int, context: KlockLocaleContext = KlockLocaleContext.Default): String = ordinals[day]

    open fun getDayByOrdinal(ordinal: String): Int = ordinals.indexOf(ordinal)

    //open val monthsShort: List<String> by klockAtomicLazy { months.map { it.substr(0, 3) } }
    //open val daysOfWeekShort: List<String> by klockAtomicLazy { daysOfWeek.map { it.substr(0, 3) } }
    /*
    private val _lock = KlockLock()
    private val _monthsShort = KlockAtomicRef<List<String>?>(null)
    private val _daysOfWeekShort = KlockAtomicRef<List<String>?>(null)
	//open val monthsShort by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { months.map { it.substr(0, 3) } }
    open val monthsShort: List<String> get() = _lock {
        if (_monthsShort.value == null) {
            _monthsShort.value = months.map { it.substr(0, 3) }
        }
        _monthsShort.value!!
    }
	open val daysOfWeekShort: List<String> get() = _lock {
        if (_daysOfWeekShort.value == null) {
            _daysOfWeekShort.value = daysOfWeek.map { it.substr(0, 3) }
        }
        _daysOfWeekShort.value!!
    }
    */

	open val h12Marker: List<String> get() = listOf("am", "pm")

	// This might be required for some languages like chinese?
	open fun intToString(value: Int) = "$value"

	open fun isWeekend(dayOfWeek: DayOfWeek): Boolean = dayOfWeek == DayOfWeek.Saturday || dayOfWeek == DayOfWeek.Sunday

	protected fun format(str: String) = PatternDateFormat(str, this)

	open val formatDateTimeMedium get() = format("MMM d, y h:mm:ss a")
	open val formatDateTimeShort get() = format("M/d/yy h:mm a")

	open val formatDateFull get() = format("EEEE, MMMM d, y")
	open val formatDateLong get() = format("MMMM d, y")
	open val formatDateMedium get() = format("MMM d, y")
	open val formatDateShort get() = format("M/d/yy")

	open val formatTimeMedium get() = format("HH:mm:ss")
	open val formatTimeShort get() = format("HH:mm")

	companion object {
		val english get() = English

		var default: KlockLocale
			set(value) { KlockLocale_default = value }
			get() = KlockLocale_default ?: English

		inline fun <R> setTemporarily(locale: KlockLocale, callback: () -> R): R {
			val old = default
			default = locale
			try {
				return callback()
			} finally {
				default = old
			}
		}
	}

	open class English : KlockLocale() {
		companion object : English()

		override val ISO639_1 get() = "en"

		override val firstDayOfWeek: DayOfWeek get() = DayOfWeek.Sunday

		override val daysOfWeek: List<String> get() = listOf(
			"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
		)
		override val months: List<String> get() = listOf(
			"January", "February", "March", "April", "May", "June",
			"July", "August", "September", "October", "November", "December"
		)

		override val formatTimeMedium get() = format("h:mm:ss a")
		override val formatTimeShort get() = format("h:mm a")
	}
}

fun DateTime.format(format: String, locale: KlockLocale): String = DateFormat(format).withLocale(locale).format(this)
fun DateTimeTz.format(format: String, locale: KlockLocale): String = DateFormat(format).withLocale(locale).format(this)
