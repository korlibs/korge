package korlibs.math.geom

import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*

data class Circle(override val center: Point, val radius: Float) : Shape2d {
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
    override fun toVectorPath(): VectorPath = buildVectorPath { circle(this@Circle.center, this@Circle.radius) }
}
