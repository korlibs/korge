package com.soywiz.korma.geom

import com.soywiz.kds.*
import com.soywiz.kmem.clamp
import com.soywiz.korma.annotations.*
import com.soywiz.korma.internal.umod
import kotlin.jvm.*

@Suppress("NOTHING_TO_INLINE")
@KormaMutableApi
class PointPool(val capacity: Int = 16, preallocate: Boolean = false) {
    @PublishedApi
    internal var offset = 0

    //@PublishedApi internal val points = Array(capacity) { com.soywiz.korma.geom.Point() }
    //@PublishedApi internal fun alloc(): Point = points[offset++]

    @PublishedApi internal val points = FastArrayList<MPoint>()
    @PublishedApi internal fun alloc(): MPoint {
        return if (offset < points.size) {
            points[offset++]
        } else {
            offset++
            com.soywiz.korma.geom.MPoint().also { points.add(it) }
        }
    }

    init {
        if (preallocate) {
            invoke {
                repeat(capacity) { alloc() }
            }
        }
    }

    fun MPoint(): MPoint = alloc()
    fun Point(x: Double, y: Double): MPoint = alloc().setTo(x, y)
    fun Point(x: Float, y: Float): MPoint = Point(x.toDouble(), y.toDouble())
    fun Point(x: Int, y: Int): MPoint = Point(x.toDouble(), y.toDouble())
    fun Point(): MPoint = Point(0.0, 0.0)
    fun Point(angle: Angle, length: Double = 1.0): MPoint = MPoint.fromPolar(angle, length, alloc())
    fun Point(base: MPoint, angle: Angle, length: Double = 1.0): MPoint = MPoint.fromPolar(base, angle, length, alloc())
    fun Point(angle: Angle, length: Float = 1f): MPoint = MPoint.fromPolar(angle, length.toDouble(), alloc())
    fun Point(base: MPoint, angle: Angle, length: Float = 1f): MPoint = MPoint.fromPolar(base, angle, length.toDouble(), alloc())

    fun abs(a: MPoint): MPoint = alloc().setTo(kotlin.math.abs(a.x), kotlin.math.abs(a.y))
    fun sqrt(a: MPoint): MPoint = alloc().setTo(kotlin.math.sqrt(a.x), kotlin.math.sqrt(a.y))
    fun min(a: MPoint, b: MPoint): MPoint = alloc().setTo(kotlin.math.min(a.x, b.x), kotlin.math.min(a.y, b.y))
    fun max(a: MPoint, b: MPoint): MPoint = alloc().setTo(kotlin.math.max(a.x, b.x), kotlin.math.max(a.y, b.y))
    fun clamp(p: MPoint, min: Double, max: Double): MPoint = p.clamp(min, max)
    @JvmName("IPoint_clamp")
    fun MPoint.clamp(min: Double, max: Double): MPoint = alloc().setTo(x.clamp(min, max), y.clamp(min, max))
    val MPoint.absoluteValue: MPoint get() = abs(this)

    operator fun Double.times(other: MPoint): MPoint = other.times(this)
    operator fun Double.minus(other: MPoint): MPoint = alloc().setTo(this - other.x, this - other.y)
    operator fun Double.plus(other: MPoint): MPoint = alloc().setTo(this + other.x, this + other.y)

    operator fun MPoint.unaryMinus(): MPoint = alloc().setTo(-x, -y)

    operator fun MPoint.plus(other: MPoint): MPoint = alloc().setToAdd(this, other)
    operator fun MPoint.minus(other: MPoint): MPoint = alloc().setToSub(this, other)

    operator fun MPoint.times(value: MPoint): MPoint = alloc().setToMul(this, value)
    operator fun MPoint.times(value: Double): MPoint = alloc().setToMul(this, value)
    operator fun MPoint.times(value: Float): MPoint = this * value.toDouble()
    operator fun MPoint.times(value: Int): MPoint = this * value.toDouble()

    operator fun MPoint.div(value: MPoint): MPoint = alloc().setToDiv(this, value)
    operator fun MPoint.div(value: Double): MPoint = alloc().setToDiv(this, value)
    operator fun MPoint.div(value: Float): MPoint = this / value.toDouble()
    operator fun MPoint.div(value: Int): MPoint = this / value.toDouble()

    operator fun MPoint.rem(value: MPoint): MPoint = Point(this.x % value.x, this.y % value.y)
    operator fun MPoint.rem(value: Double): MPoint = Point(this.x % value, this.y % value)
    operator fun MPoint.rem(value: Float): MPoint = this % value.toDouble()
    operator fun MPoint.rem(value: Int): MPoint = this % value.toDouble()

    operator fun PointList.get(index: Int): MPoint = MPoint().setTo(this[index])
    fun PointList.getCyclic(index: Int): MPoint = this[index umod size].mutable

    inline operator fun <T> invoke(callback: PointPool.() -> T): T {
        val oldOffset = offset
        try {
            return callback()
        } finally {
            offset = oldOffset
        }
    }
}
