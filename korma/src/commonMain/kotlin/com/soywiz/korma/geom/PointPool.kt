package com.soywiz.korma.geom

import com.soywiz.kds.FastArrayList
import com.soywiz.korma.internal.umod
import com.soywiz.kmem.clamp
import kotlin.jvm.JvmName
import kotlin.math.min
import kotlin.math.max

typealias PointScope = PointPool

@Suppress("NOTHING_TO_INLINE")
class PointPool(val capacity: Int = 16, preallocate: Boolean = false) {
    @PublishedApi
    internal var offset = 0

    //@PublishedApi internal val points = Array(capacity) { com.soywiz.korma.geom.Point() }
    //@PublishedApi internal fun alloc(): Point = points[offset++]

    @PublishedApi internal val points = FastArrayList<Point>()
    @PublishedApi internal fun alloc(): Point {
        return if (offset < points.size) {
            points[offset++]
        } else {
            offset++
            com.soywiz.korma.geom.Point().also { points.add(it) }
        }
    }

    init {
        if (preallocate) {
            invoke {
                repeat(capacity) { alloc() }
            }
        }
    }

    fun MPoint(): Point = alloc()
    fun Point(x: Double, y: Double): IPoint = alloc().setTo(x, y)
    fun Point(x: Float, y: Float): IPoint = Point(x.toDouble(), y.toDouble())
    fun Point(x: Int, y: Int): IPoint = Point(x.toDouble(), y.toDouble())
    fun Point(): IPoint = Point(0.0, 0.0)
    fun Point(angle: Angle, length: Double = 1.0): IPoint = Point.fromPolar(angle, length, alloc())
    fun Point(base: IPoint, angle: Angle, length: Double = 1.0): IPoint = Point.fromPolar(base, angle, length, alloc())
    fun Point(angle: Angle, length: Float = 1f): IPoint = Point.fromPolar(angle, length.toDouble(), alloc())
    fun Point(base: IPoint, angle: Angle, length: Float = 1f): IPoint = Point.fromPolar(base, angle, length.toDouble(), alloc())

    fun abs(a: IPoint): IPoint = alloc().setTo(kotlin.math.abs(a.x), kotlin.math.abs(a.y))
    fun sqrt(a: IPoint): IPoint = alloc().setTo(kotlin.math.sqrt(a.x), kotlin.math.sqrt(a.y))
    fun min(a: IPoint, b: IPoint): IPoint = alloc().setTo(min(a.x, b.x), min(a.y, b.y))
    fun max(a: IPoint, b: IPoint): IPoint = alloc().setTo(max(a.x, b.x), max(a.y, b.y))
    fun clamp(p: IPoint, min: Double, max: Double): IPoint = p.clamp(min, max)
    @JvmName("IPoint_clamp")
    fun IPoint.clamp(min: Double, max: Double): IPoint = alloc().setTo(x.clamp(min, max), y.clamp(min, max))
    val IPoint.absoluteValue: IPoint get() = abs(this)

    operator fun Double.times(other: IPoint): IPoint = other.times(this)
    operator fun Double.minus(other: IPoint): IPoint = alloc().setTo(this - other.x, this - other.y)
    operator fun Double.plus(other: IPoint): IPoint = alloc().setTo(this + other.x, this + other.y)

    operator fun IPoint.unaryMinus(): IPoint = alloc().setTo(-x, -y)

    operator fun IPoint.plus(other: IPoint): IPoint = alloc().setToAdd(this, other)
    operator fun IPoint.minus(other: IPoint): IPoint = alloc().setToSub(this, other)

    operator fun IPoint.times(value: IPoint): IPoint = alloc().setToMul(this, value)
    operator fun IPoint.times(value: Double): IPoint = alloc().setToMul(this, value)
    operator fun IPoint.times(value: Float): IPoint = this * value.toDouble()
    operator fun IPoint.times(value: Int): IPoint = this * value.toDouble()

    operator fun IPoint.div(value: IPoint): IPoint = alloc().setToDiv(this, value)
    operator fun IPoint.div(value: Double): IPoint = alloc().setToDiv(this, value)
    operator fun IPoint.div(value: Float): IPoint = this / value.toDouble()
    operator fun IPoint.div(value: Int): IPoint = this / value.toDouble()

    operator fun IPoint.rem(value: IPoint): IPoint = Point(this.x % value.x, this.y % value.y)
    operator fun IPoint.rem(value: Double): IPoint = Point(this.x % value, this.y % value)
    operator fun IPoint.rem(value: Float): IPoint = this % value.toDouble()
    operator fun IPoint.rem(value: Int): IPoint = this % value.toDouble()

    operator fun IPointArrayList.get(index: Int): Point = MPoint().setTo(this.getX(index), this.getY(index))
    fun IPointArrayList.getCyclic(index: Int): Point = this[index umod size]

    inline operator fun <T> invoke(callback: PointPool.() -> T): T {
        val oldOffset = offset
        try {
            return callback()
        } finally {
            offset = oldOffset
        }
    }
}
