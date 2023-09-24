package korlibs.math.geom

import korlibs.math.*
import korlibs.math.annotations.*

typealias Ray = Ray2D
typealias Ray2 = Ray

/** Represents an infinite [Ray] starting at [point] in the specified [direction] with an [angle] */
//inline class Ray(val data: Float4Pack) {
data class Ray2D
/** Constructs a [Ray] starting from [point] in the specified [direction] */
private constructor(
    /** Starting point */
    val point: Point,
    /** Normalized direction of the ray starting at [point] */
    val direction: Vector2D,
) : IsAlmostEquals<Ray2D> {
    companion object {
        /** Creates a ray starting in [start] and passing by [end] */
        fun fromTwoPoints(start: Point, end: Point): Ray = Ray(start, end - start, Unit)
    }

    //val point: Point get() = Point(data.f0, data.f1)
    //val direction: Vector2 get() = Vector2(data.f2, data.f3)
    /** Angle between two points */
    val angle: Angle get() = direction.angle

    /** Constructs a [Ray] starting from [point] in the specified [direction] */
    constructor(point: Point, direction: Vector2D, unit: Unit = Unit) : this(point, direction.normalized)
    /** Constructs a [Ray] starting from [point] in the specified [angle] */
    constructor(point: Point, angle: Angle) : this(point, Vector2D.polar(angle), Unit)

    //private constructor(point: Point, normalizedDirection: Vector2, unit: Unit) : this(point.x, point.y, normalizedDirection.x, normalizedDirection.y)

    /** Checks if [this] and [other]are equals with an [epsilon] difference */
    override fun isAlmostEquals(other: Ray, epsilon: Double): Boolean =
        this.point.isAlmostEquals(other.point, epsilon) && this.direction.isAlmostEquals(other.direction, epsilon)

    /** Checks if [this] and [other]are equals with an [epsilon] tolerance */
    fun transformed(m: Matrix): Ray = Ray(m.transform(point), m.deltaTransform(direction).normalized)

    /** Converts this [Ray] into a [Line] of a specific [length] starting by [point] */
    fun toLine(length: Double = 100000.0): Line = Line(point, point + direction * length)

    override fun toString(): String = "Ray($point, $angle)"
}

typealias Ray3 = Ray3F

data class Ray3F(val pos: Vector3F, val dir: Vector3F) {//: Shape3D {
    //override val center: Vector3 get() = pos
    //override val volume: Float = 0f
}

@KormaMutableApi
fun Ray3F.intersectRayAABox1(box: AABB3D) : Boolean {
    val ray = this
    // r.dir is unit direction vector of ray
    val dirfrac = ray.dir.inv()
    // lb is the corner of AABB with minimal coordinates - left bottom, rt is maximal corner
    // r.org is origin of ray
    val t1 = (box.min.x - ray.pos.x) * dirfrac.x
    val t2 = (box.max.x - ray.pos.x) * dirfrac.x
    val t3 = (box.min.y - ray.pos.y) * dirfrac.y
    val t4 = (box.max.y - ray.pos.y) * dirfrac.y
    val t5 = (box.min.z - ray.pos.z) * dirfrac.z
    val t6 = (box.max.z - ray.pos.z) * dirfrac.z

    val tmin =
        kotlin.math.max(kotlin.math.max(kotlin.math.min(t1, t2), kotlin.math.min(t3, t4)), kotlin.math.min(t5, t6))
    val tmax =
        kotlin.math.min(kotlin.math.min(kotlin.math.max(t1, t2), kotlin.math.max(t3, t4)), kotlin.math.max(t5, t6))

    // if tmax < 0, ray (line) is intersecting AABB, but whole AABB is behing us
    if (tmax < 0) {
        val t = tmax
        return false
    }

    // if tmin > tmax, ray doesn't intersect AABB
    if (tmin > tmax) {
        val t = tmax
        return false
    }

    val t = tmin
    return true

}
