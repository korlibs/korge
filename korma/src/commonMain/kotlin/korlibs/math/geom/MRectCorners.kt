package korlibs.math.geom

import korlibs.math.annotations.KormaMutableApi

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
