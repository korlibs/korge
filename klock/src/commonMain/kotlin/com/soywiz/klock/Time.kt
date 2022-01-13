package com.soywiz.klock

import com.soywiz.klock.internal.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.abs

/**
 * Represents a union of [millisecond], [second], [minute] and [hour].
 */
@JvmInline
value class Time(val encoded: TimeSpan) : Comparable<Time>, Serializable {
	companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /** Constructs a new [Time] from the [hour], [minute], [second] and [millisecond] components. */
		operator fun invoke(hour: Int, minute: Int = 0, second: Int = 0, millisecond: Int = 0): Time =
			Time(hour.hours + minute.minutes + second.seconds + millisecond.milliseconds)

		private const val DIV_MILLISECONDS = 1
		private const val DIV_SECONDS = DIV_MILLISECONDS * 1000
		private const val DIV_MINUTES = DIV_SECONDS * 60
		private const val DIV_HOURS = DIV_MINUTES * 60
	}
    /** The [millisecond] part. */
	val millisecond: Int get() = abs((encoded.millisecondsInt / DIV_MILLISECONDS) % 1000)
    /** The [second] part. */
    val second: Int get() = abs((encoded.millisecondsInt / DIV_SECONDS) % 60)
    /** The [minute] part. */
	val minute: Int get() = abs((encoded.millisecondsInt / DIV_MINUTES) % 60)
    /** The [hour] part. */
	val hour: Int get() = (encoded.millisecondsInt / DIV_HOURS)
    /** The [hour] part adjusted to 24-hour format. */
	val hourAdjusted: Int get() = (encoded.millisecondsInt / DIV_HOURS % 24)

    /** Returns new [Time] instance adjusted to 24-hour format. */
    fun adjust(): Time = Time(hourAdjusted, minute, second, millisecond)

    /** Converts this date to String using [format] for representing it. */
    fun format(format: String) = TimeFormat(format).format(this)
    /** Converts this date to String using [format] for representing it. */
    fun format(format: TimeFormat) = format.format(this)

    /** Converts this time to String formatting it like "00:00:00.000", "23:59:59.999" or "-23:59:59.999" if the [hour] is negative */
	override fun toString(): String = "${if (hour < 0) "-" else ""}${abs(hour).toString().padStart(2, '0')}:${abs(minute).toString().padStart(2, '0')}:${abs(second).toString().padStart(2, '0')}.${abs(millisecond).toString().padStart(3, '0')}"

	override fun compareTo(other: Time): Int = encoded.compareTo(other.encoded)
}

operator fun Time.plus(span: TimeSpan) = Time(this.encoded + span)
