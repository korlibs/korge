package korlibs.math.geom

import korlibs.math.geom.bezier.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*

data class RoundRectangle(val rect: Rectangle, val corners: RectCorners) : AbstractNShape2D() {
    private fun areaQuarter(radius: Float): Float = Arc.length(radius, Angle.QUARTER)
    private fun areaComplementaryQuarter(radius: Float): Float = (radius * radius) - areaQuarter(radius)
    override val lazyVectorPath: VectorPath by lazy { buildVectorPath { roundRect(this@RoundRectangle) } }

    override val area: Float get() = rect.area - (
        areaComplementaryQuarter(corners.topLeft) +
            areaComplementaryQuarter(corners.topRight) +
            areaComplementaryQuarter(corners.bottomLeft) +
            areaComplementaryQuarter(corners.bottomRight)
        )
}
