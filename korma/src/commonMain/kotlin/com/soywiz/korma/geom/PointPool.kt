package com.soywiz.korma.geom

@Suppress("NOTHING_TO_INLINE")
class PointPool(val size: Int) {
    @PublishedApi
    internal val points = Array(size) { com.soywiz.korma.geom.Point() }
    @PublishedApi
    internal var offset = 0

    @PublishedApi
    internal fun alloc() = points[offset++]

    fun Point(x: Double, y: Double) = alloc().setTo(x, y)
    fun Point(x: Float, y: Float) = Point(x.toDouble(), y.toDouble())
    fun Point(x: Int, y: Int) = Point(x.toDouble(), y.toDouble())
    fun Point() = Point(0.0, 0.0)

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

    inline operator fun invoke(callback: PointPool.() -> Unit) {
        val oldOffset = offset
        try {
            callback()
        } finally {
            offset = oldOffset
        }
    }
}
