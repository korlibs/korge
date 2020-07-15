package com.soywiz.klock

/** Allows to [format] and [parse] instances of [Date], [DateTime] and [DateTimeTz] */
interface DateFormat {
    fun format(dd: DateTimeTz): String
    fun tryParse(str: String, doThrow: Boolean = false): DateTimeTz?

    companion object {
        val DEFAULT_FORMAT = DateFormat("EEE, dd MMM yyyy HH:mm:ss z")
        val FORMAT1 = DateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        val FORMAT2 = DateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        val FORMAT_DATE = DateFormat("yyyy-MM-dd")

        val FORMATS = listOf(DEFAULT_FORMAT, FORMAT1, FORMAT2, FORMAT_DATE)

        fun parse(date: String): DateTimeTz {
            var lastError: Throwable? = null
            for (format in FORMATS) {
                try {
                    return format.parse(date)
                } catch (e: Throwable) {
                    lastError = e
                }
            }
            throw lastError!!
        }

        operator fun invoke(pattern: String) = PatternDateFormat(pattern)
    }
}

fun DateFormat.parse(str: String): DateTimeTz =
    tryParse(str, doThrow = true) ?: throw DateException("Not a valid format: '$str' for '$this'")
fun DateFormat.parseDate(str: String): Date = parse(str).local.date

fun DateFormat.parseUtc(str: String): DateTime = parse(str).utc

fun DateFormat.format(date: Double): String = format(DateTime.fromUnix(date))
fun DateFormat.format(date: Long): String = format(DateTime.fromUnix(date))

fun DateFormat.format(dd: DateTime): String = format(dd.toOffsetUnadjusted(0.minutes))
fun DateFormat.format(dd: Date): String = format(dd.dateTimeDayStart)
