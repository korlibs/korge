package korlibs.time

import korlibs.time.internal.*
import korlibs.time.internal.MicroStrReader
import korlibs.time.internal.increment
import korlibs.time.internal.padded
import korlibs.time.internal.substr
import kotlin.math.log10
import kotlin.math.pow

data class PatternTimeFormat(
    val format: String,
    val options: Options = Options.DEFAULT,
    val locale: KlockLocale = KlockLocale.default,
) : BasePatternDateTimeFormat(format, options.optionalSupport), TimeFormat, Serializable {
    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L
    }

    data class Options(val optionalSupport: Boolean = false) : Serializable {
        companion object {
            @Suppress("MayBeConstant", "unused")
            private const val serialVersionUID = 1L

            val DEFAULT = Options(optionalSupport = false)
            val WITH_OPTIONAL = Options(optionalSupport = true)
        }
    }

    fun withLocale(locale: KlockLocale) = this.copy(locale = locale)
    fun withOptions(options: Options) = this.copy(options = options)
    fun withOptional() = this.copy(options = options.copy(optionalSupport = true))
    fun withNonOptional() = this.copy(options = options.copy(optionalSupport = false))

    override fun matchChunkToRegex(it: String): String? = matchTimeChunkToRegex(it)

    override fun format(dd: TimeSpan): String {
        val time = Time(dd)
        return chunks.joinToString("") {
            formatTimeChunk(it, locale, time.hour, time.minute, time.second, time.millisecond, clampHours = false)
                ?: formatElseChunk(it)
        }
    }

    override fun tryParse(str: String, doThrow: Boolean, doAdjust: Boolean): TimeSpan? {
        val (_, _, _, hour, minute, second, millisecond, _) = _tryParseBase(str, doThrow, doAdjust, locale, TimezoneNames.DEFAULT) ?: return null
        return hour.hours + minute.minutes + second.seconds + millisecond.milliseconds
    }

    override fun toString(): String = format
}
