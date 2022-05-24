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

import com.esotericsoftware.spine.Animation.*
import com.esotericsoftware.spine.utils.*
import com.soywiz.kds.*
import com.soywiz.kmem.*
import kotlin.js.*
import kotlin.math.*

/** Applies animations over time, queues animations for later playback, mixes (crossfading) between animations, and applies
 * multiple animations on top of each other (layering).
 *
 *
 * See [Applying Animations](http://esotericsoftware.com/spine-applying-animations/) in the Spine Runtimes Guide.  */
class AnimationState {

    private var data: AnimationStateData? = null

    /** The list of tracks that currently have animations, which may contain null entries.  */
    val tracks: FastArrayList<TrackEntry?> = FastArrayList()
    private val events = FastArrayList<Event>()
    internal val listeners: FastArrayList<AnimationStateListener> = FastArrayList()
    private val queue = EventQueue()
    private val propertyIDs = IntSet()
    @JsName("animationsChangedProp")
    internal var animationsChanged: Boolean = false

    /** Multiplier for the delta time when the animation state is updated, causing time for all animations and mixes to play slower
     * or faster. Defaults to 1.
     *
     *
     * See TrackEntry [TrackEntry.getTimeScale] for affecting a single animation.  */
    var timeScale = 1f
    private var unkeyedState: Int = 0

    internal val trackEntryPool: Pool<TrackEntry> = Pool.fromPoolable { TrackEntry() }

    /** Creates an uninitialized AnimationState. The animation state data must be set before use.  */
    constructor() {}

    constructor(data: AnimationStateData) {
        this.data = data
    }

    /** Increments each track entry [TrackEntry.getTrackTime], setting queued animations as current if needed.  */
    fun update(delta: Float) {
        var delta = delta
        delta *= timeScale
        var i = 0
        val n = tracks.size
        while (i < n) {
            val current = tracks[i]
            if (current == null) {
                i++
                continue
            }

            current.animationLast = current.nextAnimationLast
            current.trackLast = current.nextTrackLast

            var currentDelta = delta * current.timeScale

            if (current.delay > 0) {
                current.delay -= currentDelta
                if (current.delay > 0) {
                    i++
                    continue
                }
                currentDelta = -current.delay
                current.delay = 0f
            }

            var next = current.next
            if (next != null) {
                // When the next entry's delay is passed, change to the next entry, preserving leftover time.
                val nextTime = current.trackLast - next.delay
                if (nextTime >= 0) {
                    next.delay = 0f
                    next.trackTime += if (current.timeScale == 0f) 0f else (nextTime / current.timeScale + delta) * next.timeScale
                    current.trackTime += currentDelta
                    setCurrent(i, next, true)
                    while (next!!.mixingFrom != null) {
                        next.mixTime += delta
                        next = next.mixingFrom
                    }
                    i++
                    continue
                }
            } else if (current.trackLast >= current.trackEnd && current.mixingFrom == null) {
                // Clear the track when there is no next entry, the track end time is reached, and there is no mixingFrom.
                tracks.setAndGrow(i, null)
                queue.end(current)
                disposeNext(current)
                continue
            }
            if (current.mixingFrom != null && updateMixingFrom(current, delta)) {
                // End mixing from entries once all have completed.
                var from = current.mixingFrom
                current.mixingFrom = null
                if (from != null) from.mixingTo = null
                while (from != null) {
                    queue.end(from)
                    from = from.mixingFrom
                }
            }

            current.trackTime += currentDelta
            i++
        }

        queue.drain()
    }

    /** Returns true when all mixing from entries are complete.  */
    private fun updateMixingFrom(to: TrackEntry, delta: Float): Boolean {
        val from = to.mixingFrom ?: return true

        val finished = updateMixingFrom(from, delta)

        from.animationLast = from.nextAnimationLast
        from.trackLast = from.nextTrackLast

        // Require mixTime > 0 to ensure the mixing from entry was applied at least once.
        if (to.mixTime > 0 && to.mixTime >= to.mixDuration) {
            // Require totalAlpha == 0 to ensure mixing is complete, unless mixDuration == 0 (the transition is a single frame).
            if (from.totalAlpha == 0f || to.mixDuration == 0f) {
                to.mixingFrom = from.mixingFrom
                if (from.mixingFrom != null) from.mixingFrom!!.mixingTo = to
                to.interruptAlpha = from.interruptAlpha
                queue.end(from)
            }
            return finished
        }

        from.trackTime += delta * from.timeScale
        to.mixTime += delta
        return false
    }

    /** Poses the skeleton using the track entry animations. The animation state is not changed, so can be applied to multiple
     * skeletons to pose them identically.
     * @return True if any animations were applied.
     */
    fun apply(skeleton: Skeleton): Boolean {
        if (animationsChanged) animationsChanged()

        val events = this.events
        var applied = false
        run {
            var i = 0
            val n = tracks.size
            while (i < n) {
                val current = tracks[i]
                if (current == null || current.delay > 0) {
                    i++
                    continue
                }
                applied = true

                // Track 0 animations aren't for layering, so do not show the previously applied animations before the first key.
                val blend = if (i == 0) MixBlend.first else current.mixBlend

                // Apply mixing from entries first.
                var mix = current.alpha
                if (current.mixingFrom != null)
                    mix *= applyMixingFrom(current, skeleton, blend)
                else if (current.trackTime >= current.trackEnd && current.next == null)
                //
                    mix = 0f // Set to setup pose the last time the entry will be applied.

                // Apply current entry.
                val animationLast = current.animationLast
                val animationTime = current.animationTime
                val timelineCount = current.animation!!.timelines.size
                val timelines = current.animation!!.timelines
                if (i == 0 && mix == 1f || blend == MixBlend.add) {
                    for (ii in 0 until timelineCount) {
                        val timeline = timelines[ii]
                        if (timeline is AttachmentTimeline)
                            applyAttachmentTimeline(timeline, skeleton, animationTime, blend, true)
                        else
                            (timeline as Timeline).apply(skeleton, animationLast, animationTime, events, mix, blend, MixDirection.`in`)
                    }
                } else {
                    val timelineMode = current.timelineMode.data

                    val firstFrame = current.timelinesRotation.size != timelineCount shl 1
                    if (firstFrame) current.timelinesRotation.setSize(timelineCount shl 1)
                    val timelinesRotation = current.timelinesRotation.data

                    for (ii in 0 until timelineCount) {
                        val timeline = timelines[ii] as Timeline
                        val timelineBlend = if (timelineMode[ii] == SUBSEQUENT) blend else MixBlend.setup
                        if (timeline is RotateTimeline) {
                            applyRotateTimeline(timeline, skeleton, animationTime, mix, timelineBlend, timelinesRotation,
                                    ii shl 1, firstFrame)
                        } else if (timeline is AttachmentTimeline)
                            applyAttachmentTimeline(timeline, skeleton, animationTime, blend, true)
                        else
                            timeline.apply(skeleton, animationLast, animationTime, events, mix, timelineBlend, MixDirection.`in`)
                    }
                }
                queueEvents(current, animationTime)
                events.clear()
                current.nextAnimationLast = animationTime
                current.nextTrackLast = current.trackTime
                i++
            }
        }

        // Set slots attachments to the setup pose, if needed. This occurs if an animation that is mixing out sets attachments so
        // subsequent timelines see any deform, but the subsequent timelines don't set an attachment (eg they are also mixing out or
        // the time is before the first key).
        val setupState = unkeyedState + SETUP
        val slots = skeleton.slots
        var i = 0
        val n = skeleton.slots.size
        while (i < n) {
            val slot = slots[i] as Slot
            if (slot.attachmentState == setupState) {
                val attachmentName = slot.data.attachmentName
                slot.setAttachment(if (attachmentName == null) null else skeleton.getAttachment(slot.data.index, attachmentName))
            }
            i++
        }
        unkeyedState += 2 // Increasing after each use avoids the need to reset attachmentState for every slot.

        queue.drain()
        return applied
    }

    private fun applyMixingFrom(to: TrackEntry, skeleton: Skeleton, blend: MixBlend): Float {
        var blend = blend
        val from = to.mixingFrom
        if (from!!.mixingFrom != null) applyMixingFrom(from, skeleton, blend)

        var mix: Float
        if (to.mixDuration == 0f) { // Single frame mix to undo mixingFrom changes.
            mix = 1f
            if (blend == MixBlend.first) blend = MixBlend.setup // Tracks >0 are transparent and can't reset to setup pose.
        } else {
            mix = to.mixTime / to.mixDuration
            if (mix > 1) mix = 1f
            if (blend != MixBlend.first) blend = from.mixBlend // Track 0 ignores track mix blend.
        }

        val events = if (mix < from.eventThreshold) this.events else null
        val attachments = mix < from.attachmentThreshold
        val drawOrder = mix < from.drawOrderThreshold
        val animationLast = from.animationLast
        val animationTime = from.animationTime
        val timelineCount = from.animation!!.timelines.size
        val timelines = from.animation!!.timelines
        val alphaHold = from.alpha * to.interruptAlpha
        val alphaMix = alphaHold * (1 - mix)

        if (blend == MixBlend.add) {
            for (i in 0 until timelineCount)
                timelines[i].apply(skeleton, animationLast, animationTime, events, alphaMix, blend, MixDirection.out)
        } else {
            val timelineMode = from.timelineMode
            val timelineHoldMix = from.timelineHoldMix

            val firstFrame = from.timelinesRotation.size != timelineCount shl 1
            if (firstFrame) from.timelinesRotation.setSize(timelineCount shl 1)
            val timelinesRotation = from.timelinesRotation.data

            from.totalAlpha = 0f
            for (i in 0 until timelineCount) {
                val timeline = timelines[i] as Timeline
                var direction = MixDirection.out
                val timelineBlend: MixBlend
                val alpha: Float
                when (timelineMode[i]) {
                    SUBSEQUENT -> {
                        if (!drawOrder && timeline is DrawOrderTimeline) continue
                        timelineBlend = blend
                        alpha = alphaMix
                    }
                    FIRST -> {
                        timelineBlend = MixBlend.setup
                        alpha = alphaMix
                    }
                    HOLD -> {
                        timelineBlend = MixBlend.setup
                        alpha = alphaHold
                    }
                    else // HOLD_MIX
                    -> {
                        timelineBlend = MixBlend.setup
                        val holdMix = timelineHoldMix[i]
                        alpha = alphaHold * max(0f, 1 - holdMix.mixTime / holdMix.mixDuration)
                    }
                }
                from.totalAlpha += alpha
                if (timeline is RotateTimeline) {
                    applyRotateTimeline(timeline, skeleton, animationTime, alpha, timelineBlend, timelinesRotation,
                            i shl 1, firstFrame)
                } else if (timeline is AttachmentTimeline)
                    applyAttachmentTimeline(timeline, skeleton, animationTime, timelineBlend, attachments)
                else {
                    if (drawOrder && timeline is DrawOrderTimeline && timelineBlend == MixBlend.setup)
                        direction = MixDirection.`in`
                    timeline.apply(skeleton, animationLast, animationTime, events, alpha, timelineBlend, direction)
                }
            }
        }

        if (to.mixDuration > 0) queueEvents(from, animationTime)
        this.events.clear()
        from.nextAnimationLast = animationTime
        from.nextTrackLast = from.trackTime

        return mix
    }

    /** Applies the attachment timeline and sets [Slot.attachmentState].
     * @param attachments False when: 1) the attachment timeline is mixing out, 2) mix < attachmentThreshold, and 3) the timeline
     * is not the last timeline to set the slot's attachment. In that case the timeline is applied only so subsequent
     * timelines see any deform.
     */
    private fun applyAttachmentTimeline(timeline: AttachmentTimeline, skeleton: Skeleton, time: Float, blend: MixBlend,
                                        attachments: Boolean) {

        val slot = skeleton.slots[timeline.slotIndex]
        if (!slot.bone.isActive) return

        val frames = timeline.frames
        if (time < frames[0]) { // Time is before first frame.
            if (blend == MixBlend.setup || blend == MixBlend.first)
                setAttachment(skeleton, slot, slot.data.attachmentName, attachments)
        } else {
            val frameIndex: Int
            if (time >= frames[frames.size - 1])
            // Time is after last frame.
                frameIndex = frames.size - 1
            else
                frameIndex = Animation.binarySearch(frames, time) - 1
            setAttachment(skeleton, slot, timeline.attachmentNames[frameIndex], attachments)
        }

        // If an attachment wasn't set (ie before the first frame or attachments is false), set the setup attachment later.
        if (slot.attachmentState <= unkeyedState) slot.attachmentState = unkeyedState + SETUP
    }

    private fun setAttachment(skeleton: Skeleton, slot: Slot, attachmentName: String?, attachments: Boolean) {
        slot.setAttachment(if (attachmentName == null) null else skeleton.getAttachment(slot.data.index, attachmentName))
        if (attachments) slot.attachmentState = unkeyedState + CURRENT
    }

    /** Applies the rotate timeline, mixing with the current pose while keeping the same rotation direction chosen as the shortest
     * the first time the mixing was applied.  */
    private fun applyRotateTimeline(timeline: RotateTimeline, skeleton: Skeleton, time: Float, alpha: Float, blend: MixBlend,
                                    timelinesRotation: FloatArray, i: Int, firstFrame: Boolean) {

        if (firstFrame) timelinesRotation[i] = 0f

        if (alpha == 1f) {
            timeline.apply(skeleton, 0f, time, null, 1f, blend, MixDirection.`in`)
            return
        }

        val bone = skeleton.bones[timeline.boneIndex]
        if (!bone.isActive) return
        val frames = timeline.frames
        var r1: Float
        var r2: Float
        if (time < frames[0]) { // Time is before first frame.
            when (blend) {
                Animation.MixBlend.setup -> {
                    bone.rotation = bone.data.rotation
                    return
                }
                Animation.MixBlend.first -> {
                    r1 = bone.rotation
                    r2 = bone.data.rotation
                }
                // Fall through.
                else -> return
            }
        } else {
            r1 = if (blend == MixBlend.setup) bone.data.rotation else bone.rotation
            if (time >= frames[frames.size - RotateTimeline.ENTRIES])
            // Time is after last frame.
                r2 = bone.data.rotation + frames[frames.size + RotateTimeline.PREV_ROTATION]
            else {
                // Interpolate between the previous frame and the current frame.
                val frame = Animation.binarySearch(frames, time, RotateTimeline.ENTRIES)
                val prevRotation = frames[frame + RotateTimeline.PREV_ROTATION]
                val frameTime = frames[frame]
                val percent = timeline.getCurvePercent((frame shr 1) - 1,
                        1 - (time - frameTime) / (frames[frame + RotateTimeline.PREV_TIME] - frameTime))

                r2 = frames[frame + RotateTimeline.ROTATION] - prevRotation
                r2 -= ((16384 - (16384.499999999996 - r2 / 360).toInt()) * 360).toFloat()
                r2 = prevRotation + r2 * percent + bone.data.rotation
                r2 -= ((16384 - (16384.499999999996 - r2 / 360).toInt()) * 360).toFloat()
            }
        }

        // Mix between rotations using the direction of the shortest route on the first frame.
        var total: Float
        var diff = r2 - r1
        diff -= ((16384 - (16384.499999999996 - diff / 360).toInt()) * 360).toFloat()
        if (diff == 0f)
            total = timelinesRotation[i]
        else {
            var lastTotal: Float
            val lastDiff: Float
            if (firstFrame) {
                lastTotal = 0f
                lastDiff = diff
            } else {
                lastTotal = timelinesRotation[i] // Angle and direction of mix, including loops.
                lastDiff = timelinesRotation[i + 1] // Difference between bones.
            }
            val current = diff > 0
            var dir = lastTotal >= 0
            // Detect cross at 0 (not 180).
            if ((lastDiff.sign) != (diff.sign) && kotlin.math.abs(lastDiff) <= 90) {
                // A cross after a 360 rotation is a loop.
                if (kotlin.math.abs(lastTotal) > 180) lastTotal += 360 * (lastTotal.sign)
                dir = current
            }
            total = diff + lastTotal - lastTotal % 360 // Store loops as part of lastTotal.
            if (dir != current) total += 360 * (lastTotal.sign)
            timelinesRotation[i] = total
        }
        timelinesRotation[i + 1] = diff
        r1 += total * alpha
        bone.rotation = r1 - (16384 - (16384.499999999996 - r1 / 360).toInt()) * 360
    }

    private fun queueEvents(entry: TrackEntry, animationTime: Float) {
        val animationStart = entry.animationStart
        val animationEnd = entry.animationEnd
        val duration = animationEnd - animationStart
        val trackLastWrapped = entry.trackLast % duration

        // Queue events before complete.
        val events = this.events
        var i = 0
        val n = events.size
        while (i < n) {
            val event = events.get(i)
            if (event.time < trackLastWrapped) break
            if (event.time > animationEnd) {
                i++
                continue
            } // Discard events outside animation start/end.
            queue.event(entry, event)
            i++
        }

        // Queue complete if completed a loop iteration or the animation.
        val complete: Boolean
        if (entry.loop)
            complete = duration == 0f || trackLastWrapped > entry.trackTime % duration
        else
            complete = animationTime >= animationEnd && entry.animationLast < animationEnd
        if (complete) queue.complete(entry)

        // Queue events after complete.
        while (i < n) {
            val event = events.get(i)
            if (event.time < animationStart) {
                i++
                continue
            } // Discard events outside animation start/end.
            queue.event(entry, events.get(i))
            i++
        }
    }

    /** Removes all animations from all tracks, leaving skeletons in their current pose.
     *
     *
     * It may be desired to use [AnimationState.setEmptyAnimations] to mix the skeletons back to the setup pose,
     * rather than leaving them in their current pose.  */
    fun clearTracks() {
        val oldDrainDisabled = queue.drainDisabled
        queue.drainDisabled = true
        var i = 0
        val n = tracks.size
        while (i < n) {
            clearTrack(i)
            i++
        }
        tracks.clear()
        queue.drainDisabled = oldDrainDisabled
        queue.drain()
    }

    /** Removes all animations from the track, leaving skeletons in their current pose.
     *
     *
     * It may be desired to use [AnimationState.setEmptyAnimation] to mix the skeletons back to the setup pose,
     * rather than leaving them in their current pose.  */
    fun clearTrack(trackIndex: Int) {
        require(trackIndex >= 0) { "trackIndex must be >= 0." }
        if (trackIndex >= tracks.size) return
        val current = tracks[trackIndex] ?: return

        queue.end(current)

        disposeNext(current)

        var entry: TrackEntry = current
        while (true) {
            val from = entry.mixingFrom ?: break
            queue.end(from)
            entry.mixingFrom = null
            entry.mixingTo = null
            entry = from
        }

        tracks.setAndGrow(current.trackIndex, null)

        queue.drain()
    }

    private fun setCurrent(index: Int, current: TrackEntry, interrupt: Boolean) {
        val from = expandToIndex(index)
        tracks.setAndGrow(index, current)

        if (from != null) {
            if (interrupt) queue.interrupt(from)
            current.mixingFrom = from
            from.mixingTo = current
            current.mixTime = 0f

            // Store the interrupted mix percentage.
            if (from.mixingFrom != null && from.mixDuration > 0)
                current.interruptAlpha *= kotlin.math.min(1f, from.mixTime / from.mixDuration)

            from.timelinesRotation.clear() // Reset rotation for mixing out, in case entry was mixed in.
        }

        queue.start(current)
    }

    /** Sets an animation by name.
     *
     *
     * [.setAnimation].  */
    fun setAnimation(trackIndex: Int, animationName: String, loop: Boolean): TrackEntry {
        val animation = data!!.skeletonData.findAnimation(animationName)
                ?: throw IllegalArgumentException("Animation not found: $animationName")
        return setAnimation(trackIndex, animation, loop)
    }

    /** Sets the current animation for a track, discarding any queued animations. If the formerly current track entry was never
     * applied to a skeleton, it is replaced (not mixed from).
     * @param loop If true, the animation will repeat. If false it will not, instead its last frame is applied if played beyond its
     * duration. In either case [TrackEntry.getTrackEnd] determines when the track is cleared.
     * @return A track entry to allow further customization of animation playback. References to the track entry must not be kept
     * after the [AnimationStateListener.dispose] event occurs.
     */
    fun setAnimation(trackIndex: Int, animation: Animation, loop: Boolean): TrackEntry {
        require(trackIndex >= 0) { "trackIndex must be >= 0." }
        var interrupt = true
        var current = expandToIndex(trackIndex)
        if (current != null) {
            if (current.nextTrackLast == -1f) {
                // Don't mix from an entry that was never applied.
                tracks.setAndGrow(trackIndex, current.mixingFrom)
                queue.interrupt(current)
                queue.end(current)
                disposeNext(current)
                current = current.mixingFrom
                interrupt = false // mixingFrom is current again, but don't interrupt it twice.
            } else
                disposeNext(current)
        }
        val entry = trackEntry(trackIndex, animation, loop, current)
        setCurrent(trackIndex, entry, interrupt)
        queue.drain()
        return entry
    }

    /** Queues an animation by name.
     *
     *
     * See [.addAnimation].  */
    fun addAnimation(trackIndex: Int, animationName: String, loop: Boolean, delay: Float): TrackEntry {
        val animation = data!!.skeletonData.findAnimation(animationName)
                ?: throw IllegalArgumentException("Animation not found: $animationName")
        return addAnimation(trackIndex, animation, loop, delay)
    }

    /** Adds an animation to be played after the current or last queued animation for a track. If the track is empty, it is
     * equivalent to calling [.setAnimation].
     * @param delay If > 0, sets [TrackEntry.getDelay]. If <= 0, the delay set is the duration of the previous track entry
     * minus any mix duration (from the [AnimationStateData]) plus the specified `delay` (ie the mix
     * ends at (`delay` = 0) or before (`delay` < 0) the previous track entry duration). If the
     * previous entry is looping, its next loop completion is used instead of its duration.
     * @return A track entry to allow further customization of animation playback. References to the track entry must not be kept
     * after the [AnimationStateListener.dispose] event occurs.
     */
    fun addAnimation(trackIndex: Int, animation: Animation, loop: Boolean, delay: Float): TrackEntry {
        var delay = delay
        require(trackIndex >= 0) { "trackIndex must be >= 0." }

        var last = expandToIndex(trackIndex)
        if (last != null) {
            while (last!!.next != null)
                last = last.next
        }

        val entry = trackEntry(trackIndex, animation, loop, last)

        if (last == null) {
            setCurrent(trackIndex, entry, true)
            queue.drain()
        } else {
            last.next = entry
            if (delay <= 0) {
                val duration = last.animationEnd - last.animationStart
                if (duration != 0f) {
                    if (last.loop)
                        delay += duration * (1 + (last.trackTime / duration).toInt()) // Completion of next loop.
                    else
                        delay += max(duration, last.trackTime) // After duration, else next update.
                    delay -= data!!.getMix(last.animation, animation)
                } else
                    delay = last.trackTime // Next update.
            }
        }

        entry.delay = delay
        return entry
    }

    /** Sets an empty animation for a track, discarding any queued animations, and sets the track entry's
     * [TrackEntry.getMixDuration]. An empty animation has no timelines and serves as a placeholder for mixing in or out.
     *
     *
     * Mixing out is done by setting an empty animation with a mix duration using either [.setEmptyAnimation],
     * [.setEmptyAnimations], or [.addEmptyAnimation]. Mixing to an empty animation causes
     * the previous animation to be applied less and less over the mix duration. Properties keyed in the previous animation
     * transition to the value from lower tracks or to the setup pose value if no lower tracks key the property. A mix duration of
     * 0 still mixes out over one frame.
     *
     *
     * Mixing in is done by first setting an empty animation, then adding an animation using
     * [.addAnimation] and on the returned track entry, set the
     * [TrackEntry.setMixDuration]. Mixing from an empty animation causes the new animation to be applied more and
     * more over the mix duration. Properties keyed in the new animation transition from the value from lower tracks or from the
     * setup pose value if no lower tracks key the property to the value keyed in the new animation.  */
    fun setEmptyAnimation(trackIndex: Int, mixDuration: Float): TrackEntry {
        val entry = setAnimation(trackIndex, emptyAnimation, false)
        entry.mixDuration = mixDuration
        entry.trackEnd = mixDuration
        return entry
    }

    /** Adds an empty animation to be played after the current or last queued animation for a track, and sets the track entry's
     * [TrackEntry.getMixDuration]. If the track is empty, it is equivalent to calling
     * [.setEmptyAnimation].
     *
     *
     * See [.setEmptyAnimation].
     * @param delay If > 0, sets [TrackEntry.getDelay]. If <= 0, the delay set is the duration of the previous track entry
     * minus any mix duration plus the specified `delay` (ie the mix ends at (`delay` = 0) or
     * before (`delay` < 0) the previous track entry duration). If the previous entry is looping, its next
     * loop completion is used instead of its duration.
     * @return A track entry to allow further customization of animation playback. References to the track entry must not be kept
     * after the [AnimationStateListener.dispose] event occurs.
     */
    fun addEmptyAnimation(trackIndex: Int, mixDuration: Float, delay: Float): TrackEntry {
        var delay = delay
        if (delay <= 0) delay -= mixDuration
        val entry = addAnimation(trackIndex, emptyAnimation, false, delay)
        entry.mixDuration = mixDuration
        entry.trackEnd = mixDuration
        return entry
    }

    /** Sets an empty animation for every track, discarding any queued animations, and mixes to it over the specified mix
     * duration.  */
    fun setEmptyAnimations(mixDuration: Float) {
        val oldDrainDisabled = queue.drainDisabled
        queue.drainDisabled = true
        var i = 0
        val n = tracks.size
        while (i < n) {
            val current = tracks[i]
            if (current != null) setEmptyAnimation(current.trackIndex, mixDuration)
            i++
        }
        queue.drainDisabled = oldDrainDisabled
        queue.drain()
    }

    private fun expandToIndex(index: Int): TrackEntry? {
        if (index < tracks.size) return tracks[index]
        tracks.ensureCapacity(index - tracks.size + 1)
        tracks.setSize(index + 1)
        return null
    }

    /** @param last May be null.
     */
    private fun trackEntry(trackIndex: Int, animation: Animation, loop: Boolean, last: TrackEntry?): TrackEntry {
        val entry = trackEntryPool.alloc()
        entry.trackIndex = trackIndex
        entry.animation = animation
        entry.loop = loop
        entry.holdPrevious = false

        entry.eventThreshold = 0f
        entry.attachmentThreshold = 0f
        entry.drawOrderThreshold = 0f

        entry.animationStart = 0f
        entry.animationEnd = animation.duration
        entry.animationLast = -1f
        entry.nextAnimationLast = -1f

        entry.delay = 0f
        entry.trackTime = 0f
        entry.trackLast = -1f
        entry.nextTrackLast = -1f
        entry.trackEnd = Float.MAX_VALUE
        entry.timeScale = 1f

        entry.alpha = 1f
        entry.interruptAlpha = 1f
        entry.mixTime = 0f
        entry.mixDuration = if (last == null) 0f else data!!.getMix(last.animation, animation)
        return entry
    }

    private fun disposeNext(entry: TrackEntry) {
        var next = entry.next
        while (next != null) {
            queue.dispose(next)
            next = next.next
        }
        entry.next = null
    }

    internal fun animationsChanged() {
        animationsChanged = false

        // Process in the order that animations are applied.
        propertyIDs.clear(2048)
        var i = 0
        val n = tracks.size
        while (i < n) {
            var entry: TrackEntry? = tracks[i]
            if (entry == null) {
                i++
                continue
            }
            while (entry!!.mixingFrom != null)
            // Move to last entry, then iterate in reverse.
                entry = entry.mixingFrom
            do {
                if (entry!!.mixingTo == null || entry.mixBlend != MixBlend.add) computeHold(entry)
                entry = entry.mixingTo
            } while (entry != null)
            i++
        }
    }

    private fun computeHold(entry: TrackEntry) {
        val to = entry.mixingTo
        val timelines = entry.animation!!.timelines
        val timelinesCount = entry.animation!!.timelines.size
        val timelineMode = entry.timelineMode.setSize(timelinesCount)
        entry.timelineHoldMix.clear()
        val timelineHoldMix = entry.timelineHoldMix.setSize(timelinesCount)
        val propertyIDs = this.propertyIDs

        if (to != null && to.holdPrevious) {
            for (i in 0 until timelinesCount) {
                propertyIDs.add((timelines[i] as Timeline).propertyId)
                timelineMode[i] = HOLD
            }
            return
        }

        outer@ for (i in 0 until timelinesCount) {
            val timeline = timelines[i] as Timeline
            val id = timeline.propertyId
            if (!propertyIDs.add(id))
                timelineMode[i] = SUBSEQUENT
            else if (to == null || timeline is AttachmentTimeline || timeline is DrawOrderTimeline
                    || timeline is EventTimeline || !to.animation!!.hasTimeline(id)) {
                timelineMode[i] = FIRST
            } else {
                var next = to.mixingTo
                while (next != null) {
                    if (next.animation!!.hasTimeline(id)) {
                        next = next.mixingTo
                        continue
                    }
                    if (next.mixDuration > 0) {
                        timelineMode[i] = HOLD_MIX
                        timelineHoldMix.setAndGrow(i, next)
                        continue@outer
                    }
                    break
                }
                timelineMode[i] = HOLD
            }
        }
    }

    /** Returns the track entry for the animation currently playing on the track, or null if no animation is currently playing.  */
    fun getCurrent(trackIndex: Int): TrackEntry? {
        require(trackIndex >= 0) { "trackIndex must be >= 0." }
        return if (trackIndex >= tracks.size) null else tracks[trackIndex]
    }

    /** Adds a listener to receive events for all track entries.  */
    fun addListener(listener: AnimationStateListener) {
        listeners.add(listener)
    }

    /** Removes the listener added with [.addListener].  */
    fun removeListener(listener: AnimationStateListener) {
        listeners.removeValueIdentity(listener)
    }

    /** Removes all listeners added with [.addListener].  */
    fun clearListeners() {
        listeners.clear()
    }

    /** Discards all listener notifications that have not yet been delivered. This can be useful to call from an
     * [AnimationStateListener] when it is known that further notifications that may have been already queued for delivery
     * are not wanted because new animations are being set.  */
    fun clearListenerNotifications() {
        queue.clear()
    }

    /** The AnimationStateData to look up mix durations.  */
    fun getData(): AnimationStateData? {
        return data
    }

    fun setData(data: AnimationStateData) {
        this.data = data
    }

    override fun toString(): String {
        val buffer = StringBuilder(64)
        var i = 0
        val n = tracks.size
        while (i < n) {
            val entry = tracks[i]
            if (entry == null) {
                i++
                continue
            }
            if (buffer.length > 0) buffer.append(", ")
            buffer.append(entry.toString())
            i++
        }
        return if (buffer.length == 0) "<none>" else buffer.toString()
    }

    /** Stores settings and other state for the playback of an animation on an [AnimationState] track.
     *
     *
     * References to a track entry must not be kept after the [AnimationStateListener.dispose] event occurs.  */
    class TrackEntry : Pool.Poolable {
        internal var animation: Animation? = null

        /** The animation queued to start after this animation, or null. `next` makes up a linked list.  */
        var next: TrackEntry? = null
            internal set

        /** The track entry for the previous animation when mixing from the previous animation to this animation, or null if no
         * mixing is currently occuring. When mixing from multiple animations, `mixingFrom` makes up a linked list.  */
        var mixingFrom: TrackEntry? = null
            internal set

        /** The track entry for the next animation when mixing from this animation to the next animation, or null if no mixing is
         * currently occuring. When mixing to multiple animations, `mixingTo` makes up a linked list.  */
        var mixingTo: TrackEntry? = null
            internal set
        /** The listener for events generated by this track entry, or null.
         *
         *
         * A track entry returned from [AnimationState.setAnimation] is already the current animation
         * for the track, so the track entry listener [AnimationStateListener.start] will not be called.  */
        /** @param listener May be null.
         */
        var listener: AnimationStateListener? = null

        /** The index of the track where this track entry is either current or queued.
         *
         *
         * See [AnimationState.getCurrent].  */
        var trackIndex: Int = 0
            internal set

        /** If true, the animation will repeat. If false it will not, instead its last frame is applied if played beyond its
         * duration.  */
        var loop: Boolean = false

        /** If true, when mixing from the previous animation to this animation, the previous animation is applied as normal instead
         * of being mixed out.
         *
         *
         * When mixing between animations that key the same property, if a lower track also keys that property then the value will
         * briefly dip toward the lower track value during the mix. This happens because the first animation mixes from 100% to 0%
         * while the second animation mixes from 0% to 100%. Setting `holdPrevious` to true applies the first animation
         * at 100% during the mix so the lower track value is overwritten. Such dipping does not occur on the lowest track which
         * keys the property, only when a higher track also keys the property.
         *
         *
         * Snapping will occur if `holdPrevious` is true and this animation does not key all the same properties as the
         * previous animation.  */
        var holdPrevious: Boolean = false

        /** When the mix percentage ([.getMixTime] / [.getMixDuration]) is less than the
         * `eventThreshold`, event timelines are applied while this animation is being mixed out. Defaults to 0, so event
         * timelines are not applied while this animation is being mixed out.  */
        var eventThreshold: Float = 0.toFloat()

        /** When the mix percentage ([.getMixTime] / [.getMixDuration]) is less than the
         * `attachmentThreshold`, attachment timelines are applied while this animation is being mixed out. Defaults to
         * 0, so attachment timelines are not applied while this animation is being mixed out.  */
        var attachmentThreshold: Float = 0.toFloat()

        /** When the mix percentage ([.getMixTime] / [.getMixDuration]) is less than the
         * `drawOrderThreshold`, draw order timelines are applied while this animation is being mixed out. Defaults to 0,
         * so draw order timelines are not applied while this animation is being mixed out.  */
        var drawOrderThreshold: Float = 0.toFloat()

        /** Seconds when this animation starts, both initially and after looping. Defaults to 0.
         *
         *
         * When changing the `animationStart` time, it often makes sense to set [.getAnimationLast] to the same
         * value to prevent timeline keys before the start time from triggering.  */
        var animationStart: Float = 0.toFloat()

        /** Seconds for the last frame of this animation. Non-looping animations won't play past this time. Looping animations will
         * loop back to [.getAnimationStart] at this time. Defaults to the animation [Animation.duration].  */
        var animationEnd: Float = 0.toFloat()
        internal var animationLast: Float = 0.toFloat()
        internal var nextAnimationLast: Float = 0.toFloat()

        /** Seconds to postpone playing the animation. When this track entry is the current track entry, `delay`
         * postpones incrementing the [.getTrackTime]. When this track entry is queued, `delay` is the time from
         * the start of the previous animation to when this track entry will become the current track entry (ie when the previous
         * track entry [TrackEntry.getTrackTime] >= this track entry's `delay`).
         *
         *
         * [.getTimeScale] affects the delay.  */
        var delay: Float = 0.toFloat()

        /** Current time in seconds this track entry has been the current track entry. The track time determines
         * [.getAnimationTime]. The track time can be set to start the animation at a time other than 0, without affecting
         * looping.  */
        var trackTime: Float = 0.toFloat()
        internal var trackLast: Float = 0.toFloat()
        internal var nextTrackLast: Float = 0.toFloat()

        /** The track time in seconds when this animation will be removed from the track. Defaults to the highest possible float
         * value, meaning the animation will be applied until a new animation is set or the track is cleared. If the track end time
         * is reached, no other animations are queued for playback, and mixing from any previous animations is complete, then the
         * properties keyed by the animation are set to the setup pose and the track is cleared.
         *
         *
         * It may be desired to use [AnimationState.addEmptyAnimation] rather than have the animation
         * abruptly cease being applied.  */
        var trackEnd: Float = 0.toFloat()

        /** Multiplier for the delta time when this track entry is updated, causing time for this animation to pass slower or
         * faster. Defaults to 1.
         *
         *
         * [.getMixTime] is not affected by track entry time scale, so [.getMixDuration] may need to be adjusted to
         * match the animation speed.
         *
         *
         * When using [AnimationState.addAnimation] with a `delay` <= 0, note the
         * [.getDelay] is set using the mix duration from the [AnimationStateData], assuming time scale to be 1. If
         * the time scale is not 1, the delay may need to be adjusted.
         *
         *
         * See AnimationState [AnimationState.getTimeScale] for affecting all animations.  */
        var timeScale: Float = 0.toFloat()

        /** Values < 1 mix this animation with the skeleton's current pose (usually the pose resulting from lower tracks). Defaults
         * to 1, which overwrites the skeleton's current pose with this animation.
         *
         *
         * Typically track 0 is used to completely pose the skeleton, then alpha is used on higher tracks. It doesn't make sense to
         * use alpha on track 0 if the skeleton pose is from the last frame render.  */
        var alpha: Float = 0.toFloat()

        /** Seconds from 0 to the [.getMixDuration] when mixing from the previous animation to this animation. May be
         * slightly more than `mixDuration` when the mix is complete.  */
        var mixTime: Float = 0.toFloat()

        /** Seconds for mixing from the previous animation to this animation. Defaults to the value provided by AnimationStateData
         * [AnimationStateData.getMix] based on the animation before this animation (if any).
         *
         *
         * A mix duration of 0 still mixes out over one frame to provide the track entry being mixed out a chance to revert the
         * properties it was animating.
         *
         *
         * The `mixDuration` can be set manually rather than use the value from
         * [AnimationStateData.getMix]. In that case, the `mixDuration` can be set for a new
         * track entry only before [AnimationState.update] is first called.
         *
         *
         * When using [AnimationState.addAnimation] with a `delay` <= 0, note the
         * [.getDelay] is set using the mix duration from the [AnimationStateData], not a mix duration set
         * afterward.  */
        var mixDuration: Float = 0.toFloat()
        internal var interruptAlpha: Float = 0.toFloat()
        internal var totalAlpha: Float = 0.toFloat()
        internal var mixBlend = MixBlend.replace

        internal val timelineMode = IntArrayList()
        internal val timelineHoldMix: FastArrayList<TrackEntry> = FastArrayList()
        internal val timelinesRotation = FloatArrayList()

        /** Uses [.getTrackTime] to compute the `animationTime`, which is between [.getAnimationStart]
         * and [.getAnimationEnd]. When the `trackTime` is 0, the `animationTime` is equal to the
         * `animationStart` time.  */
        val animationTime: Float
            get() {
                if (loop) {
                    val duration = animationEnd - animationStart
                    return if (duration == 0f) animationStart else trackTime % duration + animationStart
                }
                return kotlin.math.min(trackTime + animationStart, animationEnd)
            }

        /** Returns true if at least one loop has been completed.
         *
         *
         * See [AnimationStateListener.complete].  */
        val isComplete: Boolean
            get() = trackTime >= animationEnd - animationStart

        override fun reset() {
            next = null
            mixingFrom = null
            mixingTo = null
            animation = null
            listener = null
            timelineMode.clear()
            timelineHoldMix.clear()
            timelinesRotation.clear()
        }

        /** The animation to apply for this track entry.  */
        fun getAnimation(): Animation? {
            return animation
        }

        fun setAnimation(animation: Animation) {
            this.animation = animation
        }

        /** The time in seconds this animation was last applied. Some timelines use this for one-time triggers. Eg, when this
         * animation is applied, event timelines will fire all events between the `animationLast` time (exclusive) and
         * `animationTime` (inclusive). Defaults to -1 to ensure triggers on frame 0 happen the first time this animation
         * is applied.  */
        fun getAnimationLast(): Float {
            return animationLast
        }

        fun setAnimationLast(animationLast: Float) {
            this.animationLast = animationLast
            nextAnimationLast = animationLast
        }

        /** Controls how properties keyed in the animation are mixed with lower tracks. Defaults to [MixBlend.replace], which
         * replaces the values from the lower tracks with the animation values. [MixBlend.add] adds the animation values to
         * the values from the lower tracks.
         *
         *
         * The `mixBlend` can be set for a new track entry only before [AnimationState.apply] is first
         * called.  */
        fun getMixBlend(): MixBlend {
            return mixBlend
        }

        fun setMixBlend(mixBlend: MixBlend) {
            this.mixBlend = mixBlend
        }

        /** Resets the rotation directions for mixing this entry's rotate timelines. This can be useful to avoid bones rotating the
         * long way around when using [.alpha] and starting animations on other tracks.
         *
         *
         * Mixing with [MixBlend.replace] involves finding a rotation between two others, which has two possible solutions:
         * the short way or the long way around. The two rotations likely change over time, so which direction is the short or long
         * way also changes. If the short way was always chosen, bones would flip to the other side when that direction became the
         * long way. TrackEntry chooses the short way the first time it is applied and remembers that direction.  */
        fun resetRotationDirections() {
            timelinesRotation.clear()
        }

        override fun toString(): String {
            return if (animation == null) "<none>" else animation!!.name
        }
    }

    internal inner class EventQueue {
        private val objects = FastArrayList<Any>()
        var drainDisabled: Boolean = false

        fun start(entry: TrackEntry) {
            objects.add(EventType.start)
            objects.add(entry)
            animationsChanged = true
        }

        fun interrupt(entry: TrackEntry) {
            objects.add(EventType.interrupt)
            objects.add(entry)
        }

        fun end(entry: TrackEntry) {
            objects.add(EventType.end)
            objects.add(entry)
            animationsChanged = true
        }

        fun dispose(entry: TrackEntry) {
            objects.add(EventType.dispose)
            objects.add(entry)
        }

        fun complete(entry: TrackEntry) {
            objects.add(EventType.complete)
            objects.add(entry)
        }

        fun event(entry: TrackEntry, event: Event) {
            objects.add(EventType.event)
            objects.add(entry)
            objects.add(event)
        }

        fun drain() {
            if (drainDisabled) return  // Not reentrant.
            drainDisabled = true

            val objects = this.objects
            val listeners = this@AnimationState.listeners
            var i = 0
            while (i < objects.size) {
                val type = objects.get(i) as EventType
                val entry = objects.get(i + 1) as TrackEntry
                when (type) {
                    AnimationState.EventType.start -> {
                        if (entry.listener != null) entry.listener!!.start(entry)
                        for (ii in 0 until listeners.size)
                            listeners[ii].start(entry)
                    }
                    AnimationState.EventType.interrupt -> {
                        if (entry.listener != null) entry.listener!!.interrupt(entry)
                        for (ii in 0 until listeners.size)
                            listeners[ii].interrupt(entry)
                    }
                    AnimationState.EventType.end -> {
                        if (entry.listener != null) entry.listener!!.end(entry)
                        for (ii in 0 until listeners.size)
                            listeners[ii].end(entry)
                        if (entry.listener != null) entry.listener!!.dispose(entry)
                        for (ii in 0 until listeners.size)
                            listeners[ii].dispose(entry)
                        trackEntryPool.free(entry)
                    }
                    // Fall through.
                    AnimationState.EventType.dispose -> {
                        if (entry.listener != null) entry.listener!!.dispose(entry)
                        for (ii in 0 until listeners.size)
                            listeners[ii].dispose(entry)
                        trackEntryPool.free(entry)
                    }
                    AnimationState.EventType.complete -> {
                        if (entry.listener != null) entry.listener!!.complete(entry)
                        for (ii in 0 until listeners.size)
                            listeners[ii].complete(entry)
                    }
                    AnimationState.EventType.event -> {
                        val event = objects.get(i++ + 2) as Event
                        if (entry.listener != null) entry.listener!!.event(entry, event)
                        for (ii in 0 until listeners.size)
                            listeners[ii].event(entry, event)
                    }
                }
                i += 2
            }
            clear()

            drainDisabled = false
        }

        fun clear() {
            objects.clear()
        }
    }

    private enum class EventType {
        start, interrupt, end, dispose, complete, event
    }

    /** The interface to implement for receiving TrackEntry events. It is always safe to call AnimationState methods when receiving
     * events.
     *
     *
     * See TrackEntry [TrackEntry.setListener] and AnimationState
     * [AnimationState.addListener].  */
    interface AnimationStateListener {
        /** Invoked when this entry has been set as the current entry.  */
        fun start(entry: TrackEntry)

        /** Invoked when another entry has replaced this entry as the current entry. This entry may continue being applied for
         * mixing.  */
        fun interrupt(entry: TrackEntry)

        /** Invoked when this entry is no longer the current entry and will never be applied again.  */
        fun end(entry: TrackEntry)

        /** Invoked when this entry will be disposed. This may occur without the entry ever being set as the current entry.
         * References to the entry should not be kept after `dispose` is called, as it may be destroyed or reused.  */
        fun dispose(entry: TrackEntry)

        /** Invoked every time this entry's animation completes a loop. Because this event is trigged in
         * [AnimationState.apply], any animations set in response to the event won't be applied until the next time
         * the AnimationState is applied.  */
        fun complete(entry: TrackEntry)

        /** Invoked when this entry's animation triggers an event. Because this event is trigged in
         * [AnimationState.apply], any animations set in response to the event won't be applied until the next time
         * the AnimationState is applied.  */
        fun event(entry: TrackEntry, event: Event)
    }

    abstract class AnimationStateAdapter : AnimationStateListener {
        override fun start(entry: TrackEntry) {}

        override fun interrupt(entry: TrackEntry) {}

        override fun end(entry: TrackEntry) {}

        override fun dispose(entry: TrackEntry) {}

        override fun complete(entry: TrackEntry) {}

        override fun event(entry: TrackEntry, event: Event) {}
    }

    companion object {
        private val emptyAnimation = Animation("<empty>", FastArrayList(0), 0f)

        /** 1) A previously applied timeline has set this property.<br></br>
         * Result: Mix from the current pose to the timeline pose.  */
        private val SUBSEQUENT = 0

        /** 1) This is the first timeline to set this property.<br></br>
         * 2) The next track entry applied after this one does not have a timeline to set this property.<br></br>
         * Result: Mix from the setup pose to the timeline pose.  */
        private val FIRST = 1

        /** 1) This is the first timeline to set this property.<br></br>
         * 2) The next track entry to be applied does have a timeline to set this property.<br></br>
         * 3) The next track entry after that one does not have a timeline to set this property.<br></br>
         * Result: Mix from the setup pose to the timeline pose, but do not mix out. This avoids "dipping" when crossfading animations
         * that key the same property. A subsequent timeline will set this property using a mix.  */
        private val HOLD = 2

        /** 1) This is the first timeline to set this property.<br></br>
         * 2) The next track entry to be applied does have a timeline to set this property.<br></br>
         * 3) The next track entry after that one does have a timeline to set this property.<br></br>
         * 4) timelineHoldMix stores the first subsequent track entry that does not have a timeline to set this property.<br></br>
         * Result: The same as HOLD except the mix percentage from the timelineHoldMix track entry is used. This handles when more than
         * 2 track entries in a row have a timeline that sets the same property.<br></br>
         * Eg, A -> B -> C -> D where A, B, and C have a timeline setting same property, but D does not. When A is applied, to avoid
         * "dipping" A is not mixed out, however D (the first entry that doesn't set the property) mixing in is used to mix out A
         * (which affects B and C). Without using D to mix out, A would be applied fully until mixing completes, then snap into
         * place.  */
        private val HOLD_MIX = 3

        private val SETUP = 1
        private val CURRENT = 2
    }
}
