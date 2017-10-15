package com.soywiz.korge.time

import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.interpolate

@Suppress("DataClassPrivateConstructor")
data class TimeSpan private constructor(val ms: Int) : Comparable<TimeSpan>, Interpolable<TimeSpan> {
	val milliseconds: Int get() = this.ms
	val seconds: Double get() = this.ms.toDouble() / 1000.0

	companion object {
		val ZERO = TimeSpan(0)
		@PublishedApi internal fun fromMilliseconds(ms: Int) = when (ms) {
			0 -> ZERO
			else -> TimeSpan(ms)
		}
	}

	override fun compareTo(other: TimeSpan): Int = this.ms.compareTo(other.ms)

	operator fun plus(other: TimeSpan): TimeSpan = TimeSpan(this.ms + other.ms)
	operator fun minus(other: TimeSpan): TimeSpan = TimeSpan(this.ms - other.ms)
	operator fun times(scale: Int): TimeSpan = TimeSpan(this.ms * scale)
	operator fun times(scale: Double): TimeSpan = TimeSpan((this.ms * scale).toInt())

	override fun interpolateWith(other: TimeSpan, ratio: Double): TimeSpan = TimeSpan(ratio.interpolate(this.ms, other.ms))
}



inline val Number.milliseconds get() = TimeSpan.fromMilliseconds(this.toInt())
inline val Number.seconds get() = TimeSpan.fromMilliseconds((this.toDouble() * 1000.0).toInt())
