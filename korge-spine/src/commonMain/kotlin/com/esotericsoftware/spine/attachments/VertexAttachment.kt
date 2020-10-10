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

package com.esotericsoftware.spine.attachments

import com.esotericsoftware.spine.Bone
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.Slot
import com.esotericsoftware.spine.utils.*
import com.esotericsoftware.spine.utils.SpineUtils.arraycopy
import com.soywiz.korio.concurrent.atomic.*
import kotlin.jvm.*
import kotlin.native.concurrent.*

/** Base class for an attachment with vertices that are transformed by one or more bones and can be deformed by a slot's
 * [Slot.getDeform].  */
abstract class VertexAttachment(name: String) : Attachment(name) {

    /** Returns a unique ID for this attachment.  */
    val id = nextID() and 65535 shl 11
    /** The bones which affect the [.getVertices]. The array entries are, for each vertex, the number of bones affecting
     * the vertex followed by that many bone indices, which is the index of the bone in [Skeleton.getBones]. Will be null
     * if this attachment has no weights.  */
    /** @param bones May be null if this attachment has no weights.
     */
    var bones: IntArray? = null

    /** The vertex positions in the bone's coordinate system. For a non-weighted attachment, the values are `x,y`
     * entries for each vertex. For a weighted attachment, the values are `x,y,weight` entries for each bone affecting
     * each vertex.  */
    var vertices: FloatArray? = null

    /** The maximum number of world vertex values that can be output by
     * [.computeWorldVertices] using the `count` parameter.  */
    var worldVerticesLength: Int = 0
    /** Deform keys for the deform attachment are also applied to this attachment.
     * @return May be null if no deform keys should be applied.
     */
    /** @param deformAttachment May be null if no deform keys should be applied.
     */
    var deformAttachment = this

    /** Transforms the attachment's local [.getVertices] to world coordinates. If the slot's [Slot.getDeform] is
     * not empty, it is used to deform the vertices.
     *
     *
     * See [World transforms](http://esotericsoftware.com/spine-runtime-skeletons#World-transforms) in the Spine
     * Runtimes Guide.
     * @param start The index of the first [.getVertices] value to transform. Each vertex has 2 values, x and y.
     * @param count The number of world vertex values to output. Must be <= [.getWorldVerticesLength] - `start`.
     * @param worldVertices The output world vertices. Must have a length >= `offset` + `count` *
     * `stride` / 2.
     * @param offset The `worldVertices` index to begin writing values.
     * @param stride The number of `worldVertices` entries between the value pairs written.
     */
    fun computeWorldVertices(slot: Slot, start: Int, count: Int, worldVertices: FloatArray, offset: Int, stride: Int) {
        var count = count
        count = offset + (count shr 1) * stride
        val skeleton = slot.skeleton
        val deformArray = slot.deform!!
        var vertices = this.vertices
        val bones = this.bones
        if (bones == null) {
            if (deformArray.size > 0) vertices = deformArray.data
            val bone = slot.bone
            val x = bone.worldX
            val y = bone.worldY
            val a = bone.a
            val b = bone.b
            val c = bone.c
            val d = bone.d
            var v = start
            var w = offset
            while (w < count) {
                val vx = vertices!![v]
                val vy = vertices[v + 1]
                worldVertices[w] = vx * a + vy * b + x
                worldVertices[w + 1] = vx * c + vy * d + y
                v += 2
                w += stride
            }
            return
        }
        var v = 0
        var skip = 0
        var i = 0
        while (i < start) {
            val n = bones[v]
            v += n + 1
            skip += n
            i += 2
        }
        val skeletonBones = skeleton.bones
        if (deformArray.size == 0) {
            var w = offset
            var b = skip * 3
            while (w < count) {
                var wx = 0f
                var wy = 0f
                var n = bones[v++]
                n += v
                while (v < n) {
                    val bone = skeletonBones[bones[v]] as Bone
                    val vx = vertices!![b]
                    val vy = vertices[b + 1]
                    val weight = vertices[b + 2]
                    wx += (vx * bone.a + vy * bone.b + bone.worldX) * weight
                    wy += (vx * bone.c + vy * bone.d + bone.worldY) * weight
                    v++
                    b += 3
                }
                worldVertices[w] = wx
                worldVertices[w + 1] = wy
                w += stride
            }
        } else {
            val deform = deformArray.data
            var w = offset
            var b = skip * 3
            var f = skip shl 1
            while (w < count) {
                var wx = 0f
                var wy = 0f
                var n = bones[v++]
                n += v
                while (v < n) {
                    val bone = skeletonBones[bones[v]] as Bone
                    val vx = vertices!![b] + deform[f]
                    val vy = vertices[b + 1] + deform[f + 1]
                    val weight = vertices[b + 2]
                    wx += (vx * bone.a + vy * bone.b + bone.worldX) * weight
                    wy += (vx * bone.c + vy * bone.d + bone.worldY) * weight
                    v++
                    b += 3
                    f += 2
                }
                worldVertices[w] = wx
                worldVertices[w + 1] = wy
                w += stride
            }
        }
    }

    /** Does not copy id (generated) or name (set on construction).  */
    internal fun copyTo(attachment: VertexAttachment) {
        if (bones != null) {
            attachment.bones = IntArray(bones!!.size)
            arraycopy(bones!!, 0, attachment.bones!!, 0, bones!!.size)
        } else
            attachment.bones = null

        if (vertices != null) {
            attachment.vertices = FloatArray(vertices!!.size)
            arraycopy(vertices!!, 0, attachment.vertices!!, 0, vertices!!.size)
        } else
            attachment.vertices = null

        attachment.worldVerticesLength = worldVerticesLength
        attachment.deformAttachment = deformAttachment
    }

    companion object {
        //private fun nextID(): Int = nextID.incrementAndGet() - 1
        private fun nextID(): Int = nextID++
    }
}

// @TODO: Do this properly.
@Suppress("VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL")
@ThreadLocal
//private var nextID = korAtomic(0)
private var nextID = 0
