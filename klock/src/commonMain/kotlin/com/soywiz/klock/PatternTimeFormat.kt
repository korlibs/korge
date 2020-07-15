package com.soywiz.klock

import com.soywiz.klock.internal.*
import com.soywiz.klock.internal.MicroStrReader
import com.soywiz.klock.internal.increment
import com.soywiz.klock.internal.padded
import com.soywiz.klock.internal.substr
import kotlin.math.log10
import kotlin.math.pow

data class PatternTimeFormat(
    val format: String,
    val options: Options = Options.DEFAULT
) : TimeFormat, Serializable {
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
            val chunk = s.readChunk {
                val c = s.readChar()
                while (s.hasMore && s.tryRead(c)) Unit
            }
            chunks.add(chunk)
        }
    }.toList()

    private val regexChunks = chunks.map {
        when (it) {
            "H", "k" -> """(\d{1,})"""
            "HH", "kk" -> """(\d{2,})"""
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
            "a" -> """(\w+)"""
            " " -> """(\s+)"""
            else -> when {
                it.startsWith('\'') -> "(" + Regex.escapeReplacement(it.substr(1, it.length - 2)) + ")"
                else -> "(" + Regex.escapeReplacement(it) + ")"
            }
        }
    }

    private val rx2: Regex = Regex("^" + regexChunks.mapIndexed { index, it ->
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

    private fun clampZero(value: Int, size: Int) = (value umod size)

    private fun clampNonZero(value: Int, size: Int) = (value umod size).let { if (it == 0) size else it }

    override fun format(dd: TimeSpan): String {
        val time = Time(dd)
        var out = ""
        for (name in chunks) {
            val nlen = name.length
            out += when (name) {
                "H", "HH" -> time.hour.padded(nlen)
                "k", "kk" -> time.hour.padded(nlen)

                "h", "hh" -> clampNonZero(time.hour, 12).padded(nlen)
                "K", "KK" -> clampZero(time.hour, 12).padded(nlen)

                "m", "mm" -> time.minute.padded(nlen)
                "s", "ss" -> time.second.padded(nlen)

                "S", "SS", "SSS", "SSSS", "SSSSS", "SSSSSS", "SSSSSSS", "SSSSSSSS" -> {
                    val milli = time.millisecond
                    val numberLength = log10(time.millisecond.toDouble()).toInt() + 1
                    if (numberLength > name.length) {
                        (milli.toDouble() / 10.0.pow(numberLength - name.length)).toInt()
                    } else {
                        "${milli.padded(3)}00000".substr(0, name.length)
                    }
                }
                "a" -> if (time.hour < 12) "am" else if (time.hour < 24) "pm" else ""
                else -> if (name.startsWith('\'')) name.substring(1, name.length - 1) else name
            }
        }
        return out
    }

    override fun tryParse(str: String, doThrow: Boolean): TimeSpan? {
        var millisecond = 0
        var second = 0
        var minute = 0
        var hour = 0
        var isPm = false
        var is12HourFormat = false
        val result = rx2.find(str) ?: return null //println("Parser error: Not match, $str, $rx2");
        for ((name, value) in chunks.zip(result.groupValues.drop(1))) {
            if (value.isEmpty()) continue
            when (name) {
                "H", "HH", "k", "kk" -> hour = value.toInt()
                "h", "hh", "K", "KK" -> {
                    hour = value.toInt() umod 24
                    is12HourFormat = true
                }
                "m", "mm" -> minute = value.toInt()
                "s", "ss" -> second = value.toInt()
                "S", "SS", "SSS", "SSSS", "SSSSS", "SSSSSS" -> {
                    val numberLength = log10(value.toDouble()).toInt() + 1
                    millisecond = if (numberLength > 3) {
                        // only precision to millisecond supported, ignore the rest: 9999999 => 999
                        (value.toDouble() * 10.0.pow(-1 * (numberLength - 3))).toInt()
                    } else {
                        value.toInt()
                    }
                }
                "a" -> isPm = value == "pm"
                else -> {
                    // ...
                }
            }
        }
        if (is12HourFormat && isPm) {
            hour += 12
        }
        return hour.hours + minute.minutes + second.seconds + millisecond.milliseconds
    }

    override fun toString(): String = format
}
