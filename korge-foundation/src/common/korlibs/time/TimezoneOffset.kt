package korlibs.time

import korlibs.time.internal.KlockInternal
import korlibs.time.internal.MILLIS_PER_MINUTE
import korlibs.time.internal.Serializable
import korlibs.time.internal.padded
import kotlin.jvm.JvmInline
import kotlin.math.abs

/**
 * Represents a time zone offset with millisecond precision. Usually minute is enough.
 * Can be used along [DateTimeTz] to construct non universal, local times.
 *
 * This class is inlined so no boxing should be required.
 */
@JvmInline
value class TimezoneOffset(
    /** [TimezoneOffset] in [totalMilliseconds] */
    val totalMilliseconds: Double
) : Comparable<TimezoneOffset>, Serializable {
    /** Returns whether this [TimezoneOffset] has a positive component */
    val positive: Boolean get() = totalMilliseconds >= 0.0

    /** [TimeSpan] time for this [TimezoneOffset] */
    val time get() = totalMilliseconds.milliseconds

    /** [TimezoneOffset] in [totalMinutes] */
    val totalMinutes: Double get() = totalMilliseconds / MILLIS_PER_MINUTE

    /** [TimezoneOffset] in [totalMinutes] as integer */
    val totalMinutesInt get() = totalMinutes.toInt()

    /** Returns a string representation of this [TimezoneOffset] */
    val timeZone: String get() {
        val sign = if (positive) "+" else "-"
        val hour = deltaHoursAbs.padded(2)
        val minute = deltaMinutesAbs.padded(2)
        return if (time == 0.minutes) "UTC" else "GMT$sign$hour$minute"
    }


    private val deltaTotalMinutesAbs: Int get() = abs(totalMinutes.toInt())
    internal val deltaHoursAbs: Int get() = deltaTotalMinutesAbs / 60
    internal val deltaMinutesAbs: Int get() = deltaTotalMinutesAbs % 60

    override fun toString(): String = timeZone

    companion object {
        val UTC = TimezoneOffset(0.0)

        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        /** Constructs a new [TimezoneOffset] from a [TimeSpan]. */
        operator fun invoke(time: TimeSpan) = TimezoneOffset(time.milliseconds)

        /**
         * Returns timezone offset as a [TimeSpan], for a specified [time].
         * For example, GMT+01 would return 60.minutes.
         * This uses the Operating System to compute daylight offsets when required.
         */
        fun local(time: DateTime): TimezoneOffset = KlockInternal.localTimezoneOffsetMinutes(time).offset
    }

    override fun compareTo(other: TimezoneOffset): Int = totalMilliseconds.compareTo(other.totalMilliseconds)
}

/** A [TimeSpan] as a [TimezoneOffset]. */
val TimeSpan.offset get() = TimezoneOffset(this)
