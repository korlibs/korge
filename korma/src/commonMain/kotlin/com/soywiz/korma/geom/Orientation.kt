package com.soywiz.korma.geom

enum class Orientation(val value: Int) {
    CLOCK_WISE(+1), COUNTER_CLOCK_WISE(-1), COLLINEAR(0);

    companion object {
        private const val EPSILON: Double = 1e-12

        fun orient2d(pa: IPoint, pb: IPoint, pc: IPoint): Orientation = orient2d(pa.x, pa.y, pb.x, pb.y, pc.x, pc.y)

        fun orient2d(paX: Double, paY: Double, pbX: Double, pbY: Double, pcX: Double, pcY: Double): Orientation {
            val detleft: Double = (paX - pcX) * (pbY - pcY)
            val detright: Double = (paY - pcY) * (pbX - pcX)
            val `val`: Double = detleft - detright

            if ((`val` > -EPSILON) && (`val` < EPSILON)) return Orientation.COLLINEAR
            if (`val` > 0) return Orientation.COUNTER_CLOCK_WISE
            return Orientation.CLOCK_WISE
        }
    }
}
