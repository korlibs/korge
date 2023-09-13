package korlibs.math.geom.ds

import korlibs.datastructure.*
import korlibs.datastructure.ds.*
import korlibs.math.geom.*

/**
 * A Bounding Volume Hierarchy implementation for 1D.
 * It uses [Segment1D] to describe volumes and [Ray] for raycasting.
 */
open class BVH1D<T>(
    val allowUpdateObjects: Boolean = true
) {
    val bvh = BVH<T>(dimensions = 1, allowUpdateObjects = allowUpdateObjects)

    fun intersectRay(ray: Ray1D, rect: Rectangle? = null): BVHRect? = bvh.intersectRay(ray.toBVH(), rect?.toBVH())
    fun envelope(): Segment1D = bvh.envelope().toSegment1D()

    fun intersect(
        ray: Ray1D,
        returnArray: FastArrayList<BVH.IntersectResult<T>> = fastArrayListOf(),
    ): FastArrayList<BVH.IntersectResult<T>> = bvh.intersect(ray.toBVH(), returnArray)

    fun search(
        segment: Segment1D,
        returnArray: FastArrayList<BVH.Node<T>> = fastArrayListOf(),
    ): FastArrayList<BVH.Node<T>> = bvh.search(intervals = segment.toBVH(), return_array = returnArray)

    fun insertOrUpdate(segment: Segment1D, obj: T) = bvh.insertOrUpdate(segment.toBVH(), obj)

    fun remove(segment: Segment1D, obj: T? = null): FastArrayList<BVH.Node<T>> = bvh.remove(segment.toBVH(), obj = obj)

    fun remove(obj: T): Unit = bvh.remove(obj)

    fun getObjectBounds(obj: T): Segment1D? = bvh.getObjectBoundsRect(obj)?.toSegment1D()

    fun debug() {
        bvh.debug()
    }
}

data class Segment1D(val start: Float, val end: Float) {
    val size: Float get() = end - start
}
data class Ray1D(val start: Float, val dir: Float)

fun Segment1D.toBVH(): BVHRect = BVHRect(BVHIntervals(start, size))
fun Ray1D.toBVH(): BVHRay = BVHRay(BVHIntervals(start, dir))

fun BVHRect.toSegment1D(): Segment1D = Segment1D(min(0), max(0))
fun BVHRay.toRay1D(): Ray1D = Ray1D(pos(0), dir(0))
