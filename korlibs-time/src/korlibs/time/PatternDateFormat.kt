package korlibs.time

import korlibs.time.internal.*
import korlibs.time.internal.MicroStrReader
import korlibs.time.internal.increment
import korlibs.time.internal.padded
import korlibs.time.internal.readTimeZoneOffset
import korlibs.time.internal.substr
import kotlin.jvm.JvmOverloads
import kotlin.math.absoluteValue
import kotlin.math.log10
import kotlin.math.pow

data class PatternDateFormat @JvmOverloads constructor(
    val format: String,
    val locale: KlockLocale? = null,
    val tzNames: TimezoneNames = TimezoneNames.DEFAULT,
    val options: Options = Options.DEFAULT
) : BasePatternDateTimeFormat(format, options.optionalSupport), DateFormat, Serializable {
    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L
    }

    val realLocale get() = locale ?: KlockLocale.default

    data class Options(val optionalSupport: Boolean = false) : Serializable {
        companion object {
            @Suppress("MayBeConstant", "unused")
            private const val serialVersionUID = 1L

            val DEFAULT = Options(optionalSupport = false)
            val WITH_OPTIONAL = Options(optionalSupport = true)
        }
    }

    fun withLocale(locale: KlockLocale?) = this.copy(locale = locale)
    fun withTimezoneNames(tzNames: TimezoneNames) = this.copy(tzNames = this.tzNames + tzNames)
    fun withOptions(options: Options) = this.copy(options = options)
    fun withOptional() = this.copy(options = options.copy(optionalSupport = true))
    fun withNonOptional() = this.copy(options = options.copy(optionalSupport = false))

    override fun matchChunkToRegex(it: String): String? = matchDateChunkToRegex(it) ?: matchTimeChunkToRegex(it)

    // EEE, dd MMM yyyy HH:mm:ss z -- > Sun, 06 Nov 1994 08:49:37 GMT
    // YYYY-MM-dd HH:mm:ss

    override fun format(dd: DateTimeTz): String {
        val utc = dd.local
        val locale = realLocale
        return chunks.joinToString("") {
            formatDateChunk(it, dd, locale)
                ?: formatTimeChunk(it, locale, utc.hours, utc.minutes, utc.seconds, utc.milliseconds, clampHours = true)
                ?: formatElseChunk(it)
        }
    }

    override fun tryParse(str: String, doThrow: Boolean, doAdjust: Boolean): DateTimeTz? {
        val (
            fullYear, month, day,
            hour, minute, second, millisecond,
            offset,
        ) = _tryParseBase(str, doThrow, doAdjust, realLocale, tzNames) ?: return null

        if (!doAdjust) {
            if (month !in 1..12) if (doThrow) error("Invalid month $month") else return null
            if (day !in 1..32) if (doThrow) error("Invalid day $day") else return null
            if (hour !in 0..24) if (doThrow) error("Invalid hour $hour") else return null
            if (minute !in 0..59) if (doThrow) error("Invalid minute $minute") else return null
            if (second !in 0..59) if (doThrow) error("Invalid second $second") else return null
            if (millisecond !in 0..999) if (doThrow) error("Invalid millisecond $millisecond") else return null
        }
        val dateTime = DateTime.createAdjusted(fullYear, month, day, hour umod 24, minute, second, millisecond)
        return dateTime.toOffsetUnadjusted(offset ?: 0.hours)
    }

    override fun toString(): String = format
}
