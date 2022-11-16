package com.soywiz.korma.geom

interface RectCorners {
    val topLeft: Double
    val topRight: Double
    val bottomRight: Double
    val bottomLeft: Double

    fun duplicate(
        topLeft: Double = this.topLeft,
        topRight: Double = this.topRight,
        bottomRight: Double = this.bottomRight,
        bottomLeft: Double = this.bottomLeft,
    ): RectCorners = MutableRectCorners(topLeft, topRight, bottomRight, bottomLeft)

    companion object {
        val EMPTY: RectCorners = MutableRectCorners(0.0, 0.0, 0.0, 0.0)

        operator fun invoke(
            topLeft: Double,
            topRight: Double = topLeft,
            bottomRight: Double = topLeft,
            bottomLeft: Double = topRight,
        ): RectCorners = MutableRectCorners(topLeft, topRight, bottomRight, bottomLeft)

        operator fun invoke(
            topLeft: Int,
            topRight: Int = topLeft,
            bottomRight: Int = topLeft,
            bottomLeft: Int = topRight,
        ): RectCorners = MutableRectCorners(topLeft.toDouble(), topRight.toDouble(), bottomRight.toDouble(), bottomLeft.toDouble())
    }
}

data class MutableRectCorners(
    override var topLeft: Double,
    override var topRight: Double = topLeft,
    override var bottomRight: Double = topLeft,
    override var bottomLeft: Double = topRight,
) : RectCorners {
}
