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

import com.esotericsoftware.spine.attachments.Attachment
import com.esotericsoftware.spine.attachments.MeshAttachment
import com.esotericsoftware.spine.utils.*
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*

/** Stores attachments by slot index and attachment name.
 *
 *
 * See SkeletonData [SkeletonData.defaultSkin], Skeleton [Skeleton.skin], and
 * [Runtime skins](http://esotericsoftware.com/spine-runtime-skins) in the Spine Runtimes Guide.  */
class Skin(
        /** The skin's name, which is unique across all skins in the skeleton.  */
        val name: String
) {
    internal val attachments: LinkedHashMap<SkinEntry, SkinEntry> = LinkedHashMap()
    val bones: FastArrayList<BoneData> = FastArrayList()
    val constraints: FastArrayList<ConstraintData> = FastArrayList()
    private val lookup = SkinEntry()

    /** Adds an attachment to the skin for the specified slot index and name.  */
    fun setAttachment(slotIndex: Int, name: String, attachment: Attachment) {
        require(slotIndex >= 0) { "slotIndex must be >= 0." }
        val newEntry = SkinEntry(slotIndex, name, attachment)
        val oldEntry = attachments.put(newEntry, newEntry)
        if (oldEntry != null) {
            oldEntry.attachment = attachment
        }
    }

    /** Adds all attachments, bones, and constraints from the specified skin to this skin.  */
    fun addSkin(skin: Skin) {
        skin.bones.fastForEach { data ->
            if (!bones.containsIdentity(data)) bones.add(data)
        }

        skin.constraints.fastForEach { data ->
            if (!constraints.containsIdentity(data)) constraints.add(data)
        }

        for (entry in skin.attachments.keys)
            setAttachment(entry.slotIndex, entry.name, entry.attachment!!)
    }

    /** Adds all bones and constraints and copies of all attachments from the specified skin to this skin. Mesh attachments are not
     * copied, instead a new linked mesh is created. The attachment copies can be modified without affecting the originals.  */
    fun copySkin(skin: Skin) {
        skin.bones.fastForEach { data ->
            if (!bones.containsIdentity(data)) bones.add(data)
        }

        skin.constraints.fastForEach { data ->
            if (!constraints.containsIdentity(data)) constraints.add(data)
        }

        for (entry in skin.attachments.keys) {
            if (entry.attachment is MeshAttachment)
                setAttachment(entry.slotIndex, entry.name, (entry.attachment as MeshAttachment).newLinkedMesh())
            else
                setAttachment(entry.slotIndex, entry.name, entry.attachment?.copy() ?: error("Cannot be null (verify)"))
        }
    }

    /** Returns the attachment for the specified slot index and name, or null.  */
    fun getAttachment(slotIndex: Int, name: String): Attachment? {
        require(slotIndex >= 0) { "slotIndex must be >= 0." }
        lookup[slotIndex] = name
        val entry = attachments[lookup]
        return entry?.attachment
    }

    /** Removes the attachment in the skin for the specified slot index and name, if any.  */
    fun removeAttachment(slotIndex: Int, name: String) {
        require(slotIndex >= 0) { "slotIndex must be >= 0." }
        lookup[slotIndex] = name
        attachments.remove(lookup)
    }

    /** Returns all attachments in this skin.  */
    fun getAttachments(): Set<SkinEntry> {
        return attachments.keys
    }

    /** Returns all attachments in this skin for the specified slot index.  */
    fun getAttachments(slotIndex: Int, attachments: FastArrayList<SkinEntry>) {
        require(slotIndex >= 0) { "slotIndex must be >= 0." }
        for (entry in this.attachments.keys)
            if (entry.slotIndex == slotIndex) attachments.add(entry)
    }

    /** Clears all attachments, bones, and constraints.  */
    fun clear() {
        //attachments.clear(1024)
        attachments.clear()
        bones.clear()
        constraints.clear()
    }

    override fun toString(): String {
        return name
    }

    /** Attach each attachment in this skin if the corresponding attachment in the old skin is currently attached.  */
    internal fun attachAll(skeleton: Skeleton, oldSkin: Skin) {
        for (entry in oldSkin.attachments.keys) {
            val slotIndex = entry.slotIndex
            val slot = skeleton.slots[slotIndex]
            if (slot.attachment === entry.attachment) {
                val attachment = getAttachment(slotIndex, entry.name)
                if (attachment != null) slot.setAttachment(attachment)
            }
        }
    }

    /** Stores an entry in the skin consisting of the slot index, name, and attachment  */
    class SkinEntry {
        var slotIndex: Int = 0
            internal set

        /** The name the attachment is associated with, equivalent to the skin placeholder name in the Spine editor.  */
        lateinit var name: String
            internal set
        var attachment: Attachment? = null
            internal set
        private var hashCode: Int = 0

        internal constructor() {
            set(0, "")
        }

        internal constructor(slotIndex: Int, name: String, attachment: Attachment) {
            set(slotIndex, name)
            this.attachment = attachment
        }

        internal operator fun set(slotIndex: Int, name: String) {
            this.slotIndex = slotIndex
            this.name = name
            this.hashCode = name.hashCode() + slotIndex * 37
        }

        override fun hashCode(): Int {
            return hashCode
        }

        override fun equals(`object`: Any?): Boolean {
            if (`object` == null) return false
            val other = `object` as SkinEntry?
            if (slotIndex != other!!.slotIndex) return false
            return if (name != other.name) false else true
        }

        override fun toString(): String {
            return "$slotIndex:$name"
        }
    }
}
