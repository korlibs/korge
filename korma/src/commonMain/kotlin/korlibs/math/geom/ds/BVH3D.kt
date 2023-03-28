package korlibs.math.geom.ds

import korlibs.datastructure.*
import korlibs.datastructure.ds.*
import korlibs.math.geom.*

fun BVHIntervals.toAABB3D(out: MAABB3D = MAABB3D()): MAABB3D {
    out.min.setTo(a(0), a(1), a(2))
    out.max.setTo(aPlusB(0), aPlusB(1), aPlusB(2))
    return out
}
fun MAABB3D.toBVH(out: BVHIntervals = BVHIntervals(3)): BVHIntervals {
    out.setTo(minX, sizeX, minY, sizeY, minZ, sizeZ)
    return out
}
fun Ray3D.toBVH(out: BVHIntervals = BVHIntervals(3)): BVHIntervals {
    out.setTo(pos.x, dir.x, pos.y, dir.y, pos.z, dir.z)
    return out
}

/**
 * A Bounding Volume Hierarchy implementation for 3D.
 * It uses [MAABB3D] to describe volumes and [MRay3D] for raycasting.
 */
open class BVH3D<T>(
    val allowUpdateObjects: Boolean = true
) {
    val bvh = BVH<T>(allowUpdateObjects = allowUpdateObjects)

    fun intersectRay(ray: Ray3D, rect: MAABB3D? = null) = bvh.intersectRay(ray.toBVH(), rect?.toBVH())

    fun envelope(): MAABB3D = bvh.envelope().toAABB3D()

    fun intersect(
        ray: Ray3D,
        return_array: FastArrayList<BVH.IntersectResult<T>> = fastArrayListOf(),
    ) = bvh.intersect(ray.toBVH(), return_array)

    fun search(
        rect: MAABB3D,
        return_array: FastArrayList<BVH.Node<T>> = fastArrayListOf(),
    ): FastArrayList<BVH.Node<T>> = bvh.search(intervals = rect.toBVH(), return_array = return_array)

    fun insertOrUpdate(rect: MAABB3D, obj: T) = bvh.insertOrUpdate(rect.toBVH(), obj)

    fun remove(rect: MAABB3D, obj: T? = null) = bvh.remove(rect.toBVH(), obj = obj)

    fun remove(obj: T) = bvh.remove(obj)

    fun getObjectBounds(obj: T, out: MAABB3D = MAABB3D()): MAABB3D? = bvh.getObjectBounds(obj)?.toAABB3D(out)

    fun debug() {
        bvh.debug()
    }
}
