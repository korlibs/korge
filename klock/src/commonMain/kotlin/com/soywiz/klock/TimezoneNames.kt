package com.soywiz.klock

import com.soywiz.klock.internal.Serializable

data class TimezoneNames(val namesToOffsets: Map<String, TimeSpan>) : Serializable {
	constructor(vararg tz: Pair<String, TimeSpan>) : this(tz.toMap())

	operator fun plus(other: TimezoneNames) = TimezoneNames(this.namesToOffsets + other.namesToOffsets)

	companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        val DEFAULT = TimezoneNames(
			"PDT" to (-7).hours,
			"PST" to (-8).hours,
			"GMT" to 0.hours,
			"UTC" to 0.hours
		)
	}
}
