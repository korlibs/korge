package korlibs.math.geom

data class RectCorners(
    val topLeft: Double,
    val topRight: Double,
    val bottomRight: Double,
    val bottomLeft: Double,
) {
    companion object {
        val EMPTY = RectCorners(0)

        inline operator fun invoke(corner: Number): RectCorners = RectCorners(corner.toDouble(), corner.toDouble(), corner.toDouble(), corner.toDouble())
        inline operator fun invoke(topLeftBottomRight: Number, topRightAndBottomLeft: Number): RectCorners = RectCorners(topLeftBottomRight.toDouble(), topRightAndBottomLeft.toDouble(), topLeftBottomRight.toDouble(), topRightAndBottomLeft.toDouble())
        inline operator fun invoke(topLeft: Number, topRightAndBottomLeft: Number, bottomRight: Number): RectCorners = RectCorners(topLeft.toDouble(), topRightAndBottomLeft.toDouble(), bottomRight.toDouble(), topRightAndBottomLeft.toDouble())
        inline operator fun invoke(topLeft: Number, topRight: Number, bottomRight: Number, bottomLeft: Number): RectCorners = RectCorners(topLeft.toDouble(), topRight.toDouble(), bottomRight.toDouble(), bottomLeft.toDouble())
    }
}
