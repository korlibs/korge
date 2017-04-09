package com.soywiz.korge.time

data class TimeSpan(val ms: Int) {
	val milliseconds: Int get() = this.ms
	val seconds: Double get() = this.ms.toDouble() / 1000.0
}

inline val Number.milliseconds get() = TimeSpan(this.toInt())
inline val Number.seconds get() = TimeSpan((this.toDouble() / 1000.0).toInt())
