/******************************************************************************
 * Spine Runtimes License Agreement
 * Last updated January 1, 2020. Replaces all prior versions.
 *
 * Copyright (c) 2013-2020, Esoteric Software LLC
 *
 * Integration of the Spine Runtimes into software or otherwise creating
 * derivative works of the Spine Runtimes is permitted under the terms and
 * conditions of Section 2 of the Spine Editor License Agreement:
 * http://esotericsoftware.com/spine-editor-license
 *
 * Otherwise, it is permitted to integrate the Spine Runtimes into software
 * or otherwise create derivative works of the Spine Runtimes (collectively,
 * "Products"), provided that each user of the Products must obtain their own
 * Spine Editor license and redistribution of the Products in any form must
 * include this license and copyright notice.
 *
 * THE SPINE RUNTIMES ARE PROVIDED BY ESOTERIC SOFTWARE LLC "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ESOTERIC SOFTWARE LLC BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES,
 * BUSINESS INTERRUPTION, OR LOSS OF USE, DATA, OR PROFITS) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THE SPINE RUNTIMES, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.esotericsoftware.spine.utils

import com.soywiz.kds.*
import kotlin.math.*

internal class SpineTriangulator {
    private val convexPolygons = FastArrayList<FloatArrayList>()
    private val convexPolygonsIndices = FastArrayList<ShortArrayList>()

    private val indicesArray = ShortArrayList()
    private val isConcaveArray = BooleanArrayList()
    private val triangles = ShortArrayList()

    private val polygonPool = Pool { FloatArrayList(16) }
    private val polygonIndicesPool = Pool { ShortArrayList(16) }

    fun triangulate(verticesArray: FloatArrayList): ShortArrayList {
        val vertices = verticesArray.data
        var vertexCount = verticesArray.size shr 1

        val indicesArray = this.indicesArray
        indicesArray.clear()
        indicesArray.setSize(vertexCount)
        val indices = indicesArray
        for (i in 0 until vertexCount)
            indices[i] = i.toShort()

        val isConcaveArray = this.isConcaveArray
        isConcaveArray.setSize(vertexCount)
        val isConcave = isConcaveArray
        run {
            var i = 0
            val n = vertexCount
            while (i < n) {
                isConcave[i] = isConcave(i, vertexCount, vertices, indices)
                ++i
            }
        }

        val triangles = this.triangles
        triangles.clear()
        triangles.ensureCapacity(max(0, vertexCount - 2) shl 2)

        while (vertexCount > 3) {
            // Find ear tip.
            var previous = vertexCount - 1
            var i = 0
            var next = 1
            outer2@while (true) {
                outer@ while (true) {
                    if (!isConcave[i]) {
                        val p1 = indices[previous].toInt() shl 1
                        val p2 = indices[i].toInt() shl 1
                        val p3 = indices[next].toInt() shl 1
                        val p1x = vertices[p1]
                        val p1y = vertices[p1 + 1]
                        val p2x = vertices[p2]
                        val p2y = vertices[p2 + 1]
                        val p3x = vertices[p3]
                        val p3y = vertices[p3 + 1]
                        var ii = (next + 1) % vertexCount
                        while (ii != previous) {
                            if (isConcave[ii]) {
                                val v = indices[ii].toInt() shl 1
                                val vx = vertices[v]
                                val vy = vertices[v + 1]
                                if (positiveArea(p3x, p3y, p1x, p1y, vx, vy)) {
                                    if (positiveArea(p1x, p1y, p2x, p2y, vx, vy)) {
                                        if (positiveArea(p2x, p2y, p3x, p3y, vx, vy)) break@outer
                                    }
                                }
                            }
                            ii = (ii + 1) % vertexCount
                        }
                        break@outer2
                    }
                    break
                }

                if (next == 0) {
                    do {
                        if (!isConcave[i]) break
                        i--
                    } while (i > 0)
                    break
                }

                previous = i
                i = next
                next = (next + 1) % vertexCount
            }

            // Cut ear tip.
            triangles.add(indices[(vertexCount + i - 1) % vertexCount])
            triangles.add(indices[i])
            triangles.add(indices[(i + 1) % vertexCount])
            indicesArray.removeIndex(i)
            isConcaveArray.removeIndex(i)
            vertexCount--

            val previousIndex = (vertexCount + i - 1) % vertexCount
            val nextIndex = if (i == vertexCount) 0 else i
            isConcave[previousIndex] = isConcave(previousIndex, vertexCount, vertices, indices)
            isConcave[nextIndex] = isConcave(nextIndex, vertexCount, vertices, indices)
        }

        if (vertexCount == 3) {
            triangles.add(indices[2])
            triangles.add(indices[0])
            triangles.add(indices[1])
        }

        return triangles
    }

    fun decompose(verticesArray: FloatArrayList, triangles: ShortArrayList): FastArrayList<FloatArrayList> {
        val vertices = verticesArray.data

        val convexPolygons = this.convexPolygons
        polygonPool.free(convexPolygons)
        convexPolygons.clear()

        val convexPolygonsIndices = this.convexPolygonsIndices
        polygonIndicesPool.free(convexPolygonsIndices)
        convexPolygonsIndices.clear()

        var polygonIndices = polygonIndicesPool.alloc()
        polygonIndices.clear()

        var polygon = polygonPool.alloc()
        polygon.clear()

        // Merge subsequent triangles if they form a triangle fan.
        var fanBaseIndex = -1
        var lastWinding = 0
        val trianglesItems = triangles
        run {
            var i = 0
            val n = triangles.size
            while (i < n) {
                val t1 = trianglesItems[i].toInt() shl 1
                val t2 = trianglesItems[i + 1].toInt() shl 1
                val t3 = trianglesItems[i + 2].toInt() shl 1
                val x1 = vertices[t1]
                val y1 = vertices[t1 + 1]
                val x2 = vertices[t2]
                val y2 = vertices[t2 + 1]
                val x3 = vertices[t3]
                val y3 = vertices[t3 + 1]

                // If the base of the last triangle is the same as this triangle, check if they form a convex polygon (triangle fan).
                var merged = false
                if (fanBaseIndex == t1) {
                    val o = polygon.size - 4
                    val p = polygon.data
                    val winding1 = winding(p[o], p[o + 1], p[o + 2], p[o + 3], x3, y3)
                    val winding2 = winding(x3, y3, p[0], p[1], p[2], p[3])
                    if (winding1 == lastWinding && winding2 == lastWinding) {
                        polygon.add(x3)
                        polygon.add(y3)
                        polygonIndices.add(t3.toShort())
                        merged = true
                    }
                }

                // Otherwise make this triangle the new base.
                if (!merged) {
                    if (polygon.size > 0) {
                        convexPolygons.add(polygon)
                        convexPolygonsIndices.add(polygonIndices)
                    } else {
                        polygonPool.free(polygon)
                        polygonIndicesPool.free(polygonIndices)
                    }
                    polygon = polygonPool.alloc()
                    polygon.clear()
                    polygon.add(x1)
                    polygon.add(y1)
                    polygon.add(x2)
                    polygon.add(y2)
                    polygon.add(x3)
                    polygon.add(y3)
                    polygonIndices = polygonIndicesPool.alloc()
                    polygonIndices.clear()
                    polygonIndices.add(t1.toShort())
                    polygonIndices.add(t2.toShort())
                    polygonIndices.add(t3.toShort())
                    lastWinding = winding(x1, y1, x2, y2, x3, y3)
                    fanBaseIndex = t1
                }
                i += 3
            }
        }

        if (polygon.size > 0) {
            convexPolygons.add(polygon)
            convexPolygonsIndices.add(polygonIndices)
        }

        // Go through the list of polygons and try to merge the remaining triangles with the found triangle fans.
        run {
            var i = 0
            val n = convexPolygons.size
            while (i < n) {
                polygonIndices = convexPolygonsIndices.get(i)
                if (polygonIndices.size == 0) {
                    i++
                    continue
                }
                val firstIndex = polygonIndices.get(0).toInt()
                val lastIndex = polygonIndices.get(polygonIndices.size - 1).toInt()

                polygon = convexPolygons.get(i)
                val o = polygon.size - 4
                val p = polygon.data
                var prevPrevX = p[o]
                var prevPrevY = p[o + 1]
                var prevX = p[o + 2]
                var prevY = p[o + 3]
                val firstX = p[0]
                val firstY = p[1]
                val secondX = p[2]
                val secondY = p[3]
                val winding = winding(prevPrevX, prevPrevY, prevX, prevY, firstX, firstY)

                var ii = 0
                while (ii < n) {
                    if (ii == i) {
                        ii++
                        continue
                    }
                    val otherIndices = convexPolygonsIndices.get(ii)
                    if (otherIndices.size != 3) {
                        ii++
                        continue
                    }
                    val otherFirstIndex = otherIndices.get(0).toInt()
                    val otherSecondIndex = otherIndices.get(1).toInt()
                    val otherLastIndex = otherIndices.get(2).toInt()

                    val otherPoly = convexPolygons.get(ii)
                    val x3 = otherPoly.get(otherPoly.size - 2)
                    val y3 = otherPoly.get(otherPoly.size - 1)

                    if (otherFirstIndex != firstIndex || otherSecondIndex != lastIndex) {
                        ii++
                        continue
                    }
                    val winding1 = winding(prevPrevX, prevPrevY, prevX, prevY, x3, y3)
                    val winding2 = winding(x3, y3, firstX, firstY, secondX, secondY)
                    if (winding1 == winding && winding2 == winding) {
                        otherPoly.clear()
                        otherIndices.clear()
                        polygon.add(x3)
                        polygon.add(y3)
                        polygonIndices.add(otherLastIndex.toShort())
                        prevPrevX = prevX
                        prevPrevY = prevY
                        prevX = x3
                        prevY = y3
                        ii = 0
                    }
                    ii++
                }
                i++
            }
        }

        // Remove empty polygons that resulted from the merge step above.
        for (i in convexPolygons.size - 1 downTo 0) {
            polygon = convexPolygons.get(i)
            if (polygon.size == 0) {
                convexPolygons.removeAt(i)
                polygonPool.free(polygon)
                polygonIndices = convexPolygonsIndices.removeAt(i)
                polygonIndicesPool.free(polygonIndices)
            }
        }

        return convexPolygons
    }

    private fun isConcave(index: Int, vertexCount: Int, vertices: FloatArray, indices: ShortArrayList): Boolean {
        val previous = indices[(vertexCount + index - 1) % vertexCount].toInt() shl 1
        val current = indices[index].toInt() shl 1
        val next = indices[(index + 1) % vertexCount].toInt() shl 1
        return !positiveArea(vertices[previous], vertices[previous + 1], vertices[current], vertices[current + 1], vertices[next],
                vertices[next + 1])
    }

    private fun positiveArea(p1x: Float, p1y: Float, p2x: Float, p2y: Float, p3x: Float, p3y: Float): Boolean {
        return p1x * (p3y - p2y) + p2x * (p1y - p3y) + p3x * (p2y - p1y) >= 0
    }

    private fun winding(p1x: Float, p1y: Float, p2x: Float, p2y: Float, p3x: Float, p3y: Float): Int {
        val px = p2x - p1x
        val py = p2y - p1y
        return if (p3x * py - p3y * px + px * p1y - p1x * py >= 0) 1 else -1
    }
}
