package korlibs.math.geom

import korlibs.math.geom.range.*
import korlibs.math.internal.*
import korlibs.math.interpolation.*
import korlibs.math.math.*
import kotlin.math.*

@PublishedApi internal const val PIF = (PI).toFloat()
@PublishedApi internal const val PI2 = PI * 2.0
@PublishedApi internal const val PI2F = (PI * 2f).toFloat()
@PublishedApi internal const val DEG2RAD = PI / 180.0
@PublishedApi internal const val RAD2DEG = 180.0 / PI

@PublishedApi internal fun Angle_shortDistanceTo(from: Angle, to: Angle): Angle {
    val r0 = from.ratioF umod 1f
    val r1 = to.ratioF umod 1f
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
//@KormaValueApi
inline class Angle private constructor(
    /** [0..1] ratio -> [0..360] degrees */
    val ratioF: Float
) : Comparable<Angle> {
    val ratio: Float get() = ratioF
    val ratioD: Double get() = ratioF.toDouble()

    val radiansF: Float get() = ratioToRadians(ratioF)
    val degreesF: Float get() = ratioToDegrees(ratioF)

    /** [0..PI * 2] radians -> [0..360] degrees */
    val radians: Double get() = ratioToRadians(ratioD)
    /** [0..360] degrees -> [0..PI * 2] radians -> [0..1] ratio */
    val degrees: Double get() = ratioToDegrees(ratioD)

    val cosine: Float get() = kotlin.math.cos(radiansF)
    val sine: Float get() = kotlin.math.sin(radiansF)
    val tangent: Float get() = kotlin.math.tan(radiansF)

    val cosineD: Double get() = kotlin.math.cos(radians)
    val sineD: Double get() = kotlin.math.sin(radians)
    val tangentD: Double get() = kotlin.math.tan(radians)

    val cosineF: Float get() = kotlin.math.cos(radiansF)
    val sineF: Float get() = kotlin.math.sin(radiansF)
    val tangentF: Float get() = kotlin.math.tan(radiansF)

    val absoluteValue: Angle get() = fromRatio(ratioF.absoluteValue)
    fun shortDistanceTo(other: Angle): Angle = Angle.shortDistanceTo(this, other)
    fun longDistanceTo(other: Angle): Angle = Angle.longDistanceTo(this, other)

    operator fun times(scale: Double): Angle = fromRatio(this.ratioF * scale)
    operator fun div(scale: Double): Angle = fromRatio(this.ratioF / scale)
    operator fun times(scale: Float): Angle = fromRatio(this.ratioF * scale)
    operator fun div(scale: Float): Angle = fromRatio(this.ratioF / scale)
    operator fun times(scale: Int): Angle = fromRatio(this.ratioF * scale)
    operator fun div(scale: Int): Angle = fromRatio(this.ratioF / scale)
    operator fun rem(angle: Angle): Angle = fromRatio(this.ratioF % angle.ratioF)
    infix fun umod(angle: Angle): Angle = fromRatio(this.ratioF umod angle.ratioF)

    operator fun div(other: Angle): Float = this.ratioF / other.ratioF // Ratio
    operator fun plus(other: Angle): Angle = fromRatio(this.ratioF + other.ratioF)
    operator fun minus(other: Angle): Angle = fromRatio(this.ratioF - other.ratioF)
    operator fun unaryMinus(): Angle = fromRatio(-ratioF)
    operator fun unaryPlus(): Angle = fromRatio(+ratioF)

    fun inBetweenInclusive(min: Angle, max: Angle): Boolean = inBetween(min, max, inclusive = true)
    fun inBetweenExclusive(min: Angle, max: Angle): Boolean = inBetween(min, max, inclusive = false)

    infix fun inBetween(range: ClosedRange<Angle>): Boolean = inBetween(range.start, range.endInclusive, inclusive = true)
    infix fun inBetween(range: OpenRange<Angle>): Boolean = inBetween(range.start, range.endExclusive, inclusive = false)

    fun inBetween(min: Angle, max: Angle, inclusive: Boolean): Boolean {
        val nthis = this.normalized
        val nmin = min.normalized
        val nmax = max.normalized
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        return when {
            nmin > nmax -> nthis >= nmin || (if (inclusive) nthis <= nmax else nthis < nmax)
            else -> nthis >= nmin && (if (inclusive) nthis <= nmax else nthis < nmax)
        }
    }

    fun isAlmostEquals(other: Angle, epsilon: Float = 0.001f): Boolean = this.ratioF.isAlmostEquals(other.ratioF, epsilon)
    fun isAlmostZero(epsilon: Float = 0.001f): Boolean = isAlmostEquals(ZERO, epsilon)

    fun isAlmostEquals(other: Angle, epsilon: Double): Boolean = isAlmostEquals(other, epsilon.toFloat())
    fun isAlmostZero(epsilon: Double): Boolean = isAlmostZero(epsilon.toFloat())

    val normalized: Angle get() = fromRatio(ratioF umod 1f)

    override operator fun compareTo(other: Angle): Int = this.ratio.compareTo(other.ratio)

    //override fun compareTo(other: Angle): Int {
    //    //return this.radians.compareTo(other.radians) // @TODO: Double.compareTo calls EnterFrame/LeaveFrame! because it uses a Double companion object
    //    val left = this.ratio
    //    val right = other.ratio
    //    // @TODO: Handle infinite/NaN? Though usually this won't happen
    //    if (left < right) return -1
    //    if (left > right) return +1
    //    return 0
    //}

    override fun toString(): String = "${degrees.roundDecimalPlaces(2).niceStr}.degrees"

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        inline val EPSILON get() = fromRatio(0.00001f)
        inline val ZERO get() = fromRatio(0.0f)
        inline val QUARTER get() = fromRatio(0.25f)
        inline val HALF get() = fromRatio(0.5f)
        inline val FULL get() = fromRatio(1.0f)

        fun fromRatio(ratio: Float): Angle = Angle(ratio)
        fun fromRatio(ratio: Double): Angle = Angle(ratio.toFloat())
        inline fun fromRadians(radians: Double): Angle = fromRatio(radiansToRatio(radians))
        inline fun fromDegrees(degrees: Double): Angle = fromRatio(degreesToRatio(degrees))

        inline fun fromRadians(radians: Float) = fromRadians(radians.toDouble())
        inline fun fromDegrees(degrees: Float) = fromDegrees(degrees.toDouble())

        inline fun fromRadians(radians: Int) = fromRadians(radians.toDouble())
        inline fun fromDegrees(degrees: Int) = fromDegrees(degrees.toDouble())

        inline fun cos01(ratio: Double) = kotlin.math.cos(PI2 * ratio)
        inline fun sin01(ratio: Double) = kotlin.math.sin(PI2 * ratio)
        inline fun tan01(ratio: Double) = kotlin.math.tan(PI2 * ratio)

        inline fun atan2(x: Float, y: Float): Angle = fromRadians(kotlin.math.atan2(x, y))
        inline fun atan2(x: Double, y: Double): Angle = fromRadians(kotlin.math.atan2(x, y))
        inline fun atan2(p: Point): Angle = atan2(p.xD, p.yD)

        inline fun ratioToDegrees(ratio: Double): Double = ratio * 360.0
        inline fun ratioToRadians(ratio: Double): Double = ratio * PI2
        inline fun degreesToRatio(degrees: Double): Double = degrees / 360.0
        inline fun radiansToRatio(radians: Double): Double = radians / PI2

        inline fun degreesToRatio(degrees: Float): Float = degrees / 360f
        inline fun radiansToRatio(radians: Float): Float = radians / PI2F
        inline fun ratioToDegrees(ratio: Float): Float = ratio * 360f
        inline fun ratioToRadians(ratio: Float): Float = ratio * PI2F

        inline fun shortDistanceTo(from: Angle, to: Angle): Angle = Angle_shortDistanceTo(from, to)
        inline fun longDistanceTo(from: Angle, to: Angle): Angle = Angle_longDistanceTo(from, to)
        inline fun between(x0: Double, y0: Double, x1: Double, y1: Double): Angle = Angle_between(x0, y0, x1, y1)

        inline fun between(x0: Int, y0: Int, x1: Int, y1: Int): Angle = between(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())
        inline fun between(x0: Float, y0: Float, x1: Float, y1: Float): Angle = between(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())
        inline fun between(p0: Point, p1: Point): Angle = between(p0.x, p0.y, p1.x, p1.y)

        inline fun between(ox: Double, oy: Double, x1: Double, y1: Double, x2: Double, y2: Double): Angle = between(x1 - ox, y1 - oy, x2 - ox, y2 - oy)
        inline fun between(ox: Float, oy: Float, x1: Float, y1: Float, x2: Float, y2: Float): Angle = between(x1 - ox, y1 - oy, x2 - ox, y2 - oy)

        inline fun between(o: Point, v1: Point, v2: Point): Angle = between(o.x, o.y, v1.x, v1.y, v2.x, v2.y)
    }
}

inline fun cos(angle: Angle): Float = angle.cosine
inline fun sin(angle: Angle): Float = angle.sine
inline fun tan(angle: Angle): Float = angle.tangent

inline fun cosd(angle: Angle): Double = angle.cosineD
inline fun sind(angle: Angle): Double = angle.sineD
inline fun tand(angle: Angle): Double = angle.tangentD

inline fun cosf(angle: Angle): Float = angle.cosineF
inline fun sinf(angle: Angle): Float = angle.sineF
inline fun tanf(angle: Angle): Float = angle.tangentF

inline fun abs(angle: Angle): Angle = Angle.fromRatio(angle.ratio.absoluteValue)
inline fun min(a: Angle, b: Angle): Angle = Angle.fromRatio(min(a.ratio, b.ratio))
inline fun max(a: Angle, b: Angle): Angle = Angle.fromRatio(max(a.ratio, b.ratio))

operator fun ClosedRange<Angle>.contains(angle: Angle): Boolean = angle.inBetween(this.start, this.endInclusive, inclusive = true)
operator fun OpenRange<Angle>.contains(angle: Angle): Boolean = angle.inBetween(this.start, this.endExclusive, inclusive = false)
infix fun Angle.until(other: Angle): OpenRange<Angle> = OpenRange(this, other)

val Double.degrees: Angle get() = Angle.fromDegrees(this)
val Double.radians: Angle get() = Angle.fromRadians(this)
val Int.degrees: Angle get() = Angle.fromDegrees(this)
val Int.radians: Angle get() = Angle.fromRadians(this)
val Float.degrees: Angle get() = Angle.fromDegrees(this)
val Float.radians: Angle get() = Angle.fromRadians(this)

fun Ratio.interpolateAngle(l: Angle, r: Angle, minimizeAngle: Boolean): Angle = _interpolateAngleAny(this, l, r, minimizeAngle)
fun Ratio.interpolateAngle(l: Angle, r: Angle): Angle = interpolateAngle(l, r, minimizeAngle = true)
fun Ratio.interpolateAngleNormalized(l: Angle, r: Angle): Angle = interpolateAngle(l, r, minimizeAngle = true)
fun Ratio.interpolateAngleDenormalized(l: Angle, r: Angle): Angle = interpolateAngle(l, r, minimizeAngle = false)

private fun _interpolateAngleAny(ratio: Ratio, l: Angle, r: Angle, minimizeAngle: Boolean = true): Angle {
    if (!minimizeAngle) return Angle.fromRatio(ratio.interpolate(l.ratio, r.ratio))
    val ln = l.normalized
    val rn = r.normalized
    return when {
        (rn - ln).absoluteValue <= Angle.HALF -> Angle.fromRadians(ratio.interpolate(ln.radiansF, rn.radiansF))
        ln < rn -> Angle.fromRadians(ratio.interpolate((ln + Angle.FULL).radiansF, rn.radiansF)).normalized
        else -> Angle.fromRadians(ratio.interpolate(ln.radiansF, (rn + Angle.FULL).radiansF)).normalized
    }
}
