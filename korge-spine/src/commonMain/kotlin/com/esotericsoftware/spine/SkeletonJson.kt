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

import com.esotericsoftware.spine.assets.*
import com.esotericsoftware.spine.graphics.*
import com.esotericsoftware.spine.Animation.*
import com.esotericsoftware.spine.BoneData.*
import com.esotericsoftware.spine.PathConstraintData.*
import com.esotericsoftware.spine.attachments.*
import com.esotericsoftware.spine.utils.*
import com.esotericsoftware.spine.utils.SpineUtils.arraycopy
import com.soywiz.kds.*

/** Loads skeleton data in the Spine JSON format.
 *
 *
 * See [Spine JSON format](http://esotericsoftware.com/spine-json-format) and
 * [JSON and binary data](http://esotericsoftware.com/spine-loading-skeleton-data#JSON-and-binary-data) in the Spine
 * Runtimes Guide.  */
class SkeletonJson {
    private val attachmentLoader: AttachmentLoader

    /** Scales bone positions, image sizes, and translations as they are loaded. This allows different size images to be used at
     * runtime than were used in Spine.
     *
     *
     * See [Scaling](http://esotericsoftware.com/spine-loading-skeleton-data#Scaling) in the Spine Runtimes Guide.  */
    var scale = 1f
        set(scale) {
            require(scale != 0f) { "scale cannot be 0." }
            field = scale
        }
    private val linkedMeshes = ArrayList<LinkedMesh>()

    constructor(atlas: TextureAtlas) {
        attachmentLoader = AtlasAttachmentLoader(atlas)
    }

    constructor(attachmentLoader: AttachmentLoader) {
        this.attachmentLoader = attachmentLoader
    }

    protected fun parse(file: FileHandle): JsonValue? {
        return JsonReader().parse(file.readAsString())
    }

    fun readSkeletonData(file: FileHandle): SkeletonData {
        val scale = this.scale

        val skeletonData = SkeletonData()
        skeletonData.name = file.nameWithoutExtension()

        val root = parse(file)

        // Skeleton.
        val skeletonMap = root!!["skeleton"]
        if (skeletonMap != null) {
            skeletonData.hash = skeletonMap.getString("hash", null)
            skeletonData.version = skeletonMap.getString("spine", null)
            if ("3.8.75" == skeletonData.version)
                throw RuntimeException("Unsupported skeleton data, please export with a newer version of Spine.")
            skeletonData.x = skeletonMap.getFloat("x", 0f)
            skeletonData.y = skeletonMap.getFloat("y", 0f)
            skeletonData.width = skeletonMap.getFloat("width", 0f)
            skeletonData.height = skeletonMap.getFloat("height", 0f)
            skeletonData.fps = skeletonMap.getFloat("fps", 30f)
            skeletonData.imagesPath = skeletonMap.getString("images", null)
            skeletonData.audioPath = skeletonMap.getString("audio", null)
        }

        // Bones.
        var boneMap = root.getChild("bones")
        while (boneMap != null) {
            var parent: BoneData? = null
            val parentName = boneMap.getString("parent", null)
            if (parentName != null) {
                parent = skeletonData.findBone(parentName)
                if (parent == null) error("Parent bone not found: $parentName")
            }
            val data = BoneData(skeletonData.bones.size, boneMap.getString("name")!!, parent)
            data.length = boneMap.getFloat("length", 0f) * scale
            data.x = boneMap.getFloat("x", 0f) * scale
            data.y = boneMap.getFloat("y", 0f) * scale
            data.rotation = boneMap.getFloat("rotation", 0f)
            data.scaleX = boneMap.getFloat("scaleX", 1f)
            data.scaleY = boneMap.getFloat("scaleY", 1f)
            data.shearX = boneMap.getFloat("shearX", 0f)
            data.shearY = boneMap.getFloat("shearY", 0f)
            data.transformMode = TransformMode.valueOf(boneMap.getString("transform", TransformMode.normal.name)!!)
            data.skinRequired = boneMap.getBoolean("skin", false)

            val color = boneMap.getString("color", null)
            if (color != null) data.color.set(Color.valueOf(color))

            skeletonData.bones.add(data)
            boneMap = boneMap.next
        }

        // Slots.
        var slotMap = root.getChild("slots")
        while (slotMap != null) {
            val slotName = slotMap.getString("name")!!
            val boneName = slotMap.getString("bone")
            val boneData = skeletonData.findBone(boneName)
                    ?: error("Slot bone not found: " + boneName!!)
            val data = SlotData(skeletonData.slots.size, slotName, boneData)

            val color = slotMap.getString("color", null)
            if (color != null) data.color.set(Color.valueOf(color))

            val dark = slotMap.getString("dark", null)
            if (dark != null) data.darkColor = Color.valueOf(dark)

            data.attachmentName = slotMap.getString("attachment", null)
            data.blendMode = BlendMode.valueOf(slotMap.getString("blend", BlendMode.normal.name)!!)
            skeletonData.slots.add(data)
            slotMap = slotMap.next
        }

        // IK constraints.
        run {
            var constraintMap = root.getChild("ik")
            while (constraintMap != null) {
                val data = IkConstraintData(constraintMap!!.getString("name")!!)
                data.order = constraintMap!!.getInt("order", 0)
                data.skinRequired = constraintMap!!.getBoolean("skin", false)

                var entry = constraintMap!!.getChild("bones")
                while (entry != null) {
                    val bone = skeletonData.findBone(entry!!.asString())
                            ?: error("IK bone not found: " + entry!!)
                    data.bones.add(bone)
                    entry = entry!!.next
                }

                val targetName = constraintMap!!.getString("target")!!
                data.target = skeletonData.findBone(targetName)
                    ?: error("IK target bone not found: " + targetName!!)

                data.mix = constraintMap!!.getFloat("mix", 1f)
                data.softness = constraintMap!!.getFloat("softness", 0f) * scale
                data.bendDirection = if (constraintMap!!.getBoolean("bendPositive", true)) 1 else -1
                data.compress = constraintMap!!.getBoolean("compress", false)
                data.stretch = constraintMap!!.getBoolean("stretch", false)
                data.uniform = constraintMap!!.getBoolean("uniform", false)

                skeletonData.ikConstraints.add(data)
                constraintMap = constraintMap!!.next
            }
        }

        // Transform constraints.
        run {
            var constraintMap = root.getChild("transform")
            while (constraintMap != null) {
                val data = TransformConstraintData(constraintMap!!.getString("name")!!)
                data.order = constraintMap!!.getInt("order", 0)
                data.skinRequired = constraintMap!!.getBoolean("skin", false)

                var entry = constraintMap!!.getChild("bones")
                while (entry != null) {
                    val bone = skeletonData.findBone(entry!!.asString())
                            ?: error("Transform constraint bone not found: " + entry!!)
                    data.bones.add(bone)
                    entry = entry!!.next
                }

                val targetName = constraintMap!!.getString("target")
                data.target = skeletonData.findBone(targetName)
                    ?: error("Transform constraint target bone not found: " + targetName!!)

                data.local = constraintMap!!.getBoolean("local", false)
                data.relative = constraintMap!!.getBoolean("relative", false)

                data.offsetRotation = constraintMap!!.getFloat("rotation", 0f)
                data.offsetX = constraintMap!!.getFloat("x", 0f) * scale
                data.offsetY = constraintMap!!.getFloat("y", 0f) * scale
                data.offsetScaleX = constraintMap!!.getFloat("scaleX", 0f)
                data.offsetScaleY = constraintMap!!.getFloat("scaleY", 0f)
                data.offsetShearY = constraintMap!!.getFloat("shearY", 0f)

                data.rotateMix = constraintMap!!.getFloat("rotateMix", 1f)
                data.translateMix = constraintMap!!.getFloat("translateMix", 1f)
                data.scaleMix = constraintMap!!.getFloat("scaleMix", 1f)
                data.shearMix = constraintMap!!.getFloat("shearMix", 1f)

                skeletonData.transformConstraints.add(data)
                constraintMap = constraintMap!!.next
            }
        }

        // Path constraints.
        var constraintMap = root.getChild("path")
        while (constraintMap != null) {
            val data = PathConstraintData(constraintMap!!.getString("name")!!)
            data.order = constraintMap!!.getInt("order", 0)
            data.skinRequired = constraintMap!!.getBoolean("skin", false)

            var entry = constraintMap!!.getChild("bones")
            while (entry != null) {
                val bone = skeletonData.findBone(entry!!.asString())
                        ?: error("Path bone not found: " + entry!!)
                data.bones.add(bone)
                entry = entry!!.next
            }

            val targetName = constraintMap!!.getString("target")!!
            data.target = skeletonData.findSlot(targetName)
                ?: error("Path target slot not found: " + targetName!!)

            data.positionMode = PositionMode.valueOf(constraintMap!!.getString("positionMode", "percent")!!)
            data.spacingMode = SpacingMode.valueOf(constraintMap!!.getString("spacingMode", "length")!!)
            data.rotateMode = RotateMode.valueOf(constraintMap!!.getString("rotateMode", "tangent")!!)
            data.offsetRotation = constraintMap!!.getFloat("rotation", 0f)
            data.position = constraintMap!!.getFloat("position", 0f)
            if (data.positionMode == PositionMode.fixed) data.position *= scale
            data.spacing = constraintMap!!.getFloat("spacing", 0f)
            if (data.spacingMode == SpacingMode.length || data.spacingMode == SpacingMode.fixed) data.spacing *= scale
            data.rotateMix = constraintMap!!.getFloat("rotateMix", 1f)
            data.translateMix = constraintMap!!.getFloat("translateMix", 1f)

            skeletonData.pathConstraints.add(data)
            constraintMap = constraintMap!!.next
        }

        // Skins.
        var skinMap = root.getChild("skins")
        while (skinMap != null) {
            val skin = Skin(skinMap.getString("name")!!)
            run {
                var entry = skinMap!!.getChild("bones")
                while (entry != null) {
                    val bone = skeletonData.findBone(entry!!.asString())
                            ?: error("Skin bone not found: " + entry!!)
                    skin.bones.add(bone)
                    entry = entry!!.next
                }
            }
            run {
                var entry = skinMap!!.getChild("ik")
                while (entry != null) {
                    val constraint = skeletonData.findIkConstraint(entry!!.asString())
                            ?: error("Skin IK constraint not found: " + entry!!)
                    skin.constraints.add(constraint)
                    entry = entry!!.next
                }
            }
            run {
                var entry = skinMap!!.getChild("transform")
                while (entry != null) {
                    val constraint = skeletonData.findTransformConstraint(entry!!.asString())
                            ?: error("Skin transform constraint not found: " + entry!!)
                    skin.constraints.add(constraint)
                    entry = entry!!.next
                }
            }
            run {
                var entry = skinMap!!.getChild("path")
                while (entry != null) {
                    val constraint = skeletonData.findPathConstraint(entry!!.asString())
                            ?: error("Skin path constraint not found: " + entry!!)
                    skin.constraints.add(constraint)
                    entry = entry!!.next
                }
            }
            var slotEntry = skinMap.getChild("attachments")
            while (slotEntry != null) {
                val slot = skeletonData.findSlot(slotEntry.name)
                        ?: error("Slot not found: " + slotEntry.name!!)
                var entry = slotEntry.child
                while (entry != null) {
                    try {
                        val attachment = readAttachment(entry!!, skin, slot.index, entry!!.name, skeletonData)
                        if (attachment != null) skin.setAttachment(slot.index, entry!!.name!!, attachment)
                    } catch (ex: Throwable) {
                        throw RuntimeException("Error reading attachment: " + entry!!.name + ", skin: " + skin, ex)
                    }

                    entry = entry!!.next
                }
                slotEntry = slotEntry.next
            }
            skeletonData.skins.add(skin)
            if (skin.name == "default") skeletonData.defaultSkin = skin
            skinMap = skinMap.next
        }

        // Linked meshes.
        var i = 0
        val n = linkedMeshes.size
        while (i < n) {
            val linkedMesh = linkedMeshes.get(i)
            val skin = (if (linkedMesh.skin == null) skeletonData.defaultSkin else skeletonData.findSkin(linkedMesh.skin))
                    ?: error("Skin not found: " + linkedMesh.skin!!)
            val parent = skin.getAttachment(linkedMesh.slotIndex, linkedMesh.parent!!)
                    ?: error("Parent mesh not found: " + linkedMesh.parent)
            linkedMesh.mesh.deformAttachment = if (linkedMesh.inheritDeform) parent as VertexAttachment else linkedMesh.mesh
            linkedMesh.mesh.parentMesh = parent as MeshAttachment
            linkedMesh.mesh.updateUVs()
            i++
        }
        linkedMeshes.clear()

        // Events.
        var eventMap = root.getChild("events")
        while (eventMap != null) {
            val data = EventData(eventMap.name!!)
            data.int = eventMap.getInt("int", 0)
            data.float = eventMap.getFloat("float", 0f)
            data.stringValue = eventMap.getString("string", "")
            data.audioPath = eventMap.getString("audio", null)
            if (data.audioPath != null) {
                data.volume = eventMap.getFloat("volume", 1f)
                data.balance = eventMap.getFloat("balance", 0f)
            }
            skeletonData.events.add(data)
            eventMap = eventMap.next
        }

        // Animations.
        var animationMap = root.getChild("animations")
        while (animationMap != null) {
            try {
                readAnimation(animationMap, animationMap.name!!, skeletonData)
            } catch (ex: Throwable) {
                throw RuntimeException("Error reading animation: " + animationMap.name!!, ex)
            }

            animationMap = animationMap.next
        }

        skeletonData.bones.shrink()
        skeletonData.slots.shrink()
        skeletonData.skins.shrink()
        skeletonData.events.shrink()
        skeletonData.animations.shrink()
        skeletonData.ikConstraints.shrink()
        return skeletonData
    }

    private fun readAttachment(map: JsonValue, skin: Skin, slotIndex: Int, name: String?, skeletonData: SkeletonData): Attachment? {
        var name = name
        val scale = this.scale
        name = map.getString("name", name!!)

        val type = map.getString("type", AttachmentType.region.name)

        when (AttachmentType.valueOf(type)) {
            AttachmentType.region -> {
                val path = map.getString("path", name!!)
                val region = attachmentLoader.newRegionAttachment(skin, name, path) ?: return null
                region.path = path
                region.x = map.getFloat("x", 0f) * scale
                region.y = map.getFloat("y", 0f) * scale
                region.scaleX = map.getFloat("scaleX", 1f)
                region.scaleY = map.getFloat("scaleY", 1f)
                region.rotation = map.getFloat("rotation", 0f)
                region.width = map.getFloat("width") * scale
                region.height = map.getFloat("height") * scale

                val color = map.getString("color", null)
                if (color != null) region.color.set(Color.valueOf(color))

                region.updateOffset()
                return region
            }
            AttachmentType.boundingbox -> {
                val box = attachmentLoader.newBoundingBoxAttachment(skin, name) ?: return null
                readVertices(map, box, map.getInt("vertexCount") shl 1)

                val color = map.getString("color", null)
                if (color != null) box.color.set(Color.valueOf(color))
                return box
            }
            AttachmentType.mesh, AttachmentType.linkedmesh -> {
                val path = map.getString("path", name!!)
                val mesh = attachmentLoader.newMeshAttachment(skin, name, path) ?: return null
                mesh.path = path

                val color = map.getString("color", null)
                if (color != null) mesh.color.set(Color.valueOf(color))

                mesh.width = map.getFloat("width", 0f) * scale
                mesh.height = map.getFloat("height", 0f) * scale

                val parent = map.getString("parent", null)
                if (parent != null) {
                    linkedMeshes
                            .add(LinkedMesh(mesh, map.getString("skin", null), slotIndex, parent, map.getBoolean("deform", true)))
                    return mesh
                }

                val uvs = map.require("uvs").asFloatArray()
                readVertices(map, mesh, uvs.size)
                mesh.triangles = map.require("triangles").asShortArray()
                mesh.regionUVs = uvs
                mesh.updateUVs()

                if (map.has("hull")) mesh.hullLength = map.require("hull").asInt() * 2
                if (map.has("edges")) mesh.edges = map.require("edges").asShortArray()
                return mesh
            }
            AttachmentType.path -> {
                val path = attachmentLoader.newPathAttachment(skin, name) ?: return null
                path.closed = map.getBoolean("closed", false)
                path.constantSpeed = map.getBoolean("constantSpeed", true)

                val vertexCount = map.getInt("vertexCount")
                readVertices(map, path, vertexCount shl 1)

                val lengths = FloatArray(vertexCount / 3)
                var i = 0
                var curves = map.require("lengths").child
                while (curves != null) {
                    lengths[i++] = curves.asFloat() * scale
                    curves = curves.next
                }
                path.lengths = lengths

                val color = map.getString("color", null)
                if (color != null) path.color.set(Color.valueOf(color))
                return path
            }
            AttachmentType.point -> {
                val point = attachmentLoader.newPointAttachment(skin, name) ?: return null
                point.x = map.getFloat("x", 0f) * scale
                point.y = map.getFloat("y", 0f) * scale
                point.rotation = map.getFloat("rotation", 0f)

                val color = map.getString("color", null)
                if (color != null) point.color.set(Color.valueOf(color))
                return point
            }
            AttachmentType.clipping -> {
                val clip = attachmentLoader.newClippingAttachment(skin, name) ?: return null

                val end = map.getString("end", null)
                if (end != null) {
                    val slot = skeletonData.findSlot(end)
                            ?: error("Clipping end slot not found: $end")
                    clip.endSlot = slot
                }

                readVertices(map, clip, map.getInt("vertexCount") shl 1)

                val color = map.getString("color", null)
                if (color != null) clip.color.set(Color.valueOf(color))
                return clip
            }
        }
        return null
    }

    private fun readVertices(map: JsonValue, attachment: VertexAttachment, verticesLength: Int) {
        attachment.worldVerticesLength = verticesLength
        val vertices = map.require("vertices").asFloatArray()
        if (verticesLength == vertices.size) {
            if (this.scale != 1f) {
                var i = 0
                val n = vertices.size
                while (i < n) {
                    vertices[i] *= this.scale
                    i++
                }
            }
            attachment.vertices = vertices
            return
        }
        val weights = FloatArrayList(verticesLength * 3 * 3)
        val bones = IntArrayList(verticesLength * 3)
        var i = 0
        val n = vertices.size
        while (i < n) {
            val boneCount = vertices[i++].toInt()
            bones.add(boneCount)
            val nn = i + boneCount * 4
            while (i < nn) {
                bones.add(vertices[i].toInt())
                weights.add(vertices[i + 1] * this.scale)
                weights.add(vertices[i + 2] * this.scale)
                weights.add(vertices[i + 3])
                i += 4
            }
        }
        attachment.bones = bones.toArray()
        attachment.vertices = weights.toArray()
    }

    private fun readAnimation(map: JsonValue, name: String, skeletonData: SkeletonData) {
        val scale = this.scale
        val timelines = ArrayList<Timeline>()
        var duration = 0f

        // Slot timelines.
        run {
            var slotMap = map.getChild("slots")
            while (slotMap != null) {
                val slot = skeletonData.findSlot(slotMap!!.name)
                        ?: error("Slot not found: " + slotMap!!.name!!)
                var timelineMap = slotMap!!.child
                while (timelineMap != null) {
                    val timelineName = timelineMap!!.name
                    if (timelineName == "attachment") {
                        val timeline = AttachmentTimeline(timelineMap!!.size)
                        timeline.slotIndex = slot.index

                        var frameIndex = 0
                        var valueMap = timelineMap!!.child
                        while (valueMap != null) {
                            timeline.setFrame(frameIndex++, valueMap!!.getFloat("time", 0f), valueMap!!.getString("name")!!)
                            valueMap = valueMap!!.next
                        }
                        timelines.add(timeline)
                        duration = kotlin.math.max(duration, timeline.frames[timeline.frameCount - 1])

                    } else if (timelineName == "color") {
                        val timeline = ColorTimeline(timelineMap!!.size)
                        timeline.slotIndex = slot.index

                        var frameIndex = 0
                        var valueMap = timelineMap!!.child
                        while (valueMap != null) {
                            val color = Color.valueOf(valueMap!!.getString("color")!!)
                            timeline.setFrame(frameIndex, valueMap!!.getFloat("time", 0f), color.r, color.g, color.b, color.a)
                            readCurve(valueMap!!, timeline, frameIndex)
                            frameIndex++
                            valueMap = valueMap!!.next
                        }
                        timelines.add(timeline)
                        duration = kotlin.math.max(duration, timeline.frames[(timeline.frameCount - 1) * ColorTimeline.ENTRIES])

                    } else if (timelineName == "twoColor") {
                        val timeline = TwoColorTimeline(timelineMap!!.size)
                        timeline.slotIndex = slot.index

                        var frameIndex = 0
                        var valueMap = timelineMap!!.child
                        while (valueMap != null) {
                            val light = Color.valueOf(valueMap!!.getString("light")!!)
                            val dark = Color.valueOf(valueMap!!.getString("dark")!!)
                            timeline.setFrame(frameIndex, valueMap!!.getFloat("time", 0f), light.r, light.g, light.b, light.a, dark.r, dark.g,
                                    dark.b)
                            readCurve(valueMap!!, timeline, frameIndex)
                            frameIndex++
                            valueMap = valueMap!!.next
                        }
                        timelines.add(timeline)
                        duration = kotlin.math.max(duration, timeline.frames[(timeline.frameCount - 1) * TwoColorTimeline.ENTRIES])

                    } else
                        throw RuntimeException("Invalid timeline type for a slot: " + timelineName + " (" + slotMap!!.name + ")")
                    timelineMap = timelineMap!!.next
                }
                slotMap = slotMap!!.next
            }
        }

        // Bone timelines.
        var boneMap = map.getChild("bones")
        while (boneMap != null) {
            val bone = skeletonData.findBone(boneMap.name)
                    ?: error("Bone not found: " + boneMap.name!!)
            var timelineMap = boneMap.child
            while (timelineMap != null) {
                val timelineName = timelineMap!!.name
                if (timelineName == "rotate") {
                    val timeline = RotateTimeline(timelineMap!!.size)
                    timeline.boneIndex = bone.index

                    var frameIndex = 0
                    var valueMap = timelineMap!!.child
                    while (valueMap != null) {
                        timeline.setFrame(frameIndex, valueMap!!.getFloat("time", 0f), valueMap!!.getFloat("angle", 0f))
                        readCurve(valueMap!!, timeline, frameIndex)
                        frameIndex++
                        valueMap = valueMap!!.next
                    }
                    timelines.add(timeline)
                    duration = kotlin.math.max(duration, timeline.frames[(timeline.frameCount - 1) * RotateTimeline.ENTRIES])

                } else if (timelineName == "translate" || timelineName == "scale" || timelineName == "shear") {
                    val timeline: TranslateTimeline
                    var timelineScale = 1f
                    var defaultValue = 0f
                    if (timelineName == "scale") {
                        timeline = ScaleTimeline(timelineMap!!.size)
                        defaultValue = 1f
                    } else if (timelineName == "shear")
                        timeline = ShearTimeline(timelineMap!!.size)
                    else {
                        timeline = TranslateTimeline(timelineMap!!.size)
                        timelineScale = scale
                    }
                    timeline.boneIndex = bone.index

                    var frameIndex = 0
                    var valueMap = timelineMap!!.child
                    while (valueMap != null) {
                        val x = valueMap!!.getFloat("x", defaultValue)
                        val y = valueMap!!.getFloat("y", defaultValue)
                        timeline.setFrame(frameIndex, valueMap!!.getFloat("time", 0f), x * timelineScale, y * timelineScale)
                        readCurve(valueMap!!, timeline, frameIndex)
                        frameIndex++
                        valueMap = valueMap!!.next
                    }
                    timelines.add(timeline)
                    duration = kotlin.math.max(duration, timeline.frames[(timeline.frameCount - 1) * TranslateTimeline.ENTRIES])

                } else
                    throw RuntimeException("Invalid timeline type for a bone: " + timelineName + " (" + boneMap.name + ")")
                timelineMap = timelineMap!!.next
            }
            boneMap = boneMap.next
        }

        // IK constraint timelines.
        run {
            var constraintMap = map.getChild("ik")
            while (constraintMap != null) {
                val constraint = skeletonData.findIkConstraint(constraintMap!!.name)
                val timeline = IkConstraintTimeline(constraintMap!!.size)
                timeline.ikConstraintIndex = skeletonData.ikConstraints.indexOfIdentity(constraint)
                var frameIndex = 0
                var valueMap = constraintMap!!.child
                while (valueMap != null) {
                    timeline.setFrame(frameIndex, valueMap!!.getFloat("time", 0f), valueMap!!.getFloat("mix", 1f),
                            valueMap!!.getFloat("softness", 0f) * scale, if (valueMap!!.getBoolean("bendPositive", true)) 1 else -1,
                            valueMap!!.getBoolean("compress", false), valueMap!!.getBoolean("stretch", false))
                    readCurve(valueMap!!, timeline, frameIndex)
                    frameIndex++
                    valueMap = valueMap!!.next
                }
                timelines.add(timeline)
                duration = kotlin.math.max(duration, timeline.frames[(timeline.frameCount - 1) * IkConstraintTimeline.ENTRIES])
                constraintMap = constraintMap!!.next
            }
        }

        // Transform constraint timelines.
        run {
            var constraintMap = map.getChild("transform")
            while (constraintMap != null) {
                val constraint = skeletonData.findTransformConstraint(constraintMap!!.name)
                val timeline = TransformConstraintTimeline(constraintMap!!.size)
                timeline.transformConstraintIndex = skeletonData.transformConstraints.indexOfIdentity(constraint)
                var frameIndex = 0
                var valueMap = constraintMap!!.child
                while (valueMap != null) {
                    timeline.setFrame(frameIndex, valueMap!!.getFloat("time", 0f), valueMap!!.getFloat("rotateMix", 1f),
                            valueMap!!.getFloat("translateMix", 1f), valueMap!!.getFloat("scaleMix", 1f), valueMap!!.getFloat("shearMix", 1f))
                    readCurve(valueMap!!, timeline, frameIndex)
                    frameIndex++
                    valueMap = valueMap!!.next
                }
                timelines.add(timeline)
                duration = kotlin.math.max(duration,
                        timeline.frames[(timeline.frameCount - 1) * TransformConstraintTimeline.ENTRIES])
                constraintMap = constraintMap!!.next
            }
        }

        // Path constraint timelines.
        var constraintMap = map.getChild("path")
        while (constraintMap != null) {
            val data = skeletonData.findPathConstraint(constraintMap!!.name)
                    ?: error("Path constraint not found: " + constraintMap!!.name!!)
            val index = skeletonData.pathConstraints.indexOfIdentity(data)
            var timelineMap = constraintMap!!.child
            while (timelineMap != null) {
                val timelineName = timelineMap!!.name
                if (timelineName == "position" || timelineName == "spacing") {
                    val timeline: PathConstraintPositionTimeline
                    var timelineScale = 1f
                    if (timelineName == "spacing") {
                        timeline = PathConstraintSpacingTimeline(timelineMap!!.size)
                        if (data.spacingMode == SpacingMode.length || data.spacingMode == SpacingMode.fixed) timelineScale = scale
                    } else {
                        timeline = PathConstraintPositionTimeline(timelineMap!!.size)
                        if (data.positionMode == PositionMode.fixed) timelineScale = scale
                    }
                    timeline.pathConstraintIndex = index
                    var frameIndex = 0
                    var valueMap = timelineMap!!.child
                    while (valueMap != null) {
                        timeline.setFrame(frameIndex, valueMap!!.getFloat("time", 0f), valueMap!!.getFloat(timelineName, 0f) * timelineScale)
                        readCurve(valueMap!!, timeline, frameIndex)
                        frameIndex++
                        valueMap = valueMap!!.next
                    }
                    timelines.add(timeline)
                    duration = kotlin.math.max(duration,
                            timeline.frames[(timeline.frameCount - 1) * PathConstraintPositionTimeline.ENTRIES])
                } else if (timelineName == "mix") {
                    val timeline = PathConstraintMixTimeline(timelineMap!!.size)
                    timeline.pathConstraintIndex = index
                    var frameIndex = 0
                    var valueMap = timelineMap!!.child
                    while (valueMap != null) {
                        timeline.setFrame(frameIndex, valueMap!!.getFloat("time", 0f), valueMap!!.getFloat("rotateMix", 1f),
                                valueMap!!.getFloat("translateMix", 1f))
                        readCurve(valueMap!!, timeline, frameIndex)
                        frameIndex++
                        valueMap = valueMap!!.next
                    }
                    timelines.add(timeline)
                    duration = kotlin.math.max(duration,
                            timeline.frames[(timeline.frameCount - 1) * PathConstraintMixTimeline.ENTRIES])
                }
                timelineMap = timelineMap!!.next
            }
            constraintMap = constraintMap!!.next
        }

        // Deform timelines.
        var deformMap = map.getChild("deform")
        while (deformMap != null) {
            val skin = skeletonData.findSkin(deformMap.name)
                    ?: error("Skin not found: " + deformMap.name!!)
            var slotMap = deformMap.child
            while (slotMap != null) {
                val slot = skeletonData.findSlot(slotMap!!.name)
                        ?: error("Slot not found: " + slotMap!!.name!!)
                var timelineMap = slotMap!!.child
                while (timelineMap != null) {
                    val attachment = skin.getAttachment(slot.index, timelineMap!!.name!!) as? VertexAttachment?
                            ?: error("Deform attachment not found: " + timelineMap!!.name!!)
                    val weighted = attachment.bones != null
                    val vertices = attachment.vertices
                    val deformLength = if (weighted) vertices!!.size / 3 * 2 else vertices!!.size

                    val timeline = DeformTimeline(timelineMap!!.size)
                    timeline.slotIndex = slot.index
                    timeline.attachment = attachment

                    var frameIndex = 0
                    var valueMap = timelineMap!!.child
                    while (valueMap != null) {
                        val deform: FloatArray
                        val verticesValue = valueMap!!["vertices"]
                        if (verticesValue == null)
                            deform = if (weighted) FloatArray(deformLength) else vertices
                        else {
                            deform = FloatArray(deformLength)
                            val start = valueMap!!.getInt("offset", 0)
                            arraycopy(verticesValue.asFloatArray(), 0, deform, start, verticesValue.size)
                            if (scale != 1f) {
                                var i = start
                                val n = i + verticesValue.size
                                while (i < n) {
                                    deform[i] *= scale
                                    i++
                                }
                            }
                            if (!weighted) {
                                for (i in 0 until deformLength)
                                    deform[i] += vertices[i]
                            }
                        }

                        timeline.setFrame(frameIndex, valueMap!!.getFloat("time", 0f), deform)
                        readCurve(valueMap!!, timeline, frameIndex)
                        frameIndex++
                        valueMap = valueMap!!.next
                    }
                    timelines.add(timeline)
                    duration = kotlin.math.max(duration, timeline.frames[timeline.frameCount - 1])
                    timelineMap = timelineMap!!.next
                }
                slotMap = slotMap!!.next
            }
            deformMap = deformMap.next
        }

        // Draw order timeline.
        var drawOrdersMap = map["drawOrder"]
        if (drawOrdersMap == null) drawOrdersMap = map["draworder"]
        if (drawOrdersMap != null) {
            val timeline = DrawOrderTimeline(drawOrdersMap.size)
            val slotCount = skeletonData.slots.size
            var frameIndex = 0
            var drawOrderMap = drawOrdersMap.child
            while (drawOrderMap != null) {
                var drawOrder: IntArray? = null
                val offsets = drawOrderMap["offsets"]
                if (offsets != null) {
                    drawOrder = IntArray(slotCount)
                    for (i in slotCount - 1 downTo 0)
                        drawOrder[i] = -1
                    val unchanged = IntArray(slotCount - offsets.size)
                    var originalIndex = 0
                    var unchangedIndex = 0
                    var offsetMap = offsets.child
                    while (offsetMap != null) {
                        val slot = skeletonData.findSlot(offsetMap.getString("slot"))
                                ?: error("Slot not found: " + offsetMap.getString("slot")!!)
// Collect unchanged items.
                        while (originalIndex != slot.index)
                            unchanged[unchangedIndex++] = originalIndex++
                        // Set changed items.
                        drawOrder[originalIndex + offsetMap.getInt("offset")] = originalIndex++
                        offsetMap = offsetMap.next
                    }
                    // Collect remaining unchanged items.
                    while (originalIndex < slotCount)
                        unchanged[unchangedIndex++] = originalIndex++
                    // Fill in unchanged items.
                    for (i in slotCount - 1 downTo 0)
                        if (drawOrder[i] == -1) drawOrder[i] = unchanged[--unchangedIndex]
                }
                timeline.setFrame(frameIndex++, drawOrderMap.getFloat("time", 0f), drawOrder)
                drawOrderMap = drawOrderMap.next
            }
            timelines.add(timeline)
            duration = kotlin.math.max(duration, timeline.frames[timeline.frameCount - 1])
        }

        // Event timeline.
        val eventsMap = map["events"]
        if (eventsMap != null) {
            val timeline = EventTimeline(eventsMap.size)
            var frameIndex = 0
            var eventMap = eventsMap.child
            while (eventMap != null) {
                val eventData = skeletonData.findEvent(eventMap.getString("name"))
                        ?: error("Event not found: " + eventMap.getString("name")!!)
                val event = Event(eventMap.getFloat("time", 0f), eventData)
                event.int = eventMap.getInt("int", eventData.int)
                event.float = eventMap.getFloat("float", eventData.float)
                event.stringValue = eventMap.getString("string", eventData.stringValue)
                if (event.data.audioPath != null) {
                    event.volume = eventMap.getFloat("volume", eventData.volume)
                    event.balance = eventMap.getFloat("balance", eventData.balance)
                }
                timeline.setFrame(frameIndex++, event)
                eventMap = eventMap.next
            }
            timelines.add(timeline)
            duration = kotlin.math.max(duration, timeline.frames[timeline.frameCount - 1])
        }

        timelines.shrink()
        skeletonData.animations.add(Animation(name, timelines, duration))
    }

    internal fun readCurve(map: JsonValue, timeline: CurveTimeline, frameIndex: Int) {
        val curve = map["curve"] ?: return
        if (curve.isString)
            timeline.setStepped(frameIndex)
        else
            timeline.setCurve(frameIndex, curve.asFloat(), map.getFloat("c2", 0f), map.getFloat("c3", 1f), map.getFloat("c4", 1f))
    }

    internal class LinkedMesh(
        var mesh: MeshAttachment,
        var skin: String?,
        var slotIndex: Int,
        var parent: String?,
        var inheritDeform: Boolean
    )
}
