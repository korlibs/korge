package com.soywiz.korma.geom.range

data class DoubleRangeExclusive(val start: Double, val endExclusive: Double) {
    val length: Double get() = endExclusive - start
    operator fun contains(value: Double): Boolean = value >= start && value < endExclusive
}

inline infix fun Double.until(endExclusive: Double): DoubleRangeExclusive = DoubleRangeExclusive(this, endExclusive)

class OpenRange<T : Comparable<T>>(val start: T, val endExclusive: T)

// @TODO: Would cause conflicts with Int until Int for example
//infix fun <T : Comparable<T>> T.until(other: T) = OpenRange(this, other)

operator fun <T : Comparable<T>> OpenRange<T>.contains(item: T) = item >= this.start && item < this.endExclusive
