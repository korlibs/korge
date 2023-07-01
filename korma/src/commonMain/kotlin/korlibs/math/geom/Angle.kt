package korlibs.math.geom

import korlibs.math.geom.range.*
import korlibs.math.internal.*
import korlibs.math.interpolation.*
import korlibs.math.isAlmostEquals
import korlibs.math.roundDecimalPlaces
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

@PublishedApi internal fun Angle_between(x0: Double, y0: Double, x1: Double, y1: Double, up: Vector2 = Vector2.UP): Angle {
    val angle = Angle.atan2(y1 - y0, x1 - x0)
    return (if (angle < Angle.ZERO) angle + Angle.FULL else angle).adjustFromUp(up)
}

@PublishedApi internal fun Angle.adjustFromUp(up: Vector2): Angle {
    Orientation.checkValidUpVector(up)
    return if (up.y > 0) this else -this
}

/**
 * Represents an [Angle], [ratio] is in [0, 1] range, [radians] is in [0, 2PI] range, and [degrees] in [0, 360] range
 * The internal representation is in [0, 1] range to reduce rounding errors, since floating points can represent
 * a lot of values in that range.
 *
 * The equivalent old [Angle] constructor is now [Angle.fromRadians]
 *
 * Angles advance counter-clock-wise, starting with 0.degrees representing the right vector:
 *
 * Depending on what the up vector means, then numeric values of sin might be negated.
 *
 *   0.degrees represent right:    up=Vector2.UP: cos =+1, sin= 0 || up=Vector2.UP_SCREEN: cos =+1, sin= 0
 *  90.degrees represents up:      up=Vector2.UP: cos = 0, sin=+1 || up=Vector2.UP_SCREEN: cos = 0, sin=-1
 * 180.degrees represents left:    up=Vector2.UP: cos =-1, sin= 0 || up=Vector2.UP_SCREEN: cos =-1, sin= 0
 * 270.degrees represents down:    up=Vector2.UP: cos = 0, sin=-1 || up=Vector2.UP_SCREEN: cos = 0, sin=+1
 */
//@KormaValueApi
inline class Angle @PublishedApi internal constructor(
    /** [0..1] ratio -> [0..360] degrees */
    val ratioF: Float
) : Comparable<Angle> {
    val ratio: Float get() = ratioF
    val ratioD: Double get() = ratioF.toDouble()

    val radians: Float get() = ratioToRadians(ratioF)
    val degrees: Float get() = ratioToDegrees(ratioF)

    /** [0..PI * 2] radians -> [0..360] degrees */
    val radiansD: Double get() = ratioToRadians(ratioD)
    /** [0..360] degrees -> [0..PI * 2] radians -> [0..1] ratio */
    val degreesD: Double get() = ratioToDegrees(ratioD)

    val cosine: Float get() = kotlin.math.cos(radians)
    val sine: Float get() = kotlin.math.sin(radians)
    val tangent: Float get() = kotlin.math.tan(radians)

    val cosineD: Double get() = kotlin.math.cos(radiansD)
    val sineD: Double get() = kotlin.math.sin(radiansD)
    val tangentD: Double get() = kotlin.math.tan(radiansD)

    val cosineF: Float get() = kotlin.math.cos(radians)
    val sineF: Float get() = kotlin.math.sin(radians)
    val tangentF: Float get() = kotlin.math.tan(radians)

    fun cosine(up: Vector2 = Vector2.UP): Float = cosineF(up)
    fun sine(up: Vector2 = Vector2.UP): Float = sineF(up)
    fun tangent(up: Vector2 = Vector2.UP): Float = tangentF(up)

    fun cosineD(up: Vector2 = Vector2.UP): Double = adjustFromUp(up).cosineD
    fun sineD(up: Vector2 = Vector2.UP): Double = adjustFromUp(up).sineD
    fun tangentD(up: Vector2 = Vector2.UP): Double = adjustFromUp(up).tangentD

    fun cosineF(up: Vector2 = Vector2.UP): Float = adjustFromUp(up).cosineF
    fun sineF(up: Vector2 = Vector2.UP): Float = adjustFromUp(up).sineF
    fun tangentF(up: Vector2 = Vector2.UP): Float = adjustFromUp(up).tangentF

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

    /** Normalize between 0..1  ... 0..(PI*2).radians ... 0..360.degrees */
    val normalized: Angle get() = fromRatio(ratioF umod 1f)
    /** Normalize between -.5..+.5  ... -PI..+PI.radians ... -180..+180.degrees */
    val normalizedHalf: Angle get() {
        val res = normalized
        return if (res > Angle.HALF) -Angle.FULL + res else res
    }

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

    override fun toString(): String = "${degreesD.roundDecimalPlaces(2).niceStr}.degrees"

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val EPSILON = Angle(0.00001f)
        val ZERO = Angle(0.0f)
        val QUARTER = Angle(0.25f)
        val HALF = Angle(0.5f)
        val THREE_QUARTERS = Angle(0.75f)
        val FULL = Angle(1.0f)

        inline fun fromRatio(ratio: Float): Angle = Angle(ratio)
        inline fun fromRatio(ratio: Double): Angle = Angle(ratio.toFloat())

        inline fun fromRadians(radians: Double): Angle = fromRatio(radiansToRatio(radians))
        inline fun fromRadians(radians: Float) = fromRadians(radians.toDouble())
        inline fun fromRadians(radians: Int) = fromRadians(radians.toDouble())

        inline fun fromDegrees(degrees: Double): Angle = fromRatio(degreesToRatio(degrees))
        inline fun fromDegrees(degrees: Float) = fromDegrees(degrees.toDouble())
        inline fun fromDegrees(degrees: Int) = fromDegrees(degrees.toDouble())

        @Deprecated("", ReplaceWith("Angle.fromRatio(ratio).cosineD"))
        inline fun cos01(ratio: Double): Double = Angle.fromRatio(ratio).cosineD
        @Deprecated("", ReplaceWith("Angle.fromRatio(ratio).sineD"))
        inline fun sin01(ratio: Double): Double = Angle.fromRatio(ratio).sineD
        @Deprecated("", ReplaceWith("Angle.fromRatio(ratio).tangentD"))
        inline fun tan01(ratio: Double): Double = Angle.fromRatio(ratio).tangentD

        inline fun atan2(x: Float, y: Float, up: Vector2 = Vector2.UP): Angle = fromRadians(kotlin.math.atan2(x, y)).adjustFromUp(up)
        inline fun atan2(x: Double, y: Double, up: Vector2 = Vector2.UP): Angle = fromRadians(kotlin.math.atan2(x, y)).adjustFromUp(up)
        inline fun atan2(p: Point, up: Vector2 = Vector2.UP): Angle = atan2(p.xD, p.yD, up)

        inline fun asin(v: Double): Angle = kotlin.math.asin(v).radians
        inline fun asin(v: Float): Angle = kotlin.math.asin(v).radians

        inline fun acos(v: Double): Angle = kotlin.math.acos(v).radians
        inline fun acos(v: Float): Angle = kotlin.math.acos(v).radians

        fun arcCosine(v: Double): Angle = kotlin.math.acos(v).radians
        fun arcCosine(v: Float): Angle = kotlin.math.acos(v).radians

        fun arcSine(v: Double): Angle = kotlin.math.asin(v).radians
        fun arcSine(v: Float): Angle = kotlin.math.asin(v).radians

        fun arcTangent(x: Double, y: Double): Angle = kotlin.math.atan2(x, y).radians
        fun arcTangent(x: Float, y: Float): Angle = kotlin.math.atan2(x, y).radians
        fun arcTangent(v: Vector2): Angle = kotlin.math.atan2(v.x, v.y).radians

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
        inline fun between(x0: Double, y0: Double, x1: Double, y1: Double, up: Vector2 = Vector2.UP): Angle = Angle_between(x0, y0, x1, y1, up)

        inline fun between(x0: Int, y0: Int, x1: Int, y1: Int, up: Vector2 = Vector2.UP): Angle = between(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble(), up)
        inline fun between(x0: Float, y0: Float, x1: Float, y1: Float, up: Vector2 = Vector2.UP): Angle = between(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble(), up)
        inline fun between(p0: Point, p1: Point, up: Vector2 = Vector2.UP): Angle = between(p0.x, p0.y, p1.x, p1.y, up)

        inline fun between(ox: Double, oy: Double, x1: Double, y1: Double, x2: Double, y2: Double, up: Vector2 = Vector2.UP): Angle = between(x1 - ox, y1 - oy, x2 - ox, y2 - oy, up)
        inline fun between(ox: Float, oy: Float, x1: Float, y1: Float, x2: Float, y2: Float, up: Vector2 = Vector2.UP): Angle = between(x1 - ox, y1 - oy, x2 - ox, y2 - oy, up)

        inline fun between(o: Point, v1: Point, v2: Point, up: Vector2 = Vector2.UP): Angle = between(o.x, o.y, v1.x, v1.y, v2.x, v2.y, up)
    }
}

inline fun cos(angle: Angle, up: Vector2 = Vector2.UP): Float = angle.cosine(up)
inline fun sin(angle: Angle, up: Vector2 = Vector2.UP): Float = angle.sine(up)
inline fun tan(angle: Angle, up: Vector2 = Vector2.UP): Float = angle.tangent(up)

inline fun cosd(angle: Angle, up: Vector2 = Vector2.UP): Double = angle.cosineD(up)
inline fun sind(angle: Angle, up: Vector2 = Vector2.UP): Double = angle.sineD(up)
inline fun tand(angle: Angle, up: Vector2 = Vector2.UP): Double = angle.tangentD(up)

inline fun cosf(angle: Angle, up: Vector2 = Vector2.UP): Float = angle.cosineF(up)
inline fun sinf(angle: Angle, up: Vector2 = Vector2.UP): Float = angle.sineF(up)
inline fun tanf(angle: Angle, up: Vector2 = Vector2.UP): Float = angle.tangentF(up)

inline fun abs(angle: Angle): Angle = Angle.fromRatio(angle.ratio.absoluteValue)
inline fun min(a: Angle, b: Angle): Angle = Angle.fromRatio(min(a.ratio, b.ratio))
inline fun max(a: Angle, b: Angle): Angle = Angle.fromRatio(max(a.ratio, b.ratio))

fun Angle.clamp(min: Angle, max: Angle): Angle = min(max(this, min), max)

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
        (rn - ln).absoluteValue <= Angle.HALF -> Angle.fromRadians(ratio.interpolate(ln.radians, rn.radians))
        ln < rn -> Angle.fromRadians(ratio.interpolate((ln + Angle.FULL).radians, rn.radians)).normalized
        else -> Angle.fromRadians(ratio.interpolate(ln.radians, (rn + Angle.FULL).radians)).normalized
    }
}
