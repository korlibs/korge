package com.soywiz.korma.geom

import com.soywiz.kmem.*
import com.soywiz.kmem.pack.*
import com.soywiz.korma.annotations.*

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

@KormaMutableApi
@Deprecated("Use RectCorners")
sealed interface IRectCorners {
    val topLeft: Double
    val topRight: Double
    val bottomRight: Double
    val bottomLeft: Double

    fun duplicate(
        topLeft: Double = this.topLeft,
        topRight: Double = this.topRight,
        bottomRight: Double = this.bottomRight,
        bottomLeft: Double = this.bottomLeft,
    ): IRectCorners = MRectCorners(topLeft, topRight, bottomRight, bottomLeft)

    companion object {
        val EMPTY: IRectCorners = MRectCorners(0.0, 0.0, 0.0, 0.0)

        operator fun invoke(
            topLeft: Double,
            topRight: Double = topLeft,
            bottomRight: Double = topLeft,
            bottomLeft: Double = topRight,
        ): IRectCorners = MRectCorners(topLeft, topRight, bottomRight, bottomLeft)

        operator fun invoke(
            topLeft: Int,
            topRight: Int = topLeft,
            bottomRight: Int = topLeft,
            bottomLeft: Int = topRight,
        ): IRectCorners = MRectCorners(topLeft.toDouble(), topRight.toDouble(), bottomRight.toDouble(), bottomLeft.toDouble())
    }
}

@KormaMutableApi
@Deprecated("Use RectCorners")
data class MRectCorners(
    override var topLeft: Double,
    override var topRight: Double = topLeft,
    override var bottomRight: Double = topLeft,
    override var bottomLeft: Double = topRight,
) : IRectCorners
