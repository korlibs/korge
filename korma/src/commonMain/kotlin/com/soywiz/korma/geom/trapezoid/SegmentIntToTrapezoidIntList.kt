package com.soywiz.korma.geom.trapezoid

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*

object SegmentIntToTrapezoidIntList {
    fun convert(path: VectorPath, scale: Int = 1): FTrapezoidsInt = convert(path.toSegments(scale), path.winding)

    fun convert(segments: List<SegmentInt>, winding: Winding = Winding.EVEN_ODD): FTrapezoidsInt {
        //segments.fastForEach { println("seg=$it") }
        val (allY, allSegmentsInY) = segmentLookups(segments)
        val trapezoids = FTrapezoidsInt()
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
        val len = bezier.length.toIntRound().coerceIn(2, 20)
        var oldX = 0.0
        var oldY = 0.0
        for (n in 0 .. len) {
            val ratio = n.toDouble() / len
            bezier.calc(ratio, p)
            if (n > 0) {
                emit(oldX, oldY, p.x, p.y)
            }
            oldX = p.x
            oldY = p.y
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

fun VectorPath.toTrapezoids(scale: Int = 1): FTrapezoidsInt =
    SegmentIntToTrapezoidIntList.convert(this, scale)

fun List<TrapezoidInt>.triangulate(out: FTrianglesInt = FTrianglesInt()): FTrianglesInt {
    fastForEach { it.triangulate(out) }
    return out
}

fun FTrapezoidsInt.triangulate(out: FTrianglesInt = FTrianglesInt()): FTrianglesInt {
    fastForEach { it.triangulate(out) }
    return out
}
