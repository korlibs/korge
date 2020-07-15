package com.soywiz.klock.wrapped

import com.soywiz.klock.*
import com.soywiz.klock.annotations.*
import com.soywiz.klock.internal.Serializable

@KlockExperimental
val TimezoneOffset.wrapped get() = WTimezoneOffset(this)

/**
 * Wrapped Version, that is not inline. You can use [value] to get the wrapped inline class.
 *
 * Represents a time zone offset with millisecond precision. Usually minute is enough.
 * Can be used along [WDateTimeTz] to construct non universal, local times.
 *
 * This class is inlined so no boxing should be required.
 */
@KlockExperimental
class WTimezoneOffset(val value: TimezoneOffset) : Serializable {
    /** Returns whether this [WTimezoneOffset] has a positive component */
    val positive: Boolean get() = value.positive

    /** [WTimeSpan] time for this [WTimezoneOffset] */
    val time get() = value.time.wrapped

    /** [WTimezoneOffset] in [totalMinutes] */
    val totalMinutes: Double get() = value.totalMinutes

    /** [WTimezoneOffset] in [totalMinutes] as integer */
    val totalMinutesInt get() = value.totalMinutesInt

    /** Returns a string representation of this [WTimezoneOffset] */
    val timeZone: String get() = value.timeZone

    override fun toString(): String = value.toString()

    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L
        
        /** Constructs a new [WTimezoneOffset] from a [WTimeSpan]. */
        operator fun invoke(time: WTimeSpan) = TimezoneOffset(time.value).wrapped

        /**
         * Returns timezone offset as a [WTimeSpan], for a specified [time].
         * For example, GMT+01 would return 60.minutes.
         * This uses the Operating System to compute daylight offsets when required.
         */
        fun local(time: WDateTime) = TimezoneOffset.local(time.value).wrapped
    }
}

/** A [WTimeSpan] as a [WTimezoneOffset]. */
@KlockExperimental
val WTimeSpan.offset get() = this.value.offset.wrapped
