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

/** Stores the setup pose for a [TransformConstraint].
 *
 *
 * See [Transform constraints](http://esotericsoftware.com/spine-transform-constraints) in the Spine User Guide.  */
class TransformConstraintData(name: String) : ConstraintData(name) {
    /** The bones that will be modified by this transform constraint.  */
    val bones: FastArrayList<BoneData> = FastArrayList()
    internal lateinit var target: BoneData

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained rotations.  */
    var rotateMix: Float = 0.toFloat()

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained translations.  */
    var translateMix: Float = 0.toFloat()

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained scales.  */
    var scaleMix: Float = 0.toFloat()

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained shears.  */
    var shearMix: Float = 0.toFloat()

    /** An offset added to the constrained bone rotation.  */
    var offsetRotation: Float = 0.toFloat()

    /** An offset added to the constrained bone X translation.  */
    var offsetX: Float = 0.toFloat()

    /** An offset added to the constrained bone Y translation.  */
    var offsetY: Float = 0.toFloat()

    /** An offset added to the constrained bone scaleX.  */
    var offsetScaleX: Float = 0.toFloat()

    /** An offset added to the constrained bone scaleY.  */
    var offsetScaleY: Float = 0.toFloat()

    /** An offset added to the constrained bone shearY.  */
    var offsetShearY: Float = 0.toFloat()
    var relative: Boolean = false
    var local: Boolean = false

    /** The target bone whose world transform will be copied to the constrained bones.  */
    fun getTarget(): BoneData {
        return target
    }

    fun setTarget(target: BoneData) {
        this.target = target
    }
}
