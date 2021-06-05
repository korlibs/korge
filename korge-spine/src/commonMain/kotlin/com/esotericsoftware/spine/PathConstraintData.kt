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
import kotlin.js.*

/** Stores the setup pose for a [PathConstraint].
 *
 *
 * See [Path constraints](http://esotericsoftware.com/spine-path-constraints) in the Spine User Guide.  */
class PathConstraintData(name: String) : ConstraintData(name) {
    /** The bones that will be modified by this path constraint.  */
    val bones: FastArrayList<BoneData> = FastArrayList()
    internal lateinit var target: SlotData
    internal lateinit var positionMode: PositionMode
    internal lateinit var spacingMode: SpacingMode
    internal lateinit var rotateMode: RotateMode

    /** An offset added to the constrained bone rotation.  */
    var offsetRotation: Float = 0.toFloat()

    /** The position along the path.  */
    var position: Float = 0.toFloat()

    /** The spacing between bones.  */
    var spacing: Float = 0.toFloat()

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained rotations.  */
    var rotateMix: Float = 0.toFloat()

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained translations.  */
    var translateMix: Float = 0.toFloat()

    /** The slot whose path attachment will be used to constrained the bones.  */
    fun getTarget(): SlotData {
        return target
    }

    fun setTarget(target: SlotData) {
        this.target = target
    }

    /** The mode for positioning the first bone on the path.  */
    fun getPositionMode(): PositionMode {
        return positionMode
    }

    fun setPositionMode(positionMode: PositionMode) {
        this.positionMode = positionMode
    }

    /** The mode for positioning the bones after the first bone on the path.  */
    fun getSpacingMode(): SpacingMode {
        return spacingMode
    }

    fun setSpacingMode(spacingMode: SpacingMode) {
        this.spacingMode = spacingMode
    }

    /** The mode for adjusting the rotation of the bones.  */
    fun getRotateMode(): RotateMode {
        return rotateMode
    }

    fun setRotateMode(rotateMode: RotateMode) {
        this.rotateMode = rotateMode
    }

    /** Controls how the first bone is positioned along the path.
     *
     *
     * See [Position mode](http://esotericsoftware.com/spine-path-constraints#Position-mode) in the Spine User Guide.  */
    enum class PositionMode {
        fixed, percent;


        companion object {

            val values = PositionMode.values()
        }
    }

    /** Controls how bones after the first bone are positioned along the path.
     *
     *
     * See [Spacing mode](http://esotericsoftware.com/spine-path-constraints#Spacing-mode) in the Spine User Guide.  */
    enum class SpacingMode {
        @JsName("length2") length, fixed, percent;


        companion object {

            val values = SpacingMode.values()
        }
    }

    /** Controls how bones are rotated, translated, and scaled to match the path.
     *
     *
     * See [Rotate mode](http://esotericsoftware.com/spine-path-constraints#Rotate-mode) in the Spine User Guide.  */
    enum class RotateMode {
        tangent, chain, chainScale;


        companion object {

            val values = RotateMode.values()
        }
    }
}
