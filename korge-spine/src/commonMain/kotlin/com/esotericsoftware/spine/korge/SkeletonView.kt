package com.esotericsoftware.spine.korge

import com.esotericsoftware.spine.*
import com.esotericsoftware.spine.BlendMode
import com.esotericsoftware.spine.attachments.*
import com.esotericsoftware.spine.effect.*
import com.esotericsoftware.spine.graphics.RGBAf
import com.esotericsoftware.spine.graphics.Texture
import com.esotericsoftware.spine.utils.*
import com.soywiz.kds.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*

inline fun Container.skeletonView(skeleton: Skeleton, animationState: AnimationState, block: @ViewDslMarker SkeletonView.() -> Unit = {})
    = SkeletonView(skeleton, animationState).addTo(this, block)

class SkeletonView(val skeleton: Skeleton, val animationState: AnimationState) : View() {
    init {
        addUpdater { delta ->
            animationState.update(delta.seconds.toFloat())
            animationState.apply(skeleton)
        }
    }

    override fun renderInternal(ctx: RenderContext) {
        skeleton.setPosition(0f, 0f)
        skeleton.updateWorldTransform()
        renderSkeleton(ctx, skeleton)
    }

    var premultipliedAlpha: Boolean = false
    private val vertices = FloatArrayList(32)
    private val clipper = SkeletonClipping()
    /** @return May be null.
     */
    /** @param vertexEffect May be null.
     */
    var vertexEffect: VertexEffect? = null
    private val temp = SpineVector2()
    private val temp2 = SpineVector2()
    private val temp3 = RGBAf()
    private val temp4 = RGBAf()
    private val temp5 = RGBAf()
    private val temp6 = RGBAf()

    private fun renderSkeleton(ctx: RenderContext, skeleton: Skeleton) {
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
        var color: RGBAf? = null
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
                vertices = this.vertices.data
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
                if (attachmentSkeleton != null) renderSkeleton(ctx, attachmentSkeleton)
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
                    //setBlendFunction(ctx, blendMode!!.getSource(premultipliedAlpha), blendMode.dest)
                }

                val c = RGBA(
                    (r * slotColor.r * color.r * multiplier).toInt(),
                    (g * slotColor.g * color.g * multiplier).toInt(),
                    (b * slotColor.b * color.b * multiplier).toInt(),
                    alpha.toInt()
                )

                if (clipper.isClipping) {
                    clipper.clipTriangles(vertices, verticesLength, triangles, triangles.size, uvs!!, c, Colors.BLACK, false)
                    val clippedVertices = clipper.clippedVertices
                    val clippedTriangles = clipper.clippedTriangles
                    if (vertexEffect != null) applyVertexEffect(clippedVertices.data, clippedVertices.size, 5, c, Colors.BLACK)
                    // @TODO: Remove clippedTriangles.toArray() so it doesn't allocate
                    draw(ctx, texture, clippedVertices.data, 0, clippedVertices.size, clippedTriangles.toArray(), 0, clippedTriangles.size, vertexSize)
                } else {
                    if (vertexEffect != null) {
                        tempLight1.setTo(c)
                        tempDark1.setTo(Colors.BLACK)
                        var v = 0
                        var u = 0
                        while (v < verticesLength) {
                            tempPosition.x = vertices!![v]
                            tempPosition.y = vertices[v + 1]
                            tempLight2.setTo(tempLight1)
                            tempDark2.setTo(tempDark1)
                            tempUV.x = uvs!![u]
                            tempUV.y = uvs[u + 1]
                            vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2)
                            vertices[v + 0] = tempPosition.x
                            vertices[v + 1] = tempPosition.y
                            vertices[v + 2] = tempLight2.toFloatBits()
                            vertices[v + 3] = tempUV.x
                            vertices[v + 4] = tempUV.y
                            v += 5
                            u += 2
                        }
                    } else {
                        var v = 0
                        var u = 0
                        while (v < verticesLength) {
                            vertices[v + 2] = Float.fromBits(c.value)
                            vertices[v + 3] = uvs!![u]
                            vertices[v + 4] = uvs[u + 1]
                            v += 5
                            u += 2
                        }
                    }
                    draw(ctx, texture, vertices, 0, verticesLength, triangles, 0, triangles.size, vertexSize)
                }

                //break // @TODO: Remove this
            }

            clipper.clipEnd(slot)
            i++

        }
        clipper.clipEnd()
        vertexEffect?.end()
    }

    private fun applyVertexEffect(vertices: FloatArray, verticesLength: Int, stride: Int, light: RGBA, dark: RGBA) {
        val tempPosition = this.temp
        val tempUV = this.temp2
        val tempLight1 = this.temp3
        val tempDark1 = this.temp4
        val tempLight2 = this.temp5
        val tempDark2 = this.temp6
        val vertexEffect = this.vertexEffect
        tempLight1.setTo(light)
        tempDark1.setTo(dark)
        if (stride == 5) {
            var v = 0
            while (v < verticesLength) {
                tempPosition.x = vertices[v]
                tempPosition.y = vertices[v + 1]
                tempUV.x = vertices[v + 3]
                tempUV.y = vertices[v + 4]
                tempLight2.setTo(tempLight1)
                tempDark2.setTo(tempDark1)
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
                tempLight2.setTo(tempLight1)
                tempDark2.setTo(tempDark1)
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

    //private fun setBlendFunction(ctx: RenderContext, source: Int, dest: Int) {
    //}

    private fun draw(ctx: RenderContext, texture: Texture, verticesData: FloatArray, verticesOffset: Int, verticesCount: Int, triangle: ShortArray, trianglesOffset: Int, trianglesCount: Int, vertexSize: Int) {
        val batch = ctx.batch
        //ctx.flush()

        val vertexCount = verticesCount / vertexSize

        batch.setStateFast(texture.bmp, true, blendMode.factors, null)
        batch.ensure(trianglesCount, vertexCount)

        val transform = this.globalMatrix

        for (n in 0 until trianglesCount) {
            batch.addIndexRelative(triangle[trianglesOffset + n].toInt())
        }
        if (vertexSize == 5) {
            val colorMul = this.colorMul
            val colorAdd = this.colorAdd
            for (n in 0 until vertexCount) {
                val x = verticesData[verticesOffset + n * vertexSize + 0]
                val y = -verticesData[verticesOffset + n * vertexSize + 1]
                val u = verticesData[verticesOffset + n * vertexSize + 3]
                val v = verticesData[verticesOffset + n * vertexSize + 4]
                val realX = transform.transformXf(x, y)
                val realY = transform.transformYf(x, y)
                batch.addVertex(realX, realY, u, v, colorMul, colorAdd)
            }
        } else {
            TODO()
        }

        //batch.flush()
    }

    @Deprecated("We shouldn't do this")
    private fun RGBAf.toFloatBits(): Float = Float.fromBits(this.rgba.value)

    companion object {
        private val quadTriangles = shortArrayOf(0, 1, 2, 2, 3, 0)
    }
}
