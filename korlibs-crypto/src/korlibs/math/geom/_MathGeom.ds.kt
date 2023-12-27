@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.geom.ds

import korlibs.datastructure.*
import korlibs.datastructure.ds.*
import korlibs.math.annotations.*
import korlibs.math.geom.*

inline operator fun <T> Array2<T>.get(p: Point): T = get(p.x.toInt(), p.y.toInt())
inline operator fun <T> Array2<T>.set(p: Point, value: T) = set(p.x.toInt(), p.y.toInt(), value)
inline fun <T> Array2<T>.tryGet(p: Point): T? = tryGet(p.x.toInt(), p.y.toInt())
inline fun <T> Array2<T>.trySet(p: Point, value: T) = trySet(p.x.toInt(), p.y.toInt(), value)
inline operator fun <T> Array2<T>.get(p: PointInt): T = get(p.x, p.y)
inline operator fun <T> Array2<T>.set(p: PointInt, value: T) = set(p.x, p.y, value)
inline fun <T> Array2<T>.tryGet(p: PointInt): T? = tryGet(p.x, p.y)
inline fun <T> Array2<T>.trySet(p: PointInt, value: T) = trySet(p.x, p.y, value)

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

data class Segment1D(val start: Double, val end: Double) {
    constructor(start: Float, end: Float) : this(start.toDouble(), end.toDouble())
    val size: Double get() = end - start
}
data class Ray1D(val start: Double, val dir: Double) {
    constructor(start: Float, dir: Float) : this(start.toDouble(), dir.toDouble())
}

fun Segment1D.toBVH(): BVHRect = BVHRect(BVHIntervals(start, size))
fun Ray1D.toBVH(): BVHRay = BVHRay(BVHIntervals(start, dir))

fun BVHRect.toSegment1D(): Segment1D = Segment1D(min(0), max(0))
fun BVHRay.toRay1D(): Ray1D = Ray1D(pos(0), dir(0))


/**
 * A Bounding Volume Hierarchy implementation for 2D.
 * It uses [Rectangle] to describe volumes and [Ray] for raycasting.
 */
open class BVH2D<T>(
    val allowUpdateObjects: Boolean = true
) {
    val bvh = BVH<T>(dimensions = 2, allowUpdateObjects = allowUpdateObjects)

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

    fun insertOrUpdate(rect: Rectangle, obj: T): Unit = bvh.insertOrUpdate(rect.toBVH(), obj)

    fun remove(rect: Rectangle, obj: T? = null) = bvh.remove(rect.toBVH(), obj = obj)

    fun remove(obj: T) = bvh.remove(obj)

    fun getObjectBounds(obj: T) = bvh.getObjectBounds(obj)?.toRectangle()

    fun debug() {
        bvh.debug()
    }
}

fun BVHRect.toRectangle(): Rectangle = Rectangle(min(0), min(1), size(0), size(1))
@Deprecated("Use BVHRect signature")
fun BVHIntervals.toRectangle(): Rectangle = Rectangle(min(0), min(1), size(0), size(1))
fun Rectangle.toBVH(out: BVHIntervals = BVHIntervals(2)): BVHRect {
    out.setTo(x, width, y, height)
    return BVHRect(out)
}
fun Ray.toBVH(out: BVHIntervals = BVHIntervals(2)): BVHRay {
    out.setTo(point.x, direction.x, point.y, direction.y)
    return BVHRay(out)
}
fun BVHRay.toRay(): Ray = Ray(pos.toVector2(), dir.toVector2())
fun BVHVector.toVector2(): Vector2D {
    checkDimensions(2)
    return Vector2D(this[0], this[1])
}

/**
 * A Bounding Volume Hierarchy implementation for 3D.
 * It uses [AABB3D] to describe volumes and [MRay3D] for raycasting.
 */
open class BVH3D<T>(
    val allowUpdateObjects: Boolean = true
) {
    val bvh = BVH<T>(dimensions = 3, allowUpdateObjects = allowUpdateObjects)

    fun intersectRay(ray: Ray3F, rect: AABB3D? = null): BVHRect? = bvh.intersectRay(ray.toBVH(), rect?.toBVH())

    fun envelope(): AABB3D = bvh.envelope().toAABB3D()

    fun intersect(
        ray: Ray3F,
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

fun BVHIntervals.toAABB3D(): AABB3D = AABB3D(Vector3F(min(0), min(1), min(2)), Vector3F(aPlusB(0), aPlusB(1), aPlusB(2)))
fun BVHRect.toAABB3D(): AABB3D = AABB3D(min.toVector3(), max.toVector3())
fun AABB3D.toBVH(out: BVHIntervals = BVHIntervals(3)): BVHRect {
    out.setTo(minX.toDouble(), sizeX.toDouble(), minY.toDouble(), sizeY.toDouble(), minZ.toDouble(), sizeZ.toDouble())
    return BVHRect(out)
}
fun Ray3F.toBVH(out: BVHIntervals = BVHIntervals(3)): BVHRay {
    out.setTo(pos.x.toDouble(), dir.x.toDouble(), pos.y.toDouble(), dir.y.toDouble(), pos.z.toDouble(), dir.z.toDouble())
    return BVHRay(out)
}
fun BVHRay.toRay3D(): Ray3F {
    return Ray3F(this.pos.toVector3(), this.pos.toVector3())
}
fun BVHVector.toVector3(): Vector3F {
    checkDimensions(3)
    return Vector3F(this[0], this[1], this[2])
}
