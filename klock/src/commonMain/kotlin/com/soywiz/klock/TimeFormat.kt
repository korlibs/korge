package com.soywiz.klock

interface TimeFormat {
    fun format(dd: TimeSpan): String
    fun tryParse(str: String, doThrow: Boolean): TimeSpan?

    companion object {
        val DEFAULT_FORMAT = TimeFormat("HH:mm:ss.SSS")
        val FORMAT_TIME = TimeFormat("HH:mm:ss")

        val FORMATS = listOf(DEFAULT_FORMAT, FORMAT_TIME)

        fun parse(time: String): TimeSpan {
            var lastError: Throwable? = null
            for (format in FORMATS) {
                try {
                    return format.parse(time)
                } catch (e: Throwable) {
                    lastError = e
                }
            }
            throw lastError!!
        }

        operator fun invoke(pattern: String) = PatternTimeFormat(pattern)
    }
}

fun TimeFormat.parse(str: String): TimeSpan =
    tryParse(str, doThrow = true) ?: throw DateException("Not a valid format: '$str' for '$this'")
fun TimeFormat.parseTime(str: String): Time = Time(parse(str))

fun TimeFormat.format(time: Double): String = format(time.milliseconds)
fun TimeFormat.format(time: Long): String = format(time.milliseconds)
fun TimeFormat.format(time: Time): String = format(time.encoded)

