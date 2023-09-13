package korlibs.math.geom

import korlibs.memory.pack.*

/** Represents an infinite [Ray] starting at [point] in the specified [direction] with an [angle] */
//inline class Ray(val data: Float4Pack) {
data class Ray
/** Constructs a [Ray] starting from [point] in the specified [direction] */
private constructor(
    /** Starting point */
    val point: Point,
    /** Normalized direction of the ray starting at [point] */
    val direction: Vector2,
) {
    companion object {
        /** Creates a ray starting in [start] and passing by [end] */
        fun fromTwoPoints(start: Point, end: Point): Ray = Ray(start, end - start, Unit)
    }

    //val point: Point get() = Point(data.f0, data.f1)
    //val direction: Vector2 get() = Vector2(data.f2, data.f3)
    /** Angle between two points */
    val angle: Angle get() = direction.angle

    /** Constructs a [Ray] starting from [point] in the specified [direction] */
    constructor(point: Point, direction: Vector2, unit: Unit = Unit) : this(point, direction.normalized)
    /** Constructs a [Ray] starting from [point] in the specified [angle] */
    constructor(point: Point, angle: Angle) : this(point, Vector2.polar(angle), Unit)

    //private constructor(point: Point, normalizedDirection: Vector2, unit: Unit) : this(point.x, point.y, normalizedDirection.x, normalizedDirection.y)

    /** Checks if [this] and [other]are equals with an [epsilon] difference */
    fun isAlmostEquals(other: Ray, epsilon: Float = 0.00001f): Boolean =
        this.point.isAlmostEquals(other.point, epsilon) && this.direction.isAlmostEquals(other.direction, epsilon)

    /** Checks if [this] and [other]are equals with an [epsilon] tolerance */
    fun transformed(m: Matrix): Ray = Ray(m.transform(point), m.deltaTransform(direction).normalized)

    /** Converts this [Ray] into a [Line] of a specific [length] starting by [point] */
    fun toLine(length: Float = 100000f): Line = Line(point, point + direction * length)

    override fun toString(): String = "Ray($point, $angle)"
}
