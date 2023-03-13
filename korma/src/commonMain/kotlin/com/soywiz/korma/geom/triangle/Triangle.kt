package com.soywiz.korma.geom.triangle

import com.soywiz.korma.geom.*
import kotlin.math.*

val Triangle.center get() = MPoint((p0.x + p1.x + p2.x) / 3, (p0.y + p1.y + p2.y) / 3)

interface Triangle {
    val p0: MPoint
    val p1: MPoint
    val p2: MPoint

    data class Base(override val p0: MPoint, override val p1: MPoint, override val p2: MPoint) : Triangle

    companion object {
        private const val EPSILON: Double = 1e-12

        fun area(p1: MPoint, p2: MPoint, p3: MPoint): Double = area(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)

        fun area(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double {
            val a = bx - ax
            val b = by - ay
            val c = cx - ax
            val d = cy - ay
            return abs(a * d - c * b) / 2f
        }

        fun getNotCommonVertexIndex(t1: Triangle, t2: Triangle): Int {
            var sum = 0
            var index: Int = -1
            if (!t2.containsPoint(t1.point(0))) {
                index = 0
                sum++
            }
            if (!t2.containsPoint(t1.point(1))) {
                index = 1
                sum++
            }
            if (!t2.containsPoint(t1.point(2))) {
                index = 2
                sum++
            }
            if (sum != 1) throw Error("Triangles are not contiguous")
            return index
        }

        fun getNotCommonVertex(t1: Triangle, t2: Triangle): MPoint = t1.point(getNotCommonVertexIndex(t1, t2))

        fun getUniquePointsFromTriangles(triangles: List<Triangle>) = triangles.flatMap { listOf(it.p0, it.p1, it.p2) }.distinct()

        fun insideIncircle(pa: MPoint, pb: MPoint, pc: MPoint, pd: MPoint): Boolean {
            val adx = pa.x - pd.x
            val ady = pa.y - pd.y
            val bdx = pb.x - pd.x
            val bdy = pb.y - pd.y

            val adxbdy = adx * bdy
            val bdxady = bdx * ady
            val oabd = adxbdy - bdxady

            if (oabd <= 0) return false

            val cdx = pc.x - pd.x
            val cdy = pc.y - pd.y

            val cdxady = cdx * ady
            val adxcdy = adx * cdy
            val ocad = cdxady - adxcdy

            if (ocad <= 0) return false

            val bdxcdy = bdx * cdy
            val cdxbdy = cdx * bdy

            val alift = adx * adx + ady * ady
            val blift = bdx * bdx + bdy * bdy
            val clift = cdx * cdx + cdy * cdy

            val det = alift * (bdxcdy - cdxbdy) + blift * ocad + clift * oabd
            return det > 0
        }

        fun inScanArea(pa: MPoint, pb: MPoint, pc: MPoint, pd: MPoint): Boolean {
            val pdx = pd.x
            val pdy = pd.y
            val adx = pa.x - pdx
            val ady = pa.y - pdy
            val bdx = pb.x - pdx
            val bdy = pb.y - pdy

            val adxbdy = adx * bdy
            val bdxady = bdx * ady
            val oabd = adxbdy - bdxady

            if (oabd <= EPSILON) return false

            val cdx = pc.x - pdx
            val cdy = pc.y - pdy

            val cdxady = cdx * ady
            val adxcdy = adx * cdy
            val ocad = cdxady - adxcdy

            if (ocad <= EPSILON) return false

            return true
        }
    }
}

fun Triangle.point(index: Int): MPoint = when (index) {
    0 -> p0
    1 -> p1
    2 -> p2
    else -> error("Invalid triangle point index $index")
}
/**
 * Test if this Triangle contains the Point2d object given as parameter as its vertices.
 *
 * @return <code>True</code> if the Point2d objects are of the Triangle's vertices,
 *         <code>false</code> otherwise.
 */
fun Triangle.containsPoint(point: Point): Boolean = (point == p0.point) || (point == p1.point) || (point == p2.point)
fun Triangle.containsPoint(point: MPoint): Boolean = (point == p0) || (point == p1) || (point == p2)
/**
 * Test if this Triangle contains the Edge object given as parameters as its bounding edges.
 * @return <code>True</code> if the Edge objects are of the Triangle's bounding
 *         edges, <code>false</code> otherwise.
 */
// In a triangle to check if contains and edge is enough to check if it contains the two vertices.
fun Triangle.containsEdge(edge: Edge): Boolean = containsEdgePoints(edge.p, edge.q)

// In a triangle to check if contains and edge is enough to check if it contains the two vertices.
fun Triangle.containsEdgePoints(p1: Point, p2: Point): Boolean = containsPoint(p1) && containsPoint(p2)
fun Triangle.containsEdgePoints(p1: MPoint, p2: MPoint): Boolean = containsPoint(p1) && containsPoint(p2)

private fun _product(p1x: Double, p1y: Double, p2x: Double, p2y: Double, p3x: Double, p3y: Double): Double = (p1x - p3x) * (p2y - p3y) - (p1y - p3y) * (p2x - p3x)
private fun _product(p1: MPoint, p2: MPoint, p3: MPoint): Double = _product(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)

fun Triangle.pointInsideTriangle(x: Double, y: Double): Boolean {
    val sign0 = _product(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y)
    val sign1 = _product(p0.x, p0.y, p1.x, p1.y, x, y)
    val sign2 = _product(p1.x, p1.y, p2.x, p2.y, x, y)
    val sign3 = _product(p2.x, p2.y, p0.x, p0.y, x, y)
    return if (sign0 >= 0) (sign1 >= 0) && (sign2 >= 0) && (sign3 >= 0) else (sign1 <= 0) && (sign2 <= 0) && (sign3 <= 0)
}

fun Triangle.pointInsideTriangle(pp: Point): Boolean = pointInsideTriangle(pp.xD, pp.yD)
fun Triangle.pointInsideTriangle(pp: MPoint): Boolean = pointInsideTriangle(pp.x, pp.y)

// Optimized?
fun Triangle.getPointIndexOffsetNoThrow(p: MPoint, offset: Int = 0, notFound: Int = Int.MIN_VALUE): Int {
    var no: Int = offset
    for (n in 0 until 3) {
        while (no < 0) no += 3
        while (no > 2) no -= 3
        if (p == (this.point(n))) return no
        no++
    }
    return notFound
}
fun Triangle.getPointIndexOffsetNoThrow(p: Point, offset: Int = 0, notFound: Int = Int.MIN_VALUE): Int {
    var no: Int = offset
    for (n in 0 until 3) {
        while (no < 0) no += 3
        while (no > 2) no -= 3
        if (p == (this.point(n).point)) return no
        no++
    }
    return notFound
}

fun Triangle.getPointIndexOffset(p: Point, offset: Int = 0): Int {
    val v = getPointIndexOffsetNoThrow(p, offset, Int.MIN_VALUE)
    if (v == Int.MIN_VALUE) throw Error("Point2d not in triangle")
    return v
}
fun Triangle.getPointIndexOffset(p: MPoint, offset: Int = 0): Int {
    val v = getPointIndexOffsetNoThrow(p, offset, Int.MIN_VALUE)
    if (v == Int.MIN_VALUE) throw Error("Point2d not in triangle")
    return v
}

fun Triangle.pointCW(p: MPoint): MPoint = this.point(getPointIndexOffset(p, -1))
fun Triangle.pointCCW(p: MPoint): MPoint = this.point(getPointIndexOffset(p, +1))
fun Triangle.oppositePoint(t: Triangle, p: MPoint): MPoint = this.pointCW(t.pointCW(p))

fun Triangle.pointCW(p: Point): Point = this.point(getPointIndexOffset(p, -1)).point
fun Triangle.pointCCW(p: Point): Point = this.point(getPointIndexOffset(p, +1)).point
fun Triangle.oppositePoint(t: Triangle, p: Point): Point = this.pointCW(t.pointCW(p))

fun Triangle(p0: MPoint, p1: MPoint, p2: MPoint, fixOrientation: Boolean = false, checkOrientation: Boolean = true): Triangle {
    @Suppress("NAME_SHADOWING")
    var p1 = p1
    @Suppress("NAME_SHADOWING")
    var p2 = p2
    if (fixOrientation) {
        if (Orientation.orient2d(p0, p1, p2) == Orientation.CLOCK_WISE) {
            val pt = p2
            p2 = p1
            p1 = pt
            //println("Fixed orientation");
        }
    }
    if (checkOrientation && Orientation.orient2d(p2, p1, p0) != Orientation.CLOCK_WISE) throw(Error("Triangle must defined with Orientation.CW"))
    return Triangle.Base(p0, p1, p2)
}

val Triangle.area: Double get() = Triangle.area(p0, p1, p2)

/** Alias for getPointIndexOffset */
fun Triangle.index(p: Point): Int = this.getPointIndexOffsetNoThrow(p, 0, -1)
fun Triangle.index(p: MPoint): Int = this.getPointIndexOffsetNoThrow(p, 0, -1)

fun Triangle.edgeIndex(p1: MPoint, p2: MPoint): Int = edgeIndex(p1.point, p2.point)

fun Triangle.edgeIndex(p1: Point, p2: Point): Int {
    when (p1) {
        this.point(0).point -> {
            if (p2 == this.point(1).point) return 2
            if (p2 == this.point(2).point) return 1
        }
        this.point(1).point -> {
            if (p2 == this.point(2).point) return 0
            if (p2 == this.point(0).point) return 2
        }
        this.point(2).point -> {
            if (p2 == this.point(0).point) return 1
            if (p2 == this.point(1).point) return 0
        }
        else -> Unit
    }
    return -1
}

class TriangleList(val points: PointArrayList, val indices: ShortArray, val numTriangles: Int = indices.size / 3) : Iterable<Triangle> {
    val numIndices get() = numTriangles * 3
    val pointCount get() = points.size

    val size get() = numTriangles

    @PublishedApi
    internal val tempTriangle: MutableTriangle = MutableTriangle()

    class MutableTriangle : Triangle {
        override val p0 = MPoint()
        override val p1 = MPoint()
        override val p2 = MPoint()
        override fun toString(): String = "Triangle($p0, $p1, $p2)"
    }

    fun getTriangle(index: Int, out: MutableTriangle = MutableTriangle()): MutableTriangle {
        points.getPoint(indices[index * 3 + 0].toInt() and 0xFFFF, out.p0)
        points.getPoint(indices[index * 3 + 1].toInt() and 0xFFFF, out.p1)
        points.getPoint(indices[index * 3 + 2].toInt() and 0xFFFF, out.p2)
        return out
    }

    fun getTriangles(): List<Triangle> = (0 until numTriangles).map { getTriangle(it) }
    fun toTriangleList(): List<Triangle> = (0 until numTriangles).map { getTriangle(it) }

    override fun toString(): String = "TriangleList[$numTriangles](${getTriangles()})"

    inline fun fastForEach(block: (MutableTriangle) -> Unit) {
        for (n in 0 until numTriangles) block(getTriangle(n, tempTriangle))
    }

    inline fun <T> map(block: (MutableTriangle) -> T): List<T> {
        return  arrayListOf<T>().also { out -> fastForEach { out += block(it) } }
    }

    override fun iterator(): Iterator<Triangle> = toTriangleList().iterator()
}
