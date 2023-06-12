package korlibs.math.geom.ds

import korlibs.datastructure.*
import korlibs.datastructure.ds.*
import korlibs.math.geom.*

data class BVH1DSegment(val start: Float, val end: Float) {
    val size: Float get() = end - start
}
data class BVH1DRay(val start: Float, val dir: Float)

fun BVH1DSegment.toBVH(): BVHIntervals = BVHIntervals(start, size)
fun BVH1DRay.toBVH(): BVHIntervals = BVHIntervals(start, dir)
fun BVHIntervals.toSegment(): BVH1DSegment = BVH1DSegment(min(0), max(0))

/**
 * A Bounding Volume Hierarchy implementation for 1D.
 * It uses [BVH1DSegment] to describe volumes and [Ray] for raycasting.
 */
open class BVH1D<T>(
    val allowUpdateObjects: Boolean = true
) {
    val bvh = BVH<T>(dimensions = 1, allowUpdateObjects = allowUpdateObjects)

    fun intersectRay(ray: BVH1DRay, rect: Rectangle? = null): BVHIntervals? = bvh.intersectRay(ray.toBVH(), rect?.toBVH())
    fun envelope(): BVH1DSegment = bvh.envelope().toSegment()

    fun intersect(
        ray: BVH1DRay,
        returnArray: FastArrayList<BVH.IntersectResult<T>> = fastArrayListOf(),
    ): FastArrayList<BVH.IntersectResult<T>> = bvh.intersect(ray.toBVH(), returnArray)

    fun search(
        segment: BVH1DSegment,
        returnArray: FastArrayList<BVH.Node<T>> = fastArrayListOf(),
    ): FastArrayList<BVH.Node<T>> = bvh.search(intervals = segment.toBVH(), return_array = returnArray)

    fun insertOrUpdate(segment: BVH1DSegment, obj: T) = bvh.insertOrUpdate(segment.toBVH(), obj)

    fun remove(segment: BVH1DSegment, obj: T? = null): FastArrayList<BVH.Node<T>> = bvh.remove(segment.toBVH(), obj = obj)

    fun remove(obj: T): Unit = bvh.remove(obj)

    fun getObjectBounds(obj: T): BVH1DSegment? = bvh.getObjectBounds(obj)?.toSegment()

    fun debug() {
        bvh.debug()
    }
}
