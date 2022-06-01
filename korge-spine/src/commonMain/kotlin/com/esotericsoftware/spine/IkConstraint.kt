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
import com.esotericsoftware.spine.utils.SpineUtils.atan2
import com.esotericsoftware.spine.utils.SpineUtils.cos
import com.esotericsoftware.spine.utils.SpineUtils.radDeg
import com.esotericsoftware.spine.utils.SpineUtils.sin
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*

/** Stores the current pose for an IK constraint. An IK constraint adjusts the rotation of 1 or 2 constrained bones so the tip of
 * the last bone is as close to the target bone as possible.
 *
 *
 * See [IK constraints](http://esotericsoftware.com/spine-ik-constraints) in the Spine User Guide.  */
class IkConstraint : Updatable {
    /** The IK constraint's setup pose data.  */

    val data: IkConstraintData

    /** The bones that will be modified by this IK constraint.  */

    val bones: FastArrayList<Bone>


    internal var target: Bone? = null

    /** Controls the bend direction of the IK bones, either 1 or -1.  */

    var bendDirection: Int = 0

    /** When true and only a single bone is being constrained, if the target is too close, the bone is scaled to reach it.  */

    var compress: Boolean = false

    /** When true, if the target is out of range, the parent bone is scaled to reach it. If more than one bone is being constrained
     * and the parent bone has local nonuniform scale, stretch is not applied.  */

    var stretch: Boolean = false

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained rotations.  */

    var mix = 1f

    /** For two bone IK, the distance from the maximum reach of the bones that rotation will slow.  */

    var softness: Float = 0.toFloat()


    internal var active: Boolean = false

    constructor(data: IkConstraintData, skeleton: Skeleton) {
        this.data = data
        mix = data.mix
        softness = data.softness
        bendDirection = data.bendDirection
        compress = data.compress
        stretch = data.stretch

        bones = FastArrayList(data.bones.size)
        data.bones.fastForEach { boneData ->
            val bone = skeleton.findBone(boneData.name)
            bones.add(bone!!)
        }
        target = skeleton.findBone(data.target.name)
    }

    /** Copy constructor.  */
    constructor(constraint: IkConstraint, skeleton: Skeleton) {
        data = constraint.data
        bones = FastArrayList(constraint.bones.size)
        constraint.bones.fastForEach { bone ->
            bones.add(skeleton.bones[bone!!.data.index])
        }
        target = skeleton.bones[constraint.target!!.data.index]
        mix = constraint.mix
        softness = constraint.softness
        bendDirection = constraint.bendDirection
        compress = constraint.compress
        stretch = constraint.stretch
    }

    /** Applies the constraint to the constrained bones.  */
    fun apply() {
        update()
    }

    override fun update() {
        val target = this.target
        val bones = this.bones
        when (bones.size) {
            1 -> apply(bones.first(), target!!.worldX, target.worldY, compress, stretch, data.uniform, mix)
            2 -> apply(bones.first(), bones[1], target!!.worldX, target.worldY, bendDirection, stretch, softness, mix)
        }
    }

    /** The bone that is the IK target.  */
    fun getTarget(): Bone? {
        return target
    }

    fun setTarget(target: Bone) {
        this.target = target
    }

    override val isActive: Boolean
        get() = active

    override fun toString(): String {
        return data.name ?: ""
    }

    companion object {

        /** Applies 1 bone IK. The target is specified in the world coordinate system.  */

        fun apply(bone: Bone, targetX: Float, targetY: Float, compress: Boolean, stretch: Boolean, uniform: Boolean, alpha: Float) {
            if (!bone.appliedValid) bone.updateAppliedTransform()
            val p = bone.parent!!
            val pa = p.a
            var pb = p.b
            val pc = p.c
            var pd = p.d
            var rotationIK = -bone.ashearX - bone.arotation
            var tx: Float
            var ty: Float
            when (bone.data.transformMode) {
                BoneData.TransformMode.onlyTranslation -> {
                    tx = targetX - bone.worldX
                    ty = targetY - bone.worldY
                }
                BoneData.TransformMode.noRotationOrReflection -> {
                    val s = kotlin.math.abs(pa * pd - pb * pc) / (pa * pa + pc * pc)
                    val sa = pa / bone.skeleton.scaleX
                    val sc = pc / bone.skeleton.scaleY
                    pb = -sc * s * bone.skeleton.scaleX
                    pd = sa * s * bone.skeleton.scaleY
                    rotationIK += atan2(sc, sa) * radDeg
                    val x = targetX - p.worldX
                    val y = targetY - p.worldY
                    val d = pa * pd - pb * pc
                    tx = (x * pd - y * pb) / d - bone.ax
                    ty = (y * pa - x * pc) / d - bone.ay
                }
                // Fall through.
                else -> {
                    val x = targetX - p.worldX
                    val y = targetY - p.worldY
                    val d = pa * pd - pb * pc
                    tx = (x * pd - y * pb) / d - bone.ax
                    ty = (y * pa - x * pc) / d - bone.ay
                }
            }
            rotationIK += atan2(ty, tx) * radDeg
            if (bone.ascaleX < 0) rotationIK += 180f
            if (rotationIK > 180)
                rotationIK -= 360f
            else if (rotationIK < -180)
            //
                rotationIK += 360f
            var sx = bone.ascaleX
            var sy = bone.ascaleY
            if (compress || stretch) {
                when (bone.data.transformMode) {
                    BoneData.TransformMode.noScale, BoneData.TransformMode.noScaleOrReflection -> {
                        tx = targetX - bone.worldX
                        ty = targetY - bone.worldY
                    }
                    else -> Unit
                }
                val b = bone.data.length * sx
                val dd = kotlin.math.sqrt((tx * tx + ty * ty).toDouble()).toFloat()
                if (compress && dd < b || stretch && dd > b && b > 0.0001f) {
                    val s = (dd / b - 1) * alpha + 1
                    sx *= s
                    if (uniform) sy *= s
                }
            }
            bone.updateWorldTransform(bone.ax, bone.ay, bone.arotation + rotationIK * alpha, sx, sy, bone.ashearX, bone.ashearY)
        }

        /** Applies 2 bone IK. The target is specified in the world coordinate system.
         * @param child A direct descendant of the parent bone.
         */

        fun apply(parent: Bone, child: Bone, targetX: Float, targetY: Float, bendDir: Int, stretch: Boolean, softness: Float, alpha: Float) {
            var softness = softness
            if (alpha == 0f) {
                child.updateWorldTransform()
                return
            }
            if (!parent.appliedValid) parent.updateAppliedTransform()
            if (!child.appliedValid) child.updateAppliedTransform()
            val px = parent.ax
            val py = parent.ay
            var psx = parent.ascaleX
            var sx = psx
            var psy = parent.ascaleY
            var csx = child.ascaleX
            val os1: Int
            val os2: Int
            var s2: Int
            if (psx < 0) {
                psx = -psx
                os1 = 180
                s2 = -1
            } else {
                os1 = 0
                s2 = 1
            }
            if (psy < 0) {
                psy = -psy
                s2 = -s2
            }
            if (csx < 0) {
                csx = -csx
                os2 = 180
            } else
                os2 = 0

            var cx = 0f
            var cy = 0f
            var cwx = 0f
            var cwy = 0f
            var a = 0f
            var b = 0f
            var c = 0f
            var d = 0f
            var u = false
            var id = 0f
            var x = 0f
            var y = 0f
            var dx = 0f
            var dy = 0f
            var l1 = 0f
            var l2 = 0f
            var a1 = 0f
            var a2 = 0f

            cx = child.ax
            a = parent.a
            b = parent.b
            c = parent.c
            d = parent.d;
            u = kotlin.math.abs(psx - psy) <= 0.0001f;
            if (!u) {
                cy = 0f;
                cwx = a * cx + parent.worldX;
                cwy = c * cx + parent.worldY;
            } else {
                cy = child.ay;
                cwx = a * cx + b * cy + parent.worldX;
                cwy = c * cx + d * cy + parent.worldY;
            }
            var pp = parent.parent!!
            a = pp.a;
            b = pp.b;
            c = pp.c;
            d = pp.d;
            id = 1 / (a * d - b * c)
            x = cwx - pp.worldX
            y = cwy - pp.worldY;
            dx = (x * d - y * b) * id - px
            dy = (y * a - x * c) * id - py
            l1 = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            l2 = child.data.length * csx
            if (l1 < 0.0001f) {
                apply(parent, targetX, targetY, false, stretch, false, alpha);
                child.updateWorldTransform(cx, cy, 0f, child.ascaleX, child.ascaleY, child.ashearX, child.ashearY);
                return;
            }
            x = targetX - pp.worldX;
            y = targetY - pp.worldY;
            var tx = (x * d - y * b) * id - px
            var ty = (y * a - x * c) * id - py;
            var dd = tx * tx + ty * ty;
            if (softness != 0f) {
                softness *= psx * (csx + 1) / 2;
                var td = kotlin.math.sqrt(dd.toDouble()).toFloat()
                var sd = td - l1 - l2 * psx + softness;
                if (sd > 0) {
                    var p = kotlin.math.min(1f, sd / (softness * 2)) - 1;
                    p = (sd - softness * (1 - p * p)) / td;
                    tx -= p * tx;
                    ty -= p * ty;
                    dd = tx * tx + ty * ty;
                }
            }
            outer@ while (true) {
                if (u) {
                    l2 *= psx;
                    var cos =(dd - l1 * l1 - l2 * l2) / (2 * l1 * l2);
                    if (cos < -1)
                        cos = -1f;
                    else if (cos > 1) {
                        cos = 1f;
                        if (stretch) sx *= (kotlin.math.sqrt(dd.toDouble()).toFloat() / (l1 + l2) - 1) * alpha + 1;
                    }
                    a2 = kotlin.math.acos(cos.toDouble()).toFloat() * bendDir;
                    a = l1 + l2 * cos;
                    b = l2 * sin(a2);
                    a1 = atan2(ty * a - tx * b, tx * a + ty * b);
                } else {
                    a = psx * l2;
                    b = psy * l2;
                    val aa = a * a
                    val bb = b * b
                    val ta = atan2(ty, tx);
                    c = bb * l1 * l1 + aa * dd - aa * bb;
                    val c1 = - 2 * bb * l1
                    val c2 = bb-aa;
                    d = c1 * c1 - 4 * c2 * c;
                    if (d >= 0) {
                        var q =kotlin.math.sqrt(d.toDouble()).toFloat();
                        if (c1 < 0) q = -q;
                        q = -(c1 + q) / 2;
                        val r0 = q / c2
                        val r1 = c / q;
                        val r = if (kotlin.math.abs(r0) < kotlin.math.abs(r1)) r0 else r1;
                        if (r * r <= dd) {
                            y = kotlin.math.sqrt((dd - r * r).toDouble()).toFloat() * bendDir;
                            a1 = ta - atan2(y, r);
                            a2 = atan2(y / psy, (r - l1) / psx);
                            break@outer;
                        }
                    }
                    var minAngle = PI
                    var minX = l1-a
                    var minDist = minX * minX
                    var minY = 0f;
                    var maxAngle = 0f
                    var maxX = l1+a
                    var maxDist = maxX * maxX
                    var maxY = 0f;
                    c = -a * l1 / (aa - bb);
                    if (c >= -1 && c <= 1) {
                        c = kotlin.math.acos(c.toDouble()).toFloat();
                        x = a * cos(c) + l1;
                        y = b * sin(c);
                        d = x * x + y * y;
                        if (d < minDist) {
                            minAngle = c;
                            minDist = d;
                            minX = x;
                            minY = y;
                        }
                        if (d > maxDist) {
                            maxAngle = c;
                            maxDist = d;
                            maxX = x;
                            maxY = y;
                        }
                    }
                    if (dd <= (minDist + maxDist) / 2) {
                        a1 = ta - atan2(minY * bendDir, minX);
                        a2 = minAngle * bendDir;
                    } else {
                        a1 = ta - atan2(maxY * bendDir, maxX);
                        a2 = maxAngle * bendDir;
                    }
                }
                break@outer
            }

            val os = atan2(cy, cx) * s2
            var rotation = parent.arotation
            a1 = (a1 - os) * radDeg + os1 - rotation
            if (a1 > 180)
                a1 -= 360f
            else if (a1 < -180) a1 += 360f
            parent.updateWorldTransform(px, py, rotation + a1 * alpha, sx, parent.ascaleY, 0f, 0f)
            rotation = child.arotation
            a2 = ((a2 + os) * radDeg - child.ashearX) * s2 + os2 - rotation
            if (a2 > 180)
                a2 -= 360f
            else if (a2 < -180) a2 += 360f
            child.updateWorldTransform(cx, cy, rotation + a2 * alpha, child.ascaleX, child.ascaleY, child.ashearX, child.ashearY)
        }
    }
}
