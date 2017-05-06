package com.soywiz.korge.time

@Suppress("DataClassPrivateConstructor")
data class TimeSpan private constructor(val ms: Int) : Comparable<TimeSpan> {
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
}



inline val Number.milliseconds get() = TimeSpan.fromMilliseconds(this.toInt())
inline val Number.seconds get() = TimeSpan.fromMilliseconds((this.toDouble() * 1000.0).toInt())
