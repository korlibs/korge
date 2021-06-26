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

import com.esotericsoftware.spine.PathConstraintData.PositionMode
import com.esotericsoftware.spine.PathConstraintData.RotateMode
import com.esotericsoftware.spine.PathConstraintData.SpacingMode
import com.esotericsoftware.spine.attachments.PathAttachment
import com.esotericsoftware.spine.utils.*
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*

/** Stores the current pose for a path constraint. A path constraint adjusts the rotation, translation, and scale of the
 * constrained bones so they follow a [PathAttachment].
 *
 *
 * See [Path constraints](http://esotericsoftware.com/spine-path-constraints) in the Spine User Guide.  */
class PathConstraint : Updatable {

    /** The path constraint's setup pose data.  */
    val data: PathConstraintData

    /** The bones that will be modified by this path constraint.  */
    val bones: FastArrayList<Bone>

    lateinit internal var target: Slot

    /** The position along the path.  */
    var position: Float = 0.toFloat()

    /** The spacing between bones.  */
    var spacing: Float = 0.toFloat()

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained rotations.  */
    var rotateMix: Float = 0.toFloat()

    /** A percentage (0-1) that controls the mix between the constrained and unconstrained translations.  */
    var translateMix: Float = 0.toFloat()

    override var isActive: Boolean = false
        internal set

    private val spaces = FloatArrayList()
    private val positions = FloatArrayList()
    private val world = FloatArrayList()
    private val curves = FloatArrayList()
    private val lengths = FloatArrayList()
    private val segments = FloatArray(10)

    constructor(data: PathConstraintData, skeleton: Skeleton) {
        this.data = data
        bones = FastArrayList(data.bones.size)
        data.bones.fastForEach { boneData ->
            bones.add(skeleton.findBone(boneData.name)!!)
        }
        target = skeleton.findSlot(data.target.name)!!
        position = data.position
        spacing = data.spacing
        rotateMix = data.rotateMix
        translateMix = data.translateMix
    }

    /** Copy constructor.  */
    constructor(constraint: PathConstraint, skeleton: Skeleton) {
        data = constraint.data
        bones = FastArrayList(constraint.bones.size)
        constraint.bones.fastForEach { bone ->
            bones.add(skeleton.bones[bone.data.index])
        }
        target = skeleton.slots[constraint.target!!.data.index]
        position = constraint.position
        spacing = constraint.spacing
        rotateMix = constraint.rotateMix
        translateMix = constraint.translateMix
    }

    /** Applies the constraint to the constrained bones.  */
    fun apply() {
        update()
    }

    override fun update() {
        val attachment = target!!.attachment
        if (attachment !is PathAttachment) return

        val rotateMix = this.rotateMix
        val translateMix = this.translateMix
        val translate = translateMix > 0
        val rotate = rotateMix > 0
        if (!translate && !rotate) return

        val data = this.data
        val percentSpacing = data.spacingMode == SpacingMode.percent
        val rotateMode = data.rotateMode
        val tangents = rotateMode == RotateMode.tangent
        val scale = rotateMode == RotateMode.chainScale
        val boneCount = this.bones.size
        val spacesCount = if (tangents) boneCount else boneCount + 1
        val bones = this.bones
        val spaces = this.spaces.setSize(spacesCount)
        var lengths: FloatArray? = null
        val spacing = this.spacing
        if (scale || !percentSpacing) {
            if (scale) lengths = this.lengths.setSize(boneCount)
            val lengthSpacing = data.spacingMode == SpacingMode.length
            var i = 0
            val n = spacesCount - 1
            while (i < n) {
                val bone = bones[i]
                val setupLength = bone.data.length
                if (setupLength < epsilon) {
                    if (scale) lengths!![i] = 0f
                    spaces[++i] = 0f
                } else if (percentSpacing) {
                    if (scale) {
                        val x = setupLength * bone.a
                        val y = setupLength * bone.c
                        val length = kotlin.math.sqrt((x * x + y * y).toDouble()).toFloat()
                        lengths!![i] = length
                    }
                    spaces[++i] = spacing
                } else {
                    val x = setupLength * bone.a
                    val y = setupLength * bone.c
                    val length = kotlin.math.sqrt((x * x + y * y).toDouble()).toFloat()
                    if (scale) lengths!![i] = length
                    spaces[++i] = (if (lengthSpacing) setupLength + spacing else spacing) * length / setupLength
                }
            }
        } else {
            for (i in 1 until spacesCount)
                spaces[i] = spacing
        }

        val positions = computeWorldPositions(attachment, spacesCount, tangents,
                data.positionMode == PositionMode.percent, percentSpacing)
        var boneX = positions[0]
        var boneY = positions[1]
        var offsetRotation = data.offsetRotation
        val tip: Boolean
        if (offsetRotation == 0f)
            tip = rotateMode == RotateMode.chain
        else {
            tip = false
            val p = target!!.bone
            offsetRotation *= if (p.a * p.d - p.b * p.c > 0) SpineUtils.degRad else -SpineUtils.degRad
        }
        var i = 0
        var p = 3
        while (i < boneCount) {
            val bone = bones[i]
            bone.worldX += (boneX - bone.worldX) * translateMix
            bone.worldY += (boneY - bone.worldY) * translateMix
            val x = positions[p]
            val y = positions[p + 1]
            val dx = x - boneX
            val dy = y - boneY
            if (scale) {
                val length = lengths!![i]
                if (length >= epsilon) {
                    val s = (kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat() / length - 1) * rotateMix + 1
                    bone.a *= s
                    bone.c *= s
                }
            }
            boneX = x
            boneY = y
            if (rotate) {
                val a = bone.a
                val b = bone.b
                val c = bone.c
                val d = bone.d
                var r: Float
                var cos: Float
                var sin: Float
                if (tangents)
                    r = positions[p - 1]
                else if (spaces[i + 1] < epsilon)
                    r = positions[p + 2]
                else
                    r = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
                r -= kotlin.math.atan2(c.toDouble(), a.toDouble()).toFloat()
                if (tip) {
                    cos = kotlin.math.cos(r.toDouble()).toFloat()
                    sin = kotlin.math.sin(r.toDouble()).toFloat()
                    val length = bone.data.length
                    boneX += (length * (cos * a - sin * c) - dx) * rotateMix
                    boneY += (length * (sin * a + cos * c) - dy) * rotateMix
                } else
                    r += offsetRotation
                if (r > SpineUtils.PI)
                    r -= SpineUtils.PI2
                else if (r < -SpineUtils.PI)
                //
                    r += SpineUtils.PI2
                r *= rotateMix
                cos = kotlin.math.cos(r.toDouble()).toFloat()
                sin = kotlin.math.sin(r.toDouble()).toFloat()
                bone.a = cos * a - sin * c
                bone.b = cos * b - sin * d
                bone.c = sin * a + cos * c
                bone.d = sin * b + cos * d
            }
            bone.appliedValid = false
            i++
            p += 3
        }
    }

    internal fun computeWorldPositions(path: PathAttachment, spacesCount: Int, tangents: Boolean, percentPosition: Boolean,
                                       percentSpacing: Boolean): FloatArray {
        val target = this.target
        var position = this.position
        val spaces = this.spaces.data
        val out = this.positions.setSize(spacesCount * 3 + 2)
        val world: FloatArray
        val closed = path.closed
        var verticesLength = path.worldVerticesLength
        var curveCount = verticesLength / 6
        var prevCurve = NONE

        if (!path.constantSpeed) {
            val lengths = path.lengths
            curveCount -= if (closed) 1 else 2
            val pathLength = lengths[curveCount]
            if (percentPosition) position *= pathLength
            if (percentSpacing) {
                for (i in 1 until spacesCount)
                    spaces[i] *= pathLength
            }
            world = this.world.setSize(8)
            var i = 0
            var o = 0
            var curve = 0
            while (i < spacesCount) {
                val space = spaces[i]
                position += space
                var p = position

                if (closed) {
                    p %= pathLength
                    if (p < 0) p += pathLength
                    curve = 0
                } else if (p < 0) {
                    if (prevCurve != BEFORE) {
                        prevCurve = BEFORE
                        path.computeWorldVertices(target!!, 2, 4, world, 0, 2)
                    }
                    addBeforePosition(p, world, 0, out, o)
                    i++
                    o += 3
                    continue
                } else if (p > pathLength) {
                    if (prevCurve != AFTER) {
                        prevCurve = AFTER
                        path.computeWorldVertices(target!!, verticesLength - 6, 4, world, 0, 2)
                    }
                    addAfterPosition(p - pathLength, world, 0, out, o)
                    continue
                }

                // Determine curve containing position.
                while (true) {
                    val length = lengths[curve]
                    if (p > length) {
                        curve++
                        continue
                    }
                    if (curve == 0)
                        p /= length
                    else {
                        val prev = lengths[curve - 1]
                        p = (p - prev) / (length - prev)
                    }
                    break
                    curve++
                }
                if (curve != prevCurve) {
                    prevCurve = curve
                    if (closed && curve == curveCount) {
                        path.computeWorldVertices(target!!, verticesLength - 4, 4, world, 0, 2)
                        path.computeWorldVertices(target, 0, 4, world, 4, 2)
                    } else
                        path.computeWorldVertices(target!!, curve * 6 + 2, 8, world, 0, 2)
                }
                addCurvePosition(p, world[0], world[1], world[2], world[3], world[4], world[5], world[6], world[7], out, o,
                        tangents || i > 0 && space < epsilon)
                i++
                o += 3
            }
            return out
        }

        // World vertices.
        if (closed) {
            verticesLength += 2
            world = this.world.setSize(verticesLength)
            path.computeWorldVertices(target!!, 2, verticesLength - 4, world, 0, 2)
            path.computeWorldVertices(target, 0, 2, world, verticesLength - 4, 2)
            world[verticesLength - 2] = world[0]
            world[verticesLength - 1] = world[1]
        } else {
            curveCount--
            verticesLength -= 4
            world = this.world.setSize(verticesLength)
            path.computeWorldVertices(target!!, 2, verticesLength, world, 0, 2)
        }

        // Curve lengths.
        val curves = this.curves.setSize(curveCount)
        var pathLength = 0f
        var x1 = world[0]
        var y1 = world[1]
        var cx1 = 0f
        var cy1 = 0f
        var cx2 = 0f
        var cy2 = 0f
        var x2 = 0f
        var y2 = 0f
        var tmpx: Float
        var tmpy: Float
        var dddfx: Float
        var dddfy: Float
        var ddfx: Float
        var ddfy: Float
        var dfx: Float
        var dfy: Float
        run {
            var i = 0
            var w = 2
            while (i < curveCount) {
                cx1 = world[w]
                cy1 = world[w + 1]
                cx2 = world[w + 2]
                cy2 = world[w + 3]
                x2 = world[w + 4]
                y2 = world[w + 5]
                tmpx = (x1 - cx1 * 2 + cx2) * 0.1875f
                tmpy = (y1 - cy1 * 2 + cy2) * 0.1875f
                dddfx = ((cx1 - cx2) * 3 - x1 + x2) * 0.09375f
                dddfy = ((cy1 - cy2) * 3 - y1 + y2) * 0.09375f
                ddfx = tmpx * 2 + dddfx
                ddfy = tmpy * 2 + dddfy
                dfx = (cx1 - x1) * 0.75f + tmpx + dddfx * 0.16666667f
                dfy = (cy1 - y1) * 0.75f + tmpy + dddfy * 0.16666667f
                pathLength += kotlin.math.sqrt((dfx * dfx + dfy * dfy).toDouble()).toFloat()
                dfx += ddfx
                dfy += ddfy
                ddfx += dddfx
                ddfy += dddfy
                pathLength += kotlin.math.sqrt((dfx * dfx + dfy * dfy).toDouble()).toFloat()
                dfx += ddfx
                dfy += ddfy
                pathLength += kotlin.math.sqrt((dfx * dfx + dfy * dfy).toDouble()).toFloat()
                dfx += ddfx + dddfx
                dfy += ddfy + dddfy
                pathLength += kotlin.math.sqrt((dfx * dfx + dfy * dfy).toDouble()).toFloat()
                curves[i] = pathLength
                x1 = x2
                y1 = y2
                i++
                w += 6
            }
        }
        if (percentPosition)
            position *= pathLength
        else
            position *= pathLength / path.lengths[curveCount - 1]
        if (percentSpacing) {
            for (i in 1 until spacesCount)
                spaces[i] *= pathLength
        }

        val segments = this.segments
        var curveLength = 0f
        var i = 0
        var o = 0
        var curve = 0
        var segment = 0
        while (i < spacesCount) {
            val space = spaces[i]
            position += space
            var p = position

            if (closed) {
                p %= pathLength
                if (p < 0) p += pathLength
                curve = 0
            } else if (p < 0) {
                addBeforePosition(p, world, 0, out, o)
                i++
                o += 3
                continue
            } else if (p > pathLength) {
                addAfterPosition(p - pathLength, world, verticesLength - 4, out, o)
                continue
            }

            // Determine curve containing position.
            while (true) {
                val length = curves[curve]
                if (p > length) {
                    curve++
                    continue
                }
                if (curve == 0)
                    p /= length
                else {
                    val prev = curves[curve - 1]
                    p = (p - prev) / (length - prev)
                }
                break
                curve++
            }

            // Curve segment lengths.
            if (curve != prevCurve) {
                prevCurve = curve
                var ii = curve * 6
                x1 = world[ii]
                y1 = world[ii + 1]
                cx1 = world[ii + 2]
                cy1 = world[ii + 3]
                cx2 = world[ii + 4]
                cy2 = world[ii + 5]
                x2 = world[ii + 6]
                y2 = world[ii + 7]
                tmpx = (x1 - cx1 * 2 + cx2) * 0.03f
                tmpy = (y1 - cy1 * 2 + cy2) * 0.03f
                dddfx = ((cx1 - cx2) * 3 - x1 + x2) * 0.006f
                dddfy = ((cy1 - cy2) * 3 - y1 + y2) * 0.006f
                ddfx = tmpx * 2 + dddfx
                ddfy = tmpy * 2 + dddfy
                dfx = (cx1 - x1) * 0.3f + tmpx + dddfx * 0.16666667f
                dfy = (cy1 - y1) * 0.3f + tmpy + dddfy * 0.16666667f
                curveLength = kotlin.math.sqrt((dfx * dfx + dfy * dfy).toDouble()).toFloat()
                segments[0] = curveLength
                ii = 1
                while (ii < 8) {
                    dfx += ddfx
                    dfy += ddfy
                    ddfx += dddfx
                    ddfy += dddfy
                    curveLength += kotlin.math.sqrt((dfx * dfx + dfy * dfy).toDouble()).toFloat()
                    segments[ii] = curveLength
                    ii++
                }
                dfx += ddfx
                dfy += ddfy
                curveLength += kotlin.math.sqrt((dfx * dfx + dfy * dfy).toDouble()).toFloat()
                segments[8] = curveLength
                dfx += ddfx + dddfx
                dfy += ddfy + dddfy
                curveLength += kotlin.math.sqrt((dfx * dfx + dfy * dfy).toDouble()).toFloat()
                segments[9] = curveLength
                segment = 0
            }

            // Weight by segment length.
            p *= curveLength
            while (true) {
                val length = segments[segment]
                if (p > length) {
                    segment++
                    continue
                }
                if (segment == 0)
                    p /= length
                else {
                    val prev = segments[segment - 1]
                    p = segment + (p - prev) / (length - prev)
                }
                break
                segment++
            }
            addCurvePosition(p * 0.1f, x1, y1, cx1, cy1, cx2, cy2, x2, y2, out, o, tangents || i > 0 && space < epsilon)
            i++
            o += 3
        }
        return out
    }

    private fun addBeforePosition(p: Float, temp: FloatArray, i: Int, out: FloatArray, o: Int) {
        val x1 = temp[i]
        val y1 = temp[i + 1]
        val dx = temp[i + 2] - x1
        val dy = temp[i + 3] - y1
        val r = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
        out[o] = x1 + p * kotlin.math.cos(r.toDouble()).toFloat()
        out[o + 1] = y1 + p * kotlin.math.sin(r.toDouble()).toFloat()
        out[o + 2] = r
    }

    private fun addAfterPosition(p: Float, temp: FloatArray, i: Int, out: FloatArray, o: Int) {
        val x1 = temp[i + 2]
        val y1 = temp[i + 3]
        val dx = x1 - temp[i]
        val dy = y1 - temp[i + 1]
        val r = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
        out[o] = x1 + p * kotlin.math.cos(r.toDouble()).toFloat()
        out[o + 1] = y1 + p * kotlin.math.sin(r.toDouble()).toFloat()
        out[o + 2] = r
    }

    private fun addCurvePosition(p: Float, x1: Float, y1: Float, cx1: Float, cy1: Float, cx2: Float, cy2: Float, x2: Float, y2: Float,
                                 out: FloatArray, o: Int, tangents: Boolean) {
        if (p < epsilon || p.isNaN()) {
            out[o] = x1
            out[o + 1] = y1
            out[o + 2] = kotlin.math.atan2((cy1 - y1).toDouble(), (cx1 - x1).toDouble()).toFloat()
            return
        }
        val tt = p * p
        val ttt = tt * p
        val u = 1 - p
        val uu = u * u
        val uuu = uu * u
        val ut = u * p
        val ut3 = ut * 3
        val uut3 = u * ut3
        val utt3 = ut3 * p
        val x = x1 * uuu + cx1 * uut3 + cx2 * utt3 + x2 * ttt
        val y = y1 * uuu + cy1 * uut3 + cy2 * utt3 + y2 * ttt
        out[o] = x
        out[o + 1] = y
        if (tangents) {
            if (p < 0.001f)
                out[o + 2] = kotlin.math.atan2((cy1 - y1).toDouble(), (cx1 - x1).toDouble()).toFloat()
            else
                out[o + 2] = kotlin.math.atan2((y - (y1 * uu + cy1 * ut * 2f + cy2 * tt)).toDouble(), (x - (x1 * uu + cx1 * ut * 2f + cx2 * tt)).toDouble()).toFloat()
        }
    }

    /** The slot whose path attachment will be used to constrained the bones.  */
    fun getTarget(): Slot? {
        return target
    }

    fun setTarget(target: Slot) {
        this.target = target
    }

    override fun toString(): String {
        return data.name
    }

    companion object {
        private val NONE = -1
        private val BEFORE = -2
        private val AFTER = -3
        private val epsilon = 0.00001f
    }
}
