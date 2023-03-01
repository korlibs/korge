package com.soywiz.korma.geom

enum class Orientation(val value: Int) {
    CLOCK_WISE(+1), COUNTER_CLOCK_WISE(-1), COLLINEAR(0);

    operator fun unaryMinus(): Orientation = when (this) {
        CLOCK_WISE -> COUNTER_CLOCK_WISE
        COUNTER_CLOCK_WISE -> CLOCK_WISE
        COLLINEAR -> COLLINEAR
    }

    companion object {
        private const val EPSILON: Double = 1e-12

        fun orient2d(pa: IPoint, pb: IPoint, pc: IPoint): Orientation = orient2d(pa.point, pb.point, pc.point)

        fun orient2d(pa: Point, pb: Point, pc: Point): Orientation = -orient2dFixed(pa, pb, pc)

        fun orient2dFixed(pa: Point, pb: Point, pc: Point): Orientation {
            val detleft: Float = (pa.x - pc.x) * (pb.y - pc.y)
            val detright: Float = (pa.y - pc.y) * (pb.x - pc.x)
            val v: Float = detleft - detright

            return when {
                (v > -EPSILON) && (v < EPSILON) -> COLLINEAR
                v > 0 -> CLOCK_WISE
                else -> COUNTER_CLOCK_WISE
            }
        }
    }
}
