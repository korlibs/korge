package com.soywiz.klock

import com.soywiz.klock.internal.*
import kotlin.jvm.JvmOverloads
import kotlin.math.*

data class PatternDateFormat @JvmOverloads constructor(
    val format: String,
    val locale: KlockLocale? = null,
    val tzNames: TimezoneNames = TimezoneNames.DEFAULT,
    val options: Options = Options.DEFAULT
) : DateFormat, Serializable {
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

    private val openOffsets = LinkedHashMap<Int, Int>()
    private val closeOffsets = LinkedHashMap<Int, Int>()

    internal val chunks = arrayListOf<String>().also { chunks ->
        val s = MicroStrReader(format)
        while (s.hasMore) {
            if (s.peekChar() == '\'') {
                val escapedChunk = s.readChunk {
                    s.tryRead('\'')
                    while (s.hasMore && s.readChar() != '\'') Unit
                }
                chunks.add(escapedChunk)
                continue
            }
            if (options.optionalSupport) {
                val offset = chunks.size
                if (s.tryRead('[')) {
                    openOffsets.increment(offset)
                    continue
                }
                if (s.tryRead(']')) {
                    closeOffsets.increment(offset - 1)
                    continue
                }
            }
            chunks.add(s.tryReadOrNull("do") ?: s.readRepeatedChar())
        }
    }.toList()

    internal val regexChunks = chunks.map {
        when (it) {
            "E", "EE", "EEE", "EEEE", "EEEEE", "EEEEEE" -> """(\w+)"""
            "z", "zzz" -> """([\w\s\-+:]+)"""
            "do" -> """(\d{1,2}\w+)"""
            "d" -> """(\d{1,2})"""
            "dd" -> """(\d{2})"""
            "M" -> """(\d{1,5})"""
            "MM" -> """(\d{2})"""
            "MMM", "MMMM", "MMMMM" -> """(\w+)"""
            "y" -> """(\d{1,5})"""
            "yy" -> """(\d{2})"""
            "yyy" -> """(\d{3})"""
            "yyyy" -> """(\d{4})"""
            "YYYY" -> """(\d{4})"""
            "H", "k" -> """(\d{1,2})"""
            "HH", "kk" -> """(\d{2})"""
            "h", "K" -> """(\d{1,2})"""
            "hh", "KK" -> """(\d{2})"""
            "m" -> """(\d{1,2})"""
            "mm" -> """(\d{2})"""
            "s" -> """(\d{1,2})"""
            "ss" -> """(\d{2})"""
            "S" -> """(\d{1,6})"""
            "SS" -> """(\d{2})"""
            "SSS" -> """(\d{3})"""
            "SSSS" -> """(\d{4})"""
            "SSSSS" -> """(\d{5})"""
            "SSSSSS" -> """(\d{6})"""
            "SSSSSSS" -> """(\d{7})"""
            "SSSSSSSS" -> """(\d{8})"""
            "SSSSSSSSS" -> """(\d{9})"""
            "X", "XX", "XXX", "x", "xx", "xxx" -> """([\w:\+\-]+)"""
            "a" -> """(\w+)"""
            " " -> """(\s+)"""
            else -> when {
                it.startsWith('\'') -> "(" + Regex.escape(it.substr(1, it.length - 2)) + ")"
                else -> "(" + Regex.escape(it) + ")"
            }
        }
    }

    //val escapedFormat = Regex.escape(format)
    internal val rx2: Regex = Regex("^" + regexChunks.mapIndexed { index, it ->
        if (options.optionalSupport) {
            val opens = openOffsets.getOrElse(index) { 0 }
            val closes = closeOffsets.getOrElse(index) { 0 }
            buildString {
                repeat(opens) { append("(?:") }
                append(it)
                repeat(closes) { append(")?") }
            }
        } else {
            it
        }
    }.joinToString("") + "$")


    // EEE, dd MMM yyyy HH:mm:ss z -- > Sun, 06 Nov 1994 08:49:37 GMT
    // YYYY-MM-dd HH:mm:ss

    override fun format(dd: DateTimeTz): String {
        val utc = dd.local
        var out = ""
        for (name in chunks) {
            val nlen = name.length
            out += when (name) {
                "E", "EE", "EEE" -> DayOfWeek[utc.dayOfWeek.index0].localShortName(realLocale)
                "EEEE", "EEEEE", "EEEEEE" -> DayOfWeek[utc.dayOfWeek.index0].localName(realLocale)
                "z", "zzz" -> dd.offset.timeZone
                "d", "dd" -> utc.dayOfMonth.padded(nlen)
                "do" -> realLocale.getOrdinalByDay(utc.dayOfMonth)
                "M", "MM" -> utc.month1.padded(nlen)
                "MMM" -> Month[utc.month1].localName(realLocale).substr(0, 3)
                "MMMM" -> Month[utc.month1].localName(realLocale)
                "MMMMM" -> Month[utc.month1].localName(realLocale).substr(0, 1)
                "y" -> utc.yearInt
                "yy" -> (utc.yearInt % 100).padded(2)
                "yyy" -> (utc.yearInt % 1000).padded(3)
                "yyyy" -> utc.yearInt.padded(4)
                "YYYY" -> utc.yearInt.padded(4)

                "H", "HH" -> mconvertRangeZero(utc.hours, 24).padded(nlen)
                "k", "kk" -> mconvertRangeNonZero(utc.hours, 24).padded(nlen)

                "h", "hh" -> mconvertRangeNonZero(utc.hours, 12).padded(nlen)
                "K", "KK" -> mconvertRangeZero(utc.hours, 12).padded(nlen)

                "m", "mm" -> utc.minutes.padded(nlen)
                "s", "ss" -> utc.seconds.padded(nlen)

                "S", "SS", "SSS", "SSSS", "SSSSS", "SSSSSS", "SSSSSSS", "SSSSSSSS", "SSSSSSSSS" -> {
                    val milli = utc.milliseconds
                    val base10length = log10(utc.milliseconds.toDouble()).toInt() + 1
                    if (base10length > name.length) {
                        (milli.toDouble() * 10.0.pow(-1 * (base10length - name.length))).toInt()
                    } else {
                        "${milli.padded(3)}000000".substr(0, name.length)
                    }
                }
                "X", "XX", "XXX", "x", "xx", "xxx" -> {
                    when {
                        name.startsWith("X") && dd.offset.totalMinutesInt == 0 -> "Z"
                        else -> {
                            val p = if (dd.offset.totalMinutesInt >= 0) "+" else "-"
                            val hours = (dd.offset.totalMinutesInt / 60).absoluteValue
                            val minutes = (dd.offset.totalMinutesInt % 60).absoluteValue
                            when (name) {
                                "X", "x" -> "$p${hours.padded(2)}"
                                "XX", "xx" -> "$p${hours.padded(2)}${minutes.padded(2)}"
                                "XXX", "xxx" -> "$p${hours.padded(2)}:${minutes.padded(2)}"
                                else -> name
                            }
                        }
                    }
                }
                "a" -> if (utc.hours < 12) "am" else "pm"
                else -> when {
                    name.startsWith('\'') -> name.substring(1, name.length - 1)
                    else -> name
                }
            }
        }
        return out
    }

    override fun tryParse(str: String, doThrow: Boolean): DateTimeTz? {
        var millisecond = 0
        var second = 0
        var minute = 0
        var hour = 0
        var day = 1
        var month = 1
        var fullYear = 1970
        var offset: TimeSpan? = null
        var isPm = false
        var is12HourFormat = false
        val result = rx2.find(str) ?: return null //println("Parser error: Not match, $str, $rx2");
        for ((name, value) in chunks.zip(result.groupValues.drop(1))) {
            if (value.isEmpty()) continue

            when (name) {
                "E", "EE", "EEE", "EEEE", "EEEEE", "EEEEEE" -> Unit // day of week (Sun | Sunday)
                "z", "zzz" -> { // timezone (GMT)
                    offset = MicroStrReader(value).readTimeZoneOffset(tzNames)
                }
                "d", "dd" -> day = value.toInt()
                "do" -> day = realLocale.getDayByOrdinal(value)
                "M", "MM" -> month = value.toInt()
                "MMM" -> month = realLocale.monthsShort.indexOf(value) + 1
                "y", "yyyy", "YYYY" -> fullYear = value.toInt()
                "yy" -> if (doThrow) throw RuntimeException("Not guessing years from two digits.") else return null
                "yyy" -> fullYear = value.toInt() + if (value.toInt() < 800) 2000 else 1000 // guessing year...
                "H", "HH", "k", "kk" -> hour = value.toInt() umod 24
                "h", "hh", "K", "KK" -> {
                    hour = value.toInt() umod 24
                    is12HourFormat = true
                }
                "m", "mm" -> minute = value.toInt()
                "s", "ss" -> second = value.toInt()
                "S", "SS", "SSS", "SSSS", "SSSSS", "SSSSSS", "SSSSSSS", "SSSSSSSS", "SSSSSSSSS" -> {
                    val base10length = log10(value.toDouble()).toInt() + 1
                    millisecond = if (base10length > 3) {
                        // only precision to millisecond supported, ignore the rest. ex: 9999999 => 999"
                        (value.toDouble() * 10.0.pow(-1 * (base10length - 3))).toInt()
                    } else {
                        value.toInt()
                    }
                }
                "X", "XX", "XXX", "x", "xx", "xxx" -> when {
                    name.startsWith("X") && value.first() == 'Z' -> offset = 0.hours
                    name.startsWith("x") && value.first() == 'Z' -> {
                        if (doThrow) throw RuntimeException("Zulu Time Zone is only accepted with X-XXX formats.") else return null
                    }
                    value.first() != 'Z' -> {
                        val valueUnsigned = value.drop(1)
                        val hours = when (name) {
                            "X", "x" -> valueUnsigned.toInt()
                            "XX", "xx" -> valueUnsigned.take(2).toInt()
                            "XXX", "xxx" -> valueUnsigned.substringBefore(':').toInt()
                            else -> throw RuntimeException("Unreachable code! Incorrect implementation!")
                        }
                        val minutes = when (name) {
                            "X", "x" -> 0
                            "XX", "xx" -> valueUnsigned.drop(2).toInt()
                            "XXX", "xxx" -> valueUnsigned.substringAfter(':', "0").toInt()
                            else -> throw RuntimeException("Unreachable code! Incorrect implementation!")
                        }
                        offset = hours.hours + minutes.minutes
                        if (value.first() == '-') {
                            offset = -offset
                        }
                    }
                }
                "MMMM" -> month = realLocale.months.indexOf(value) + 1
                "MMMMM" -> if (doThrow) throw RuntimeException("Not possible to get the month from one letter.") else return null
                "a" -> isPm = value == "pm"
                else -> {
                    // ...
                }
            }
        }
        //return DateTime.createClamped(fullYear, month, day, hour, minute, second)
        if (is12HourFormat && isPm) {
            hour += 12
        }
        val dateTime = DateTime.createAdjusted(fullYear, month, day, hour, minute, second, millisecond)
        return dateTime.toOffsetUnadjusted(offset ?: 0.hours)
    }

    override fun toString(): String = format
}

private fun mconvertRangeZero(value: Int, size: Int): Int {
    return (value umod size)
}

private fun mconvertRangeNonZero(value: Int, size: Int): Int {
    val res = (value umod size)
    return if (res == 0) size else res
}

private fun MicroStrReader.readRepeatedChar(): String {
    return readChunk {
        val c = readChar()
        while (hasMore && (tryRead(c))) Unit
    }
}
