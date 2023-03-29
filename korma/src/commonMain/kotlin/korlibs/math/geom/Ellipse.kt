package korlibs.math.geom

import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import kotlin.math.*

data class Ellipse(override val center: Point, val radius: Size) : Shape2d {
    override val area: Float get() = (PI * radius.widthD * radius.heightD).toFloat()
    override val perimeter: Float get() {
        if (radius.width == radius.height) return (2 * PI * radius.width).toFloat() // Circle formula
        val (a, b) = radius
        val h = ((a - b) * (a - b)) / ((a + b) * (a + b))
        return (PI * (a + b) * (1 + ((3 * h) / (10 + sqrt(4 - (3 * h)))))).toFloat()
    }

    override fun distance(p: Point): Float {
        val p = p - center
        val scaledPoint = Vector2(p.x / radius.width, p.y / radius.height)
        val length = scaledPoint.length
        return (length - 1) * min(radius.width, radius.height)
    }

    override fun normalVectorAt(p: Point): Vector2 {
        val pointOnEllipse = p - center
        val (a, b) = radius
        val normal = Vector2(pointOnEllipse.x / (a * a), pointOnEllipse.y / (b * b))
        return normal.normalized
        //val d = p - center
        //val r2 = radius.toVector() * radius.toVector()
        //return (d / r2).normalized
    }

    override fun projectedPoint(p: Point): Point {
        val angle = Angle.between(center, p)
        return center + Point(radius.width * angle.cosineF, radius.height * angle.sineF)

        //val k = (radius.width * radius.height) / sqrt()
        //return projectPointOntoEllipse(p, center, radius.toVector())
    }

    override fun containsPoint(p: Point): Boolean {
        if (radius.isEmpty()) return false
        // Check if the point is inside the ellipse using the ellipse equation:
        // (x - centerX)^2 / radiusX^2 + (y - centerY)^2 / radiusY^2 <= 1
        return ((p.x - center.x).pow(2f) / radius.width.pow(2)) + ((p.y - center.y).pow(2) / radius.height.pow(2)) <= 1
    }

    override fun toVectorPath(): VectorPath = buildVectorPath { ellipse(this@Ellipse.center, this@Ellipse.radius) }

    companion object {
        private fun projectPointOntoEllipse(point: Vector2, center: Vector2, radius: Vector2, tolerance: Double = 1e-6, maxIterations: Int = 100): Vector2 {
            var currentPoint = point
            var i = 0

            while (i < maxIterations) {
                val dx = currentPoint.x - center.x
                val dy = currentPoint.y - center.y
                val rx2 = radius.x * radius.x
                val ry2 = radius.y * radius.y

                val f = Vector2(
                    (dx * rx2 - dy * dx * dy) / (rx2 * ry2),
                    (dy * ry2 - dx * dy * dx) / (rx2 * ry2)
                )

                val df = Vector2(
                    (ry2 - 2.0 * dy * dy) / (rx2 * ry2),
                    (rx2 - 2.0 * dx * dx) / (rx2 * ry2)
                )

                val nextPoint = currentPoint - f / df
                val dist = (nextPoint - currentPoint).length

                if (dist < tolerance) return nextPoint

                currentPoint = nextPoint
                i++
            }

            return currentPoint
        }
    }
}
