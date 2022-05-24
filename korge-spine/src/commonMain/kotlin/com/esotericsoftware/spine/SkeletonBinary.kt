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

import com.esotericsoftware.spine.Animation.*
import com.esotericsoftware.spine.BoneData.*
import com.esotericsoftware.spine.PathConstraintData.*
import com.esotericsoftware.spine.SkeletonJson.*
import com.esotericsoftware.spine.attachments.*
import com.esotericsoftware.spine.utils.*
import com.soywiz.kds.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import kotlin.math.*

/** Loads skeleton data in the Spine binary format.
 *
 *
 * See [Spine binary format](http://esotericsoftware.com/spine-binary-format) and
 * [JSON and binary data](http://esotericsoftware.com/spine-loading-skeleton-data#JSON-and-binary-data) in the Spine
 * Runtimes Guide.  */
class SkeletonBinary {

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
    //private val linkedMeshes = FastArrayList<LinkedMesh>()
    private val linkedMeshes by lazy { FastArrayList<LinkedMesh>() }

    constructor(atlas: Atlas) {
        attachmentLoader = AtlasAttachmentLoader(atlas)
    }

    constructor(attachmentLoader: AttachmentLoader) {
        this.attachmentLoader = attachmentLoader
    }

    suspend fun readSkeletonData(file: VfsFile): SkeletonData = readSkeletonData(file.readAll(), file.fullName)

    fun readSkeletonData(file: ByteArray, fileName: String = "unknown"): SkeletonData {
        val scale = this.scale

        val skeletonData = SkeletonData()
        skeletonData.name = PathInfo(fileName).baseNameWithoutExtension

        val input = SkeletonInput(file)
        try {
            skeletonData.hash = input.readString()
            if (skeletonData.hash!!.isEmpty()) skeletonData.hash = null
            skeletonData.version = input.readString()
            if (skeletonData.version!!.isEmpty()) skeletonData.version = null
            if ("3.8.75" == skeletonData.version)
                throw RuntimeException("Unsupported skeleton data, please export with a newer version of Spine.")
            skeletonData.x = input.readFloat()
            skeletonData.y = input.readFloat()
            skeletonData.width = input.readFloat()
            skeletonData.height = input.readFloat()

            val nonessential = input.readBoolean()
            if (nonessential) {
                skeletonData.fps = input.readFloat()

                skeletonData.imagesPath = input.readString()
                if (skeletonData.imagesPath!!.isEmpty()) skeletonData.imagesPath = null

                skeletonData.audioPath = input.readString()
                if (skeletonData.audioPath!!.isEmpty()) skeletonData.audioPath = null
            }

            var n: Int = 0

            // Strings.
            run {
                input.strings = FastArrayList<String>(input.readInt(true).also { n = it })
                val o = input.strings!!.setSize(n)
                for (i in 0 until n)
                    o.setAndGrow(i, input.readString()!!)
            }

            // Bones.
            run {
                val o = skeletonData.bones.setSize(input.readInt(true).also { n = it })
                for (i in 0 until n) {
                    val name = input.readString()!!
                    val parent = if (i == 0) null else skeletonData.bones[input.readInt(true)]
                    val data = BoneData(i, name, parent)
                    data.rotation = input.readFloat()
                    data.x = input.readFloat() * scale
                    data.y = input.readFloat() * scale
                    data.scaleX = input.readFloat()
                    data.scaleY = input.readFloat()
                    data.shearX = input.readFloat()
                    data.shearY = input.readFloat()
                    data.length = input.readFloat() * scale
                    data.transformMode = TransformMode.values[input.readInt(true)]
                    data.skinRequired = input.readBoolean()
                    if (nonessential) rgba8888ToColor(data.color, input.readInt())
                    o.setAndGrow(i, data)
                }
            }

            // Slots.
            run {
                val o = skeletonData.slots.setSize(input.readInt(true).also { n = it })
                for (i in 0 until n) {
                    val slotName = input.readString()!!
                    val boneData = skeletonData.bones[input.readInt(true)]
                    val data = SlotData(i, slotName, boneData)
                    rgba8888ToColor(data.color, input.readInt())

                    val darkColor = input.readInt()
                    if (darkColor != -1) rgb888ToColor(RGBAf().also { data.darkColor = it }, darkColor)

                    data.attachmentName = input.readStringRef()
                    data.blendMode = BlendMode.values[input.readInt(true)]
                    o.setAndGrow(i, data)
                }
            }

            // IK constraints.
            run {
                val o = skeletonData.ikConstraints.setSize(input.readInt(true).also { n = it })
                run {
                    var i = 0
                    var nn: Int
                    while (i < n) {
                        val data = IkConstraintData(input.readString()!!)
                        data.order = input.readInt(true)
                        data.skinRequired = input.readBoolean()
                        val bones = data.bones.setSize(input.readInt(true).also { nn = it })
                        for (ii in 0 until nn)
                            bones.setAndGrow(ii, skeletonData.bones[input.readInt(true)])
                        data.target = skeletonData.bones[input.readInt(true)]
                        data.mix = input.readFloat()
                        data.softness = input.readFloat() * scale
                        data.bendDirection = input.readByte().toInt()
                        data.compress = input.readBoolean()
                        data.stretch = input.readBoolean()
                        data.uniform = input.readBoolean()
                        o.setAndGrow(i, data)
                        i++
                    }
                }
            }

            // Transform constraints.
            run {
                val o = skeletonData.transformConstraints.setSize(input.readInt(true).also { n = it })
                run {
                    var i = 0
                    var nn: Int
                    while (i < n) {
                        val data = TransformConstraintData(input.readString()!!)
                        data.order = input.readInt(true)
                        data.skinRequired = input.readBoolean()
                        val bones = data.bones.setSize(input.readInt(true).also { nn = it })
                        for (ii in 0 until nn)
                            bones.setAndGrow(ii, skeletonData.bones[input.readInt(true)])
                        data.target = skeletonData.bones[input.readInt(true)]
                        data.local = input.readBoolean()
                        data.relative = input.readBoolean()
                        data.offsetRotation = input.readFloat()
                        data.offsetX = input.readFloat() * scale
                        data.offsetY = input.readFloat() * scale
                        data.offsetScaleX = input.readFloat()
                        data.offsetScaleY = input.readFloat()
                        data.offsetShearY = input.readFloat()
                        data.rotateMix = input.readFloat()
                        data.translateMix = input.readFloat()
                        data.scaleMix = input.readFloat()
                        data.shearMix = input.readFloat()
                        o.setAndGrow(i, data)
                        i++
                    }
                }
            }

            // Path constraints.
            run {
                val o = skeletonData.pathConstraints.setSize(input.readInt(true).also { n = it })
                run {
                    var i = 0
                    var nn: Int
                    while (i < n) {
                        val data = PathConstraintData(input.readString()!!)
                        data.order = input.readInt(true)
                        data.skinRequired = input.readBoolean()
                        val bones = data.bones.setSize(input.readInt(true).also { nn = it })
                        for (ii in 0 until nn)
                            bones.setAndGrow(ii, skeletonData.bones[input.readInt(true)])
                        data.target = skeletonData.slots[input.readInt(true)]
                        data.positionMode = PositionMode.values[input.readInt(true)]
                        data.spacingMode = SpacingMode.values[input.readInt(true)]
                        data.rotateMode = RotateMode.values[input.readInt(true)]
                        data.offsetRotation = input.readFloat()
                        data.position = input.readFloat()
                        if (data.positionMode == PositionMode.fixed) data.position *= scale
                        data.spacing = input.readFloat()
                        if (data.spacingMode == SpacingMode.length || data.spacingMode == SpacingMode.fixed) data.spacing *= scale
                        data.rotateMix = input.readFloat()
                        data.translateMix = input.readFloat()
                        o.setAndGrow(i, data)
                        i++
                    }
                }
            }

            // Default skin.
            val defaultSkin = readSkin(input, skeletonData, true, nonessential)
            if (defaultSkin != null) {
                skeletonData.defaultSkin = defaultSkin
                skeletonData.skins.add(defaultSkin)
            }

            // Skins.
            run {
                var i = skeletonData.skins.size
                val o = skeletonData.skins.setSize((i + input.readInt(true)).also { n = it })
                while (i < n) {
                    o.setAndGrow(i, readSkin(input, skeletonData, false, nonessential)!!)
                    i++
                }
            }

            // Linked meshes.
            n = linkedMeshes.size
            for (i in 0 until n) {
                val linkedMesh = linkedMeshes.get(i)
                val skin = (if (linkedMesh.skin == null) skeletonData.defaultSkin else skeletonData.findSkin(linkedMesh.skin))
                        ?: error("Skin not found: " + linkedMesh.skin!!)
                val parent = skin.getAttachment(linkedMesh.slotIndex, linkedMesh.parent!!)
                        ?: error("Parent mesh not found: " + linkedMesh.parent)
                linkedMesh.mesh.deformAttachment = if (linkedMesh.inheritDeform) parent as VertexAttachment else linkedMesh.mesh
                linkedMesh.mesh.parentMesh = parent as MeshAttachment
                linkedMesh.mesh.updateUVs()
            }
            linkedMeshes.clear()

            // Events.
            run {
                val o = skeletonData.events.setSize(input.readInt(true).also { n = it })
                for (i in 0 until n) {
                    val data = EventData(input.readStringRef()!!)
                    data.int = input.readInt(false)
                    data.float = input.readFloat()
                    data.stringValue = input.readString()!!
                    data.audioPath = input.readString()
                    if (data.audioPath != null) {
                        data.volume = input.readFloat()
                        data.balance = input.readFloat()
                    }
                    o.setAndGrow(i, data)
                }
            }

            // Animations.
            run {
                val o = skeletonData.animations.setSize(input.readInt(true).also { n = it })
                for (i in 0 until n)
                    o.setAndGrow(i, readAnimation(input, input.readString(), skeletonData))
            }

        } catch (ex: Throwable) {
            throw RuntimeException("Error reading skeleton file.", ex)
        } finally {
            try {
                input.close()
            } catch (ignored: Throwable) {
            }

        }
        return skeletonData
    }

    private fun rgb888ToColor(color: RGBAf, value: Int) {
        color.r = (value and 0x00ff0000 ushr 16) / 255f
        color.g = (value and 0x0000ff00 ushr 8) / 255f
        color.b = (value and 0x000000ff) / 255f
    }

    private fun rgba8888ToColor(color: RGBAf, value: Int) {
        color.r = (value and -0x1000000 ushr 24) / 255f
        color.g = (value and 0x00ff0000 ushr 16) / 255f
        color.b = (value and 0x0000ff00 ushr 8) / 255f
        color.a = (value and 0x000000ff) / 255f
    }

    /** @return May be null.
     */
    
    private fun readSkin(input: SkeletonInput, skeletonData: SkeletonData, defaultSkin: Boolean, nonessential: Boolean): Skin? {

        val skin: Skin
        val slotCount: Int
        if (defaultSkin) {
            slotCount = input.readInt(true)
            if (slotCount == 0) return null
            skin = Skin("default")
        } else {
            skin = Skin(input.readStringRef()!!)
            val bones = skin.bones.setSize(input.readInt(true))
            run {
                var i = 0
                val n = skin.bones.size
                while (i < n) {
                    bones.setAndGrow(i, skeletonData.bones[input.readInt(true)])
                    i++
                }
            }

            run {
                var i = 0
                val n = input.readInt(true)
                while (i < n) {
                    skin.constraints.add(skeletonData.ikConstraints[input.readInt(true)])
                    i++
                }
            }
            run {
                var i = 0
                val n = input.readInt(true)
                while (i < n) {
                    skin.constraints.add(skeletonData.transformConstraints[input.readInt(true)])
                    i++
                }
            }
            var i = 0
            val n = input.readInt(true)
            while (i < n) {
                skin.constraints.add(skeletonData.pathConstraints[input.readInt(true)])
                i++
            }
            skin.constraints.shrink()

            slotCount = input.readInt(true)
        }

        for (i in 0 until slotCount) {
            val slotIndex = input.readInt(true)
            var ii = 0
            val nn = input.readInt(true)
            while (ii < nn) {
                val name = input.readStringRef()!!
                val attachment = readAttachment(input, skeletonData, skin, slotIndex, name, nonessential)
                if (attachment != null) skin.setAttachment(slotIndex, name, attachment)
                ii++
            }
        }
        return skin
    }

    
    private fun readAttachment(input: SkeletonInput, skeletonData: SkeletonData, skin: Skin, slotIndex: Int,
                               attachmentName: String?, nonessential: Boolean): Attachment? {
        val scale = this.scale

        var name = input.readStringRef()
        if (name == null) name = attachmentName

        val type = AttachmentType.values[input.readByte().toInt()]
        when (type) {
            AttachmentType.region -> {
                var path = input.readStringRef()
                val rotation = input.readFloat()
                val x = input.readFloat()
                val y = input.readFloat()
                val scaleX = input.readFloat()
                val scaleY = input.readFloat()
                val width = input.readFloat()
                val height = input.readFloat()
                val color = input.readInt()

                if (path == null) path = name
                val region = attachmentLoader.newRegionAttachment(skin, name!!, path!!) ?: return null
                region.path = path
                region.x = x * scale
                region.y = y * scale
                region.scaleX = scaleX
                region.scaleY = scaleY
                region.rotation = rotation
                region.width = width * scale
                region.height = height * scale
                rgba8888ToColor(region.color, color)
                region.updateOffset()
                return region
            }
            AttachmentType.boundingbox -> {
                val vertexCount = input.readInt(true)
                val vertices = readVertices(input, vertexCount)
                val color = if (nonessential) input.readInt() else 0

                val box = attachmentLoader.newBoundingBoxAttachment(skin, name!!) ?: return null
                box.worldVerticesLength = vertexCount shl 1
                box.vertices = vertices.vertices
                box.bones = vertices.bones
                if (nonessential) rgba8888ToColor(box.color, color)
                return box
            }
            AttachmentType.mesh -> {
                var path = input.readStringRef()
                val color = input.readInt()
                val vertexCount = input.readInt(true)
                val uvs = readFloatArray(input, vertexCount shl 1, 1f)
                val triangles = readShortArray(input)
                val vertices = readVertices(input, vertexCount)
                val hullLength = input.readInt(true)
                var edges: ShortArray? = null
                var width = 0f
                var height = 0f
                if (nonessential) {
                    edges = readShortArray(input)
                    width = input.readFloat()
                    height = input.readFloat()
                }

                if (path == null) path = name
                val mesh = attachmentLoader.newMeshAttachment(skin, name!!, path!!) ?: return null
                mesh.path = path
                rgba8888ToColor(mesh.color, color)
                mesh.bones = vertices.bones
                mesh.vertices = vertices.vertices
                mesh.worldVerticesLength = vertexCount shl 1
                mesh.triangles = triangles
                mesh.regionUVs = uvs
                mesh.updateUVs()
                mesh.hullLength = hullLength shl 1
                if (nonessential) {
                    mesh.edges = edges
                    mesh.width = width * scale
                    mesh.height = height * scale
                }
                return mesh
            }
            AttachmentType.linkedmesh -> {
                var path = input.readStringRef()
                val color = input.readInt()
                val skinName = input.readStringRef()
                val parent = input.readStringRef()
                val inheritDeform = input.readBoolean()
                var width = 0f
                var height = 0f
                if (nonessential) {
                    width = input.readFloat()
                    height = input.readFloat()
                }

                if (path == null) path = name
                val mesh = attachmentLoader.newMeshAttachment(skin, name!!, path!!) ?: return null
                mesh.path = path
                rgba8888ToColor(mesh.color, color)
                if (nonessential) {
                    mesh.width = width * scale
                    mesh.height = height * scale
                }
                linkedMeshes.add(LinkedMesh(mesh, skinName, slotIndex, parent, inheritDeform))
                return mesh
            }
            AttachmentType.path -> {
                val closed = input.readBoolean()
                val constantSpeed = input.readBoolean()
                val vertexCount = input.readInt(true)
                val vertices = readVertices(input, vertexCount)
                val lengths = FloatArray(vertexCount / 3)
                var i = 0
                val n = lengths.size
                while (i < n) {
                    lengths[i] = input.readFloat() * scale
                    i++
                }
                val color = if (nonessential) input.readInt() else 0

                val path = attachmentLoader.newPathAttachment(skin, name!!) ?: return null
                path.closed = closed
                path.constantSpeed = constantSpeed
                path.worldVerticesLength = vertexCount shl 1
                path.vertices = vertices.vertices
                path.bones = vertices.bones
                path.lengths = lengths
                if (nonessential) rgba8888ToColor(path.color, color)
                return path
            }
            AttachmentType.point -> {
                val rotation = input.readFloat()
                val x = input.readFloat()
                val y = input.readFloat()
                val color = if (nonessential) input.readInt() else 0

                val point = attachmentLoader.newPointAttachment(skin, name!!) ?: return null
                point.x = x * scale
                point.y = y * scale
                point.rotation = rotation
                if (nonessential) rgba8888ToColor(point.color, color)
                return point
            }
            AttachmentType.clipping -> {
                val endSlotIndex = input.readInt(true)
                val vertexCount = input.readInt(true)
                val vertices = readVertices(input, vertexCount)
                val color = if (nonessential) input.readInt() else 0

                val clip = attachmentLoader.newClippingAttachment(skin, name!!) ?: return null
                clip.endSlot = skeletonData.slots[endSlotIndex]
                clip.worldVerticesLength = vertexCount shl 1
                clip.vertices = vertices.vertices
                clip.bones = vertices.bones
                if (nonessential) rgba8888ToColor(clip.color, color)
                return clip
            }
        }
        return null
    }

    
    private fun readVertices(input: SkeletonInput, vertexCount: Int): Vertices {
        val verticesLength = vertexCount shl 1
        val vertices = Vertices()
        if (!input.readBoolean()) {
            vertices.vertices = readFloatArray(input, verticesLength, this.scale)
            return vertices
        }
        val weights = FloatArrayList(verticesLength * 3 * 3)
        val bonesArray = IntArrayList(verticesLength * 3)
        for (i in 0 until vertexCount) {
            val boneCount = input.readInt(true)
            bonesArray.add(boneCount)
            for (ii in 0 until boneCount) {
                bonesArray.add(input.readInt(true))
                weights.add(input.readFloat() * this.scale)
                weights.add(input.readFloat() * this.scale)
                weights.add(input.readFloat())
            }
        }
        vertices.vertices = weights.toArray()
        vertices.bones = bonesArray.toArray()
        return vertices
    }

    private fun readFloatArray(input: SkeletonInput, n: Int, scale: Float): FloatArray {
        val array = FloatArray(n)
        if (scale == 1f) {
            for (i in 0 until n)
                array[i] = input.readFloat()
        } else {
            for (i in 0 until n)
                array[i] = input.readFloat() * scale
        }
        return array
    }

    private fun readShortArray(input: SkeletonInput): ShortArray {
        val n = input.readInt(true)
        val array = ShortArray(n)
        for (i in 0 until n)
            array[i] = input.readShort()
        return array
    }

    private fun readAnimation(input: SkeletonInput, name: String?, skeletonData: SkeletonData): Animation {
        val timelines = FastArrayList<Timeline>(32)
        val scale = this.scale
        var duration = 0f

        try {
            // Slot timelines.
            run {
                var i = 0
                val n = input.readInt(true)
                while (i < n) {
                    val slotIndex = input.readInt(true)
                    var ii = 0
                    val nn = input.readInt(true)
                    while (ii < nn) {
                        val timelineType = input.readByte().toInt()
                        val frameCount = input.readInt(true)
                        when (timelineType) {
                            SLOT_ATTACHMENT -> {
                                val timeline = AttachmentTimeline(frameCount)
                                timeline.slotIndex = slotIndex
                                for (frameIndex in 0 until frameCount)
                                    timeline.setFrame(frameIndex, input.readFloat(), input.readStringRef())
                                timelines.add(timeline)
                                duration = max(duration, timeline.frames[frameCount - 1])
                            }
                            SLOT_COLOR -> {
                                val timeline = ColorTimeline(frameCount)
                                timeline.slotIndex = slotIndex
                                for (frameIndex in 0 until frameCount) {
                                    val time = input.readFloat()
                                    rgba8888ToColor(tempColor1, input.readInt())
                                    timeline.setFrame(frameIndex, time, tempColor1.r, tempColor1.g, tempColor1.b, tempColor1.a)
                                    if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline)
                                }
                                timelines.add(timeline)
                                duration = max(duration, timeline.frames[(frameCount - 1) * ColorTimeline.ENTRIES])
                            }
                            SLOT_TWO_COLOR -> {
                                val timeline = TwoColorTimeline(frameCount)
                                timeline.slotIndex = slotIndex
                                for (frameIndex in 0 until frameCount) {
                                    val time = input.readFloat()
                                    rgba8888ToColor(tempColor1, input.readInt())
                                    rgb888ToColor(tempColor2, input.readInt())
                                    timeline.setFrame(frameIndex, time, tempColor1.r, tempColor1.g, tempColor1.b, tempColor1.a, tempColor2.r,
                                            tempColor2.g, tempColor2.b)
                                    if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline)
                                }
                                timelines.add(timeline)
                                duration = max(duration, timeline.frames[(frameCount - 1) * TwoColorTimeline.ENTRIES])
                            }
                        }
                        ii++
                    }
                    i++
                }
            }

            // Bone timelines.
            run {
                var i = 0
                val n = input.readInt(true)
                while (i < n) {
                    val boneIndex = input.readInt(true)
                    var ii = 0
                    val nn = input.readInt(true)
                    while (ii < nn) {
                        val timelineType = input.readByte().toInt()
                        val frameCount = input.readInt(true)
                        when (timelineType) {
                            BONE_ROTATE -> {
                                val timeline = RotateTimeline(frameCount)
                                timeline.boneIndex = boneIndex
                                for (frameIndex in 0 until frameCount) {
                                    timeline.setFrame(frameIndex, input.readFloat(), input.readFloat())
                                    if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline)
                                }
                                timelines.add(timeline)
                                duration = max(duration, timeline.frames[(frameCount - 1) * RotateTimeline.ENTRIES])
                            }
                            BONE_TRANSLATE, BONE_SCALE, BONE_SHEAR -> {
                                val timeline: TranslateTimeline
                                var timelineScale = 1f
                                if (timelineType == BONE_SCALE)
                                    timeline = ScaleTimeline(frameCount)
                                else if (timelineType == BONE_SHEAR)
                                    timeline = ShearTimeline(frameCount)
                                else {
                                    timeline = TranslateTimeline(frameCount)
                                    timelineScale = scale
                                }
                                timeline.boneIndex = boneIndex
                                for (frameIndex in 0 until frameCount) {
                                    timeline.setFrame(frameIndex, input.readFloat(), input.readFloat() * timelineScale,
                                            input.readFloat() * timelineScale)
                                    if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline)
                                }
                                timelines.add(timeline)
                                duration = max(duration, timeline.frames[(frameCount - 1) * TranslateTimeline.ENTRIES])
                            }
                        }
                        ii++
                    }
                    i++
                }
            }

            // IK constraint timelines.
            run {
                var i = 0
                val n = input.readInt(true)
                while (i < n) {
                    val index = input.readInt(true)
                    val frameCount = input.readInt(true)
                    val timeline = IkConstraintTimeline(frameCount)
                    timeline.ikConstraintIndex = index
                    for (frameIndex in 0 until frameCount) {
                        timeline.setFrame(frameIndex, input.readFloat(), input.readFloat(), input.readFloat() * scale, input.readByte().toInt(),
                                input.readBoolean(), input.readBoolean())
                        if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline)
                    }
                    timelines.add(timeline)
                    duration = max(duration, timeline.frames[(frameCount - 1) * IkConstraintTimeline.ENTRIES])
                    i++
                }
            }

            // Transform constraint timelines.
            run {
                var i = 0
                val n = input.readInt(true)
                while (i < n) {
                    val index = input.readInt(true)
                    val frameCount = input.readInt(true)
                    val timeline = TransformConstraintTimeline(frameCount)
                    timeline.transformConstraintIndex = index
                    for (frameIndex in 0 until frameCount) {
                        timeline.setFrame(frameIndex, input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat(),
                                input.readFloat())
                        if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline)
                    }
                    timelines.add(timeline)
                    duration = max(duration, timeline.frames[(frameCount - 1) * TransformConstraintTimeline.ENTRIES])
                    i++
                }
            }

            // Path constraint timelines.
            run {
                var i = 0
                val n = input.readInt(true)
                while (i < n) {
                    val index = input.readInt(true)
                    val data = skeletonData.pathConstraints[index]
                    var ii = 0
                    val nn = input.readInt(true)
                    while (ii < nn) {
                        val timelineType = input.readByte().toInt()
                        val frameCount = input.readInt(true)
                        when (timelineType) {
                            PATH_POSITION, PATH_SPACING -> {
                                val timeline: PathConstraintPositionTimeline
                                var timelineScale = 1f
                                if (timelineType == PATH_SPACING) {
                                    timeline = PathConstraintSpacingTimeline(frameCount)
                                    if (data.spacingMode == SpacingMode.length || data.spacingMode == SpacingMode.fixed) timelineScale = scale
                                } else {
                                    timeline = PathConstraintPositionTimeline(frameCount)
                                    if (data.positionMode == PositionMode.fixed) timelineScale = scale
                                }
                                timeline.pathConstraintIndex = index
                                for (frameIndex in 0 until frameCount) {
                                    timeline.setFrame(frameIndex, input.readFloat(), input.readFloat() * timelineScale)
                                    if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline)
                                }
                                timelines.add(timeline)
                                duration = max(duration, timeline.frames[(frameCount - 1) * PathConstraintPositionTimeline.ENTRIES])
                            }
                            PATH_MIX -> {
                                val timeline = PathConstraintMixTimeline(frameCount)
                                timeline.pathConstraintIndex = index
                                for (frameIndex in 0 until frameCount) {
                                    timeline.setFrame(frameIndex, input.readFloat(), input.readFloat(), input.readFloat())
                                    if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline)
                                }
                                timelines.add(timeline)
                                duration = max(duration, timeline.frames[(frameCount - 1) * PathConstraintMixTimeline.ENTRIES])
                            }
                        }
                        ii++
                    }
                    i++
                }
            }

            // Deform timelines.
            run {
                var i = 0
                val n = input.readInt(true)
                while (i < n) {
                    val skin = skeletonData.skins[input.readInt(true)]
                    var ii = 0
                    val nn = input.readInt(true)
                    while (ii < nn) {
                        val slotIndex = input.readInt(true)
                        var iii = 0
                        val nnn = input.readInt(true)
                        while (iii < nnn) {
                            val attachment = skin.getAttachment(slotIndex, input.readStringRef()!!) as VertexAttachment
                            val weighted = attachment.bones != null
                            val vertices = attachment.vertices
                            val deformLength = if (weighted) vertices!!.size / 3 * 2 else vertices!!.size

                            val frameCount = input.readInt(true)
                            val timeline = DeformTimeline(frameCount)
                            timeline.slotIndex = slotIndex
                            timeline.attachment = attachment

                            for (frameIndex in 0 until frameCount) {
                                val time = input.readFloat()
                                val deform: FloatArray
                                var end = input.readInt(true)
                                if (end == 0)
                                    deform = if (weighted) FloatArray(deformLength) else vertices
                                else {
                                    deform = FloatArray(deformLength)
                                    val start = input.readInt(true)
                                    end += start
                                    if (scale == 1f) {
                                        for (v in start until end)
                                            deform[v] = input.readFloat()
                                    } else {
                                        for (v in start until end)
                                            deform[v] = input.readFloat() * scale
                                    }
                                    if (!weighted) {
                                        var v = 0
                                        val vn = deform.size
                                        while (v < vn) {
                                            deform[v] += vertices[v]
                                            v++
                                        }
                                    }
                                }

                                timeline.setFrame(frameIndex, time, deform)
                                if (frameIndex < frameCount - 1) readCurve(input, frameIndex, timeline)
                            }
                            timelines.add(timeline)
                            duration = max(duration, timeline.frames[frameCount - 1])
                            iii++
                        }
                        ii++
                    }
                    i++
                }
            }

            // Draw order timeline.
            val drawOrderCount = input.readInt(true)
            if (drawOrderCount > 0) {
                val timeline = DrawOrderTimeline(drawOrderCount)
                val slotCount = skeletonData.slots.size
                for (i in 0 until drawOrderCount) {
                    val time = input.readFloat()
                    val offsetCount = input.readInt(true)
                    val drawOrder = IntArray(slotCount)
                    for (ii in slotCount - 1 downTo 0)
                        drawOrder[ii] = -1
                    val unchanged = IntArray(slotCount - offsetCount)
                    var originalIndex = 0
                    var unchangedIndex = 0
                    for (ii in 0 until offsetCount) {
                        val slotIndex = input.readInt(true)
                        // Collect unchanged items.
                        while (originalIndex != slotIndex)
                            unchanged[unchangedIndex++] = originalIndex++
                        // Set changed items.
                        drawOrder[originalIndex + input.readInt(true)] = originalIndex++
                    }
                    // Collect remaining unchanged items.
                    while (originalIndex < slotCount)
                        unchanged[unchangedIndex++] = originalIndex++
                    // Fill in unchanged items.
                    for (ii in slotCount - 1 downTo 0)
                        if (drawOrder[ii] == -1) drawOrder[ii] = unchanged[--unchangedIndex]
                    timeline.setFrame(i, time, drawOrder)
                }
                timelines.add(timeline)
                duration = max(duration, timeline.frames[drawOrderCount - 1])
            }

            // Event timeline.
            val eventCount = input.readInt(true)
            if (eventCount > 0) {
                val timeline = EventTimeline(eventCount)
                for (i in 0 until eventCount) {
                    val time = input.readFloat()
                    val eventData = skeletonData.events[input.readInt(true)]
                    val event = Event(time, eventData)
                    event.int = input.readInt(false)
                    event.float = input.readFloat()
                    event.stringValue = if (input.readBoolean()) input.readString()!! else eventData.stringValue
                    if (event.data.audioPath != null) {
                        event.volume = input.readFloat()
                        event.balance = input.readFloat()
                    }
                    timeline.setFrame(i, event)
                }
                timelines.add(timeline)
                duration = max(duration, timeline.frames[eventCount - 1])
            }
        } catch (ex: Throwable) {
            throw RuntimeException("Error reading skeleton file.", ex)
        }

        timelines.shrink()
        return Animation(name!!, timelines, duration)
    }

    
    private fun readCurve(input: SkeletonInput, frameIndex: Int, timeline: CurveTimeline) {
        when (input.readByte().toInt()) {
            CURVE_STEPPED -> timeline.setStepped(frameIndex)
            CURVE_BEZIER -> setCurve(timeline, frameIndex, input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat())
        }
    }

    internal fun setCurve(timeline: CurveTimeline, frameIndex: Int, cx1: Float, cy1: Float, cx2: Float, cy2: Float) {
        timeline.setCurve(frameIndex, cx1, cy1, cx2, cy2)
    }

    internal class Vertices {
        var bones: IntArray? = null
        var vertices: FloatArray? = null
    }

    internal class SkeletonInput(val data: ByteArray) {
        private var n = 0

        fun read(): Int {
            if (n >= data.size) {
                return -1
            }
            return data[n++].toInt() and 0xFF
        }

        fun close() {
        }

        fun readFloat(): Float {
            return Float.fromBits(readInt())
        }

        fun readBoolean(): Boolean {
            return readUnsignedByte() != 0
        }

        fun readInt(optimizePositive: Boolean): Int {
            var b = read()
            var result = b and 0x7F
            if (b and 0x80 != 0) {
                b = read()
                result = result or (b and 0x7F shl 7)
                if (b and 0x80 != 0) {
                    b = read()
                    result = result or (b and 0x7F shl 14)
                    if (b and 0x80 != 0) {
                        b = read()
                        result = result or (b and 0x7F shl 21)
                        if (b and 0x80 != 0) {
                            b = read()
                            result = result or (b and 0x7F shl 28)
                        }
                    }
                }
            }
            return if (optimizePositive) result else result.ushr(1) xor -(result and 1)
        }
        fun readInt(): Int {
            val a = readUnsignedByte()
            val b = readUnsignedByte()
            val c = readUnsignedByte()
            val d = readUnsignedByte()
            return a shl 24 or (b shl 16) or (c shl 8) or d
        }

        fun readUnsignedByte(): Int {
            val i = read()
            if (i == -1) {
                error("EOF")
            }
            return i
        }

        fun readByte(): Byte {
            return readUnsignedByte().toByte()
        }

        fun readShort(): Short {
            val a = readUnsignedByte()
            val b = readUnsignedByte()
            return (a shl 8 or b).toShort()
        }

        private var chars = CharArray(32)
        var strings: FastArrayList<String>? = null

        /** @return May be null.
         */
        
        fun readStringRef(): String? {
            val index = readInt(true)
            return if (index == 0) null else strings!![index - 1]
        }

        
        fun readString(): String? {
            var byteCount = readInt(true)
            when (byteCount) {
                0 -> return null
                1 -> return ""
            }
            byteCount--
            if (chars.size < byteCount) chars = CharArray(byteCount)
            val chars = this.chars
            var charCount = 0
            var i = 0
            while (i < byteCount) {
                val b = read()
                when (b shr 4) {
                    -1 -> error("EOF")
                    12, 13 -> {
                        chars[charCount++] = (b and 0x1F shl 6 or (read() and 0x3F)).toChar()
                        i += 2
                    }
                    14 -> {
                        chars[charCount++] = (b and 0x0F shl 12 or (read() and 0x3F shl 6) or (read() and 0x3F)).toChar()
                        i += 3
                    }
                    else -> {
                        chars[charCount++] = b.toChar()
                        i++
                    }
                }
            }
            return chars.concatToString(0, charCount)
        }
    }

    private val tempColor1 = RGBAf()
    private val tempColor2 = RGBAf()

    companion object {
        val BONE_ROTATE = 0
        val BONE_TRANSLATE = 1
        val BONE_SCALE = 2
        val BONE_SHEAR = 3

        val SLOT_ATTACHMENT = 0
        val SLOT_COLOR = 1
        val SLOT_TWO_COLOR = 2

        val PATH_POSITION = 0
        val PATH_SPACING = 1
        val PATH_MIX = 2

        val CURVE_LINEAR = 0
        val CURVE_STEPPED = 1
        val CURVE_BEZIER = 2
    }
}

suspend fun VfsFile.readSkeletonBinary(atlas: Atlas, scale: Float = 1f): SkeletonData
    = SkeletonBinary(atlas).also { it.scale = scale }.readSkeletonData(this)
