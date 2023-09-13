package korlibs.math.geom

import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*

data class Polygon(val points: PointList) : AbstractShape2D() {
    override val lazyVectorPath: VectorPath by lazy { buildVectorPath { polygon(points, close = true) }  }
}
