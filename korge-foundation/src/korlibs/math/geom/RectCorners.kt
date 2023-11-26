package korlibs.math.geom

data class RectCorners(
    val topLeft: Double,
    val topRight: Double,
    val bottomRight: Double,
    val bottomLeft: Double,
) {
    operator fun unaryMinus(): RectCorners = this * (-1.0)
    operator fun unaryPlus(): RectCorners = this
    operator fun plus(that: RectCorners): RectCorners = RectCorners(this.topLeft + that.topLeft, this.topRight + that.topRight, this.bottomLeft + that.bottomLeft, this.bottomRight + that.bottomRight)
    operator fun minus(that: RectCorners): RectCorners = RectCorners(this.topLeft - that.topLeft, this.topRight - that.topRight, this.bottomLeft - that.bottomLeft, this.bottomRight - that.bottomRight)
    operator fun times(scale: Double): RectCorners = RectCorners(topLeft * scale, topRight * scale, bottomRight * scale, bottomLeft * scale)
    operator fun div(scale: Double): RectCorners = this * (1.0 / scale)

    companion object {
        val EMPTY = RectCorners(0)
        val ZERO = RectCorners(0)
        val ONE = RectCorners(1.0)
        val MINUS_ONE = RectCorners(-1.0)
        val NaN = RectCorners(Double.NaN)

        inline operator fun invoke(corner: Number): RectCorners = RectCorners(corner.toDouble(), corner.toDouble(), corner.toDouble(), corner.toDouble())
        inline operator fun invoke(topLeftBottomRight: Number, topRightAndBottomLeft: Number): RectCorners = RectCorners(topLeftBottomRight.toDouble(), topRightAndBottomLeft.toDouble(), topLeftBottomRight.toDouble(), topRightAndBottomLeft.toDouble())
        inline operator fun invoke(topLeft: Number, topRightAndBottomLeft: Number, bottomRight: Number): RectCorners = RectCorners(topLeft.toDouble(), topRightAndBottomLeft.toDouble(), bottomRight.toDouble(), topRightAndBottomLeft.toDouble())
        inline operator fun invoke(topLeft: Number, topRight: Number, bottomRight: Number, bottomLeft: Number): RectCorners = RectCorners(topLeft.toDouble(), topRight.toDouble(), bottomRight.toDouble(), bottomLeft.toDouble())
    }
}
