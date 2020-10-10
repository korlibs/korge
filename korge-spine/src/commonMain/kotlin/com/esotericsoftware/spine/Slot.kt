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

import com.esotericsoftware.spine.Animation.DeformTimeline
import com.esotericsoftware.spine.attachments.Attachment
import com.esotericsoftware.spine.attachments.VertexAttachment
import com.soywiz.kds.*

/** Stores a slot's current pose. Slots organize attachments for [Skeleton.drawOrder] purposes and provide a place to store
 * state for an attachment. State cannot be stored in an attachment itself because attachments are stateless and may be shared
 * across multiple skeletons.  */
class Slot {
    /** The slot's setup pose data.  */
    val data: SlotData

    /** The bone this slot belongs to.  */
    val bone: Bone

    /** The color used to tint the slot's attachment. If [.getDarkColor] is set, this is used as the light color for two
     * color tinting.  */
    val color = RGBAf()

    /** The dark color used to tint the slot's attachment for two color tinting, or null if two color tinting is not used. The dark
     * color's alpha is not used.  */
    val darkColor: RGBAf?
    internal var attachment: Attachment? = null
    private var attachmentTime: Float = 0.toFloat()

    /** Values to deform the slot's attachment. For an unweighted mesh, the entries are local positions for each vertex. For a
     * weighted mesh, the entries are an offset for each vertex which will be added to the mesh's local vertex positions.
     *
     *
     * See [VertexAttachment.computeWorldVertices] and [DeformTimeline].  */
    var deform: FloatArrayList = FloatArrayList()

    internal var attachmentState: Int = 0

    /** The skeleton this slot belongs to.  */
    val skeleton: Skeleton
        get() = bone.skeleton

    constructor(data: SlotData, bone: Bone) {
        this.data = data
        this.bone = bone
        darkColor = if (data.darkColor == null) null else RGBAf()
        setToSetupPose()
    }

    /** Copy constructor.  */
    constructor(slot: Slot, bone: Bone) {
        data = slot.data
        this.bone = bone
        color.setTo(slot.color)
        darkColor = if (slot.darkColor == null) null else RGBAf(slot.darkColor)
        attachment = slot.attachment
        attachmentTime = slot.attachmentTime
        this.deform.add(slot.deform)
    }

    /** The current attachment for the slot, or null if the slot has no attachment.  */
    fun getAttachment(): Attachment? {
        return attachment
    }

    /** Sets the slot's attachment and, if the attachment changed, resets [.attachmentTime] and clears [.deform].
     * @param attachment May be null.
     */
    fun setAttachment(attachment: Attachment?) {
        if (this.attachment === attachment) return
        this.attachment = attachment
        attachmentTime = bone.skeleton.time
        this.deform!!.clear()
    }

    /** The time that has elapsed since the last time the attachment was set or cleared. Relies on Skeleton
     * [Skeleton.time].  */
    fun getAttachmentTime(): Float {
        return bone.skeleton.time - attachmentTime
    }

    fun setAttachmentTime(time: Float) {
        attachmentTime = bone.skeleton.time - time
    }

    /** Sets this slot to the setup pose.  */
    fun setToSetupPose() {
        color.setTo(data.color)
        darkColor?.setTo(data.darkColor!!)
        if (data.attachmentName == null)
            setAttachment(null)
        else {
            attachment = null
            setAttachment(bone.skeleton.getAttachment(data.index, data.attachmentName!!))
        }
    }

    override fun toString(): String {
        return data.name
    }
}
