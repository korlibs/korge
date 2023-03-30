package korlibs.math.geom

import korlibs.memory.pack.*

inline class Ray(val data: Float4Pack) {
    val point: Point get() = Point(data.f0, data.f1)
    val direction: Vector2 get() = Vector2(data.f2, data.f3)
    val angle: Angle get() = direction.angle

    constructor(point: Point, direction: Vector2) : this(float4PackOf(point.x, point.y, direction.x, direction.y))
    constructor(point: Point, angle: Angle) : this(point, Vector2.polar(angle))

    fun isAlmostEquals(other: Ray, epsilon: Float = 0.00001f): Boolean =
        this.point.isAlmostEquals(other.point, epsilon) && this.direction.isAlmostEquals(other.direction, epsilon)

    fun transformed(m: Matrix): Ray = Ray(m.transform(point), m.deltaTransform(direction))
    fun toLine(length: Float = 100000f): Line = Line(point, point + direction * length)

    override fun toString(): String = "Ray($point, $angle)"
}
