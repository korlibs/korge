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

import com.esotericsoftware.spine.BoneData.*
import com.esotericsoftware.spine.utils.*
import com.esotericsoftware.spine.utils.SpineUtils.PI
import com.esotericsoftware.spine.utils.SpineUtils.atan2
import com.esotericsoftware.spine.utils.SpineUtils.cos
import com.esotericsoftware.spine.utils.SpineUtils.cosDeg
import com.esotericsoftware.spine.utils.SpineUtils.radDeg
import com.esotericsoftware.spine.utils.SpineUtils.sin
import com.esotericsoftware.spine.utils.SpineUtils.sinDeg
import com.soywiz.kds.*
import com.soywiz.korma.geom.*

/** Stores a bone's current pose.
 *
 *
 * A bone has a local transform which is used to compute its world transform. A bone also has an applied transform, which is a
 * local transform that can be applied to compute the world transform. The local transform and applied transform may differ if a
 * constraint or application code modifies the world transform after it was computed from the local transform.  */
class Bone : Updatable {
    /** The bone's setup pose data.  */
    val data: BoneData

    /** The skeleton this bone belongs to.  */
    val skeleton: Skeleton

    /** The parent bone, or null if this is the root bone.  */
    val parent: Bone?

    /** The immediate children of this bone.  */
    val children: FastArrayList<Bone> = FastArrayList()
    // -- Local transform

    /** The local x translation.  */
    var x: Float = 0.toFloat()

    /** The local y translation.  */
    var y: Float = 0.toFloat()

    /** The local rotation in degrees, counter clockwise.  */
    var rotation: Float = 0.toFloat()

    /** The local scaleX.  */
    var scaleX: Float = 0.toFloat()

    /** The local scaleY.  */
    var scaleY: Float = 0.toFloat()

    /** The local shearX.  */
    var shearX: Float = 0.toFloat()

    /** The local shearY.  */
    var shearY: Float = 0.toFloat()
    // -- Applied transform

    /** The applied local x translation.  */
    var ax: Float = 0.toFloat()

    /** The applied local y translation.  */
    var ay: Float = 0.toFloat()

    /** The applied local rotation in degrees, counter clockwise.  */
    var arotation: Float = 0.toFloat()

    /** The applied local scaleX.  */
    var ascaleX: Float = 0.toFloat()

    /** The applied local scaleY.  */
    var ascaleY: Float = 0.toFloat()

    /** The applied local shearX.  */
    var ashearX: Float = 0.toFloat()

    /** The applied local shearY.  */
    var ashearY: Float = 0.toFloat()

    /** If true, the applied transform matches the world transform. If false, the world transform has been modified since it was
     * computed and [.updateAppliedTransform] must be called before accessing the applied transform.  */
    var appliedValid: Boolean = false
    // -- World transform

    /** Part of the world transform matrix for the X axis. If changed, [.setAppliedValid] should be set to false.  */
    var a: Float = 0.toFloat()

    /** Part of the world transform matrix for the Y axis. If changed, [.setAppliedValid] should be set to false.  */
    var b: Float = 0.toFloat()

    /** The world X position. If changed, [.setAppliedValid] should be set to false.  */
    var worldX: Float = 0.toFloat()

    /** Part of the world transform matrix for the X axis. If changed, [.setAppliedValid] should be set to false.  */
    var c: Float = 0.toFloat()

    /** Part of the world transform matrix for the Y axis. If changed, [.setAppliedValid] should be set to false.  */
    var d: Float = 0.toFloat()

    /** The world Y position. If changed, [.setAppliedValid] should be set to false.  */
    var worldY: Float = 0.toFloat()

    internal var sorted: Boolean = false

    /** Returns false when the bone has not been computed because [BoneData.getSkinRequired] is true and the
     * [active skin][Skeleton.getSkin] does not [contain][Skin.getBones] this bone.  */
    override var isActive: Boolean = false
        internal set

    /** The world rotation for the X axis, calculated using [.a] and [.c].  */
    val worldRotationX: Float
        get() = atan2(c, a) * radDeg

    /** The world rotation for the Y axis, calculated using [.b] and [.d].  */
    val worldRotationY: Float
        get() = atan2(d, b) * radDeg

    /** The magnitude (always positive) of the world scale X, calculated using [.a] and [.c].  */
    val worldScaleX: Float
        get() = kotlin.math.sqrt((a * a + c * c).toDouble()).toFloat()

    /** The magnitude (always positive) of the world scale Y, calculated using [.b] and [.d].  */
    val worldScaleY: Float
        get() = kotlin.math.sqrt((b * b + d * d).toDouble()).toFloat()

    /** @param parent May be null.
     */
    constructor(data: BoneData, skeleton: Skeleton, parent: Bone?) {
        this.data = data
        this.skeleton = skeleton
        this.parent = parent
        setToSetupPose()
    }

    /** Copy constructor. Does not copy the children bones.
     * @param parent May be null.
     */
    constructor(bone: Bone, skeleton: Skeleton, parent: Bone?) {
        this.skeleton = skeleton
        this.parent = parent
        data = bone.data
        x = bone.x
        y = bone.y
        rotation = bone.rotation
        scaleX = bone.scaleX
        scaleY = bone.scaleY
        shearX = bone.shearX
        shearY = bone.shearY
    }

    /** Same as [.updateWorldTransform]. This method exists for Bone to implement [Updatable].  */
    override fun update() {
        updateWorldTransform(x, y, rotation, scaleX, scaleY, shearX, shearY)
    }

    /** Computes the world transform using the parent bone and the specified local transform. Child bones are not updated.
     *
     *
     * See [World transforms](http://esotericsoftware.com/spine-runtime-skeletons#World-transforms) in the Spine
     * Runtimes Guide.  */

    fun updateWorldTransform(x: Float = this.x, y: Float = this.y, rotation: Float = this.rotation, scaleX: Float = this.scaleX, scaleY: Float = this.scaleY, shearX: Float = this.shearX, shearY: Float = this.shearY) {
        ax = x
        ay = y
        arotation = rotation
        ascaleX = scaleX
        ascaleY = scaleY
        ashearX = shearX
        ashearY = shearY
        appliedValid = true

        val parent = this.parent
        if (parent == null) { // Root bone.
            val skeleton = this.skeleton
            val rotationY = rotation + 90f + shearY
            val sx = skeleton.scaleX
            val sy = skeleton.scaleY
            a = cosDeg(rotation + shearX) * scaleX * sx
            b = cosDeg(rotationY) * scaleY * sx
            c = sinDeg(rotation + shearX) * scaleX * sy
            d = sinDeg(rotationY) * scaleY * sy
            worldX = x * sx + skeleton.x
            worldY = y * sy + skeleton.y
            return
        }

        var pa = parent.a
        var pb = parent.b
        var pc = parent.c
        var pd = parent.d
        worldX = pa * x + pb * y + parent.worldX
        worldY = pc * x + pd * y + parent.worldY

        when (data.transformMode) {
            TransformMode.normal -> {
                val rotationY = rotation + 90f + shearY
                val la = cosDeg(rotation + shearX) * scaleX
                val lb = cosDeg(rotationY) * scaleY
                val lc = sinDeg(rotation + shearX) * scaleX
                val ld = sinDeg(rotationY) * scaleY
                a = pa * la + pb * lc
                b = pa * lb + pb * ld
                c = pc * la + pd * lc
                d = pc * lb + pd * ld
                return
            }
            TransformMode.onlyTranslation -> {
                val rotationY = rotation + 90f + shearY
                a = cosDeg(rotation + shearX) * scaleX
                b = cosDeg(rotationY) * scaleY
                c = sinDeg(rotation + shearX) * scaleX
                d = sinDeg(rotationY) * scaleY
            }
            TransformMode.noRotationOrReflection -> {
                var s = pa * pa + pc * pc
                val prx: Float
                if (s > 0.0001f) {
                    s = kotlin.math.abs(pa * pd - pb * pc) / s
                    pa /= skeleton.scaleX
                    pc /= skeleton.scaleY
                    pb = pc * s
                    pd = pa * s
                    prx = atan2(pc, pa) * radDeg
                } else {
                    pa = 0f
                    pc = 0f
                    prx = 90 - atan2(pd, pb) * radDeg
                }
                val rx = rotation + shearX - prx
                val ry = rotation + shearY - prx + 90
                val la = cosDeg(rx) * scaleX
                val lb = cosDeg(ry) * scaleY
                val lc = sinDeg(rx) * scaleX
                val ld = sinDeg(ry) * scaleY
                a = pa * la - pb * lc
                b = pa * lb - pb * ld
                c = pc * la + pd * lc
                d = pc * lb + pd * ld
            }
            TransformMode.noScale, TransformMode.noScaleOrReflection -> {
                val cos = cosDeg(rotation)
                val sin = sinDeg(rotation)
                var za = (pa * cos + pb * sin) / skeleton.scaleX
                var zc = (pc * cos + pd * sin) / skeleton.scaleY
                var s = kotlin.math.sqrt((za * za + zc * zc).toDouble()).toFloat()
                if (s > 0.00001f) s = 1 / s
                za *= s
                zc *= s
                s = kotlin.math.sqrt((za * za + zc * zc).toDouble()).toFloat()
                if (data.transformMode == TransformMode.noScale && pa * pd - pb * pc < 0 != (skeleton.scaleX < 0 != skeleton.scaleY < 0))
                    s = -s
                val r = PI / 2 + atan2(zc, za)
                val zb = cos(r) * s
                val zd = sin(r) * s
                val la = cosDeg(shearX) * scaleX
                val lb = cosDeg(90 + shearY) * scaleY
                val lc = sinDeg(shearX) * scaleX
                val ld = sinDeg(90 + shearY) * scaleY
                a = za * la + zb * lc
                b = za * lb + zb * ld
                c = zc * la + zd * lc
                d = zc * lb + zd * ld
            }
        }
        a *= skeleton.scaleX
        b *= skeleton.scaleX
        c *= skeleton.scaleY
        d *= skeleton.scaleY
    }

    /** Sets this bone's local transform to the setup pose.  */
    fun setToSetupPose() {
        val data = this.data
        x = data.x
        y = data.y
        rotation = data.rotation
        scaleX = data.scaleX
        scaleY = data.scaleY
        shearX = data.shearX
        shearY = data.shearY
    }

    fun setPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun setScale(scaleX: Float, scaleY: Float) {
        this.scaleX = scaleX
        this.scaleY = scaleY
    }

    fun setScale(scale: Float) {
        scaleX = scale
        scaleY = scale
    }

    /** Computes the applied transform values from the world transform. This allows the applied transform to be accessed after the
     * world transform has been modified (by a constraint, [.rotateWorld], etc).
     *
     *
     * If [.updateWorldTransform] has been called for a bone and [.isAppliedValid] is false, then
     * [.updateAppliedTransform] must be called before accessing the applied transform.
     *
     *
     * Some information is ambiguous in the world transform, such as -1,-1 scale versus 180 rotation. The applied transform after
     * calling this method is equivalent to the local tranform used to compute the world transform, but may not be identical.  */
    fun updateAppliedTransform() {
        appliedValid = true
        val parent = this.parent
        if (parent == null) {
            ax = worldX
            ay = worldY
            arotation = atan2(c, a) * radDeg
            ascaleX = kotlin.math.sqrt((a * a + c * c).toDouble()).toFloat()
            ascaleY = kotlin.math.sqrt((b * b + d * d).toDouble()).toFloat()
            ashearX = 0f
            ashearY = atan2(a * b + c * d, a * d - b * c) * radDeg
            return
        }
        val pa = parent.a
        val pb = parent.b
        val pc = parent.c
        val pd = parent.d
        val pid = 1 / (pa * pd - pb * pc)
        val dx = worldX - parent.worldX
        val dy = worldY - parent.worldY
        ax = dx * pd * pid - dy * pb * pid
        ay = dy * pa * pid - dx * pc * pid
        val ia = pid * pd
        val id = pid * pa
        val ib = pid * pb
        val ic = pid * pc
        val ra = ia * a - ib * c
        val rb = ia * b - ib * d
        val rc = id * c - ic * a
        val rd = id * d - ic * b
        ashearX = 0f
        ascaleX = kotlin.math.sqrt((ra * ra + rc * rc).toDouble()).toFloat()
        if (ascaleX > 0.0001f) {
            val det = ra * rd - rb * rc
            ascaleY = det / ascaleX
            ashearY = atan2(ra * rb + rc * rd, det) * radDeg
            arotation = atan2(rc, ra) * radDeg
        } else {
            ascaleX = 0f
            ascaleY = kotlin.math.sqrt((rb * rb + rd * rd).toDouble()).toFloat()
            ashearY = 0f
            arotation = 90 - atan2(rd, rb) * radDeg
        }
    }

    fun getWorldTransform(worldTransform: Matrix3D): Matrix3D {
        // @TODO: Ensure this is right
        worldTransform.setRows3x3(
            a, b, worldX,
            c, d, worldY,
            0f, 0f, 1f
        )
        return worldTransform
    }

    /** Transforms a point from world coordinates to the bone's local coordinates.  */
    fun worldToLocal(world: SpineVector2): SpineVector2 {
        val invDet = 1 / (a * d - b * c)
        val x = world.x - worldX
        val y = world.y - worldY
        world.x = x * d * invDet - y * b * invDet
        world.y = y * a * invDet - x * c * invDet
        return world
    }

    /** Transforms a point from the bone's local coordinates to world coordinates.  */
    fun localToWorld(local: SpineVector2): SpineVector2 {
        val x = local.x
        val y = local.y
        local.x = x * a + y * b + worldX
        local.y = x * c + y * d + worldY
        return local
    }

    /** Transforms a world rotation to a local rotation.  */
    fun worldToLocalRotation(worldRotation: Float): Float {
        val sin = sinDeg(worldRotation)
        val cos = cosDeg(worldRotation)
        return atan2(a * sin - c * cos, d * cos - b * sin) * radDeg + rotation - shearX
    }

    /** Transforms a local rotation to a world rotation.  */
    fun localToWorldRotation(localRotation: Float): Float {
        var localRotation = localRotation
        localRotation -= rotation - shearX
        val sin = sinDeg(localRotation)
        val cos = cosDeg(localRotation)
        return atan2(cos * c + sin * d, cos * a + sin * b) * radDeg
    }

    /** Rotates the world transform the specified amount and sets [.isAppliedValid] to false.
     * [.updateWorldTransform] will need to be called on any child bones, recursively, and any constraints reapplied.  */
    fun rotateWorld(degrees: Float) {
        val cos = cosDeg(degrees)
        val sin = sinDeg(degrees)
        a = cos * a - sin * c
        b = cos * b - sin * d
        c = sin * a + cos * c
        d = sin * b + cos * d
        appliedValid = false
    }

    // ---

    override fun toString(): String {
        return data.name
    }
}
/** Computes the world transform using the parent bone and this bone's local transform.
 *
 *
 * See [.updateWorldTransform].  */
