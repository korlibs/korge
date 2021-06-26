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

import com.soywiz.korim.color.RGBAf

import com.esotericsoftware.spine.attachments.Attachment
import com.esotericsoftware.spine.attachments.MeshAttachment
import com.esotericsoftware.spine.attachments.PathAttachment
import com.esotericsoftware.spine.attachments.RegionAttachment
import com.esotericsoftware.spine.utils.*
import com.esotericsoftware.spine.utils.SpineUtils.arraycopy
import com.esotericsoftware.spine.utils.SpineUtils.cosDeg
import com.esotericsoftware.spine.utils.SpineUtils.sinDeg
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import kotlin.js.*
import kotlin.math.*

/** Stores the current pose for a skeleton.
 *
 *
 * See [Instance objects](http://esotericsoftware.com/spine-runtime-architecture#Instance-objects) in the Spine
 * Runtimes Guide.  */
class Skeleton {
    /** The skeleton's setup pose data.  */
    val data: SkeletonData

    /** The skeleton's bones, sorted parent first. The root bone is always the first bone.  */
    val bones: FastArrayList<Bone>

    /** The skeleton's slots.  */
    val slots: FastArrayList<Slot>
    internal var drawOrder: FastArrayList<Slot>

    /** The skeleton's IK constraints.  */
    val ikConstraints: FastArrayList<IkConstraint>

    /** The skeleton's transform constraints.  */
    val transformConstraints: FastArrayList<TransformConstraint>

    /** The skeleton's path constraints.  */
    val pathConstraints: FastArrayList<PathConstraint>

    /** The list of bones and constraints, sorted in the order they should be updated, as computed by [.updateCache].  */
    @JsName("updateCacheProp")
    val updateCache: FastArrayList<Updatable> = FastArrayList()
    internal val updateCacheReset: FastArrayList<Bone> = FastArrayList()
    internal var skin: Skin? = null
    internal var color: RGBAf

    /** Returns the skeleton's time. This can be used for tracking, such as with Slot [Slot.getAttachmentTime].
     *
     *
     * See [.update].  */
    var time: Float = 0.toFloat()

    /** Scales the entire skeleton on the X axis. This affects all bones, even if the bone's transform mode disallows scale
     * inheritance.  */
    var scaleX = 1f

    /** Scales the entire skeleton on the Y axis. This affects all bones, even if the bone's transform mode disallows scale
     * inheritance.  */
    var scaleY = 1f

    /** Sets the skeleton X position, which is added to the root bone worldX position.  */
    var x: Float = 0.toFloat()

    /** Sets the skeleton Y position, which is added to the root bone worldY position.  */
    var y: Float = 0.toFloat()

    /** Returns the root bone, or null.  */
    val rootBone: Bone?
        get() = if (bones.size == 0) null else bones.first()

    constructor(data: SkeletonData) {
        this.data = data

        bones = FastArrayList(data.bones.size)
        data.bones.fastForEach { boneData ->
            val bone: Bone
            if (boneData.parent == null)
                bone = Bone(boneData, this, null)
            else {
                val parent = bones[boneData.parent.index]
                bone = Bone(boneData, this, parent)
                parent.children.add(bone)
            }
            bones.add(bone)
        }

        slots = FastArrayList(data.slots.size)
        drawOrder = FastArrayList(data.slots.size)
        data.slots.fastForEach { slotData ->
            val bone = bones[slotData.boneData.index]
            val slot = Slot(slotData, bone)
            slots.add(slot)
            drawOrder.add(slot)
        }

        ikConstraints = FastArrayList(data.ikConstraints.size)
        data.ikConstraints.fastForEach { ikConstraintData ->
            ikConstraints.add(IkConstraint(ikConstraintData, this))
        }

        transformConstraints = FastArrayList(data.transformConstraints.size)
        data.transformConstraints.fastForEach { transformConstraintData ->
            transformConstraints.add(TransformConstraint(transformConstraintData, this))
        }

        pathConstraints = FastArrayList(data.pathConstraints.size)
        data.pathConstraints.fastForEach { pathConstraintData ->
            pathConstraints.add(PathConstraint(pathConstraintData, this))
        }

        color = RGBAf(1f, 1f, 1f, 1f)

        updateCache()
    }

    /** Copy constructor.  */
    constructor(skeleton: Skeleton) {
        data = skeleton.data

        bones = FastArrayList(skeleton.bones.size)
        skeleton.bones.fastForEach { bone ->
            val newBone: Bone
            if (bone.parent == null)
                newBone = Bone(bone, this, null)
            else {
                val parent = bones[bone.parent.data.index]
                newBone = Bone(bone, this, parent)
                parent.children.add(newBone)
            }
            bones.add(newBone)
        }

        slots = FastArrayList(skeleton.slots.size)
        skeleton.slots.fastForEach { slot ->
            val bone = bones[slot.bone.data.index]
            slots.add(Slot(slot, bone))
        }

        drawOrder = FastArrayList(slots.size)
        skeleton.drawOrder.fastForEach { slot ->
            drawOrder.add(slots[slot.data.index])
        }

        ikConstraints = FastArrayList(skeleton.ikConstraints.size)
        skeleton.ikConstraints.fastForEach { ikConstraint ->
            ikConstraints.add(IkConstraint(ikConstraint, this))
        }

        transformConstraints = FastArrayList(skeleton.transformConstraints.size)
        skeleton.transformConstraints.fastForEach { transformConstraint ->
            transformConstraints.add(TransformConstraint(transformConstraint, this))
        }

        pathConstraints = FastArrayList(skeleton.pathConstraints.size)
        skeleton.pathConstraints.fastForEach { pathConstraint ->
            pathConstraints.add(PathConstraint(pathConstraint, this))
        }

        skin = skeleton.skin
        color = RGBAf(skeleton.color)
        time = skeleton.time
        scaleX = skeleton.scaleX
        scaleY = skeleton.scaleY

        updateCache()
    }

    /** Caches information about bones and constraints. Must be called if the [.getSkin] is modified or if bones,
     * constraints, or weighted path attachments are added or removed.  */
    fun updateCache() {
        val updateCache = this.updateCache
        updateCache.clear()
        updateCacheReset.clear()

        val boneCount = bones.size
        val bones = this.bones
        for (i in 0 until boneCount) {
            val bone = bones[i]
            bone.sorted = bone.data.skinRequired
            bone.isActive = !bone.sorted
        }
        if (skin != null) {
            val skinBones = skin!!.bones
            var i = 0
            val n = skin!!.bones.size
            while (i < n) {
                var bone: Bone? = bones[(skinBones[i] as BoneData).index]
                do {
                    bone!!.sorted = false
                    bone.isActive = true
                    bone = bone.parent
                } while (bone != null)
                i++
            }
        }

        val ikCount = ikConstraints.size
        val transformCount = transformConstraints.size
        val pathCount = pathConstraints.size
        val ikConstraints = this.ikConstraints
        val transformConstraints = this.transformConstraints
        val pathConstraints = this.pathConstraints
        val constraintCount = ikCount + transformCount + pathCount
        outer@ for (i in 0 until constraintCount) {
            for (ii in 0 until ikCount) {
                val constraint = ikConstraints[ii]
                if (constraint.data.order == i) {
                    sortIkConstraint(constraint)
                    continue@outer
                }
            }
            for (ii in 0 until transformCount) {
                val constraint = transformConstraints[ii]
                if (constraint.data.order == i) {
                    sortTransformConstraint(constraint)
                    continue@outer
                }
            }
            for (ii in 0 until pathCount) {
                val constraint = pathConstraints[ii]
                if (constraint.data.order == i) {
                    sortPathConstraint(constraint)
                    continue@outer
                }
            }
        }

        for (i in 0 until boneCount)
            sortBone(bones[i])
    }

    private fun sortIkConstraint(constraint: IkConstraint) {
        constraint.active = constraint.target!!.isActive && (!constraint.data.skinRequired || skin != null && skin!!.constraints.containsIdentity(constraint.data))
        if (!constraint.active) return

        val target = constraint.target
        sortBone(target!!)

        val constrained = constraint.bones
        val parent = constrained.first()
        sortBone(parent)

        if (constrained.size > 1) {
            val child = constrained.last()
            if (!updateCache.containsIdentity(child)) updateCacheReset.add(child)
        }

        updateCache.add(constraint)

        sortReset(parent.children)
        constrained.last().sorted = true
    }

    private fun sortPathConstraint(constraint: PathConstraint) {
        constraint.isActive = constraint.target!!.bone.isActive && (!constraint.data.skinRequired || skin != null && skin!!.constraints.containsIdentity(constraint.data))
        if (!constraint.isActive) return

        val slot = constraint.target
        val slotIndex = slot!!.data.index
        val slotBone = slot.bone
        if (skin != null) sortPathConstraintAttachment(skin!!, slotIndex, slotBone)
        if (data.defaultSkin != null && data.defaultSkin !== skin)
            sortPathConstraintAttachment(data.defaultSkin, slotIndex, slotBone)

        val attachment = slot.attachment
        if (attachment is PathAttachment) sortPathConstraintAttachment(attachment, slotBone)

        val constrained = constraint.bones
        val boneCount = constrained.size
        for (i in 0 until boneCount)
            sortBone(constrained[i])

        updateCache.add(constraint)

        for (i in 0 until boneCount)
            sortReset(constrained[i].children)
        for (i in 0 until boneCount)
            constrained[i].sorted = true
    }

    private fun sortTransformConstraint(constraint: TransformConstraint) {
        constraint.isActive = constraint.target!!.isActive && (!constraint.data.skinRequired || skin != null && skin!!.constraints.containsIdentity(constraint.data))
        if (!constraint.isActive) return

        sortBone(constraint.target!!)

        val constrained = constraint.bones
        val boneCount = constrained.size
        if (constraint.data.local) {
            for (i in 0 until boneCount) {
                val child = constrained[i]
                sortBone(child.parent!!)
                if (!updateCache.containsIdentity(child)) updateCacheReset.add(child)
            }
        } else {
            for (i in 0 until boneCount)
                sortBone(constrained[i])
        }

        updateCache.add(constraint)

        for (i in 0 until boneCount)
            sortReset(constrained[i].children)
        for (i in 0 until boneCount)
            constrained[i].sorted = true
    }

    private fun sortPathConstraintAttachment(skin: Skin, slotIndex: Int, slotBone: Bone) {
        for (entry in skin.attachments.keys)
            if (entry.slotIndex == slotIndex) sortPathConstraintAttachment(entry.attachment, slotBone)
    }

    private fun sortPathConstraintAttachment(attachment: Attachment?, slotBone: Bone) {
        if (attachment !is PathAttachment) return
        val pathBones = attachment.bones
        if (pathBones == null)
            sortBone(slotBone)
        else {
            val bones = this.bones
            var i = 0
            val n = pathBones.size
            while (i < n) {
                var nn = pathBones[i++]
                nn += i
                while (i < nn)
                    sortBone(bones[pathBones[i++]])
            }
        }
    }

    private fun sortBone(bone: Bone) {
        if (bone.sorted) return
        val parent = bone.parent
        if (parent != null) sortBone(parent)
        bone.sorted = true
        updateCache.add(bone)
    }

    private fun sortReset(bones: FastArrayList<Bone>) {
        var i = 0
        val n = bones.size
        while (i < n) {
            val bone = bones[i]
            if (!bone.isActive) {
                i++
                continue
            }
            if (bone.sorted) sortReset(bone.children)
            bone.sorted = false
            i++
        }
    }

    /** Updates the world transform for each bone and applies all constraints.
     *
     *
     * See [World transforms](http://esotericsoftware.com/spine-runtime-skeletons#World-transforms) in the Spine
     * Runtimes Guide.  */
    fun updateWorldTransform() {
        // This partial update avoids computing the world transform for constrained bones when 1) the bone is not updated
        // before the constraint, 2) the constraint only needs to access the applied local transform, and 3) the constraint calls
        // updateWorldTransform.
        val updateCacheReset = this.updateCacheReset
        run {
            var i = 0
            val n = updateCacheReset.size
            while (i < n) {
                val bone = updateCacheReset[i]
                bone.ax = bone.x
                bone.ay = bone.y
                bone.arotation = bone.rotation
                bone.ascaleX = bone.scaleX
                bone.ascaleY = bone.scaleY
                bone.ashearX = bone.shearX
                bone.ashearY = bone.shearY
                bone.appliedValid = true
                i++
            }
        }
        val updateCache = this.updateCache
        var i = 0
        val n = updateCache.size
        while (i < n) {
            updateCache[i].update()
            i++
        }
    }

    /** Temporarily sets the root bone as a child of the specified bone, then updates the world transform for each bone and applies
     * all constraints.
     *
     *
     * See [World transforms](http://esotericsoftware.com/spine-runtime-skeletons#World-transforms) in the Spine
     * Runtimes Guide.  */
    fun updateWorldTransform(parent: Bone) {
        // This partial update avoids computing the world transform for constrained bones when 1) the bone is not updated
        // before the constraint, 2) the constraint only needs to access the applied local transform, and 3) the constraint calls
        // updateWorldTransform.
        val updateCacheReset = this.updateCacheReset
        run {
            var i = 0
            val n = updateCacheReset.size
            while (i < n) {
                val bone = updateCacheReset[i]
                bone.ax = bone.x
                bone.ay = bone.y
                bone.arotation = bone.rotation
                bone.ascaleX = bone.scaleX
                bone.ascaleY = bone.scaleY
                bone.ashearX = bone.shearX
                bone.ashearY = bone.shearY
                bone.appliedValid = true
                i++
            }
        }

        // Apply the parent bone transform to the root bone. The root bone always inherits scale, rotation and reflection.
        val rootBone = rootBone
        val pa = parent.a
        val pb = parent.b
        val pc = parent.c
        val pd = parent.d
        rootBone!!.worldX = pa * x + pb * y + parent.worldX
        rootBone.worldY = pc * x + pd * y + parent.worldY

        val rotationY = rootBone.rotation + 90f + rootBone.shearY
        val la = cosDeg(rootBone.rotation + rootBone.shearX) * rootBone.scaleX
        val lb = cosDeg(rotationY) * rootBone.scaleY
        val lc = sinDeg(rootBone.rotation + rootBone.shearX) * rootBone.scaleX
        val ld = sinDeg(rotationY) * rootBone.scaleY
        rootBone.a = (pa * la + pb * lc) * scaleX
        rootBone.b = (pa * lb + pb * ld) * scaleX
        rootBone.c = (pc * la + pd * lc) * scaleY
        rootBone.d = (pc * lb + pd * ld) * scaleY

        // Update everything except root bone.
        val updateCache = this.updateCache
        var i = 0
        val n = updateCache.size
        while (i < n) {
            val updatable = updateCache[i]
            if (updatable !== rootBone) updatable.update()
            i++
        }
    }

    /** Sets the bones, constraints, slots, and draw order to their setup pose values.  */
    fun setToSetupPose() {
        setBonesToSetupPose()
        setSlotsToSetupPose()
    }

    /** Sets the bones and constraints to their setup pose values.  */
    fun setBonesToSetupPose() {
        val bones = this.bones
        run {
            var i = 0
            val n = bones.size
            while (i < n) {
                bones[i].setToSetupPose()
                i++
            }
        }

        val ikConstraints = this.ikConstraints
        run {
            var i = 0
            val n = ikConstraints.size
            while (i < n) {
                val constraint = ikConstraints[i]
                constraint.mix = constraint.data.mix
                constraint.softness = constraint.data.softness
                constraint.bendDirection = constraint.data.bendDirection
                constraint.compress = constraint.data.compress
                constraint.stretch = constraint.data.stretch
                i++
            }
        }

        val transformConstraints = this.transformConstraints
        run {
            var i = 0
            val n = transformConstraints.size
            while (i < n) {
                val constraint = transformConstraints[i]
                val data = constraint.data
                constraint.rotateMix = data.rotateMix
                constraint.translateMix = data.translateMix
                constraint.scaleMix = data.scaleMix
                constraint.shearMix = data.shearMix
                i++
            }
        }

        val pathConstraints = this.pathConstraints
        var i = 0
        val n = pathConstraints.size
        while (i < n) {
            val constraint = pathConstraints[i]
            val data = constraint.data
            constraint.position = data.position
            constraint.spacing = data.spacing
            constraint.rotateMix = data.rotateMix
            constraint.translateMix = data.translateMix
            i++
        }
    }

    /** Sets the slots and draw order to their setup pose values.  */
    fun setSlotsToSetupPose() {
        val slots = this.slots
        arraycopy(slots, 0, drawOrder, 0, slots.size)
        var i = 0
        val n = slots.size
        while (i < n) {
            slots[i].setToSetupPose()
            i++
        }
    }

    /** Finds a bone by comparing each bone's name. It is more efficient to cache the results of this method than to call it
     * repeatedly.
     * @return May be null.
     */
    fun findBone(boneName: String): Bone? {
        val bones = this.bones
        for (i in 0 until bones.size) {
            val bone = bones[i]
            if (bone.data.name == boneName) return bone
        }
        return null
    }

    /** Finds a slot by comparing each slot's name. It is more efficient to cache the results of this method than to call it
     * repeatedly.
     * @return May be null.
     */
    fun findSlot(slotName: String): Slot? {
        val slots = this.slots
        var i = 0
        val n = slots.size
        while (i < n) {
            val slot = slots[i]
            if (slot.data.name == slotName) return slot
            i++
        }
        return null
    }

    /** The skeleton's slots in the order they should be drawn. The returned array may be modified to change the draw order.  */
    fun getDrawOrder(): FastArrayList<Slot> {
        return drawOrder
    }

    fun setDrawOrder(drawOrder: FastArrayList<Slot>) {
        this.drawOrder = drawOrder
    }

    /** The skeleton's current skin.
     * @return May be null.
     */
    fun getSkin(): Skin? {
        return skin
    }

    /** Sets a skin by name.
     *
     *
     * See [.setSkin].  */
    fun setSkin(skinName: String) {
        val skin = data.findSkin(skinName) ?: throw IllegalArgumentException("Skin not found: $skinName")
        setSkin(skin)
    }

    /** Sets the skin used to look up attachments before looking in the [default skin][SkeletonData.getDefaultSkin]. If the
     * skin is changed, [.updateCache] is called.
     *
     *
     * Attachments from the new skin are attached if the corresponding attachment from the old skin was attached. If there was no
     * old skin, each slot's setup mode attachment is attached from the new skin.
     *
     *
     * After changing the skin, the visible attachments can be reset to those attached in the setup pose by calling
     * [.setSlotsToSetupPose]. Also, often [AnimationState.apply] is called before the next time the
     * skeleton is rendered to allow any attachment keys in the current animation(s) to hide or show attachments from the new skin.
     * @param newSkin May be null.
     */
    fun setSkin(newSkin: Skin?) {
        if (newSkin === skin) return
        if (newSkin != null) {
            if (skin != null)
                newSkin.attachAll(this, skin!!)
            else {
                val slots = this.slots
                var i = 0
                val n = slots.size
                while (i < n) {
                    val slot = slots[i]
                    val name = slot.data.attachmentName
                    if (name != null) {
                        val attachment = newSkin.getAttachment(i, name)
                        if (attachment != null) slot.setAttachment(attachment)
                    }
                    i++
                }
            }
        }
        skin = newSkin
        updateCache()
    }

    /** Finds an attachment by looking in the [.skin] and [SkeletonData.defaultSkin] using the slot name and attachment
     * name.
     *
     *
     * See [.getAttachment].
     * @return May be null.
     */
    fun getAttachment(slotName: String, attachmentName: String): Attachment? {
        val slot = data.findSlot(slotName) ?: throw IllegalArgumentException("Slot not found: $slotName")
        return getAttachment(slot.index, attachmentName)
    }

    /** Finds an attachment by looking in the [.skin] and [SkeletonData.defaultSkin] using the slot index and
     * attachment name. First the skin is checked and if the attachment was not found, the default skin is checked.
     *
     *
     * See [Runtime skins](http://esotericsoftware.com/spine-runtime-skins) in the Spine Runtimes Guide.
     * @return May be null.
     */
    fun getAttachment(slotIndex: Int, attachmentName: String): Attachment? {
        if (skin != null) {
            val attachment = skin!!.getAttachment(slotIndex, attachmentName)
            if (attachment != null) return attachment
        }
        return if (data.defaultSkin != null) data.defaultSkin.getAttachment(slotIndex, attachmentName) else null
    }

    /** A convenience method to set an attachment by finding the slot with [.findSlot], finding the attachment with
     * [.getAttachment], then setting the slot's [Slot.attachment].
     * @param attachmentName May be null to clear the slot's attachment.
     */
    fun setAttachment(slotName: String, attachmentName: String?) {
        val slot = findSlot(slotName) ?: throw IllegalArgumentException("Slot not found: $slotName")
        var attachment: Attachment? = null
        if (attachmentName != null) {
            attachment = getAttachment(slot.data.index, attachmentName)
                ?: error("Attachment not found: $attachmentName, for slot: $slotName")
        }
        slot.setAttachment(attachment)
    }

    /** Finds an IK constraint by comparing each IK constraint's name. It is more efficient to cache the results of this method
     * than to call it repeatedly.
     * @return May be null.
     */
    fun findIkConstraint(constraintName: String): IkConstraint? {
        val ikConstraints = this.ikConstraints
        var i = 0
        val n = ikConstraints.size
        while (i < n) {
            val ikConstraint = ikConstraints[i]
            if (ikConstraint.data.name == constraintName) return ikConstraint
            i++
        }
        return null
    }

    /** Finds a transform constraint by comparing each transform constraint's name. It is more efficient to cache the results of
     * this method than to call it repeatedly.
     * @return May be null.
     */
    fun findTransformConstraint(constraintName: String): TransformConstraint? {
        val transformConstraints = this.transformConstraints
        var i = 0
        val n = transformConstraints.size
        while (i < n) {
            val constraint = transformConstraints[i]
            if (constraint.data.name == constraintName) return constraint
            i++
        }
        return null
    }

    /** Finds a path constraint by comparing each path constraint's name. It is more efficient to cache the results of this method
     * than to call it repeatedly.
     * @return May be null.
     */
    fun findPathConstraint(constraintName: String): PathConstraint? {
        val pathConstraints = this.pathConstraints
        var i = 0
        val n = pathConstraints.size
        while (i < n) {
            val constraint = pathConstraints[i]
            if (constraint.data.name == constraintName) return constraint
            i++
        }
        return null
    }

    /** Returns the axis aligned bounding box (AABB) of the region and mesh attachments for the current pose.
     * @param offset An output value, the distance from the skeleton origin to the bottom left corner of the AABB.
     * @param size An output value, the width and height of the AABB.
     * @param temp Working memory to temporarily store attachments' computed world vertices.
     */
    fun getBounds(offset: SpineVector2, size: SpineVector2, temp: FloatArrayList) {
        val drawOrder = this.drawOrder
        var minX = Int.MAX_VALUE.toFloat()
        var minY = Int.MAX_VALUE.toFloat()
        var maxX = Int.MIN_VALUE.toFloat()
        var maxY = Int.MIN_VALUE.toFloat()
        var i = 0
        val n = drawOrder.size
        while (i < n) {
            val slot = drawOrder[i]
            if (!slot.bone.isActive) {
                i++
                continue
            }
            var verticesLength = 0
            var vertices: FloatArray? = null
            val attachment = slot.attachment
            if (attachment is RegionAttachment) {
                verticesLength = 8
                vertices = temp.setSize(8)
                attachment.computeWorldVertices(slot.bone, vertices, 0, 2)
            } else if (attachment is MeshAttachment) {
                verticesLength = attachment.worldVerticesLength
                vertices = temp.setSize(verticesLength)
                attachment.computeWorldVertices(slot, 0, verticesLength, vertices, 0, 2)
            }
            if (vertices != null) {
                var ii = 0
                while (ii < verticesLength) {
                    val x = vertices[ii]
                    val y = vertices[ii + 1]
                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)
                    ii += 2
                }
            }
            i++
        }
        offset[minX] = minY
        size[maxX - minX] = maxY - minY
    }

    /** The color to tint all the skeleton's attachments.  */
    fun getColor(): RGBAf {
        return color
    }

    /** A convenience method for setting the skeleton color. The color can also be set by modifying [.getColor].  */
    fun setColor(color: RGBAf) {
        this.color.setTo(color)
    }

    fun setScale(scaleX: Float, scaleY: Float) {
        this.scaleX = scaleX
        this.scaleY = scaleY
    }

    /** Sets the skeleton X and Y position, which is added to the root bone worldX and worldY position.  */
    fun setPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    /** Increments the skeleton's [.time].  */
    fun update(delta: Float) {
        time += delta
    }

    override fun toString(): String {
        return if (data.name != null) data.name!! else super.toString()
    }
}
