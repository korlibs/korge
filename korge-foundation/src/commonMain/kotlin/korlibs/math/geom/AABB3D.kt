package korlibs.math.geom

import korlibs.math.geom.shape.*
import kotlin.math.*

data class AABB3D(val min: Vector3F = Vector3F(), val max: Vector3F = Vector3F()) : Shape3D {
    val minX: Float get() = min.x
    val minY: Float get() = min.y
    val minZ: Float get() = min.z

    val maxX: Float get() = max.x
    val maxY: Float get() = max.y
    val maxZ: Float get() = max.z

    val sizeX: Float get() = maxX - minX
    val sizeY: Float get() = maxY - minY
    val sizeZ: Float get() = maxZ - minZ

    companion object {
        operator fun invoke(min: Float = Float.POSITIVE_INFINITY, max: Float = Float.NEGATIVE_INFINITY): AABB3D =
            AABB3D(Vector3F(min, min, min), Vector3F(max, max, max))

        fun fromSphere(pos: Vector3F, radius: Float): AABB3D = AABB3D(
            Vector3F(pos.x - radius, pos.y - radius, pos.z - radius),
            Vector3F(pos.x + radius, pos.y + radius, pos.z + radius)
        )
    }

    fun expandedToFit(that: AABB3D): AABB3D {
        val a = this
        val b = that
        return AABB3D(
            min = Vector3F(min(a.minX, b.minX), min(a.minY, b.minY), min(a.minZ, b.minZ)),
            max = Vector3F(max(a.maxX, b.maxX), max(a.maxY, b.maxY), max(a.maxZ, b.maxZ)),
        )
    }

    fun intersectsSphere(sphere: Sphere3D): Boolean = intersectsSphere(sphere.center, sphere.radius)
    fun intersectsSphere(origin: Vector3F, radius: Float): Boolean = !(origin.x + radius < minX ||
        origin.y + radius < minY ||
        origin.z + radius < minZ ||
        origin.x - radius > maxX ||
        origin.y - radius > maxY ||
        origin.z - radius > maxZ)

    fun intersectsAABB(box: AABB3D): Boolean = max.x > box.min.x && min.x < box.max.x &&
        max.y > box.min.y && min.y < box.max.y &&
        max.z > box.min.z && min.z < box.max.z

    override val center: Vector3F get() = (min + max) * 0.5f
    override val volume: Float get() {
        val v = (max - min)
        return v.x * v.y * v.z
    }
}
