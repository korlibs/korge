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

package com.esotericsoftware.spine.rendering

import com.esotericsoftware.spine.graphics.*
import com.esotericsoftware.spine.utils.Vector2
import com.esotericsoftware.spine.utils.JFloatArray
import com.esotericsoftware.spine.*
import com.esotericsoftware.spine.utils.NumberUtils

import com.esotericsoftware.spine.attachments.ClippingAttachment
import com.esotericsoftware.spine.attachments.MeshAttachment
import com.esotericsoftware.spine.attachments.RegionAttachment
import com.esotericsoftware.spine.attachments.SkeletonAttachment
import com.esotericsoftware.spine.effect.*
import com.esotericsoftware.spine.utils.SkeletonClipping

class SkeletonRenderer {

    var premultipliedAlpha: Boolean = false
    private val vertices = JFloatArray(32)
    private val clipper = SkeletonClipping()
    /** @return May be null.
     */
    /** @param vertexEffect May be null.
     */
    var vertexEffect: VertexEffect? = null
    private val temp = Vector2()
    private val temp2 = Vector2()
    private val temp3 = Color()
    private val temp4 = Color()
    private val temp5 = Color()
    private val temp6 = Color()

    /** Renders the specified skeleton. If the batch is a PolygonSpriteBatch, [.draw] is
     * called. If the batch is a TwoColorPolygonBatch, [.draw] is called. Otherwise the
     * skeleton is rendered without two color tinting and any mesh attachments will throw an exception.
     *
     *
     * This method may change the batch's [blending function][Batch.setBlendFunctionSeparate]. The
     * previous blend function is not restored, since that could result in unnecessary flushes, depending on what is rendered
     * next.  */
    fun draw(batch: Batch, skeleton: Skeleton) {
        when (batch) {
            /*
            is TwoColorPolygonBatch -> {
                draw(batch, skeleton)
                return
            }
             */
            is PolygonSpriteBatch -> {
                draw(batch, skeleton)
                return
            }
        }

        TODO()
        /*
        val vertexEffect = this.vertexEffect
        vertexEffect?.begin(skeleton)

        val premultipliedAlpha = this.premultipliedAlpha
        var blendMode: BlendMode? = null
        val vertices = this.vertices.items
        val skeletonColor = skeleton.color
        val r = skeletonColor.r
        val g = skeletonColor.g
        val b = skeletonColor.b
        val a = skeletonColor.a
        val drawOrder = skeleton.drawOrder
        drawOrder.fastForEach { slot ->
            if (!slot.bone.isActive) {
                clipper.clipEnd(slot)
                return
            }
            val attachment = slot.attachment
            if (attachment is RegionAttachment) {
                attachment.computeWorldVertices(slot.bone, vertices, 0, 5)
                val color = attachment.color
                val slotColor = slot.color
                var alpha = a * slotColor.a * color.a * 255f
                val multiplier = if (premultipliedAlpha) alpha else 255f

                var slotBlendMode = slot.data.getBlendMode()
                if (slotBlendMode != blendMode) {
                    if (slotBlendMode == BlendMode.additive && premultipliedAlpha) {
                        slotBlendMode = BlendMode.normal
                        alpha = 0f
                    }
                    blendMode = slotBlendMode
                    batch.setBlendFunction(blendMode!!.getSource(premultipliedAlpha), blendMode!!.dest)
                }

                val c = NumberUtils.intToFloatColor(alpha.toInt() shl 24 //
                    or ((b * slotColor.b * color.b * multiplier).toInt() shl 16) //
                    or ((g * slotColor.g * color.g * multiplier).toInt() shl 8) //
                    or (r * slotColor.r * color.r * multiplier).toInt())
                val uvs = attachment.uVs
                var u = 0
                var v = 2
                while (u < 8) {
                    vertices[v] = c
                    vertices[v + 1] = uvs[u]
                    vertices[v + 2] = uvs[u + 1]
                    u += 2
                    v += 5
                }

                if (vertexEffect != null) applyVertexEffect(vertices, 20, 5, c, 0f)

                batch.draw(attachment.region.texture, vertices, 0, 20)

            } else if (attachment is ClippingAttachment) {
                clipper.clipStart(slot, attachment)
                return
            } else if (attachment is MeshAttachment) {
                throw RuntimeException(batch::class.simpleName + " cannot render meshes, PolygonSpriteBatch or TwoColorPolygonBatch is required.")
            } else if (attachment is SkeletonAttachment) {
                val attachmentSkeleton = attachment.skeleton
                if (attachmentSkeleton != null) draw(batch, attachmentSkeleton)
            }

            clipper.clipEnd(slot)
        }

        clipper.clipEnd()
        vertexEffect?.end()
         */
    }

    /** Renders the specified skeleton, including meshes, but without two color tinting.
     *
     *
     * This method may change the batch's [blending function][Batch.setBlendFunctionSeparate]. The
     * previous blend function is not restored, since that could result in unnecessary flushes, depending on what is rendered
     * next.  */
    fun draw(batch: PolygonSpriteBatch, skeleton: Skeleton) {
        val tempPosition = this.temp
        val tempUV = this.temp2
        val tempLight1 = this.temp3
        val tempDark1 = this.temp4
        val tempLight2 = this.temp5
        val tempDark2 = this.temp6
        val vertexEffect = this.vertexEffect
        vertexEffect?.begin(skeleton)

        val premultipliedAlpha = this.premultipliedAlpha
        var blendMode: BlendMode? = null
        var verticesLength = 0
        lateinit var vertices: FloatArray
        var uvs: FloatArray? = null
        lateinit var triangles: ShortArray
        var color: Color? = null
        val skeletonColor = skeleton.color
        val r = skeletonColor.r
        val g = skeletonColor.g
        val b = skeletonColor.b
        val a = skeletonColor.a
        val drawOrder = skeleton.drawOrder
        var i = 0
        val n = drawOrder.size
        while (i < n) {
            val slot = drawOrder[i]
            if (!slot.bone.isActive) {
                clipper.clipEnd(slot)
                i++
                continue
            }
            var texture: Texture? = null
            val vertexSize = if (clipper.isClipping) 2 else 5
            val attachment = slot.attachment
            if (attachment is RegionAttachment) {
                verticesLength = vertexSize shl 2
                vertices = this.vertices.items
                attachment.computeWorldVertices(slot.bone, vertices, 0, vertexSize)
                triangles = quadTriangles
                texture = attachment.region.texture
                uvs = attachment.uVs
                color = attachment.color

            } else if (attachment is MeshAttachment) {
                val count = attachment.worldVerticesLength
                verticesLength = (count shr 1) * vertexSize
                vertices = this.vertices.setSize(verticesLength)
                attachment.computeWorldVertices(slot, 0, count, vertices, 0, vertexSize)
                triangles = attachment.triangles
                texture = attachment.region!!.texture
                uvs = attachment.uVs
                color = attachment.color

            } else if (attachment is ClippingAttachment) {
                clipper.clipStart(slot, attachment)
                i++
                continue

            } else if (attachment is SkeletonAttachment) {
                val attachmentSkeleton = attachment.skeleton
                if (attachmentSkeleton != null) draw(batch, attachmentSkeleton)
            }

            if (texture != null) {
                val slotColor = slot.color
                var alpha = a * slotColor.a * color!!.a * 255f
                val multiplier = if (premultipliedAlpha) alpha else 255f

                var slotBlendMode = slot.data.getBlendMode()
                if (slotBlendMode != blendMode) {
                    if (slotBlendMode == BlendMode.additive && premultipliedAlpha) {
                        slotBlendMode = BlendMode.normal
                        alpha = 0f
                    }
                    blendMode = slotBlendMode
                    batch.setBlendFunction(blendMode!!.getSource(premultipliedAlpha), blendMode.dest)
                }

                val c = NumberUtils.intToFloatColor(alpha.toInt() shl 24 //
                        or ((b * slotColor.b * color.b * multiplier).toInt() shl 16) //
                        or ((g * slotColor.g * color.g * multiplier).toInt() shl 8) //
                        or (r * slotColor.r * color.r * multiplier).toInt())

                if (clipper.isClipping) {
                    clipper.clipTriangles(vertices, verticesLength, triangles, triangles.size, uvs!!, c, 0f, false)
                    val clippedVertices = clipper.clippedVertices
                    val clippedTriangles = clipper.clippedTriangles
                    if (vertexEffect != null) applyVertexEffect(clippedVertices.items, clippedVertices.size, 5, c, 0f)
                    batch.draw(texture, clippedVertices.items, 0, clippedVertices.size, clippedTriangles.items, 0,
                            clippedTriangles.size)
                } else {
                    if (vertexEffect != null) {
                        tempLight1.set(NumberUtils.floatToIntColor(c))
                        tempDark1.set(0)
                        var v = 0
                        var u = 0
                        while (v < verticesLength) {
                            tempPosition.x = vertices!![v]
                            tempPosition.y = vertices[v + 1]
                            tempLight2.set(tempLight1)
                            tempDark2.set(tempDark1)
                            tempUV.x = uvs!![u]
                            tempUV.y = uvs[u + 1]
                            vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2)
                            vertices[v] = tempPosition.x
                            vertices[v + 1] = tempPosition.y
                            vertices[v + 2] = tempLight2.toFloatBits()
                            vertices[v + 3] = tempUV.x
                            vertices[v + 4] = tempUV.y
                            v += 5
                            u += 2
                        }
                    } else {
                        var v = 2
                        var u = 0
                        while (v < verticesLength) {
                            vertices[v] = c
                            vertices[v + 1] = uvs!![u]
                            vertices[v + 2] = uvs[u + 1]
                            v += 5
                            u += 2
                        }
                    }
                    batch.draw(texture, vertices, 0, verticesLength, triangles, 0, triangles!!.size)
                }
            }

            clipper.clipEnd(slot)
            i++
        }
        clipper.clipEnd()
        vertexEffect?.end()
    }

    /*
    /** Renders the specified skeleton, including meshes and two color tinting.
     *
     *
     * This method may change the batch's [blending function][Batch.setBlendFunctionSeparate]. The
     * previous blend function is not restored, since that could result in unnecessary flushes, depending on what is rendered
     * next.  */
    fun draw(batch: TwoColorPolygonBatch, skeleton: Skeleton) {
        val tempPosition = this.temp
        val tempUV = this.temp2
        val tempLight1 = this.temp3
        val tempDark1 = this.temp4
        val tempLight2 = this.temp5
        val tempDark2 = this.temp6
        val vertexEffect = this.vertexEffect
        vertexEffect?.begin(skeleton)

        val premultipliedAlpha = this.premultipliedAlpha
        batch.setPremultipliedAlpha(premultipliedAlpha)
        var blendMode: BlendMode? = null
        var verticesLength = 0
        lateinit var vertices: FloatArray
        var uvs: FloatArray? = null
        lateinit var triangles: ShortArray
        var color: Color? = null
        val skeletonColor = skeleton.color
        val r = skeletonColor.r
        val g = skeletonColor.g
        val b = skeletonColor.b
        val a = skeletonColor.a
        val drawOrder = skeleton.drawOrder
        var i = 0
        val n = drawOrder.size
        while (i < n) {
            val slot = drawOrder[i]
            if (!slot.bone.isActive) {
                clipper.clipEnd(slot)
                i++
                continue
            }
            var texture: Texture? = null
            val vertexSize = if (clipper.isClipping) 2 else 6
            val attachment = slot.attachment
            if (attachment is RegionAttachment) {
                verticesLength = vertexSize shl 2
                vertices = this.vertices.items
                attachment.computeWorldVertices(slot.bone, vertices, 0, vertexSize)
                triangles = quadTriangles
                texture = attachment.region.texture
                uvs = attachment.uVs
                color = attachment.color

            } else if (attachment is MeshAttachment) {
                val count = attachment.worldVerticesLength
                verticesLength = (count shr 1) * vertexSize
                vertices = this.vertices.setSize(verticesLength)
                attachment.computeWorldVertices(slot, 0, count, vertices, 0, vertexSize)
                triangles = attachment.triangles
                texture = attachment.region!!.texture
                uvs = attachment.uVs
                color = attachment.color

            } else if (attachment is ClippingAttachment) {
                clipper.clipStart(slot, attachment)
                i++
                continue

            } else if (attachment is SkeletonAttachment) {
                val attachmentSkeleton = attachment.skeleton
                if (attachmentSkeleton != null) draw(batch, attachmentSkeleton)
            }

            if (texture != null) {
                val lightColor = slot.color
                var alpha = a * lightColor.a * color!!.a * 255f
                val multiplier = if (premultipliedAlpha) alpha else 255f

                var slotBlendMode = slot.data.getBlendMode()
                if (slotBlendMode != blendMode) {
                    if (slotBlendMode == BlendMode.additive && premultipliedAlpha) {
                        slotBlendMode = BlendMode.normal
                        alpha = 0f
                    }
                    blendMode = slotBlendMode
                    batch.setBlendFunction(blendMode!!.getSource(premultipliedAlpha), blendMode.dest)
                }

                val red = r * color.r * multiplier
                val green = g * color.g * multiplier
                val blue = b * color.b * multiplier
                val light = NumberUtils.intToFloatColor(alpha.toInt() shl 24 //

                        or ((blue * lightColor.b).toInt() shl 16) //

                        or ((green * lightColor.g).toInt() shl 8) //

                        or (red * lightColor.r).toInt())
                val darkColor = slot.darkColor
                val dark = if (darkColor == null)
                    0f
                else
                    NumberUtils.intToFloatColor((blue * darkColor.b).toInt() shl 16 //
                            or ((green * darkColor.g).toInt() shl 8 //
                            )
                            or (red * darkColor.r).toInt())

                if (clipper.isClipping) {
                    clipper.clipTriangles(vertices, verticesLength, triangles, triangles.size, uvs!!, light, dark, true)
                    val clippedVertices = clipper.clippedVertices
                    val clippedTriangles = clipper.clippedTriangles
                    if (vertexEffect != null) applyVertexEffect(clippedVertices.items, clippedVertices.size, 6, light, dark)
                    batch.drawTwoColor(texture, clippedVertices.items, 0, clippedVertices.size, clippedTriangles.items, 0,
                            clippedTriangles.size)
                } else {
                    if (vertexEffect != null) {
                        tempLight1.set(NumberUtils.floatToIntColor(light))
                        tempDark1.set(NumberUtils.floatToIntColor(dark))
                        var v = 0
                        var u = 0
                        while (v < verticesLength) {
                            tempPosition.x = vertices!![v]
                            tempPosition.y = vertices[v + 1]
                            tempLight2.set(tempLight1)
                            tempDark2.set(tempDark1)
                            tempUV.x = uvs!![u]
                            tempUV.y = uvs[u + 1]
                            vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2)
                            vertices[v] = tempPosition.x
                            vertices[v + 1] = tempPosition.y
                            vertices[v + 2] = tempLight2.toFloatBits()
                            vertices[v + 3] = tempDark2.toFloatBits()
                            vertices[v + 4] = tempUV.x
                            vertices[v + 5] = tempUV.y
                            v += 6
                            u += 2
                        }
                    } else {
                        var v = 2
                        var u = 0
                        while (v < verticesLength) {
                            vertices[v] = light
                            vertices[v + 1] = dark
                            vertices[v + 2] = uvs!![u]
                            vertices[v + 3] = uvs[u + 1]
                            v += 6
                            u += 2
                        }
                    }
                    batch.drawTwoColor(texture, vertices, 0, verticesLength, triangles, 0, triangles!!.size)
                }
            }

            clipper.clipEnd(slot)
            i++
        }
        clipper.clipEnd()
        vertexEffect?.end()
    }
     */

    private fun applyVertexEffect(vertices: FloatArray, verticesLength: Int, stride: Int, light: Float, dark: Float) {
        val tempPosition = this.temp
        val tempUV = this.temp2
        val tempLight1 = this.temp3
        val tempDark1 = this.temp4
        val tempLight2 = this.temp5
        val tempDark2 = this.temp6
        val vertexEffect = this.vertexEffect
        tempLight1.set(NumberUtils.floatToIntColor(light))
        tempDark1.set(NumberUtils.floatToIntColor(dark))
        if (stride == 5) {
            var v = 0
            while (v < verticesLength) {
                tempPosition.x = vertices[v]
                tempPosition.y = vertices[v + 1]
                tempUV.x = vertices[v + 3]
                tempUV.y = vertices[v + 4]
                tempLight2.set(tempLight1)
                tempDark2.set(tempDark1)
                vertexEffect!!.transform(tempPosition, tempUV, tempLight2, tempDark2)
                vertices[v] = tempPosition.x
                vertices[v + 1] = tempPosition.y
                vertices[v + 2] = tempLight2.toFloatBits()
                vertices[v + 3] = tempUV.x
                vertices[v + 4] = tempUV.y
                v += stride
            }
        } else {
            var v = 0
            while (v < verticesLength) {
                tempPosition.x = vertices[v]
                tempPosition.y = vertices[v + 1]
                tempUV.x = vertices[v + 4]
                tempUV.y = vertices[v + 5]
                tempLight2.set(tempLight1)
                tempDark2.set(tempDark1)
                vertexEffect!!.transform(tempPosition, tempUV, tempLight2, tempDark2)
                vertices[v] = tempPosition.x
                vertices[v + 1] = tempPosition.y
                vertices[v + 2] = tempLight2.toFloatBits()
                vertices[v + 3] = tempDark2.toFloatBits()
                vertices[v + 4] = tempUV.x
                vertices[v + 5] = tempUV.y
                v += stride
            }
        }
    }

    companion object {
        private val quadTriangles = shortArrayOf(0, 1, 2, 2, 3, 0)
    }
}
