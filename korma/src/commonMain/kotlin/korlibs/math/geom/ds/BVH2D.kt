package korlibs.math.geom.ds

import korlibs.datastructure.*
import korlibs.datastructure.ds.*
import korlibs.math.geom.*

fun BVHIntervals.toRectangle(out: MRectangle = MRectangle()) = out.setTo(a(0), a(1), b(0), b(1))
fun MRectangle.toBVH(out: BVHIntervals = BVHIntervals(2)): BVHIntervals {
    out.setTo(x, width, y, height)
    return out
}
fun MRay.toBVH(out: BVHIntervals = BVHIntervals(2)): BVHIntervals {
    out.setTo(point.x, direction.x, point.y, direction.y)
    return out
}

/**
 * A Bounding Volume Hierarchy implementation for 2D.
 * It uses [MRectangle] to describe volumes and [MRay] for raycasting.
 */
open class BVH2D<T>(
    val allowUpdateObjects: Boolean = true
) {
    val bvh = BVH<T>(allowUpdateObjects = allowUpdateObjects)

    fun intersectRay(ray: MRay, rect: MRectangle? = null) = bvh.intersectRay(ray.toBVH(), rect?.toBVH())

    fun envelope(): MRectangle = bvh.envelope().toRectangle()

    fun intersect(
        ray: MRay,
        return_array: FastArrayList<BVH.IntersectResult<T>> = fastArrayListOf(),
    ): FastArrayList<BVH.IntersectResult<T>> = bvh.intersect(ray.toBVH(), return_array)

    fun search(
        rect: MRectangle,
        return_array: FastArrayList<BVH.Node<T>> = fastArrayListOf(),
    ): FastArrayList<BVH.Node<T>> = bvh.search(intervals = rect.toBVH(), return_array = return_array)

    fun insertOrUpdate(rect: MRectangle, obj: T) = bvh.insertOrUpdate(rect.toBVH(), obj)

    fun remove(rect: MRectangle, obj: T? = null) = bvh.remove(rect.toBVH(), obj = obj)

    fun remove(obj: T) = bvh.remove(obj)

    fun getObjectBounds(obj: T, out: MRectangle = MRectangle()) = bvh.getObjectBounds(obj)?.toRectangle(out)

    fun debug() {
        bvh.debug()
    }
}