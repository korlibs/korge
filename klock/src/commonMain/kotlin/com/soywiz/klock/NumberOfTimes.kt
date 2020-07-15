package com.soywiz.klock

val infiniteTimes get() = NumberOfTimes.INFINITE
inline val Int.times get() = NumberOfTimes(this)

inline class NumberOfTimes(val count: Int) {
    companion object {
        val ZERO = NumberOfTimes(0)
        val ONE = NumberOfTimes(1)
        val INFINITE = NumberOfTimes(Int.MIN_VALUE)
    }
    val isInfinite get() = this == INFINITE
    val isFinite get() = !isInfinite
    val hasMore get() = this != ZERO
    val oneLess get() = if (this == INFINITE) INFINITE else NumberOfTimes(count - 1)
    operator fun plus(other: NumberOfTimes) = if (this == INFINITE || other == INFINITE) INFINITE else NumberOfTimes(this.count + other.count)
    operator fun minus(other: NumberOfTimes) = when {
        this == other -> ZERO
        this == INFINITE || other == INFINITE -> INFINITE
        else -> NumberOfTimes(this.count - other.count)
    }
    operator fun times(other: Int) = if (this == INFINITE) INFINITE else NumberOfTimes(this.count * other)
    operator fun div(other: Int) = if (this == INFINITE) INFINITE else NumberOfTimes(this.count / other)
    override fun toString(): String = if (this == INFINITE) "$count times" else "Infinite times"
}
