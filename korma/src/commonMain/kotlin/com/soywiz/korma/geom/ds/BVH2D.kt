package com.soywiz.korma.geom.ds

import com.soywiz.kds.*
import com.soywiz.korma.geom.*

fun BVHIntervals.toRectangle(out: Rectangle = Rectangle()) = out.setTo(a(0), a(1), b(0), b(1))
fun IRectangle.toBVH(out: BVHIntervals = BVHIntervals(2)): BVHIntervals {
    out.setTo(x, width, y, height)
    return out
}
fun Ray.toBVH(out: BVHIntervals = BVHIntervals(2)): BVHIntervals {
    out.setTo(point.x, direction.x, point.y, direction.y)
    return out
}

/**
 * A Bounding Volume Hierarchy implementation for 2D.
 * It uses [Rectangle] to describe volumes and [Ray] for raycasting.
 */
open class BVH2D<T>(
    val allowUpdateObjects: Boolean = true
) {
    val bvh = BVH<T>(allowUpdateObjects = allowUpdateObjects)

    fun intersectRay(ray: Ray, rect: Rectangle? = null) = bvh.intersectRay(ray.toBVH(), rect?.toBVH())

    fun envelope(): Rectangle = bvh.envelope().toRectangle()

    fun intersect(
        ray: Ray,
        return_array: FastArrayList<BVH.IntersectResult<T>> = fastArrayListOf(),
    ) = bvh.intersect(ray.toBVH(), return_array)

    fun search(
        rect: IRectangle,
        return_array: FastArrayList<BVH.Node<T>> = fastArrayListOf(),
    ): FastArrayList<BVH.Node<T>> = bvh.search(intervals = rect.toBVH(), return_array = return_array)

    fun insertOrUpdate(rect: IRectangle, obj: T) = bvh.insertOrUpdate(rect.toBVH(), obj)

    fun remove(rect: IRectangle, obj: T? = null) = bvh.remove(rect.toBVH(), obj = obj)

    fun remove(obj: T) = bvh.remove(obj)

    fun getObjectBounds(obj: T, out: Rectangle = Rectangle()) = bvh.getObjectBounds(obj)?.toRectangle(out)

    fun debug() {
        bvh.debug()
    }
}
