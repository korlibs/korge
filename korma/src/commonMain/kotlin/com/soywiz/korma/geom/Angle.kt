package com.soywiz.korma.geom

import com.soywiz.korma.geom.range.OpenRange
import com.soywiz.korma.internal.umod
import com.soywiz.korma.interpolation.*
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.atan2

//inline class Angle(val radians: Double) : Comparable<Angle> {
inline class Angle(val radiansf: Float) : Comparable<Angle> {
    val radians get() = radiansf.toDouble()
    constructor(radians: Int) : this(radians.toFloat())
    constructor(radians: Double) : this(radians.toFloat())

    override fun toString(): String = "$degrees.degrees"

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val ZERO = Angle(0f)

        fun fromRadians(radians: Double) = Angle(radians)
        fun fromDegrees(degrees: Double) = Angle(degreesToRadians(degrees))

        fun fromRadians(radians: Float) = Angle(radians)
        fun fromDegrees(degrees: Float) = Angle(degreesToRadians(degrees))

        fun fromRadians(radians: Int) = Angle(radians)
        fun fromDegrees(degrees: Int) = Angle(degreesToRadians(degrees))

        internal const val PI2 = PI.toFloat() * 2f

        internal const val DEG2RAD = PI.toFloat() / 180f
        internal const val RAD2DEG = 180f / PI.toFloat()

        internal const val MAX_DEGREES = 360f
        internal const val MAX_RADIANS = PI2

        internal const val HALF_DEGREES = MAX_DEGREES / 2f
        internal const val HALF_RADIANS = MAX_RADIANS / 2f

        fun cos01(ratio: Float) = kotlin.math.cos(PI2 * ratio)
        fun sin01(ratio: Float) = kotlin.math.sin(PI2 * ratio)
        fun tan01(ratio: Float) = kotlin.math.tan(PI2 * ratio)

        fun cos01(ratio: Double) = kotlin.math.cos(PI2 * ratio)
        fun sin01(ratio: Double) = kotlin.math.sin(PI2 * ratio)
        fun tan01(ratio: Double) = kotlin.math.tan(PI2 * ratio)

        fun degreesToRadians(degrees: Double): Double = degrees * DEG2RAD
        fun radiansToDegrees(radians: Double): Double = radians * RAD2DEG

        fun degreesToRadians(degrees: Float): Float = degrees * DEG2RAD
        fun radiansToDegrees(radians: Float): Float = radians * RAD2DEG

        fun degreesToRadians(degrees: Int): Float = degrees * DEG2RAD
        fun radiansToDegrees(radians: Int): Float = radians * RAD2DEG

        fun shortDistanceTo(from: Angle, to: Angle): Angle {
            val r0 = from.radiansf umod MAX_RADIANS
            val r1 = to.radiansf umod MAX_RADIANS
            val diff = (r1 - r0 + HALF_RADIANS) % MAX_RADIANS - HALF_RADIANS
            return if (diff < -HALF_RADIANS) Angle(diff + MAX_RADIANS) else Angle(diff)
        }

        fun longDistanceTo(from: Angle, to: Angle): Angle {
            val short = shortDistanceTo(from, to)
            return when {
                short == ZERO -> ZERO
                short < ZERO -> 360.degrees + short
                else -> (-360).degrees + short
            }
        }

        fun between(x0: Double, y0: Double, x1: Double, y1: Double): Angle {
            val angle = atan2(y1 - y0, x1 - x0)
            return if (angle < 0) Angle(angle + PI2) else Angle(angle)
        }

        fun between(x0: Int, y0: Int, x1: Int, y1: Int): Angle = between(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())
        fun between(x0: Float, y0: Float, x1: Float, y1: Float): Angle {
            val angle = atan2(y1 - y0, x1 - x0)
            return if (angle < 0) Angle(angle + PI2) else Angle(angle)
        }

        fun between(p0: IPoint, p1: IPoint): Angle = between(p0.x, p0.y, p1.x, p1.y)
    }

    override fun compareTo(other: Angle): Int = this.radiansf.compareTo(other.radiansf)
}

inline fun cos(angle: Angle): Double = kotlin.math.cos(angle.radians)
inline fun sin(angle: Angle): Double = kotlin.math.sin(angle.radians)
inline fun tan(angle: Angle): Double = kotlin.math.tan(angle.radians)

inline fun cosf(angle: Angle): Float = kotlin.math.cos(angle.radiansf)
inline fun sinf(angle: Angle): Float = kotlin.math.sin(angle.radiansf)
inline fun tanf(angle: Angle): Float = kotlin.math.tan(angle.radiansf)

inline fun abs(angle: Angle): Angle = Angle.fromRadians(angle.radiansf.absoluteValue)

val Angle.cosine get() = cos(this)
val Angle.sine get() = sin(this)
val Angle.tangent get() = tan(this)
val Angle.degrees get() = Angle.radiansToDegrees(radians)

val Angle.absoluteValue: Angle get() = Angle.fromRadians(radiansf.absoluteValue)
fun Angle.shortDistanceTo(other: Angle): Angle = Angle.shortDistanceTo(this, other)
fun Angle.longDistanceTo(other: Angle): Angle = Angle.longDistanceTo(this, other)

operator fun Angle.times(scale: Double): Angle = Angle(this.radians * scale)
operator fun Angle.times(scale: Float): Angle = Angle(this.radiansf * scale)
operator fun Angle.times(scale: Int): Angle = Angle(this.radiansf * scale)

operator fun Angle.div(scale: Double): Angle = Angle(this.radians / scale)
operator fun Angle.div(scale: Float): Angle = Angle(this.radiansf / scale)
operator fun Angle.div(scale: Int): Angle = Angle(this.radiansf / scale)

operator fun Angle.div(other: Angle): Double = this.radians / other.radians // Ratio
operator fun Angle.plus(other: Angle): Angle = Angle(this.radiansf + other.radiansf)
operator fun Angle.minus(other: Angle): Angle = Angle(this.radiansf - other.radiansf)
operator fun Angle.unaryMinus(): Angle = Angle(-radiansf)
operator fun Angle.unaryPlus(): Angle = Angle(+radiansf)

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

val Angle.normalized get() = Angle(radiansf umod Angle.MAX_RADIANS)

fun Double.interpolate(l: Angle, r: Angle): Angle = this.interpolate(l.radians, r.radians).radians
