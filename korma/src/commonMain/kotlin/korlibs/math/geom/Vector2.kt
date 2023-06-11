@file:Suppress("NOTHING_TO_INLINE")

package korlibs.math.geom

import korlibs.math.internal.*
import korlibs.math.interpolation.*
import korlibs.math.isAlmostEquals
import korlibs.math.isAlmostZero
import korlibs.math.roundDecimalPlaces
import korlibs.memory.*
import korlibs.memory.pack.*
import kotlin.math.*

typealias Point = Vector2

fun vec(x: Float, y: Float): Vector2 = Vector2(x, y)
fun vec2(x: Float, y: Float): Vector2 = Vector2(x, y)

//////////////////////////////
// VALUE CLASSES
//////////////////////////////

@Deprecated("", ReplaceWith("p", "korlibs.math.geom.Point")) fun Point(p: Vector2): Vector2 = p
@Deprecated("", ReplaceWith("p", "korlibs.math.geom.Vector2")) fun Vector2(p: Vector2): Vector2 = p

//data class Point(val x: Double, val y: Double) {
// @JvmInline value
//@KormaValueApi
//data class Point(val x: Double, val y: Double) {
inline class Vector2 internal constructor(internal val raw: Float2Pack) {
    val x: Float get() = raw.f0
    val y: Float get() = raw.f1

    val xF: Float get() = x
    val yF: Float get() = y

    val xD: Double get() = x.toDouble()
    val yD: Double get() = y.toDouble()

    constructor(x: Float, y: Float) : this(float2PackOf(x, y))
    constructor(x: Double, y: Double) : this(float2PackOf(x.toFloat(), y.toFloat()))
    constructor(x: Int, y: Int) : this(float2PackOf(x.toFloat(), y.toFloat()))

    constructor(x: Double, y: Int) : this(float2PackOf(x.toFloat(), y.toFloat()))
    constructor(x: Int, y: Double) : this(float2PackOf(x.toFloat(), y.toFloat()))

    constructor(x: Float, y: Int) : this(float2PackOf(x.toFloat(), y.toFloat()))
    constructor(x: Int, y: Float) : this(float2PackOf(x.toFloat(), y.toFloat()))

    @Deprecated("")
    constructor(p: MPoint) : this(p.x.toFloat(), p.y.toFloat())
    //constructor(p: Vector2) : this(p.raw)
    constructor() : this(0f, 0f)
    //constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
    //constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())

    operator fun component1(): Float = x
    operator fun component2(): Float = y

    fun copy(x: Float = this.x, y: Float = this.y): Vector2 = Point(x, y)
    fun copy(x: Double = this.xD, y: Double = this.yD): Vector2 = Point(x, y)

    inline operator fun unaryMinus(): Vector2 = Point(-x, -y)
    inline operator fun unaryPlus(): Vector2 = this

    inline operator fun plus(that: Size): Vector2 = Point(x + that.width, y + that.height)
    inline operator fun minus(that: Size): Vector2 = Point(x - that.width, y - that.height)

    inline operator fun plus(that: Vector2): Vector2 = Point(x + that.x, y + that.y)
    inline operator fun minus(that: Vector2): Vector2 = Point(x - that.x, y - that.y)
    inline operator fun times(that: Vector2): Vector2 = Point(x * that.x, y * that.y)
    inline operator fun times(that: Size): Vector2 = Point(x * that.width, y * that.height)
    inline operator fun times(that: Scale): Vector2 = Point(x * that.scaleX, y * that.scaleY)
    inline operator fun div(that: Vector2): Vector2 = Point(x / that.x, y / that.y)
    inline operator fun div(that: Size): Vector2 = Point(x / that.width, y / that.height)
    inline operator fun rem(that: Vector2): Vector2 = Point(x % that.x, y % that.y)
    inline operator fun rem(that: Size): Vector2 = Point(x % that.width, y % that.height)

    inline operator fun times(scale: Float): Vector2 = Point(x * scale, y * scale)
    inline operator fun times(scale: Double): Vector2 = this * scale.toFloat()
    inline operator fun times(scale: Int): Vector2 = this * scale.toDouble()

    inline operator fun div(scale: Float): Vector2 = Point(x / scale, y / scale)
    inline operator fun div(scale: Double): Vector2 = this / scale.toFloat()
    inline operator fun div(scale: Int): Vector2 = this / scale.toDouble()

    inline operator fun rem(scale: Float): Vector2 = Point(x % scale, y % scale)
    inline operator fun rem(scale: Double): Vector2 = this % scale.toFloat()
    inline operator fun rem(scale: Int): Vector2 = this % scale.toDouble()

    fun avgComponent(): Float = x * 0.5f + y * 0.5f
    fun minComponent(): Float = min(x, y)
    fun maxComponent(): Float = max(x, y)

    fun distanceTo(x: Float, y: Float): Float = hypot(x - this.x, y - this.y)
    fun distanceTo(x: Double, y: Double): Float = this.distanceTo(x.toFloat(), y.toFloat())
    fun distanceTo(x: Int, y: Int): Float = this.distanceTo(x.toDouble(), y.toDouble())
    fun distanceTo(that: Vector2): Float = distanceTo(that.x, that.y)

    infix fun dot(that: Vector2): Float = ((this.x * that.x) + (this.y * that.y))

    fun angleTo(other: Vector2): Angle = Angle.between(this.x, this.y, other.x, other.y)
    val angle: Angle get() = Angle.between(0f, 0f, this.x, this.y)

    inline fun deltaTransformed(m: Matrix): Vector2 = m.deltaTransform(this)
    inline fun transformed(m: Matrix): Vector2 = m.transform(this)

    fun transformX(m: Matrix): Float = m.transform(this).x
    fun transformY(m: Matrix): Float = m.transform(this).y

    inline fun transformedNullable(m: Matrix?): Vector2 = if (m != null && m.isNotNIL) m.transform(this) else this
    fun transformNullableX(m: Matrix?): Float = if (m != null && m.isNotNIL) m.transform(this).x else x
    fun transformNullableY(m: Matrix?): Float = if (m != null && m.isNotNIL) m.transform(this).y else y

    operator fun get(component: Int) = when (component) {
        0 -> x; 1 -> y
        else -> throw IndexOutOfBoundsException("Point doesn't have $component component")
    }
    val length: Float get() = hypot(x, y)
    val lengthSquared: Float get() {
        val x = x
        val y = y
        return x*x + y*y
    }
    val magnitude: Float get() = hypot(x, y)
    val normalized: Vector2 get() = this * (1f / magnitude)
    val unit: Vector2 get() = this / length

    /** Normal vector. Rotates the vector/point -90 degrees (not normalizing it) */
    fun toNormal(): Vector2 = Point(-this.y, this.x)


    val int: Vector2Int get() = Vector2Int(x.toInt(), y.toInt())
    val intRound: Vector2Int get() = Vector2Int(x.roundToInt(), y.roundToInt())

    fun roundDecimalPlaces(places: Int): Vector2 = Point(x.roundDecimalPlaces(places), y.roundDecimalPlaces(places))
    fun round(): Vector2 = Point(kotlin.math.round(x), kotlin.math.round(y))
    fun ceil(): Vector2 = Point(kotlin.math.ceil(x), kotlin.math.ceil(y))
    fun floor(): Vector2 = Point(kotlin.math.floor(x), kotlin.math.floor(y))

    //fun copy(x: Double = this.x, y: Double = this.y): Vector2 = Point(x, y)

    fun isAlmostEquals(other: Vector2, epsilon: Float = 0.00001f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon)

    val niceStr: String get() = "(${x.niceStr}, ${y.niceStr})"
    fun niceStr(decimalPlaces: Int): String = "(${x.niceStr(decimalPlaces)}, ${y.niceStr(decimalPlaces)})"
    override fun toString(): String = niceStr

    fun Vector2.reflected(normal: Vector2): Vector2 {
        val d = this
        val n = normal
        return d - 2f * (d dot n) * n
    }

    /** Vector2 with inverted (1f / v) components to this */
    fun inv(): Vector2 = Vector2(1f / x, 1f / y)

    fun isNaN(): Boolean = this.x.isNaN() && this.y.isNaN()

    val absoluteValue: Vector2 get() = Point(abs(x), abs(y))

    @Deprecated("", ReplaceWith("ratio.interpolate(this, other)", "korlibs.math.interpolation.interpolate")) fun interpolateWith(ratio: Ratio, other: Vector2): Vector2 = ratio.interpolate(this, other)

    companion object {
        val ZERO = Point(0f, 0f)
        val NaN = Point(Float.NaN, Float.NaN)

        //inline operator fun invoke(x: Int, y: Int): Vector2 = Point(x.toDouble(), y.toDouble())
        //inline operator fun invoke(x: Float, y: Float): Vector2 = Point(x.toDouble(), y.toDouble())

        //fun fromRaw(raw: Float2Pack) = Point(raw)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        inline fun polar(x: Float, y: Float, angle: Angle, length: Float = 1f): Vector2 = Point(x + angle.cosineF * length, y + angle.sineF * length)
        inline fun polar(x: Double, y: Double, angle: Angle, length: Float = 1f): Vector2 = Point(x + angle.cosineD * length, y + angle.sineD * length)
        inline fun polar(base: Vector2, angle: Angle, length: Float = 1f): Vector2 = polar(base.x, base.y, angle, length)
        inline fun polar(angle: Angle, length: Float = 1f): Vector2 = polar(0.0, 0.0, angle, length)

        inline fun middle(a: Vector2, b: Vector2): Vector2 = (a + b) * 0.5

        fun angle(ax: Double, ay: Double, bx: Double, by: Double): Angle = Angle.between(ax, ay, bx, by)
        fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Angle = Angle.between(x1 - x2, y1 - y2, x1 - x3, y1 - y3)

        fun angle(a: Vector2, b: Vector2): Angle = Angle.between(a, b)
        fun angle(p1: Vector2, p2: Vector2, p3: Vector2): Angle = Angle.between(p1 - p2, p1 - p3)

        fun angleArc(a: Vector2, b: Vector2): Angle = Angle.fromRadians(acos((a dot b) / (a.length * b.length)))
        fun angleFull(a: Vector2, b: Vector2): Angle = Angle.between(a, b)

        fun distance(a: Double, b: Double): Double = kotlin.math.abs(a - b)
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = kotlin.math.hypot(x1 - x2, y1 - y2)
        fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float = kotlin.math.hypot(x1 - x2, y1 - y2)
        fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Float = distance(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())
        fun distance(a: Vector2, b: Vector2): Float = distance(a.x, a.y, b.x, b.y)
        fun distance(a: Vector2Int, b: Vector2Int): Float = distance(a.x, a.y, b.x, b.y)

        fun distanceSquared(a: Vector2, b: Vector2): Float = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(a: Vector2Int, b: Vector2Int): Int = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(x1: Double, y1: Double, x2: Double, y2: Double): Double = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Float, y1: Float, x2: Float, y2: Float): Float = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Int, y1: Int, x2: Int, y2: Int): Int = square(x1 - x2) + square(y1 - y2)

        inline fun direction(a: Vector2, b: Vector2): Vector2 = b - a

        fun compare(l: Vector2, r: Vector2): Int = compare(l.x, l.y, r.x, r.y)
        fun compare(lx: Float, ly: Float, rx: Float, ry: Float): Int = ly.compareTo(ry).let { ret -> if (ret == 0) lx.compareTo(rx) else ret }
        fun compare(lx: Double, ly: Double, rx: Double, ry: Double): Int = ly.compareTo(ry).let { ret -> if (ret == 0) lx.compareTo(rx) else ret }

        private fun square(x: Double): Double = x * x
        private fun square(x: Float): Float = x * x
        private fun square(x: Int): Int = x * x

        fun dot(aX: Double, aY: Double, bX: Double, bY: Double): Double = (aX * bX) + (aY * bY)
        fun dot(aX: Float, aY: Float, bX: Float, bY: Float): Float = (aX * bX) + (aY * bY)
        fun dot(a: Vector2, b: Vector2): Float = dot(a.x, a.y, b.x, b.y)

        fun isCollinear(p1: Point, p2: Point, p3: Point): Boolean =
            isCollinear(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)

        fun isCollinear(p1x: Float, p1y: Float, p2x: Float, p2y: Float, p3x: Float, p3y: Float): Boolean {
            val area2 = (p1x * (p2y - p3y) + p2x * (p3y - p1y) + p3x * (p1y - p2y)) // 2x triangle area
            //println("($p1x, $p1y), ($p2x, $p2y), ($p3x, $p3y) :: area=$area2")
            return area2.isAlmostZero()

            //val div1 = (p2x - p1x) / (p2y - p1y)
            //val div2 = (p1x - p3x) / (p1y - p3y)
            //val result = (div1 - div2).absoluteValue
            //println("result=$result, div1=$div1, div2=$div2, xa=$p1x, ya=$p1y, x=$p2x, y=$p2y, xb=$p3x, yb=$p3y")
            //if (div1.isInfinite() != div2.isInfinite()) return false
            //return result.isAlmostZero() || result.isInfinite()
        }

        fun isCollinear(xa: Double, ya: Double, x: Double, y: Double, xb: Double, yb: Double): Boolean = isCollinear(
            xa.toFloat(), ya.toFloat(),
            x.toFloat(), y.toFloat(),
            xb.toFloat(), yb.toFloat(),
        )

        fun isCollinear(xa: Int, ya: Int, x: Int, y: Int, xb: Int, yb: Int): Boolean = isCollinear(
            xa.toFloat(), ya.toFloat(),
            x.toFloat(), y.toFloat(),
            xb.toFloat(), yb.toFloat(),
        )

        // https://algorithmtutor.com/Computational-Geometry/Determining-if-two-consecutive-segments-turn-left-or-right/
        /** < 0 left, > 0 right, 0 collinear */
        fun orientation(p1: Vector2, p2: Vector2, p3: Vector2): Float = orientation(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
        fun orientation(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Float = crossProduct(cx - ax, cy - ay, bx - ax, by - ay)
        fun orientation(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double = crossProduct(cx - ax, cy - ay, bx - ax, by - ay)

        fun crossProduct(ax: Float, ay: Float, bx: Float, by: Float): Float = (ax * by) - (bx * ay)
        fun crossProduct(ax: Double, ay: Double, bx: Double, by: Double): Double = (ax * by) - (bx * ay)
        fun crossProduct(p1: Vector2, p2: Vector2): Float = crossProduct(p1.x, p1.y, p2.x, p2.y)

        fun minComponents(p1: Vector2, p2: Vector2): Vector2 = Point(min(p1.x, p2.x), min(p1.y, p2.y))
        fun minComponents(p1: Vector2, p2: Vector2, p3: Vector2): Vector2 = Point(
            korlibs.math.min(p1.x, p2.x, p3.x),
            korlibs.math.min(p1.y, p2.y, p3.y)
        )
        fun minComponents(p1: Vector2, p2: Vector2, p3: Vector2, p4: Vector2): Vector2 = Point(
            korlibs.math.min(
                p1.x,
                p2.x,
                p3.x,
                p4.x
            ), korlibs.math.min(p1.y, p2.y, p3.y, p4.y)
        )
        fun maxComponents(p1: Vector2, p2: Vector2): Vector2 = Point(max(p1.x, p2.x), max(p1.y, p2.y))
        fun maxComponents(p1: Vector2, p2: Vector2, p3: Vector2): Vector2 = Point(
            korlibs.math.max(p1.x, p2.x, p3.x),
            korlibs.math.max(p1.y, p2.y, p3.y)
        )
        fun maxComponents(p1: Vector2, p2: Vector2, p3: Vector2, p4: Vector2): Vector2 = Point(
            korlibs.math.max(
                p1.x,
                p2.x,
                p3.x,
                p4.x
            ), korlibs.math.max(p1.y, p2.y, p3.y, p4.y)
        )
    }
}

@Deprecated("Allocates") val MPoint.int: MPointInt get() = MPointInt(this.x.toInt(), this.y.toInt())
@Deprecated("Allocates") val MPointInt.double: MPoint get() = MPoint(x.toDouble(), y.toDouble())

@Deprecated("")
fun Point.toMPoint(out: MPoint = MPoint()): MPoint = out.setTo(x, y)
@Deprecated("")
fun Point.mutable(out: MPoint = MPoint()): MPoint = out.setTo(x, y)
@Deprecated("")
val Point.mutable: MPoint get() = mutable()

internal inline fun getPolylineLength(size: Int, crossinline get: (n: Int) -> Point): Double {
    var out = 0.0
    var prev = Point.ZERO
    for (n in 0 until size) {
        val p = get(n)
        if (n > 0) out += Point.distance(prev, p)
        prev = p
    }
    return out
}

operator fun Int.times(v: Vector2): Vector2 = v * this
operator fun Float.times(v: Vector2): Vector2 = v * this
operator fun Double.times(v: Vector2): Vector2 = v * this

fun PointList.getPolylineLength(): Double = getPolylineLength(size) { get(it) }
fun List<Point>.getPolylineLength(): Double = getPolylineLength(size) { get(it) }

fun List<Point>.bounds(): Rectangle = BoundsBuilder(size) { this + get(it) }.bounds
fun Iterable<Point>.bounds(): Rectangle {
    var bb = BoundsBuilder()
    for (p in this) bb += p
    return bb.bounds
}

fun abs(a: Point): Point = a.absoluteValue
fun min(a: Point, b: Point): Point = Point(min(a.x, b.x), min(a.y, b.y))
fun max(a: Point, b: Point): Point = Point(max(a.x, b.x), max(a.y, b.y))
fun Point.clamp(min: Float, max: Float): Point = Point(x.clamp(min, max), y.clamp(min, max))
fun Point.clamp(min: Double, max: Double): Point = clamp(min.toFloat(), max.toFloat())
fun Point.clamp(min: Point, max: Point): Point = Point(x.clamp(min.x, max.x), y.clamp(min.y, max.y))

fun Point.toInt(): Vector2Int = Vector2Int(x.toInt(), y.toInt())
fun Point.toIntCeil(): Vector2Int = Vector2Int(x.toIntCeil(), y.toIntCeil())
fun Point.toIntRound(): Vector2Int = Vector2Int(x.toIntRound(), y.toIntRound())
fun Point.toIntFloor(): Vector2Int = Vector2Int(x.toIntFloor(), y.toIntFloor())
fun Vector2Int.toFloat(): Vector2 = Point(x, y)
