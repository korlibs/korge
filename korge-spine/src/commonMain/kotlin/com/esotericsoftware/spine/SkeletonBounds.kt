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

package com.esotericsoftware.spine

import com.esotericsoftware.spine.attachments.BoundingBoxAttachment
import com.esotericsoftware.spine.utils.*
import com.soywiz.kds.*
import kotlin.math.*

/** Collects each visible [BoundingBoxAttachment] and computes the world vertices for its polygon. The polygon vertices are
 * provided along with convenience methods for doing hit detection.  */
class SkeletonBounds {
    /** The left edge of the axis aligned bounding box.  */
    var minX: Float = 0.toFloat()
        private set

    /** The bottom edge of the axis aligned bounding box.  */
    var minY: Float = 0.toFloat()
        private set

    /** The right edge of the axis aligned bounding box.  */
    var maxX: Float = 0.toFloat()
        private set

    /** The top edge of the axis aligned bounding box.  */
    var maxY: Float = 0.toFloat()
        private set

    /** The visible bounding boxes.  */
    val boundingBoxes: FastArrayList<BoundingBoxAttachment> = FastArrayList()

    /** The world vertices for the bounding box polygons.  */
    val polygons: FastArrayList<FloatArrayList> = FastArrayList()
    private val polygonPool = Pool { FloatArrayList() }

    /** The width of the axis aligned bounding box.  */
    val width: Float
        get() = maxX - minX

    /** The height of the axis aligned bounding box.  */
    val height: Float
        get() = maxY - minY

    /** Clears any previous polygons, finds all visible bounding box attachments, and computes the world vertices for each bounding
     * box's polygon.
     * @param updateAabb If true, the axis aligned bounding box containing all the polygons is computed. If false, the
     * SkeletonBounds AABB methods will always return true.
     */
    fun update(skeleton: Skeleton, updateAabb: Boolean) {
        val boundingBoxes = this.boundingBoxes
        val polygons = this.polygons
        val slots = skeleton.slots
        val slotCount = slots.size

        boundingBoxes.clear()
        polygonPool.free(polygons)
        polygons.clear()

        for (i in 0 until slotCount) {
            val slot = slots[i]
            if (!slot.bone.isActive) continue
            val attachment = slot.attachment
            if (attachment is BoundingBoxAttachment) {
                boundingBoxes.add(attachment)

                val polygon = polygonPool.alloc()
                polygons.add(polygon)
                attachment.computeWorldVertices(slot, 0, attachment.worldVerticesLength,
                        polygon.setSize(attachment.worldVerticesLength), 0, 2)
            }
        }

        if (updateAabb)
            aabbCompute()
        else {
            minX = Int.MIN_VALUE.toFloat()
            minY = Int.MIN_VALUE.toFloat()
            maxX = Int.MAX_VALUE.toFloat()
            maxY = Int.MAX_VALUE.toFloat()
        }
    }

    private fun aabbCompute() {
        var minX = Int.MAX_VALUE.toFloat()
        var minY = Int.MAX_VALUE.toFloat()
        var maxX = Int.MIN_VALUE.toFloat()
        var maxY = Int.MIN_VALUE.toFloat()
        val polygons = this.polygons
        var i = 0
        val n = polygons.size
        while (i < n) {
            val polygon = polygons[i]
            val vertices = polygon.data
            var ii = 0
            val nn = polygon.size
            while (ii < nn) {
                val x = vertices[ii]
                val y = vertices[ii + 1]
                minX = min(minX, x)
                minY = min(minY, y)
                maxX = max(maxX, x)
                maxY = max(maxY, y)
                ii += 2
            }
            i++
        }
        this.minX = minX
        this.minY = minY
        this.maxX = maxX
        this.maxY = maxY
    }

    /** Returns true if the axis aligned bounding box contains the point.  */
    fun aabbContainsPoint(x: Float, y: Float): Boolean {
        return x >= minX && x <= maxX && y >= minY && y <= maxY
    }

    /** Returns true if the axis aligned bounding box intersects the line segment.  */
    fun aabbIntersectsSegment(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val minX = this.minX
        val minY = this.minY
        val maxX = this.maxX
        val maxY = this.maxY
        if (x1 <= minX && x2 <= minX || y1 <= minY && y2 <= minY || x1 >= maxX && x2 >= maxX || y1 >= maxY && y2 >= maxY)
            return false
        val m = (y2 - y1) / (x2 - x1)
        var y = m * (minX - x1) + y1
        if (y > minY && y < maxY) return true
        y = m * (maxX - x1) + y1
        if (y > minY && y < maxY) return true
        var x = (minY - y1) / m + x1
        if (x > minX && x < maxX) return true
        x = (maxY - y1) / m + x1
        return if (x > minX && x < maxX) true else false
    }

    /** Returns true if the axis aligned bounding box intersects the axis aligned bounding box of the specified bounds.  */
    fun aabbIntersectsSkeleton(bounds: SkeletonBounds): Boolean {
        return minX < bounds.maxX && maxX > bounds.minX && minY < bounds.maxY && maxY > bounds.minY
    }

    /** Returns the first bounding box attachment that contains the point, or null. When doing many checks, it is usually more
     * efficient to only call this method if [.aabbContainsPoint] returns true.  */
    fun containsPoint(x: Float, y: Float): BoundingBoxAttachment? {
        val polygons = this.polygons
        var i = 0
        val n = polygons.size
        while (i < n) {
            if (containsPoint(polygons[i], x, y)) return boundingBoxes[i]
            i++
        }
        return null
    }

    /** Returns true if the polygon contains the point.  */
    fun containsPoint(polygon: FloatArrayList, x: Float, y: Float): Boolean {
        val vertices = polygon.data
        val nn = polygon.size

        var prevIndex = nn - 2
        var inside = false
        var ii = 0
        while (ii < nn) {
            val vertexY = vertices[ii + 1]
            val prevY = vertices[prevIndex + 1]
            if (vertexY < y && prevY >= y || prevY < y && vertexY >= y) {
                val vertexX = vertices[ii]
                if (vertexX + (y - vertexY) / (prevY - vertexY) * (vertices[prevIndex] - vertexX) < x) inside = !inside
            }
            prevIndex = ii
            ii += 2
        }
        return inside
    }

    /** Returns the first bounding box attachment that contains any part of the line segment, or null. When doing many checks, it
     * is usually more efficient to only call this method if [.aabbIntersectsSegment] returns
     * true.  */
    fun intersectsSegment(x1: Float, y1: Float, x2: Float, y2: Float): BoundingBoxAttachment? {
        val polygons = this.polygons
        var i = 0
        val n = polygons.size
        while (i < n) {
            if (intersectsSegment(polygons[i], x1, y1, x2, y2)) return boundingBoxes[i]
            i++
        }
        return null
    }

    /** Returns true if the polygon contains any part of the line segment.  */
    fun intersectsSegment(polygon: FloatArrayList, x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val vertices = polygon.data
        val nn = polygon.size

        val width12 = x1 - x2
        val height12 = y1 - y2
        val det1 = x1 * y2 - y1 * x2
        var x3 = vertices[nn - 2]
        var y3 = vertices[nn - 1]
        var ii = 0
        while (ii < nn) {
            val x4 = vertices[ii]
            val y4 = vertices[ii + 1]
            val det2 = x3 * y4 - y3 * x4
            val width34 = x3 - x4
            val height34 = y3 - y4
            val det3 = width12 * height34 - height12 * width34
            val x = (det1 * width34 - width12 * det2) / det3
            if ((x >= x3 && x <= x4 || x >= x4 && x <= x3) && (x >= x1 && x <= x2 || x >= x2 && x <= x1)) {
                val y = (det1 * height34 - height12 * det2) / det3
                if ((y >= y3 && y <= y4 || y >= y4 && y <= y3) && (y >= y1 && y <= y2 || y >= y2 && y <= y1)) return true
            }
            x3 = x4
            y3 = y4
            ii += 2
        }
        return false
    }

    /** Returns the polygon for the specified bounding box, or null.  */
    fun getPolygon(boundingBox: BoundingBoxAttachment): FloatArrayList? {
        val index = boundingBoxes.indexOfIdentity(boundingBox)
        return if (index == -1) null else polygons[index]
    }
}
