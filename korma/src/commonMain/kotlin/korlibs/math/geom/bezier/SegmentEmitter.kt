package korlibs.math.geom.bezier

import korlibs.math.geom.*

object SegmentEmitter {
    inline fun emit(
        segments: Int,
        crossinline curveGen: (p: MPoint, t: Double) -> MPoint,
        crossinline gen: (p0: MPoint, p1: MPoint) -> Unit,
        p1: MPoint = MPoint(),
        p2: MPoint = MPoint()
    ) {
        val dt = 1.0 / segments
        for (n in 0 until segments) {
            p1.copyFrom(p2)
            p2.copyFrom(curveGen(p2, dt * n))
            if (n > 1) gen(p1, p2)
        }
    }
}
