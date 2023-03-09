package com.soywiz.korma.geom

import com.soywiz.kds.*
import com.soywiz.korma.annotations.*
import com.soywiz.korma.internal.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.*
import kotlin.math.*

//////////////////////////////
// MUTABLE IMPLEMENTATIONS
//////////////////////////////

@KormaMutableApi
@Deprecated("Use Point instead")
data class MPoint(
    var x: Double,
    var y: Double
    //override var xf: Float,
    //override var yf: Float
) : MutableInterpolable<MPoint>, Interpolable<MPoint>, Comparable<MPoint> {
    //constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
    constructor(p: Point) : this(p.xD, p.yD)
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    val point: Point get() = Point(x, y)

    val niceStr: String get() = "(${x.niceStr}, ${y.niceStr})"
    fun niceStr(decimalPlaces: Int): String = "(${x.niceStr(decimalPlaces)}, ${y.niceStr(decimalPlaces)})"

    val angle: Angle get() = Angle.between(0.0, 0.0, this.x, this.y)
    fun transformX(m: MMatrix?): Double = m?.transformX(this) ?: x
    fun transformY(m: MMatrix?): Double = m?.transformY(this) ?: y
    val mutable: MPoint get() = MPoint(x, y)
    val immutable: MPoint get() = MPoint(x, y)
    fun isAlmostEquals(other: MPoint, epsilon: Double = 0.000001): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon)

    fun clear() = setToZero()
    fun setToZero() = setTo(0.0, 0.0)
    fun setToOne() = setTo(1.0, 1.0)
    fun setToUp() = setTo(0.0, -1.0)
    fun setToDown() = setTo(0.0, +1.0)
    fun setToLeft() = setTo(-1.0, 0.0)
    fun setToRight() = setTo(+1.0, 0.0)

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

    fun setTo(p: Point): MPoint = setTo(p.x, p.y)

    /** Updates a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
    fun setToPolar(angle: Angle, length: Double = 1.0): MPoint = setToPolar(0.0, 0.0, angle, length)
    fun setToPolar(base: Point, angle: Angle, length: Float = 1f): MPoint = setToPolar(base.x, base.y, angle, length)
    fun setToPolar(base: MPoint, angle: Angle, length: Double = 1.0): MPoint = setToPolar(base.x, base.y, angle, length)
    fun setToPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0): MPoint = setTo(x + angle.cosineD * length, y + angle.sineD * length)
    fun setToPolar(x: Float, y: Float, angle: Angle, length: Float = 1f): MPoint = setTo(x + angle.cosineF * length, y + angle.sineF * length)

    /** Rotates the vector/point -90 degrees (not normalizing it) */
    fun setToNormal(): MPoint = setTo(-this.y, this.x)
    fun neg() = setTo(-this.x, -this.y)
    fun mul(s: Double) = setTo(this.x * s, this.y * s)
    fun mul(s: Float) = mul(s.toDouble())
    fun mul(s: Int) = mul(s.toDouble())

    fun add(p: Point) = this.setTo(x + p.x, y + p.y)
    fun add(p: MPoint) = this.setToAdd(this, p)
    fun add(x: Double, y: Double): MPoint = this.setTo(this.x + x, this.y + y)

    fun sub(p: Point) = this.setTo(x - p.x, y - p.y)
    fun sub(p: MPoint) = this.setToSub(this, p)
    fun sub(x: Double, y: Double): MPoint = this.setTo(this.x - x, this.y - y)

    fun copyFrom(that: Point) = setTo(that.x, that.y)
    fun copyFrom(that: MPoint) = setTo(that.x, that.y)

    fun setToTransform(mat: MMatrix, p: MPoint): MPoint = setToTransform(mat, p.x, p.y)
    fun setToTransform(mat: MMatrix, x: Double, y: Double): MPoint = setTo(mat.transformX(x, y), mat.transformY(x, y))

    fun setToAdd(a: MPoint, b: MPoint): MPoint = setTo(a.x + b.x, a.y + b.y)
    fun setToSub(a: MPoint, b: MPoint): MPoint = setTo(a.x - b.x, a.y - b.y)
    fun setToMul(a: MPoint, b: MPoint): MPoint = setTo(a.x * b.x, a.y * b.y)
    fun setToMul(a: MPoint, s: Double): MPoint = setTo(a.x * s, a.y * s)
    fun setToMul(a: MPoint, s: Float): MPoint = setToMul(a, s.toDouble())
    fun setToDiv(a: MPoint, b: MPoint): MPoint = setTo(a.x / b.x, a.y / b.y)
    fun setToDiv(a: MPoint, s: Double): MPoint = setTo(a.x / s, a.y / s)
    fun setToDiv(a: MPoint, s: Float): MPoint = setToDiv(a, s.toDouble())

    operator fun plusAssign(that: MPoint) { setTo(this.x + that.x, this.y + that.y) }
    operator fun minusAssign(that: MPoint) { setTo(this.x - that.x, this.y - that.y) }
    operator fun remAssign(that: MPoint) { setTo(this.x % that.x, this.y % that.y) }
    operator fun remAssign(scale: Double) { setTo(this.x % scale, this.y % scale) }
    operator fun divAssign(that: MPoint) { setTo(this.x / that.x, this.y / that.y) }
    operator fun divAssign(scale: Double) { setTo(this.x / scale, this.y / scale) }
    operator fun timesAssign(that: MPoint) { setTo(this.x * that.x, this.y * that.y) }
    operator fun timesAssign(scale: Double) { setTo(this.x * scale, this.y * scale) }

    @Deprecated("allocates") operator fun plus(that: MPoint): MPoint = MPoint(this.x + that.x, this.y + that.y)
    @Deprecated("allocates") operator fun minus(that: MPoint): MPoint = MPoint(this.x - that.x, this.y - that.y)
    @Deprecated("allocates") operator fun times(that: MPoint): MPoint = MPoint(this.x * that.x, this.y * that.y)
    @Deprecated("allocates") operator fun div(that: MPoint): MPoint = MPoint(this.x / that.x, this.y / that.y)
    @Deprecated("allocates") infix fun dot(that: MPoint): Double = this.x * that.x + this.y * that.y

    @Deprecated("allocates") operator fun times(scale: Double): MPoint = MPoint(this.x * scale, this.y * scale)
    @Deprecated("allocates") operator fun times(scale: Float): MPoint = this * scale.toDouble()
    @Deprecated("allocates") operator fun times(scale: Int): MPoint = this * scale.toDouble()

    @Deprecated("allocates") operator fun div(scale: Double): MPoint = MPoint(this.x / scale, this.y / scale)
    @Deprecated("allocates") operator fun div(scale: Float): MPoint = this / scale.toDouble()
    @Deprecated("allocates") operator fun div(scale: Int): MPoint = this / scale.toDouble()

    fun distanceTo(x: Double, y: Double): Double = hypot(x - this.x, y - this.y)
    fun distanceTo(x: Int, y: Int): Double = distanceTo(x.toDouble(), y.toDouble())
    fun distanceTo(x: Float, y: Float): Float = distanceTo(x.toDouble(), y.toDouble()).toFloat()
    fun distanceTo(that: MPoint): Double = distanceTo(that.x, that.y)

    fun angleTo(other: MPoint): Angle = Angle.between(this.x, this.y, other.x, other.y)
    fun angleTo(other: Point): Angle = Angle.between(this.x, this.y, other.xD, other.yD)

    fun transformed(mat: MMatrix, out: MPoint = MPoint()): MPoint = out.setToTransform(mat, this)
    operator fun get(index: Int): Double = when (index) {
        0 -> this.x; 1 -> this.y
        else -> throw IndexOutOfBoundsException("IPoint doesn't have $index component")
    }
    fun copy() = MPoint(this.x, this.y)

    @Deprecated("Allocates") val unit: MPoint get() = this / length
    val squaredLength: Double get() = (x * x) + (y * y)
    val length: Double get() = hypot(this.x, this.y)
    val magnitude: Double get() = hypot(this.x, this.y)
    @Deprecated("Allocates") val normalized: MPoint
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

    @Deprecated("Allocates") override fun interpolateWith(ratio: Ratio, other: MPoint): MPoint =
        MPoint().setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Ratio, l: MPoint, r: MPoint): MPoint = setToInterpolated(ratio, l.x, l.y, r.x, r.y)

    fun setToInterpolated(ratio: Ratio, lx: Double, ly: Double, rx: Double, ry: Double): MPoint =
        this.setTo(ratio.interpolate(lx, rx), ratio.interpolate(ly, ry))

    override fun compareTo(other: MPoint): Int = compare(this.x, this.y, other.x, other.y)

    fun rotate(rotation: Angle, out: MPoint = MPoint()): MPoint =
        out.setToPolar(Angle.between(0.0, 0.0, this.x, this.y) + rotation, this.length)

    override fun toString(): String = "(${this.x.niceStr}, ${this.y.niceStr})"

    @Deprecated("")
    companion object {
        @Deprecated("")
        val POOL: ConcurrentPool<MPoint> = ConcurrentPool<MPoint>({ it.setTo(0.0, 0.0) }) { MPoint() }

        @Deprecated("")
        val Zero: MPoint = MPoint(0.0, 0.0)
        @Deprecated("")
        val One: MPoint = MPoint(1.0, 1.0)
        @Deprecated("")
        val Up: MPoint = MPoint(0.0, -1.0)
        @Deprecated("")
        val Down: MPoint = MPoint(0.0, +1.0)
        @Deprecated("")
        val Left: MPoint = MPoint(-1.0, 0.0)
        @Deprecated("")
        val Right: MPoint = MPoint(+1.0, 0.0)

        //inline operator fun invoke(): Point = Point(0.0, 0.0) // @TODO: // e: java.lang.NullPointerException at org.jetbrains.kotlin.com.google.gwt.dev.js.JsAstMapper.mapFunction(JsAstMapper.java:562) (val pt = Array(1) { Point() })
        operator fun invoke(): MPoint = MPoint(0.0, 0.0)
        operator fun invoke(v: MPoint): MPoint = MPoint(v.x, v.y)
        operator fun invoke(x: Double, y: Double): MPoint = MPoint(x, y)
        operator fun invoke(x: Float, y: Float): MPoint = MPoint(x, y)
        operator fun invoke(x: Int, y: Int): MPoint = MPoint(x, y)
        operator fun invoke(xy: Int): MPoint = MPoint(xy.toDouble(), xy.toDouble())
        operator fun invoke(xy: Float): MPoint = MPoint(xy.toDouble(), xy.toDouble())
        operator fun invoke(xy: Double): MPoint = MPoint(xy, xy)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        inline operator fun invoke(angle: Angle, length: Double = 1.0): MPoint = fromPolar(angle, length)

        fun angleArc(a: Point, b: Point): Angle = Angle.fromRadians(acos((a.dot(b)) / (a.length * b.length)))
        fun angleArc(a: MPoint, b: MPoint): Angle = Angle.fromRadians(acos((a.dot(b)) / (a.length * b.length)))
        fun angleFull(a: MPoint, b: MPoint): Angle = Angle.between(a, b)

        fun middle(a: MPoint, b: MPoint): MPoint = MPoint((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        fun fromPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0, out: MPoint = MPoint()): MPoint = out.setTo(x + angle.cosineD * length, y + angle.sineD * length)
        fun fromPolar(angle: Angle, length: Double = 1.0, out: MPoint = MPoint()): MPoint = fromPolar(0.0, 0.0, angle, length, out)
        fun fromPolar(base: MPoint, angle: Angle, length: Double = 1.0, out: MPoint = MPoint()): MPoint = fromPolar(base.x, base.y, angle, length, out)

        fun direction(a: MPoint, b: MPoint, out: MPoint = MPoint()): MPoint = out.setTo(b.x - a.x, b.y - a.y)
        fun middle(a: MPoint, b: MPoint, out: MPoint = MPoint()): MPoint = out.setTo((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)

        fun angle(ax: Double, ay: Double, bx: Double, by: Double): Angle = Angle.between(ax, ay, bx, by)
        //acos(((ax * bx) + (ay * by)) / (hypot(ax, ay) * hypot(bx, by)))

        fun compare(l: MPoint, r: MPoint): Int = compare(l.x, l.y, r.x, r.y)
        fun compare(lx: Double, ly: Double, rx: Double, ry: Double): Int {
            val ret = ly.compareTo(ry)
            return if (ret == 0) lx.compareTo(rx) else ret
        }

        fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Angle = Angle.between(x1 - x2, y1 - y2, x1 - x3, y1 - y3)

        private fun square(x: Double) = x * x
        private fun square(x: Int) = x * x

        fun distanceSquared(x1: Double, y1: Double, x2: Double, y2: Double): Double = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Int, y1: Int, x2: Int, y2: Int): Int = square(x1 - x2) + square(y1 - y2)

        fun distance(a: MPoint, b: MPoint): Double = distance(a.x, a.y, b.x, b.y)
        fun distance(a: MPointInt, b: MPointInt): Double = distance(a.x, a.y, b.x, b.y)
        fun distance(a: Double, b: Double): Double = kotlin.math.abs(a - b)
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = kotlin.math.hypot(x1 - x2, y1 - y2)
        fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double = distance(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
        fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double = distance(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())

        fun distanceSquared(a: MPoint, b: MPoint): Double = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(a: MPointInt, b: MPointInt): Int = distanceSquared(a.x, a.y, b.x, b.y)

        fun dot(aX: Double, aY: Double, bX: Double, bY: Double): Double = (aX * bX) + (aY * bY)
        fun dot(a: MPoint, b: MPoint): Double = dot(a.x, a.y, b.x, b.y)
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
        fun orientation(p1: MPoint, p2: MPoint, p3: MPoint): Double =
            orientation(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
        fun orientation(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double =
            crossProduct(cx - ax, cy - ay, bx - ax, by - ay)

        fun crossProduct(ax: Double, ay: Double, bx: Double, by: Double): Double = (ax * by) - (bx * ay)
        fun crossProduct(p1: MPoint, p2: MPoint): Double = crossProduct(p1.x, p1.y, p2.x, p2.y)

        //val ax = x1 - x2
        //val ay = y1 - y2
        //val al = hypot(ax, ay)
        //val bx = x1 - x3
        //val by = y1 - y3
        //val bl = hypot(bx, by)
        //return acos((ax * bx + ay * by) / (al * bl))
    }
}
