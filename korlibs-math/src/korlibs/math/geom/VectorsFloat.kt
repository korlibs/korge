@file:Suppress("NOTHING_TO_INLINE")

package korlibs.math.geom

import korlibs.math.*
import korlibs.math.interpolation.*
import korlibs.number.*
import kotlin.math.*

typealias Vector2 = Vector2F
typealias Vector3 = Vector3F
typealias Vector4 = Vector4F

fun vec(x: Float, y: Float): Vector2F = Vector2F(x, y)
fun vec2(x: Float, y: Float): Vector2F = Vector2F(x, y)
fun vec(x: Float, y: Float, z: Float): Vector3F = Vector3F(x, y, z)
fun vec3(x: Float, y: Float, z: Float): Vector3F = Vector3F(x, y, z)
fun vec(x: Float, y: Float, z: Float, w: Float): Vector4F = Vector4F(x, y, z, w)
fun vec4(x: Float, y: Float, z: Float, w: Float = 1f): Vector4F = Vector4F(x, y, z, w)

//////////////////////////////
// VALUE CLASSES
//////////////////////////////

//@Deprecated("", ReplaceWith("p", "korlibs.math.geom.Point")) fun Point(p: Vector2F): Vector2F = p
//@Deprecated("", ReplaceWith("p", "korlibs.math.geom.Vector2")) fun Vector2(p: Vector2F): Vector2F = p

data class Vector2F(val x: Float, val y: Float) {
    constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
    constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())

    constructor(x: Double, y: Int) : this(x.toFloat(), y.toFloat())
    constructor(x: Int, y: Double) : this(x.toFloat(), y.toFloat())

    constructor(x: Float, y: Int) : this(x.toFloat(), y.toFloat())
    constructor(x: Int, y: Float) : this(x.toFloat(), y.toFloat())

    //constructor(p: Vector2) : this(p.raw)
    constructor() : this(0f, 0f)
    //constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
    //constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())

    fun copy(x: Double = this.x.toDouble(), y: Double = this.y.toDouble()): Vector2F = Vector2F(x, y)

    inline operator fun unaryMinus(): Vector2F = Vector2F(-x, -y)
    inline operator fun unaryPlus(): Vector2F = this

    inline operator fun plus(that: Size): Vector2F = Vector2F(x + that.width, y + that.height)
    inline operator fun minus(that: Size): Vector2F = Vector2F(x - that.width, y - that.height)

    inline operator fun plus(that: Vector2F): Vector2F = Vector2F(x + that.x, y + that.y)
    inline operator fun minus(that: Vector2F): Vector2F = Vector2F(x - that.x, y - that.y)
    inline operator fun times(that: Vector2F): Vector2F = Vector2F(x * that.x, y * that.y)
    inline operator fun times(that: Size): Vector2F = Vector2F(x * that.width, y * that.height)
    inline operator fun times(that: Scale): Vector2F = Vector2F(x * that.scaleX, y * that.scaleY)
    inline operator fun div(that: Vector2F): Vector2F = Vector2F(x / that.x, y / that.y)
    inline operator fun div(that: Size): Vector2F = Vector2F(x / that.width, y / that.height)
    inline operator fun rem(that: Vector2F): Vector2F = Vector2F(x % that.x, y % that.y)
    inline operator fun rem(that: Size): Vector2F = Vector2F(x % that.width, y % that.height)

    inline operator fun times(scale: Float): Vector2F = Vector2F(x * scale, y * scale)
    inline operator fun times(scale: Double): Vector2F = this * scale.toFloat()
    inline operator fun times(scale: Int): Vector2F = this * scale.toDouble()

    inline operator fun div(scale: Float): Vector2F = Vector2F(x / scale, y / scale)
    inline operator fun div(scale: Double): Vector2F = this / scale.toFloat()
    inline operator fun div(scale: Int): Vector2F = this / scale.toDouble()

    inline operator fun rem(scale: Float): Vector2F = Vector2F(x % scale, y % scale)
    inline operator fun rem(scale: Double): Vector2F = this % scale.toFloat()
    inline operator fun rem(scale: Int): Vector2F = this % scale.toDouble()

    fun avgComponent(): Float = x * 0.5f + y * 0.5f
    fun minComponent(): Float = min(x, y)
    fun maxComponent(): Float = max(x, y)

    fun distanceTo(x: Float, y: Float): Float = hypot(x - this.x, y - this.y)
    fun distanceTo(x: Double, y: Double): Float = this.distanceTo(x.toFloat(), y.toFloat())
    fun distanceTo(x: Int, y: Int): Float = this.distanceTo(x.toDouble(), y.toDouble())
    fun distanceTo(that: Vector2F): Float = distanceTo(that.x, that.y)

    infix fun cross(that: Vector2F): Float = crossProduct(this, that)
    infix fun dot(that: Vector2F): Float = ((this.x * that.x) + (this.y * that.y))

    fun angleTo(other: Vector2F, up: Vector2D = Vector2D.UP): Angle = Angle.between(this.x, this.y, other.x, other.y, up)
    val angle: Angle get() = angle()
    fun angle(up: Vector2D = Vector2D.UP): Angle = Angle.between(0f, 0f, this.x, this.y, up)

    inline fun deltaTransformed(m: Matrix): Vector2F = m.deltaTransform(this)
    inline fun transformed(m: Matrix): Vector2F = m.transform(this)

    fun transformX(m: Matrix): Float = m.transform(this).x
    fun transformY(m: Matrix): Float = m.transform(this).y

    inline fun transformedNullable(m: Matrix?): Vector2F = if (m != null && m.isNotNIL) m.transform(this) else this
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
    val normalized: Vector2F get() = this * (1f / magnitude)
    val unit: Vector2F get() = this / length

    /** Normal vector. Rotates the vector/point -90 degrees (not normalizing it) */
    fun toNormal(): Vector2F = Vector2F(-this.y, this.x)


    val int: Vector2I get() = Vector2I(x.toInt(), y.toInt())
    val intRound: Vector2I get() = Vector2I(x.roundToInt(), y.roundToInt())

    fun roundDecimalPlaces(places: Int): Vector2F = Vector2F(x.roundDecimalPlaces(places), y.roundDecimalPlaces(places))
    fun round(): Vector2F = Vector2F(round(x), round(y))
    fun ceil(): Vector2F = Vector2F(ceil(x), ceil(y))
    fun floor(): Vector2F = Vector2F(floor(x), floor(y))

    //fun copy(x: Double = this.x, y: Double = this.y): Vector2 = Point(x, y)

    fun isAlmostEquals(other: Vector2F, epsilon: Float = 0.00001f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon)

    val niceStr: String get() = "(${x.niceStr}, ${y.niceStr})"
    fun niceStr(decimalPlaces: Int): String = "(${x.niceStr(decimalPlaces)}, ${y.niceStr(decimalPlaces)})"
    override fun toString(): String = niceStr

    fun reflected(normal: Vector2F): Vector2F {
        val d = this
        val n = normal
        return d - 2f * (d dot n) * n
    }

    /** Vector2 with inverted (1f / v) components to this */
    fun inv(): Vector2F = Vector2F(1f / x, 1f / y)

    fun isNaN(): Boolean = this.x.isNaN() && this.y.isNaN()

    val absoluteValue: Vector2F get() = Vector2F(abs(x), abs(y))

    @Deprecated("", ReplaceWith("ratio.interpolate(this, other)", "korlibs.math.interpolation.interpolate")) fun interpolateWith(ratio: Ratio, other: Vector2F): Vector2F = ratio.interpolate(this, other)

    companion object {
        val ZERO = Vector2F(0f, 0f)
        val NaN = Vector2F(Float.NaN, Float.NaN)

        /** Mathematically typical LEFT, matching screen coordinates (-1, 0) */
        val LEFT = Vector2F(-1f, 0f)
        /** Mathematically typical RIGHT, matching screen coordinates (+1, 0) */
        val RIGHT = Vector2F(+1f, 0f)

        /** Mathematically typical UP (0, +1) */
        val UP = Vector2F(0f, +1f)
        /** UP using 2D screen coordinates as reference (0, -1) */
        val UP_SCREEN = Vector2F(0f, -1f)

        /** Mathematically typical DOWN (0, -1) */
        val DOWN = Vector2F(0f, -1f)
        /** DOWN using 2D screen coordinates as reference (0, +1) */
        val DOWN_SCREEN = Vector2F(0f, +1f)


        //inline operator fun invoke(x: Int, y: Int): Vector2 = Point(x.toDouble(), y.toDouble())
        //inline operator fun invoke(x: Float, y: Float): Vector2 = Point(x.toDouble(), y.toDouble())

        //fun fromRaw(raw: Float2Pack) = Point(raw)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise for up=UP and clock-wise for up=UP_SCREEN */
        inline fun polar(x: Float, y: Float, angle: Angle, length: Float = 1f, up: Vector2D = Vector2D.UP): Vector2F = Vector2F(x + angle.cosine(up) * length, y + angle.sine(up) * length)
        inline fun polar(x: Double, y: Double, angle: Angle, length: Float = 1f, up: Vector2D = Vector2D.UP): Vector2F = Vector2F(x + angle.cosine(up) * length, y + angle.sine(up) * length)
        inline fun polar(base: Vector2F, angle: Angle, length: Float = 1f, up: Vector2D = Vector2D.UP): Vector2F = polar(base.x, base.y, angle, length, up)
        inline fun polar(angle: Angle, length: Float = 1f, up: Vector2D = Vector2D.UP): Vector2F = polar(0.0, 0.0, angle, length, up)

        inline fun middle(a: Vector2F, b: Vector2F): Vector2F = (a + b) * 0.5

        fun angle(ax: Double, ay: Double, bx: Double, by: Double, up: Vector2D = Vector2D.UP): Angle = Angle.between(ax, ay, bx, by, up)
        fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, up: Vector2D = Vector2D.UP): Angle = Angle.between(x1 - x2, y1 - y2, x1 - x3, y1 - y3, up)

        fun angle(a: Vector2F, b: Vector2F, up: Vector2D = Vector2D.UP): Angle = Angle.between(a, b, up)
        fun angle(p1: Vector2F, p2: Vector2F, p3: Vector2F, up: Vector2D = Vector2D.UP): Angle = Angle.between(p1 - p2, p1 - p3, up)

        fun angleArc(a: Vector2F, b: Vector2F, up: Vector2D = Vector2D.UP): Angle = Angle.fromRadians(acos((a dot b) / (a.length * b.length))).adjustFromUp(up)
        fun angleFull(a: Vector2F, b: Vector2F, up: Vector2D = Vector2D.UP): Angle = Angle.between(a, b, up)

        fun distance(a: Double, b: Double): Double = abs(a - b)
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = hypot(x1 - x2, y1 - y2)
        fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float = hypot(x1 - x2, y1 - y2)
        fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Float = distance(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())
        fun distance(a: Vector2F, b: Vector2F): Float = distance(a.x, a.y, b.x, b.y)
        fun distance(a: Vector2I, b: Vector2I): Float = distance(a.x, a.y, b.x, b.y)

        fun distanceSquared(a: Vector2F, b: Vector2F): Float = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(a: Vector2I, b: Vector2I): Int = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(x1: Double, y1: Double, x2: Double, y2: Double): Double = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Float, y1: Float, x2: Float, y2: Float): Float = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Int, y1: Int, x2: Int, y2: Int): Int = square(x1 - x2) + square(y1 - y2)

        @Deprecated("Likely searching for orientation")
        inline fun direction(a: Vector2F, b: Vector2F): Vector2F = b - a

        fun compare(l: Vector2F, r: Vector2F): Int = compare(l.x, l.y, r.x, r.y)
        fun compare(lx: Float, ly: Float, rx: Float, ry: Float): Int = ly.compareTo(ry).let { ret -> if (ret == 0) lx.compareTo(rx) else ret }
        fun compare(lx: Double, ly: Double, rx: Double, ry: Double): Int = ly.compareTo(ry).let { ret -> if (ret == 0) lx.compareTo(rx) else ret }

        private fun square(x: Double): Double = x * x
        private fun square(x: Float): Float = x * x
        private fun square(x: Int): Int = x * x

        fun dot(aX: Double, aY: Double, bX: Double, bY: Double): Double = (aX * bX) + (aY * bY)
        fun dot(aX: Float, aY: Float, bX: Float, bY: Float): Float = (aX * bX) + (aY * bY)
        fun dot(a: Vector2F, b: Vector2F): Float = dot(a.x, a.y, b.x, b.y)

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
        fun orientation(p1: Vector2F, p2: Vector2F, p3: Vector2F, up: Vector2D = Vector2D.UP): Float = orientation(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, up)
        fun orientation(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, up: Vector2D = Vector2D.UP): Float {
            Orientation.checkValidUpVector(up)
            val res = crossProduct(cx - ax, cy - ay, bx - ax, by - ay)
            return if (up.y > 0f) res else -res
        }
        fun orientation(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, up: Vector2D = Vector2D.UP): Double {
            Orientation.checkValidUpVector(up)
            val res = crossProduct(cx - ax, cy - ay, bx - ax, by - ay)
            return if (up.y > 0f) res else -res
        }

        fun crossProduct(ax: Float, ay: Float, bx: Float, by: Float): Float = (ax * by) - (bx * ay)
        fun crossProduct(ax: Double, ay: Double, bx: Double, by: Double): Double = (ax * by) - (bx * ay)
        fun crossProduct(p1: Vector2F, p2: Vector2F): Float = crossProduct(p1.x, p1.y, p2.x, p2.y)

        fun minComponents(p1: Vector2F, p2: Vector2F): Vector2F = Vector2F(min(p1.x, p2.x), min(p1.y, p2.y))
        fun minComponents(p1: Vector2F, p2: Vector2F, p3: Vector2F): Vector2F = Vector2F(
            korlibs.math.min(p1.x, p2.x, p3.x),
            korlibs.math.min(p1.y, p2.y, p3.y)
        )
        fun minComponents(p1: Vector2F, p2: Vector2F, p3: Vector2F, p4: Vector2F): Vector2F = Vector2F(
            korlibs.math.min(
                p1.x,
                p2.x,
                p3.x,
                p4.x
            ), korlibs.math.min(p1.y, p2.y, p3.y, p4.y)
        )
        fun maxComponents(p1: Vector2F, p2: Vector2F): Vector2F = Vector2F(max(p1.x, p2.x), max(p1.y, p2.y))
        fun maxComponents(p1: Vector2F, p2: Vector2F, p3: Vector2F): Vector2F = Vector2F(
            korlibs.math.max(p1.x, p2.x, p3.x),
            korlibs.math.max(p1.y, p2.y, p3.y)
        )
        fun maxComponents(p1: Vector2F, p2: Vector2F, p3: Vector2F, p4: Vector2F): Vector2F = Vector2F(
            korlibs.math.max(
                p1.x,
                p2.x,
                p3.x,
                p4.x
            ), korlibs.math.max(p1.y, p2.y, p3.y, p4.y)
        )
    }
}

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

operator fun Int.times(v: Vector2F): Vector2F = v * this
operator fun Float.times(v: Vector2F): Vector2F = v * this
operator fun Double.times(v: Vector2F): Vector2F = v * this

fun PointList.getPolylineLength(): Double = getPolylineLength(size) { get(it) }
fun List<Point>.getPolylineLength(): Double = getPolylineLength(size) { get(it) }

fun List<Point>.bounds(): Rectangle = BoundsBuilder(size) { this + get(it) }.bounds
fun Iterable<Point>.bounds(): Rectangle {
    var bb = BoundsBuilder()
    for (p in this) bb += p
    return bb.bounds
}

fun abs(a: Vector2F): Vector2F = a.absoluteValue
fun min(a: Vector2F, b: Vector2F): Vector2F = Vector2F(min(a.x, b.x), min(a.y, b.y))
fun max(a: Vector2F, b: Vector2F): Vector2F = Vector2F(max(a.x, b.x), max(a.y, b.y))
fun Vector2F.clamp(min: Float, max: Float): Vector2F = Vector2F(x.clamp(min, max), y.clamp(min, max))
fun Vector2F.clamp(min: Double, max: Double): Vector2F = clamp(min.toFloat(), max.toFloat())
fun Vector2F.clamp(min: Vector2F, max: Vector2F): Vector2F = Vector2F(x.clamp(min.x, max.x), y.clamp(min.y, max.y))

fun Vector2F.toInt(): Vector2I = Vector2I(x.toInt(), y.toInt())
fun Vector2F.toIntCeil(): Vector2I = Vector2I(x.toIntCeil(), y.toIntCeil())
fun Vector2F.toIntRound(): Vector2I = Vector2I(x.toIntRound(), y.toIntRound())
fun Vector2F.toIntFloor(): Vector2I = Vector2I(x.toIntFloor(), y.toIntFloor())


data class Vector3F(val x: Float, val y: Float, val z: Float) : IsAlmostEqualsF<Vector3F> {
    companion object {
        val NaN = Vector3F(Float.NaN, Float.NaN, Float.NaN)

        val ZERO = Vector3F(0f, 0f, 0f)
        val ONE = Vector3F(1f, 1f, 1f)

        val FORWARD	= Vector3F(0f, 0f, 1f)
        val BACK = Vector3F(0f, 0f, -1f)
        val LEFT = Vector3F(-1f, 0f, 0f)
        val RIGHT = Vector3F(1f, 0f, 0f)
        val UP = Vector3F(0f, 1f, 0f)
        val DOWN = Vector3F(0f, -1f, 0f)

        operator fun invoke(): Vector3F = ZERO

        fun cross(a: Vector3F, b: Vector3F): Vector3F = Vector3F(
            ((a.y * b.z) - (a.z * b.y)),
            ((a.z * b.x) - (a.x * b.z)),
            ((a.x * b.y) - (a.y * b.x)),
        )

        fun length(x: Float, y: Float, z: Float): Float = sqrt(lengthSq(x, y, z))
        fun lengthSq(x: Float, y: Float, z: Float): Float = x * x + y * y + z * z

        fun fromArray(array: FloatArray, offset: Int): Vector3F =
            Vector3F(array[offset + 0], array[offset + 1], array[offset + 2])

        inline fun func(func: (index: Int) -> Float): Vector3F = Vector3F(func(0), func(1), func(2))
    }

    //constructor(x: Float, y: Float, z: Float) : this(float4PackOf(x, y, z, 0f))
    constructor(x: Int, y: Int, z: Int) : this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(x: Double, y: Double, z: Double) : this(x.toFloat(), y.toFloat(), z.toFloat())

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z)
    val length: Float get() = sqrt(lengthSquared)
    fun normalized(): Vector3F {
        val length = this.length
        //if (length.isAlmostZero()) return Vector3.ZERO
        if (length == 0f) return Vector3F.ZERO
        return this / length
    }

    // https://math.stackexchange.com/questions/13261/how-to-get-a-reflection-vector
    // 𝑟=𝑑−2(𝑑⋅𝑛)𝑛
    fun reflected(surfaceNormal: Vector3F): Vector3F {
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

    operator fun unaryPlus(): Vector3F = this
    operator fun unaryMinus(): Vector3F = Vector3F(-this.x, -this.y, -this.z)

    operator fun plus(v: Vector3F): Vector3F = Vector3F(this.x + v.x, this.y + v.y, this.z + v.z)
    operator fun minus(v: Vector3F): Vector3F = Vector3F(this.x - v.x, this.y - v.y, this.z - v.z)

    operator fun times(v: Vector3F): Vector3F = Vector3F(this.x * v.x, this.y * v.y, this.z * v.z)
    operator fun div(v: Vector3F): Vector3F = Vector3F(this.x / v.x, this.y / v.y, this.z / v.z)
    operator fun rem(v: Vector3F): Vector3F = Vector3F(this.x % v.x, this.y % v.y, this.z % v.z)

    operator fun times(v: Float): Vector3F = Vector3F(this.x * v, this.y * v, this.z * v)
    operator fun div(v: Float): Vector3F = Vector3F(this.x / v, this.y / v, this.z / v)
    operator fun rem(v: Float): Vector3F = Vector3F(this.x % v, this.y % v, this.z % v)

    operator fun times(v: Int): Vector3F = this * v.toFloat()
    operator fun div(v: Int): Vector3F = this / v.toFloat()
    operator fun rem(v: Int): Vector3F = this % v.toFloat()

    operator fun times(v: Double): Vector3F = this * v.toFloat()
    operator fun div(v: Double): Vector3F = this / v.toFloat()
    operator fun rem(v: Double): Vector3F = this % v.toFloat()

    infix fun dot(v: Vector3F): Float = (x * v.x) + (y * v.y) + (z * v.z)
    infix fun cross(v: Vector3F): Vector3F = cross(this, v)

    /** Vector3 with inverted (1f / v) components to this */
    fun inv(): Vector3F = Vector3F(1f / x, 1f / y, 1f / z)

    fun isNaN(): Boolean = this.x.isNaN() && this.y.isNaN() && this.z.isNaN()
    val absoluteValue: Vector3F get() = Vector3F(abs(x), abs(y), abs(z))

    override fun toString(): String = "Vector3(${x.niceStr}, ${y.niceStr}, ${z.niceStr})"

    fun toVector4(w: Float = 1f): Vector4F = Vector4F(x, y, z, w)
    override fun isAlmostEquals(other: Vector3F, epsilon: Float): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) &&
            this.y.isAlmostEquals(other.y, epsilon) &&
            this.z.isAlmostEquals(other.z, epsilon)
}

operator fun Int.times(v: Vector3F): Vector3F = v * this
operator fun Float.times(v: Vector3F): Vector3F = v * this
operator fun Double.times(v: Vector3F): Vector3F = v * this

fun abs(a: Vector3F): Vector3F = a.absoluteValue
fun min(a: Vector3F, b: Vector3F): Vector3F = Vector3F(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))
fun max(a: Vector3F, b: Vector3F): Vector3F = Vector3F(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))
fun Vector3F.clamp(min: Float, max: Float): Vector3F = Vector3F(x.clamp(min, max), y.clamp(min, max), z.clamp(min, max))
fun Vector3F.clamp(min: Double, max: Double): Vector3F = clamp(min.toFloat(), max.toFloat())
fun Vector3F.clamp(min: Vector3F, max: Vector3F): Vector3F = Vector3F(x.clamp(min.x, max.x), y.clamp(min.y, max.y), z.clamp(min.z, max.z))

data class Vector4F(val x: Float, val y: Float, val z: Float, val w: Float) {
    companion object {
        val ZERO = Vector4F(0f, 0f, 0f, 0f)
        val ONE = Vector4F(1f, 1f, 1f, 1f)

        operator fun invoke(): Vector4F = Vector4F.ZERO

        fun fromArray(array: FloatArray, offset: Int = 0): Vector4F = Vector4F(array[offset + 0], array[offset + 1], array[offset + 2], array[offset + 3])

        fun length(x: Float, y: Float, z: Float, w: Float): Float = sqrt(lengthSq(x, y, z, w))
        fun lengthSq(x: Float, y: Float, z: Float, w: Float): Float = x * x + y * y + z * z + w * w

        inline fun func(func: (index: Int) -> Float): Vector4F = Vector4F(func(0), func(1), func(2), func(3))
    }

    constructor(xyz: Vector3F, w: Float) : this(xyz.x, xyz.y, xyz.z, w)
    //constructor(x: Float, y: Float, z: Float, w: Float) : this(float4PackOf(x, y, z, w))
    constructor(x: Int, y: Int, z: Int, w: Int) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    constructor(x: Double, y: Double, z: Double, w: Double) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    val xyz: Vector3F get() = Vector3F(x, y, z)

    val length3Squared: Float get() = (x * x) + (y * y) + (z * z)
    /** Only taking into accoount x, y, z */
    val length3: Float get() = sqrt(length3Squared)

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z) + (w * w)
    val length: Float get() = sqrt(lengthSquared)

    fun normalized(): Vector4F {
        val length = this.length
        if (length == 0f) return Vector4F.ZERO
        return this / length
    }

    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> throw IndexOutOfBoundsException()
    }

    operator fun unaryPlus(): Vector4F = this
    operator fun unaryMinus(): Vector4F = Vector4F(-x, -y, -z, -w)

    operator fun plus(v: Vector4F): Vector4F = Vector4F(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: Vector4F): Vector4F = Vector4F(x - v.x, y - v.y, z - v.z, w - v.w)

    operator fun times(v: Vector4F): Vector4F = Vector4F(x * v.x, y * v.y, z * v.z, w * v.w)
    operator fun div(v: Vector4F): Vector4F = Vector4F(x / v.x, y / v.y, z / v.z, w / v.w)
    operator fun rem(v: Vector4F): Vector4F = Vector4F(x % v.x, y % v.y, z % v.z, w % v.w)

    operator fun times(v: Float): Vector4F = Vector4F(x * v, y * v, z * v, w * v)
    operator fun div(v: Float): Vector4F = Vector4F(x / v, y / v, z / v, w / v)
    operator fun rem(v: Float): Vector4F = Vector4F(x % v, y % v, z % v, w % v)

    infix fun dot(v: Vector4F): Float = (x * v.x) + (y * v.y) + (z * v.z) + (w * v.w)
    //infix fun cross(v: Vector4): Vector4 = cross(this, v)

    fun copyTo(out: FloatArray, offset: Int = 0): FloatArray {
        out[offset + 0] = x
        out[offset + 1] = y
        out[offset + 2] = z
        out[offset + 3] = w
        return out
    }

    /** Vector4 with inverted (1f / v) components to this */
    fun inv(): Vector4F = Vector4F(1f / x, 1f / y, 1f / z, 1f / w)

    fun isNaN(): Boolean = this.x.isNaN() && this.y.isNaN() && this.z.isNaN() && this.w.isNaN()
    val absoluteValue: Vector4F get() = Vector4F(abs(x), abs(y), abs(z), abs(w))

    override fun toString(): String = "Vector4(${x.niceStr}, ${y.niceStr}, ${z.niceStr}, ${w.niceStr})"

    // @TODO: Should we scale Vector3 by w?
    fun toVector3(): Vector3F = Vector3F(x, y, z)
    fun isAlmostEquals(other: Vector4F, epsilon: Float = 0.00001f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon) && this.z.isAlmostEquals(other.z, epsilon) && this.w.isAlmostEquals(other.w, epsilon)
}

fun abs(a: Vector4F): Vector4F = a.absoluteValue
fun min(a: Vector4F, b: Vector4F): Vector4F = Vector4F(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z), min(a.w, b.w))
fun max(a: Vector4F, b: Vector4F): Vector4F = Vector4F(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z), max(a.w, b.w))
fun Vector4F.clamp(min: Float, max: Float): Vector4F = Vector4F(x.clamp(min, max), y.clamp(min, max), z.clamp(min, max), w.clamp(min, max))
fun Vector4F.clamp(min: Double, max: Double): Vector4F = clamp(min.toFloat(), max.toFloat())
fun Vector4F.clamp(min: Vector4F, max: Vector4F): Vector4F = Vector4F(x.clamp(min.x, max.x), y.clamp(min.y, max.y), z.clamp(min.z, max.z), w.clamp(min.w, max.w))

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

data class CylindricalVector(
    val radius: Double = 1.0,
    val angle: Angle = Angle.ZERO,
    val y: Double = 0.0,
) {
    fun toVector3(): Vector3F = toCartesian(this).toFloat()

    companion object {
        fun fromCartesian(v: Vector3F): CylindricalVector = fromCartesian(v.x, v.y, v.z)
        fun fromCartesian(v: Vector3D): CylindricalVector = fromCartesian(v.x, v.y, v.z)
        inline fun fromCartesian(x: Number, y: Number, z: Number): CylindricalVector = fromCartesian(x.toDouble(), y.toDouble(), z.toDouble())
        fun fromCartesian(x: Double, y: Double, z: Double): CylindricalVector = CylindricalVector(
            radius = sqrt(x * x + z * z),
            angle = Angle.atan2(x, z),
            y = y,
        )

        fun toCartesian(c: CylindricalVector): Vector3D = toCartesian(c.radius, c.angle, c.y)
        fun toCartesian(radius: Double, angle: Angle, y: Double): Vector3D = Vector3D(
            x = radius * sin(angle),
            y = y,
            z = radius * cos(angle),
        )
    }
}

fun Vector3F.toCylindrical(): CylindricalVector = CylindricalVector.fromCartesian(this)
