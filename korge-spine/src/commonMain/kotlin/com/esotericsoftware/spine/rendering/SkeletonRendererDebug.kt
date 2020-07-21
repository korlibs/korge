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

import com.esotericsoftware.spine.graphics.Color
import com.esotericsoftware.spine.rendering.ShapeRenderer.ShapeType
import com.esotericsoftware.spine.utils.Vector2
import com.esotericsoftware.spine.utils.JFloatArray
import com.esotericsoftware.spine.*
import com.esotericsoftware.spine.attachments.ClippingAttachment
import com.esotericsoftware.spine.attachments.MeshAttachment
import com.esotericsoftware.spine.attachments.PathAttachment
import com.esotericsoftware.spine.attachments.PointAttachment
import com.esotericsoftware.spine.attachments.RegionAttachment

class SkeletonRendererDebug {

    val shapeRenderer: ShapeRenderer
    private var drawBones = true
    private var drawRegionAttachments = true
    private var drawBoundingBoxes = true
    private var drawPoints = true
    private var drawMeshHull = true
    private var drawMeshTriangles = true
    private var drawPaths = true
    private var drawClipping = true
    private val bounds = SkeletonBounds()
    private val vertices = JFloatArray(32)
    private var scale = 1f
    private val boneWidth = 2f
    private var premultipliedAlpha: Boolean = false
    private val temp1 = Vector2()
    private val temp2 = Vector2()

    constructor() {
        shapeRenderer = ShapeRenderer()
    }

    constructor(shapes: ShapeRenderer) {
        this.shapeRenderer = shapes
    }

    fun draw(skeleton: Skeleton) {
        Gdx.gl!!.glEnable(GL20.GL_BLEND)
        val srcFunc = if (premultipliedAlpha) GL20.GL_ONE else GL20.GL_SRC_ALPHA
        Gdx.gl!!.glBlendFunc(srcFunc, GL20.GL_ONE_MINUS_SRC_ALPHA)

        val shapes = this.shapeRenderer
        val bones = skeleton.bones
        val slots = skeleton.slots

        shapes.begin(ShapeType.Filled)

        if (drawBones) {
            var i = 0
            val n = bones.size
            while (i < n) {
                val bone = bones[i]
                if (bone.parent == null || !bone.isActive) {
                    i++
                    continue
                }
                var length = bone.data.length
                var width = boneWidth
                if (length == 0f) {
                    length = 8f
                    width /= 2f
                    shapes.setColor(boneOriginColor)
                } else
                    shapes.setColor(boneLineColor)
                val x = length * bone.a + bone.worldX
                val y = length * bone.c + bone.worldY
                shapes.rectLine(bone.worldX, bone.worldY, x, y, width * scale)
                i++
            }
            shapes.x(skeleton.x, skeleton.y, 4 * scale)
        }

        if (drawPoints) {
            shapes.setColor(boneOriginColor)
            var i = 0
            val n = slots.size
            while (i < n) {
                val slot = slots[i]
                val attachment = slot.attachment
                if (attachment !is PointAttachment) {
                    i++
                    continue
                }
                attachment.computeWorldPosition(slot.bone, temp1)
                temp2.set(8f, 0f).rotate(attachment.computeWorldRotation(slot.bone))
                shapes.rectLine(temp1, temp2, boneWidth / 2 * scale)
                i++
            }
        }

        shapes.end()
        shapes.begin(ShapeType.Line)

        if (drawRegionAttachments) {
            shapes.setColor(attachmentLineColor)
            var i = 0
            val n = slots.size
            while (i < n) {
                val slot = slots[i]
                val attachment = slot.attachment
                if (attachment is RegionAttachment) {
                    val vertices = this.vertices.items
                    attachment.computeWorldVertices(slot.bone, vertices, 0, 2)
                    shapes.line(vertices[0], vertices[1], vertices[2], vertices[3])
                    shapes.line(vertices[2], vertices[3], vertices[4], vertices[5])
                    shapes.line(vertices[4], vertices[5], vertices[6], vertices[7])
                    shapes.line(vertices[6], vertices[7], vertices[0], vertices[1])
                }
                i++
            }
        }

        if (drawMeshHull || drawMeshTriangles) {
            var i = 0
            val n = slots.size
            while (i < n) {
                val slot = slots[i]
                val attachment = slot.attachment
                if (attachment !is MeshAttachment) {
                    i++
                    continue
                }
                val vertices = this.vertices.setSize(attachment.worldVerticesLength)
                attachment.computeWorldVertices(slot, 0, attachment.worldVerticesLength, vertices, 0, 2)
                val triangles = attachment.triangles
                val hullLength = attachment.hullLength
                if (drawMeshTriangles) {
                    shapes.setColor(triangleLineColor)
                    var ii = 0
                    val nn = triangles!!.size
                    while (ii < nn) {
                        val v1 = triangles[ii] * 2
                        val v2 = triangles[ii + 1] * 2
                        val v3 = triangles[ii + 2] * 2
                        shapes.triangle(vertices[v1], vertices[v1 + 1], //
                                vertices[v2], vertices[v2 + 1], //
                                vertices[v3], vertices[v3 + 1] //
                        )
                        ii += 3
                    }
                }
                if (drawMeshHull && hullLength > 0) {
                    shapes.setColor(attachmentLineColor)
                    var lastX = vertices[hullLength - 2]
                    var lastY = vertices[hullLength - 1]
                    var ii = 0
                    while (ii < hullLength) {
                        val x = vertices[ii]
                        val y = vertices[ii + 1]
                        shapes.line(x, y, lastX, lastY)
                        lastX = x
                        lastY = y
                        ii += 2
                    }
                }
                i++
            }
        }

        if (drawBoundingBoxes) {
            val bounds = this.bounds
            bounds.update(skeleton, true)
            shapes.setColor(aabbColor)
            shapes.rect(bounds.minX, bounds.minY, bounds.width, bounds.height)
            val polygons = bounds.polygons
            val boxes = bounds.boundingBoxes
            var i = 0
            val n = polygons.size
            while (i < n) {
                val polygon = polygons[i]
                shapes.setColor(boxes[i].color)
                shapes.polygon(polygon.items, 0, polygon.size)
                i++
            }
        }

        if (drawClipping) {
            var i = 0
            val n = slots.size
            while (i < n) {
                val slot = slots[i]
                val attachment = slot.attachment
                if (attachment !is ClippingAttachment) {
                    i++
                    continue
                }
                val nn = attachment.worldVerticesLength
                val vertices = this.vertices.setSize(nn)
                attachment.computeWorldVertices(slot, 0, nn, vertices, 0, 2)
                shapes.setColor(attachment.color)
                var ii = 2
                while (ii < nn) {
                    shapes.line(vertices[ii - 2], vertices[ii - 1], vertices[ii], vertices[ii + 1])
                    ii += 2
                }
                shapes.line(vertices[0], vertices[1], vertices[nn - 2], vertices[nn - 1])
                i++
            }
        }

        if (drawPaths) {
            var i = 0
            val n = slots.size
            while (i < n) {
                val slot = slots[i]
                val attachment = slot.attachment
                if (attachment !is PathAttachment) {
                    i++
                    continue
                }
                var nn = attachment.worldVerticesLength
                val vertices = this.vertices.setSize(nn)
                attachment.computeWorldVertices(slot, 0, nn, vertices, 0, 2)
                val color = attachment.color
                var x1 = vertices[2]
                var y1 = vertices[3]
                var x2 = 0f
                var y2 = 0f
                if (attachment.closed) {
                    shapes.setColor(color)
                    val cx1 = vertices[0]
                    val cy1 = vertices[1]
                    val cx2 = vertices[nn - 2]
                    val cy2 = vertices[nn - 1]
                    x2 = vertices[nn - 4]
                    y2 = vertices[nn - 3]
                    shapes.curve(x1, y1, cx1, cy1, cx2, cy2, x2, y2, 32)
                    shapes.setColor(Color.LIGHT_GRAY)
                    shapes.line(x1, y1, cx1, cy1)
                    shapes.line(x2, y2, cx2, cy2)
                }
                nn -= 4
                var ii = 4
                while (ii < nn) {
                    val cx1 = vertices[ii]
                    val cy1 = vertices[ii + 1]
                    val cx2 = vertices[ii + 2]
                    val cy2 = vertices[ii + 3]
                    x2 = vertices[ii + 4]
                    y2 = vertices[ii + 5]
                    shapes.setColor(color)
                    shapes.curve(x1, y1, cx1, cy1, cx2, cy2, x2, y2, 32)
                    shapes.setColor(Color.LIGHT_GRAY)
                    shapes.line(x1, y1, cx1, cy1)
                    shapes.line(x2, y2, cx2, cy2)
                    x1 = x2
                    y1 = y2
                    ii += 6
                }
                i++
            }
        }

        shapes.end()
        shapes.begin(ShapeType.Filled)

        if (drawBones) {
            shapes.setColor(boneOriginColor)
            var i = 0
            val n = bones.size
            while (i < n) {
                val bone = bones[i]
                if (!bone.isActive) {
                    i++
                    continue
                }
                shapes.circle(bone.worldX, bone.worldY, 3 * scale, 8)
                i++
            }
        }

        if (drawPoints) {
            shapes.setColor(boneOriginColor)
            var i = 0
            val n = slots.size
            while (i < n) {
                val slot = slots[i]
                val attachment = slot.attachment
                if (attachment !is PointAttachment) {
                    i++
                    continue
                }
                attachment.computeWorldPosition(slot.bone, temp1)
                shapes.circle(temp1.x, temp1.y, 3 * scale, 8)
                i++
            }
        }

        shapes.end()

    }

    fun setBones(bones: Boolean) {
        this.drawBones = bones
    }

    fun setScale(scale: Float) {
        this.scale = scale
    }

    fun setRegionAttachments(regionAttachments: Boolean) {
        this.drawRegionAttachments = regionAttachments
    }

    fun setBoundingBoxes(boundingBoxes: Boolean) {
        this.drawBoundingBoxes = boundingBoxes
    }

    fun setMeshHull(meshHull: Boolean) {
        this.drawMeshHull = meshHull
    }

    fun setMeshTriangles(meshTriangles: Boolean) {
        this.drawMeshTriangles = meshTriangles
    }

    fun setPaths(paths: Boolean) {
        this.drawPaths = paths
    }

    fun setPoints(points: Boolean) {
        this.drawPoints = points
    }

    fun setClipping(clipping: Boolean) {
        this.drawClipping = clipping
    }

    fun setPremultipliedAlpha(premultipliedAlpha: Boolean) {
        this.premultipliedAlpha = premultipliedAlpha
    }

    companion object {
        private val boneLineColor = Color.RED
        private val boneOriginColor = Color.GREEN
        private val attachmentLineColor = Color(0f, 0f, 1f, 0.5f)
        private val triangleLineColor = Color(1f, 0.64f, 0f, 0.5f) // ffa3007f
        private val aabbColor = Color(0f, 1f, 0f, 0.5f)
    }
}
