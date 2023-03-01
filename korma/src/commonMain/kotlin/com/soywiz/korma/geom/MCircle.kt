package com.soywiz.korma.geom

data class Circle(val center: Point, val radius: Double) {
    constructor(x: Double, y: Double, radius: Double) : this(Point(x, y), radius)

    val radiusSquared: Double get() = radius * radius
}

interface ICircle {
    val center: Point
    val radius: Double
    val radiusSquared: Double get() = radius * radius

    fun distanceToCenterSquared(p: Point): Float = Point.distanceSquared(p, center)
    // @TODO: Check if inside the circle
    fun distanceClosestSquared(p: Point): Double = distanceToCenterSquared(p) - radiusSquared
    // @TODO: Check if inside the circle
    fun distanceFarthestSquared(p: Point): Double = distanceToCenterSquared(p) + radiusSquared
    fun projectedPoint(point: Point): Point {
        val circle = this
        val center = circle.center
        return Point.fromPolar(center, Angle.between(center, point), circle.radius)
    }
}

data class MCircle(override val center: Point, override val radius: Double) : ICircle {
    constructor(x: Double, y: Double, radius: Double) : this(Point(x, y), radius)

    override val radiusSquared: Double = radius * radius
}
