package korlibs.math.geom

import korlibs.memory.*
import korlibs.memory.pack.*
import korlibs.math.annotations.*

//@KormaValueApi
inline class RectCorners internal constructor(val raw: Half4Pack) {
    companion object {
        val EMPTY = RectCorners(0f)
    }

    constructor(corner: Float) : this(corner, corner, corner, corner)
    constructor(topLeftBottomRight: Float, topRightAndBottomLeft: Float) : this(topLeftBottomRight, topRightAndBottomLeft, topLeftBottomRight, topRightAndBottomLeft)
    constructor(topLeft: Float, topRightAndBottomLeft: Float, bottomRight: Float) : this(topLeft, topRightAndBottomLeft, bottomRight, topRightAndBottomLeft)
    constructor(topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float) : this(Half4Pack(topLeft.toHalf(), topRight.toHalf(), bottomRight.toHalf(), bottomLeft.toHalf()))

    constructor(corner: Double) : this(corner.toFloat())
    constructor(topLeftBottomRight: Double, topRightAndBottomLeft: Double) : this(topLeftBottomRight.toFloat(), topRightAndBottomLeft.toFloat())
    constructor(topLeft: Double, topRightAndBottomLeft: Double, bottomRight: Double) : this(topLeft.toFloat(), topRightAndBottomLeft.toFloat(), bottomRight.toFloat())
    constructor(topLeft: Double, topRight: Double, bottomRight: Double, bottomLeft: Double) : this(topLeft.toFloat(), topRight.toFloat(), bottomRight.toFloat(), bottomLeft.toFloat())

    val topLeft: Float get() = raw.x.toFloat()
    val topRight: Float get() = raw.y.toFloat()
    val bottomRight: Float get() = raw.z.toFloat()
    val bottomLeft: Float get() = raw.w.toFloat()

    fun copy(topLeft: Float = this.topLeft, topRight: Float = this.topRight, bottomRight: Float = this.bottomRight, bottomLeft: Float = this.bottomLeft): RectCorners =
        RectCorners(topLeft, topRight, bottomRight, bottomLeft)
}
