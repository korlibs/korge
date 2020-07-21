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

import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.Skeleton

/** A scene2d actor that draws a skeleton.  */
class SkeletonActor : Actor {
    lateinit var renderer: SkeletonRenderer
    lateinit var skeleton: Skeleton
    lateinit var animationState: AnimationState

    /** If false, the blend function will be left as whatever [SkeletonRenderer.draw] set. This can reduce
     * batch flushes in some cases, but means other rendering may need to first set the blend function. Default is true.  */
    var resetBlendFunction = true

    /** Creates an uninitialized SkeletonActor. The renderer, skeleton, and animation state must be set before use.  */
    constructor() {}

    constructor(renderer: SkeletonRenderer, skeleton: Skeleton, state: AnimationState) {
        this.renderer = renderer
        this.skeleton = skeleton
        this.animationState = state
    }

    override fun act(delta: Float) {
        animationState.update(delta)
        animationState.apply(skeleton)
        super.act(delta)
    }

    fun draw(batch: Batch, parentAlpha: Float) {
        val blendSrc = batch.blendSrcFunc
        val blendDst = batch.blendDstFunc
        val blendSrcAlpha = batch.blendSrcFuncAlpha
        val blendDstAlpha = batch.blendDstFuncAlpha

        val color = skeleton.color
        val oldAlpha = color.a
        skeleton.color.a *= parentAlpha

        skeleton.setPosition(x, y)
        skeleton.updateWorldTransform()
        renderer.draw(batch, skeleton)

        if (resetBlendFunction) batch.setBlendFunctionSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha)

        color.a = oldAlpha
    }
}
