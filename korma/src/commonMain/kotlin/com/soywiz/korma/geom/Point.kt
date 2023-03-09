@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korma.geom

import com.soywiz.kds.pack.*
import com.soywiz.kmem.*
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.interpolation.*
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

@Deprecated("", ReplaceWith("p", "com.soywiz.korma.geom.Point")) fun Point(p: Point): Point = p

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

    constructor(x: Double, y: Int) : this(Float2Pack(x.toFloat(), y.toFloat()))
    constructor(x: Int, y: Double) : this(Float2Pack(x.toFloat(), y.toFloat()))

    constructor(x: Float, y: Int) : this(Float2Pack(x.toFloat(), y.toFloat()))
    constructor(x: Int, y: Float) : this(Float2Pack(x.toFloat(), y.toFloat()))

    constructor(p: IPoint) : this(p.x.toFloat(), p.y.toFloat())
    //constructor(p: Point) : this(p.raw)
    constructor() : this(0f, 0f)
    //constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
    //constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())

    operator fun component1(): Float = x
    operator fun component2(): Float = y

    fun copy(x: Float = this.x, y: Float = this.y): Point = Point(x, y)
    fun copy(x: Double = this.xD, y: Double = this.yD): Point = Point(x, y)

    inline operator fun unaryMinus(): Point = Point(-x, -y)
    inline operator fun unaryPlus(): Point = this

    inline operator fun plus(that: Point): Point = Point(x + that.x, y + that.y)
    inline operator fun minus(that: Point): Point = Point(x - that.x, y - that.y)
    inline operator fun times(that: Point): Point = Point(x * that.x, y * that.y)
    inline operator fun times(that: Size): Point = Point(x * that.width, y * that.height)
    inline operator fun times(that: Scale): Point = Point(x * that.scaleX, y * that.scaleY)
    inline operator fun div(that: Point): Point = Point(x / that.x, y / that.y)
    inline operator fun div(that: Size): Point = Point(x / that.width, y / that.height)

    inline operator fun times(scale: Float): Point = Point(x * scale, y * scale)
    inline operator fun times(scale: Double): Point = this * scale.toFloat()
    inline operator fun times(scale: Int): Point = this * scale.toDouble()

    inline operator fun div(scale: Float): Point = Point(x / scale, y / scale)
    inline operator fun div(scale: Double): Point = this / scale.toFloat()
    inline operator fun div(scale: Int): Point = this / scale.toDouble()

    fun avgComponent(): Float = x * 0.5f + y * 0.5f
    fun minComponent(): Float = min(x, y)
    fun maxComponent(): Float = max(x, y)

    fun distanceTo(x: Float, y: Float): Float = hypot(x - this.x, y - this.y)
    fun distanceTo(x: Double, y: Double): Float = this.distanceTo(x.toFloat(), y.toFloat())
    fun distanceTo(x: Int, y: Int): Float = this.distanceTo(x.toDouble(), y.toDouble())
    fun distanceTo(that: Point): Float = distanceTo(that.x, that.y)

    infix fun dot(that: Point): Float = ((this.x * that.x) + (this.y * that.y))

    fun angleTo(other: Point): Angle = Angle.between(this.x, this.y, other.x, other.y)
    val angle: Angle get() = Angle.between(0f, 0f, this.x, this.y)

    inline fun transformed(m: MMatrix?): Point = m?.transform(this) ?: this
    fun transformX(m: MMatrix?): Float = m?.transform(this)?.x ?: x
    fun transformY(m: MMatrix?): Float = m?.transform(this)?.y ?: y

    inline fun transformed(m: Matrix?): Point = m?.transform(this) ?: this
    fun transformX(m: Matrix?): Float = m?.transform(this)?.x ?: x
    fun transformY(m: Matrix?): Float = m?.transform(this)?.y ?: y

    operator fun get(component: Int) = when (component) {
        0 -> x; 1 -> y
        else -> throw IndexOutOfBoundsException("Point doesn't have $component component")
    }
    val length: Float get() = hypot(x, y)
    val squaredLength: Float get() {
        val x = x
        val y = y
        return x*x + y*y
    }
    val magnitude: Float get() = hypot(x, y)
    val normalized: Point get() = this * (1f / magnitude)
    val unit: Point get() = this / length

    /** Rotates the vector/point -90 degrees (not normalizing it) */
    fun toNormal(): Point = Point(-this.y, this.x)


    val int: PointInt get() = PointInt(x.toInt(), y.toInt())
    val intRound: PointInt get() = PointInt(x.roundToInt(), y.roundToInt())

    fun roundDecimalPlaces(places: Int): Point = Point(x.roundDecimalPlaces(places), y.roundDecimalPlaces(places))
    fun round(): Point = Point(kotlin.math.round(x), kotlin.math.round(y))
    fun ceil(): Point = Point(kotlin.math.ceil(x), kotlin.math.ceil(y))
    fun floor(): Point = Point(kotlin.math.floor(x), kotlin.math.floor(y))

    //fun copy(x: Double = this.x, y: Double = this.y): Point = Point(x, y)

    fun isAlmostEquals(other: Point, epsilon: Float = 0.00001f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon)

    val niceStr: String get() = "(${x.niceStr}, ${y.niceStr})"
    fun niceStr(decimalPlaces: Int): String = "(${x.niceStr(decimalPlaces)}, ${y.niceStr(decimalPlaces)})"
    override fun toString(): String = niceStr

    fun isNaN(): Boolean = this.x.isNaN() && this.y.isNaN()

    @Deprecated("", ReplaceWith("ratio.interpolate(this, other)", "com.soywiz.korma.interpolation.interpolate")) fun interpolateWith(ratio: Ratio, other: Point): Point = ratio.interpolate(this, other)

    companion object {
        val ZERO = Point(0f, 0f)
        val NaN = Point(Float.NaN, Float.NaN)

        //inline operator fun invoke(x: Int, y: Int): Point = Point(x.toDouble(), y.toDouble())
        //inline operator fun invoke(x: Float, y: Float): Point = Point(x.toDouble(), y.toDouble())

        //fun fromRaw(raw: Float2Pack) = Point(raw)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        inline fun fromPolar(x: Float, y: Float, angle: Angle, length: Double = 1.0): Point = Point(x + angle.cosineF * length, y + angle.sineF * length)
        inline fun fromPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0): Point = Point(x + angle.cosineD * length, y + angle.sineD * length)
        inline fun fromPolar(base: Point, angle: Angle, length: Double = 1.0): Point = fromPolar(base.x, base.y, angle, length)
        inline fun fromPolar(angle: Angle, length: Double = 1.0): Point = fromPolar(0.0, 0.0, angle, length)

        inline fun middle(a: Point, b: Point): Point = (a + b) * 0.5

        fun angle(ax: Double, ay: Double, bx: Double, by: Double): Angle = Angle.between(ax, ay, bx, by)
        fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Angle = Angle.between(x1 - x2, y1 - y2, x1 - x3, y1 - y3)

        fun angleArc(a: Point, b: Point): Angle = Angle.fromRadians(acos((a dot b) / (a.length * b.length)))
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

        fun isCollinear(xa: Float, ya: Float, x: Float, y: Float, xb: Float, yb: Float): Boolean =
            (((x - xa) / (y - ya)) - ((xa - xb) / (ya - yb))).absoluteValue.isAlmostZero()

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

val MPoint.int get() = MPointInt(this.x.toInt(), this.y.toInt())
val IPoint.int get() = MPointInt(this.x.toInt(), this.y.toInt())
val IPointInt.double get() = IPoint(x.toDouble(), y.toDouble())

@Deprecated("")
fun Point.toMPoint(out: MPoint = MPoint()): MPoint = out.setTo(x, y)
@Deprecated("")
fun Point.mutable(out: MPoint = MPoint()): MPoint = out.setTo(x, y)
@Deprecated("")
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

fun PointList.getPolylineLength(): Double = getPolylineLength(size) { n, func -> func(getX(n).toDouble(), getY(n).toDouble()) }
fun List<IPoint>.getPolylineLength(): Double = getPolylineLength(size) { n, func -> func(this[n].x, this[n].y) }

fun List<MPoint>.bounds(out: MRectangle = MRectangle(), bb: BoundsBuilder = BoundsBuilder()): MRectangle = bb.add(this).getBounds(out)
fun Iterable<IPoint>.bounds(out: MRectangle = MRectangle(), bb: BoundsBuilder = BoundsBuilder()): MRectangle = bb.add(this).getBounds(out)

fun min(a: IPoint, b: IPoint, out: MPoint = MPoint()): MPoint = out.setTo(kotlin.math.min(a.x, b.x), kotlin.math.min(a.y, b.y))
fun max(a: IPoint, b: IPoint, out: MPoint = MPoint()): MPoint = out.setTo(kotlin.math.max(a.x, b.x), kotlin.math.max(a.y, b.y))
fun IPoint.clamp(min: Double, max: Double, out: MPoint = MPoint()): MPoint = out.setTo(x.clamp(min, max), y.clamp(min, max))

fun Point.toInt(): PointInt = PointInt(x.toInt(), y.toInt())
fun Point.toIntCeil(): PointInt = PointInt(x.toIntCeil(), y.toIntCeil())
fun Point.toIntRound(): PointInt = PointInt(x.toIntRound(), y.toIntRound())
fun Point.toIntFloor(): PointInt = PointInt(x.toIntFloor(), y.toIntFloor())
fun PointInt.toFloat(): Point = Point(x, y)
