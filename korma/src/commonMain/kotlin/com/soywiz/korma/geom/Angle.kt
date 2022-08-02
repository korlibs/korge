package com.soywiz.korma.geom

import com.soywiz.korma.geom.range.OpenRange
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.internal.umod
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.korma.math.isAlmostEquals
import com.soywiz.korma.math.roundDecimalPlaces
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

@PublishedApi internal const val PI2 = PI * 2.0
@PublishedApi internal const val DEG2RAD = PI / 180.0
@PublishedApi internal const val RAD2DEG = 180.0 / PI

@PublishedApi internal fun Angle_shortDistanceTo(from: Angle, to: Angle): Angle {
    val r0 = from.ratio umod 1.0
    val r1 = to.ratio umod 1.0
    val diff = (r1 - r0 + 0.5) % 1.0 - 0.5
    return if (diff < -0.5) Angle.fromRatio(diff + 1.0) else Angle.fromRatio(diff)
}

@PublishedApi internal fun Angle_longDistanceTo(from: Angle, to: Angle): Angle {
    val short = Angle_shortDistanceTo(from, to)
    return when {
        short == Angle.ZERO -> Angle.ZERO
        short < Angle.ZERO -> Angle.FULL + short
        else -> -Angle.FULL + short
    }
}

@PublishedApi internal fun Angle_between(x0: Double, y0: Double, x1: Double, y1: Double): Angle {
    val angle = Angle.atan2(y1 - y0, x1 - x0)
    return if (angle < Angle.ZERO) angle + Angle.FULL else angle
}

/**
 * Represents an [Angle], [ratio] is in [0, 1] range, [radians] is in [0, 2PI] range, and [degrees] in [0, 360] range
 * The internal representation is in [0, 1] range to reduce rounding errors, since floating points can represent
 * a lot of values in that range.
 *
 * The equivalent old [Angle] constructor is now [Angle.fromRadians]
 */
inline class Angle private constructor(
    /** [0..1] ratio -> [0..360] degrees */
    val ratio: Double
) : Comparable<Angle> {
    /** [0..PI * 2] radians -> [0..360] degrees */
    val radians: Double get() = ratioToRadians(ratio)
    /** [0..360] degrees -> [0..PI * 2] radians -> [0..1] ratio */
    val degrees: Double get() = ratioToDegrees(ratio)

    override fun toString(): String = "${degrees.roundDecimalPlaces(2).niceStr}.degrees"

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        inline val EPSILON get() = fromRatio(0.00001)
        inline val ZERO get() = fromRatio(0.0)
        inline val QUARTER get() = fromRatio(0.25)
        inline val HALF get() = fromRatio(0.5)
        inline val FULL get() = fromRatio(1.0)

        fun fromRatio(ratio: Double): Angle = Angle(ratio)
        inline fun fromRadians(radians: Double): Angle = fromRatio(radiansToRatio(radians))
        inline fun fromDegrees(degrees: Double): Angle = fromRatio(degreesToRatio(degrees))

        inline fun fromRadians(radians: Float) = fromRadians(radians.toDouble())
        inline fun fromDegrees(degrees: Float) = fromDegrees(degrees.toDouble())

        inline fun fromRadians(radians: Int) = fromRadians(radians.toDouble())
        inline fun fromDegrees(degrees: Int) = fromDegrees(degrees.toDouble())

        inline fun cos01(ratio: Double) = kotlin.math.cos(PI2 * ratio)
        inline fun sin01(ratio: Double) = kotlin.math.sin(PI2 * ratio)
        inline fun tan01(ratio: Double) = kotlin.math.tan(PI2 * ratio)

        inline fun atan2(x: Double, y: Double): Angle = fromRadians(kotlin.math.atan2(x, y))
        inline fun atan2(p: IPoint): Angle = atan2(p.x, p.y)

        inline fun degreesToRadians(degrees: Double): Double = degrees * DEG2RAD
        inline fun radiansToDegrees(radians: Double): Double = radians * RAD2DEG

        inline fun ratioToDegrees(ratio: Double): Double = ratio * 360.0
        inline fun ratioToRadians(ratio: Double): Double = ratio * PI2

        inline fun degreesToRatio(degrees: Double): Double = degrees / 360.0
        inline fun radiansToRatio(radians: Double): Double = radians / PI2

        inline fun shortDistanceTo(from: Angle, to: Angle): Angle = Angle_shortDistanceTo(from, to)
        inline fun longDistanceTo(from: Angle, to: Angle): Angle = Angle_longDistanceTo(from, to)
        inline fun between(x0: Double, y0: Double, x1: Double, y1: Double): Angle = Angle_between(x0, y0, x1, y1)

        inline fun between(x0: Int, y0: Int, x1: Int, y1: Int): Angle = between(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())
        inline fun between(x0: Float, y0: Float, x1: Float, y1: Float): Angle = between(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())
        inline fun between(p0: IPoint, p1: IPoint): Angle = between(p0.x, p0.y, p1.x, p1.y)

        inline fun between(ox: Double, oy: Double, x1: Double, y1: Double, x2: Double, y2: Double): Angle =
            between(x1 - ox, y1 - oy, x2 - ox, y2 - oy)

        inline fun between(o: IPoint, v1: IPoint, v2: IPoint): Angle =
            between(o.x, o.y, v1.x, v1.y, v2.x, v2.y)
    }

    override fun compareTo(other: Angle): Int {
        //return this.radians.compareTo(other.radians) // @TODO: Double.compareTo calls EnterFrame/LeaveFrame! because it uses a Double companion object
        val left = this.ratio
        val right = other.ratio
        // @TODO: Handle infinite/NaN? Though usually this won't happen
        if (left < right) return -1
        if (left > right) return +1
        return 0
    }
}

val Angle.cosine: Double get() = cos(this)
val Angle.sine: Double get() = sin(this)
val Angle.tangent: Double get() = tan(this)

inline fun cos(angle: Angle): Double = kotlin.math.cos(angle.radians)
inline fun sin(angle: Angle): Double = kotlin.math.sin(angle.radians)
inline fun tan(angle: Angle): Double = kotlin.math.tan(angle.radians)
inline fun abs(angle: Angle): Angle = Angle.fromRatio(angle.ratio.absoluteValue)
inline fun min(a: Angle, b: Angle): Angle = Angle.fromRatio(min(a.ratio, b.ratio))
inline fun max(a: Angle, b: Angle): Angle = Angle.fromRatio(max(a.ratio, b.ratio))

val Angle.absoluteValue: Angle get() = Angle.fromRatio(ratio.absoluteValue)
fun Angle.shortDistanceTo(other: Angle): Angle = Angle.shortDistanceTo(this, other)
fun Angle.longDistanceTo(other: Angle): Angle = Angle.longDistanceTo(this, other)

operator fun Angle.times(scale: Double): Angle = Angle.fromRatio(this.ratio * scale)
operator fun Angle.div(scale: Double): Angle = Angle.fromRatio(this.ratio / scale)
operator fun Angle.times(scale: Float): Angle = this * scale.toDouble()
operator fun Angle.div(scale: Float): Angle = this / scale.toDouble()
operator fun Angle.times(scale: Int): Angle = this * scale.toDouble()
operator fun Angle.div(scale: Int): Angle = this / scale.toDouble()
operator fun Angle.rem(angle: Angle): Angle = Angle.fromRatio(this.ratio % angle.ratio)
infix fun Angle.umod(angle: Angle): Angle = Angle.fromRatio(this.ratio umod angle.ratio)

operator fun Angle.div(other: Angle): Double = this.ratio / other.ratio // Ratio
operator fun Angle.plus(other: Angle): Angle = Angle.fromRatio(this.ratio + other.ratio)
operator fun Angle.minus(other: Angle): Angle = Angle.fromRatio(this.ratio - other.ratio)
operator fun Angle.unaryMinus(): Angle = Angle.fromRatio(-ratio)
operator fun Angle.unaryPlus(): Angle = Angle.fromRatio(+ratio)
operator fun Angle.compareTo(other: Angle): Int = this.ratio.compareTo(other.ratio)
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

fun Angle.isAlmostEquals(other: Angle, diff: Double = 0.001): Boolean = this.ratio.isAlmostEquals(other.ratio, diff)

val Double.degrees: Angle get() = Angle.fromDegrees(this)
val Double.radians: Angle get() = Angle.fromRadians(this)
val Int.degrees: Angle get() = Angle.fromDegrees(this)
val Int.radians: Angle get() = Angle.fromRadians(this)
val Float.degrees: Angle get() = Angle.fromDegrees(this)
val Float.radians: Angle get() = Angle.fromRadians(this)

val Angle.normalized: Angle get() = Angle.fromRatio(ratio umod 1.0)

fun Double.interpolate(l: Angle, r: Angle): Angle = Angle.fromRatio(this.interpolate(l.ratio, r.ratio))
