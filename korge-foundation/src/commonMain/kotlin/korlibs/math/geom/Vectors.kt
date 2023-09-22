@file:Suppress("NOTHING_TO_INLINE")

package korlibs.math.geom

import korlibs.math.*
import korlibs.math.annotations.*
import korlibs.math.interpolation.*
import korlibs.number.*
import kotlin.math.*

data class Vec2d(val x: Double, val y: Double)
typealias Vec2f = Vector2
typealias Vec2i = PointInt
data class Vec3d(val x: Double, val y: Double, val z: Double)
typealias Vec3f = Vector3
data class Vec3i(val x: Int, val y: Int, val z: Int)
data class Vec4d(val x: Double, val y: Double, val z: Double, val w: Double)
typealias Vec4f = Vector4
typealias Vec4i = Vector4Int
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
data class Vector2(val x: Float, val y: Float) {
//inline class Vector2 internal constructor(internal val raw: Float2Pack) {
    //val x: Float get() = raw.f0
    //val y: Float get() = raw.f1
    //operator fun component1(): Float = x
    //operator fun component2(): Float = y
    //fun copy(x: Float = this.x, y: Float = this.y): Vector2 = Point(x, y)


    val xF: Float get() = x
    val yF: Float get() = y

    val xD: Double get() = x.toDouble()
    val yD: Double get() = y.toDouble()

    //constructor(x: Float, y: Float) : this(float2PackOf(x, y))
    constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
    constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())

    constructor(x: Double, y: Int) : this(x.toFloat(), y.toFloat())
    constructor(x: Int, y: Double) : this(x.toFloat(), y.toFloat())

    constructor(x: Float, y: Int) : this(x.toFloat(), y.toFloat())
    constructor(x: Int, y: Float) : this(x.toFloat(), y.toFloat())

    @Deprecated("")
    constructor(p: MPoint) : this(p.x.toFloat(), p.y.toFloat())
    //constructor(p: Vector2) : this(p.raw)
    constructor() : this(0f, 0f)
    //constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
    //constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())

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

    infix fun cross(that: Vector2): Float = crossProduct(this, that)
    infix fun dot(that: Vector2): Float = ((this.x * that.x) + (this.y * that.y))

    fun angleTo(other: Vector2, up: Vector2 = UP): Angle = Angle.between(this.x, this.y, other.x, other.y, up)
    val angle: Angle get() = angle()
    fun angle(up: Vector2 = UP): Angle = Angle.between(0f, 0f, this.x, this.y, up)

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
    fun round(): Vector2 = Point(round(x), round(y))
    fun ceil(): Vector2 = Point(ceil(x), ceil(y))
    fun floor(): Vector2 = Point(floor(x), floor(y))

    //fun copy(x: Double = this.x, y: Double = this.y): Vector2 = Point(x, y)

    fun isAlmostEquals(other: Vector2, epsilon: Float = 0.00001f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon)

    val niceStr: String get() = "(${x.niceStr}, ${y.niceStr})"
    fun niceStr(decimalPlaces: Int): String = "(${x.niceStr(decimalPlaces)}, ${y.niceStr(decimalPlaces)})"
    override fun toString(): String = niceStr

    fun reflected(normal: Vector2): Vector2 {
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

        /** Mathematically typical LEFT, matching screen coordinates (-1, 0) */
        val LEFT = Point(-1f, 0f)
        /** Mathematically typical RIGHT, matching screen coordinates (+1, 0) */
        val RIGHT = Point(+1f, 0f)

        /** Mathematically typical UP (0, +1) */
        val UP = Point(0f, +1f)
        /** UP using screen coordinates as reference (0, -1) */
        val UP_SCREEN = Point(0f, -1f)

        /** Mathematically typical DOWN (0, -1) */
        val DOWN = Point(0f, -1f)
        /** DOWN using screen coordinates as reference (0, +1) */
        val DOWN_SCREEN = Point(0f, +1f)


        //inline operator fun invoke(x: Int, y: Int): Vector2 = Point(x.toDouble(), y.toDouble())
        //inline operator fun invoke(x: Float, y: Float): Vector2 = Point(x.toDouble(), y.toDouble())

        //fun fromRaw(raw: Float2Pack) = Point(raw)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise for up=UP and clock-wise for up=UP_SCREEN */
        inline fun polar(x: Float, y: Float, angle: Angle, length: Float = 1f, up: Vector2 = UP): Vector2 = Point(x + angle.cosineF(up) * length, y + angle.sineF(up) * length)
        inline fun polar(x: Double, y: Double, angle: Angle, length: Float = 1f, up: Vector2 = UP): Vector2 = Point(x + angle.cosineD(up) * length, y + angle.sineD(up) * length)
        inline fun polar(base: Vector2, angle: Angle, length: Float = 1f, up: Vector2 = UP): Vector2 = polar(base.x, base.y, angle, length, up)
        inline fun polar(angle: Angle, length: Float = 1f, up: Vector2 = UP): Vector2 = polar(0.0, 0.0, angle, length, up)

        inline fun middle(a: Vector2, b: Vector2): Vector2 = (a + b) * 0.5

        fun angle(ax: Double, ay: Double, bx: Double, by: Double, up: Vector2 = UP): Angle = Angle.between(ax, ay, bx, by, up)
        fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, up: Vector2 = UP): Angle = Angle.between(x1 - x2, y1 - y2, x1 - x3, y1 - y3, up)

        fun angle(a: Vector2, b: Vector2, up: Vector2 = UP): Angle = Angle.between(a, b, up)
        fun angle(p1: Vector2, p2: Vector2, p3: Vector2, up: Vector2 = UP): Angle = Angle.between(p1 - p2, p1 - p3, up)

        fun angleArc(a: Vector2, b: Vector2, up: Vector2 = UP): Angle = Angle.fromRadians(acos((a dot b) / (a.length * b.length))).adjustFromUp(up)
        fun angleFull(a: Vector2, b: Vector2, up: Vector2 = UP): Angle = Angle.between(a, b, up)

        fun distance(a: Double, b: Double): Double = abs(a - b)
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = hypot(x1 - x2, y1 - y2)
        fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float = hypot(x1 - x2, y1 - y2)
        fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Float = distance(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())
        fun distance(a: Vector2, b: Vector2): Float = distance(a.x, a.y, b.x, b.y)
        fun distance(a: Vector2Int, b: Vector2Int): Float = distance(a.x, a.y, b.x, b.y)

        fun distanceSquared(a: Vector2, b: Vector2): Float = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(a: Vector2Int, b: Vector2Int): Int = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(x1: Double, y1: Double, x2: Double, y2: Double): Double = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Float, y1: Float, x2: Float, y2: Float): Float = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Int, y1: Int, x2: Int, y2: Int): Int = square(x1 - x2) + square(y1 - y2)

        @Deprecated("Likely searching for orientation")
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
        fun orientation(p1: Vector2, p2: Vector2, p3: Vector2, up: Vector2 = UP): Float = orientation(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, up)
        fun orientation(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, up: Vector2 = UP): Float {
            Orientation.checkValidUpVector(up)
            val res = crossProduct(cx - ax, cy - ay, bx - ax, by - ay)
            return if (up.y > 0f) res else -res
        }
        fun orientation(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, up: Vector2 = UP): Double {
            Orientation.checkValidUpVector(up)
            val res = crossProduct(cx - ax, cy - ay, bx - ax, by - ay)
            return if (up.y > 0f) res else -res
        }

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

fun abs(a: Vector2): Vector2 = a.absoluteValue
fun min(a: Vector2, b: Vector2): Vector2 = Vector2(min(a.x, b.x), min(a.y, b.y))
fun max(a: Vector2, b: Vector2): Vector2 = Vector2(max(a.x, b.x), max(a.y, b.y))
fun Vector2.clamp(min: Float, max: Float): Vector2 = Vector2(x.clamp(min, max), y.clamp(min, max))
fun Vector2.clamp(min: Double, max: Double): Vector2 = clamp(min.toFloat(), max.toFloat())
fun Vector2.clamp(min: Vector2, max: Vector2): Vector2 = Vector2(x.clamp(min.x, max.x), y.clamp(min.y, max.y))

fun Vector2.toInt(): Vector2Int = Vector2Int(x.toInt(), y.toInt())
fun Vector2.toIntCeil(): Vector2Int = Vector2Int(x.toIntCeil(), y.toIntCeil())
fun Vector2.toIntRound(): Vector2Int = Vector2Int(x.toIntRound(), y.toIntRound())
fun Vector2.toIntFloor(): Vector2Int = Vector2Int(x.toIntFloor(), y.toIntFloor())
fun Vector2Int.toFloat(): Vector2 = Vector2(x, y)

typealias PointInt = Vector2Int

//@KormaValueApi
data class Vector2Int(val x: Int, val y: Int) {
    //operator fun component1(): Int = x
    //operator fun component2(): Int = y
    //fun copy(x: Int = this.x, y: Int = this.y): Vector2Int = Vector2Int(x, y)

//inline class Vector2Int(internal val raw: Int2Pack) {

    companion object {
        val ZERO = Vector2Int(0, 0)
    }

    //val x: Int get() = raw.i0
    //val y: Int get() = raw.i1

    constructor() : this(0, 0)
    //constructor(x: Int, y: Int) : this(int2PackOf(x, y))

    val mutable: MPointInt get() = MPointInt(x, y)

    operator fun plus(that: Vector2Int): Vector2Int = Vector2Int(this.x + that.x, this.y + that.y)
    operator fun minus(that: Vector2Int): Vector2Int = Vector2Int(this.x - that.x, this.y - that.y)
    operator fun times(that: Vector2Int): Vector2Int = Vector2Int(this.x * that.x, this.y * that.y)
    operator fun div(that: Vector2Int): Vector2Int = Vector2Int(this.x / that.x, this.y / that.y)
    operator fun rem(that: Vector2Int): Vector2Int = Vector2Int(this.x % that.x, this.y % that.y)

    override fun toString(): String = "($x, $y)"
}

//inline class Vector3(val data: Float4Pack) {
//data class Vector3(val x: Float, val y: Float, val z: Float, val w: Float) {
data class Vector3(val x: Float, val y: Float, val z: Float) {
    //operator fun component1(): Float = x
    //operator fun component2(): Float = y
    //operator fun component3(): Float = z
    //fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z): Vector3 = Vector3(x, y, z)
    //val x: Float get() = data.f0
    //val y: Float get() = data.f1
    //val z: Float get() = data.f2

    companion object {
        val NaN = Vector3(Float.NaN, Float.NaN, Float.NaN)

        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)

        val FORWARD	= Vector3(0f, 0f, 1f)
        val BACK = Vector3(0f, 0f, -1f)
        val LEFT = Vector3(-1f, 0f, 0f)
        val RIGHT = Vector3(1f, 0f, 0f)
        val UP = Vector3(0f, 1f, 0f)
        val DOWN = Vector3(0f, -1f, 0f)

        operator fun invoke(): Vector3 = ZERO

        fun cross(a: Vector3, b: Vector3): Vector3 = Vector3(
            ((a.y * b.z) - (a.z * b.y)),
            ((a.z * b.x) - (a.x * b.z)),
            ((a.x * b.y) - (a.y * b.x)),
        )

        fun length(x: Float, y: Float, z: Float): Float = sqrt(lengthSq(x, y, z))
        fun lengthSq(x: Float, y: Float, z: Float): Float = x * x + y * y + z * z

        fun fromArray(array: FloatArray, offset: Int): Vector3 =
            Vector3(array[offset + 0], array[offset + 1], array[offset + 2])

        inline fun func(func: (index: Int) -> Float): Vector3 = Vector3(func(0), func(1), func(2))
    }

    //constructor(x: Float, y: Float, z: Float) : this(float4PackOf(x, y, z, 0f))
    constructor(x: Int, y: Int, z: Int) : this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(x: Double, y: Double, z: Double) : this(x.toFloat(), y.toFloat(), z.toFloat())

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z)
    val length: Float get() = sqrt(lengthSquared)
    fun normalized(): Vector3 {
        val length = this.length
        //if (length.isAlmostZero()) return Vector3.ZERO
        if (length == 0f) return Vector3.ZERO
        return this / length
    }

    // https://math.stackexchange.com/questions/13261/how-to-get-a-reflection-vector
    // 𝑟=𝑑−2(𝑑⋅𝑛)𝑛
    fun reflected(surfaceNormal: Vector3): Vector3 {
        val d = this
        val n = surfaceNormal
        return d - 2f * (d dot n) * n
    }

    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IndexOutOfBoundsException()
    }

    operator fun unaryPlus(): Vector3 = this
    operator fun unaryMinus(): Vector3 = Vector3(-this.x, -this.y, -this.z)

    operator fun plus(v: Vector3): Vector3 = Vector3(this.x + v.x, this.y + v.y, this.z + v.z)
    operator fun minus(v: Vector3): Vector3 = Vector3(this.x - v.x, this.y - v.y, this.z - v.z)

    operator fun times(v: Vector3): Vector3 = Vector3(this.x * v.x, this.y * v.y, this.z * v.z)
    operator fun div(v: Vector3): Vector3 = Vector3(this.x / v.x, this.y / v.y, this.z / v.z)
    operator fun rem(v: Vector3): Vector3 = Vector3(this.x % v.x, this.y % v.y, this.z % v.z)

    operator fun times(v: Float): Vector3 = Vector3(this.x * v, this.y * v, this.z * v)
    operator fun div(v: Float): Vector3 = Vector3(this.x / v, this.y / v, this.z / v)
    operator fun rem(v: Float): Vector3 = Vector3(this.x % v, this.y % v, this.z % v)

    operator fun times(v: Int): Vector3 = this * v.toFloat()
    operator fun div(v: Int): Vector3 = this / v.toFloat()
    operator fun rem(v: Int): Vector3 = this % v.toFloat()

    operator fun times(v: Double): Vector3 = this * v.toFloat()
    operator fun div(v: Double): Vector3 = this / v.toFloat()
    operator fun rem(v: Double): Vector3 = this % v.toFloat()

    infix fun dot(v: Vector3): Float = (x * v.x) + (y * v.y) + (z * v.z)
    infix fun cross(v: Vector3): Vector3 = cross(this, v)

    /** Vector3 with inverted (1f / v) components to this */
    fun inv(): Vector3 = Vector3(1f / x, 1f / y, 1f / z)

    fun isNaN(): Boolean = this.x.isNaN() && this.y.isNaN() && this.z.isNaN()
    val absoluteValue: Vector3 get() = Vector3(abs(x), abs(y), abs(z))

    override fun toString(): String = "Vector3(${x.niceStr}, ${y.niceStr}, ${z.niceStr})"

    fun toVector4(w: Float = 1f): Vector4 = Vector4(x, y, z, w)
    fun isAlmostEquals(other: Vector3, epsilon: Float = 0.00001f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon) && this.z.isAlmostEquals(other.z, epsilon)
}

operator fun Int.times(v: Vector3): Vector3 = v * this
operator fun Float.times(v: Vector3): Vector3 = v * this
operator fun Double.times(v: Vector3): Vector3 = v * this

fun vec(x: Float, y: Float, z: Float): Vector3 = Vector3(x, y, z)
fun vec3(x: Float, y: Float, z: Float): Vector3 = Vector3(x, y, z)

@KormaMutableApi
@Deprecated("")
sealed interface IVector3 {
    val x: Float
    val y: Float
    val z: Float

    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> 0f
    }
}

fun abs(a: Vector3): Vector3 = a.absoluteValue
fun min(a: Vector3, b: Vector3): Vector3 = Vector3(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))
fun max(a: Vector3, b: Vector3): Vector3 = Vector3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))
fun Vector3.clamp(min: Float, max: Float): Vector3 = Vector3(x.clamp(min, max), y.clamp(min, max), z.clamp(min, max))
fun Vector3.clamp(min: Double, max: Double): Vector3 = clamp(min.toFloat(), max.toFloat())
fun Vector3.clamp(min: Vector3, max: Vector3): Vector3 = Vector3(x.clamp(min.x, max.x), y.clamp(min.y, max.y), z.clamp(min.z, max.z))

//fun Vector3.toInt(): Vector3Int = Vector3Int(x.toInt(), y.toInt(), z.toInt())
//fun Vector3.toIntCeil(): Vector3Int = Vector3Int(x.toIntCeil(), y.toIntCeil(), z.toIntCeil())
//fun Vector3.toIntRound(): Vector3Int = Vector3Int(x.toIntRound(), y.toIntRound(), z.toIntRound())
//fun Vector3.toIntFloor(): Vector3Int = Vector3Int(x.toIntFloor(), y.toIntFloor(), z.toIntFloor())

//@KormaValueApi
//inline class Vector4(val data: Float4) {
data class Vector4(val x: Float, val y: Float, val z: Float, val w: Float) {
    //operator fun component1(): Float = x
    //operator fun component2(): Float = y
    //operator fun component3(): Float = z
    //operator fun component4(): Float = w
    //val x: Float get() = data.f0
    //val y: Float get() = data.f1
    //val z: Float get() = data.f2
    //val w: Float get() = data.f3
    //fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z, w: Float = this.w): Vector4 = Vector4(x, y, z, w)

    companion object {
        val ZERO = Vector4(0f, 0f, 0f, 0f)
        val ONE = Vector4(1f, 1f, 1f, 1f)

        operator fun invoke(): Vector4 = Vector4.ZERO

        //fun cross(a: Vector4, b: Vector4): Vector4 = Vector4(
        //    (a.y * b.z - a.z * b.y),
        //    (a.z * b.x - a.x * b.z),
        //    (a.x * b.y - a.y * b.x),
        //    1f
        //)

        //fun cross(v1: Vector4, v2: Vector4, v3: Vector4): Vector4 = TODO()
        fun fromArray(array: FloatArray, offset: Int = 0): Vector4 = Vector4(array[offset + 0], array[offset + 1], array[offset + 2], array[offset + 3])

        fun length(x: Float, y: Float, z: Float, w: Float): Float = sqrt(lengthSq(x, y, z, w))
        fun lengthSq(x: Float, y: Float, z: Float, w: Float): Float = x * x + y * y + z * z + w * w

        inline fun func(func: (index: Int) -> Float): Vector4 = Vector4(func(0), func(1), func(2), func(3))
    }

    constructor(xyz: Vector3, w: Float) : this(xyz.x, xyz.y, xyz.z, w)
    //constructor(x: Float, y: Float, z: Float, w: Float) : this(float4PackOf(x, y, z, w))
    constructor(x: Int, y: Int, z: Int, w: Int) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    constructor(x: Double, y: Double, z: Double, w: Double) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    val xyz: Vector3 get() = Vector3(x, y, z)

    val length3Squared: Float get() = (x * x) + (y * y) + (z * z)
    /** Only taking into accoount x, y, z */
    val length3: Float get() = sqrt(length3Squared)

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z) + (w * w)
    val length: Float get() = sqrt(lengthSquared)

    fun normalized(): Vector4 {
        val length = this.length
        if (length == 0f) return Vector4.ZERO
        return this / length
    }

    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> throw IndexOutOfBoundsException()
    }

    operator fun unaryPlus(): Vector4 = this
    operator fun unaryMinus(): Vector4 = Vector4(-x, -y, -z, -w)

    operator fun plus(v: Vector4): Vector4 = Vector4(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: Vector4): Vector4 = Vector4(x - v.x, y - v.y, z - v.z, w - v.w)

    operator fun times(v: Vector4): Vector4 = Vector4(x * v.x, y * v.y, z * v.z, w * v.w)
    operator fun div(v: Vector4): Vector4 = Vector4(x / v.x, y / v.y, z / v.z, w / v.w)
    operator fun rem(v: Vector4): Vector4 = Vector4(x % v.x, y % v.y, z % v.z, w % v.w)

    operator fun times(v: Float): Vector4 = Vector4(x * v, y * v, z * v, w * v)
    operator fun div(v: Float): Vector4 = Vector4(x / v, y / v, z / v, w / v)
    operator fun rem(v: Float): Vector4 = Vector4(x % v, y % v, z % v, w % v)

    infix fun dot(v: Vector4): Float = (x * v.x) + (y * v.y) + (z * v.z) + (w * v.w)
    //infix fun cross(v: Vector4): Vector4 = cross(this, v)

    fun copyTo(out: FloatArray, offset: Int = 0): FloatArray {
        out[offset + 0] = x
        out[offset + 1] = y
        out[offset + 2] = z
        out[offset + 3] = w
        return out
    }

    /** Vector4 with inverted (1f / v) components to this */
    fun inv(): Vector4 = Vector4(1f / x, 1f / y, 1f / z, 1f / w)

    fun isNaN(): Boolean = this.x.isNaN() && this.y.isNaN() && this.z.isNaN() && this.w.isNaN()
    val absoluteValue: Vector4 get() = Vector4(abs(x), abs(y), abs(z), abs(w))

    override fun toString(): String = "Vector4(${x.niceStr}, ${y.niceStr}, ${z.niceStr}, ${w.niceStr})"

    // @TODO: Should we scale Vector3 by w?
    fun toVector3(): Vector3 = Vector3(x, y, z)
    fun isAlmostEquals(other: Vector4, epsilon: Float = 0.00001f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon) && this.z.isAlmostEquals(other.z, epsilon) && this.w.isAlmostEquals(other.w, epsilon)
}

fun vec(x: Float, y: Float, z: Float, w: Float): Vector4 = Vector4(x, y, z, w)
fun vec4(x: Float, y: Float, z: Float, w: Float = 1f): Vector4 = Vector4(x, y, z, w)

fun abs(a: Vector4): Vector4 = a.absoluteValue
fun min(a: Vector4, b: Vector4): Vector4 = Vector4(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z), min(a.w, b.w))
fun max(a: Vector4, b: Vector4): Vector4 = Vector4(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z), max(a.w, b.w))
fun Vector4.clamp(min: Float, max: Float): Vector4 = Vector4(x.clamp(min, max), y.clamp(min, max), z.clamp(min, max), w.clamp(min, max))
fun Vector4.clamp(min: Double, max: Double): Vector4 = clamp(min.toFloat(), max.toFloat())
fun Vector4.clamp(min: Vector4, max: Vector4): Vector4 = Vector4(x.clamp(min.x, max.x), y.clamp(min.y, max.y), z.clamp(min.z, max.z), w.clamp(min.w, max.w))

data class Vector4Int(val x: Int, val y: Int, val z: Int, val w: Int) {
}

data class PointFixed(val x: Fixed, val y: Fixed) {
    operator fun unaryMinus(): PointFixed = PointFixed(-this.x, -this.y)
    operator fun unaryPlus(): PointFixed = this

    operator fun plus(that: PointFixed): PointFixed = PointFixed(this.x + that.x, this.y + that.y)
    operator fun minus(that: PointFixed): PointFixed = PointFixed(this.x - that.x, this.y - that.y)
    operator fun times(that: PointFixed): PointFixed = PointFixed(this.x * that.x, this.y * that.y)
    operator fun times(that: Fixed): PointFixed = PointFixed(this.x * that, this.y * that)
    operator fun div(that: PointFixed): PointFixed = PointFixed(this.x / that.x, this.y / that.y)
    operator fun rem(that: PointFixed): PointFixed = PointFixed(this.x % that.x, this.y % that.y)

    override fun toString(): String = "($x, $y)"
}
