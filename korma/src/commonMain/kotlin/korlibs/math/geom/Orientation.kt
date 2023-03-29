package korlibs.math.geom

import kotlin.math.*

enum class Orientation(val value: Int) {
    CLOCK_WISE(+1), COUNTER_CLOCK_WISE(-1), COLLINEAR(0);

    companion object {
        private const val EPSILON: Double = 1e-7

        fun orient2d(pa: Point, pb: Point, pc: Point): Orientation = orient2d(pa.xD, pa.yD, pb.xD, pb.yD, pc.xD, pc.yD)

        fun orient2d(paX: Double, paY: Double, pbX: Double, pbY: Double, pcX: Double, pcY: Double, epsilon: Double = EPSILON): Orientation {
            val detleft: Double = (paX - pcX) * (pbY - pcY)
            val detright: Double = (paY - pcY) * (pbX - pcX)
            val v: Double = detleft - detright

            return when {
                v.absoluteValue < epsilon -> COLLINEAR
                v > 0 -> COUNTER_CLOCK_WISE
                else -> CLOCK_WISE
            }
        }

        fun orient2dFixed(paX: Double, paY: Double, pbX: Double, pbY: Double, pcX: Double, pcY: Double): Orientation {
            val detleft: Double = (paX - pcX) * (pbY - pcY)
            val detright: Double = (paY - pcY) * (pbX - pcX)
            val v: Double = detleft - detright

            return when {
                (v > -EPSILON) && (v < EPSILON) -> COLLINEAR
                v > 0 -> CLOCK_WISE
                else -> COUNTER_CLOCK_WISE
            }
        }
    }
}
