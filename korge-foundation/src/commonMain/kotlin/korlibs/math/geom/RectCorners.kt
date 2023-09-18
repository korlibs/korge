package korlibs.math.geom

import korlibs.memory.*

//@KormaValueApi
data class RectCorners(
    val topLeft: Float,
    val topRight: Float,
    val bottomRight: Float,
    val bottomLeft: Float,
) {
    companion object {
        val EMPTY = RectCorners(0f)
    }

    constructor(corner: Int) : this(corner, corner, corner, corner)
    constructor(topLeftBottomRight: Int, topRightAndBottomLeft: Int) : this(topLeftBottomRight, topRightAndBottomLeft, topLeftBottomRight, topRightAndBottomLeft)
    constructor(topLeft: Int, topRightAndBottomLeft: Int, bottomRight: Int) : this(topLeft, topRightAndBottomLeft, bottomRight, topRightAndBottomLeft)
    constructor(topLeft: Int, topRight: Int, bottomRight: Int, bottomLeft: Int) : this(topLeft.toFloat(), topRight.toFloat(), bottomRight.toFloat(), bottomLeft.toFloat())

    constructor(corner: Float) : this(corner, corner, corner, corner)
    constructor(topLeftBottomRight: Float, topRightAndBottomLeft: Float) : this(topLeftBottomRight, topRightAndBottomLeft, topLeftBottomRight, topRightAndBottomLeft)
    constructor(topLeft: Float, topRightAndBottomLeft: Float, bottomRight: Float) : this(topLeft, topRightAndBottomLeft, bottomRight, topRightAndBottomLeft)

    constructor(corner: Double) : this(corner.toFloat())
    constructor(topLeftBottomRight: Double, topRightAndBottomLeft: Double) : this(topLeftBottomRight.toFloat(), topRightAndBottomLeft.toFloat())
    constructor(topLeft: Double, topRightAndBottomLeft: Double, bottomRight: Double) : this(topLeft.toFloat(), topRightAndBottomLeft.toFloat(), bottomRight.toFloat())
    constructor(topLeft: Double, topRight: Double, bottomRight: Double, bottomLeft: Double) : this(topLeft.toFloat(), topRight.toFloat(), bottomRight.toFloat(), bottomLeft.toFloat())
}
