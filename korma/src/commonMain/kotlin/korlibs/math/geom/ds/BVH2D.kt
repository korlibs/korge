package korlibs.math.geom.ds

import korlibs.datastructure.*
import korlibs.datastructure.ds.*
import korlibs.math.geom.*

fun BVHIntervals.toRectangle(): Rectangle = Rectangle(a(0), a(1), b(0), b(1))
fun Rectangle.toBVH(out: BVHIntervals = BVHIntervals(2)): BVHIntervals {
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
    ): FastArrayList<BVH.IntersectResult<T>> = bvh.intersect(ray.toBVH(), return_array)

    fun search(
        rect: Rectangle,
        return_array: FastArrayList<BVH.Node<T>> = fastArrayListOf(),
    ): FastArrayList<BVH.Node<T>> = bvh.search(intervals = rect.toBVH(), return_array = return_array)

    fun insertOrUpdate(rect: Rectangle, obj: T) = bvh.insertOrUpdate(rect.toBVH(), obj)

    fun remove(rect: Rectangle, obj: T? = null) = bvh.remove(rect.toBVH(), obj = obj)

    fun remove(obj: T) = bvh.remove(obj)

    fun getObjectBounds(obj: T) = bvh.getObjectBounds(obj)?.toRectangle()

    fun debug() {
        bvh.debug()
    }
}
