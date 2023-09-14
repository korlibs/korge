package korlibs.math.geom

import kotlin.math.*

enum class Orientation(val value: Int) {
    CLOCK_WISE(+1), COUNTER_CLOCK_WISE(-1), COLLINEAR(0);

    operator fun unaryMinus(): Orientation = when (this) {
        CLOCK_WISE -> COUNTER_CLOCK_WISE
        COUNTER_CLOCK_WISE -> CLOCK_WISE
        COLLINEAR -> COLLINEAR
    }
    operator fun unaryPlus(): Orientation = this

    companion object {
        private const val EPSILONf: Float = 1e-4f
        private const val EPSILON: Double = 1e-7

        //fun orient3d(v1: Vector3, v2: Vector3, v3: Vector3, epsilon: Float = EPSILONf): Orientation {
        //    // vectors from v1 to v2 and from v1 to v3
        //    val a = v2 - v1
        //    val b = v3 - v1
        //    val crossProduct = a.cross(b)
        //    // check the direction of the cross product
        //    return when {
        //        abs(crossProduct.z) < epsilon -> Orientation.COLLINEAR
        //        crossProduct.z < 0 -> Orientation.CLOCK_WISE
        //        else -> Orientation.COUNTER_CLOCK_WISE
        //    }
        //}

        internal fun checkValidUpVector(up: Vector2) {
            check(up.x == 0f && up.y.absoluteValue == 1f) { "up vector only supports (0, -1) and (0, +1) for now" }
        }

        // @TODO: Should we provide an UP vector as reference instead? ie. Vector2(0, +1) or Vector2(0, -1), would make sense for 3d?
        fun orient2d(pa: Point, pb: Point, pc: Point, up: Vector2 = Vector2.UP): Orientation {
            return orient2d(pa.xD, pa.yD, pb.xD, pb.yD, pc.xD, pc.yD, up = up)
        }

        fun orient2d(paX: Double, paY: Double, pbX: Double, pbY: Double, pcX: Double, pcY: Double, epsilon: Double = EPSILON, up: Vector2 = Vector2.UP): Orientation {
            checkValidUpVector(up)
            // Cross product
            val detleft: Double = (paX - pcX) * (pbY - pcY)
            val detright: Double = (paY - pcY) * (pbX - pcX)
            val v: Double = detleft - detright

            val res: Orientation = when {
                v.absoluteValue < epsilon -> COLLINEAR
                v > 0 -> COUNTER_CLOCK_WISE
                else -> CLOCK_WISE
            }
            return if (up.y > 0) res else -res
        }

        @Deprecated("", ReplaceWith(
            "orient2d(paX, paY, pbX, pbY, pcX, pcY, epsilon, up = Vector2.UP_SCREEN)",
            "korlibs.math.geom.Orientation.Companion.orient2d"
        ))
        fun orient2dFixed(paX: Double, paY: Double, pbX: Double, pbY: Double, pcX: Double, pcY: Double, epsilon: Double = EPSILON): Orientation =
            orient2d(paX, paY, pbX, pbY, pcX, pcY, epsilon, up = Vector2.UP_SCREEN)
    }
}
