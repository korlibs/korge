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

import com.esotericsoftware.spine.Animation.MixBlend.*
import com.esotericsoftware.spine.Animation.MixDirection.*
import com.esotericsoftware.spine.attachments.*
import com.esotericsoftware.spine.utils.*
import com.esotericsoftware.spine.utils.SpineUtils.arraycopy
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import kotlin.math.*

/** A simple container for a list of timelines and a name.  */
class Animation(
    /** The animation's name, which is unique across all animations in the skeleton.  */
    val name: String,
    timelines: FastArrayList<Timeline>,
    /** The duration of the animation in seconds, which is the highest time of all keys in the timeline.  */
    var duration: Float
) {
    lateinit internal var timelines: FastArrayList<Timeline>
    internal val timelineIDs = IntSet()

    init {
        setTimelines(timelines)
    }

    /** If the returned array or the timelines it contains are modified, [.setTimelines] must be called.  */
    fun getTimelines(): FastArrayList<Timeline> {
        return timelines
    }

    fun setTimelines(timelines: FastArrayList<Timeline>) {
        this.timelines = timelines

        timelineIDs.clear()
        timelines.fastForEach { timeline ->
            timelineIDs.add(timeline.propertyId)
        }
    }

    /** Return true if this animation contains a timeline with the specified property ID.  */
    fun hasTimeline(id: Int): Boolean {
        return timelineIDs.contains(id)
    }

    /** Applies all the animation's timelines to the specified skeleton.
     *
     *
     * See Timeline [Timeline.apply].
     * @param loop If true, the animation repeats after [.getDuration].
     * @param events May be null to ignore fired events.
     */
    fun apply(skeleton: Skeleton, lastTime: Float, time: Float, loop: Boolean, events: FastArrayList<Event>?, alpha: Float,
              blend: MixBlend, direction: MixDirection) {
        var lastTime = lastTime
        var time = time

        if (loop && duration != 0f) {
            time %= duration
            if (lastTime > 0) lastTime %= duration
        }

        val timelines = this.timelines
        var i = 0
        val n = timelines.size
        while (i < n) {
            timelines[i].apply(skeleton, lastTime, time, events, alpha, blend, direction)
            i++
        }
    }

    override fun toString(): String {
        return name ?: ""
    }

    /** The interface for all timelines.  */
    interface Timeline {

        /** Uniquely encodes both the type of this timeline and the skeleton property that it affects.  */
        val propertyId: Int

        /** Applies this timeline to the skeleton.
         * @param skeleton The skeleton the timeline is being applied to. This provides access to the bones, slots, and other
         * skeleton components the timeline may change.
         * @param lastTime The time this timeline was last applied. Timelines such as [EventTimeline] trigger only at specific
         * times rather than every frame. In that case, the timeline triggers everything between `lastTime`
         * (exclusive) and `time` (inclusive).
         * @param time The time within the animation. Most timelines find the key before and the key after this time so they can
         * interpolate between the keys.
         * @param events If any events are fired, they are added to this list. Can be null to ignore fired events or if the timeline
         * does not fire events.
         * @param alpha 0 applies the current or setup value (depending on `blend`). 1 applies the timeline value.
         * Between 0 and 1 applies a value between the current or setup value and the timeline value. By adjusting
         * `alpha` over time, an animation can be mixed in or out. `alpha` can also be useful to
         * apply animations on top of each other (layering).
         * @param blend Controls how mixing is applied when `alpha` < 1.
         * @param direction Indicates whether the timeline is mixing in or out. Used by timelines which perform instant transitions,
         * such as [DrawOrderTimeline] or [AttachmentTimeline], and other such as [ScaleTimeline].
         */
        fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                  direction: MixDirection)
    }

    /** Controls how a timeline value is mixed with the setup pose value or current pose value when a timeline's `alpha`
     * < 1.
     *
     *
     * See Timeline [Timeline.apply].  */
    enum class MixBlend {
        /** Transitions from the setup value to the timeline value (the current value is not used). Before the first key, the setup
         * value is set.  */
        setup,

        /** Transitions from the current value to the timeline value. Before the first key, transitions from the current value to
         * the setup value. Timelines which perform instant transitions, such as [DrawOrderTimeline] or
         * [AttachmentTimeline], use the setup value before the first key.
         *
         *
         * `first` is intended for the first animations applied, not for animations layered on top of those.  */
        first,

        /** Transitions from the current value to the timeline value. No change is made before the first key (the current value is
         * kept until the first key).
         *
         *
         * `replace` is intended for animations layered on top of others, not for the first animations applied.  */
        replace,

        /** Transitions from the current value to the current value plus the timeline value. No change is made before the first key
         * (the current value is kept until the first key).
         *
         *
         * `add` is intended for animations layered on top of others, not for the first animations applied. Properties
         * keyed by additive animations must be set manually or by another animation before applying the additive animations, else
         * the property values will increase continually.  */
        add
    }

    /** Indicates whether a timeline's `alpha` is mixing out over time toward 0 (the setup or current pose value) or
     * mixing in toward 1 (the timeline's value).
     *
     *
     * See Timeline [Timeline.apply].  */
    enum class MixDirection {
        `in`, out
    }

    private enum class TimelineType {
        rotate, translate, scale, shear, //
        attachment, color, deform, //
        event, drawOrder, //
        ikConstraint, transformConstraint, //
        pathConstraintPosition, pathConstraintSpacing, pathConstraintMix, //
        twoColor
    }

    /** An interface for timelines which change the property of a bone.  */
    interface BoneTimeline : Timeline {

        /** The index of the bone in [Skeleton.getBones] that will be changed.  */
        var boneIndex: Int
    }

    /** An interface for timelines which change the property of a slot.  */
    interface SlotTimeline : Timeline {

        /** The index of the slot in [Skeleton.getSlots] that will be changed.  */
        var slotIndex: Int
    }

    /** The base class for timelines that use interpolation between key frame values.  */
    abstract class CurveTimeline(frameCount: Int) : Timeline {

        private val curves: FloatArray // type, x, y, ...

        /** The number of key frames for this timeline.  */
        val frameCount: Int
            get() = curves.size / BEZIER_SIZE + 1

        init {
            require(frameCount > 0) { "frameCount must be > 0: $frameCount" }
            curves = FloatArray((frameCount - 1) * BEZIER_SIZE)
        }

        /** Sets the specified key frame to linear interpolation.  */
        fun setLinear(frameIndex: Int) {
            curves[frameIndex * BEZIER_SIZE] = LINEAR
        }

        /** Sets the specified key frame to stepped interpolation.  */
        fun setStepped(frameIndex: Int) {
            curves[frameIndex * BEZIER_SIZE] = STEPPED
        }

        /** Returns the interpolation type for the specified key frame.
         * @return Linear is 0, stepped is 1, Bezier is 2.
         */
        fun getCurveType(frameIndex: Int): Float {
            val index = frameIndex * BEZIER_SIZE
            if (index == curves.size) return LINEAR
            val type = curves[index]
            if (type == LINEAR) return LINEAR
            return if (type == STEPPED) STEPPED else BEZIER
        }

        /** Sets the specified key frame to Bezier interpolation. `cx1` and `cx2` are from 0 to 1,
         * representing the percent of time between the two key frames. `cy1` and `cy2` are the percent of the
         * difference between the key frame's values.  */
        fun setCurve(frameIndex: Int, cx1: Float, cy1: Float, cx2: Float, cy2: Float) {
            val tmpx = (-cx1 * 2 + cx2) * 0.03f
            val tmpy = (-cy1 * 2 + cy2) * 0.03f
            val dddfx = ((cx1 - cx2) * 3 + 1) * 0.006f
            val dddfy = ((cy1 - cy2) * 3 + 1) * 0.006f
            var ddfx = tmpx * 2 + dddfx
            var ddfy = tmpy * 2 + dddfy
            var dfx = cx1 * 0.3f + tmpx + dddfx * 0.16666667f
            var dfy = cy1 * 0.3f + tmpy + dddfy * 0.16666667f

            var i = frameIndex * BEZIER_SIZE
            val curves = this.curves
            curves[i++] = BEZIER

            var x = dfx
            var y = dfy
            val n = i + BEZIER_SIZE - 1
            while (i < n) {
                curves[i] = x
                curves[i + 1] = y
                dfx += ddfx
                dfy += ddfy
                ddfx += dddfx
                ddfy += dddfy
                x += dfx
                y += dfy
                i += 2
            }
        }

        /** Returns the interpolated percentage for the specified key frame and linear percentage.  */
        fun getCurvePercent(frameIndex: Int, percent: Float): Float {
            var percent = percent
            percent = percent.coerceIn(0f, 1f)
            val curves = this.curves
            var i = frameIndex * BEZIER_SIZE
            val type = curves[i]
            if (type == LINEAR) return percent
            if (type == STEPPED) return 0f
            i++
            var x = 0f
            val start = i
            val n = i + BEZIER_SIZE - 1
            while (i < n) {
                x = curves[i]
                if (x >= percent) {
                    if (i == start) return curves[i + 1] * percent / x // First point is 0,0.
                    val prevX = curves[i - 2]
                    val prevY = curves[i - 1]
                    return prevY + (curves[i + 1] - prevY) * (percent - prevX) / (x - prevX)
                }
                i += 2
            }
            val y = curves[i - 1]
            return y + (1 - y) * (percent - x) / (1 - x) // Last point is 1,1.
        }

        companion object {
            val LINEAR = 0f
            val STEPPED = 1f
            val BEZIER = 2f
            private val BEZIER_SIZE = 10 * 2 - 1
        }
    }

    /** Changes a bone's local [Bone.getRotation].  */
    class RotateTimeline(frameCount: Int) : CurveTimeline(frameCount), BoneTimeline {

        /** The index of the bone in [Skeleton.getBones] that will be changed.  */
        override var boneIndex: Int = 0
            set(index) {
                require(index >= 0) { "index must be >= 0." }
                field = index
            }

        /** The time in seconds and rotation in degrees for each key frame.  */
        val frames: FloatArray = FloatArray(frameCount shl 1) // time, degrees, ...

        override val propertyId: Int
            get() = (TimelineType.rotate.ordinal shl 24) + boneIndex

        /** Sets the time in seconds and the rotation in degrees for the specified key frame.  */
        fun setFrame(frameIndex: Int, time: Float, degrees: Float) {
            var frameIndex = frameIndex
            frameIndex = frameIndex shl 1
            frames[frameIndex] = time
            frames[frameIndex + ROTATION] = degrees
        }

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val bone = skeleton.bones[boneIndex]
            if (!bone.isActive) return
            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                when (blend) {
                    setup -> {
                        bone.rotation = bone.data.rotation
                        return
                    }
                    first -> {
                        val r = bone.data.rotation - bone.rotation
                        bone.rotation += (r - (16384 - (16384.499999999996 - r / 360).toInt()) * 360) * alpha
                    }
                    else -> Unit
                }
                return
            }

            if (time >= frames[frames.size - ENTRIES]) { // Time is after last frame.
                var r = frames[frames.size + PREV_ROTATION]
                when (blend) {
                    setup -> bone.rotation = bone.data.rotation + r * alpha
                    first, replace -> {
                        r += bone.data.rotation - bone.rotation
                        r -= ((16384 - (16384.499999999996 - r / 360).toInt()) * 360).toFloat()
                        bone.rotation += r * alpha
                    }
                    // Fall through.
                    add -> bone.rotation += r * alpha
                }
                return
            }

            // Interpolate between the previous frame and the current frame.
            val frame = binarySearch(frames, time, ENTRIES)
            val prevRotation = frames[frame + PREV_ROTATION]
            val frameTime = frames[frame]
            val percent = getCurvePercent((frame shr 1) - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime))

            var r = frames[frame + ROTATION] - prevRotation
            r = prevRotation + (r - (16384 - (16384.499999999996 - r / 360).toInt()) * 360) * percent
            when (blend) {
                setup -> bone.rotation = bone.data.rotation + (r - (16384 - (16384.499999999996 - r / 360).toInt()) * 360) * alpha
                first, replace -> {
                    r += bone.data.rotation - bone.rotation
                    bone.rotation += (r - (16384 - (16384.499999999996 - r / 360).toInt()) * 360) * alpha
                }
                // Fall through.
                add -> bone.rotation += (r - (16384 - (16384.499999999996 - r / 360).toInt()) * 360) * alpha
            }
        }

        companion object {
            val ENTRIES = 2
            internal val PREV_TIME = -2
            internal val PREV_ROTATION = -1
            internal val ROTATION = 1
        }
    }

    /** Changes a bone's local [Bone.getX] and [Bone.getY].  */
    open class TranslateTimeline(frameCount: Int) : CurveTimeline(frameCount), BoneTimeline {

        /** The index of the bone in [Skeleton.getBones] that will be changed.  */
        override var boneIndex: Int = 0
            set(index) {
                require(index >= 0) { "index must be >= 0." }
                field = index
            }

        /** The time in seconds, x, and y values for each key frame.  */
        val frames: FloatArray // time, x, y, ...

        override val propertyId: Int
            get() = (TimelineType.translate.ordinal shl 24) + boneIndex

        init {
            frames = FloatArray(frameCount * ENTRIES)
        }

        /** Sets the time in seconds, x, and y values for the specified key frame.  */
        fun setFrame(frameIndex: Int, time: Float, x: Float, y: Float) {
            var frameIndex = frameIndex
            frameIndex *= ENTRIES
            frames[frameIndex] = time
            frames[frameIndex + X] = x
            frames[frameIndex + Y] = y
        }

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val bone = skeleton.bones[boneIndex]
            if (!bone.isActive) return
            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                when (blend) {
                    setup -> {
                        bone.x = bone.data.x
                        bone.y = bone.data.y
                        return
                    }
                    first -> {
                        bone.x += (bone.data.x - bone.x) * alpha
                        bone.y += (bone.data.y - bone.y) * alpha
                    }
                    else -> Unit
                }
                return
            }

            var x: Float
            var y: Float
            if (time >= frames[frames.size - ENTRIES]) { // Time is after last frame.
                x = frames[frames.size + PREV_X]
                y = frames[frames.size + PREV_Y]
            } else {
                // Interpolate between the previous frame and the current frame.
                val frame = binarySearch(frames, time, ENTRIES)
                x = frames[frame + PREV_X]
                y = frames[frame + PREV_Y]
                val frameTime = frames[frame]
                val percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime))

                x += (frames[frame + X] - x) * percent
                y += (frames[frame + Y] - y) * percent
            }
            when (blend) {
                setup -> {
                    bone.x = bone.data.x + x * alpha
                    bone.y = bone.data.y + y * alpha
                }
                first, replace -> {
                    bone.x += (bone.data.x + x - bone.x) * alpha
                    bone.y += (bone.data.y + y - bone.y) * alpha
                }
                add -> {
                    bone.x += x * alpha
                    bone.y += y * alpha
                }
            }
        }

        companion object {
            val ENTRIES = 3
            internal val PREV_TIME = -3
            internal val PREV_X = -2
            internal val PREV_Y = -1
            internal val X = 1
            internal val Y = 2
        }
    }

    /** Changes a bone's local [Bone.getScaleX] and [Bone.getScaleY].  */
    class ScaleTimeline(frameCount: Int) : TranslateTimeline(frameCount) {

        override val propertyId: Int
            get() = (TimelineType.scale.ordinal shl 24) + boneIndex

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val bone = skeleton.bones[boneIndex]
            if (!bone.isActive) return
            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                when (blend) {
                    setup -> {
                        bone.scaleX = bone.data.scaleX
                        bone.scaleY = bone.data.scaleY
                        return
                    }
                    first -> {
                        bone.scaleX += (bone.data.scaleX - bone.scaleX) * alpha
                        bone.scaleY += (bone.data.scaleY - bone.scaleY) * alpha
                    }
                    else -> Unit
                }
                return
            }

            var x: Float
            var y: Float
            if (time >= frames[frames.size - Animation.TranslateTimeline.ENTRIES]) { // Time is after last frame.
                x = frames[frames.size + Animation.TranslateTimeline.PREV_X] * bone.data.scaleX
                y = frames[frames.size + Animation.TranslateTimeline.PREV_Y] * bone.data.scaleY
            } else {
                // Interpolate between the previous frame and the current frame.
                val frame = binarySearch(frames, time, Animation.TranslateTimeline.ENTRIES)
                x = frames[frame + Animation.TranslateTimeline.PREV_X]
                y = frames[frame + Animation.TranslateTimeline.PREV_Y]
                val frameTime = frames[frame]
                val percent = getCurvePercent(frame / Animation.TranslateTimeline.ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + Animation.TranslateTimeline.PREV_TIME] - frameTime))

                x = (x + (frames[frame + Animation.TranslateTimeline.X] - x) * percent) * bone.data.scaleX
                y = (y + (frames[frame + Animation.TranslateTimeline.Y] - y) * percent) * bone.data.scaleY
            }
            if (alpha == 1f) {
                if (blend == add) {
                    bone.scaleX += x - bone.data.scaleX
                    bone.scaleY += y - bone.data.scaleY
                } else {
                    bone.scaleX = x
                    bone.scaleY = y
                }
            } else {
                // Mixing out uses sign of setup or current pose, else use sign of key.
                val bx: Float
                val by: Float
                if (direction == out) {
                    when (blend) {
                        setup -> {
                            bx = bone.data.scaleX
                            by = bone.data.scaleY
                            bone.scaleX = bx + (kotlin.math.abs(x) * (bx.sign) - bx) * alpha
                            bone.scaleY = by + (kotlin.math.abs(y) * (by.sign) - by) * alpha
                        }
                        first, replace -> {
                            bx = bone.scaleX
                            by = bone.scaleY
                            bone.scaleX = bx + (kotlin.math.abs(x) * (bx.sign) - bx) * alpha
                            bone.scaleY = by + (kotlin.math.abs(y) * (by.sign) - by) * alpha
                        }
                        add -> {
                            bx = bone.scaleX
                            by = bone.scaleY
                            bone.scaleX = bx + (kotlin.math.abs(x) * (bx.sign) - bone.data.scaleX) * alpha
                            bone.scaleY = by + (kotlin.math.abs(y) * (by.sign) - bone.data.scaleY) * alpha
                        }
                    }
                } else {
                    when (blend) {
                        setup -> {
                            bx = kotlin.math.abs(bone.data.scaleX) * (x.sign)
                            by = kotlin.math.abs(bone.data.scaleY) * (y.sign)
                            bone.scaleX = bx + (x - bx) * alpha
                            bone.scaleY = by + (y - by) * alpha
                        }
                        first, replace -> {
                            bx = kotlin.math.abs(bone.scaleX) * (x.sign)
                            by = kotlin.math.abs(bone.scaleY) * (y.sign)
                            bone.scaleX = bx + (x - bx) * alpha
                            bone.scaleY = by + (y - by) * alpha
                        }
                        add -> {
                            bx = (x.sign)
                            by = (y.sign)
                            bone.scaleX = kotlin.math.abs(bone.scaleX) * bx + (x - kotlin.math.abs(bone.data.scaleX) * bx) * alpha
                            bone.scaleY = kotlin.math.abs(bone.scaleY) * by + (y - kotlin.math.abs(bone.data.scaleY) * by) * alpha
                        }
                    }
                }
            }
        }
    }

    /** Changes a bone's local [Bone.getShearX] and [Bone.getShearY].  */
    class ShearTimeline(frameCount: Int) : TranslateTimeline(frameCount) {

        override val propertyId: Int
            get() = (TimelineType.shear.ordinal shl 24) + boneIndex

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val bone = skeleton.bones[boneIndex]
            if (!bone.isActive) return
            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                when (blend) {
                    setup -> {
                        bone.shearX = bone.data.shearX
                        bone.shearY = bone.data.shearY
                        return
                    }
                    first -> {
                        bone.shearX += (bone.data.shearX - bone.shearX) * alpha
                        bone.shearY += (bone.data.shearY - bone.shearY) * alpha
                    }
                    else -> Unit
                }
                return
            }

            var x: Float
            var y: Float
            if (time >= frames[frames.size - Animation.TranslateTimeline.ENTRIES]) { // Time is after last frame.
                x = frames[frames.size + Animation.TranslateTimeline.PREV_X]
                y = frames[frames.size + Animation.TranslateTimeline.PREV_Y]
            } else {
                // Interpolate between the previous frame and the current frame.
                val frame = binarySearch(frames, time, Animation.TranslateTimeline.ENTRIES)
                x = frames[frame + Animation.TranslateTimeline.PREV_X]
                y = frames[frame + Animation.TranslateTimeline.PREV_Y]
                val frameTime = frames[frame]
                val percent = getCurvePercent(frame / Animation.TranslateTimeline.ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + Animation.TranslateTimeline.PREV_TIME] - frameTime))

                x = x + (frames[frame + Animation.TranslateTimeline.X] - x) * percent
                y = y + (frames[frame + Animation.TranslateTimeline.Y] - y) * percent
            }
            when (blend) {
                setup -> {
                    bone.shearX = bone.data.shearX + x * alpha
                    bone.shearY = bone.data.shearY + y * alpha
                }
                first, replace -> {
                    bone.shearX += (bone.data.shearX + x - bone.shearX) * alpha
                    bone.shearY += (bone.data.shearY + y - bone.shearY) * alpha
                }
                add -> {
                    bone.shearX += x * alpha
                    bone.shearY += y * alpha
                }
            }
        }
    }

    /** Changes a slot's [Slot.getColor].  */
    class ColorTimeline(frameCount: Int) : CurveTimeline(frameCount), SlotTimeline {

        /** The index of the slot in [Skeleton.getSlots] that will be changed.  */
        override var slotIndex: Int = 0
            set(index) {
                require(index >= 0) { "index must be >= 0." }
                field = index
            }

        /** The time in seconds, red, green, blue, and alpha values for each key frame.  */
        val frames: FloatArray // time, r, g, b, a, ...

        override val propertyId: Int
            get() = (TimelineType.color.ordinal shl 24) + slotIndex

        init {
            frames = FloatArray(frameCount * ENTRIES)
        }

        /** Sets the time in seconds, red, green, blue, and alpha for the specified key frame.  */
        fun setFrame(frameIndex: Int, time: Float, r: Float, g: Float, b: Float, a: Float) {
            var frameIndex = frameIndex
            frameIndex *= ENTRIES
            frames[frameIndex] = time
            frames[frameIndex + R] = r
            frames[frameIndex + G] = g
            frames[frameIndex + B] = b
            frames[frameIndex + A] = a
        }

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val slot = skeleton.slots[slotIndex]
            if (!slot.bone.isActive) return
            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                when (blend) {
                    setup -> {
                        slot.color.setTo(slot.data.color)
                        return
                    }
                    first -> {
                        val color = slot.color
                        val setup = slot.data.color
                        color.add((setup.r - color.r) * alpha, (setup.g - color.g) * alpha, (setup.b - color.b) * alpha,
                                (setup.a - color.a) * alpha)
                    }
                    else -> Unit
                }
                return
            }

            var r: Float
            var g: Float
            var b: Float
            var a: Float
            if (time >= frames[frames.size - ENTRIES]) { // Time is after last frame.
                val i = frames.size
                r = frames[i + PREV_R]
                g = frames[i + PREV_G]
                b = frames[i + PREV_B]
                a = frames[i + PREV_A]
            } else {
                // Interpolate between the previous frame and the current frame.
                val frame = binarySearch(frames, time, ENTRIES)
                r = frames[frame + PREV_R]
                g = frames[frame + PREV_G]
                b = frames[frame + PREV_B]
                a = frames[frame + PREV_A]
                val frameTime = frames[frame]
                val percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime))

                r += (frames[frame + R] - r) * percent
                g += (frames[frame + G] - g) * percent
                b += (frames[frame + B] - b) * percent
                a += (frames[frame + A] - a) * percent
            }
            if (alpha == 1f)
                slot.color.setTo(r, g, b, a)
            else {
                val color = slot.color
                if (blend == setup) color.setTo(slot.data.color)
                color.add((r - color.r) * alpha, (g - color.g) * alpha, (b - color.b) * alpha, (a - color.a) * alpha)
            }
        }

        companion object {
            val ENTRIES = 5
            private val PREV_TIME = -5
            private val PREV_R = -4
            private val PREV_G = -3
            private val PREV_B = -2
            private val PREV_A = -1
            private val R = 1
            private val G = 2
            private val B = 3
            private val A = 4
        }
    }

    /** Changes a slot's [Slot.getColor] and [Slot.getDarkColor] for two color tinting.  */
    class TwoColorTimeline(frameCount: Int) : CurveTimeline(frameCount), SlotTimeline {

        /** The index of the slot in [Skeleton.getSlots] that will be changed. The [Slot.getDarkColor] must not be
         * null.  */
        override var slotIndex: Int = 0
            set(index) {
                require(index >= 0) { "index must be >= 0." }
                field = index

            }

        /** The time in seconds, red, green, blue, and alpha values for each key frame.  */
        val frames: FloatArray // time, r, g, b, a, r2, g2, b2, ...

        override val propertyId: Int
            get() = (TimelineType.twoColor.ordinal shl 24) + slotIndex

        init {
            frames = FloatArray(frameCount * ENTRIES)
        }

        /** Sets the time in seconds, light, and dark colors for the specified key frame.  */
        fun setFrame(frameIndex: Int, time: Float, r: Float, g: Float, b: Float, a: Float, r2: Float, g2: Float, b2: Float) {
            var frameIndex = frameIndex
            frameIndex *= ENTRIES
            frames[frameIndex] = time
            frames[frameIndex + R] = r
            frames[frameIndex + G] = g
            frames[frameIndex + B] = b
            frames[frameIndex + A] = a
            frames[frameIndex + R2] = r2
            frames[frameIndex + G2] = g2
            frames[frameIndex + B2] = b2
        }

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val slot = skeleton.slots[slotIndex]
            if (!slot.bone.isActive) return
            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                when (blend) {
                    setup -> {
                        slot.color.setTo(slot.data.color)
                        slot.darkColor!!.setTo(slot.data.darkColor!!)
                        return
                    }
                    first -> {
                        val light = slot.color
                        val dark = slot.darkColor
                        val setupLight = slot.data.color
                        val setupDark = slot.data.darkColor!!
                        light.add((setupLight.r - light.r) * alpha, (setupLight.g - light.g) * alpha, (setupLight.b - light.b) * alpha,
                                (setupLight.a - light.a) * alpha)
                        dark!!.add((setupDark.r - dark.r) * alpha, (setupDark.g - dark.g) * alpha, (setupDark.b - dark.b) * alpha, 0f)
                    }
                    else -> Unit
                }
                return
            }

            var r: Float
            var g: Float
            var b: Float
            var a: Float
            var r2: Float
            var g2: Float
            var b2: Float
            if (time >= frames[frames.size - ENTRIES]) { // Time is after last frame.
                val i = frames.size
                r = frames[i + PREV_R]
                g = frames[i + PREV_G]
                b = frames[i + PREV_B]
                a = frames[i + PREV_A]
                r2 = frames[i + PREV_R2]
                g2 = frames[i + PREV_G2]
                b2 = frames[i + PREV_B2]
            } else {
                // Interpolate between the previous frame and the current frame.
                val frame = binarySearch(frames, time, ENTRIES)
                r = frames[frame + PREV_R]
                g = frames[frame + PREV_G]
                b = frames[frame + PREV_B]
                a = frames[frame + PREV_A]
                r2 = frames[frame + PREV_R2]
                g2 = frames[frame + PREV_G2]
                b2 = frames[frame + PREV_B2]
                val frameTime = frames[frame]
                val percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime))

                r += (frames[frame + R] - r) * percent
                g += (frames[frame + G] - g) * percent
                b += (frames[frame + B] - b) * percent
                a += (frames[frame + A] - a) * percent
                r2 += (frames[frame + R2] - r2) * percent
                g2 += (frames[frame + G2] - g2) * percent
                b2 += (frames[frame + B2] - b2) * percent
            }
            if (alpha == 1f) {
                slot.color.setTo(r, g, b, a)
                slot.darkColor!!.setTo(r2, g2, b2, 1f)
            } else {
                val light = slot.color
                val dark = slot.darkColor
                if (blend == setup) {
                    light.setTo(slot.data.color)
                    dark!!.setTo(slot.data.darkColor!!)
                }
                light.add((r - light.r) * alpha, (g - light.g) * alpha, (b - light.b) * alpha, (a - light.a) * alpha)
                dark!!.add((r2 - dark.r) * alpha, (g2 - dark.g) * alpha, (b2 - dark.b) * alpha, 0f)
            }
        }

        companion object {
            val ENTRIES = 8
            private val PREV_TIME = -8
            private val PREV_R = -7
            private val PREV_G = -6
            private val PREV_B = -5
            private val PREV_A = -4
            private val PREV_R2 = -3
            private val PREV_G2 = -2
            private val PREV_B2 = -1
            private val R = 1
            private val G = 2
            private val B = 3
            private val A = 4
            private val R2 = 5
            private val G2 = 6
            private val B2 = 7
        }
    }

    /** Changes a slot's [Slot.getAttachment].  */
    class AttachmentTimeline(frameCount: Int) : SlotTimeline {
        /** The index of the slot in [Skeleton.getSlots] that will be changed.  */
        override var slotIndex: Int = 0
            set(index) {
                require(index >= 0) { "index must be >= 0." }
                field = index
            }

        init {
            require(frameCount > 0) { "frameCount must be > 0: $frameCount" }
        }

        /** The time in seconds for each key frame.  */
        val frames: FloatArray = FloatArray(frameCount) // time, ...

        /** The attachment name for each key frame. May contain null values to clear the attachment.  */
        val attachmentNames: Array<String?> = arrayOfNulls(frameCount)

        override val propertyId: Int
            get() = (TimelineType.attachment.ordinal shl 24) + slotIndex

        /** The number of key frames for this timeline.  */
        val frameCount: Int
            get() = frames.size

        /** Sets the time in seconds and the attachment name for the specified key frame.  */
        fun setFrame(frameIndex: Int, time: Float, attachmentName: String?) {
            frames[frameIndex] = time
            attachmentNames[frameIndex] = attachmentName
        }

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val slot = skeleton.slots[slotIndex]
            if (!slot.bone.isActive) return
            if (direction == out) {
                if (blend == setup) setAttachment(skeleton, slot, slot.data.attachmentName)
                return
            }

            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                if (blend == setup || blend == first) setAttachment(skeleton, slot, slot.data.attachmentName)
                return
            }

            val frameIndex: Int
            if (time >= frames[frames.size - 1])
            // Time is after last frame.
                frameIndex = frames.size - 1
            else
                frameIndex = binarySearch(frames, time) - 1

            setAttachment(skeleton, slot, attachmentNames[frameIndex])
        }

        private fun setAttachment(skeleton: Skeleton, slot: Slot, attachmentName: String?) {
            slot.setAttachment(if (attachmentName == null) null else skeleton.getAttachment(slotIndex, attachmentName))
        }
    }

    /** Changes a slot's [Slot.getDeform] to deform a [VertexAttachment].  */
    class DeformTimeline(frameCount: Int) : CurveTimeline(frameCount), SlotTimeline {
        /** The index of the slot in [Skeleton.getSlots] that will be changed.  */
        override var slotIndex: Int = 0
            set(index) {
                require(index >= 0) { "index must be >= 0." }
                field = index
            }

        /** The attachment that will be deformed.  */
        lateinit var attachment: VertexAttachment

        /** The time in seconds for each key frame.  */
        val frames: FloatArray = FloatArray(frameCount) // time, ...

        /** The vertices for each key frame.  */
        val vertices: Array<FloatArray> = arrayOfNulls<FloatArray>(frameCount) as Array<FloatArray>

        override val propertyId: Int
            get() = (TimelineType.deform.ordinal shl 27) + attachment.id + slotIndex

        /** Sets the time in seconds and the vertices for the specified key frame.
         * @param vertices Vertex positions for an unweighted VertexAttachment, or deform offsets if it has weights.
         */
        fun setFrame(frameIndex: Int, time: Float, vertices: FloatArray) {
            frames[frameIndex] = time
            this.vertices[frameIndex] = vertices
        }

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {
            var alpha = alpha
            var blend = blend

            val slot = skeleton.slots[slotIndex]
            if (!slot.bone.isActive) return
            val slotAttachment = slot.attachment
            if (slotAttachment !is VertexAttachment || slotAttachment.deformAttachment !== attachment)
                return

            val deformArray = slot.deform
            if (deformArray.size == 0) blend = setup

            val frameVertices = this.vertices
            val vertexCount = frameVertices[0].size

            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                when (blend) {
                    setup -> {
                        deformArray.clear()
                        return
                    }
                    first -> {
                        if (alpha == 1f) {
                            deformArray.clear()
                            return
                        }
                        val deform = deformArray.setSize(vertexCount)
                        if (slotAttachment.bones == null) {
                            // Unweighted vertex positions.
                            val setupVertices = slotAttachment.vertices
                            for (i in 0 until vertexCount)
                                deform[i] += (setupVertices!![i] - deform[i]) * alpha
                        } else {
                            // Weighted deform offsets.
                            alpha = 1 - alpha
                            for (i in 0 until vertexCount)
                                deform[i] *= alpha
                        }
                    }
                    else -> Unit
                }
                return
            }

            val deform = deformArray.setSize(vertexCount)

            if (time >= frames[frames.size - 1]) { // Time is after last frame.
                val lastVertices = frameVertices[frames.size - 1]
                if (alpha == 1f) {
                    if (blend == add) {
                        if (slotAttachment.bones == null) {
                            // Unweighted vertex positions, no alpha.
                            val setupVertices = slotAttachment.vertices
                            for (i in 0 until vertexCount)
                                deform[i] += lastVertices[i] - setupVertices!![i]
                        } else {
                            // Weighted deform offsets, no alpha.
                            for (i in 0 until vertexCount)
                                deform[i] += lastVertices[i]
                        }
                    } else {
                        // Vertex positions or deform offsets, no alpha.
                        arraycopy(lastVertices, 0, deform, 0, vertexCount)
                    }
                } else {
                    when (blend) {
                        setup -> {
                            if (slotAttachment.bones == null) {
                                // Unweighted vertex positions, with alpha.
                                val setupVertices = slotAttachment.vertices
                                for (i in 0 until vertexCount) {
                                    val setup = setupVertices!![i]
                                    deform[i] = setup + (lastVertices[i] - setup) * alpha
                                }
                            } else {
                                // Weighted deform offsets, with alpha.
                                for (i in 0 until vertexCount)
                                    deform[i] = lastVertices[i] * alpha
                            }
                        }
                        first, replace ->
                            // Vertex positions or deform offsets, with alpha.
                            for (i in 0 until vertexCount)
                                deform[i] += (lastVertices[i] - deform[i]) * alpha
                        add -> {
                            if (slotAttachment.bones == null) {
                                // Unweighted vertex positions, no alpha.
                                val setupVertices = slotAttachment.vertices
                                for (i in 0 until vertexCount)
                                    deform[i] += (lastVertices[i] - setupVertices!![i]) * alpha
                            } else {
                                // Weighted deform offsets, alpha.
                                for (i in 0 until vertexCount)
                                    deform[i] += lastVertices[i] * alpha
                            }
                        }
                    }
                }
                return
            }

            // Interpolate between the previous frame and the current frame.
            val frame = binarySearch(frames, time)
            val prevVertices = frameVertices[frame - 1]
            val nextVertices = frameVertices[frame]
            val frameTime = frames[frame]
            val percent = getCurvePercent(frame - 1, 1 - (time - frameTime) / (frames[frame - 1] - frameTime))

            if (alpha == 1f) {
                if (blend == add) {
                    if (slotAttachment.bones == null) {
                        // Unweighted vertex positions, no alpha.
                        val setupVertices = slotAttachment.vertices
                        for (i in 0 until vertexCount) {
                            val prev = prevVertices[i]
                            deform[i] += prev + (nextVertices[i] - prev) * percent - setupVertices!![i]
                        }
                    } else {
                        // Weighted deform offsets, no alpha.
                        for (i in 0 until vertexCount) {
                            val prev = prevVertices[i]
                            deform[i] += prev + (nextVertices[i] - prev) * percent
                        }
                    }
                } else {
                    // Vertex positions or deform offsets, no alpha.
                    for (i in 0 until vertexCount) {
                        val prev = prevVertices[i]
                        deform[i] = prev + (nextVertices[i] - prev) * percent
                    }
                }
            } else {
                when (blend) {
                    setup -> {
                        if (slotAttachment.bones == null) {
                            // Unweighted vertex positions, with alpha.
                            val setupVertices = slotAttachment.vertices
                            for (i in 0 until vertexCount) {
                                val prev = prevVertices[i]
                                val setup = setupVertices!![i]
                                deform[i] = setup + (prev + (nextVertices[i] - prev) * percent - setup) * alpha
                            }
                        } else {
                            // Weighted deform offsets, with alpha.
                            for (i in 0 until vertexCount) {
                                val prev = prevVertices[i]
                                deform[i] = (prev + (nextVertices[i] - prev) * percent) * alpha
                            }
                        }
                    }
                    first, replace ->
                        // Vertex positions or deform offsets, with alpha.
                        for (i in 0 until vertexCount) {
                            val prev = prevVertices[i]
                            deform[i] += (prev + (nextVertices[i] - prev) * percent - deform[i]) * alpha
                        }
                    add -> {
                        if (slotAttachment.bones == null) {
                            // Unweighted vertex positions, with alpha.
                            val setupVertices = slotAttachment.vertices
                            for (i in 0 until vertexCount) {
                                val prev = prevVertices[i]
                                deform[i] += (prev + (nextVertices[i] - prev) * percent - setupVertices!![i]) * alpha
                            }
                        } else {
                            // Weighted deform offsets, with alpha.
                            for (i in 0 until vertexCount) {
                                val prev = prevVertices[i]
                                deform[i] += (prev + (nextVertices[i] - prev) * percent) * alpha
                            }
                        }
                    }
                }
            }
        }
    }

    /** Fires an [Event] when specific animation times are reached.  */
    class EventTimeline(frameCount: Int) : Timeline {
        init {
            require(frameCount > 0) { "frameCount must be > 0: $frameCount" }
        }
        /** The time in seconds for each key frame.  */
        val frames: FloatArray = FloatArray(frameCount) // time, ...

        /** The event for each key frame.  */
        val events: Array<Event> = arrayOfNulls<Event>(frameCount) as Array<Event>

        override val propertyId: Int
            get() = TimelineType.event.ordinal shl 24

        /** The number of key frames for this timeline.  */
        val frameCount: Int
            get() = frames.size

        /** Sets the time in seconds and the event for the specified key frame.  */
        fun setFrame(frameIndex: Int, event: Event) {
            frames[frameIndex] = event.time
            events[frameIndex] = event
        }

        /** Fires events for frames > `lastTime` and <= `time`.  */
        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, firedEvents: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {
            var lastTime = lastTime

            if (firedEvents == null) return
            val frames = this.frames
            val frameCount = frames.size

            if (lastTime > time) { // Fire events after last time for looped animations.
                apply(skeleton, lastTime, Int.MAX_VALUE.toFloat(), firedEvents, alpha, blend, direction)
                lastTime = -1f
            } else if (lastTime >= frames[frameCount - 1])
            // Last time is after last frame.
                return
            if (time < frames[0]) return  // Time is before first frame.

            var frame: Int
            if (lastTime < frames[0])
                frame = 0
            else {
                frame = binarySearch(frames, lastTime)
                val frameTime = frames[frame]
                while (frame > 0) { // Fire multiple events with the same frame.
                    if (frames[frame - 1] != frameTime) break
                    frame--
                }
            }
            while (frame < frameCount && time >= frames[frame]) {
                firedEvents.add(events[frame])
                frame++
            }
        }
    }

    /** Changes a skeleton's [Skeleton.getDrawOrder].  */
    class DrawOrderTimeline(frameCount: Int) : Timeline {
        init {
            require(frameCount > 0) { "frameCount must be > 0: $frameCount" }
        }
        /** The time in seconds for each key frame.  */
        val frames: FloatArray = FloatArray(frameCount) // time, ...

        /** The draw order for each key frame. See [.setFrame].  */
        val drawOrders: Array<IntArray?> = arrayOfNulls<IntArray>(frameCount)

        override val propertyId: Int
            get() = TimelineType.drawOrder.ordinal shl 24

        /** The number of key frames for this timeline.  */
        val frameCount: Int
            get() = frames.size

        /** Sets the time in seconds and the draw order for the specified key frame.
         * @param drawOrder For each slot in [Skeleton.slots], the index of the new draw order. May be null to use setup pose
         * draw order.
         */
        fun setFrame(frameIndex: Int, time: Float, drawOrder: IntArray?) {
            frames[frameIndex] = time
            drawOrders[frameIndex] = drawOrder
        }

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val drawOrder = skeleton.drawOrder
            val slots = skeleton.slots
            if (direction == out) {
                if (blend == setup) arraycopy(slots, 0, drawOrder, 0, slots.size)
                return
            }

            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                if (blend == setup || blend == first) arraycopy(slots, 0, drawOrder, 0, slots.size)
                return
            }

            val frame: Int
            if (time >= frames[frames.size - 1])
            // Time is after last frame.
                frame = frames.size - 1
            else
                frame = binarySearch(frames, time) - 1

            val drawOrderToSetupIndex = drawOrders[frame]
            if (drawOrderToSetupIndex == null)
                arraycopy(slots, 0, drawOrder, 0, slots.size)
            else {
                var i = 0
                val n = drawOrderToSetupIndex.size
                while (i < n) {
                    drawOrder.setAndGrow(i, slots[drawOrderToSetupIndex[i]])
                    i++
                }
            }
        }
    }

    /** Changes an IK constraint's [IkConstraint.getMix], [IkConstraint.getSoftness],
     * [IkConstraint.getBendDirection], [IkConstraint.getStretch], and [IkConstraint.getCompress].  */
    class IkConstraintTimeline(frameCount: Int) : CurveTimeline(frameCount) {

        internal var ikConstraintIndex: Int = 0

        /** The time in seconds, mix, softness, bend direction, compress, and stretch for each key frame.  */
        val frames: FloatArray = FloatArray(frameCount * ENTRIES) // time, mix, softness, bendDirection, compress, stretch, ...

        override val propertyId: Int
            get() = (TimelineType.ikConstraint.ordinal shl 24) + ikConstraintIndex

        fun setIkConstraintIndex(index: Int) {
            require(index >= 0) { "index must be >= 0." }
            this.ikConstraintIndex = index
        }

        /** The index of the IK constraint slot in [Skeleton.getIkConstraints] that will be changed.  */
        fun getIkConstraintIndex(): Int {
            return ikConstraintIndex
        }

        /** Sets the time in seconds, mix, softness, bend direction, compress, and stretch for the specified key frame.  */
        fun setFrame(frameIndex: Int, time: Float, mix: Float, softness: Float, bendDirection: Int, compress: Boolean,
                     stretch: Boolean) {
            var frameIndex = frameIndex
            frameIndex *= ENTRIES
            frames[frameIndex] = time
            frames[frameIndex + MIX] = mix
            frames[frameIndex + SOFTNESS] = softness
            frames[frameIndex + BEND_DIRECTION] = bendDirection.toFloat()
            frames[frameIndex + COMPRESS] = (if (compress) 1 else 0).toFloat()
            frames[frameIndex + STRETCH] = (if (stretch) 1 else 0).toFloat()
        }

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val constraint = skeleton.ikConstraints[ikConstraintIndex]
            if (!constraint.active) return
            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                when (blend) {
                    setup -> {
                        constraint.mix = constraint.data.mix
                        constraint.softness = constraint.data.softness
                        constraint.bendDirection = constraint.data.bendDirection
                        constraint.compress = constraint.data.compress
                        constraint.stretch = constraint.data.stretch
                        return
                    }
                    first -> {
                        constraint.mix += (constraint.data.mix - constraint.mix) * alpha
                        constraint.softness += (constraint.data.softness - constraint.softness) * alpha
                        constraint.bendDirection = constraint.data.bendDirection
                        constraint.compress = constraint.data.compress
                        constraint.stretch = constraint.data.stretch
                    }
                    else -> Unit
                }
                return
            }

            if (time >= frames[frames.size - ENTRIES]) { // Time is after last frame.
                if (blend == setup) {
                    constraint.mix = constraint.data.mix + (frames[frames.size + PREV_MIX] - constraint.data.mix) * alpha
                    constraint.softness = constraint.data.softness + (frames[frames.size + PREV_SOFTNESS] - constraint.data.softness) * alpha
                    if (direction == out) {
                        constraint.bendDirection = constraint.data.bendDirection
                        constraint.compress = constraint.data.compress
                        constraint.stretch = constraint.data.stretch
                    } else {
                        constraint.bendDirection = frames[frames.size + PREV_BEND_DIRECTION].toInt()
                        constraint.compress = frames[frames.size + PREV_COMPRESS] != 0f
                        constraint.stretch = frames[frames.size + PREV_STRETCH] != 0f
                    }
                } else {
                    constraint.mix += (frames[frames.size + PREV_MIX] - constraint.mix) * alpha
                    constraint.softness += (frames[frames.size + PREV_SOFTNESS] - constraint.softness) * alpha
                    if (direction == `in`) {
                        constraint.bendDirection = frames[frames.size + PREV_BEND_DIRECTION].toInt()
                        constraint.compress = frames[frames.size + PREV_COMPRESS] != 0f
                        constraint.stretch = frames[frames.size + PREV_STRETCH] != 0f
                    }
                }
                return
            }

            // Interpolate between the previous frame and the current frame.
            val frame = binarySearch(frames, time, ENTRIES)
            val mix = frames[frame + PREV_MIX]
            val softness = frames[frame + PREV_SOFTNESS]
            val frameTime = frames[frame]
            val percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime))

            if (blend == setup) {
                constraint.mix = constraint.data.mix + (mix + (frames[frame + MIX] - mix) * percent - constraint.data.mix) * alpha
                constraint.softness = constraint.data.softness + (softness + (frames[frame + SOFTNESS] - softness) * percent - constraint.data.softness) * alpha
                if (direction == out) {
                    constraint.bendDirection = constraint.data.bendDirection
                    constraint.compress = constraint.data.compress
                    constraint.stretch = constraint.data.stretch
                } else {
                    constraint.bendDirection = frames[frame + PREV_BEND_DIRECTION].toInt()
                    constraint.compress = frames[frame + PREV_COMPRESS] != 0f
                    constraint.stretch = frames[frame + PREV_STRETCH] != 0f
                }
            } else {
                constraint.mix += (mix + (frames[frame + MIX] - mix) * percent - constraint.mix) * alpha
                constraint.softness += (softness + (frames[frame + SOFTNESS] - softness) * percent - constraint.softness) * alpha
                if (direction == `in`) {
                    constraint.bendDirection = frames[frame + PREV_BEND_DIRECTION].toInt()
                    constraint.compress = frames[frame + PREV_COMPRESS] != 0f
                    constraint.stretch = frames[frame + PREV_STRETCH] != 0f
                }
            }
        }

        companion object {
            val ENTRIES = 6
            private val PREV_TIME = -6
            private val PREV_MIX = -5
            private val PREV_SOFTNESS = -4
            private val PREV_BEND_DIRECTION = -3
            private val PREV_COMPRESS = -2
            private val PREV_STRETCH = -1
            private val MIX = 1
            private val SOFTNESS = 2
            private val BEND_DIRECTION = 3
            private val COMPRESS = 4
            private val STRETCH = 5
        }
    }

    /** Changes a transform constraint's [TransformConstraint.getRotateMix], [TransformConstraint.getTranslateMix],
     * [TransformConstraint.getScaleMix], and [TransformConstraint.getShearMix].  */
    class TransformConstraintTimeline(frameCount: Int) : CurveTimeline(frameCount) {

        internal var transformConstraintIndex: Int = 0

        /** The time in seconds, rotate mix, translate mix, scale mix, and shear mix for each key frame.  */
        val frames: FloatArray = FloatArray(frameCount * ENTRIES) // time, rotate mix, translate mix, scale mix, shear mix, ...

        override val propertyId: Int
            get() = (TimelineType.transformConstraint.ordinal shl 24) + transformConstraintIndex

        fun setTransformConstraintIndex(index: Int) {
            require(index >= 0) { "index must be >= 0." }
            this.transformConstraintIndex = index
        }

        /** The index of the transform constraint slot in [Skeleton.getTransformConstraints] that will be changed.  */
        fun getTransformConstraintIndex(): Int {
            return transformConstraintIndex
        }

        /** The time in seconds, rotate mix, translate mix, scale mix, and shear mix for the specified key frame.  */
        fun setFrame(frameIndex: Int, time: Float, rotateMix: Float, translateMix: Float, scaleMix: Float, shearMix: Float) {
            var frameIndex = frameIndex
            frameIndex *= ENTRIES
            frames[frameIndex] = time
            frames[frameIndex + ROTATE] = rotateMix
            frames[frameIndex + TRANSLATE] = translateMix
            frames[frameIndex + SCALE] = scaleMix
            frames[frameIndex + SHEAR] = shearMix
        }

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val constraint = skeleton.transformConstraints[transformConstraintIndex]
            if (!constraint.isActive) return
            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                val data = constraint.data
                when (blend) {
                    setup -> {
                        constraint.rotateMix = data.rotateMix
                        constraint.translateMix = data.translateMix
                        constraint.scaleMix = data.scaleMix
                        constraint.shearMix = data.shearMix
                        return
                    }
                    first -> {
                        constraint.rotateMix += (data.rotateMix - constraint.rotateMix) * alpha
                        constraint.translateMix += (data.translateMix - constraint.translateMix) * alpha
                        constraint.scaleMix += (data.scaleMix - constraint.scaleMix) * alpha
                        constraint.shearMix += (data.shearMix - constraint.shearMix) * alpha
                    }
                    else -> Unit
                }
                return
            }

            var rotate: Float
            var translate: Float
            var scale: Float
            var shear: Float
            if (time >= frames[frames.size - ENTRIES]) { // Time is after last frame.
                val i = frames.size
                rotate = frames[i + PREV_ROTATE]
                translate = frames[i + PREV_TRANSLATE]
                scale = frames[i + PREV_SCALE]
                shear = frames[i + PREV_SHEAR]
            } else {
                // Interpolate between the previous frame and the current frame.
                val frame = binarySearch(frames, time, ENTRIES)
                rotate = frames[frame + PREV_ROTATE]
                translate = frames[frame + PREV_TRANSLATE]
                scale = frames[frame + PREV_SCALE]
                shear = frames[frame + PREV_SHEAR]
                val frameTime = frames[frame]
                val percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime))

                rotate += (frames[frame + ROTATE] - rotate) * percent
                translate += (frames[frame + TRANSLATE] - translate) * percent
                scale += (frames[frame + SCALE] - scale) * percent
                shear += (frames[frame + SHEAR] - shear) * percent
            }
            if (blend == setup) {
                val data = constraint.data
                constraint.rotateMix = data.rotateMix + (rotate - data.rotateMix) * alpha
                constraint.translateMix = data.translateMix + (translate - data.translateMix) * alpha
                constraint.scaleMix = data.scaleMix + (scale - data.scaleMix) * alpha
                constraint.shearMix = data.shearMix + (shear - data.shearMix) * alpha
            } else {
                constraint.rotateMix += (rotate - constraint.rotateMix) * alpha
                constraint.translateMix += (translate - constraint.translateMix) * alpha
                constraint.scaleMix += (scale - constraint.scaleMix) * alpha
                constraint.shearMix += (shear - constraint.shearMix) * alpha
            }
        }

        companion object {
            val ENTRIES = 5
            private val PREV_TIME = -5
            private val PREV_ROTATE = -4
            private val PREV_TRANSLATE = -3
            private val PREV_SCALE = -2
            private val PREV_SHEAR = -1
            private val ROTATE = 1
            private val TRANSLATE = 2
            private val SCALE = 3
            private val SHEAR = 4
        }
    }

    /** Changes a path constraint's [PathConstraint.getPosition].  */
    open class PathConstraintPositionTimeline(frameCount: Int) : CurveTimeline(frameCount) {

        internal var pathConstraintIndex: Int = 0

        /** The time in seconds and path constraint position for each key frame.  */
        val frames: FloatArray = FloatArray(frameCount * ENTRIES) // time, position, ...

        override val propertyId: Int
            get() = (TimelineType.pathConstraintPosition.ordinal shl 24) + pathConstraintIndex

        fun setPathConstraintIndex(index: Int) {
            require(index >= 0) { "index must be >= 0." }
            this.pathConstraintIndex = index
        }

        /** The index of the path constraint slot in [Skeleton.getPathConstraints] that will be changed.  */
        fun getPathConstraintIndex(): Int {
            return pathConstraintIndex
        }

        /** Sets the time in seconds and path constraint position for the specified key frame.  */
        fun setFrame(frameIndex: Int, time: Float, position: Float) {
            var frameIndex = frameIndex
            frameIndex *= ENTRIES
            frames[frameIndex] = time
            frames[frameIndex + VALUE] = position
        }

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val constraint = skeleton.pathConstraints[pathConstraintIndex]
            if (!constraint.isActive) return
            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                when (blend) {
                    setup -> {
                        constraint.position = constraint.data.position
                        return
                    }
                    first -> constraint.position += (constraint.data.position - constraint.position) * alpha
                    else -> Unit
                }
                return
            }

            var position: Float
            if (time >= frames[frames.size - ENTRIES])
            // Time is after last frame.
                position = frames[frames.size + PREV_VALUE]
            else {
                // Interpolate between the previous frame and the current frame.
                val frame = binarySearch(frames, time, ENTRIES)
                position = frames[frame + PREV_VALUE]
                val frameTime = frames[frame]
                val percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime))

                position += (frames[frame + VALUE] - position) * percent
            }
            if (blend == setup)
                constraint.position = constraint.data.position + (position - constraint.data.position) * alpha
            else
                constraint.position += (position - constraint.position) * alpha
        }

        companion object {
            val ENTRIES = 2
            internal val PREV_TIME = -2
            internal val PREV_VALUE = -1
            internal val VALUE = 1
        }
    }

    /** Changes a path constraint's [PathConstraint.getSpacing].  */
    class PathConstraintSpacingTimeline(frameCount: Int) : PathConstraintPositionTimeline(frameCount) {

        override val propertyId: Int
            get() = (TimelineType.pathConstraintSpacing.ordinal shl 24) + pathConstraintIndex

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val constraint = skeleton.pathConstraints[pathConstraintIndex]
            if (!constraint.isActive) return
            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                when (blend) {
                    setup -> {
                        constraint.spacing = constraint.data.spacing
                        return
                    }
                    first -> constraint.spacing += (constraint.data.spacing - constraint.spacing) * alpha
                    else -> Unit
                }
                return
            }

            var spacing: Float
            if (time >= frames[frames.size - Animation.PathConstraintPositionTimeline.ENTRIES])
            // Time is after last frame.
                spacing = frames[frames.size + Animation.PathConstraintPositionTimeline.PREV_VALUE]
            else {
                // Interpolate between the previous frame and the current frame.
                val frame = binarySearch(frames, time, Animation.PathConstraintPositionTimeline.ENTRIES)
                spacing = frames[frame + Animation.PathConstraintPositionTimeline.PREV_VALUE]
                val frameTime = frames[frame]
                val percent = getCurvePercent(frame / Animation.PathConstraintPositionTimeline.ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + Animation.PathConstraintPositionTimeline.PREV_TIME] - frameTime))

                spacing += (frames[frame + Animation.PathConstraintPositionTimeline.VALUE] - spacing) * percent
            }

            if (blend == setup)
                constraint.spacing = constraint.data.spacing + (spacing - constraint.data.spacing) * alpha
            else
                constraint.spacing += (spacing - constraint.spacing) * alpha
        }
    }

    /** Changes a transform constraint's [PathConstraint.getRotateMix] and
     * [TransformConstraint.getTranslateMix].  */
    class PathConstraintMixTimeline(frameCount: Int) : CurveTimeline(frameCount) {

        internal var pathConstraintIndex: Int = 0

        /** The time in seconds, rotate mix, and translate mix for each key frame.  */
        val frames: FloatArray = FloatArray(frameCount * ENTRIES) // time, rotate mix, translate mix, ...

        override val propertyId: Int
            get() = (TimelineType.pathConstraintMix.ordinal shl 24) + pathConstraintIndex

        fun setPathConstraintIndex(index: Int) {
            require(index >= 0) { "index must be >= 0." }
            this.pathConstraintIndex = index
        }

        /** The index of the path constraint slot in [Skeleton.getPathConstraints] that will be changed.  */
        fun getPathConstraintIndex(): Int {
            return pathConstraintIndex
        }

        /** The time in seconds, rotate mix, and translate mix for the specified key frame.  */
        fun setFrame(frameIndex: Int, time: Float, rotateMix: Float, translateMix: Float) {
            var frameIndex = frameIndex
            frameIndex *= ENTRIES
            frames[frameIndex] = time
            frames[frameIndex + ROTATE] = rotateMix
            frames[frameIndex + TRANSLATE] = translateMix
        }

        override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: FastArrayList<Event>?, alpha: Float, blend: MixBlend,
                           direction: MixDirection) {

            val constraint = skeleton.pathConstraints[pathConstraintIndex]
            if (!constraint.isActive) return
            val frames = this.frames
            if (time < frames[0]) { // Time is before first frame.
                when (blend) {
                    setup -> {
                        constraint.rotateMix = constraint.data.rotateMix
                        constraint.translateMix = constraint.data.translateMix
                        return
                    }
                    first -> {
                        constraint.rotateMix += (constraint.data.rotateMix - constraint.rotateMix) * alpha
                        constraint.translateMix += (constraint.data.translateMix - constraint.translateMix) * alpha
                    }
                    else -> Unit
                }
                return
            }

            var rotate: Float
            var translate: Float
            if (time >= frames[frames.size - ENTRIES]) { // Time is after last frame.
                rotate = frames[frames.size + PREV_ROTATE]
                translate = frames[frames.size + PREV_TRANSLATE]
            } else {
                // Interpolate between the previous frame and the current frame.
                val frame = binarySearch(frames, time, ENTRIES)
                rotate = frames[frame + PREV_ROTATE]
                translate = frames[frame + PREV_TRANSLATE]
                val frameTime = frames[frame]
                val percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime))

                rotate += (frames[frame + ROTATE] - rotate) * percent
                translate += (frames[frame + TRANSLATE] - translate) * percent
            }

            if (blend == setup) {
                constraint.rotateMix = constraint.data.rotateMix + (rotate - constraint.data.rotateMix) * alpha
                constraint.translateMix = constraint.data.translateMix + (translate - constraint.data.translateMix) * alpha
            } else {
                constraint.rotateMix += (rotate - constraint.rotateMix) * alpha
                constraint.translateMix += (translate - constraint.translateMix) * alpha
            }
        }

        companion object {
            val ENTRIES = 3
            private val PREV_TIME = -3
            private val PREV_ROTATE = -2
            private val PREV_TRANSLATE = -1
            private val ROTATE = 1
            private val TRANSLATE = 2
        }
    }

    companion object {

        /** @param target After the first and before the last value.
         * @return index of first value greater than the target.
         */
        internal fun binarySearch(values: FloatArray, target: Float, step: Int): Int {
            var low = 0
            var high = values.size / step - 2
            if (high == 0) return step
            var current = high.ushr(1)
            while (true) {
                if (values[(current + 1) * step] <= target)
                    low = current + 1
                else
                    high = current
                if (low == high) return (low + 1) * step
                current = (low + high).ushr(1)
            }
        }

        /** @param target After the first and before the last value.
         * @return index of first value greater than the target.
         */
        internal fun binarySearch(values: FloatArray, target: Float): Int {
            var low = 0
            var high = values.size - 2
            if (high == 0) return 1
            var current = high.ushr(1)
            while (true) {
                if (values[current + 1] <= target)
                    low = current + 1
                else
                    high = current
                if (low == high) return low + 1
                current = (low + high).ushr(1)
            }
        }

        internal fun linearSearch(values: FloatArray, target: Float, step: Int): Int {
            var i = 0
            val last = values.size - step
            while (i <= last) {
                if (values[i] > target) return i
                i += step
            }
            return -1
        }
    }
}
