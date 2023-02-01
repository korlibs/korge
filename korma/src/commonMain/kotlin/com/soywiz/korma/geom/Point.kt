@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korma.geom

import com.soywiz.kds.*
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.kmem.clamp
import com.soywiz.korma.math.*
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.hypot

@Deprecated("", replaceWith = ReplaceWith("com.soywiz.korma.geom.Point"))
typealias IPoint = Point
@Deprecated("")
typealias IPointInt = PointInt

//inline
data
class Point(
    val x: Double,
    val y: Double
    //override var xf: Float,
    //override var yf: Float
) : Interpolable<Point>, Comparable<Point> {
    //constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    //override var x: Double get() = xf.toDouble() ; set(value) { xf = value.toFloat() }
    //override var y: Double get() = yf.toDouble() ; set(value) { yf = value.toFloat() }

    override fun compareTo(other: Point): Int = compare(this.x, this.y, other.x, other.y)

    companion object {
        val ZERO: Point = Point(0, 0)

        val Zero: Point = Point(0.0, 0.0)
        val One: Point = Point(1.0, 1.0)
        val Up: Point = Point(0.0, -1.0)
        val Down: Point = Point(0.0, +1.0)
        val Left: Point = Point(-1.0, 0.0)
        val Right: Point = Point(+1.0, 0.0)

        //inline operator fun invoke(): Point = Point(0.0, 0.0) // @TODO: // e: java.lang.NullPointerException at org.jetbrains.kotlin.com.google.gwt.dev.js.JsAstMapper.mapFunction(JsAstMapper.java:562) (val pt = Array(1) { Point() })
        operator fun invoke(): Point = Point(0.0, 0.0)
        operator fun invoke(v: Point): Point = Point(v.x, v.y)
        operator fun invoke(x: Double, y: Double): Point = Point(x, y)
        operator fun invoke(x: Float, y: Float): Point = Point(x, y)
        operator fun invoke(x: Int, y: Int): Point = Point(x, y)
        operator fun invoke(xy: Int): Point = Point(xy.toDouble(), xy.toDouble())
        operator fun invoke(xy: Float): Point = Point(xy.toDouble(), xy.toDouble())
        operator fun invoke(xy: Double): Point = Point(xy, xy)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        inline operator fun invoke(angle: Angle, length: Double = 1.0): Point = fromPolar(angle, length)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        fun fromPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0): Point = Point(x + angle.cosine * length, y + angle.sine * length)
        fun fromPolar(angle: Angle, length: Double = 1.0): Point = fromPolar(0.0, 0.0, angle, length)
        fun fromPolar(base: IPoint, angle: Angle, length: Double = 1.0): Point = fromPolar(base.x, base.y, angle, length)

        fun direction(a: IPoint, b: IPoint): Point = Point(b.x - a.x, b.y - b.y)
        fun middle(a: IPoint, b: IPoint): Point = Point((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)
        fun angleArc(a: IPoint, b: IPoint): Angle = Angle.fromRadians(acos((a.dot(b)) / (a.length * b.length)))
        @Deprecated("")
        fun angle(a: IPoint, b: IPoint): Angle = angleArc(a, b)
        fun angleFull(a: IPoint, b: IPoint): Angle = Angle.between(a, b)

        fun interpolated(ratio: Double, a: Point, b: Point): Point = Point(ratio.interpolate(a.x, b.x), ratio.interpolate(a.y, b.y))

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

    /** Rotates the vector/point -90 degrees (not normalizing it) */

    operator fun unaryMinus(): Point = Point(-this.x, -this.y)
    operator fun unaryPlus(): Point = this

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
    fun transformed(mat: Matrix): Point = Point(mat.transformX(x, y), mat.transformY(x, y))
    fun transformX(m: Matrix?): Double = m?.transformX(this) ?: x
    fun transformY(m: Matrix?): Double = m?.transformY(this) ?: y

    operator fun get(index: Int) = when (index) {
        0 -> this.x; 1 -> this.y
        else -> throw IndexOutOfBoundsException("IPoint doesn't have $index component")
    }

    @Deprecated("")
    val mutable: Point get() = Point(this.x, this.y)
    @Deprecated("")
    val immutable: Point get() = Point(this.x, this.y)
    @Deprecated("")
    fun copy(): Point = Point(this.x, this.y)

    fun roundDecimalPlaces(places: Int): Point = Point(x.roundDecimalPlaces(places), y.roundDecimalPlaces(places))

    fun floored(): Point = Point(kotlin.math.floor(x), kotlin.math.floor(y))
    fun rounded(): Point = Point(kotlin.math.round(x), kotlin.math.round(y))
    fun ceiled(): Point = Point(kotlin.math.ceil(x), kotlin.math.ceil(y))


    val unit: Point get() = this / this.length
    val squaredLength: Double get() = (x * x) + (y * y)
    val length: Double get() = hypot(this.x, this.y)
    val magnitude: Double get() = hypot(this.x, this.y)
    val normalized: Point
        get() {
            val imag = 1.0 / magnitude
            return Point(this.x * imag, this.y * imag)
        }

    val niceStr: String get() = "(${x.niceStr}, ${y.niceStr})"
    fun niceStr(decimalPlaces: Int): String = "(${x.niceStr(decimalPlaces)}, ${y.niceStr(decimalPlaces)})"
    val angle: Angle get() = Angle.between(0.0, 0.0, this.x, this.y)

    fun isAlmostEquals(other: IPoint, epsilon: Double = 0.000001): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon)

    override fun interpolateWith(ratio: Double, other: Point): Point =
        Point(ratio.interpolate(this.x, other.x), ratio.interpolate(this.y, other.y))

    override fun toString(): String = "(${this.x.niceStr}, ${this.y.niceStr})"
}

inline class PointInt(val p: Point) : Comparable<IPointInt> {
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
    val x: Int get() = p.x.toInt()
    val y: Int get() = p.y.toInt()

    operator fun plus(that: IPointInt): PointInt = PointInt(this.x + that.x, this.y + that.y)
    operator fun minus(that: IPointInt): PointInt = PointInt(this.x - that.x, this.y - that.y)
    operator fun times(that: IPointInt): PointInt = PointInt(this.x * that.x, this.y * that.y)
    operator fun div(that: IPointInt): PointInt = PointInt(this.x / that.x, this.y / that.y)
    operator fun rem(that: IPointInt): PointInt = PointInt(this.x % that.x, this.y % that.y)

    override fun toString(): String = "($x, $y)"
}

fun Point.asInt(): PointInt = PointInt(this)
fun PointInt.asDouble(): Point = this.p

val Point.int get() = PointInt(this.x.toInt(), this.y.toInt())
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

fun min(a: Point, b: Point): Point = Point(kotlin.math.min(a.x, b.x), kotlin.math.min(a.y, b.y))
fun max(a: Point, b: Point): Point = Point(kotlin.math.max(a.x, b.x), kotlin.math.max(a.y, b.y))
fun Point.clamp(min: Double, max: Double): Point = Point(x.clamp(min, max), y.clamp(min, max))

typealias Vector2D = Point
