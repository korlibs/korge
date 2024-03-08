@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.geom.trapezoid

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.math.*
import korlibs.math.annotations.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import kotlin.jvm.*
import kotlin.math.*

/**
 * https://en.wikipedia.org/wiki/Trapezoid
 *
 *   (x0a, y0)  (x0b, y0)
 *   +----------+
 *  /            \
 * +--------------+
 * (x1a, y1)     (x1b, y1)
 */
data class TrapezoidInt(
    val x0a: Int, val x0b: Int, val y0: Int,
    val x1a: Int, val x1b: Int, val y1: Int,
) {
    val height: Int get() = y1 - y0
    val baseA: Int get() = x0b - x0a
    val baseB: Int get() = x1b - x1a
    val area: Double get() = ((baseA + baseB) * height) / 2.0

    companion object {
        fun inside(
            x0a: Int, x0b: Int, y0: Int,
            x1a: Int, x1b: Int, y1: Int,
            x: Int, y: Int
        ): Boolean {
            if (y < y0 || y > y1) return false
            if ((x < x0a && x < x1a) || (x > x0b && x > x1b)) return false
            val sign1 = det(x1a - x, y1 - y, x0a - x, y0 - y).sign
            val sign2 = det(x1b - x, y1 - y, x0b - x, y0 - y).sign
            return sign1 != sign2
        }

        fun triangulate(
            x0a: Int, x0b: Int, y0: Int,
            x1a: Int, x1b: Int, y1: Int,
            out: FTrianglesInt = FTrianglesInt()
        ): FTrianglesInt {
            when {
                x0a == x0b -> out.add(x1a, y1, x0a, y0, x1b, y1)
                x1a == x1b -> out.add(x0a, y0, x0b, y0, x1a, y1)
                else -> {
                    out.add(x0a, y0, x0b, y0, x1a, y1)
                    out.add(x1a, y1, x0b, y0, x1b, y1)
                }
            }
            return out
        }

        private fun det(x0: Int, y0: Int, x1: Int, y1: Int) = (x0 * y1) - (x1 * y0)
    }

    fun inside(x: Int, y: Int): Boolean = inside(x0a, x0b, y0, x1a, x1b, y1, x, y)
    fun triangulate(out: FTrianglesInt = FTrianglesInt()): FTrianglesInt = Companion.triangulate(x0a, x0b, y0, x1a, x1b, y1, out)
}

fun List<TrapezoidInt>.pointInside(x: Int, y: Int, assumeSorted: Boolean = false): TrapezoidInt? {
    this.fastForEach {
        if (it.inside(x, y)) return it
    }
    return null
}

data class TriangleInt(
    val p0: Vector2I,
    val p1: Vector2I,
    val p2: Vector2I,
)

@KormaMutableApi
sealed interface ITriangleInt {
    val x0: Int
    val y0: Int
    val x1: Int
    val y1: Int
    val x2: Int
    val y2: Int
}

@KormaMutableApi
data class MTriangleInt(
    override var x0: Int, override var y0: Int,
    override var x1: Int, override var y1: Int,
    override var x2: Int, override var y2: Int,
) : ITriangleInt {
    constructor() : this(0, 0, 0, 0, 0, 0)

    fun copyFrom(other: MTriangleInt) {
        setTo(other.x0, other.y0, other.x1, other.y1, other.x2, other.y2)
    }

    fun setTo(
        x0: Int, y0: Int,
        x1: Int, y1: Int,
        x2: Int, y2: Int,
    ) {
        this.x0 = x0
        this.y0 = y0
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2
    }

    override fun toString(): String = "TriangleInt(($x0, $y0), ($x1, $y1), ($x2, $y2))"
}

/*
object SegmentIntToTrapezoidIntList {
    fun convert(path: VectorPath, scale: Int = 1, winding: Winding = path.winding, out: FTrapezoidsInt = FTrapezoidsInt()): FTrapezoidsInt = convert(path.toSegments(scale), winding, out)

    fun convert(segments: List<SegmentInt>, winding: Winding = Winding.EVEN_ODD, trapezoids: FTrapezoidsInt = FTrapezoidsInt()): FTrapezoidsInt {
        //segments.fastForEach { println("seg=$it") }
        trapezoids.assumeSorted = true
        val (allY, allSegmentsInY) = segmentLookups(segments)
        for (n in 0 until allY.size - 1) {
            val y0 = allY[n]
            val y1 = allY[n + 1]
            val segs = allSegmentsInY[n]
            //println("y=$y0, segs=$segs")
            val pairs = arrayListOf<Pair<SegmentInt, SegmentInt>>()
            when (winding) {
                Winding.EVEN_ODD -> {
                    for (m in segs.indices step 2) {
                        val s0 = segs.getOrNull(m + 0) ?: continue
                        val s1 = segs.getOrNull(m + 1) ?: continue
                        if (!s0.containsY(y1) || !s1.containsY(y1)) continue
                        pairs += s0 to s1
                    }
                }
                Winding.NON_ZERO -> {
                    var sign = 0
                    //println("y0=$y0, segs=$segs")
                    for (m in segs.indices) {
                        val seg = segs[m]
                        if (sign != 0 && m > 0) {
                            val s0 = segs[m - 1]
                            val s1 = seg
                            if (!s0.containsY(y1) || !s1.containsY(y1)) continue
                            pairs += s0 to s1
                        }
                        sign += seg.dy.sign
                    }
                }
            }

            for ((s0, s1) in pairs) {
                val x0a = s0.x(y0)
                val x0b = s1.x(y0)
                val x1a = s0.x(y1)
                val x1b = s1.x(y1)
                // Segments are crossing
                if (x1b < x1a) {
                    val intersectY = SegmentInt.getIntersectY(s0, s1)
                    val intersectX = s0.x(intersectY)
                    trapezoids.add(
                        x0a = x0a, x0b = x0b, y0 = y0,
                        x1a = intersectX, x1b = intersectX, y1 = intersectY,
                    )
                    trapezoids.add(
                        x0a = intersectX, x0b = intersectX, y0 = intersectY,
                        x1a = x1a, x1b = x1b, y1 = y1,
                    )
                } else {
                    trapezoids.add(
                        x0a = x0a, x0b = x0b, y0 = y0,
                        x1a = x1a, x1b = x1b, y1 = y1,
                    )
                }
            }
        }
        //parallelograms.fastForEach { println(it) }
        return trapezoids
    }

    private fun segmentLookups(segments: List<SegmentInt>): Pair<IntArray, List<List<SegmentInt>>> {
        val list = segments.sortedBy { it.yMin }.filter { it.dy != 0 }
        val allY = (list.map { it.yMin } + list.map { it.yMax }).distinct().toIntArray().sortedArray()
        //list.fastForEach { println("segment: $it") }
        //println("allY=${allY.toList()}")
        val initialSegmentsInY = Array(allY.size) { arrayListOf<SegmentInt>() }.toList()
        val allSegmentsInY = Array(allY.size) { arrayListOf<SegmentInt>() }.toList()
        var listPivot = 0
        var yPivot = 0
        while (yPivot < allY.size && listPivot < list.size) {
            val currentY = allY[yPivot]
            val currentItem = list[listPivot]
            if (currentItem.yMin == currentY) {
                //println("currentItem[$currentY]=$currentItem")
                initialSegmentsInY[yPivot].add(currentItem)
                listPivot++
            } else {
                yPivot++
            }
        }
        for (n in allY.indices) {
            for (segment in initialSegmentsInY[n]) {
                for (m in n until allY.size) {
                    val y = allY[m]
                    if (!segment.containsY(y) || segment.yMax == y) break
                    //println("m=$m, y=$y, segment=$segment")
                    allSegmentsInY[m].add(segment)
                }
            }
        }

        // Sort segments
        allSegmentsInY.fastForEach { it.sortBy { it.xMin } }

        // Checks
        for (n in allY.indices) {
            allSegmentsInY[n].fastForEach { segment ->
                check(segment.containsY(allY[n]))
            }
        }

        //println("segmentsInY=$initialSegmentsInY")
        //println("allSegmentsInY=$allSegmentsInY")
        return Pair(allY, allSegmentsInY)
    }
}

fun VectorPath.toSegments(scale: Int = 1): List<SegmentInt> {
    val segments = fastArrayListOf<SegmentInt>()
    val p = Point()
    fun emit(x0: Double, y0: Double, x1: Double, y1: Double) {
        //println("EMIT")
        segments.add(SegmentInt((x0 * scale).toIntRound(), (y0 * scale).toIntRound(), (x1 * scale).toIntRound(), (y1 * scale).toIntRound()).also {
            //println("EMIT: $it")
        })
    }
    fun emit(bezier: Bezier) {
        val len = (bezier.length / 2.5).toIntRound().coerceIn(2, 32)
        var oldX = 0.0
        var oldY = 0.0
        for (n in 0 .. len) {
            val ratio = n.toDouble() / len
            bezier.calc(ratio, p)
            val newX = p.x
            val newY = p.y
            if (n > 0) {
                emit(oldX, oldY, newX, newY)
            }
            oldX = newX
            oldY = newY
        }
    }

    //getAllLines().fastForEach { emit(it.x0, it.y0, it.x1, it.y1) }

    visitEdges(
        line = { x0, y0, x1, y1 -> emit(x0, y0, x1, y1) },
        quad = { x0, y0, x1, y1, x2, y2 -> emit(Bezier(x0, y0, x1, y1, x2, y2)) },
        cubic = { x0, y0, x1, y1, x2, y2, x3, y3 -> emit(Bezier(x0, y0, x1, y1, x2, y2, x3, y3)) },
        optimizeClose = false
    )
    return segments
}

fun VectorPath.toTrapezoids(scale: Int = 1, winding: Winding = this.winding, out: FTrapezoidsInt = FTrapezoidsInt()): FTrapezoidsInt =
    SegmentIntToTrapezoidIntList.convert(this, scale, winding, out)

fun List<TrapezoidInt>.triangulate(out: FTrianglesInt = FTrianglesInt()): FTrianglesInt {
    fastForEach { it.triangulate(out) }
    return out
}

fun FTrapezoidsInt.triangulate(out: FTrianglesInt = FTrianglesInt()): FTrianglesInt {
    fastForEach { it.triangulate(out) }
    return out
}
*/

object SegmentIntToTrapezoidIntList {
    fun convert(path: VectorPath, scale: Int = 1, winding: Winding = path.winding, out: FTrapezoidsInt = FTrapezoidsInt()): FTrapezoidsInt = convert(path.toSegments(scale), path.winding)

    fun convert(segments: FSegmentsInt, winding: Winding = Winding.EVEN_ODD, trapezoids: FTrapezoidsInt = FTrapezoidsInt()): FTrapezoidsInt {
        //segments.fastForEach { println("seg=$it") }
        val (allY, allSegmentsInY) = segmentLookups(segments)
        for (n in 0 until allY.size - 1) {
            val y0 = allY[n]
            val y1 = allY[n + 1]
            val segs = allSegmentsInY[n]
            //println("y=$y0, segs=$segs")
            val chunks = arrayListOf<Pair<FSegmentsInt.Item, FSegmentsInt.Item>>()
            when (winding) {
                Winding.EVEN_ODD -> {
                    for (m in 0 until segs.size step 2) {
                        val s0 = segs.getOrNull(m + 0) ?: continue
                        val s1 = segs.getOrNull(m + 1) ?: continue
                        if (!segs { s0.containsY(y1) } || segs { !s1.containsY(y1) }) continue
                        chunks += Pair(s0, s1)
                    }
                }
                Winding.NON_ZERO -> {
                    var sign = 0
                    //println("y0=$y0, segs=$segs")
                    for (m in 0 until segs.size) {
                        val seg = segs[m]
                        if (sign != 0 && m > 0) {
                            val s0 = segs[m - 1]
                            val s1 = seg
                            if (!segs { s0.containsY(y1) } || segs { !s1.containsY(y1) }) continue
                            chunks += Pair(s0, s1)
                        }
                        sign += segs { seg.dy.sign }
                    }
                }
            }

            for ((s0, s1) in chunks) {
                segs {
                    val x0a = s0.x(y0)
                    val x0b = s1.x(y0)
                    val x1a = s0.x(y1)
                    val x1b = s1.x(y1)
                    // Segments are crossing
                    if (x1b < x1a) {
                        val intersectY = s0.getIntersectY(s1)
                        val intersectX = s0.x(intersectY)
                        trapezoids.add(
                            x0a = x0a, x0b = x0b, y0 = y0,
                            x1a = intersectX, x1b = intersectX, y1 = intersectY,
                        )
                        trapezoids.add(
                            x0a = intersectX, x0b = intersectX, y0 = intersectY,
                            x1a = x1a, x1b = x1b, y1 = y1,
                        )
                    } else {
                        trapezoids.add(
                            x0a = x0a, x0b = x0b, y0 = y0,
                            x1a = x1a, x1b = x1b, y1 = y1,
                        )
                    }
                }
            }
        }
        //parallelograms.fastForEach { println(it) }
        return trapezoids
    }

    private fun segmentLookups(segments: FSegmentsInt): Pair<IntArray, List<FSegmentsInt>> {
        val list = segments.sortedBy { it.yMin }.filter { it.dy != 0 }
        val allY = (list.map { it.yMin } + list.map { it.yMax }).distinct().toIntArray().sortedArray()
        //list.fastForEach { println("segment: ${it.toStringDefault()}") }
        //println("allY=${allY.toList()}")
        val initialSegmentsInY = Array(allY.size) { FSegmentsInt() }.toList()
        val allSegmentsInY = Array(allY.size) { FSegmentsInt() }.toList()
        var listPivot = 0
        var yPivot = 0
        while (yPivot < allY.size && listPivot < list.size) {
            val currentY = allY[yPivot]
            val currentItem = list[listPivot]
            if (currentItem.use(list) { it.yMin } == currentY) {
                //println("currentItem[$currentY]=$currentItem")
                initialSegmentsInY[yPivot].add(currentItem, list)
                listPivot++
            } else {
                yPivot++
            }
        }
        for (n in allY.indices) {
            initialSegmentsInY[n].fastForEach { segment ->
                for (m in n until allY.size) {
                    val y = allY[m]
                    if (!segment.containsY(y) || segment.yMax == y) break
                    //println("m=$m, y=$y, segment=$segment")
                    allSegmentsInY[m].add(segment, this)
                }
            }
        }

        // Sort segments
        allSegmentsInY.fastForEach { it.sortBy { it.xMin } }

        // Checks
        for (n in allY.indices) {
            allSegmentsInY[n].fastForEach { segment ->
                check(segment.containsY(allY[n]))
            }
        }

        //println("segmentsInY=$initialSegmentsInY")
        //println("allSegmentsInY=$allSegmentsInY")
        return Pair(allY, allSegmentsInY)
    }
}

fun VectorPath.toSegments(scale: Int = 1): FSegmentsInt {
    val segments = FSegmentsInt()
    fun emit(p0: Point, p1: Point) {
        //println("EMIT")
        segments.add(
            (p0.x * scale).toIntRound(), (p0.y * scale).toIntRound(),
            (p1.x * scale).toIntRound(), (p1.y * scale).toIntRound()
        )
    }
    fun emit(bezier: Bezier) {
        val len = bezier.length.toIntRound().coerceIn(2, 20)
        var oldPos = Point()
        Ratio.forEachRatio(len) { ratio ->
            val p = bezier.calc(ratio)
            if (ratio > Ratio.ZERO) {
                emit(oldPos, p)
            }
            oldPos = p
        }
    }

    //getAllLines().fastForEach { emit(it.x0, it.y0, it.x1, it.y1) }

    visitEdges(
        line = { p0, p1 -> emit(p0, p1) },
        quad = { p0, p1, p2 -> emit(Bezier(p0, p1, p2)) },
        cubic = { p0, p1, p2, p3 -> emit(Bezier(p0, p1, p2, p3)) },
        optimizeClose = false
    )
    return segments
}

fun VectorPath.toTrapezoids(scale: Int = 1, winding: Winding = this.winding, out: FTrapezoidsInt = FTrapezoidsInt()): FTrapezoidsInt =
    SegmentIntToTrapezoidIntList.convert(this, scale, winding, out)

fun List<TrapezoidInt>.triangulate(out: FTrianglesInt = FTrianglesInt()): FTrianglesInt {
    fastForEach { it.triangulate(out) }
    return out
}

fun FTrapezoidsInt.triangulate(out: FTrianglesInt = FTrianglesInt()): FTrianglesInt {
    fastForEach { it.triangulate(out) }
    return out
}

// @TODO: What about [Line]?
data class SegmentInt(
    val p0: Vector2I, val p1: Vector2I
)

@KormaMutableApi
sealed interface ISegmentInt {
    var x0: Int
    var y0: Int
    var x1: Int
    var y1: Int
}

@KormaMutableApi
data class MSegmentInt(
    override var x0: Int, override var y0: Int,
    override var x1: Int, override var y1: Int
) : ISegmentInt{
    constructor() : this(0, 0, 0, 0)

    fun setTo(x0: Int, y0: Int, x1: Int, y1: Int) {
        this.x0 = x0
        this.y0 = y0
        this.x1 = x1
        this.y1 = y1
    }

    val dx: Int get() = x1 - x0 // run
    val dy: Int get() = y1 - y0 // rise
    val slope: Double get() = dy.toDouble() / dx.toDouble()
    val islope: Double get() = dx.toDouble() / dy.toDouble()
    val xMin: Int get() = minOf(x0, x1)
    val yMin: Int get() = minOf(y0, y1)
    val xMax: Int get() = maxOf(x0, x1)
    val yMax: Int get() = maxOf(y0, y1)
    fun x(y: Int): Int = x0 + ((y - y0) * islope).toIntRound()
    fun y(x: Int): Int = y0 + (slope * (x - x0)).toIntRound()
    fun containsX(x: Int): Boolean = x in xMin..xMax
    fun containsY(y: Int): Boolean = y in yMin..yMax
    fun getIntersectY(other: MSegmentInt): Int = MSegmentInt.getIntersectY(this, other)

    override fun toString(): String = "SegmentInt(($x0, $y0), ($x1, $y1))"

    companion object {
        inline fun getIntersectXY(Ax: Int, Ay: Int, Bx: Int, By: Int, Cx: Int, Cy: Int, Dx: Int, Dy: Int, out: (x: Int, y: Int) -> Unit): Boolean {
            val a1 = By - Ay
            val b1 = Ax - Bx
            val c1 = a1 * (Ax) + b1 * (Ay)
            val a2 = Dy - Cy
            val b2 = Cx - Dx
            val c2 = a2 * (Cx) + b2 * (Cy)
            val determinant = a1 * b2 - a2 * b1
            if (determinant == 0) return false
            val x = (b2 * c1 - b1 * c2) / determinant
            val y = (a1 * c2 - a2 * c1) / determinant
            out(x, y)
            return true
        }

        inline fun getIntersectXY(a: MSegmentInt, b: MSegmentInt, crossinline out: (x: Int, y: Int) -> Unit): Boolean =
            getIntersectXY(a.x0, a.y0, a.x1, a.y1, b.x0, b.y0, b.x1, b.y1, out)

        fun getIntersectY(a: MSegmentInt, b: MSegmentInt): Int {
            var outY = Int.MIN_VALUE
            getIntersectXY(a, b) { x, y -> outY = y }
            return outY
        }

        fun getIntersectY(Ax: Int, Ay: Int, Bx: Int, By: Int, Cx: Int, Cy: Int, Dx: Int, Dy: Int): Int {
            var outY = Int.MIN_VALUE
            getIntersectXY(Ax, Ay, Bx, By, Cx, Cy, Dx, Dy) { x, y -> outY = y }
            return outY
        }
    }
}

class FTrianglesInt {
    companion object {
        operator fun invoke(block: FTrianglesInt.() -> Unit): FTrianglesInt = FTrianglesInt().also(block)
    }
    val coords = intArrayListOf()
    val indices = intArrayListOf()
    val size: Int get() = indices.size / 3

    operator fun get(index: Int): Item = Item(index)
    inline fun fastForEach(block: FTrianglesInt.(Item) -> Unit) { for (n in 0 until size) this.block(this[n]) }
    fun toTriangleIntList(): List<MTriangleInt> = map { it.toTriangleInt() }
    inline fun <T> map(block: FTrianglesInt.(Item) -> T): List<T> = fastArrayListOf<T>().also { out -> fastForEach { out.add(block(it)) } }

    private fun Item.index(i: Int, c: Int): Int = indices[index * 3 + i] * 2 + c
    private fun Item.coord(i: Int, c: Int): Int = coords[index(i, c)]
    private fun Item.coordX(i: Int): Int = coord(i, 0)
    private fun Item.coordY(i: Int): Int = coord(i, 1)

    private fun Item.setCoord(i: Int, c: Int, value: Int) { coords[index(i, c)] = value }
    private fun Item.setCoordX(i: Int, value: Int) { setCoord(i, 0, value) }
    private fun Item.setCoordY(i: Int, value: Int) { setCoord(i, 1, value) }

    var Item.x0: Int get() = coordX(0); set(value) { setCoordX(0, value) }
    var Item.y0: Int get() = coordY(0); set(value) { setCoordY(0, value) }
    var Item.x1: Int get() = coordX(1); set(value) { setCoordX(1, value) }
    var Item.y1: Int get() = coordY(1); set(value) { setCoordY(1, value) }
    var Item.x2: Int get() = coordX(2); set(value) { setCoordX(2, value) }
    var Item.y2: Int get() = coordY(2); set(value) { setCoordY(2, value) }
    fun Item.toTriangleInt(out: MTriangleInt = MTriangleInt()): MTriangleInt {
        out.setTo(x0, y0, x1, y1, x2, y2)
        return out
    }

    fun add(x0: Int, y0: Int, x1: Int, y1: Int, x2: Int, y2: Int): Item {
        val triangleIndex = size
        val coordIndex = coords.size / 2
        coords.add(x0, y0, x1, y1, x2, y2)
        indices.add(coordIndex, coordIndex + 1, coordIndex + 2)
        return Item(triangleIndex)
    }

    fun add(v: Item): Item = add(v.x0, v.y0, v.x1, v.y1, v.x2, v.y2)
    fun add(v: MTriangleInt): Item = add(v.x0, v.y0, v.x1, v.y1, v.x2, v.y2)

    //fun addStrip(x0: Int, y0: Int, x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int): Item {
    //}

    inline class Item(val index: Int) {
        inline fun <T> use(triangles: FTrianglesInt, block: FTrianglesInt.(Item) -> T): T = block(triangles, this)
    }
}

fun List<MTriangleInt>.toFTrianglesInt(): FTrianglesInt = FTrianglesInt { this@toFTrianglesInt.fastForEach { add(it) } }

/**
 *   (x0a, y0)  (x0b, y0)
 *   +----------+
 *  /            \
 * +--------------+
 * (x1a, y1)     (x1b, y1)
 */
class FTrapezoidsInt(capacity: Int = 5) {
    var assumeSorted: Boolean = false
    private val data = IntArrayList(capacity * 7)
    val size: Int get() = data.size / 6

    val area: Double get() = TODO()

    fun clear() { data.clear() }
    operator fun get(index: Int): Item = Item(index)
    inline fun fastForEach(block: FTrapezoidsInt.(Item) -> Unit) { for (n in 0 until size) this.block(this[n]) }
    inline fun <T> map(block: FTrapezoidsInt.(Item) -> T): List<T> = fastArrayListOf<T>().also { out -> fastForEach { out.add(block(it)) } }
    fun toTrapezoidIntList(): List<TrapezoidInt> = map { it.toTrapezoidInt() }

    override fun toString(): String = "FTrapezoidsInt[$size]"

    fun containsPoint(x: Int, y: Int, assumeSorted: Boolean = this.assumeSorted): Boolean = pointInside(x, y, assumeSorted)

    data class PointInsideStats(
        var found: Boolean = false,
        var iterations: Int = 0,
        var iterations2: Int = 0,
        var total: Int = 0,
    )

    // @TODO: Optimize for [assumeSorted] = true
    inline fun pointInside(
        x: Int,
        y: Int,
        assumeSorted: Boolean = this.assumeSorted,
        out: FTrapezoidsInt.(Item) -> Unit = { },
        stats: PointInsideStats? = null
    ): Boolean {
        //println("x=$x, y=$y")
        //println(this.map { it.toStringDefault() })
        var iterations = 0
        var iterations2 = 0
        var found = false
        if (assumeSorted) {
            val size = this.size
            val result = genericBinarySearchResult(0, size - 1, check = {
                iterations2++
                Item(it).y0.compareTo(y)
            })
            var index = result.nearIndex
            //println("xy=($x,$y), nearIndex=$index")

            while (index in 0 until size) {
                val item = Item(index)
                if (y > item.y1) break
                iterations++
                index--
            }
            index++
            for (n in index until size) {
                iterations++
                val item = Item(n)
                val inside = item.inside(x, y)
                //println("xy=($x,$y), inside=$inside, index=$n : ${item.toStringDefault()}")
                if (inside) {
                    found = true
                    break
                }
                if (item.y0 > y) break
            }
        } else {
            for (n in 0 until size) {
                iterations++
                val it = this[n]
                if (it.inside(x, y)) {
                    found = true
                    this.out(it)
                    break
                }
            }
        }
        if (stats != null) {
            stats.iterations = iterations
            stats.iterations2 = iterations2
            stats.total = size
            stats.found = found
        }
        //println("found=$found, iterations2=$iterations2, iterations=$iterations, total=$size")
        return found
    }

    companion object {
        operator fun invoke(capacity: Int = 5, block: FTrapezoidsInt.() -> Unit): FTrapezoidsInt =
            FTrapezoidsInt(capacity).apply(block)
    }

    fun Item.toStringDefault(): String = "Trapezoid[$index](($x0a, $x0b, $y0), ($x1a, $x1b, $y1))"

    /** Left coordinate of top part */
    var Item.x0a: Int; get() = data[index * 6 + 0]; set(value) { data[index * 6 + 0] = value }
    /** Right coordinate of top part */
    var Item.x0b: Int; get() = data[index * 6 + 1]; set(value) { data[index * 6 + 1] = value }
    /** Top coordinate */
    var Item.y0: Int; get() = data[index * 6 + 2]; set(value) { data[index * 6 + 2] = value }

    /** Left coordinate of bottom part */
    var Item.x1a: Int; get() = data[index * 6 + 3]; set(value) { data[index * 6 + 3] = value }
    /** Right coordinate of bottom part */
    var Item.x1b: Int; get() = data[index * 6 + 4]; set(value) { data[index * 6 + 4] = value }
    /** Bottom coordinate */
    var Item.y1: Int; get() = data[index * 6 + 5]; set(value) { data[index * 6 + 5] = value }

    val Item.area: Double get() = (y1 - y0).toDouble() * ((x0b - x0a) + (x1b - x1a)) / 2.0

    fun Item.containsY(y: Int): Boolean = y in y0..y1

    fun Item.toTrapezoidInt(): TrapezoidInt = TrapezoidInt(x0a, x0b, y0, x1a, x1b, y1)

    fun Item.inside(x: Int, y: Int): Boolean = TrapezoidInt.inside(x0a, x0b, y0, x1a, x1b, y1, x, y)
    fun Item.triangulate(out: FTrianglesInt = FTrianglesInt()): FTrianglesInt = TrapezoidInt.triangulate(x0a, x0b, y0, x1a, x1b, y1, out)

    fun add(x0a: Int, x0b: Int, y0: Int, x1a: Int, x1b: Int, y1: Int): Item = Item(size).also {
        data.add(x0a, x0b, y0, x1a, x1b, y1)
    }
    fun add(v: Item): Item = add(v.x0a, v.x0b, v.y0, v.x1a, v.x1b, v.y1)
    fun add(v: TrapezoidInt): Item = add(v.x0a, v.x0b, v.y0, v.x1a, v.x1b, v.y1)

    fun toInsideString(width: Int, height: Int, scale: Int = 1, inside: Char = '#', outside: Char = '.'): String {
        return Array(height) { y ->
            CharArray(width) { x -> if (containsPoint(((x + 0.5) * scale).toInt(), ((y + 0.5) * scale).toInt())) inside else outside }.concatToString()
        }.joinToString("\n")
    }

    @JvmInline
    value class Item(val index: Int) {
        inline fun <T> use(parallelograms: FTrapezoidsInt, block: FTrapezoidsInt.(Item) -> T): T = block(parallelograms, this)
    }
}

fun List<TrapezoidInt>.toFTrapezoidsInt(): FTrapezoidsInt = FTrapezoidsInt(this.size) {
    this@toFTrapezoidsInt.fastForEach { add(it) }
}

class FSegmentsInt(capacity: Int = 5) {
    companion object {
        operator fun invoke(block: FSegmentsInt.() -> Unit): FSegmentsInt = FSegmentsInt().also(block)
    }
    inline operator fun <R> invoke(block: FSegmentsInt.() -> R): R = run(block)
    private val data = IntArrayList(capacity * 4)
    val size: Int get() = data.size / 4

    fun clone(): FSegmentsInt = FSegmentsInt(size).also { out ->
        fastForEach { out.add(it.x0, it.y0, it.x1, it.y1) }
    }

    fun sortedBy(gen: FSegmentsInt.(Item) -> Int): FSegmentsInt = clone().also { it.sortBy(gen) }

    fun sortBy(gen: FSegmentsInt.(Item) -> Int) {
        genericSort(this, 0, size - 1, SortOps(gen))
    }

    fun swap(a: Item, b: Item) {
        val ax0 = a.x0
        val ay0 = a.y0
        val ax1 = a.x1
        val ay1 = a.y1
        a.setTo(b.x0, b.y0, b.x1, b.y1)
        b.setTo(ax0, ay0, ax1, ay1)
    }

    class SortOps(val gen: FSegmentsInt.(Item) -> Int) : korlibs.datastructure.SortOps<FSegmentsInt>() {
        override fun compare(subject: FSegmentsInt, l: Int, r: Int): Int = subject.gen(Item(l)) compareTo subject.gen(Item(r))
        override fun swap(subject: FSegmentsInt, indexL: Int, indexR: Int) = subject.swap(Item(indexL), Item(indexR))
    }

    operator fun get(index: Int): Item = Item(index)
    fun getOrNull(index: Int): Item? = if (index in 0 until size) get(index) else null
    inline fun fastForEach(block: FSegmentsInt.(Item) -> Unit) { for (n in 0 until size) this.block(this[n]) }
    inline fun <T> map(block: FSegmentsInt.(Item) -> T): List<T> = fastArrayListOf<T>().also { out -> fastForEach { out.add(block(it)) } }
    inline fun filter(block: FSegmentsInt.(Item) -> Boolean): FSegmentsInt = FSegmentsInt().also {  out ->
        this@FSegmentsInt.fastForEach { if (this@FSegmentsInt.block(it)) out.add(it, this) }
    }
    fun toSegmentIntList(): List<MSegmentInt> = map { it.toSegmentInt() }

    inline class Item(val index: Int) {
        inline fun <T> use(segments: FSegmentsInt, block: FSegmentsInt.(Item) -> T): T = block(segments, this)
    }

    var Item.x0: Int; get() = data[index * 4 + 0]; set(value) { data[index * 4 + 0] = value }
    var Item.y0: Int; get() = data[index * 4 + 1]; set(value) { data[index * 4 + 1] = value }
    var Item.x1: Int; get() = data[index * 4 + 2]; set(value) { data[index * 4 + 2] = value }
    var Item.y1: Int; get() = data[index * 4 + 3]; set(value) { data[index * 4 + 3] = value }

    fun Item.setTo(x0: Int, y0: Int, x1: Int, y1: Int) {
        this.x0 = x0
        this.y0 = y0
        this.x1 = x1
        this.y1 = y1
    }

    val Item.dx: Int get() = x1 - x0 // run
    val Item.dy: Int get() = y1 - y0 // rise
    val Item.slope: Double get() = dy.toDouble() / dx.toDouble()
    val Item.islope: Double get() = dx.toDouble() / dy.toDouble()
    val Item.xMin: Int get() = minOf(x0, x1)
    val Item.yMin: Int get() = minOf(y0, y1)
    val Item.xMax: Int get() = maxOf(x0, x1)
    val Item.yMax: Int get() = maxOf(y0, y1)
    fun Item.x(y: Int): Int = x0 + ((y - y0) * islope).toIntRound()
    fun Item.y(x: Int): Int = y0 + (slope * (x - x0)).toIntRound()
    fun Item.containsX(x: Int): Boolean = x in xMin..xMax
    fun Item.containsY(y: Int): Boolean = y in yMin..yMax
    fun Item.getIntersectY(other: Item): Int = MSegmentInt.getIntersectY(x0, y0, x1, y1, other.x0, other.y0, other.x1, other.y1)

    fun Item.toStringDefault(): String = "Segment[$index](($x0, $y0), ($x1, $y1))"
    fun Item.toSegmentInt(out: MSegmentInt = MSegmentInt()): MSegmentInt = out.also { it.setTo(x0, y0, x1, y1) }

    fun add(x0: Int, y0: Int, x1: Int, y1: Int): Item {
        val index = size
        data.add(x0, y0, x1, y1)
        return Item(index)
    }
    fun add(v: Item): Item = add(v.x0, v.y0, v.x1, v.y1)
    fun add(v: Item, segments: FSegmentsInt): Item = segments.run { this@FSegmentsInt.add(v.x0, v.y0, v.x1, v.y1) }
    fun add(v: MSegmentInt): Item = add(v.x0, v.y0, v.x1, v.y1)
}

fun List<MSegmentInt>.toFSegmentsInt(): FSegmentsInt = FSegmentsInt { this@toFSegmentsInt.fastForEach { add(it) } }

fun FSegmentsInt.getAllYSorted(): IntArray {
    val set = IntArray(size * 2)
    for (n in 0 until size) {
        val segment = this[n]
        set[n * 2 + 0] = segment.y0
        set[n * 2 + 1] = segment.y1
    }
    return set.distinct().toIntArray().sortedArray()

    //val set = IntSet()
    //for (n in 0 until size) {
    //    val segment = this[n]
    //    set.add(segment.y0)
    //    set.add(segment.y1)
    //}
    //return set.toIntArray().sortedArray()
}
