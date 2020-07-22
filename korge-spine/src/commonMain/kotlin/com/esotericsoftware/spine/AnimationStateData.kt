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

import com.esotericsoftware.spine.AnimationState.*

/** Stores mix (crossfade) durations to be applied when [AnimationState] animations are changed.  */
class AnimationStateData(
        /** The SkeletonData to look up animations when they are specified by name.  */
        val skeletonData: SkeletonData
) {
    data class FloatHolder(var value: Float)
    internal val animationToMixTime: HashMap<Key, FloatHolder> = HashMap(51, 0.8f)
    internal val tempKey = Key()

    /** The mix duration to use when no mix duration has been defined between two animations.  */
    var defaultMix: Float = 0.toFloat()

    /** Sets a mix duration by animation name.
     *
     *
     * See [.setMix].  */
    fun setMix(fromName: String, toName: String, duration: Float) {
        val from = skeletonData.findAnimation(fromName)
                ?: throw IllegalArgumentException("Animation not found: $fromName")
        val to = skeletonData.findAnimation(toName) ?: throw IllegalArgumentException("Animation not found: $toName")
        setMix(from, to, duration)
    }

    /** Sets the mix duration when changing from the specified animation to the other.
     *
     *
     * See [TrackEntry.mixDuration].  */
    fun setMix(from: Animation, to: Animation, duration: Float) {
        val key = Key()
        key.a1 = from
        key.a2 = to
        val holder = animationToMixTime.getOrPut(key) { FloatHolder(0f) }
        holder.value = duration
    }

    /** Returns the mix duration to use when changing from the specified animation to the other, or the [.getDefaultMix] if
     * no mix duration has been set.  */
    fun getMix(from: Animation?, to: Animation?): Float {
        tempKey.a1 = from
        tempKey.a2 = to
        val holder = animationToMixTime[tempKey]
        @Suppress("IfThenToElvis")
        return if (holder != null) holder.value else defaultMix
    }

    internal class Key {
        var a1: Animation? = null
        var a2: Animation? = null

        override fun hashCode(): Int {
            return 31 * (31 + a1!!.hashCode()) + a2!!.hashCode()
        }

        override fun equals(obj: Any?): Boolean {
            if (this === obj) return true
            if (obj == null) return false
            val other = obj as Key?
            if (a1 == null) {
                if (other!!.a1 != null) return false
            } else if (a1 != other!!.a1) return false
            if (a2 == null) {
                if (other.a2 != null) return false
            } else if (a2 != other.a2) return false
            return true
        }

        override fun toString(): String {
            return a1!!.name + "->" + a2!!.name
        }
    }
}
