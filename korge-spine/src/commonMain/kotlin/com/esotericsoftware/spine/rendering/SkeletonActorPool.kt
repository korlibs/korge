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
import com.esotericsoftware.spine.utils.JArray
import com.esotericsoftware.spine.utils.Pool
import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.AnimationStateData
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.SkeletonData

class SkeletonActorPool  constructor(
    private val renderer: SkeletonRenderer,
    internal var skeletonData: SkeletonData, internal var stateData: AnimationStateData,
    initialCapacity: Int = 16, max: Int = Int.MAX_VALUE
) : Pool<SkeletonActor>(initialCapacity, max) {
    private val skeletonPool: Pool<Skeleton>
    private val statePool: Pool<AnimationState>
    val obtained: JArray<SkeletonActor>

    init {

        obtained = JArray(false, initialCapacity)

        skeletonPool = object : Pool<Skeleton>(initialCapacity, max) {
            override fun newObject(): Skeleton {
                return Skeleton(this@SkeletonActorPool.skeletonData)
            }

            override fun reset(skeleton: Skeleton) {
                skeleton.color = Color.WHITE
                skeleton.setScale(1f, 1f)
                skeleton.skin = null
                skeleton.skin = this@SkeletonActorPool.skeletonData.defaultSkin
                skeleton.setToSetupPose()
            }
        }

        statePool = object : Pool<AnimationState>(initialCapacity, max) {
            override fun newObject(): AnimationState {
                return AnimationState(this@SkeletonActorPool.stateData)
            }

            override fun reset(state: AnimationState) {
                state.clearTracks()
                state.clearListeners()
            }
        }
    }

    /** Each obtained skeleton actor that is no longer playing an animation is removed from the stage and returned to the pool.  */
    fun freeComplete() {
        val obtained = this.obtained
        outer@ for (i in obtained.size - 1 downTo 0) {
            val actor = obtained[i]
            val tracks = actor.animationState.tracks
            var ii = 0
            val nn = tracks.size
            while (ii < nn) {
                if (tracks[ii] != null) continue@outer
                ii++
            }
            free(actor)
        }
    }

    override fun newObject(): SkeletonActor {
        val actor = SkeletonActor()
        actor.renderer = renderer
        return actor
    }

    /** This pool keeps a reference to the obtained instance, so it should be returned to the pool via [.free]
     * , [.freeAll] or [.freeComplete] to avoid leaking memory.  */
    override fun obtain(): SkeletonActor {
        val actor = super.obtain()
        actor.skeleton = skeletonPool.obtain()
        actor.animationState = statePool.obtain()
        obtained.add(actor)
        return actor
    }

    override fun reset(actor: SkeletonActor) {
        actor.remove()
        obtained.removeValue(actor, true)
        skeletonPool.free(actor.skeleton)
        statePool.free(actor.animationState)
    }
}
