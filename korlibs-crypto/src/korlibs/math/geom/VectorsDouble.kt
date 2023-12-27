package korlibs.math.geom

import korlibs.math.*
import korlibs.math.interpolation.*
import korlibs.number.*
import kotlin.math.*

typealias Point = Vector2D
typealias Point2 = Vector2D
typealias Point3 = Vector3D

data class Vector3D(val x: Double, val y: Double, val z: Double)
data class Vector4D(val x: Double, val y: Double, val z: Double, val w: Double)

fun Vector3F.toDouble(): Vector3D = Vector3D(x.toDouble(), y.toDouble(), z.toDouble())
fun Vector3D.toFloat(): Vector3F = Vector3F(x, y, z)

data class Vector2D(val x: Double, val y: Double) : IsAlmostEquals<Vector2D> {
    //constructor(x: Float, y: Float) : this(float2PackOf(x, y))
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    constructor(x: Double, y: Int) : this(x.toDouble(), y.toDouble())
    constructor(x: Int, y: Double) : this(x.toDouble(), y.toDouble())

    constructor(x: Float, y: Int) : this(x.toDouble(), y.toDouble())
    constructor(x: Int, y: Float) : this(x.toDouble(), y.toDouble())

    //constructor(p: Vector2) : this(p.raw)
    constructor() : this(0.0, 0.0)
    //constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
    //constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())

    fun copy(x: Float = this.x.toFloat(), y: Float = this.y.toFloat()): Vector2D = Vector2D(x, y)

    inline operator fun unaryMinus(): Vector2D = Vector2D(-x, -y)
    inline operator fun unaryPlus(): Vector2D = this

    inline operator fun plus(that: Size): Vector2D = Vector2D(x + that.width, y + that.height)
    inline operator fun minus(that: Size): Vector2D = Vector2D(x - that.width, y - that.height)

    inline operator fun plus(that: Vector2D): Vector2D = Vector2D(x + that.x, y + that.y)
    inline operator fun minus(that: Vector2D): Vector2D = Vector2D(x - that.x, y - that.y)
    inline operator fun times(that: Vector2D): Vector2D = Vector2D(x * that.x, y * that.y)
    inline operator fun times(that: Size): Vector2D = Vector2D(x * that.width, y * that.height)
    inline operator fun times(that: Scale): Vector2D = Vector2D(x * that.scaleX, y * that.scaleY)
    inline operator fun div(that: Vector2D): Vector2D = Vector2D(x / that.x, y / that.y)
    inline operator fun div(that: Size): Vector2D = Vector2D(x / that.width, y / that.height)
    inline operator fun rem(that: Vector2D): Vector2D = Vector2D(x % that.x, y % that.y)
    inline operator fun rem(that: Size): Vector2D = Vector2D(x % that.width, y % that.height)

    inline operator fun times(scale: Double): Vector2D = Vector2D(x * scale, y * scale)
    inline operator fun times(scale: Float): Vector2D = this * scale.toDouble()
    inline operator fun times(scale: Int): Vector2D = this * scale.toDouble()

    inline operator fun div(scale: Double): Vector2D = Vector2D(x / scale, y / scale)
    inline operator fun div(scale: Float): Vector2D = this / scale.toDouble()
    inline operator fun div(scale: Int): Vector2D = this / scale.toDouble()

    inline operator fun rem(scale: Double): Vector2D = Vector2D(x % scale, y % scale)
    inline operator fun rem(scale: Float): Vector2D = this % scale.toDouble()
    inline operator fun rem(scale: Int): Vector2D = this % scale.toDouble()

    fun avgComponent(): Double = x * 0.5 + y * 0.5
    fun minComponent(): Double = min(x, y)
    fun maxComponent(): Double = max(x, y)

    fun distanceTo(x: Double, y: Double): Double = hypot(x - this.x, y - this.y)
    fun distanceTo(x: Float, y: Float): Double = distanceTo(x.toDouble(), y.toDouble())
    fun distanceTo(x: Int, y: Int): Double = this.distanceTo(x.toDouble(), y.toDouble())
    fun distanceTo(that: Vector2D): Double = distanceTo(that.x, that.y)

    infix fun cross(that: Vector2D): Double = crossProduct(this, that)
    infix fun dot(that: Vector2D): Double = ((this.x * that.x) + (this.y * that.y))

    fun angleTo(other: Vector2D, up: Vector2D = UP): Angle = Angle.between(this.x, this.y, other.x, other.y, up)
    val angle: Angle get() = angle()
    fun angle(up: Vector2D = UP): Angle = Angle.between(0.0, 0.0, this.x, this.y, up)

    inline fun deltaTransformed(m: Matrix): Vector2D = m.deltaTransform(this)
    inline fun transformed(m: Matrix): Vector2D = m.transform(this)

    fun transformX(m: Matrix): Double = m.transform(this).x
    fun transformY(m: Matrix): Double = m.transform(this).y

    inline fun transformedNullable(m: Matrix?): Vector2D = if (m != null && m.isNotNIL) m.transform(this) else this
    fun transformNullableX(m: Matrix?): Double = if (m != null && m.isNotNIL) m.transform(this).x else x
    fun transformNullableY(m: Matrix?): Double = if (m != null && m.isNotNIL) m.transform(this).y else y

    operator fun get(component: Int): Double = when (component) {
        0 -> x; 1 -> y
        else -> throw IndexOutOfBoundsException("Point doesn't have $component component")
    }
    val length: Double get() = hypot(x, y)
    val lengthSquared: Double get() {
        val x = x
        val y = y
        return x*x + y*y
    }
    val magnitude: Double get() = hypot(x, y)
    val normalized: Vector2D get() = this * (1f / magnitude)
    val unit: Vector2D get() = this / length

    /** Normal vector. Rotates the vector/point -90 degrees (not normalizing it) */
    fun toNormal(): Vector2D = Vector2D(-this.y, this.x)


    val int: Vector2I get() = Vector2I(x.toInt(), y.toInt())
    val intRound: Vector2I get() = Vector2I(x.roundToInt(), y.roundToInt())

    fun roundDecimalPlaces(places: Int): Vector2D = Vector2D(x.roundDecimalPlaces(places), y.roundDecimalPlaces(places))
    fun round(): Vector2D = Vector2D(round(x), round(y))
    fun ceil(): Vector2D = Vector2D(ceil(x), ceil(y))
    fun floor(): Vector2D = Vector2D(floor(x), floor(y))

    //fun copy(x: Double = this.x, y: Double = this.y): Vector2 = Vector2D(x, y)

    override fun isAlmostEquals(other: Vector2D, epsilon: Double): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon)

    val niceStr: String get() = "(${x.niceStr}, ${y.niceStr})"
    fun niceStr(decimalPlaces: Int): String = "(${x.niceStr(decimalPlaces)}, ${y.niceStr(decimalPlaces)})"
    override fun toString(): String = niceStr

    fun reflected(normal: Vector2D): Vector2D {
        val d = this
        val n = normal
        return d - 2.0 * (d dot n) * n
    }

    /** Vector2 with inverted (1f / v) components to this */
    fun inv(): Vector2D = Vector2D(1.0 / x, 1.0 / y)

    fun isNaN(): Boolean = this.x.isNaN() && this.y.isNaN()

    val absoluteValue: Vector2D get() = Vector2D(abs(x), abs(y))

    @Deprecated("", ReplaceWith("ratio.interpolate(this, other)", "korlibs.math.interpolation.interpolate"))
    fun interpolateWith(ratio: Ratio, other: Vector2D): Vector2D = ratio.interpolate(this, other)

    companion object {
        val ZERO = Vector2D(0.0, 0.0)
        val NaN = Vector2D(Double.NaN, Double.NaN)

        /** Mathematically typical LEFT, matching screen coordinates (-1, 0) */
        val LEFT = Vector2D(-1.0, 0.0)
        /** Mathematically typical RIGHT, matching screen coordinates (+1, 0) */
        val RIGHT = Vector2D(+1.0, 0.0)

        /** Mathematically typical UP (0, +1) */
        val UP = Vector2D(0.0, +1.0)
        /** UP using screen coordinates as reference (0, -1) */
        val UP_SCREEN = Vector2D(0.0, -1.0)

        /** Mathematically typical DOWN (0, -1) */
        val DOWN = Vector2D(0.0, -1.0)
        /** DOWN using screen coordinates as reference (0, +1) */
        val DOWN_SCREEN = Vector2D(0.0, +1.0)


        inline operator fun invoke(x: Number, y: Number): Vector2D = Vector2D(x.toDouble(), y.toDouble())
        //inline operator fun invoke(x: Float, y: Float): Vector2D = Vector2D(x.toDouble(), y.toDouble())

        //fun fromRaw(raw: Float2Pack) = Vector2D(raw)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise for up=UP and clock-wise for up=UP_SCREEN */
        inline fun polar(x: Float, y: Float, angle: Angle, length: Float = 1f, up: Vector2D = UP): Vector2D = Vector2D(x + angle.cosine(up) * length, y + angle.sine(up) * length)
        inline fun polar(x: Double, y: Double, angle: Angle, length: Double = 1.0, up: Vector2D = UP): Vector2D = Vector2D(x + angle.cosine(up) * length, y + angle.sine(up) * length)
        inline fun polar(base: Vector2D, angle: Angle, length: Double = 1.0, up: Vector2D = UP): Vector2D = polar(base.x, base.y, angle, length, up)
        inline fun polar(angle: Angle, length: Double = 1.0, up: Vector2D = UP): Vector2D = polar(0.0, 0.0, angle, length, up)

        inline fun middle(a: Vector2D, b: Vector2D): Vector2D = (a + b) * 0.5

        fun angle(ax: Double, ay: Double, bx: Double, by: Double, up: Vector2D = UP): Angle = Angle.between(ax, ay, bx, by, up)
        fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, up: Vector2D = UP): Angle = Angle.between(x1 - x2, y1 - y2, x1 - x3, y1 - y3, up)

        fun angle(a: Vector2D, b: Vector2D, up: Vector2D = UP): Angle = Angle.between(a, b, up)
        fun angle(p1: Vector2D, p2: Vector2D, p3: Vector2D, up: Vector2D = UP): Angle = Angle.between(p1 - p2, p1 - p3, up)

        fun angleArc(a: Vector2D, b: Vector2D, up: Vector2D = UP): Angle = Angle.fromRadians(acos((a dot b) / (a.length * b.length))).adjustFromUp(up)
        fun angleFull(a: Vector2D, b: Vector2D, up: Vector2D = UP): Angle = Angle.between(a, b, up)

        fun distance(a: Double, b: Double): Double = abs(a - b)
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = hypot(x1 - x2, y1 - y2)
        fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double = hypot(x1 - x2, y1 - y2).toDouble()
        fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double = hypot(x1.toDouble() - x2.toDouble(), y1.toDouble() - y2.toDouble())
        fun distance(a: Vector2D, b: Vector2D): Double = distance(a.x, a.y, b.x, b.y)
        fun distance(a: Vector2I, b: Vector2I): Double = distance(a.x, a.y, b.x, b.y)

        fun distanceSquared(a: Vector2D, b: Vector2D): Double = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(a: Vector2I, b: Vector2I): Int = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(x1: Double, y1: Double, x2: Double, y2: Double): Double = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Float, y1: Float, x2: Float, y2: Float): Float = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Int, y1: Int, x2: Int, y2: Int): Int = square(x1 - x2) + square(y1 - y2)

        @Deprecated("Likely searching for orientation")
        inline fun direction(a: Vector2D, b: Vector2D): Vector2D = b - a

        fun compare(l: Vector2D, r: Vector2D): Int = compare(l.x, l.y, r.x, r.y)
        fun compare(lx: Float, ly: Float, rx: Float, ry: Float): Int = ly.compareTo(ry).let { ret -> if (ret == 0) lx.compareTo(rx) else ret }
        fun compare(lx: Double, ly: Double, rx: Double, ry: Double): Int = ly.compareTo(ry).let { ret -> if (ret == 0) lx.compareTo(rx) else ret }

        private fun square(x: Double): Double = x * x
        private fun square(x: Float): Float = x * x
        private fun square(x: Int): Int = x * x

        fun dot(aX: Double, aY: Double, bX: Double, bY: Double): Double = (aX * bX) + (aY * bY)
        fun dot(aX: Float, aY: Float, bX: Float, bY: Float): Float = (aX * bX) + (aY * bY)
        fun dot(a: Vector2D, b: Vector2D): Double = dot(a.x, a.y, b.x, b.y)

        fun isCollinear(p1: Point, p2: Point, p3: Point): Boolean =
            isCollinear(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)

        fun isCollinear(p1x: Double, p1y: Double, p2x: Double, p2y: Double, p3x: Double, p3y: Double): Boolean {
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

        fun isCollinear(xa: Float, ya: Float, x: Float, y: Float, xb: Float, yb: Float): Boolean = isCollinear(
            xa.toDouble(), ya.toDouble(),
            x.toDouble(), y.toDouble(),
            xb.toDouble(), yb.toDouble(),
        )

        fun isCollinear(xa: Int, ya: Int, x: Int, y: Int, xb: Int, yb: Int): Boolean = isCollinear(
            xa.toDouble(), ya.toDouble(),
            x.toDouble(), y.toDouble(),
            xb.toDouble(), yb.toDouble(),
        )

        // https://algorithmtutor.com/Computational-Geometry/Determining-if-two-consecutive-segments-turn-left-or-right/
        /** < 0 left, > 0 right, 0 collinear */
        fun orientation(p1: Vector2D, p2: Vector2D, p3: Vector2D, up: Vector2D = UP): Double = orientation(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, up)
        fun orientation(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, up: Vector2D = UP): Float {
            Orientation.checkValidUpVector(up)
            val res = crossProduct(cx - ax, cy - ay, bx - ax, by - ay)
            return if (up.y > 0f) res else -res
        }
        fun orientation(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, up: Vector2D = UP): Double {
            Orientation.checkValidUpVector(up)
            val res = crossProduct(cx - ax, cy - ay, bx - ax, by - ay)
            return if (up.y > 0f) res else -res
        }

        fun crossProduct(ax: Float, ay: Float, bx: Float, by: Float): Float = (ax * by) - (bx * ay)
        fun crossProduct(ax: Double, ay: Double, bx: Double, by: Double): Double = (ax * by) - (bx * ay)
        fun crossProduct(p1: Vector2D, p2: Vector2D): Double = crossProduct(p1.x, p1.y, p2.x, p2.y)

        fun minComponents(p1: Vector2D, p2: Vector2D): Vector2D = Vector2D(min(p1.x, p2.x), min(p1.y, p2.y))
        fun minComponents(p1: Vector2D, p2: Vector2D, p3: Vector2D): Vector2D = Vector2D(
            korlibs.math.min(p1.x, p2.x, p3.x),
            korlibs.math.min(p1.y, p2.y, p3.y)
        )
        fun minComponents(p1: Vector2D, p2: Vector2D, p3: Vector2D, p4: Vector2D): Vector2D = Vector2D(
            korlibs.math.min(
                p1.x,
                p2.x,
                p3.x,
                p4.x
            ), korlibs.math.min(p1.y, p2.y, p3.y, p4.y)
        )
        fun maxComponents(p1: Vector2D, p2: Vector2D): Vector2D = Vector2D(max(p1.x, p2.x), max(p1.y, p2.y))
        fun maxComponents(p1: Vector2D, p2: Vector2D, p3: Vector2D): Vector2D = Vector2D(
            korlibs.math.max(p1.x, p2.x, p3.x),
            korlibs.math.max(p1.y, p2.y, p3.y)
        )
        fun maxComponents(p1: Vector2D, p2: Vector2D, p3: Vector2D, p4: Vector2D): Vector2D = Vector2D(
            korlibs.math.max(
                p1.x,
                p2.x,
                p3.x,
                p4.x
            ), korlibs.math.max(p1.y, p2.y, p3.y, p4.y)
        )
    }
}

operator fun Int.times(v: Vector2D): Vector2D = v * this
operator fun Float.times(v: Vector2D): Vector2D = v * this
operator fun Double.times(v: Vector2D): Vector2D = v * this

fun Vector2D.toFloat(): Vector2F = Vector2F(x, y)
fun Vector2F.toDouble(): Vector2D = Vector2D(x, y)

fun abs(a: Vector2D): Vector2D = a.absoluteValue
fun min(a: Vector2D, b: Vector2D): Vector2D = Vector2D(min(a.x, b.x), min(a.y, b.y))
fun max(a: Vector2D, b: Vector2D): Vector2D = Vector2D(max(a.x, b.x), max(a.y, b.y))
fun Vector2D.clamp(min: Float, max: Float): Vector2D = clamp(min.toDouble(), max.toDouble())
fun Vector2D.clamp(min: Double, max: Double): Vector2D = Vector2D(x.clamp(min, max), y.clamp(min, max))
fun Vector2D.clamp(min: Vector2D, max: Vector2D): Vector2D = Vector2D(x.clamp(min.x, max.x), y.clamp(min.y, max.y))

fun Vector2D.toInt(): Vector2I = Vector2I(x.toInt(), y.toInt())
fun Vector2D.toIntCeil(): Vector2I = Vector2I(x.toIntCeil(), y.toIntCeil())
fun Vector2D.toIntRound(): Vector2I = Vector2I(x.toIntRound(), y.toIntRound())
fun Vector2D.toIntFloor(): Vector2I = Vector2I(x.toIntFloor(), y.toIntFloor())

fun Vector3D.toCylindrical(): CylindricalVector = CylindricalVector.fromCartesian(this)
