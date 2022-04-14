package com.soywiz.korma.geom

interface ICircle {
    val center: IPoint
    val radius: Double
}

val ICircle.centerX: Double get() = center.x
val ICircle.centerY: Double get() = center.y

class Circle(override val center: IPoint, override val radius: Double) : ICircle {
    constructor(x: Double, y: Double, radius: Double) : this(IPoint(x, y), radius)
}

fun ICircle.projectedPoint(point: IPoint, out: Point = Point()): Point {
    //if (point == this.center) return out.copyFrom(center)

    val circle = this
    val pos = point
    val angle = Angle.between(circle.center, pos)
    return out.setToPolar(circle.center, angle, circle.radius)
}
