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

import com.esotericsoftware.spine.Slot
import com.esotericsoftware.spine.attachments.ClippingAttachment
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korim.color.*

class SkeletonClipping {
    private val triangulator = SpineTriangulator()
    private val clippingPolygon = FloatArrayList()
    private val clipOutput = FloatArrayList(128)
    val clippedVertices = FloatArrayList(128)
    val clippedTriangles = ShortArrayList(128)
    private val scratch = FloatArrayList()

    private var clipAttachment: ClippingAttachment? = null
    private var clippingPolygons: FastArrayList<FloatArrayList>? = null

    val isClipping: Boolean
        get() = clipAttachment != null

    fun clipStart(slot: Slot, clip: ClippingAttachment): Int {
        if (clipAttachment != null) return 0
        val n = clip.worldVerticesLength
        if (n < 6) return 0
        clipAttachment = clip

        val vertices = clippingPolygon.setSize(n)
        clip.computeWorldVertices(slot, 0, n, vertices, 0, 2)
        makeClockwise(clippingPolygon)
        val triangles = triangulator.triangulate(clippingPolygon)
        clippingPolygons = triangulator.decompose(clippingPolygon, triangles)
        clippingPolygons!!.fastForEach { polygon ->
            makeClockwise(polygon)
            polygon.add(polygon.data[0])
            polygon.add(polygon.data[1])
        }
        return clippingPolygons!!.size
    }

    fun clipEnd(slot: Slot) {
        if (clipAttachment != null && clipAttachment!!.endSlot === slot.data) clipEnd()
    }

    fun clipEnd() {
        if (clipAttachment == null) return
        clipAttachment = null
        clippingPolygons = null
        clippedVertices.clear()
        clippedTriangles.clear()
        clippingPolygon.clear()
    }

    fun clipTriangles(
        vertices: FloatArray, verticesLength: Int, triangles: ShortArray, trianglesLength: Int, uvs: FloatArray,
        light: RGBA, dark: RGBA, twoColor: Boolean
    ) {
        val light = Float.fromBits(light.value)
        val dark = Float.fromBits(dark.value)

        val clipOutput = this.clipOutput
        val clippedVertices = this.clippedVertices
        val clippedTriangles = this.clippedTriangles
        val polygons = clippingPolygons!!
        val polygonsCount = clippingPolygons!!.size
        val vertexSize = if (twoColor) 6 else 5

        var index: Short = 0
        clippedVertices.clear()
        clippedTriangles.clear()
        var i = 0
        outer@ while (i < trianglesLength) {
            var vertexOffset = triangles[i].toInt() shl 1
            val x1 = vertices[vertexOffset]
            val y1 = vertices[vertexOffset + 1]
            val u1 = uvs[vertexOffset]
            val v1 = uvs[vertexOffset + 1]

            vertexOffset = triangles[i + 1].toInt() shl 1
            val x2 = vertices[vertexOffset]
            val y2 = vertices[vertexOffset + 1]
            val u2 = uvs[vertexOffset]
            val v2 = uvs[vertexOffset + 1]

            vertexOffset = triangles[i + 2].toInt() shl 1
            val x3 = vertices[vertexOffset]
            val y3 = vertices[vertexOffset + 1]
            val u3 = uvs[vertexOffset]
            val v3 = uvs[vertexOffset + 1]

            for (p in 0 until polygonsCount) {
                var s = clippedVertices.size
                if (clip(x1, y1, x2, y2, x3, y3, polygons[p], clipOutput)) {
                    val clipOutputLength = clipOutput.size
                    if (clipOutputLength == 0) continue
                    val d0 = y2 - y3
                    val d1 = x3 - x2
                    val d2 = x1 - x3
                    val d4 = y3 - y1
                    val d = 1 / (d0 * d2 + d1 * (y1 - y3))

                    var clipOutputCount = clipOutputLength shr 1
                    val clipOutputItems = clipOutput.data
                    val clippedVerticesItems = clippedVertices.setSize(s + clipOutputCount * vertexSize)
                    run {
                        var ii = 0
                        while (ii < clipOutputLength) {
                            val x = clipOutputItems[ii]
                            val y = clipOutputItems[ii + 1]
                            clippedVerticesItems[s] = x
                            clippedVerticesItems[s + 1] = y
                            clippedVerticesItems[s + 2] = light
                            if (twoColor) {
                                clippedVerticesItems[s + 3] = dark
                                s += 4
                            } else
                                s += 3
                            val c0 = x - x3
                            val c1 = y - y3
                            val a = (d0 * c0 + d1 * c1) * d
                            val b = (d4 * c0 + d2 * c1) * d
                            val c = 1f - a - b
                            clippedVerticesItems[s] = u1 * a + u2 * b + u3 * c
                            clippedVerticesItems[s + 1] = v1 * a + v2 * b + v3 * c
                            s += 2
                            ii += 2
                        }
                    }

                    s = clippedTriangles.size
                    clippedTriangles.setSize(s + 3 * (clipOutputCount - 2))
                    val clippedTrianglesItems = clippedTriangles
                    clipOutputCount--
                    for (ii in 1 until clipOutputCount) {
                        clippedTrianglesItems[s] = index
                        clippedTrianglesItems[s + 1] = (index + ii).toShort()
                        clippedTrianglesItems[s + 2] = (index.toInt() + ii + 1).toShort()
                        s += 3
                    }
                    index = (index + (clipOutputCount + 1).toShort()).toShort()

                } else {
                    val clippedVerticesItems = clippedVertices.setSize(s + 3 * vertexSize)
                    clippedVerticesItems[s] = x1
                    clippedVerticesItems[s + 1] = y1
                    clippedVerticesItems[s + 2] = light
                    if (!twoColor) {
                        clippedVerticesItems[s + 3] = u1
                        clippedVerticesItems[s + 4] = v1

                        clippedVerticesItems[s + 5] = x2
                        clippedVerticesItems[s + 6] = y2
                        clippedVerticesItems[s + 7] = light
                        clippedVerticesItems[s + 8] = u2
                        clippedVerticesItems[s + 9] = v2

                        clippedVerticesItems[s + 10] = x3
                        clippedVerticesItems[s + 11] = y3
                        clippedVerticesItems[s + 12] = light
                        clippedVerticesItems[s + 13] = u3
                        clippedVerticesItems[s + 14] = v3
                    } else {
                        clippedVerticesItems[s + 3] = dark
                        clippedVerticesItems[s + 4] = u1
                        clippedVerticesItems[s + 5] = v1

                        clippedVerticesItems[s + 6] = x2
                        clippedVerticesItems[s + 7] = y2
                        clippedVerticesItems[s + 8] = light
                        clippedVerticesItems[s + 9] = dark
                        clippedVerticesItems[s + 10] = u2
                        clippedVerticesItems[s + 11] = v2

                        clippedVerticesItems[s + 12] = x3
                        clippedVerticesItems[s + 13] = y3
                        clippedVerticesItems[s + 14] = light
                        clippedVerticesItems[s + 15] = dark
                        clippedVerticesItems[s + 16] = u3
                        clippedVerticesItems[s + 17] = v3
                    }

                    s = clippedTriangles.size
                    val clippedTrianglesItems = clippedTriangles.setSize(s + 3)
                    clippedTrianglesItems[s] = index
                    clippedTrianglesItems[s + 1] = (index + 1).toShort()
                    clippedTrianglesItems[s + 2] = (index + 2).toShort()
                    index = (index + 3).toShort()
                    i += 3
                    continue@outer
                }
            }
            i += 3
        }
    }

    /** Clips the input triangle against the convex, clockwise clipping area. If the triangle lies entirely within the clipping
     * area, false is returned. The clipping area must duplicate the first vertex at the end of the vertices list.  */
    internal fun clip(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, clippingArea: FloatArrayList, output: FloatArrayList): Boolean {
        var output = output
        val originalOutput = output
        var clipped = false

        // Avoid copy at the end.
        var input: FloatArrayList? = null
        if (clippingArea.size % 4 >= 2) {
            input = output
            output = scratch
        } else
            input = scratch

        input.clear()
        input.add(x1)
        input.add(y1)
        input.add(x2)
        input.add(y2)
        input.add(x3)
        input.add(y3)
        input.add(x1)
        input.add(y1)
        output.clear()

        val clippingVertices = clippingArea.data
        val clippingVerticesLast = clippingArea.size - 4
        var i = 0
        while (true) {
            val edgeX = clippingVertices[i]
            val edgeY = clippingVertices[i + 1]
            val edgeX2 = clippingVertices[i + 2]
            val edgeY2 = clippingVertices[i + 3]
            val deltaX = edgeX - edgeX2
            val deltaY = edgeY - edgeY2

            val inputVertices = input!!.data
            val inputVerticesLength = input.size - 2
            val outputStart = output.size
            var ii = 0
            while (ii < inputVerticesLength) {
                val inputX = inputVertices[ii]
                val inputY = inputVertices[ii + 1]
                val inputX2 = inputVertices[ii + 2]
                val inputY2 = inputVertices[ii + 3]
                val side2 = deltaX * (inputY2 - edgeY2) - deltaY * (inputX2 - edgeX2) > 0
                if (deltaX * (inputY - edgeY2) - deltaY * (inputX - edgeX2) > 0) {
                    if (side2) { // v1 inside, v2 inside
                        output.add(inputX2)
                        output.add(inputY2)
                        ii += 2
                        continue
                    }
                    // v1 inside, v2 outside
                    val c0 = inputY2 - inputY
                    val c2 = inputX2 - inputX
                    val s = c0 * (edgeX2 - edgeX) - c2 * (edgeY2 - edgeY)
                    if (kotlin.math.abs(s) > 0.000001f) {
                        val ua = (c2 * (edgeY - inputY) - c0 * (edgeX - inputX)) / s
                        output.add(edgeX + (edgeX2 - edgeX) * ua)
                        output.add(edgeY + (edgeY2 - edgeY) * ua)
                    } else {
                        output.add(edgeX)
                        output.add(edgeY)
                    }
                } else if (side2) { // v1 outside, v2 inside
                    val c0 = inputY2 - inputY
                    val c2 = inputX2 - inputX
                    val s = c0 * (edgeX2 - edgeX) - c2 * (edgeY2 - edgeY)
                    if (kotlin.math.abs(s) > 0.000001f) {
                        val ua = (c2 * (edgeY - inputY) - c0 * (edgeX - inputX)) / s
                        output.add(edgeX + (edgeX2 - edgeX) * ua)
                        output.add(edgeY + (edgeY2 - edgeY) * ua)
                    } else {
                        output.add(edgeX)
                        output.add(edgeY)
                    }
                    output.add(inputX2)
                    output.add(inputY2)
                }
                clipped = true
                ii += 2
            }

            if (outputStart == output.size) { // All edges outside.
                originalOutput.clear()
                return true
            }

            output.add(output.data[0])
            output.add(output.data[1])

            if (i == clippingVerticesLast) break
            val temp = output
            output = input
            output.clear()
            input = temp
            i += 2
        }

        if (originalOutput !== output) {
            originalOutput.clear()
            originalOutput.add(output.data, 0, output.size - 2)
        } else
            originalOutput.setSize(originalOutput.size - 2)

        return clipped
    }

    companion object {

        internal fun makeClockwise(polygon: FloatArrayList) {
            val vertices = polygon.data
            val verticeslength = polygon.size

            var area = vertices[verticeslength - 2] * vertices[1] - vertices[0] * vertices[verticeslength - 1]
            var p1x: Float
            var p1y: Float
            var p2x: Float
            var p2y: Float
            run {
                var i = 0
                val n = verticeslength - 3
                while (i < n) {
                    p1x = vertices[i]
                    p1y = vertices[i + 1]
                    p2x = vertices[i + 2]
                    p2y = vertices[i + 3]
                    area += p1x * p2y - p2x * p1y
                    i += 2
                }
            }
            if (area < 0) return

            var i = 0
            val lastX = verticeslength - 2
            val n = verticeslength shr 1
            while (i < n) {
                val x = vertices[i]
                val y = vertices[i + 1]
                val other = lastX - i
                vertices[i] = vertices[other]
                vertices[i + 1] = vertices[other + 1]
                vertices[other] = x
                vertices[other + 1] = y
                i += 2
            }
        }
    }
}
