package com.soywiz.korma.geom

import com.soywiz.korma.geom.ds.*
import kotlin.math.*


data class AABB3D(val min: Vector3 = Vector3(), val max: Vector3) {
    var minX get() = min.x; set(value) { min.x = value }
    var minY get() = min.y; set(value) { min.y = value }
    var minZ get() = min.z; set(value) { min.z = value }

    var maxX get() = max.x; set(value) { max.x = value }
    var maxY get() = max.y; set(value) { max.y = value }
    var maxZ get() = max.z; set(value) { max.z = value }

    val sizeX get() = maxX - minX
    val sizeY get() = maxY - minY
    val sizeZ get() = maxZ - minZ

    companion object {
        operator fun invoke(min: Float = Float.POSITIVE_INFINITY, max: Float = Float.NEGATIVE_INFINITY): AABB3D =
            AABB3D(Vector3(min, min, min), Vector3(max, max, max))

        fun fromSphere(pos: IVector3, radius: Float): AABB3D = AABB3D(
            Vector3(pos.x - radius, pos.y - radius, pos.z - radius),
            Vector3(pos.x + radius, pos.y + radius, pos.z + radius)
        )
    }

    fun setX(min: Float, max: Float) {
        this.minX = min
        this.maxX = max
    }
    fun setY(min: Float, max: Float) {
        this.minY = min
        this.maxY = max
    }
    fun setZ(min: Float, max: Float) {
        this.minZ = min
        this.maxZ = max
    }

    fun copyFrom(other: AABB3D) {
        this.min.copyFrom(other.min)
        this.max.copyFrom(other.max)
    }

    fun expandBy(that: AABB3D) {
        val a = this
        val b = that
        a.minX = min(a.minX, b.minX)
        a.minY = min(a.minY, b.minY)
        a.minZ = min(a.minZ, b.minZ)
        a.maxX = max(a.maxX, b.maxX)
        a.maxY = max(a.maxY, b.maxY)
        a.maxZ = max(a.maxZ, b.maxZ)
    }

    fun expandToFit(that: AABB3D) = expandBy(that)

    fun expandedBy(that: AABB3D, out: AABB3D = AABB3D()): AABB3D {
        out.copyFrom(this)
        out.expandBy(that)
        return out
    }

    fun intersectsSphere(sphere: Sphere3D): Boolean = intersectsSphere(sphere.origin, sphere.radius)
    fun intersectsSphere(origin: Vector3, radius: Float): Boolean = !(origin.x + radius < minX ||
        origin.y + radius < minY ||
        origin.z + radius < minZ ||
        origin.x - radius > maxX ||
        origin.y - radius > maxY ||
        origin.z - radius > maxZ)

    fun intersectsAABB(box: AABB3D): Boolean = max.x > box.min.x && min.x < box.max.x &&
        max.y > box.min.y && min.y < box.max.y &&
        max.z > box.min.z && min.z < box.max.z

    fun clone() = AABB3D(min.clone(), max.clone())
}
