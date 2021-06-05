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

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*

/** Stores the setup pose and all of the stateless data for a skeleton.
 *
 *
 * See [Data objects](http://esotericsoftware.com/spine-runtime-architecture#Data-objects) in the Spine Runtimes
 * Guide.  */
class SkeletonData {
    // ---

    /** The skeleton's name, which by default is the name of the skeleton data file, if possible.
     * @return May be null.
     */
    /** @param name May be null.
     */
    var name: String? = null
    // --- Bones.

    /** The skeleton's bones, sorted parent first. The root bone is always the first bone.  */
    val bones: FastArrayList<BoneData> = FastArrayList<BoneData>() // Ordered parents first.
    // --- Slots.

    /** The skeleton's slots.  */
    val slots: FastArrayList<SlotData> = FastArrayList<SlotData>() // Setup pose draw order.

    /** All skins, including the default skin.  */
    val skins: FastArrayList<Skin> = FastArrayList<Skin>()
    // --- Skins.

    /** The skeleton's default skin. By default this skin contains all attachments that were not in a skin in Spine.
     *
     *
     * See [Skeleton.getAttachment].
     * @return May be null.
     */
    /** @param defaultSkin May be null.
     */
    lateinit var defaultSkin: Skin

    /** The skeleton's events.  */
    val events: FastArrayList<EventData> = FastArrayList()
    // --- Animations.

    /** The skeleton's animations.  */
    val animations: FastArrayList<Animation> = FastArrayList()
    // --- IK constraints

    /** The skeleton's IK constraints.  */
    val ikConstraints: FastArrayList<IkConstraintData> = FastArrayList()
    // --- Transform constraints

    /** The skeleton's transform constraints.  */
    val transformConstraints: FastArrayList<TransformConstraintData> = FastArrayList()
    // --- Path constraints

    /** The skeleton's path constraints.  */
    val pathConstraints: FastArrayList<PathConstraintData> = FastArrayList()

    /** The X coordinate of the skeleton's axis aligned bounding box in the setup pose.  */
    var x: Float = 0.toFloat()

    /** The Y coordinate of the skeleton's axis aligned bounding box in the setup pose.  */
    var y: Float = 0.toFloat()

    /** The width of the skeleton's axis aligned bounding box in the setup pose.  */
    var width: Float = 0.toFloat()

    /** The height of the skeleton's axis aligned bounding box in the setup pose.  */
    var height: Float = 0.toFloat()
    /** The Spine version used to export the skeleton data, or null.  */
    /** @param version May be null.
     */
    var version: String? = null
    /** The skeleton data hash. This value will change if any of the skeleton data has changed.
     * @return May be null.
     */
    /** @param hash May be null.
     */
    var hash: String? = null

    // Nonessential.
    /** The dopesheet FPS in Spine. Available only when nonessential data was exported.  */
    var fps = 30f
    /** The path to the images directory as defined in Spine. Available only when nonessential data was exported.
     * @return May be null.
     */
    /** @param imagesPath May be null.
     */
    var imagesPath: String? = null
    /** The path to the audio directory as defined in Spine. Available only when nonessential data was exported.
     * @return May be null.
     */
    /** @param audioPath May be null.
     */
    var audioPath: String? = null

    /** Finds a bone by comparing each bone's name. It is more efficient to cache the results of this method than to call it
     * multiple times.
     * @return May be null.
     */
    fun findBone(boneName: String?): BoneData? {
        val bones = this.bones
        var i = 0
        val n = bones.size
        while (i < n) {
            val bone = bones[i]
            if (bone.name == boneName) return bone
            i++
        }
        return null
    }

    /** Finds a slot by comparing each slot's name. It is more efficient to cache the results of this method than to call it
     * multiple times.
     * @return May be null.
     */
    fun findSlot(slotName: String?): SlotData? {
        val slots = this.slots
        var i = 0
        val n = slots.size
        while (i < n) {
            val slot = slots[i]
            if (slot.name == slotName) return slot
            i++
        }
        return null
    }

    /** Finds a skin by comparing each skin's name. It is more efficient to cache the results of this method than to call it
     * multiple times.
     * @return May be null.
     */
    fun findSkin(skinName: String?): Skin? {
        skins.fastForEach { skin ->
            if (skin.name == skinName) return skin
        }
        return null
    }

    // --- Events.

    /** Finds an event by comparing each events's name. It is more efficient to cache the results of this method than to call it
     * multiple times.
     * @return May be null.
     */
    fun findEvent(eventDataName: String?): EventData? {
        events.fastForEach { eventData ->
            if (eventData.name == eventDataName) return eventData
        }
        return null
    }

    /** Finds an animation by comparing each animation's name. It is more efficient to cache the results of this method than to
     * call it multiple times.
     * @return May be null.
     */
    fun findAnimation(animationName: String): Animation? {
        val animations = this.animations
        var i = 0
        val n = animations.size
        while (i < n) {
            val animation = animations[i]
            if (animation.name == animationName) return animation
            i++
        }
        return null
    }

    /** Finds an IK constraint by comparing each IK constraint's name. It is more efficient to cache the results of this method
     * than to call it multiple times.
     * @return May be null.
     */
    fun findIkConstraint(constraintName: String?): IkConstraintData? {
        val ikConstraints = this.ikConstraints
        var i = 0
        val n = ikConstraints.size
        while (i < n) {
            val constraint = ikConstraints[i]
            if (constraint.name == constraintName) return constraint
            i++
        }
        return null
    }

    /** Finds a transform constraint by comparing each transform constraint's name. It is more efficient to cache the results of
     * this method than to call it multiple times.
     * @return May be null.
     */
    fun findTransformConstraint(constraintName: String?): TransformConstraintData? {
        val transformConstraints = this.transformConstraints
        var i = 0
        val n = transformConstraints.size
        while (i < n) {
            val constraint = transformConstraints[i]
            if (constraint.name == constraintName) return constraint
            i++
        }
        return null
    }

    /** Finds a path constraint by comparing each path constraint's name. It is more efficient to cache the results of this method
     * than to call it multiple times.
     * @return May be null.
     */
    fun findPathConstraint(constraintName: String?): PathConstraintData? {
        val pathConstraints = this.pathConstraints
        var i = 0
        val n = pathConstraints.size
        while (i < n) {
            val constraint = pathConstraints[i]
            if (constraint.name == constraintName) return constraint
            i++
        }
        return null
    }

    override fun toString(): String = name ?: super.toString()
}
