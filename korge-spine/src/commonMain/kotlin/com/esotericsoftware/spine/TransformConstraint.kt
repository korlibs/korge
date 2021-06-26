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

import com.esotericsoftware.spine.utils.SpineUtils.PI
import com.esotericsoftware.spine.utils.SpineUtils.PI2
import com.esotericsoftware.spine.utils.SpineUtils.atan2
import com.esotericsoftware.spine.utils.SpineUtils.cos
import com.esotericsoftware.spine.utils.SpineUtils.degRad
import com.esotericsoftware.spine.utils.SpineUtils.sin
import com.esotericsoftware.spine.utils.SpineVector2
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*

/** Stores the current pose for a transform constraint. A transform constraint adjusts the world transform of the constrained
 * bones to match that of the target bone.
 *
 *
 * See [Transform constraints](http://esotericsoftware.com/spine-transform-constraints) in the Spine User Guide.  */
class TransformConstraint : Updatable {
    /** The transform constraint's setup pose data.  */
    val data: TransformConstraintData

    /** The bones that will be modified by this transform constraint.  */
    val bones: FastArrayList<Bone>
    internal var target: Bone? = null

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained rotations.  */
    var rotateMix: Float = 0.toFloat()

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained translations.  */
    var translateMix: Float = 0.toFloat()

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained scales.  */
    var scaleMix: Float = 0.toFloat()

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained scales.  */
    var shearMix: Float = 0.toFloat()

    override var isActive: Boolean = false
        internal set
    internal val temp = SpineVector2()

    constructor(data: TransformConstraintData, skeleton: Skeleton) {
        this.data = data
        rotateMix = data.rotateMix
        translateMix = data.translateMix
        scaleMix = data.scaleMix
        shearMix = data.shearMix
        bones = FastArrayList(data.bones.size)
        data.bones.fastForEach { boneData ->
            bones.add(skeleton.findBone(boneData.name)!!)
        }
        target = skeleton.findBone(data.target.name)
    }

    /** Copy constructor.  */
    constructor(constraint: TransformConstraint, skeleton: Skeleton) {
        data = constraint.data
        bones = FastArrayList(constraint.bones.size)
        constraint.bones.fastForEach { bone ->
            bones.add(skeleton.bones[bone.data.index])
        }
        target = skeleton.bones[constraint.target!!.data.index]
        rotateMix = constraint.rotateMix
        translateMix = constraint.translateMix
        scaleMix = constraint.scaleMix
        shearMix = constraint.shearMix
    }

    /** Applies the constraint to the constrained bones.  */
    fun apply() {
        update()
    }

    override fun update() {
        if (data.local) {
            if (data.relative)
                applyRelativeLocal()
            else
                applyAbsoluteLocal()
        } else {
            if (data.relative)
                applyRelativeWorld()
            else
                applyAbsoluteWorld()
        }
    }

    private fun applyAbsoluteWorld() {
        val rotateMix = this.rotateMix
        val translateMix = this.translateMix
        val scaleMix = this.scaleMix
        val shearMix = this.shearMix
        val target = this.target
        val ta = target!!.a
        val tb = target.b
        val tc = target.c
        val td = target.d
        val degRadReflect = if (ta * td - tb * tc > 0) degRad else -degRad
        val offsetRotation = data.offsetRotation * degRadReflect
        val offsetShearY = data.offsetShearY * degRadReflect
        val bones = this.bones
        var i = 0
        val n = bones.size
        while (i < n) {
            val bone = bones[i]
            var modified = false

            if (rotateMix != 0f) {
                val a = bone.a
                val b = bone.b
                val c = bone.c
                val d = bone.d
                var r = atan2(tc, ta) - atan2(c, a) + offsetRotation
                if (r > PI)
                    r -= PI2
                else if (r < -PI) r += PI2
                r *= rotateMix
                val cos = cos(r)
                val sin = sin(r)
                bone.a = cos * a - sin * c
                bone.b = cos * b - sin * d
                bone.c = sin * a + cos * c
                bone.d = sin * b + cos * d
                modified = true
            }

            if (translateMix != 0f) {
                val temp = this.temp
                target.localToWorld(temp.set(data.offsetX, data.offsetY))
                bone.worldX += (temp.x - bone.worldX) * translateMix
                bone.worldY += (temp.y - bone.worldY) * translateMix
                modified = true
            }

            if (scaleMix > 0) {
                var s = kotlin.math.sqrt((bone.a * bone.a + bone.c * bone.c).toDouble()).toFloat()
                if (s != 0f) s = (s + (kotlin.math.sqrt((ta * ta + tc * tc).toDouble()).toFloat() - s + data.offsetScaleX) * scaleMix) / s
                bone.a *= s
                bone.c *= s
                s = kotlin.math.sqrt((bone.b * bone.b + bone.d * bone.d).toDouble()).toFloat()
                if (s != 0f) s = (s + (kotlin.math.sqrt((tb * tb + td * td).toDouble()).toFloat() - s + data.offsetScaleY) * scaleMix) / s
                bone.b *= s
                bone.d *= s
                modified = true
            }

            if (shearMix > 0) {
                val b = bone.b
                val d = bone.d
                val by = atan2(d, b)
                var r = atan2(td, tb) - atan2(tc, ta) - (by - atan2(bone.c, bone.a))
                if (r > PI)
                    r -= PI2
                else if (r < -PI) r += PI2
                r = by + (r + offsetShearY) * shearMix
                val s = kotlin.math.sqrt((b * b + d * d).toDouble()).toFloat()
                bone.b = cos(r) * s
                bone.d = sin(r) * s
                modified = true
            }

            if (modified) bone.appliedValid = false
            i++
        }
    }

    private fun applyRelativeWorld() {
        val rotateMix = this.rotateMix
        val translateMix = this.translateMix
        val scaleMix = this.scaleMix
        val shearMix = this.shearMix
        val target = this.target
        val ta = target!!.a
        val tb = target.b
        val tc = target.c
        val td = target.d
        val degRadReflect = if (ta * td - tb * tc > 0) degRad else -degRad
        val offsetRotation = data.offsetRotation * degRadReflect
        val offsetShearY = data.offsetShearY * degRadReflect
        val bones = this.bones
        var i = 0
        val n = bones.size
        while (i < n) {
            val bone = bones[i]
            var modified = false

            if (rotateMix != 0f) {
                val a = bone.a
                val b = bone.b
                val c = bone.c
                val d = bone.d
                var r = atan2(tc, ta) + offsetRotation
                if (r > PI)
                    r -= PI2
                else if (r < -PI) r += PI2
                r *= rotateMix
                val cos = cos(r)
                val sin = sin(r)
                bone.a = cos * a - sin * c
                bone.b = cos * b - sin * d
                bone.c = sin * a + cos * c
                bone.d = sin * b + cos * d
                modified = true
            }

            if (translateMix != 0f) {
                val temp = this.temp
                target.localToWorld(temp.set(data.offsetX, data.offsetY))
                bone.worldX += temp.x * translateMix
                bone.worldY += temp.y * translateMix
                modified = true
            }

            if (scaleMix > 0) {
                var s = (kotlin.math.sqrt((ta * ta + tc * tc).toDouble()).toFloat() - 1 + data.offsetScaleX) * scaleMix + 1
                bone.a *= s
                bone.c *= s
                s = (kotlin.math.sqrt((tb * tb + td * td).toDouble()).toFloat() - 1 + data.offsetScaleY) * scaleMix + 1
                bone.b *= s
                bone.d *= s
                modified = true
            }

            if (shearMix > 0) {
                var r = atan2(td, tb) - atan2(tc, ta)
                if (r > PI)
                    r -= PI2
                else if (r < -PI) r += PI2
                val b = bone.b
                val d = bone.d
                r = atan2(d, b) + (r - PI / 2 + offsetShearY) * shearMix
                val s = kotlin.math.sqrt((b * b + d * d).toDouble()).toFloat()
                bone.b = cos(r) * s
                bone.d = sin(r) * s
                modified = true
            }

            if (modified) bone.appliedValid = false
            i++
        }
    }

    private fun applyAbsoluteLocal() {
        val rotateMix = this.rotateMix
        val translateMix = this.translateMix
        val scaleMix = this.scaleMix
        val shearMix = this.shearMix
        val target = this.target
        if (!target!!.appliedValid) target.updateAppliedTransform()
        val bones = this.bones
        var i = 0
        val n = bones.size
        while (i < n) {
            val bone = bones[i]
            if (!bone.appliedValid) bone.updateAppliedTransform()

            var rotation = bone.arotation
            if (rotateMix != 0f) {
                var r = target.arotation - rotation + data.offsetRotation
                r -= ((16384 - (16384.499999999996 - r / 360).toInt()) * 360).toFloat()
                rotation += r * rotateMix
            }

            var x = bone.ax
            var y = bone.ay
            if (translateMix != 0f) {
                x += (target.ax - x + data.offsetX) * translateMix
                y += (target.ay - y + data.offsetY) * translateMix
            }

            var scaleX = bone.ascaleX
            var scaleY = bone.ascaleY
            if (scaleMix != 0f) {
                if (scaleX != 0f) scaleX = (scaleX + (target.ascaleX - scaleX + data.offsetScaleX) * scaleMix) / scaleX
                if (scaleY != 0f) scaleY = (scaleY + (target.ascaleY - scaleY + data.offsetScaleY) * scaleMix) / scaleY
            }

            var shearY = bone.ashearY
            if (shearMix != 0f) {
                var r = target.ashearY - shearY + data.offsetShearY
                r -= ((16384 - (16384.499999999996 - r / 360).toInt()) * 360).toFloat()
                shearY += r * shearMix
            }

            bone.updateWorldTransform(x, y, rotation, scaleX, scaleY, bone.ashearX, shearY)
            i++
        }
    }

    private fun applyRelativeLocal() {
        val rotateMix = this.rotateMix
        val translateMix = this.translateMix
        val scaleMix = this.scaleMix
        val shearMix = this.shearMix
        val target = this.target
        if (!target!!.appliedValid) target.updateAppliedTransform()
        val bones = this.bones
        var i = 0
        val n = bones.size
        while (i < n) {
            val bone = bones[i]
            if (!bone.appliedValid) bone.updateAppliedTransform()

            var rotation = bone.arotation
            if (rotateMix != 0f) rotation += (target.arotation + data.offsetRotation) * rotateMix

            var x = bone.ax
            var y = bone.ay
            if (translateMix != 0f) {
                x += (target.ax + data.offsetX) * translateMix
                y += (target.ay + data.offsetY) * translateMix
            }

            var scaleX = bone.ascaleX
            var scaleY = bone.ascaleY
            if (scaleMix != 0f) {
                scaleX *= (target.ascaleX - 1 + data.offsetScaleX) * scaleMix + 1
                scaleY *= (target.ascaleY - 1 + data.offsetScaleY) * scaleMix + 1
            }

            var shearY = bone.ashearY
            if (shearMix != 0f) shearY += (target.ashearY + data.offsetShearY) * shearMix

            bone.updateWorldTransform(x, y, rotation, scaleX, scaleY, bone.ashearX, shearY)
            i++
        }
    }

    /** The target bone whose world transform will be copied to the constrained bones.  */
    fun getTarget(): Bone? {
        return target
    }

    fun setTarget(target: Bone) {
        this.target = target
    }

    override fun toString(): String {
        return data.name
    }
}
