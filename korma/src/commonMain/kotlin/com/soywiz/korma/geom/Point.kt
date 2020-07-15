@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korma.geom

import com.soywiz.korma.internal.*
import com.soywiz.korma.interpolation.*
import kotlin.math.*

//@Deprecated("Use Point instead")
interface IPoint {
    val _x: Double
    val _y: Double

    companion object {
        //@Deprecated("Use Point instead")
        operator fun invoke(): IPoint = Point(0.0, 0.0)
        @Deprecated("Kotlin/Native boxes inline + Number")
        inline operator fun invoke(x: Number, y: Number): IPoint = Point(x.toDouble(), y.toDouble())
    }
}

//@Deprecated("Use Point instead")
val IPoint.x: Double get() = _x
//@Deprecated("Use Point instead")
val IPoint.y: Double get() = _y

@Deprecated("Kotlin/Native boxes inline + Number")
operator fun Point.Companion.invoke(v: IPoint): Point = Point(v._x, v._y)
//@Deprecated("Use Point instead")
fun Point.Companion.middle(a: IPoint, b: IPoint): Point = Point((a._x + b._x) * 0.5, (a._y + b._y) * 0.5)
//@Deprecated("Use Point instead")
fun Point.Companion.angle(a: IPoint, b: IPoint): Angle = Angle.fromRadians(acos((a.dot(b)) / (a.length * b.length)))
//@Deprecated("Use Point instead")
fun Point.Companion.compare(l: IPoint, r: IPoint): Int = Point.compare(l._x, l._y, r._x, r._y)
//@Deprecated("Use Point instead")
fun Point.Companion.distance(a: IPoint, b: IPoint): Double = Point.distance(a._x, a._y, b._x, b._y)
//@Deprecated("Use Point instead")
fun Point.copyFrom(that: IPoint) = setTo(that._x, that._y)
//@Deprecated("Use Point instead")
fun Point.add(p: IPoint) = this.setToAdd(this, p)
//@Deprecated("Use Point instead")
fun Point.sub(p: IPoint) = this.setToSub(this, p)
// @TODO: mul instead of dot
//@Deprecated("Use Point instead")
operator fun IPoint.plus(that: IPoint): IPoint = IPoint(_x + that._x, _y + that._y)
//@Deprecated("Use Point instead")
operator fun IPoint.minus(that: IPoint): IPoint = IPoint(_x - that._x, _y - that._y)
//@Deprecated("Use Point instead")
operator fun IPoint.times(that: IPoint): IPoint = IPoint(_x * that._x, _y * that._y)
//@Deprecated("Use Point instead")
operator fun IPoint.div(that: IPoint): IPoint = IPoint(_x / that._x, _y / that._y)
//@Deprecated("Use Point instead")
inline operator fun IPoint.times(scale: Number): IPoint = IPoint(_x * scale.toDouble(), _y * scale.toDouble())
//@Deprecated("Use Point instead")
inline operator fun IPoint.div(scale: Number): IPoint = IPoint(_x / scale.toDouble(), _y / scale.toDouble())
//@Deprecated("Use Point instead")
infix fun IPoint.dot(that: IPoint): Double = this._x * that._x + this._y * that._y
//@Deprecated("Use Point instead")
inline fun IPoint.distanceTo(x: Number, y: Number): Double = hypot(x.toDouble() - this._x, y.toDouble() - this._y)
//@Deprecated("Use Point instead")
fun IPoint.distanceTo(that: IPoint): Double = distanceTo(that._x, that._y)
//@Deprecated("Use Point instead")
fun IPoint.angleTo(other: IPoint): Angle = Angle.between(this._x, this._y, other._x, other._y)
//@Deprecated("Use Point instead")
fun IPoint.transformed(mat: IMatrix, out: Point = Point()): Point = out.setToTransform(mat, this)
//@Deprecated("Use Point instead")
operator fun IPoint.get(index: Int) = when (index) {
    0 -> _x; 1 -> _y
    else -> throw IndexOutOfBoundsException("IPoint doesn't have $index component")
}
//@Deprecated("Use Point instead")
val IPoint.unit: IPoint get() = this / this.length
//@Deprecated("Use Point instead")
val IPoint.length: Double get() = hypot(_x, _y)
//@Deprecated("Use Point instead")
val IPoint.magnitude: Double get() = hypot(_x, _y)
//@Deprecated("Use Point instead")
val IPoint.normalized: IPoint
    get() {
        val imag = 1.0 / magnitude
        return IPoint(_x * imag, _y * imag)
    }
//@Deprecated("Use Point instead")
val IPoint.mutable: Point get() = Point(_x, _y)
//@Deprecated("Use Point instead")
val IPoint.immutable: IPoint get() = IPoint(_x, _y)
//@Deprecated("Use Point instead")
fun IPoint.copy() = IPoint(_x, _y)
//@Deprecated("Use Point instead")
fun Point.setToTransform(mat: IMatrix, p: IPoint): Point = setToTransform(mat, p._x, p._y)
//@Deprecated("Use Point instead")
fun Point.setToTransform(mat: IMatrix, x: Double, y: Double): Point = setTo(mat.transformX(x, y), mat.transformY(x, y))
//@Deprecated("Use Point instead")
fun Point.setToAdd(a: IPoint, b: IPoint): Point = setTo(a._x + b._x, a._y + b._y)
//@Deprecated("Use Point instead")
fun Point.setToSub(a: IPoint, b: IPoint): Point = setTo(a._x - b._x, a._y - b._y)
//@Deprecated("Use Point instead")
fun Point.setToMul(a: IPoint, b: IPoint): Point = setTo(a._x * b._x, a._y * b._y)
//@Deprecated("Use Point instead")
fun Point.setToMul(a: IPoint, s: Double): Point = setTo(a._x * s, a._y * s)
//@Deprecated("Use Point instead")
inline fun Point.setToMul(a: IPoint, s: Number): Point = setToMul(a, s.toDouble())
//@Deprecated("Use Point instead")
fun Point.setToDiv(a: IPoint, b: IPoint): Point = setTo(a._x / b._x, a._y / b._y)
//@Deprecated("Use Point instead")
fun Point.setToDiv(a: IPoint, s: Double): Point = setTo(a._x / s, a._y / s)
//@Deprecated("Use Point instead")
inline fun Point.setToDiv(a: IPoint, s: Number): Point = setToDiv(a, s.toDouble())
//@Deprecated("Use Point instead")
operator fun Point.plusAssign(that: IPoint): Unit = run { setTo(this.x + that._x, this.y + that._y) }

data class Point(
    var x: Double,
    var y: Double
) : MutableInterpolable<Point>, Interpolable<Point>, Comparable<IPoint>, IPoint {
    override val _x: Double get() = x
    override val _y: Double get() = y

    @Deprecated("Kotlin/Native boxes inline + Number")
    override fun compareTo(other: IPoint): Int = compare(this.x, this.y, other._x, other._y)
    fun compareTo(other: Point): Int = compare(this.x, this.y, other.x, other.y)

    fun setToZero() = setTo(0.0, 0.0)
    fun setToOne() = setTo(1.0, 1.0)
    fun setToUp() = setTo(0.0, -1.0)
    fun setToDown() = setTo(0.0, +1.0)
    fun setToLeft() = setTo(-1.0, 0.0)
    fun setToRight() = setTo(+1.0, 0.0)

    companion object {
        @Deprecated("")
        val Zero: IPoint = Point(0.0, 0.0)
        @Deprecated("")
        val One: IPoint = Point(1.0, 1.0)

        @Deprecated("")
        val Up: IPoint = Point(0.0, +1.0)
        @Deprecated("")
        val Down: IPoint = Point(0.0, -1.0)
        @Deprecated("")
        val Left: IPoint = Point(-1.0, 0.0)
        @Deprecated("")
        val Right: IPoint = Point(+1.0, 0.0)

        //inline operator fun invoke(): Point = Point(0.0, 0.0) // @TODO: // e: java.lang.NullPointerException at org.jetbrains.kotlin.com.google.gwt.dev.js.JsAstMapper.mapFunction(JsAstMapper.java:562) (val pt = Array(1) { Point() })
        operator fun invoke(): Point = Point(0.0, 0.0)
        operator fun invoke(v: Point): Point = Point(v.x, v.y)

        inline operator fun invoke(x: Float, y: Float): Point = Point(x.toDouble(), y.toDouble())
        inline operator fun invoke(x: Int, y: Int): Point = Point(x.toDouble(), y.toDouble())
        inline operator fun invoke(xy: Int): Point = Point(xy.toDouble(), xy.toDouble())
        inline operator fun invoke(xy: Float): Point = Point(xy.toDouble(), xy.toDouble())
        inline operator fun invoke(xy: Double): Point = Point(xy, xy)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        inline operator fun invoke(angle: Angle, length: Double = 1.0): Point = fromPolar(angle, length)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        fun fromPolar(angle: Angle, length: Double = 1.0): Point = Point(angle.cosine * length, angle.sine * length)

        @Deprecated("Kotlin/Native boxes inline + Number")
        inline operator fun invoke(x: Number, y: Number): Point = Point(x.toDouble(), y.toDouble())
        @Deprecated("Kotlin/Native boxes inline + Number")
        inline operator fun invoke(xy: Number): Point = Point(xy.toDouble(), xy.toDouble())

        fun middle(a: Point, b: Point): Point = Point((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)
        fun angle(a: Point, b: Point): Angle = Angle.fromRadians(acos((a.dot(b)) / (a.length * b.length)))

        fun angle(ax: Double, ay: Double, bx: Double, by: Double): Angle = Angle.between(ax, ay, bx, by)
            //acos(((ax * bx) + (ay * by)) / (hypot(ax, ay) * hypot(bx, by)))

        fun compare(lx: Double, ly: Double, rx: Double, ry: Double): Int {
            val ret = ly.compareTo(ry)
            return if (ret == 0) lx.compareTo(rx) else ret
        }

        fun compare(l: Point, r: Point): Int = compare(l.x, l.y, r.x, r.y)

        fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Angle = Angle.between(x1 - x2, y1 - y2, x1 - x3, y1 - y3)

        fun distance(a: Double, b: Double): Double = kotlin.math.abs(a - b)
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = kotlin.math.hypot(x1 - x2, y1 - y2)

        inline fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double = distance(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
        inline fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double = distance(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
        @Deprecated("Kotlin/Native boxes inline + Number")
        inline fun distance(x1: Number, y1: Number, x2: Number, y2: Number): Double = distance(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())

        fun distance(a: Point, b: Point): Double = distance(a.x, a.y, b.x, b.y)
        fun distance(a: IPointInt, b: IPointInt): Double = distance(a.x, a.y, b.x, b.y)

        //val ax = x1 - x2
        //val ay = y1 - y2
        //val al = hypot(ax, ay)
        //val bx = x1 - x3
        //val by = y1 - y3
        //val bl = hypot(bx, by)
        //return acos((ax * bx + ay * by) / (al * bl))
    }

    fun setTo(x: Double, y: Double): Point {
        this.x = x
        this.y = y
        return this
    }

    /** Updates a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
    fun setToPolar(angle: Angle, length: Double = 1.0): Point = setTo(angle.cosine * length, angle.sine * length)

    fun neg() = setTo(-x, -y)
    fun mul(s: Double) = setTo(x * s, y * s)
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun mul(s: Number) = mul(s.toDouble())
    fun mul(s: Float) = mul(s.toDouble())
    fun mul(s: Int) = mul(s.toDouble())

    fun add(p: Point) = this.setToAdd(this, p)
    fun sub(p: Point) = this.setToSub(this, p)

    fun copyFrom(that: Point) = setTo(that.x, that.y)

    fun setToTransform(mat: Matrix, p: Point): Point = setToTransform(mat, p.x, p.y)
    fun setToTransform(mat: Matrix, x: Double, y: Double): Point = setTo(mat.transformX(x, y), mat.transformY(x, y))
    fun setToAdd(a: Point, b: Point): Point = setTo(a.x + b.x, a.y + b.y)
    fun setToSub(a: Point, b: Point): Point = setTo(a.x - b.x, a.y - b.y)
    fun setToMul(a: Point, b: Point): Point = setTo(a.x * b.x, a.y * b.y)
    fun setToMul(a: Point, s: Double): Point = setTo(a.x * s, a.y * s)
    fun setToDiv(a: Point, b: Point): Point = setTo(a.x / b.x, a.y / b.y)
    fun setToDiv(a: Point, s: Double): Point = setTo(a.x / s, a.y / s)
    operator fun plusAssign(that: Point): Unit = run { setTo(this.x + that.x, this.y + that.y) }

    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun setToMul(a: Point, s: Number): Point = setToMul(a, s.toDouble())
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun setToDiv(a: Point, s: Number): Point = setToDiv(a, s.toDouble())

    operator fun plus(that: Point): Point = Point(_x + that._x, _y + that._y)
    operator fun minus(that: Point): Point = Point(_x - that._x, _y - that._y)
    operator fun times(that: Point): Point = Point(_x * that._x, _y * that._y)
    operator fun div(that: Point): Point = Point(_x / that._x, _y / that._y)
    infix fun dot(that: Point): Double = this._x * that._x + this._y * that._y

    operator fun times(scale: Double): Point = Point(_x * scale.toDouble(), _y * scale.toDouble())
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline operator fun times(scale: Number): Point = this * scale.toDouble()
    operator fun times(scale: Float): Point = this * scale.toDouble()
    operator fun times(scale: Int): Point = this * scale.toDouble()

    operator fun div(scale: Double): Point = Point(_x / scale.toDouble(), _y / scale.toDouble())
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline operator fun div(scale: Number): Point = this / scale.toDouble()
    operator fun div(scale: Float): Point = this / scale.toDouble()
    operator fun div(scale: Int): Point = this / scale.toDouble()

    fun distanceTo(x: Double, y: Double): Double = hypot(x.toDouble() - this._x, y.toDouble() - this._y)
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun distanceTo(x: Number, y: Number): Double = hypot(x.toDouble() - this._x, y.toDouble() - this._y)
    fun distanceTo(x: Int, y: Int): Double = distanceTo(x.toDouble(), y.toDouble())
    fun distanceTo(x: Float, y: Float): Float = distanceTo(x.toDouble(), y.toDouble()).toFloat()

    fun distanceTo(that: Point): Double = distanceTo(that._x, that._y)
    fun angleTo(other: Point): Angle = Angle.between(this._x, this._y, other._x, other._y)
    fun transformed(mat: Matrix, out: Point = Point()): Point = out.setToTransform(mat, this)
    operator fun get(index: Int) = when (index) {
        0 -> x; 1 -> y
        else -> throw IndexOutOfBoundsException("IPoint doesn't have $index component")
    }
    val mutable: Point get() = Point(_x, _y)
    val immutable: Point get() = Point(_x, _y)
    fun copy() = Point(_x, _y)


    val unit: Point get() = this / this.length
    val length: Double get() = hypot(_x, _y)
    val magnitude: Double get() = hypot(_x, _y)
    val normalized: Point
        get() {
            val imag = 1.0 / magnitude
            return Point(_x * imag, _y * imag)
        }

    fun normalize() {
        val len = this.length
        this.setTo(this.x / len, this.y / len)
    }

    override fun interpolateWith(ratio: Double, other: Point): Point =
        Point().setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Double, l: Point, r: Point): Point =
        this.setTo(ratio.interpolate(l.x, r.x), ratio.interpolate(l.y, r.y))

    override fun toString(): String = "(${x.niceStr}, ${y.niceStr})"
}


val Point.unit: IPoint get() = this / length

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

        fun compare(lx: Int, ly: Int, rx: Int, ry: Int): Int {
            val ret = ly.compareTo(ry)
            return if (ret == 0) lx.compareTo(rx) else ret
        }
    }
    override var x: Int
        set(value) = run { p.x = value.toDouble() }
        get() = p.x.toInt()
    override var y: Int
        set(value) = run { p.y = value.toDouble() }
        get() = p.y.toInt()
    fun setTo(x: Int, y: Int) = this.apply { this.x = x; this.y = y }
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

val Point.int get() = PointInt(x.toInt(), y.toInt())
//@Deprecated("Use Point instead")
val IPoint.int get() = PointInt(_x.toInt(), _y.toInt())
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

//@Deprecated("Use Point instead")
fun Iterable<IPoint>.getPolylineLength(): Double {
    var out = 0.0
    var prev: IPoint? = null
    for (cur in this) {
        if (prev != null) out += prev.distanceTo(cur)
        prev = cur
    }
    return out
}
//@Deprecated("Use Point instead")
fun Iterable<IPoint>.bounds(out: Rectangle = Rectangle(), bb: BoundsBuilder = BoundsBuilder()): Rectangle = bb.add(this).getBounds(out)
