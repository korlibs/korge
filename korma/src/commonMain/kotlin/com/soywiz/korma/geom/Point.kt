@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korma.geom

import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.kmem.clamp
import com.soywiz.korma.math.isAlmostEquals
import com.soywiz.korma.math.isAlmostZero
import com.soywiz.korma.math.roundDecimalPlaces
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.round

interface IPoint {
    val x: Double
    val y: Double

    companion object {
        operator fun invoke(): IPoint = Point(0.0, 0.0)
        operator fun invoke(v: IPoint): IPoint = Point(v.x, v.y)
        operator fun invoke(x: Double, y: Double): IPoint = Point(x, y)
        operator fun invoke(x: Float, y: Float): IPoint = Point(x, y)
        operator fun invoke(x: Int, y: Int): IPoint = Point(x, y)
    }
}

interface XY : IPoint {
    override var x: Double
    override var y: Double
}

interface XYf {
    var xf: Float
    var yf: Float
}

fun Point.Companion.middle(a: IPoint, b: IPoint): Point = Point((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)
fun Point.Companion.angle(a: IPoint, b: IPoint): Angle = Angle.fromRadians(acos((a.dot(b)) / (a.length * b.length)))
fun Point.Companion.compare(l: IPoint, r: IPoint): Int = Point.compare(l.x, l.y, r.x, r.y)
fun Point.Companion.distance(a: IPoint, b: IPoint): Double = Point.distance(a.x, a.y, b.x, b.y)
fun Point.copyFrom(that: IPoint) = setTo(that.x, that.y)
fun Point.add(p: IPoint) = this.setToAdd(this, p)
fun Point.sub(p: IPoint) = this.setToSub(this, p)

operator fun IPoint.plus(that: IPoint): IPoint = IPoint(x + that.x, y + that.y)
operator fun IPoint.minus(that: IPoint): IPoint = IPoint(x - that.x, y - that.y)
operator fun IPoint.times(that: IPoint): IPoint = IPoint(x * that.x, y * that.y)
operator fun IPoint.div(that: IPoint): IPoint = IPoint(x / that.x, y / that.y)

operator fun IPoint.times(scale: Double): IPoint = IPoint(x * scale, y * scale)
operator fun IPoint.div(scale: Double): IPoint = IPoint(x / scale, y / scale)
fun IPoint.distanceTo(x: Double, y: Double): Double = hypot(x - this.x, y - this.y)

operator fun IPoint.times(scale: Int): IPoint = this * scale.toDouble()
operator fun IPoint.div(scale: Int): IPoint = this / scale.toDouble()
fun IPoint.distanceTo(x: Int, y: Int): Double = this.distanceTo(x.toDouble(), y.toDouble())

operator fun IPoint.times(scale: Float): IPoint = this * scale.toDouble()
operator fun IPoint.div(scale: Float): IPoint = this / scale.toDouble()
fun IPoint.distanceTo(x: Float, y: Float): Double = this.distanceTo(x.toDouble(), y.toDouble())

infix fun IPoint.dot(that: IPoint): Double = this.x * that.x + this.y * that.y
fun IPoint.distanceTo(that: IPoint): Double = distanceTo(that.x, that.y)
fun IPoint.angleTo(other: IPoint): Angle = Angle.between(this.x, this.y, other.x, other.y)
val IPoint.angle: Angle get() = Angle.between(0.0, 0.0, this.x, this.y)
fun IPoint.transformed(mat: Matrix, out: Point = Point()): Point = out.setToTransform(mat, this)
fun IPoint.transformX(m: Matrix?): Double = m?.transformX(this) ?: x
fun IPoint.transformY(m: Matrix?): Double = m?.transformY(this) ?: y
operator fun IPoint.get(component: Int) = when (component) {
    0 -> x; 1 -> y
    else -> throw IndexOutOfBoundsException("IPoint doesn't have $component component")
}
val IPoint.unit: IPoint get() = this / this.length
val IPoint.length: Double get() = hypot(x, y)
val IPoint.magnitude: Double get() = hypot(x, y)
val IPoint.normalized: IPoint
    get() {
        val imag = 1.0 / magnitude
        return IPoint(x * imag, y * imag)
    }
val IPoint.mutable: Point get() = Point(x, y)
val IPoint.immutable: IPoint get() = IPoint(x, y)
fun IPoint.copy() = IPoint(x, y)
fun IPoint.isAlmostEquals(other: IPoint, epsilon: Double = 0.000001): Boolean =
    this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon)

fun Point.setToTransform(mat: Matrix, p: IPoint): Point = setToTransform(mat, p.x, p.y)
fun Point.setToTransform(mat: Matrix, x: Double, y: Double): Point = setTo(mat.transformX(x, y), mat.transformY(x, y))
fun Point.setToAdd(a: IPoint, b: IPoint): Point = setTo(a.x + b.x, a.y + b.y)
fun Point.setToSub(a: IPoint, b: IPoint): Point = setTo(a.x - b.x, a.y - b.y)
fun Point.setToMul(a: IPoint, b: IPoint): Point = setTo(a.x * b.x, a.y * b.y)
fun Point.setToMul(a: IPoint, s: Double): Point = setTo(a.x * s, a.y * s)
inline fun Point.setToMul(a: IPoint, s: Number): Point = setToMul(a, s.toDouble())
fun Point.setToDiv(a: IPoint, b: IPoint): Point = setTo(a.x / b.x, a.y / b.y)
fun Point.setToDiv(a: IPoint, s: Double): Point = setTo(a.x / s, a.y / s)
inline fun Point.setToDiv(a: IPoint, s: Number): Point = setToDiv(a, s.toDouble())
operator fun Point.plusAssign(that: IPoint) { setTo(this.x + that.x, this.y + that.y) }

data class Point(
    override var x: Double,
    override var y: Double
    //override var xf: Float,
    //override var yf: Float
) : MutableInterpolable<Point>, Interpolable<Point>, Comparable<IPoint>, IPoint, XY, XYf {
    //constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    //override var x: Double get() = xf.toDouble() ; set(value) { xf = value.toFloat() }
    //override var y: Double get() = yf.toDouble() ; set(value) { yf = value.toFloat() }

    override var xf: Float get() = x.toFloat() ; set(value) { x = value.toDouble() }
    override var yf: Float get() = y.toFloat() ; set(value) { y = value.toDouble() }

    override fun compareTo(other: IPoint): Int = compare(this.x, this.y, other.x, other.y)
    fun compareTo(other: Point): Int = compare(this.x, this.y, other.x, other.y)

    fun clear() = setToZero()
    fun setToZero() = setTo(0.0, 0.0)
    fun setToOne() = setTo(1.0, 1.0)
    fun setToUp() = setTo(0.0, -1.0)
    fun setToDown() = setTo(0.0, +1.0)
    fun setToLeft() = setTo(-1.0, 0.0)
    fun setToRight() = setTo(+1.0, 0.0)

    companion object {
        val Zero: IPoint = IPoint(0.0, 0.0)
        val One: IPoint = IPoint(1.0, 1.0)
        val Up: IPoint = IPoint(0.0, -1.0)
        val Down: IPoint = IPoint(0.0, +1.0)
        val Left: IPoint = IPoint(-1.0, 0.0)
        val Right: IPoint = IPoint(+1.0, 0.0)

        //inline operator fun invoke(): Point = Point(0.0, 0.0) // @TODO: // e: java.lang.NullPointerException at org.jetbrains.kotlin.com.google.gwt.dev.js.JsAstMapper.mapFunction(JsAstMapper.java:562) (val pt = Array(1) { Point() })
        operator fun invoke(): Point = Point(0.0, 0.0)
        operator fun invoke(v: Point): Point = Point(v.x, v.y)
        operator fun invoke(v: IPoint): Point = Point(v.x, v.y)
        operator fun invoke(xy: Int): Point = Point(xy.toDouble(), xy.toDouble())
        operator fun invoke(xy: Float): Point = Point(xy.toDouble(), xy.toDouble())
        operator fun invoke(xy: Double): Point = Point(xy, xy)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        inline operator fun invoke(angle: Angle, length: Double = 1.0): Point = fromPolar(angle, length)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        fun fromPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0, out: Point = Point()): Point = out.setTo(x + angle.cosine * length, y + angle.sine * length)
        fun fromPolar(angle: Angle, length: Double = 1.0, out: Point = Point()): Point = fromPolar(0.0, 0.0, angle, length, out)
        fun fromPolar(base: IPoint, angle: Angle, length: Double = 1.0, out: Point = Point()): Point = fromPolar(base.x, base.y, angle, length, out)

        fun direction(a: IPoint, b: IPoint, out: Point = Point()): Point = out.setTo(b.x - a.x, b.y - b.y)
        fun middle(a: IPoint, b: IPoint, out: Point = Point()): Point = out.setTo((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)
        fun angleArc(a: IPoint, b: IPoint): Angle = Angle.fromRadians(acos((a.dot(b)) / (a.length * b.length)))
        @Deprecated("")
        fun angle(a: IPoint, b: IPoint): Angle = angleArc(a, b)
        fun angleFull(a: IPoint, b: IPoint): Angle = Angle.between(a, b)

        fun angle(ax: Double, ay: Double, bx: Double, by: Double): Angle = Angle.between(ax, ay, bx, by)
            //acos(((ax * bx) + (ay * by)) / (hypot(ax, ay) * hypot(bx, by)))

        fun compare(lx: Double, ly: Double, rx: Double, ry: Double): Int {
            val ret = ly.compareTo(ry)
            return if (ret == 0) lx.compareTo(rx) else ret
        }

        fun compare(l: Point, r: Point): Int = compare(l.x, l.y, r.x, r.y)

        fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Angle = Angle.between(x1 - x2, y1 - y2, x1 - x3, y1 - y3)

        private fun square(x: Double) = x * x
        private fun square(x: Int) = x * x

        fun distanceSquared(x1: Double, y1: Double, x2: Double, y2: Double): Double = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Int, y1: Int, x2: Int, y2: Int): Int = square(x1 - x2) + square(y1 - y2)

        fun distance(a: Double, b: Double): Double = kotlin.math.abs(a - b)
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = kotlin.math.hypot(x1 - x2, y1 - y2)
        fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double = distance(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
        fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double = distance(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())

        fun distance(a: IPoint, b: IPoint): Double = distance(a.x, a.y, b.x, b.y)
        fun distance(a: IPointInt, b: IPointInt): Double = distance(a.x, a.y, b.x, b.y)

        fun distanceSquared(a: Point, b: Point): Double = distanceSquared(a.x, a.y, b.x, b.y)
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

    fun floor() = setTo(floor(x), floor(y))
    fun round() = setTo(round(x), round(y))
    fun ceil() = setTo(ceil(x), ceil(y))

    fun setToRoundDecimalPlaces(places: Int) = setTo(x.roundDecimalPlaces(places), y.roundDecimalPlaces(places))
    fun setTo(x: Int, y: Int): Point = setTo(x.toDouble(), y.toDouble())

    fun setTo(x: Double, y: Double): Point {
        this.x = x
        this.y = y
        return this
    }

    fun setTo(x: Float, y: Float): Point {
        this.xf = x
        this.yf = y
        return this
    }

    /** Updates a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
    fun setToPolar(angle: Angle, length: Double = 1.0): Point = setToPolar(0.0, 0.0, angle, length)
    fun setToPolar(base: IPoint, angle: Angle, length: Double = 1.0): Point = setToPolar(base.x, base.y, angle, length)
    fun setToPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0): Point = setTo(x + angle.cosine * length, y + angle.sine * length)

    /** Rotates the vector/point -90 degrees (not normalizing it) */
    fun setToNormal(): Point = setTo(-this.y, this.x)
    fun neg() = setTo(-this.x, -this.y)
    fun mul(s: Double) = setTo(this.x * s, this.y * s)
    fun mul(s: Float) = mul(s.toDouble())
    fun mul(s: Int) = mul(s.toDouble())

    fun add(p: Point) = this.setToAdd(this, p)
    fun sub(p: Point) = this.setToSub(this, p)

    fun add(x: Double, y: Double) = this.setTo(this.x + x, this.y + y)
    fun sub(x: Double, y: Double) = this.setTo(this.x - x, this.y - y)

    fun copyFrom(that: IPoint) = setTo(that.x, that.y)

    fun setToTransform(mat: Matrix, p: Point): Point = setToTransform(mat, p.x, p.y)
    fun setToTransform(mat: Matrix, x: Double, y: Double): Point = setTo(mat.transformX(x, y), mat.transformY(x, y))

    fun setToAdd(a: Point, b: Point): Point = setTo(a.x + b.x, a.y + b.y)
    fun setToSub(a: Point, b: Point): Point = setTo(a.x - b.x, a.y - b.y)
    fun setToMul(a: Point, b: Point): Point = setTo(a.x * b.x, a.y * b.y)
    fun setToMul(a: Point, s: Double): Point = setTo(a.x * s, a.y * s)
    fun setToMul(a: Point, s: Float): Point = setToMul(a, s.toDouble())
    fun setToDiv(a: Point, b: Point): Point = setTo(a.x / b.x, a.y / b.y)
    fun setToDiv(a: Point, s: Double): Point = setTo(a.x / s, a.y / s)
    fun setToDiv(a: Point, s: Float): Point = setToDiv(a, s.toDouble())
    operator fun plusAssign(that: Point) { setTo(this.x + that.x, this.y + that.y) }

    operator fun plus(that: IPoint): Point = Point(this.x + that.x, this.y + that.y)
    operator fun minus(that: IPoint): Point = Point(this.x - that.x, this.y - that.y)
    operator fun times(that: IPoint): Point = Point(this.x * that.x, this.y * that.y)
    operator fun div(that: IPoint): Point = Point(this.x / that.x, this.y / that.y)
    infix fun dot(that: IPoint): Double = this.x * that.x + this.y * that.y

    operator fun times(scale: Double): Point = Point(this.x * scale, this.y * scale)
    operator fun times(scale: Float): Point = this * scale.toDouble()
    operator fun times(scale: Int): Point = this * scale.toDouble()

    operator fun div(scale: Double): Point = Point(this.x / scale, this.y / scale)
    operator fun div(scale: Float): Point = this / scale.toDouble()
    operator fun div(scale: Int): Point = this / scale.toDouble()

    fun distanceTo(x: Double, y: Double): Double = hypot(x - this.x, y - this.y)
    fun distanceTo(x: Int, y: Int): Double = distanceTo(x.toDouble(), y.toDouble())
    fun distanceTo(x: Float, y: Float): Float = distanceTo(x.toDouble(), y.toDouble()).toFloat()

    fun distanceTo(that: Point): Double = distanceTo(that.x, that.y)
    fun angleTo(other: Point): Angle = Angle.between(this.x, this.y, other.x, other.y)
    fun transformed(mat: Matrix, out: Point = Point()): Point = out.setToTransform(mat, this)
    operator fun get(index: Int) = when (index) {
        0 -> this.x; 1 -> this.y
        else -> throw IndexOutOfBoundsException("IPoint doesn't have $index component")
    }
    val mutable: Point get() = Point(this.x, this.y)
    val immutable: Point get() = Point(this.x, this.y)
    fun copy() = Point(this.x, this.y)


    val unit: Point get() = this / this.length
    val squaredLength: Double get() = (x * x) + (y * y)
    val length: Double get() = hypot(this.x, this.y)
    val magnitude: Double get() = hypot(this.x, this.y)
    val normalized: Point
        get() {
            val imag = 1.0 / magnitude
            return Point(this.x * imag, this.y * imag)
        }

    fun normalize() {
        val len = this.length
        when {
            len.isAlmostZero() -> this.setTo(0, 0)
            else -> this.setTo(this.x / len, this.y / len)
        }
    }

    override fun interpolateWith(ratio: Double, other: Point): Point =
        Point().setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Double, l: Point, r: Point): Point = setToInterpolated(ratio, l.x, l.y, r.x, r.y)
    fun setToInterpolated(ratio: Double, l: IPoint, r: IPoint): Point = setToInterpolated(ratio, l.x, l.y, r.x, r.y)

    fun setToInterpolated(ratio: Double, lx: Double, ly: Double, rx: Double, ry: Double): Point =
        this.setTo(ratio.interpolate(lx, rx), ratio.interpolate(ly, ry))

    override fun toString(): String = "(${this.x.niceStr}, ${this.y.niceStr})"

    fun rotate(rotation: Angle, out: Point = Point()): Point =
        out.setToPolar(Angle.between(0.0, 0.0, this.x, this.y) + rotation, this.length)
}


val Point.unit: IPoint get() = this / length

@Deprecated("Use non Number version")
inline fun Point.setTo(x: Number, y: Number): Point = setTo(x.toDouble(), y.toDouble())

interface IPointInt {
    val x: Int
    val y: Int

    companion object {
        operator fun invoke(x: Int, y: Int): IPointInt = PointInt(x, y)
    }
}

inline class PointInt(val p: Point) : IPointInt, Comparable<IPointInt> {
    override fun compareTo(other: IPointInt): Int = compare(this.x, this.y, other.x, other.y)

    companion object {
        operator fun invoke(): PointInt = PointInt(0, 0)
        operator fun invoke(x: Int, y: Int): PointInt = PointInt(Point(x, y))
        operator fun invoke(that: IPointInt): PointInt = PointInt(Point(that.x, that.y))

        fun compare(lx: Int, ly: Int, rx: Int, ry: Int): Int {
            val ret = ly.compareTo(ry)
            return if (ret == 0) lx.compareTo(rx) else ret
        }
    }
    override var x: Int
        set(value) { p.x = value.toDouble() }
        get() = p.x.toInt()
    override var y: Int
        set(value) { p.y = value.toDouble() }
        get() = p.y.toInt()
    fun setTo(x: Int, y: Int) : PointInt {
        this.x = x
        this.y = y

        return this
    }
    fun setTo(that: IPointInt) = this.setTo(that.x, that.y)
    override fun toString(): String = "($x, $y)"
}

operator fun IPointInt.plus(that: IPointInt) = PointInt(this.x + that.x, this.y + that.y)
operator fun IPointInt.minus(that: IPointInt) = PointInt(this.x - that.x, this.y - that.y)
operator fun IPointInt.times(that: IPointInt) = PointInt(this.x * that.x, this.y * that.y)
operator fun IPointInt.div(that: IPointInt) = PointInt(this.x / that.x, this.y / that.y)
operator fun IPointInt.rem(that: IPointInt) = PointInt(this.x % that.x, this.y % that.y)

fun Point.asInt(): PointInt = PointInt(this)
fun PointInt.asDouble(): Point = this.p

val Point.int get() = PointInt(this.x.toInt(), this.y.toInt())
val IPoint.int get() = PointInt(this.x.toInt(), this.y.toInt())
val IPointInt.float get() = IPoint(x.toDouble(), y.toDouble())

fun List<Point>.getPolylineLength(): Double {
    var out = 0.0
    var prev: Point? = null
    for (cur in this) {
        if (prev != null) out += prev.distanceTo(cur)
        prev = cur
    }
    return out
}
fun List<Point>.bounds(out: Rectangle = Rectangle(), bb: BoundsBuilder = BoundsBuilder()): Rectangle = bb.add(this).getBounds(out)

fun Iterable<IPoint>.getPolylineLength(): Double {
    var out = 0.0
    var prev: IPoint? = null
    for (cur in this) {
        if (prev != null) out += prev.distanceTo(cur)
        prev = cur
    }
    return out
}
fun Iterable<IPoint>.bounds(out: Rectangle = Rectangle(), bb: BoundsBuilder = BoundsBuilder()): Rectangle = bb.add(this).getBounds(out)

fun min(a: Point, b: Point, out: Point = Point()): Point = out.setTo(kotlin.math.min(a.x, b.x), kotlin.math.min(a.y, b.y))
fun max(a: Point, b: Point, out: Point = Point()): Point = out.setTo(kotlin.math.max(a.x, b.x), kotlin.math.max(a.y, b.y))
fun Point.clamp(min: Double, max: Double, out: Point = Point()): Point = out.setTo(x.clamp(min, max), y.clamp(min, max))

typealias Vector2D = Point
