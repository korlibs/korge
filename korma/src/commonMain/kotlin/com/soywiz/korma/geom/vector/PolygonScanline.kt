package com.soywiz.korma.geom.vector

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.FastIntMap
import com.soywiz.kds.IntArrayList
import com.soywiz.kds.Pool
import com.soywiz.kds.SortOps
import com.soywiz.kds.clear
import com.soywiz.kds.fastForEach
import com.soywiz.kds.genericSort
import com.soywiz.kds.get
import com.soywiz.kds.getOrPut
import com.soywiz.kds.size
import com.soywiz.korma.annotations.KormaExperimental
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.IPointInt
import com.soywiz.korma.geom.MLine
import com.soywiz.korma.geom.LineIntersection
import com.soywiz.korma.geom.MPoint
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.MRectangle
import com.soywiz.korma.geom.fastForEach
import com.soywiz.korma.geom.shape.emitPoints2
import com.soywiz.korma.segment.IntSegmentSet

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

    val Double.s: Int get() = ((this * sscale).toInt() + hscale)
    val Int.d: Double get() = (this.toDouble() - hscale) / sscale
    //@PublishedApi
    //internal val Int.us: Double get() = (this.toDouble() - RAST_FIXED_SCALE_HALF) * scale / RAST_FIXED_SCALE
    //@PublishedApi
    //internal val Int.us2: Double get() = this.toDouble() * scale/ RAST_FIXED_SCALE
}

// @TODO: Further optimize this
// @TODO: We shouldn't propagate the complexity of coordinate scaling here. We should just support integers here and do the conversions outside.
@KormaExperimental
class PolygonScanline : RastScale() {
    var version = -1
    var winding = Winding.NON_ZERO
    private val boundsBuilder = BoundsBuilder()

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

    fun getBounds(out: MRectangle = MRectangle()) = boundsBuilder.getBounds(out)

    private var closed = true
    fun reset() {
        closed = true
        boundsBuilder.reset()
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
        points.fastForEach { x, y ->
            callback(x, y)
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

        boundsBuilder.add(ax, ay)
        boundsBuilder.add(bx, by)
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

    fun add(path: VectorPath) {
        path.emitPoints2(flush = { if (it) close() }, emit = { x, y, move -> add(x, y, move) })
    }

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
                out.intersection
            )
            val iX = out.intersection.x
            val iY = out.intersection.y
            if (res != null) {
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
    fun getLineIntersection(a: IPointInt, b: IPointInt, out: LineIntersection = LineIntersection()) = getLineIntersection(a.x, a.y, b.x, b.y, out)
    fun getLineIntersection(a: IPoint, b: IPoint, out: LineIntersection = LineIntersection()) = getLineIntersection(a.x.s, a.y.s, b.x.s, b.y.s, out)
    fun getLineIntersection(line: MLine, out: LineIntersection = LineIntersection()) = getLineIntersection(line.a, line.b, out)

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
