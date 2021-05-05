package com.soywiz.korma.geom

import com.soywiz.korma.geom.range.OpenRange
import com.soywiz.korma.internal.umod
import com.soywiz.korma.interpolation.*
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.atan2

@PublishedApi internal const val PI2 = PI * 2

@PublishedApi internal const val DEG2RAD = PI / 180.0
@PublishedApi internal const val RAD2DEG = 180.0 / PI

@PublishedApi internal const val MAX_DEGREES = 360.0
@PublishedApi internal const val MAX_RADIANS = PI2

@PublishedApi internal const val HALF_DEGREES = MAX_DEGREES / 2.0
@PublishedApi internal const val HALF_RADIANS = MAX_RADIANS / 2.0

@PublishedApi internal fun Angle_shortDistanceTo(from: Angle, to: Angle): Angle {
    val r0 = from.radians umod MAX_RADIANS
    val r1 = to.radians umod MAX_RADIANS
    val diff = (r1 - r0 + HALF_RADIANS) % MAX_RADIANS - HALF_RADIANS
    return if (diff < -HALF_RADIANS) Angle(diff + MAX_RADIANS) else Angle(diff)
}

@PublishedApi internal fun Angle_longDistanceTo(from: Angle, to: Angle): Angle {
    val short = Angle_shortDistanceTo(from, to)
    return when {
        short == 0.radians -> 0.radians
        short < 0.radians -> 360.degrees + short
        else -> (-360).degrees + short
    }
}

@PublishedApi internal fun Angle_between(x0: Double, y0: Double, x1: Double, y1: Double): Angle {
    val angle = atan2(y1 - y0, x1 - x0)
    return if (angle < 0) Angle(angle + PI2) else Angle(angle)
}

inline class Angle(val radians: Double) : Comparable<Angle> {
    override fun toString(): String = "$degrees.degrees"

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        inline val ZERO get() = Angle(0.0)

        inline fun fromRadians(radians: Double) = Angle(radians)
        inline fun fromDegrees(degrees: Double) = Angle(degreesToRadians(degrees))

        inline fun fromRadians(radians: Float) = fromRadians(radians.toDouble())
        inline fun fromDegrees(degrees: Float) = fromDegrees(degrees.toDouble())

        inline fun fromRadians(radians: Int) = fromRadians(radians.toDouble())
        inline fun fromDegrees(degrees: Int) = fromDegrees(degrees.toDouble())

        inline fun cos01(ratio: Double) = kotlin.math.cos(PI2 * ratio)
        inline fun sin01(ratio: Double) = kotlin.math.sin(PI2 * ratio)
        inline fun tan01(ratio: Double) = kotlin.math.tan(PI2 * ratio)

        inline fun degreesToRadians(degrees: Double): Double = degrees * DEG2RAD
        inline fun radiansToDegrees(radians: Double): Double = radians * RAD2DEG

        inline fun shortDistanceTo(from: Angle, to: Angle): Angle = Angle_shortDistanceTo(from, to)
        inline fun longDistanceTo(from: Angle, to: Angle): Angle = Angle_longDistanceTo(from, to)
        inline fun between(x0: Double, y0: Double, x1: Double, y1: Double): Angle = Angle_between(x0, y0, x1, y1)

        inline fun between(x0: Int, y0: Int, x1: Int, y1: Int): Angle = between(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())
        inline fun between(x0: Float, y0: Float, x1: Float, y1: Float): Angle = between(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())

        inline fun between(p0: IPoint, p1: IPoint): Angle = between(p0.x, p0.y, p1.x, p1.y)
    }

    override fun compareTo(other: Angle): Int {
        //return this.radians.compareTo(other.radians) // @TODO: Double.compareTo calls EnterFrame/LeaveFrame! because it uses a Double companion object
        val left = this.radians
        val right = other.radians
        // @TODO: Handle infinite/NaN? Though usually this won't happen
        if (left < right) return -1
        if (left > right) return +1
        return 0
    }
}

inline fun cos(angle: Angle): Double = kotlin.math.cos(angle.radians)
inline fun sin(angle: Angle): Double = kotlin.math.sin(angle.radians)
inline fun tan(angle: Angle): Double = kotlin.math.tan(angle.radians)
inline fun abs(angle: Angle): Angle = Angle.fromRadians(angle.radians.absoluteValue)

val Angle.cosine get() = cos(this)
val Angle.sine get() = sin(this)
val Angle.tangent get() = tan(this)
val Angle.degrees get() = Angle.radiansToDegrees(radians)

val Angle.absoluteValue: Angle get() = Angle.fromRadians(radians.absoluteValue)
fun Angle.shortDistanceTo(other: Angle): Angle = Angle.shortDistanceTo(this, other)
fun Angle.longDistanceTo(other: Angle): Angle = Angle.longDistanceTo(this, other)

operator fun Angle.times(scale: Double): Angle = Angle(this.radians * scale)
operator fun Angle.div(scale: Double): Angle = Angle(this.radians / scale)
operator fun Angle.times(scale: Float): Angle = this * scale.toDouble()
operator fun Angle.div(scale: Float): Angle = this / scale.toDouble()
operator fun Angle.times(scale: Int): Angle = this * scale.toDouble()
operator fun Angle.div(scale: Int): Angle = this / scale.toDouble()

operator fun Angle.div(other: Angle): Double = this.radians / other.radians // Ratio
operator fun Angle.plus(other: Angle): Angle = Angle(this.radians + other.radians)
operator fun Angle.minus(other: Angle): Angle = Angle(this.radians - other.radians)
operator fun Angle.unaryMinus(): Angle = Angle(-radians)
operator fun Angle.unaryPlus(): Angle = Angle(+radians)
operator fun Angle.compareTo(other: Angle): Int = this.radians.compareTo(other.radians)
operator fun ClosedRange<Angle>.contains(angle: Angle): Boolean = angle.inBetween(this.start, this.endInclusive, inclusive = true)
operator fun OpenRange<Angle>.contains(angle: Angle): Boolean = angle.inBetween(this.start, this.endExclusive, inclusive = false)
infix fun Angle.until(other: Angle) = OpenRange(this, other)

fun Angle.inBetweenInclusive(min: Angle, max: Angle): Boolean = inBetween(min, max, inclusive = true)
fun Angle.inBetweenExclusive(min: Angle, max: Angle): Boolean = inBetween(min, max, inclusive = false)

infix fun Angle.inBetween(range: ClosedRange<Angle>): Boolean = inBetween(range.start, range.endInclusive, inclusive = true)
infix fun Angle.inBetween(range: OpenRange<Angle>): Boolean = inBetween(range.start, range.endExclusive, inclusive = false)

fun Angle.inBetween(min: Angle, max: Angle, inclusive: Boolean): Boolean {
    val nthis = this.normalized
    val nmin = min.normalized
    val nmax = max.normalized
    @Suppress("ConvertTwoComparisonsToRangeCheck")
    return when {
        nmin > nmax -> nthis >= nmin || (if (inclusive) nthis <= nmax else nthis < nmax)
        else -> nthis >= nmin && (if (inclusive) nthis <= nmax else nthis < nmax)
    }
}

val Double.degrees get() = Angle.fromDegrees(this)
val Double.radians get() = Angle.fromRadians(this)
val Int.degrees get() = Angle.fromDegrees(this)
val Int.radians get() = Angle.fromRadians(this)
val Float.degrees get() = Angle.fromDegrees(this)
val Float.radians get() = Angle.fromRadians(this)

val Angle.normalized get() = Angle(radians umod MAX_RADIANS)

fun Double.interpolate(l: Angle, r: Angle): Angle = this.interpolate(l.radians, r.radians).radians
