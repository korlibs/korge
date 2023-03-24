package korlibs.math.geom

@Deprecated("")
interface ICircle {
    val center: Point
    val radius: Double
    val radiusSquared: Double get() = radius * radius

    fun distanceToCenterSquared(p: Point): Float = Point.distanceSquared(p, center)
    // @TODO: Check if inside the circle
    fun distanceClosestSquared(p: Point): Double = distanceToCenterSquared(p) - radiusSquared
    // @TODO: Check if inside the circle
    fun distanceFarthestSquared(p: Point): Double = distanceToCenterSquared(p) + radiusSquared
    fun projectedPoint(point: Point): Point = Point.fromPolar(center, Angle.between(center, point), radius)
}

@Deprecated("")
data class MCircle(override val center: Point, override val radius: Double) : ICircle {
    constructor(x: Double, y: Double, radius: Double) : this(Point(x, y), radius)

    override val radiusSquared: Double = radius * radius
}
