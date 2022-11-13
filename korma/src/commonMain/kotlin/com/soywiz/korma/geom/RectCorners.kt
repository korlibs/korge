package com.soywiz.korma.geom

interface RectCorners {
    val topLeft: Double
    val topRight: Double
    val bottomRight: Double
    val bottomLeft: Double

    companion object {
        val EMPTY: RectCorners = MutableRectCorners(0.0, 0.0, 0.0, 0.0)

        operator fun invoke(
            topLeft: Double,
            topRight: Double = topLeft,
            bottomRight: Double = topLeft,
            bottomLeft: Double = topRight,
        ): RectCorners = MutableRectCorners(topLeft, topRight, bottomRight, bottomLeft)
    }
}

data class MutableRectCorners(
    override var topLeft: Double,
    override var topRight: Double = topLeft,
    override var bottomRight: Double = topLeft,
    override var bottomLeft: Double = topRight,
) : RectCorners {
}
