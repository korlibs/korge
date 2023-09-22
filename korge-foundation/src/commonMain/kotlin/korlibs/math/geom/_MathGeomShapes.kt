package korlibs.math.geom

import korlibs.math.geom.bezier.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import kotlin.math.*

data class Circle(override val center: Point, val radius: Float) : AbstractShape2D() {
    override val lazyVectorPath: VectorPath by lazy { buildVectorPath { circle(this@Circle.center, this@Circle.radius) } }

    constructor(x: Float, y: Float, radius: Float) : this(Point(x, y), radius)

    override val area: Float get() = (PIF * radius * radius)
    override val perimeter: Float get() = (PI2F * radius)
    override fun distance(p: Point): Float = (p - center).length - radius
    override fun normalVectorAt(p: Point): Vector2 = (p - center).normalized

    val radiusSquared: Float get() = radius * radius

    fun distanceToCenterSquared(p: Point): Float = Point.distanceSquared(p, center)
    // @TODO: Check if inside the circle
    fun distanceClosestSquared(p: Point): Float = distanceToCenterSquared(p) - radiusSquared
    // @TODO: Check if inside the circle
    fun distanceFarthestSquared(p: Point): Float = distanceToCenterSquared(p) + radiusSquared
    override fun projectedPoint(p: Point): Point = Point.polar(center, Angle.between(center, p), radius)
    override fun containsPoint(p: Point): Boolean = (p - center).length <= radius
}

data class Ellipse(override val center: Point, val radius: Size) : Shape2D {
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

data class Polygon(val points: PointList) : AbstractShape2D() {
    override val lazyVectorPath: VectorPath by lazy { buildVectorPath { polygon(points, close = true) }  }
}

data class Polyline(val points: PointList) : AbstractShape2D() {
    override val lazyVectorPath: VectorPath by lazy { buildVectorPath { polygon(points, close = false) }  }
}

data class RoundRectangle(val rect: Rectangle, val corners: RectCorners) : AbstractShape2D() {
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
