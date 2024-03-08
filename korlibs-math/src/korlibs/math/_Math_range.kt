@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.range

import korlibs.number.*

data class DoubleRangeExclusive(val start: Double, val endExclusive: Double) {
    val length: Double get() = endExclusive - start
    operator fun contains(value: Double): Boolean = value >= start && value < endExclusive
    override fun toString(): String = "${start.niceStr} until ${endExclusive.niceStr}"
}

inline infix fun Double.until(endExclusive: Double): DoubleRangeExclusive = DoubleRangeExclusive(this, endExclusive)

data class FloatInRange(val value: Float, val min: Float, val max: Float, val inclusive: Boolean = true)

data class FloatRangeExclusive(val start: Float, val endExclusive: Float) {
    val length: Float get() = endExclusive - start
    operator fun contains(value: Double): Boolean = value >= start && value < endExclusive
    override fun toString(): String = "${start.niceStr} until ${endExclusive.niceStr}"
}

inline infix fun Float.until(endExclusive: Float): FloatRangeExclusive = FloatRangeExclusive(this, endExclusive)

class OpenRange<T : Comparable<T>>(val start: T, val endExclusive: T)

// @TODO: Would cause conflicts with Int until Int for example
//infix fun <T : Comparable<T>> T.until(other: T) = OpenRange(this, other)

operator fun <T : Comparable<T>> OpenRange<T>.contains(item: T) = item >= this.start && item < this.endExclusive
