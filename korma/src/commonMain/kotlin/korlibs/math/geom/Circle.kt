package korlibs.math.geom

data class Circle(val center: Point, val radius: Double) {
    constructor(x: Double, y: Double, radius: Double) : this(Point(x, y), radius)

    val radiusSquared: Double get() = radius * radius

    fun distanceToCenterSquared(p: Point): Float = Point.distanceSquared(p, center)
    // @TODO: Check if inside the circle
    fun distanceClosestSquared(p: Point): Double = distanceToCenterSquared(p) - radiusSquared
    // @TODO: Check if inside the circle
    fun distanceFarthestSquared(p: Point): Double = distanceToCenterSquared(p) + radiusSquared
    fun projectedPoint(point: Point): Point = Point.fromPolar(center, Angle.between(center, point), radius)
}