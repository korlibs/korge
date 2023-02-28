package com.soywiz.korma.geom

data class Circle(val center: Point, val radius: Double) {
    constructor(x: Double, y: Double, radius: Double) : this(Point(x, y), radius)

    val radiusSquared: Double get() = radius * radius
}

interface ICircle {
    val center: IPoint
    val radius: Double
    val radiusSquared: Double get() = radius * radius

    val centerX: Double get() = center.x
    val centerY: Double get() = center.y

    fun distanceToCenterSquared(p: IPoint): Double = MPoint.distanceSquared(p, center)
    fun distanceToCenterSquared(p: Point): Float = Point.distanceSquared(p, center.point)
    // @TODO: Check if inside the circle
    fun distanceClosestSquared(p: Point): Double = distanceToCenterSquared(p) - radiusSquared
    fun distanceClosestSquared(p: IPoint): Double = distanceToCenterSquared(p) - radiusSquared
    // @TODO: Check if inside the circle
    fun distanceFarthestSquared(p: Point): Double = distanceToCenterSquared(p) + radiusSquared
    fun distanceFarthestSquared(p: IPoint): Double = distanceToCenterSquared(p) + radiusSquared
    fun projectedPoint(point: IPoint, out: MPoint = MPoint()): MPoint {
        val circle = this
        val pos = point
        val center = circle.center
        val angle = Angle.between(center, pos)
        return out.setToPolar(center, angle, circle.radius)
    }
    fun projectedPoint(point: Point): Point {
        val circle = this
        val center = circle.center.point
        return Point.fromPolar(center, Angle.between(center, point), circle.radius)
    }
}

data class MCircle(override val center: IPoint, override val radius: Double) : ICircle {
    constructor(x: Double, y: Double, radius: Double) : this(Point(x, y).mutable, radius)

    override val radiusSquared: Double = radius * radius
}
