package korlibs.math.geom

import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*

data class Polyline(val points: PointList) : AbstractNShape2D() {
    override val lazyVectorPath: VectorPath by lazy { buildVectorPath { polygon(points, close = false) }  }
}
