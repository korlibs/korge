package korlibs.math.geom.ds

import korlibs.datastructure.*
import korlibs.datastructure.ds.*
import korlibs.math.geom.*

/**
 * A Bounding Volume Hierarchy implementation for 3D.
 * It uses [AABB3D] to describe volumes and [MRay3D] for raycasting.
 */
open class BVH3D<T>(
    val allowUpdateObjects: Boolean = true
) {
    val bvh = BVH<T>(dimensions = 3, allowUpdateObjects = allowUpdateObjects)

    fun intersectRay(ray: Ray3D, rect: AABB3D? = null): BVHRect? = bvh.intersectRay(ray.toBVH(), rect?.toBVH())

    fun envelope(): AABB3D = bvh.envelope().toAABB3D()

    fun intersect(
        ray: Ray3D,
        return_array: FastArrayList<BVH.IntersectResult<T>> = fastArrayListOf(),
    ): FastArrayList<BVH.IntersectResult<T>> = bvh.intersect(ray.toBVH(), return_array)

    fun search(
        rect: AABB3D,
        return_array: FastArrayList<BVH.Node<T>> = fastArrayListOf(),
    ): FastArrayList<BVH.Node<T>> = bvh.search(intervals = rect.toBVH(), return_array = return_array)
    fun insertOrUpdate(rect: AABB3D, obj: T): Unit = bvh.insertOrUpdate(rect.toBVH(), obj)
    fun remove(rect: AABB3D, obj: T? = null): FastArrayList<BVH.Node<T>> = bvh.remove(rect.toBVH(), obj = obj)
    fun remove(obj: T): Unit = bvh.remove(obj)
    fun getObjectBounds(obj: T): AABB3D? = bvh.getObjectBounds(obj)?.toAABB3D()
    fun debug() {
        bvh.debug()
    }
}

fun BVHIntervals.toAABB3D(): AABB3D = AABB3D(Vector3(min(0), min(1), min(2)), Vector3(aPlusB(0), aPlusB(1), aPlusB(2)))
fun BVHRect.toAABB3D(): AABB3D = AABB3D(min.toVector3(), max.toVector3())
fun AABB3D.toBVH(out: BVHIntervals = BVHIntervals(3)): BVHRect {
    out.setTo(minX, sizeX, minY, sizeY, minZ, sizeZ)
    return BVHRect(out)
}
fun Ray3D.toBVH(out: BVHIntervals = BVHIntervals(3)): BVHRay {
    out.setTo(pos.x, dir.x, pos.y, dir.y, pos.z, dir.z)
    return BVHRay(out)
}
fun BVHRay.toRay3D(): Ray3D {
    return Ray3D(this.pos.toVector3(), this.pos.toVector3())
}
fun BVHVector.toVector3(): Vector3 {
    checkDimensions(3)
    return Vector3(this[0], this[1], this[2])
}
