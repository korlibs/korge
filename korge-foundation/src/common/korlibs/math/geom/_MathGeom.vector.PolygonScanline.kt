@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.geom.vector

import korlibs.datastructure.*
import korlibs.math.*
import korlibs.math.annotations.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.segment.*
import kotlin.math.*

// @TODO: Further optimize this
// @TODO: We shouldn't propagate the complexity of coordinate scaling here. We should just support integers here and do the conversions outside.
@KormaExperimental
class PolygonScanline : RastScale() {
    var version = -1
    var winding = Winding.NON_ZERO
    private var boundsBuilder = BoundsBuilder()

    class Bucket {
        val edges = FastArrayList<MEdge>()
        fun clear() = this.apply { edges.clear() }
        inline fun fastForEach(block: (edge: MEdge) -> Unit) = edges.fastForEach(block)
    }

    class Buckets(private val pool: Pool<Bucket>, val ySize: Int) {
        private val buckets = FastIntMap<Bucket>()
        val size get() = buckets.size
        fun getIndex(y: Int) = y / ySize
        fun getForIndex(index: Int) = buckets.getOrPut(index) { pool.alloc() }
        fun getForYOrNull(y: Int) = buckets[getIndex(y)]
        inline fun fastForEachY(y: Int, block: (edge: MEdge) -> Unit) {
            if (size > 0) {
                getForYOrNull(y)?.fastForEach(block)
            }
        }
        fun clear() {
            buckets.fastForEach { _, value -> pool.free(value.clear()) }
            buckets.clear()
        }
        fun addThresold(edge: MEdge, threshold: Int = Int.MAX_VALUE): Boolean {
            val min = getIndex(edge.minY)
            val max = getIndex(edge.maxY)
            if (max - min < threshold) {
                for (n in min..max) getForIndex(n).edges.add(edge)
                return true
            }
            return false
        }
    }

    class AllBuckets {
        private val pool = Pool(reset = { it.clear() }) { Bucket() }
        @PublishedApi
        internal val small = Buckets(pool, RAST_SMALL_BUCKET_SIZE)
        @PublishedApi
        internal val medium = Buckets(pool, RAST_MEDIUM_BUCKET_SIZE)
        @PublishedApi
        internal val big = Buckets(pool, RAST_BIG_BUCKET_SIZE)

        fun add(edge: MEdge) {
            if (small.addThresold(edge, 4)) return
            if (medium.addThresold(edge, 4)) return
            big.addThresold(edge)
        }

        inline fun fastForEachY(y: Int, block: (edge: MEdge) -> Unit) {
            small.fastForEachY(y) { block(it) }
            medium.fastForEachY(y) { block(it) }
            big.fastForEachY(y) { block(it) }
        }

        fun clear() {
            small.clear()
            medium.clear()
            big.clear()
        }
    }

    private val edgesPool = Pool { MEdge() }

    @PublishedApi
    internal val edges = FastArrayList<MEdge>()
    @PublishedApi
    internal val hedges = FastArrayList<MEdge>()
    internal val allEdges = FastArrayList<MEdge>()
    private val buckets = AllBuckets()

    fun getBounds(): Rectangle = boundsBuilder.bounds

    private var closed = true
    fun reset() {
        closed = true
        boundsBuilder = BoundsBuilder.EMPTY
        edges.fastForEach { edgesPool.free(it) }
        hedges.fastForEach { edgesPool.free(it) }
        edges.clear()
        hedges.clear()
        allEdges.clear()
        points.clear()
        buckets.clear()
        moveToX = 0.0
        moveToY = 0.0
        lastX = 0.0
        lastY = 0.0
        lastMoveTo = false
    }

    @PublishedApi
    internal val points = PointArrayList()

    private fun addPoint(x: Double, y: Double) {
        points.add(x, y)
    }

    inline fun forEachPoint(callback: (x: Double, y: Double) -> Unit) {
        points.fastForEach { (x, y) ->
            callback(x.toDouble(), y.toDouble())
        }
    }

    private fun addEdge(ax: Double, ay: Double, bx: Double, by: Double) {
        if (ax == bx && ay == by) return
        val isHorizontal = ay == by
        val iax = ax.s
        val ibx = bx.s
        val iay = ay.s
        val iby = by.s
        val edge = if (ay < by) edgesPool.alloc().setTo(iax, iay, ibx, iby, +1) else edgesPool.alloc().setTo(ibx, iby, iax, iay, -1)
        allEdges.add(edge)
        if (isHorizontal) {
            hedges.add(edge)
        } else {
            edges.add(edge)
            buckets.add(edge)
        }

        boundsBuilder += Point(ax, ay)
        boundsBuilder += Point(bx, by)
    }

    var moveToX = 0.0
    var moveToY = 0.0
    var lastX = 0.0
    var lastY = 0.0
    val edgesSize get() = edges.size
    fun isNotEmpty() = edgesSize > 0

    var lastMoveTo = false
    fun moveTo(x: Double, y: Double) {
        lastX = x
        lastY = y
        moveToX = x
        moveToY = y
        lastMoveTo = true
    }

    fun lineTo(x: Double, y: Double) {
        if (lastMoveTo) {
            addPoint(lastX, lastY)
        }
        addEdge(lastX, lastY, x, y)
        addPoint(x, y)
        lastX = x
        lastY = y
        lastMoveTo = false
    }
    fun moveTo(p: Point) = moveTo(p.x, p.y)
    fun lineTo(p: Point) = lineTo(p.x, p.y)

    fun add(path: VectorPath) {
        path.emitPoints2(flush = { if (it) close() }, emit = { p, move -> add(p, move) })
    }

    fun add(p: Point, move: Boolean) = if (move) moveTo(p) else lineTo(p)
    fun add(x: Double, y: Double, move: Boolean) = if (move) moveTo(x, y) else lineTo(x, y)
    fun add(x: Float, y: Float, move: Boolean) = add(x.toDouble(), y.toDouble(), move)
    fun add(x: Int, y: Int, move: Boolean) = add(x.toDouble(), y.toDouble(), move)

    internal inline fun forEachActiveEdgeAtY(y: Int, block: (MEdge) -> Unit): Int {
        var edgesChecked = 0
        buckets.fastForEachY(y) { edge ->
            edgesChecked++
            if (edge.containsY(y)) block(edge)
        }
        return edgesChecked
    }

    fun close() {
        //println("CLOSE")
        //add(pointsX[startPathIndex], pointsY[startPathIndex])
        lineTo(moveToX, moveToY)
    }

    private val tempXW = XWithWind()

    var edgesChecked = 0
    fun scanline(y: Int, winding: Winding, out: IntSegmentSet = IntSegmentSet()): IntSegmentSet {
        edgesChecked = 0

        tempXW.clear()
        out.clear()
        edgesChecked += forEachActiveEdgeAtY(y) {
            if (!it.isCoplanarX) {
                tempXW.add(it.intersectX(y), it.wind)
            }
        }
        genericSort(tempXW, 0, tempXW.size - 1, IntArrayListSort)
        //tempXW.removeXDuplicates()
        val tempX = tempXW.x
        val tempW = tempXW.w
        if (tempXW.size >= 2) {
            //println(winding)
            //val winding = Winding.NON_ZERO
            when (winding) {
                Winding.EVEN_ODD -> {
                    //println(y)
                    //if (y == 2999) println("STEP: $tempX, $tempXW")
                    for (i in 0 until tempX.size - 1 step 2) {
                        val a = tempX.getAt(i)
                        val b = tempX.getAt(i + 1)
                        out.add(a, b)
                        //if (y == 2999) println("STEP: $a, $b")
                    }
                }
                Winding.NON_ZERO -> {
                    //println("NON-ZERO")

                    var count = 0
                    var startX = 0
                    var endX = 0
                    var pending = false

                    for (i in 0 until tempX.size - 1) {
                        val a = tempX.getAt(i)
                        count += tempW.getAt(i)
                        val b = tempX.getAt(i + 1)
                        if (count != 0) {
                            if (pending && a != endX) {
                                out.add(startX, endX)
                                startX = a
                                endX = b
                            } else {
                                if (!pending) {
                                    startX = a
                                }
                                endX = b
                            }
                            //func(a, b, y)
                            pending = true
                        }
                    }

                    if (pending) {
                        out.add(startX, endX)
                    }
                }
            }
        }
        return out
    }

    private val ss = IntSegmentSet()

    fun containsPoint(x: Double, y: Double, winding: Winding = this.winding): Boolean {
        return containsPointInt(x.s, y.s, winding)
    }

    fun containsPoint(p: Point, winding: Winding = this.winding): Boolean = containsPoint(p.x.toDouble(), p.y.toDouble(), winding)

    fun containsPointInt(x: Int, y: Int, winding: Winding = this.winding): Boolean {
        val ss = this.ss
        scanline(y, winding, ss.clear())
        return ss.contains(x)
    }

    fun getAllLines(): List<MLine> = allEdges.map { MLine(it.ax.d, it.ay.d, it.bx.d, it.by.d) }

    fun getLineIntersection(x0: Int, y0: Int, x1: Int, y1: Int, out: LineIntersection = LineIntersection()): LineIntersection? {
        // @TODO: Optimize not iterating over all the edges, but only the ones between y0 and y1
        allEdges.fastForEachWithIndex { index, edge ->
            val res = MEdge.getIntersectXY(
                edge.ax.toDouble(), edge.ay.toDouble(), edge.bx.toDouble(), edge.by.toDouble(),
                x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble(),
            )
            if (res != null) {
                out.intersection = res
                val iX = out.intersection.x
                val iY = out.intersection.y
                if (iY.toInt() in y0..y1 || iY.toInt() in y1..y0) {
                    println("index=$index, edge=$edge")
                    out.setFrom(
                        edge.ax.d, edge.ay.d, edge.bx.d, edge.by.d,
                        out.intersection.x.toInt().d, out.intersection.y.toInt().d,
                        MPoint.distance(x0.d, y0.d, x1.d, y1.d)
                    )
                    return out
                }
            }
        }
        return null
    }

    fun getLineIntersection(x0: Double, y0: Double, x1: Double, y1: Double, out: LineIntersection = LineIntersection()) = getLineIntersection(x0.s, y0.s, x1.s, y1.s, out)
    fun getLineIntersection(a: PointInt, b: PointInt, out: LineIntersection = LineIntersection()) = getLineIntersection(a.x, a.y, b.x, b.y, out)
    fun getLineIntersection(a: Point, b: Point, out: LineIntersection = LineIntersection()) = getLineIntersection(a.x.s, a.y.s, b.x.s, b.y.s, out)
    fun getLineIntersection(line: Line, out: LineIntersection = LineIntersection()) = getLineIntersection(line.a, line.b, out)

    private class XWithWind {
        val x = IntArrayList(1024)
        val w = IntArrayList(1024)
        val size get() = x.size

        fun add(x: Int, wind: Int) {
            this.x.add(x)
            this.w.add(wind)
        }

        fun clear() {
            x.clear()
            w.clear()
        }

        //fun removeXDuplicates() {
        //    genericRemoveSortedDuplicates(
        //        size = size,
        //        equals = { x, y -> this.x.getAt(x) == this.x.getAt(y) },
        //        copy = { src, dst ->
        //            this.x[dst] = this.x.getAt(src)
        //            this.w[dst] = this.w.getAt(src)
        //        },
        //        resize = { size ->
        //            this.x.size = size
        //            this.w.size = size
        //        }
        //    )
        //}

        override fun toString(): String = "XWithWind($x, $w)"
    }

    // @TODO: Change once KDS is updated
    private object IntArrayListSort : SortOps<XWithWind>() {
        override fun compare(subject: XWithWind, l: Int, r: Int): Int = subject.x.getAt(l).compareTo(subject.x.getAt(r))
        override fun swap(subject: XWithWind, indexL: Int, indexR: Int) {
            subject.x.swap(indexL, indexR)
            subject.w.swap(indexL, indexR)
        }
    }
}

@Suppress("DuplicatedCode")
@KormaExperimental
@KormaMutableApi
class MEdge {
    fun getX(n: Int) = if (n == 0) this.ax else this.bx
    fun getY(n: Int) = if (n == 0) this.ay else this.by

    companion object {
        operator fun invoke(ax: Int, ay: Int, bx: Int, by: Int, wind: Int = 0) = MEdge().setTo(ax, ay, bx, by, wind)
        operator fun invoke(a: PointInt, b: PointInt, wind: Int = 0) = this(a.x, a.y, b.x, b.y, wind)

        fun getIntersectY(a: MEdge, b: MEdge): Int = getIntersectXYInt(a, b)?.y ?: Int.MIN_VALUE
        fun getIntersectX(a: MEdge, b: MEdge): Int = getIntersectXYInt(a, b)?.x ?: Int.MIN_VALUE

        fun areParallel(a: MEdge, b: MEdge) = ((a.by - a.ay) * (b.ax - b.bx)) - ((b.by - b.ay) * (a.ax - a.bx)) == 0
        fun getIntersectXY(a: MEdge, b: MEdge): Point? = _getIntersectXY(a, b)?.let { Point(it.x, it.y) }
        fun getIntersectXYInt(a: MEdge, b: MEdge): Vector2I? = _getIntersectXY(a, b)

        fun angleBetween(a: MEdge, b: MEdge): Angle {
            return b.angle - a.angle
        }

        // https://www.geeksforgeeks.org/program-for-point-of-intersection-of-two-lines/
        inline fun _getIntersectXY(a: MEdge, b: MEdge): Vector2I? {
            val Ax: Double = a.ax.toDouble()
            val Ay: Double = a.ay.toDouble()
            val Bx: Double = a.bx.toDouble()
            val By: Double = a.by.toDouble()
            val Cx: Double = b.ax.toDouble()
            val Cy: Double = b.ay.toDouble()
            val Dx: Double = b.bx.toDouble()
            val Dy: Double = b.by.toDouble()
            return getIntersectXY(Ax, Ay, Bx, By, Cx, Cy, Dx, Dy)?.let { Vector2I(floorCeil(it.x).toInt(), floorCeil(it.y).toInt()) }
        }

        fun getIntersectXY(Ax: Double, Ay: Double, Bx: Double, By: Double, Cx: Double, Cy: Double, Dx: Double, Dy: Double): Point? {
            return MLine.getIntersectXY(Ax, Ay, Bx, By, Cx, Cy, Dx, Dy)
        }
    }

    var ax = 0; private set
    var ay = 0; private set
    var bx = 0; private set
    var by = 0; private set
    var wind: Int = 0; private set

    var dy: Int = 0; private set
    var dx: Int = 0; private set
    var isCoplanarX: Boolean = false; private set
    var isCoplanarY: Boolean = false; private set

    var h: Int = 0; private set

    val length: Float get() = hypot(dx.toFloat(), dy.toFloat())

    fun copyFrom(other: MEdge) = setTo(other.ax, other.ay, other.bx, other.by, other.wind)

    fun setTo(ax: Int, ay: Int, bx: Int, by: Int, wind: Int) = this.apply {
        this.ax = ax
        this.ay = ay
        this.bx = bx
        this.by = by
        this.dx = bx - ax
        this.dy = by - ay
        this.isCoplanarX = ay == by
        this.isCoplanarY = ax == bx
        this.wind = wind
        this.h = if (isCoplanarY) 0 else ay - (ax * dy) / dx
    }

    fun setToHalf(a: MEdge, b: MEdge): MEdge = this.apply {
        val minY = min(a.minY, b.minY)
        val maxY = min(a.maxY, b.maxY)
        val minX = (a.intersectX(minY) + b.intersectX(minY)) / 2
        val maxX = (a.intersectX(maxY) + b.intersectX(maxY)) / 2
        setTo(minX, minY, maxX, maxY, +1)
    }

    val minX get() = min(ax, bx)
    val maxX get() = max(ax, bx)
    val minY get() = min(ay, by)
    val maxY get() = max(ay, by)

    @Suppress("ConvertTwoComparisonsToRangeCheck")
    //fun containsY(y: Int): Boolean = if (ay == by) y == ay else if (wind >= 0) y >= ay && y < by else y > ay && y <= by
    fun containsY(y: Int): Boolean {
        return y >= ay && y < by
        //val a = if (wind >= 0) y >= ay && y < by else y > ay && y <= by
        //val b = y >= ay && y < by
        //if (a != b) {
        //    println("wind=$wind, y=$y, ay=$ay, by=$by, a=$a, b=$b")
        //}
        //return a
    }

    //fun containsYNear(y: Int, offset: Int): Boolean = y >= (ay - offset) && y < (by + offset)
    //fun containsY(y: Int): Boolean = y in ay..by
    //fun containsYNear(y: Int, offset: Int): Boolean = y >= (ay - offset) && y <= (by + offset)
    //fun intersectX(y: Int): Int = if (isCoplanarY) ax else ((y - h) * dx) / dy
    //fun intersectX(y: Int): Int = if (dy == 0) ax else ((y - h) * dx) / dy
    fun intersectX(y: Int): Int = if (isCoplanarY || dy == 0) ax else ((y - h) * dx) / dy
    //fun intersectX(y: Double): Double = if (isCoplanarY) ax else ((y - h) * this.dx) / this.dy

    // Stroke extensions
    val angle: Angle get() = Angle.between(ax, ay, bx, by)
    val cos: Double get() = angle.cosine
    val absCos: Double get() = cos.absoluteValue
    val sin: Double get() = angle.sine
    val absSin: Double get() = sin.absoluteValue

    override fun toString(): String = "Edge([$ax,$ay]-[$bx,$by])"
    fun toString(scale: Double): String = "Edge([${(ax * scale).toInt()},${(ay * scale).toInt()}]-[${(bx * scale).toInt()},${(by * scale).toInt()}])"
}


@KormaExperimental
open class RastScale {
    companion object {
        //const val RAST_FIXED_SCALE = 32 // Important NOTE: Power of two so divisions are >> and remaining &
        const val RAST_FIXED_SCALE = 20
        //const val RAST_FIXED_SCALE_HALF = (RAST_FIXED_SCALE / 2) - 1
        //const val RAST_FIXED_SCALE_HALF = (RAST_FIXED_SCALE / 2)
        const val RAST_FIXED_SCALE_HALF = 0

        const val RAST_SMALL_BUCKET_SIZE = 4 * RAST_FIXED_SCALE
        const val RAST_MEDIUM_BUCKET_SIZE = 16 * RAST_FIXED_SCALE
        const val RAST_BIG_BUCKET_SIZE = 64 * RAST_FIXED_SCALE
    }

    val sscale get() = RAST_FIXED_SCALE
    val hscale get() = RAST_FIXED_SCALE_HALF

    val Float.s: Int get() = ((this * sscale).toInt() + hscale)
    val Double.s: Int get() = ((this * sscale).toInt() + hscale)
    val Int.d: Double get() = (this.toDouble() - hscale) / sscale
    //@PublishedApi
    //internal val Int.us: Double get() = (this.toDouble() - RAST_FIXED_SCALE_HALF) * scale / RAST_FIXED_SCALE
    //@PublishedApi
    //internal val Int.us2: Double get() = this.toDouble() * scale/ RAST_FIXED_SCALE
}

// y = (m * x) + b
// x = (y - b) / m
/*
internal data class Line(var m: Double, var b: Double) {
    // Aliases
    val slope get() = m
    val yIntercept get() = b
    val isXCoplanar get() = m == 0.0
    val isYCoplanar get() = m.isInfinite()

    companion object {
        fun fromTwoPoints(ax: Double, ay: Double, bx: Double, by: Double) = Line(0.0, 0.0).setFromTwoPoints(ax, ay, bx, by)
        fun getHalfLine(a: Line, b: Line, out: Line = Line(0.0, 0.0)): Line {


            return out.setFromTwoPoints()
        }
    }

    fun setFromTwoPoints(ax: Double, ay: Double, bx: Double, by: Double) = this.apply {
        this.m = (by - ay) / (bx - ax)
        // y = (slope * x) + b
        // ay = (slope * ax) + b
        // b = ay - (slope * ax)
        this.b = ay - (this.m * ax)
    }

    fun getY(x: Double) = if (isYCoplanar) 0.0 else (m * x) + b
    fun getX(y: Double) = if (isXCoplanar) 0.0 else (y - b) / m

    // y = (m0 * x) + b0
    // y = (m1 * x) + b1
    // (m0 * x) + b0 = (m1 * x) + b1
    // (m0 * x) = (m1 * x) + b1 - b0
    // (m0 * x) - (m1 * x) = b1 - b0
    // (m0 - m1) * x = b1 - b0
    // x = (b1 - b0) / (m0 - m1)
    fun getIntersectionX(other: Line): Double = (other.b - this.b) / (this.m - other.m)

    fun getIntersection(other: Line, out: Point = Point()): Point {
        val x = getIntersectionX(other)
        return out.setTo(x, getY(x))
    }

    fun getSegmentFromX(x0: Double, x1: Double) = LineSegment(x0, getY(x0), x1, getY(x1))
    fun getSegmentFromY(y0: Double, y1: Double) = LineSegment(getX(y0), y0, getX(y1), y1)
}

internal class LineSegment(ax: Double, ay: Double, bx: Double, by: Double) {
    var ax = ax; private set
    var ay = ay; private set
    var bx = bx; private set
    var by = by; private set
    val line = Line.fromTwoPoints(ax, ay, bx, by)
    fun setTo(ax: Double, ay: Double, bx: Double, by: Double) = this.apply {
        this.ax = ax
        this.ay = ay
        this.bx = bx
        this.by = by
        this.line.setFromTwoPoints(ax, ay, bx, by)
    }
    val slope get() = line.slope
    val length get() = Point.distance(ax, ay, bx, by)
}

internal data class Line(val ax: Double, val ay: Double, val bx: Double, val by: Double) {
    val minX get() = min(ax, bx)
    val maxX get() = max(ax, bx)
    val minY get() = min(ay, by)
    val maxY get() = max(ay, by)

    val isCoplanarX get() = ay == by
    val isCoplanarY get() = ax == bx
    val dy get() = (by - ay)
    val dx get() = (bx - ax)
    val slope get() = dy / dx
    val islope get() = 1.0 / slope

    val h = if (isCoplanarY) 0.0 else ay - (ax * dy) / dx

    fun containsY(y: Double): Boolean = y >= ay && y < by
    fun containsYNear(y: Double, offset: Double): Boolean = y >= (ay - offset) && y < (by + offset)
    fun getX(y: Double): Double = if (isCoplanarY) ax else ((y - h) * dx) / dy
    fun getY(x: Double): Double = if (isCoplanarX) ay else TODO()
    fun intersect(line: Line, out: Point = Point()): Point? {

    }
    //fun intersectX(y: Double): Double = if (isCoplanarY) ax else ((y - h) * this.dx) / this.dy

    // Stroke extensions
    val angle = Angle.between(ax, ay, bx, by)
    val cos = angle.cosine
    val absCos = cos.absoluteValue
    val sin = angle.sine
    val absSin = sin.absoluteValue
}
*/
