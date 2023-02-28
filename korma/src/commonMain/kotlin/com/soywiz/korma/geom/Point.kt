@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korma.geom

import com.soywiz.kds.*
import com.soywiz.kds.pack.*
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.kmem.clamp
import com.soywiz.korma.annotations.*
import com.soywiz.korma.math.isAlmostEquals
import com.soywiz.korma.math.isAlmostZero
import com.soywiz.korma.math.roundDecimalPlaces
import kotlin.math.*

typealias Vector2D = Point
typealias IVector2D = IPoint
typealias MVector2D = MPoint

//////////////////////////////
// VALUE CLASSES
//////////////////////////////

fun Point(p: Point): Point = Point(p.raw)

//data class Point(val x: Double, val y: Double) {
// @JvmInline value
//@KormaValueApi
//data class Point(val x: Double, val y: Double) {
inline class Point internal constructor(internal val raw: Float2Pack) {
    val x: Float get() = raw.x
    val y: Float get() = raw.y

    val xF: Float get() = x
    val yF: Float get() = y

    val xD: Double get() = x.toDouble()
    val yD: Double get() = y.toDouble()

    constructor(x: Float, y: Float) : this(Float2Pack(x, y))
    constructor(x: Double, y: Double) : this(Float2Pack(x.toFloat(), y.toFloat()))
    constructor(x: Int, y: Int) : this(Float2Pack(x.toFloat(), y.toFloat()))
    constructor(p: IPoint) : this(p.x.toFloat(), p.y.toFloat())
    //constructor(p: Point) : this(p.raw)
    constructor() : this(0f, 0f)
    //constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
    //constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())

    //operator fun component1(): Double = x
    //operator fun component2(): Double = y


    inline operator fun plus(that: Point): Point = Point(x + that.x, y + that.y)
    inline operator fun minus(that: Point): Point = Point(x - that.x, y - that.y)
    inline operator fun times(that: Point): Point = Point(x * that.x, y * that.y)
    inline operator fun div(that: Point): Point = Point(x / that.x, y / that.y)

    inline operator fun times(scale: Float): Point = Point(x * scale, y * scale)
    inline operator fun times(scale: Double): Point = this * scale.toFloat()
    inline operator fun times(scale: Int): Point = this * scale.toDouble()

    inline operator fun div(scale: Float): Point = Point(x / scale, y / scale)
    inline operator fun div(scale: Double): Point = this / scale.toFloat()
    inline operator fun div(scale: Int): Point = this / scale.toDouble()

    fun distanceTo(x: Float, y: Float): Float = hypot(x - this.x, y - this.y)
    fun distanceTo(x: Double, y: Double): Float = this.distanceTo(x.toFloat(), y.toFloat())
    fun distanceTo(x: Int, y: Int): Float = this.distanceTo(x.toDouble(), y.toDouble())
    fun distanceTo(that: Point): Float = distanceTo(that.x, that.y)

    infix fun dot(that: Point): Float = this.x * that.x + this.y * that.y

    fun angleTo(other: Point): Angle = Angle.between(this.x, this.y, other.x, other.y)
    val angle: Angle get() = Angle.between(0f, 0f, this.x, this.y)

    inline fun transformed(m: IMatrix): Point = m.transform(this)
    fun transformX(m: IMatrix): Float = m.transform(this).x
    fun transformY(m: IMatrix): Float = m.transform(this).y

    inline fun transformed(m: Matrix): Point = m.transform(this)
    fun transformX(m: Matrix): Float = m.transform(this).x
    fun transformY(m: Matrix): Float = m.transform(this).y

    operator fun get(component: Int) = when (component) {
        0 -> x; 1 -> y
        else -> throw IndexOutOfBoundsException("Point doesn't have $component component")
    }
    val length: Float get() = hypot(x, y)
    val magnitude: Float get() = hypot(x, y)
    val normalized: Point get() = this * (1f / magnitude)

    val int: PointInt get() = PointInt(x.toInt(), y.toInt())
    val intRound: PointInt get() = PointInt(x.roundToInt(), y.roundToInt())

    fun roundDecimalPlaces(places: Int): Point = Point(x.roundDecimalPlaces(places), y.roundDecimalPlaces(places))
    fun round(): Point = Point(kotlin.math.round(x), kotlin.math.round(y))
    fun ceil(): Point = Point(kotlin.math.ceil(x), kotlin.math.ceil(y))
    fun floor(): Point = Point(kotlin.math.floor(x), kotlin.math.floor(y))

    //fun copy(x: Double = this.x, y: Double = this.y): Point = Point(x, y)

    fun isAlmostEquals(other: Point, epsilon: Float = 0.000001f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon)

    override fun toString(): String = "(${x.niceStr}, ${y.niceStr})"

    fun isNaN(): Boolean = this.x.isNaN() && this.y.isNaN()

    companion object {
        val ZERO = Point(0f, 0f)
        val NaN = Point(Float.NaN, Float.NaN)

        //inline operator fun invoke(x: Int, y: Int): Point = Point(x.toDouble(), y.toDouble())
        //inline operator fun invoke(x: Float, y: Float): Point = Point(x.toDouble(), y.toDouble())

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        inline fun fromPolar(x: Float, y: Float, angle: Angle, length: Double = 1.0): Point = Point(x + angle.cosine * length, y + angle.sine * length)
        inline fun fromPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0): Point = Point(x + angle.cosine * length, y + angle.sine * length)
        inline fun fromPolar(base: Point, angle: Angle, length: Double = 1.0): Point = fromPolar(base.x, base.y, angle, length)
        inline fun fromPolar(angle: Angle, length: Double = 1.0): Point = fromPolar(0.0, 0.0, angle, length)

        inline fun middle(a: Point, b: Point): Point = (a + b) * 0.5

        fun angle(ax: Double, ay: Double, bx: Double, by: Double): Angle = Angle.between(ax, ay, bx, by)
        fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Angle = Angle.between(x1 - x2, y1 - y2, x1 - x3, y1 - y3)
        //acos(((ax * bx) + (ay * by)) / (hypot(ax, ay) * hypot(bx, by)))
        fun angleArc(a: Point, b: Point): Angle = Angle.fromRadians(acos(a dot b) / (a.length * b.length))
        fun angleFull(a: Point, b: Point): Angle = Angle.between(a, b)

        fun distance(a: Double, b: Double): Double = kotlin.math.abs(a - b)
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = kotlin.math.hypot(x1 - x2, y1 - y2)
        fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float = kotlin.math.hypot(x1 - x2, y1 - y2)
        fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Float = distance(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())
        fun distance(a: Point, b: Point): Float = distance(a.x, a.y, b.x, b.y)
        fun distance(a: PointInt, b: PointInt): Float = distance(a.x, a.y, b.x, b.y)

        fun distanceSquared(a: Point, b: Point): Float = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(a: PointInt, b: PointInt): Int = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(x1: Double, y1: Double, x2: Double, y2: Double): Double = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Float, y1: Float, x2: Float, y2: Float): Float = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Int, y1: Int, x2: Int, y2: Int): Int = square(x1 - x2) + square(y1 - y2)

        inline fun direction(a: Point, b: Point): Point = b - a

        fun compare(l: Point, r: Point): Int = compare(l.x, l.y, r.x, r.y)
        fun compare(lx: Float, ly: Float, rx: Float, ry: Float): Int = ly.compareTo(ry).let { ret -> if (ret == 0) lx.compareTo(rx) else ret }
        fun compare(lx: Double, ly: Double, rx: Double, ry: Double): Int = ly.compareTo(ry).let { ret -> if (ret == 0) lx.compareTo(rx) else ret }

        private fun square(x: Double): Double = x * x
        private fun square(x: Float): Float = x * x
        private fun square(x: Int): Int = x * x

        fun dot(aX: Double, aY: Double, bX: Double, bY: Double): Double = (aX * bX) + (aY * bY)
        fun dot(aX: Float, aY: Float, bX: Float, bY: Float): Float = (aX * bX) + (aY * bY)
        fun dot(a: Point, b: Point): Float = dot(a.x, a.y, b.x, b.y)

        fun isCollinear(xa: Double, ya: Double, x: Double, y: Double, xb: Double, yb: Double): Boolean =
            (((x - xa) / (y - ya)) - ((xa - xb) / (ya - yb))).absoluteValue.isAlmostZero()

        fun isCollinear(xa: Int, ya: Int, x: Int, y: Int, xb: Int, yb: Int): Boolean = isCollinear(
            xa.toDouble(), ya.toDouble(),
            x.toDouble(), y.toDouble(),
            xb.toDouble(), yb.toDouble(),
        )

        // https://algorithmtutor.com/Computational-Geometry/Determining-if-two-consecutive-segments-turn-left-or-right/
        /** < 0 left, > 0 right, 0 collinear */
        fun orientation(p1: Point, p2: Point, p3: Point): Float = orientation(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
        fun orientation(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Float = crossProduct(cx - ax, cy - ay, bx - ax, by - ay)
        fun orientation(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double = crossProduct(cx - ax, cy - ay, bx - ax, by - ay)

        fun crossProduct(ax: Float, ay: Float, bx: Float, by: Float): Float = (ax * by) - (bx * ay)
        fun crossProduct(ax: Double, ay: Double, bx: Double, by: Double): Double = (ax * by) - (bx * ay)
        fun crossProduct(p1: Point, p2: Point): Float = crossProduct(p1.x, p1.y, p2.x, p2.y)
    }
}

//@KormaValueApi
inline class PointInt internal constructor(internal val raw: Int2Pack) {
    val x: Int get() = raw.x
    val y: Int get() = raw.y

    constructor() : this(Int2Pack(0, 0))
    constructor(x: Int, y: Int) : this(Int2Pack(x, y))

    val mutable: IPointInt get() = MPointInt(x, y)

    operator fun plus(that: PointInt): PointInt = PointInt(this.x + that.x, this.y + that.y)
    operator fun minus(that: PointInt): PointInt = PointInt(this.x - that.x, this.y - that.y)
    operator fun times(that: PointInt): PointInt = PointInt(this.x * that.x, this.y * that.y)
    operator fun div(that: PointInt): PointInt = PointInt(this.x / that.x, this.y / that.y)
    operator fun rem(that: PointInt): PointInt = PointInt(this.x % that.x, this.y % that.y)
}


//////////////////////////////
// IMMUTABLE INTERFACES
//////////////////////////////

@KormaMutableApi
interface IPoint {
    companion object {
        val ZERO: IPoint get() = MPoint.Zero

        operator fun invoke(): IPoint = MPoint(0.0, 0.0)
        operator fun invoke(v: IPoint): IPoint = MPoint(v.x, v.y)
        operator fun invoke(x: Double, y: Double): IPoint = MPoint(x, y)
        operator fun invoke(x: Float, y: Float): IPoint = MPoint(x, y)
        operator fun invoke(x: Int, y: Int): IPoint = MPoint(x, y)
    }

    val x: Double
    val y: Double

    val point: Point get() = Point(x, y)

    val niceStr: String get() = "(${x.niceStr}, ${y.niceStr})"
    fun niceStr(decimalPlaces: Int): String = "(${x.niceStr(decimalPlaces)}, ${y.niceStr(decimalPlaces)})"

    operator fun plus(that: IPoint): IPoint = IPoint(x + that.x, y + that.y)
    operator fun minus(that: IPoint): IPoint = IPoint(x - that.x, y - that.y)
    operator fun times(that: IPoint): IPoint = IPoint(x * that.x, y * that.y)
    operator fun div(that: IPoint): IPoint = IPoint(x / that.x, y / that.y)

    operator fun times(scale: Double): IPoint = IPoint(x * scale, y * scale)
    operator fun times(scale: Float): IPoint = this * scale.toDouble()
    operator fun times(scale: Int): IPoint = this * scale.toDouble()

    operator fun div(scale: Double): IPoint = IPoint(x / scale, y / scale)
    operator fun div(scale: Float): IPoint = this / scale.toDouble()
    operator fun div(scale: Int): IPoint = this / scale.toDouble()

    fun distanceTo(x: Double, y: Double): Double = hypot(x - this.x, y - this.y)
    fun distanceTo(x: Float, y: Float): Float = this.distanceTo(x.toDouble(), y.toDouble()).toFloat()
    fun distanceTo(x: Int, y: Int): Double = this.distanceTo(x.toDouble(), y.toDouble())

    fun distanceTo(that: IPoint): Double = distanceTo(that.x, that.y)

    infix fun dot(that: IPoint): Double = this.x * that.x + this.y * that.y
    fun angleTo(other: IPoint): Angle = Angle.between(this.x, this.y, other.x, other.y)
    val angle: Angle get() = Angle.between(0.0, 0.0, this.x, this.y)
    fun transformed(mat: MMatrix, out: MPoint = MPoint()): MPoint = out.setToTransform(mat, this)
    fun transformX(m: MMatrix?): Double = m?.transformX(this) ?: x
    fun transformY(m: MMatrix?): Double = m?.transformY(this) ?: y
    operator fun IPoint.get(component: Int) = when (component) {
        0 -> x; 1 -> y
        else -> throw IndexOutOfBoundsException("IPoint doesn't have $component component")
    }
    val unit: IPoint get() = this / this.length
    val length: Double get() = hypot(x, y)
    val magnitude: Double get() = hypot(x, y)
    val normalized: IPoint
        get() {
            val imag = 1.0 / magnitude
            return IPoint(x * imag, y * imag)
        }
    val mutable: MPoint get() = MPoint(x, y)
    val immutable: IPoint get() = IPoint(x, y)
    fun isAlmostEquals(other: IPoint, epsilon: Double = 0.000001): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon)
}

@KormaMutableApi
fun IPoint.copy(x: Double = this.x, y: Double = this.y): IPoint = IPoint(x, y)

@KormaMutableApi
interface IMPoint : IPoint {
    override var x: Double
    override var y: Double
}

//////////////////////////////
// MUTABLE IMPLEMENTATIONS
//////////////////////////////

@KormaMutableApi
data class MPoint(
    override var x: Double,
    override var y: Double
    //override var xf: Float,
    //override var yf: Float
) : MutableInterpolable<MPoint>, Interpolable<MPoint>, Comparable<IPoint>, IPoint, IMPoint {
    //constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    override fun compareTo(other: IPoint): Int = compare(this.x, this.y, other.x, other.y)
    fun compareTo(other: MPoint): Int = compare(this.x, this.y, other.x, other.y)

    fun clear() = setToZero()
    fun setToZero() = setTo(0.0, 0.0)
    fun setToOne() = setTo(1.0, 1.0)
    fun setToUp() = setTo(0.0, -1.0)
    fun setToDown() = setTo(0.0, +1.0)
    fun setToLeft() = setTo(-1.0, 0.0)
    fun setToRight() = setTo(+1.0, 0.0)

    fun MPoint.copyFrom(that: IPoint) = setTo(that.x, that.y)
    fun MPoint.add(p: IPoint) = this.setToAdd(this, p)
    fun MPoint.sub(p: IPoint) = this.setToSub(this, p)
    fun MPoint.setToTransform(mat: MMatrix, p: IPoint): MPoint = setToTransform(mat, p.x, p.y)
    fun MPoint.setToTransform(mat: MMatrix, x: Double, y: Double): MPoint = setTo(mat.transformX(x, y), mat.transformY(x, y))
    fun MPoint.setToAdd(a: IPoint, b: IPoint): MPoint = setTo(a.x + b.x, a.y + b.y)
    fun MPoint.setToSub(a: IPoint, b: IPoint): MPoint = setTo(a.x - b.x, a.y - b.y)
    fun MPoint.setToMul(a: IPoint, b: IPoint): MPoint = setTo(a.x * b.x, a.y * b.y)
    fun MPoint.setToMul(a: IPoint, s: Double): MPoint = setTo(a.x * s, a.y * s)
    inline fun MPoint.setToMul(a: IPoint, s: Number): MPoint = setToMul(a, s.toDouble())
    fun MPoint.setToDiv(a: IPoint, b: IPoint): MPoint = setTo(a.x / b.x, a.y / b.y)
    fun MPoint.setToDiv(a: IPoint, s: Double): MPoint = setTo(a.x / s, a.y / s)
    inline fun MPoint.setToDiv(a: IPoint, s: Number): MPoint = setToDiv(a, s.toDouble())
    operator fun MPoint.plusAssign(that: IPoint) { setTo(this.x + that.x, this.y + that.y) }


    fun floor() = setTo(floor(x), floor(y))
    fun round() = setTo(round(x), round(y))
    fun ceil() = setTo(ceil(x), ceil(y))

    fun setToRoundDecimalPlaces(places: Int) = setTo(x.roundDecimalPlaces(places), y.roundDecimalPlaces(places))
    fun setTo(x: Int, y: Int): MPoint = setTo(x.toDouble(), y.toDouble())

    fun setTo(x: Double, y: Double): MPoint {
        this.x = x
        this.y = y
        return this
    }

    fun setTo(x: Float, y: Float): MPoint {
        this.x = x.toDouble()
        this.y = y.toDouble()
        return this
    }

    /** Updates a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
    fun setToPolar(angle: Angle, length: Double = 1.0): MPoint = setToPolar(0.0, 0.0, angle, length)
    fun setToPolar(base: Point, angle: Angle, length: Float = 1f): MPoint = setToPolar(base.x, base.y, angle, length)
    fun setToPolar(base: IPoint, angle: Angle, length: Double = 1.0): MPoint = setToPolar(base.x, base.y, angle, length)
    fun setToPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0): MPoint = setTo(x + angle.cosine * length, y + angle.sine * length)
    fun setToPolar(x: Float, y: Float, angle: Angle, length: Float = 1f): MPoint = setTo(x + angle.cosine * length, y + angle.sine * length)

    /** Rotates the vector/point -90 degrees (not normalizing it) */
    fun setToNormal(): MPoint = setTo(-this.y, this.x)
    fun neg() = setTo(-this.x, -this.y)
    fun mul(s: Double) = setTo(this.x * s, this.y * s)
    fun mul(s: Float) = mul(s.toDouble())
    fun mul(s: Int) = mul(s.toDouble())

    fun add(p: MPoint) = this.setToAdd(this, p)
    fun sub(p: MPoint) = this.setToSub(this, p)

    fun add(x: Double, y: Double) = this.setTo(this.x + x, this.y + y)
    fun sub(x: Double, y: Double) = this.setTo(this.x - x, this.y - y)

    fun copyFrom(that: IPoint) = setTo(that.x, that.y)

    fun setToTransform(mat: MMatrix, p: IPoint): MPoint = setToTransform(mat, p.x, p.y)
    fun setToTransform(mat: MMatrix, x: Double, y: Double): MPoint = setTo(mat.transformX(x, y), mat.transformY(x, y))

    fun setToAdd(a: IPoint, b: IPoint): MPoint = setTo(a.x + b.x, a.y + b.y)
    fun setToSub(a: IPoint, b: IPoint): MPoint = setTo(a.x - b.x, a.y - b.y)
    fun setToMul(a: IPoint, b: IPoint): MPoint = setTo(a.x * b.x, a.y * b.y)
    fun setToMul(a: IPoint, s: Double): MPoint = setTo(a.x * s, a.y * s)
    fun setToMul(a: IPoint, s: Float): MPoint = setToMul(a, s.toDouble())
    fun setToDiv(a: IPoint, b: IPoint): MPoint = setTo(a.x / b.x, a.y / b.y)
    fun setToDiv(a: IPoint, s: Double): MPoint = setTo(a.x / s, a.y / s)
    fun setToDiv(a: IPoint, s: Float): MPoint = setToDiv(a, s.toDouble())
    operator fun plusAssign(that: IPoint) { setTo(this.x + that.x, this.y + that.y) }

    override operator fun plus(that: IPoint): MPoint = MPoint(this.x + that.x, this.y + that.y)
    override operator fun minus(that: IPoint): MPoint = MPoint(this.x - that.x, this.y - that.y)
    override operator fun times(that: IPoint): MPoint = MPoint(this.x * that.x, this.y * that.y)
    override operator fun div(that: IPoint): MPoint = MPoint(this.x / that.x, this.y / that.y)
    override infix fun dot(that: IPoint): Double = this.x * that.x + this.y * that.y

    override operator fun times(scale: Double): MPoint = MPoint(this.x * scale, this.y * scale)
    override operator fun times(scale: Float): MPoint = this * scale.toDouble()
    override operator fun times(scale: Int): MPoint = this * scale.toDouble()

    override operator fun div(scale: Double): MPoint = MPoint(this.x / scale, this.y / scale)
    override operator fun div(scale: Float): MPoint = this / scale.toDouble()
    override operator fun div(scale: Int): MPoint = this / scale.toDouble()

    override fun distanceTo(x: Double, y: Double): Double = hypot(x - this.x, y - this.y)
    override fun distanceTo(x: Int, y: Int): Double = distanceTo(x.toDouble(), y.toDouble())
    override fun distanceTo(x: Float, y: Float): Float = distanceTo(x.toDouble(), y.toDouble()).toFloat()

    fun distanceTo(that: MPoint): Double = distanceTo(that.x, that.y)
    fun angleTo(other: MPoint): Angle = Angle.between(this.x, this.y, other.x, other.y)
    override fun transformed(mat: MMatrix, out: MPoint): MPoint = out.setToTransform(mat, this)
    operator fun get(index: Int) = when (index) {
        0 -> this.x; 1 -> this.y
        else -> throw IndexOutOfBoundsException("IPoint doesn't have $index component")
    }
    override val mutable: MPoint get() = MPoint(this.x, this.y)
    override val immutable: MPoint get() = MPoint(this.x, this.y)
    fun copy() = MPoint(this.x, this.y)

    override val unit: IPoint get() = this / length
    val squaredLength: Double get() = (x * x) + (y * y)
    override val length: Double get() = hypot(this.x, this.y)
    override val magnitude: Double get() = hypot(this.x, this.y)
    override val normalized: MPoint
        get() {
            val imag = 1.0 / magnitude
            return MPoint(this.x * imag, this.y * imag)
        }

    fun normalize() {
        val len = this.length
        when {
            len.isAlmostZero() -> this.setTo(0, 0)
            else -> this.setTo(this.x / len, this.y / len)
        }
    }

    override fun interpolateWith(ratio: Double, other: MPoint): MPoint =
        MPoint().setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Double, l: MPoint, r: MPoint): MPoint = setToInterpolated(ratio, l.x, l.y, r.x, r.y)
    fun setToInterpolated(ratio: Double, l: IPoint, r: IPoint): MPoint = setToInterpolated(ratio, l.x, l.y, r.x, r.y)

    fun setToInterpolated(ratio: Double, lx: Double, ly: Double, rx: Double, ry: Double): MPoint =
        this.setTo(ratio.interpolate(lx, rx), ratio.interpolate(ly, ry))

    override fun toString(): String = "(${this.x.niceStr}, ${this.y.niceStr})"

    fun rotate(rotation: Angle, out: MPoint = MPoint()): MPoint =
        out.setToPolar(Angle.between(0.0, 0.0, this.x, this.y) + rotation, this.length)


    companion object {
        val POOL: ConcurrentPool<MPoint> = ConcurrentPool<MPoint>({ it.setTo(0.0, 0.0) }) { MPoint() }

        val Zero: IPoint = IPoint(0.0, 0.0)
        val One: IPoint = IPoint(1.0, 1.0)
        val Up: IPoint = IPoint(0.0, -1.0)
        val Down: IPoint = IPoint(0.0, +1.0)
        val Left: IPoint = IPoint(-1.0, 0.0)
        val Right: IPoint = IPoint(+1.0, 0.0)

        //inline operator fun invoke(): Point = Point(0.0, 0.0) // @TODO: // e: java.lang.NullPointerException at org.jetbrains.kotlin.com.google.gwt.dev.js.JsAstMapper.mapFunction(JsAstMapper.java:562) (val pt = Array(1) { Point() })
        operator fun invoke(): MPoint = MPoint(0.0, 0.0)
        operator fun invoke(v: MPoint): MPoint = MPoint(v.x, v.y)
        operator fun invoke(v: IPoint): MPoint = MPoint(v.x, v.y)
        operator fun invoke(xy: Int): MPoint = MPoint(xy.toDouble(), xy.toDouble())
        operator fun invoke(xy: Float): MPoint = MPoint(xy.toDouble(), xy.toDouble())
        operator fun invoke(xy: Double): MPoint = MPoint(xy, xy)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        inline operator fun invoke(angle: Angle, length: Double = 1.0): MPoint = fromPolar(angle, length)

        fun angleArc(a: IPoint, b: IPoint): Angle = Angle.fromRadians(acos((a.dot(b)) / (a.length * b.length)))
        fun angleFull(a: IPoint, b: IPoint): Angle = Angle.between(a, b)

        fun middle(a: IPoint, b: IPoint): MPoint = MPoint((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)
        fun compare(l: IPoint, r: IPoint): Int = MPoint.compare(l.x, l.y, r.x, r.y)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        fun fromPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0, out: MPoint = MPoint()): MPoint = out.setTo(x + angle.cosine * length, y + angle.sine * length)
        fun fromPolar(angle: Angle, length: Double = 1.0, out: MPoint = MPoint()): MPoint = fromPolar(0.0, 0.0, angle, length, out)
        fun fromPolar(base: IPoint, angle: Angle, length: Double = 1.0, out: MPoint = MPoint()): MPoint = fromPolar(base.x, base.y, angle, length, out)

        fun direction(a: IPoint, b: IPoint, out: MPoint = MPoint()): MPoint = out.setTo(b.x - a.x, b.y - a.y)
        fun middle(a: IPoint, b: IPoint, out: MPoint = MPoint()): MPoint = out.setTo((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)

        fun angle(ax: Double, ay: Double, bx: Double, by: Double): Angle = Angle.between(ax, ay, bx, by)
            //acos(((ax * bx) + (ay * by)) / (hypot(ax, ay) * hypot(bx, by)))

        fun compare(lx: Double, ly: Double, rx: Double, ry: Double): Int {
            val ret = ly.compareTo(ry)
            return if (ret == 0) lx.compareTo(rx) else ret
        }

        fun compare(l: MPoint, r: MPoint): Int = compare(l.x, l.y, r.x, r.y)

        fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Angle = Angle.between(x1 - x2, y1 - y2, x1 - x3, y1 - y3)

        private fun square(x: Double) = x * x
        private fun square(x: Int) = x * x

        fun distanceSquared(x1: Double, y1: Double, x2: Double, y2: Double): Double = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Int, y1: Int, x2: Int, y2: Int): Int = square(x1 - x2) + square(y1 - y2)

        fun distance(a: IPoint, b: IPoint): Double = distance(a.x, a.y, b.x, b.y)
        fun distance(a: IPointInt, b: IPointInt): Double = distance(a.x, a.y, b.x, b.y)
        fun distance(a: Double, b: Double): Double = kotlin.math.abs(a - b)
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = kotlin.math.hypot(x1 - x2, y1 - y2)
        fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double = distance(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
        fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double = distance(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())

        fun distanceSquared(a: IPoint, b: IPoint): Double = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(a: IPointInt, b: IPointInt): Int = distanceSquared(a.x, a.y, b.x, b.y)

        fun dot(aX: Double, aY: Double, bX: Double, bY: Double): Double = (aX * bX) + (aY * bY)
        fun dot(a: IPoint, b: IPoint): Double = dot(a.x, a.y, b.x, b.y)
        fun isCollinear(xa: Double, ya: Double, x: Double, y: Double, xb: Double, yb: Double): Boolean {
            return (((x - xa) / (y - ya)) - ((xa - xb) / (ya - yb))).absoluteValue.isAlmostZero()
        }

        fun isCollinear(xa: Int, ya: Int, x: Int, y: Int, xb: Int, yb: Int): Boolean = isCollinear(
            xa.toDouble(), ya.toDouble(),
            x.toDouble(), y.toDouble(),
            xb.toDouble(), yb.toDouble(),
        )

        // https://algorithmtutor.com/Computational-Geometry/Determining-if-two-consecutive-segments-turn-left-or-right/
        /** < 0 left, > 0 right, 0 collinear */
        fun orientation(p1: IPoint, p2: IPoint, p3: IPoint): Double =
            orientation(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
        fun orientation(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double =
            crossProduct(cx - ax, cy - ay, bx - ax, by - ay)

        fun crossProduct(ax: Double, ay: Double, bx: Double, by: Double): Double = (ax * by) - (bx * ay)
        fun crossProduct(p1: IPoint, p2: IPoint): Double = crossProduct(p1.x, p1.y, p2.x, p2.y)

        //val ax = x1 - x2
        //val ay = y1 - y2
        //val al = hypot(ax, ay)
        //val bx = x1 - x3
        //val by = y1 - y3
        //val bl = hypot(bx, by)
        //return acos((ax * bx + ay * by) / (al * bl))
    }
}

@KormaMutableApi
interface IPointInt {
    val x: Int
    val y: Int

    val point: PointInt get() = PointInt(x, y)

    companion object {
        operator fun invoke(x: Int, y: Int): IPointInt = MPointInt(x, y)
    }
}

@KormaMutableApi
inline class MPointInt(val p: MPoint) : IPointInt, Comparable<IPointInt>, MutableInterpolable<MPointInt> {
    override fun compareTo(other: IPointInt): Int = compare(this.x, this.y, other.x, other.y)

    companion object {
        operator fun invoke(): MPointInt = MPointInt(0, 0)
        operator fun invoke(x: Int, y: Int): MPointInt = MPointInt(MPoint(x, y))
        operator fun invoke(that: IPointInt): MPointInt = MPointInt(MPoint(that.x, that.y))

        fun compare(lx: Int, ly: Int, rx: Int, ry: Int): Int {
            val ret = ly.compareTo(ry)
            return if (ret == 0) lx.compareTo(rx) else ret
        }
    }
    override var x: Int ; set(value) { p.x = value.toDouble() } get() = p.x.toInt()
    override var y: Int ; set(value) { p.y = value.toDouble() } get() = p.y.toInt()
    fun setTo(x: Int, y: Int) : MPointInt {
        this.x = x
        this.y = y
        return this
    }
    fun setTo(that: IPointInt) = this.setTo(that.x, that.y)

    override fun setToInterpolated(ratio: Double, l: MPointInt, r: MPointInt): MPointInt =
        setTo(ratio.interpolate(l.x, r.x), ratio.interpolate(l.y, r.y))

    override fun toString(): String = "($x, $y)"
}

fun MPoint.asInt(): MPointInt = MPointInt(this)
fun MPointInt.asDouble(): MPoint = this.p

val MPoint.int get() = MPointInt(this.x.toInt(), this.y.toInt())
val IPoint.int get() = MPointInt(this.x.toInt(), this.y.toInt())
val IPointInt.double get() = IPoint(x.toDouble(), y.toDouble())

fun Point.toMPoint(out: MPoint = MPoint()): MPoint = out.setTo(x, y)
fun Point.mutable(out: MPoint = MPoint()): MPoint = out.setTo(x, y)
val Point.mutable: MPoint get() = mutable()

private inline fun getPolylineLength(size: Int, crossinline get: (n: Int, (x: Double, y: Double) -> Unit) -> Unit): Double {
    var out = 0.0
    var prevX = 0.0
    var prevY = 0.0
    for (n in 0 until size) {
        get(n) { x, y ->
            if (n > 0) {
                out += MPoint.distance(prevX, prevY, x, y)
            }
            prevX = x
            prevY = y
        }
    }
    return out
}

fun IPointArrayList.getPolylineLength(): Double = getPolylineLength(size) { n, func -> func(getX(n), getY(n)) }
fun List<IPoint>.getPolylineLength(): Double = getPolylineLength(size) { n, func -> func(this[n].x, this[n].y) }

fun List<MPoint>.bounds(out: MRectangle = MRectangle(), bb: BoundsBuilder = BoundsBuilder()): MRectangle = bb.add(this).getBounds(out)
fun Iterable<IPoint>.bounds(out: MRectangle = MRectangle(), bb: BoundsBuilder = BoundsBuilder()): MRectangle = bb.add(this).getBounds(out)

fun min(a: IPoint, b: IPoint, out: MPoint = MPoint()): MPoint = out.setTo(kotlin.math.min(a.x, b.x), kotlin.math.min(a.y, b.y))
fun max(a: IPoint, b: IPoint, out: MPoint = MPoint()): MPoint = out.setTo(kotlin.math.max(a.x, b.x), kotlin.math.max(a.y, b.y))
fun IPoint.clamp(min: Double, max: Double, out: MPoint = MPoint()): MPoint = out.setTo(x.clamp(min, max), y.clamp(min, max))
