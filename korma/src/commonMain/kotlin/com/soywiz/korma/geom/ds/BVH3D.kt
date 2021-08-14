package com.soywiz.korma.geom.ds

import com.soywiz.kds.*
import com.soywiz.korma.geom.*

fun BVHIntervals.toAABB3D(out: AABB3D = AABB3D()): AABB3D {
    out.min.setTo(a(0), a(1), a(2))
    out.max.setTo(aPlusB(0), aPlusB(1), aPlusB(2))
    return out
}
fun AABB3D.toBVH(out: BVHIntervals = BVHIntervals(3)): BVHIntervals {
    out.setTo(
        minX.toDouble(), sizeX.toDouble(),
        minY.toDouble(), sizeY.toDouble(),
        minZ.toDouble(), sizeZ.toDouble(),
    )
    return out
}
fun Ray3D.toBVH(out: BVHIntervals = BVHIntervals(3)): BVHIntervals {
    out.setTo(
        pos.x.toDouble(), dir.x.toDouble(),
        pos.y.toDouble(), dir.y.toDouble(),
        pos.z.toDouble(), dir.z.toDouble()
    )
    return out
}

/**
 * A Bounding Volume Hierarchy implementation for 3D.
 * It uses [AABB3D] to describe volumes and [Ray3D] for raycasting.
 */
open class BVH3D<T>(
    val allowUpdateObjects: Boolean = true
) {
    val bvh = BVH<T>(allowUpdateObjects = allowUpdateObjects)

    fun intersectRay(ray: Ray3D, rect: AABB3D? = null) = bvh.intersectRay(ray.toBVH(), rect?.toBVH())

    fun envelope(): AABB3D = bvh.envelope().toAABB3D()

    fun intersect(
        ray: Ray3D,
        return_array: FastArrayList<BVH.IntersectResult<T>> = fastArrayListOf(),
    ) = bvh.intersect(ray.toBVH(), return_array)

    fun search(
        rect: AABB3D,
        return_array: FastArrayList<BVH.Node<T>> = fastArrayListOf(),
    ): FastArrayList<BVH.Node<T>> = bvh.search(intervals = rect.toBVH(), return_array = return_array)

    fun insertOrUpdate(rect: AABB3D, obj: T) = bvh.insertOrUpdate(rect.toBVH(), obj)

    fun remove(rect: AABB3D, obj: T? = null) = bvh.remove(rect.toBVH(), obj = obj)

    fun remove(obj: T) = bvh.remove(obj)

    fun getObjectBounds(obj: T, out: AABB3D = AABB3D()) = bvh.getObjectBounds(obj)?.toAABB3D(out)

    fun debug() {
        bvh.debug()
    }
}
