package com.soywiz.korma.geom

interface ICircle {
    val center: IPoint
    val radius: Double
    val radiusSquared: Double get() = radius * radius
}

val ICircle.centerX: Double get() = center.x
val ICircle.centerY: Double get() = center.y

data class Circle(override val center: IPoint, override val radius: Double) : ICircle {
    override val radiusSquared: Double = radius * radius

    constructor(x: Double, y: Double, radius: Double) : this(IPoint(x, y), radius)
}

fun ICircle.distanceToCenterSquared(p: IPoint): Double {
    return Point.distanceSquared(p, center)
}

// @TODO: Check if inside the circle
fun ICircle.distanceClosestSquared(p: IPoint): Double {
    return distanceToCenterSquared(p) - radiusSquared
}

// @TODO: Check if inside the circle
fun ICircle.distanceFarthestSquared(p: IPoint): Double {
    return distanceToCenterSquared(p) + radiusSquared
}

fun ICircle.projectedPoint(point: IPoint, out: Point = Point()): Point {
    //if (point == this.center) return out.copyFrom(center)

    val circle = this
    val pos = point
    val angle = Angle.between(circle.center, pos)
    return out.setToPolar(circle.center, angle, circle.radius)
}
