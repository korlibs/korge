package com.dragonbones.model

/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012-2018 DragonBones team and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import com.dragonbones.core.*
import com.dragonbones.util.*
import com.soywiz.kds.*
import kotlin.math.*

/**
 * - The animation data.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 动画数据。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class AnimationData(pool: SingleObjectPool<AnimationData>) :  BaseObject(pool) {
	override fun toString(): String {
		return "[class dragonBones.AnimationData]"
	}

	/**
	 * - FrameIntArray.
	 * @internal
	 */
	var frameIntOffset: Int = 0
	/**
	 * - FrameFloatArray.
	 * @internal
	 */
	var frameFloatOffset: Int = 0
	/**
	 * - FrameArray.
	 * @internal
	 */
	var frameOffset: Int = 0
	/**
	 * @private
	 */
	var blendType: AnimationBlendType = AnimationBlendType.None
	/**
	 * - The frame count of the animation.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画的帧数。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var frameCount: Int = 0
	/**
	 * - The play times of the animation. [0: Loop play, [1~N]: Play N times]
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画的播放次数。 [0: 无限循环播放, [1~N]: 循环播放 N 次]
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var playTimes: Int = 0
	/**
	 * - The duration of the animation. (In seconds)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画的持续时间。 （以秒为单位）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var duration: Double = 0.0
	/**
	 * @private
	 */
	var scale: Double = 1.0
	/**
	 * - The fade in time of the animation. (In seconds)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画的淡入时间。 （以秒为单位）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var fadeInTime: Double = 0.0
	/**
	 * @private
	 */
	var cacheFrameRate: Double = 0.0
	/**
	 * - The animation name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var name: String = ""
	/**
	 * @private
	 */
	val cachedFrames: FastArrayList<Boolean> = FastArrayList()
	/**
	 * @private
	 */
	val boneTimelines: FastStringMap<FastArrayList<TimelineData>> = FastStringMap()
	/**
	 * @private
	 */
	val slotTimelines: FastStringMap<FastArrayList<TimelineData>> = FastStringMap()
	/**
	 * @private
	 */
	val constraintTimelines: FastStringMap<FastArrayList<TimelineData>> = FastStringMap()
	/**
	 * @private
	 */
	val animationTimelines: FastStringMap<FastArrayList<TimelineData>> = FastStringMap()
	/**
	 * @private
	 */
	val boneCachedFrameIndices: FastStringMap<IntArrayList> = FastStringMap()
	/**
	 * @private
	 */
	val slotCachedFrameIndices: FastStringMap<IntArrayList> = FastStringMap()
	/**
	 * @private
	 */
	var actionTimeline: TimelineData? = null // Initial value.
	/**
	 * @private
	 */
	var zOrderTimeline: TimelineData? = null // Initial value.
	/**
	 * @private
	 */
	var parent: ArmatureData? = null

	override fun _onClear() {
		this.boneTimelines.fastValueForEach { tl ->
			tl.fastForEach { timeline ->
				timeline.returnToPool()
			}
		}
		this.boneTimelines.clear()

		this.slotTimelines.fastValueForEach { tl ->
			tl.fastForEach { timeline ->
				timeline.returnToPool()
			}
		}
		this.slotTimelines.clear()

		this.constraintTimelines.fastValueForEach { tl ->
			tl.fastForEach { timeline ->
				timeline.returnToPool()
			}
		}
		this.constraintTimelines.clear()

		this.animationTimelines.fastValueForEach { tl ->
			tl.fastForEach { timeline ->
				timeline.returnToPool()
			}
		}
		this.animationTimelines.clear()

		this.boneCachedFrameIndices.clear()
		this.slotCachedFrameIndices.clear()

		this.actionTimeline?.returnToPool()
		this.zOrderTimeline?.returnToPool()

		this.frameIntOffset = 0
		this.frameFloatOffset = 0
		this.frameOffset = 0
		this.blendType = AnimationBlendType.None
		this.frameCount = 0
		this.playTimes = 0
		this.duration = 0.0
		this.scale = 1.0
		this.fadeInTime = 0.0
		this.cacheFrameRate = 0.0
		this.name = ""
		this.cachedFrames.length = 0
		// this.boneTimelines.clear();
		// this.slotTimelines.clear();
		// this.constraintTimelines.clear();
		// this.animationTimelines.clear();
		// this.boneCachedFrameIndices.clear();
		// this.slotCachedFrameIndices.clear();
		this.actionTimeline = null
		this.zOrderTimeline = null
		this.parent = null //
	}

	/**
	 * @internal
	 */
	fun cacheFrames(frameRate: Int) {
		if (this.cacheFrameRate > 0.0) { // TODO clear cache.
			return
		}

		this.cacheFrameRate = max(ceil(frameRate * this.scale), 1.0)
		val cacheFrameCount = ceil(this.cacheFrameRate * this.duration).toInt() + 1 // Cache one more frame.

		this.cachedFrames.length = cacheFrameCount
		for (i in 0 until this.cachedFrames.length) {
			this.cachedFrames[i] = false
		}

		this.parent!!.sortedBones.fastForEach { bone ->
			val indices = IntArrayList(cacheFrameCount)
			for (i in 0 until indices.length) {
				indices[i] = -1
			}

			this.boneCachedFrameIndices[bone.name] = indices
		}

		this.parent!!.sortedSlots.fastForEach { slot ->
			//val indices =  DoubleArray(cacheFrameCount)
			val indices =  IntArrayList(cacheFrameCount)
			for (i in 0 until indices.length) {
				indices[i] = -1
			}

			this.slotCachedFrameIndices[slot.name] = indices
		}
	}

	/**
	 * @private
	 */
	fun addBoneTimeline(timelineName: String, timeline: TimelineData) {
		val timelines = this.boneTimelines.getOrPut(timelineName) { FastArrayList() }
		if (timelines.indexOf(timeline) < 0) {
			timelines.add(timeline)
		}
	}

	/**
	 * @private
	 */
	fun addSlotTimeline(timelineName: String, timeline: TimelineData) {
		val timelines = this.slotTimelines.getOrPut(timelineName) { FastArrayList() }
		if (timelines.indexOf(timeline) < 0) {
			timelines.add(timeline)
		}
	}

	/**
	 * @private
	 */
	fun addConstraintTimeline(timelineName: String, timeline: TimelineData) {
		val timelines = this.constraintTimelines.getOrPut(timelineName) { FastArrayList() }
		if (timelines.indexOf(timeline) < 0) {
			timelines.add(timeline)
		}
	}

	/**
	 * @private
	 */
	fun addAnimationTimeline(timelineName: String, timeline: TimelineData) {
		val timelines = this.animationTimelines.getOrPut(timelineName) { FastArrayList() }
		if (timelines.indexOf(timeline) < 0) {
			timelines.add(timeline)
		}
	}

	/**
	 * @private
	 */
	fun getBoneTimelines(timelineName: String): FastArrayList<TimelineData>? = this.boneTimelines[timelineName]

	/**
	 * @private
	 */
	fun getSlotTimelines(timelineName: String): FastArrayList<TimelineData>? = this.slotTimelines[timelineName]

	/**
	 * @private
	 */
	fun getConstraintTimelines(timelineName: String): FastArrayList<TimelineData>? = this.constraintTimelines[timelineName]

	/**
	 * @private
	 */
	fun getAnimationTimelines(timelineName: String): FastArrayList<TimelineData>? = this.animationTimelines[timelineName]

	/**
	 * @private
	 */
	fun getBoneCachedFrameIndices(boneName: String):  IntArrayList? = this.boneCachedFrameIndices[boneName]

	/**
	 * @private
	 */
	fun getSlotCachedFrameIndices(slotName: String):  IntArrayList? = this.slotCachedFrameIndices[slotName]
}
/**
 * @private
 */
open class TimelineData(pool: SingleObjectPool<out TimelineData>) : BaseObject(pool) {
	override fun toString(): String {
		return "[class dragonBones.TimelineData]"
	}

	var type: TimelineType = TimelineType.BoneAll
	var offset: Int = 0 // TimelineArray.
	var frameIndicesOffset: Int = -1 // FrameIndices.

	override fun _onClear() {
		this.type = TimelineType.BoneAll
		this.offset = 0
		this.frameIndicesOffset = -1
	}
}
/**
 * @internal
 */
class AnimationTimelineData(pool: SingleObjectPool<AnimationTimelineData>) :  TimelineData(pool) {
	override fun toString(): String {
		return "[class dragonBones.AnimationTimelineData]"
	}

	var x: Double = 0.0
	var y: Double = 0.0

	override fun _onClear() {
		super._onClear()

		this.x = 0.0
		this.y = 0.0
	}
}
