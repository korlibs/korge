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
package com.dragonbones.animation

import com.dragonbones.armature.*
import com.dragonbones.core.*
import com.dragonbones.event.*
import com.dragonbones.model.*
import com.soywiz.kmem.*
import kotlin.math.*

/**
 * @internal
 */
class ActionTimelineState(pool: SingleObjectPool<ActionTimelineState>) : TimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.ActionTimelineState]"
	}

	private fun _onCrossFrame(frameIndex: Int) {
		val eventDispatcher = this._armature!!.eventDispatcher
		if (this._animationState!!.actionEnabled) {
			val frameOffset =
				this._animationData!!.frameOffset + this._timelineArray!![(this._timelineData!!).offset + BinaryOffset.TimelineFrameOffset + frameIndex].toInt()
			val actionCount = this._frameArray!![frameOffset + 1].toInt()
			val actions =
				this._animationData!!.parent!!.actions // May be the animaton data not belong to this armature data.

			//for (var i = 0; i < actionCount; ++i) {
			for (i in 0 until actionCount) {
				val actionIndex = this._frameArray!![frameOffset + 2 + i].toInt()
				val action = actions[actionIndex]

				if (action.type == ActionType.Play) {
					val eventObject = pool.eventObject.borrow()
					// eventObject.time = this._frameArray[frameOffset] * this._frameRateR; // Precision problem
					eventObject.time = this._frameArray!![frameOffset].toDouble() / this._frameRate
					eventObject.animationState = this._animationState!!
					EventObject.actionDataToInstance(action, eventObject, this._armature!!)
					this._armature!!._bufferAction(eventObject, true)
				} else {
					val eventType =
						if (action.type == ActionType.Frame) EventObject.FRAME_EVENT else EventObject.SOUND_EVENT
					if (action.type == ActionType.Sound || eventDispatcher.hasDBEventListener(eventType)) {
						val eventObject = pool.eventObject.borrow()
						// eventObject.time = this._frameArray[frameOffset] * this._frameRateR; // Precision problem
						eventObject.time = this._frameArray!![frameOffset].toDouble() / this._frameRate
						eventObject.animationState = this._animationState!!
						EventObject.actionDataToInstance(action, eventObject, this._armature!!)
						this._armature?.eventDispatcher?.queueEvent(eventObject)
					}
				}
			}
		}
	}

	override fun _onArriveAtFrame() {}
	override fun _onUpdateFrame() {}

	override fun update(passedTime: Double) {
		val prevState = this.playState
		var prevPlayTimes = this.currentPlayTimes
		val prevTime = this._currentTime

		if (this._setCurrentTime(passedTime)) {
			val eventActive = this._animationState?._parent == null && this._animationState!!.actionEnabled
			val eventDispatcher = this._armature?.eventDispatcher
			if (prevState < 0) {
				if (this.playState != prevState) {
					if (this._animationState!!.displayControl && this._animationState!!.resetToPose) { // Reset zorder to pose.
						this._armature?._sortZOrder(null, 0)
					}

					prevPlayTimes = this.currentPlayTimes

					if (eventActive && eventDispatcher!!.hasDBEventListener(EventObject.START)) {
						val eventObject = pool.eventObject.borrow()
						eventObject.type = EventObject.START
						eventObject.armature = this._armature!!
						eventObject.animationState = this._animationState!!
						this._armature?.eventDispatcher?.queueEvent(eventObject)
					}
				} else {
					return
				}
			}

			val isReverse = this._animationState!!.timeScale < 0.0
			var loopCompleteEvent: EventObject? = null
			var completeEvent: EventObject? = null

			if (eventActive && this.currentPlayTimes != prevPlayTimes) {
				if (eventDispatcher!!.hasDBEventListener(EventObject.LOOP_COMPLETE)) {
					loopCompleteEvent = pool.eventObject.borrow()
					loopCompleteEvent.type = EventObject.LOOP_COMPLETE
					loopCompleteEvent.armature = this._armature!!
					loopCompleteEvent.animationState = this._animationState!!
				}

				if (this.playState > 0) {
					if (eventDispatcher.hasDBEventListener(EventObject.COMPLETE)) {
						completeEvent = pool.eventObject.borrow()
						completeEvent.type = EventObject.COMPLETE
						completeEvent.armature = this._armature!!
						completeEvent.animationState = this._animationState!!
					}
				}
			}

			if (this._frameCount > 1) {
				val timelineData = this._timelineData as TimelineData
				val timelineFrameIndex = floor(this._currentTime * this._frameRate).toInt() // uint
				val frameIndex = this._frameIndices!![timelineData.frameIndicesOffset + timelineFrameIndex]

				if (this._frameIndex != frameIndex) { // Arrive at frame.
					var crossedFrameIndex = this._frameIndex
					this._frameIndex = frameIndex

					if (this._timelineArray != null) {
						this._frameOffset = this._animationData!!.frameOffset +
								this._timelineArray!![timelineData.offset + BinaryOffset.TimelineFrameOffset + this._frameIndex].toInt()

						if (isReverse) {
							if (crossedFrameIndex < 0) {
								val prevFrameIndex = floor(prevTime * this._frameRate).toInt()
								crossedFrameIndex =
									this._frameIndices!![timelineData.frameIndicesOffset + prevFrameIndex]

								if (this.currentPlayTimes == prevPlayTimes) { // Start.
									if (crossedFrameIndex == frameIndex) { // Uncrossed.
										crossedFrameIndex = -1
									}
								}
							}

							while (crossedFrameIndex >= 0) {
								val frameOffset =
									this._animationData!!.frameOffset + this._timelineArray!![timelineData.offset + BinaryOffset.TimelineFrameOffset + crossedFrameIndex].toInt()
								// val framePosition = this._frameArray[frameOffset] * this._frameRateR; // Precision problem
								val framePosition = this._frameArray!![frameOffset] / this._frameRate

								if (
									this._position <= framePosition &&
									framePosition <= this._position + this._duration
								) { // Support interval play.
									this._onCrossFrame(crossedFrameIndex)
								}

								if (loopCompleteEvent != null && crossedFrameIndex == 0) { // Add loop complete event after first frame.
									this._armature?.eventDispatcher?.queueEvent(loopCompleteEvent)
									loopCompleteEvent = null
								}

								if (crossedFrameIndex > 0) {
									crossedFrameIndex--
								} else {
									crossedFrameIndex = this._frameCount - 1
								}

								if (crossedFrameIndex == frameIndex) {
									break
								}
							}
						} else {
							if (crossedFrameIndex < 0) {
								val prevFrameIndex = floor(prevTime * this._frameRate).toInt()
								crossedFrameIndex =
									this._frameIndices!![timelineData.frameIndicesOffset + prevFrameIndex]
								val frameOffset =
									this._animationData!!.frameOffset + this._timelineArray!![timelineData.offset + BinaryOffset.TimelineFrameOffset + crossedFrameIndex].toInt()
								// val framePosition = this._frameArray[frameOffset] * this._frameRateR; // Precision problem
								val framePosition = this._frameArray!![frameOffset].toDouble() / this._frameRate

								if (this.currentPlayTimes == prevPlayTimes) { // Start.
									if (prevTime <= framePosition) { // Crossed.
										if (crossedFrameIndex > 0) {
											crossedFrameIndex--
										} else {
											crossedFrameIndex = this._frameCount - 1
										}
									} else if (crossedFrameIndex == frameIndex) { // Uncrossed.
										crossedFrameIndex = -1
									}
								}
							}

							while (crossedFrameIndex >= 0) {
								if (crossedFrameIndex < this._frameCount - 1) {
									crossedFrameIndex++
								} else {
									crossedFrameIndex = 0
								}

								val frameOffset =
									this._animationData!!.frameOffset + this._timelineArray!![timelineData.offset + BinaryOffset.TimelineFrameOffset + crossedFrameIndex].toInt()
								// val framePosition = this._frameArray[frameOffset] * this._frameRateR; // Precision problem
								val framePosition = this._frameArray!![frameOffset].toDouble() / this._frameRate

								if (
									this._position <= framePosition &&
									framePosition <= this._position + this._duration //
								) { // Support interval play.
									this._onCrossFrame(crossedFrameIndex)
								}

								if (loopCompleteEvent != null && crossedFrameIndex == 0) { // Add loop complete event before first frame.
									this._armature?.eventDispatcher?.queueEvent(loopCompleteEvent)
									loopCompleteEvent = null
								}

								if (crossedFrameIndex == frameIndex) {
									break
								}
							}
						}
					}
				}
			} else if (this._frameIndex < 0) {
				this._frameIndex = 0
				if (this._timelineData != null) {
					this._frameOffset = this._animationData!!.frameOffset +
							this._timelineArray!![this._timelineData!!.offset + BinaryOffset.TimelineFrameOffset].toInt()
					// Arrive at frame.
					val framePosition = this._frameArray!![this._frameOffset].toDouble() / this._frameRate

					if (this.currentPlayTimes == prevPlayTimes) { // Start.
						if (prevTime <= framePosition) {
							this._onCrossFrame(this._frameIndex)
						}
					} else if (this._position <= framePosition) { // Loop complete.
						if (!isReverse && loopCompleteEvent != null) { // Add loop complete event before first frame.
							this._armature?.eventDispatcher?.queueEvent(loopCompleteEvent)
							loopCompleteEvent = null
						}

						this._onCrossFrame(this._frameIndex)
					}
				}
			}

			if (loopCompleteEvent != null) {
				this._armature?.eventDispatcher?.queueEvent(loopCompleteEvent)
			}

			if (completeEvent != null) {
				this._armature?.eventDispatcher?.queueEvent(completeEvent)
			}
		}
	}

	fun setCurrentTime(value: Double) {
		this._setCurrentTime(value)
		this._frameIndex = -1
	}
}

/**
 * @internal
 */
class ZOrderTimelineState(pool: SingleObjectPool<ZOrderTimelineState>) : TimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.ZOrderTimelineState]"
	}

	override fun _onArriveAtFrame() {
		if (this.playState >= 0) {
			val count = this._frameArray!![this._frameOffset + 1].toInt()
			if (count > 0) {
				this._armature?._sortZOrder(this._frameArray!!, this._frameOffset + 2)
			} else {
				this._armature?._sortZOrder(null, 0)
			}
		}
	}

	override fun _onUpdateFrame() {}
}

/**
 * @internal
 */
class BoneAllTimelineState(pool: SingleObjectPool<BoneAllTimelineState>) : MutilpleValueTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.BoneAllTimelineState]"
	}

	override fun _onArriveAtFrame() {
		super._onArriveAtFrame()

		if (this._isTween && this._frameIndex == this._frameCount - 1) {
			this._rd[2] = TransformDb.normalizeRadian(this._rd[2])
			this._rd[3] = TransformDb.normalizeRadian(this._rd[3])
		}

		if (this._timelineData == null) { // Pose.
			this._rd[4] = 1.0
			this._rd[5] = 1.0
		}
	}

	override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		super.init(armature, animationState, timelineData)

		this._valueOffset = this._animationData!!.frameFloatOffset
		this._valueCount = 6
		this._valueArray = this._animationData?.parent?.parent?.frameFloatArray!!
	}

	override fun fadeOut() {
		this.dirty = false
		this._rd[2] = TransformDb.normalizeRadian(this._rd[2])
		this._rd[3] = TransformDb.normalizeRadian(this._rd[3])
	}

	override fun blend(_isDirty: Boolean) {
		val valueScale = this._armature!!.armatureData.scale
		val rd = this._rd
		//
		val blendState = this.targetBlendState!!
		val bone = blendState.targetBone!!
		val blendWeight = blendState.blendWeight
		val result = bone.animationPose

		if (blendState.dirty > 1) {
			result.xf += (rd[0] * blendWeight * valueScale).toFloat()
			result.yf += (rd[1] * blendWeight * valueScale).toFloat()
			result.rotation += (rd[2] * blendWeight).toFloat()
			result.skew += (rd[3] * blendWeight).toFloat()
			result.scaleX += ((rd[4] - 1.0) * blendWeight).toFloat()
			result.scaleY += ((rd[5] - 1.0) * blendWeight).toFloat()
		} else {
			result.xf = (rd[0] * blendWeight * valueScale).toFloat()
			result.yf = (rd[1] * blendWeight * valueScale).toFloat()
			result.rotation = (rd[2] * blendWeight).toFloat()
			result.skew = (rd[3] * blendWeight).toFloat()
			result.scaleX = ((rd[4] - 1.0) * blendWeight + 1.0).toFloat() //
			result.scaleY = ((rd[5] - 1.0) * blendWeight + 1.0).toFloat() //
		}

		if (_isDirty || this.dirty) {
			this.dirty = false
			bone._transformDirty = true
		}
	}
}

/**
 * @internal
 */
class BoneTranslateTimelineState(pool: SingleObjectPool<BoneTranslateTimelineState>) : DoubleValueTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.BoneTranslateTimelineState]"
	}

	override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		super.init(armature, animationState, timelineData)

		this._valueOffset = this._animationData!!.frameFloatOffset
		this._valueScale = this._armature!!.armatureData.scale
		this._valueArray = this._animationData!!.parent!!.parent!!.frameFloatArray!!
	}

	override fun blend(_isDirty: Boolean) {
		val blendState = this.targetBlendState!!
		val bone = blendState.targetBone!!
		val blendWeight = blendState.blendWeight
		val result = bone.animationPose

		when {
			blendState.dirty > 1 -> {
				result.xf += (this._resultA * blendWeight).toFloat()
				result.yf += (this._resultB * blendWeight).toFloat()
			}
			blendWeight != 1.0 -> {
				result.xf = (this._resultA * blendWeight).toFloat()
				result.yf = (this._resultB * blendWeight).toFloat()
			}
			else -> {
				result.xf = this._resultA.toFloat()
				result.yf = this._resultB.toFloat()
			}
		}

		if (_isDirty || this.dirty) {
			this.dirty = false
			bone._transformDirty = true
		}
	}
}

/**
 * @internal
 */
class BoneRotateTimelineState(pool: SingleObjectPool<BoneRotateTimelineState>) : DoubleValueTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.BoneRotateTimelineState]"
	}

	override fun _onArriveAtFrame() {
		super._onArriveAtFrame()

		if (this._isTween && this._frameIndex == this._frameCount - 1) {
			this._differenceA = TransformDb.normalizeRadian(this._differenceA)
			this._differenceB = TransformDb.normalizeRadian(this._differenceB)
		}
	}

	override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		super.init(armature, animationState, timelineData)

		this._valueOffset = this._animationData!!.frameFloatOffset
		this._valueArray = this._animationData!!.parent!!.parent!!.frameFloatArray!!
	}

	override fun fadeOut() {
		this.dirty = false
		this._resultA = TransformDb.normalizeRadian(this._resultA)
		this._resultB = TransformDb.normalizeRadian(this._resultB)
	}

	override fun blend(_isDirty: Boolean) {
		val blendState = this.targetBlendState!!
		val bone = blendState.targetBone!!
		val blendWeight = blendState.blendWeight
		val result = bone.animationPose

		when {
			blendState.dirty > 1 -> {
				result.rotation += (this._resultA * blendWeight).toFloat()
				result.skew += (this._resultB * blendWeight).toFloat()
			}
			blendWeight != 1.0 -> {
				result.rotation = (this._resultA * blendWeight).toFloat()
				result.skew = (this._resultB * blendWeight).toFloat()
			}
			else -> {
				result.rotation = this._resultA.toFloat()
				result.skew = this._resultB.toFloat()
			}
		}

		if (_isDirty || this.dirty) {
			this.dirty = false
			bone._transformDirty = true
		}
	}
}

/**
 * @internal
 */
class BoneScaleTimelineState(pool: SingleObjectPool<BoneScaleTimelineState>) : DoubleValueTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.BoneScaleTimelineState]"
	}

	override fun _onArriveAtFrame() {
		super._onArriveAtFrame()

		if (this._timelineData == null) { // Pose.
			this._resultA = 1.0
			this._resultB = 1.0
		}
	}

	override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		super.init(armature, animationState, timelineData)

		this._valueOffset = this._animationData!!.frameFloatOffset
		this._valueArray = this._animationData!!.parent!!.parent!!.frameFloatArray!!
	}

	override fun blend(_isDirty: Boolean) {
		val blendState = this.targetBlendState!!
		val bone = blendState.targetBone!!
		val blendWeight = blendState.blendWeight
		val result = bone.animationPose

		when {
			blendState.dirty > 1 -> {
				result.scaleX += ((this._resultA - 1.0) * blendWeight).toFloat()
				result.scaleY += ((this._resultB - 1.0) * blendWeight).toFloat()
			}
			blendWeight != 1.0 -> {
				result.scaleX = ((this._resultA - 1.0) * blendWeight + 1.0).toFloat()
				result.scaleY = ((this._resultB - 1.0) * blendWeight + 1.0).toFloat()
			}
			else -> {
				result.scaleX = this._resultA.toFloat()
				result.scaleY = this._resultB.toFloat()
			}
		}

		if (_isDirty || this.dirty) {
			this.dirty = false
			bone._transformDirty = true
		}
	}
}

/**
 * @internal
 */
class SurfaceTimelineState(pool: SingleObjectPool<SurfaceTimelineState>) : MutilpleValueTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.SurfaceTimelineState]"
	}

	private var _deformCount: Int = 0
	private var _deformOffset: Int = 0
	private var _sameValueOffset: Int = 0

	override fun _onClear() {
		super._onClear()

		this._deformCount = 0
		this._deformOffset = 0
		this._sameValueOffset = 0
	}

	override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		super.init(armature, animationState, timelineData)

		if (this._timelineData != null) {
			val dragonBonesData = this._animationData!!.parent!!.parent
			val frameIntArray = dragonBonesData!!.frameIntArray!!
			val frameIntOffset =
				this._animationData!!.frameIntOffset + this._timelineArray!![this._timelineData!!.offset + BinaryOffset.TimelineFrameValueCount].toInt()
			this._valueOffset = this._animationData!!.frameFloatOffset
			this._valueCount = frameIntArray[frameIntOffset + BinaryOffset.DeformValueCount].toInt()
			this._deformCount = frameIntArray[frameIntOffset + BinaryOffset.DeformCount].toInt()
			this._deformOffset = frameIntArray[frameIntOffset + BinaryOffset.DeformValueOffset].toInt()
			this._sameValueOffset = frameIntArray[frameIntOffset + BinaryOffset.DeformFloatOffset].toInt() +
					this._animationData!!.frameFloatOffset
			this._valueScale = this._armature!!.armatureData.scale
			this._valueArray = dragonBonesData.frameFloatArray!!
			this._rd = DoubleArray(this._valueCount * 2)
		} else {
			this._deformCount = ((this.targetBlendState)!!.targetSurface)!!._deformVertices.size
		}
	}

	override fun blend(_isDirty: Boolean) {
		val blendState = this.targetBlendState!!
		val surface = blendState.targetSurface
            ?: error("blendState.targetSurface=null: target=${blendState.targetCommon}")
		val blendWeight = blendState.blendWeight
		val result = surface._deformVertices
		val valueArray = this._valueArray

		if (valueArray != null) {
			val valueCount = this._valueCount
			val deformOffset = this._deformOffset
			val sameValueOffset = this._sameValueOffset
			val rd = this._rd

			for (i in 0 until this._deformCount) {
				var value: Double

				value = if (i < deformOffset) {
					valueArray[sameValueOffset + i].toDouble()
				} else if (i < deformOffset + valueCount) {
					rd[i - deformOffset]
				} else {
					valueArray[sameValueOffset + i - valueCount].toDouble()
				}

				if (blendState.dirty > 1) {
					result[i] += (value * blendWeight).toFloat()
				} else {
					result[i] = (value * blendWeight).toFloat()
				}
			}
		} else if (blendState.dirty == 1) {
			for (i in 0 until this._deformCount) {
				result[i] = 0f
			}
		}

		if (_isDirty || this.dirty) {
			this.dirty = false
			surface._transformDirty = true
		}
	}
}

/**
 * @internal
 */
class AlphaTimelineState(pool: SingleObjectPool<AlphaTimelineState>) : SingleValueTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.AlphaTimelineState]"
	}

	override fun _onArriveAtFrame() {
		super._onArriveAtFrame()

		if (this._timelineData == null) { // Pose.
			this._result = 1.0
		}
	}

	override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		super.init(armature, animationState, timelineData)

		this._valueOffset = this._animationData!!.frameIntOffset
		this._valueScale = 0.01
		this._valueArray = this._animationData!!.parent!!.parent!!.frameIntArray!!
	}

	override fun blend(_isDirty: Boolean) {
		val blendState = this.targetBlendState!!
		val alphaTarget = blendState.targetTransformObject!!
		val blendWeight = blendState.blendWeight

		if (blendState.dirty > 1) {
			alphaTarget._alpha += this._result * blendWeight
			if (alphaTarget._alpha > 1.0) {
				alphaTarget._alpha = 1.0
			}
		} else {
			alphaTarget._alpha = this._result * blendWeight
		}

		if (_isDirty || this.dirty) {
			this.dirty = false
			this._armature?._alphaDirty = true
		}
	}
}

/**
 * @internal
 */
class SlotDisplayTimelineState(pool: SingleObjectPool<SlotDisplayTimelineState>) : TimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.SlotDisplayTimelineState]"
	}

	override fun _onArriveAtFrame() {
		if (this.playState >= 0) {
			val slot = this.targetSlot!!
			val displayIndex: Int =
				if (this._timelineData != null) this._frameArray!![this._frameOffset + 1].toInt() else slot._slotData!!.displayIndex

			if (slot.displayIndex != displayIndex) {
				slot._setDisplayIndex(displayIndex, true)
			}
		}
	}

	override fun _onUpdateFrame() {
	}
}

/**
 * @internal
 */
class SlotColorTimelineState(pool: SingleObjectPool<SlotColorTimelineState>) : TweenTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.SlotColorTimelineState]"
	}

	private val _current: IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
	private val _difference: IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
	private val _result: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

	@Suppress("UNUSED_CHANGED_VALUE")
	override fun _onArriveAtFrame() {
		super._onArriveAtFrame()

		if (this._timelineData != null) {
			val dragonBonesData = this._animationData!!.parent!!.parent
			val colorArray = dragonBonesData!!.colorArray!!
			val frameIntArray = dragonBonesData.frameIntArray!!
			val valueOffset = this._animationData!!.frameIntOffset + this._frameValueOffset + this._frameIndex
			var colorOffset = frameIntArray[valueOffset].toInt()

			if (colorOffset < 0) {
				colorOffset += 65536 // Fixed out of bounds bug.
			}

			if (this._isTween) {
				this._current[0] = colorArray[colorOffset++].toInt()
				this._current[1] = colorArray[colorOffset++].toInt()
				this._current[2] = colorArray[colorOffset++].toInt()
				this._current[3] = colorArray[colorOffset++].toInt()
				this._current[4] = colorArray[colorOffset++].toInt()
				this._current[5] = colorArray[colorOffset++].toInt()
				this._current[6] = colorArray[colorOffset++].toInt()
				this._current[7] = colorArray[colorOffset++].toInt()

				colorOffset = if (this._frameIndex == this._frameCount - 1) {
					frameIntArray[this._animationData!!.frameIntOffset + this._frameValueOffset].toInt()
				} else {
					frameIntArray[valueOffset + 1].toInt()
				}

				if (colorOffset < 0) {
					colorOffset += 65536 // Fixed out of bounds bug.
				}

				this._difference[0] = colorArray[colorOffset++] - this._current[0]
				this._difference[1] = colorArray[colorOffset++] - this._current[1]
				this._difference[2] = colorArray[colorOffset++] - this._current[2]
				this._difference[3] = colorArray[colorOffset++] - this._current[3]
				this._difference[4] = colorArray[colorOffset++] - this._current[4]
				this._difference[5] = colorArray[colorOffset++] - this._current[5]
				this._difference[6] = colorArray[colorOffset++] - this._current[6]
				this._difference[7] = colorArray[colorOffset++] - this._current[7]
			} else {
				this._result[0] = colorArray[colorOffset++] * 0.01
				this._result[1] = colorArray[colorOffset++] * 0.01
				this._result[2] = colorArray[colorOffset++] * 0.01
				this._result[3] = colorArray[colorOffset++] * 0.01
				this._result[4] = colorArray[colorOffset++].toDouble()
				this._result[5] = colorArray[colorOffset++].toDouble()
				this._result[6] = colorArray[colorOffset++].toDouble()
				this._result[7] = colorArray[colorOffset++].toDouble()
			}
		} else { // Pose.
			val slot = this.targetSlot!!
			val color = slot.slotData.color!!
			this._result[0] = color.alphaMultiplier
			this._result[1] = color.redMultiplier
			this._result[2] = color.greenMultiplier
			this._result[3] = color.blueMultiplier
			this._result[4] = color.alphaOffset.toDouble()
			this._result[5] = color.redOffset.toDouble()
			this._result[6] = color.greenOffset.toDouble()
			this._result[7] = color.blueOffset.toDouble()
		}
	}

	override fun _onUpdateFrame() {
		super._onUpdateFrame()

		if (this._isTween) {
			this._result[0] = (this._current[0] + this._difference[0] * this._tweenProgress) * 0.01
			this._result[1] = (this._current[1] + this._difference[1] * this._tweenProgress) * 0.01
			this._result[2] = (this._current[2] + this._difference[2] * this._tweenProgress) * 0.01
			this._result[3] = (this._current[3] + this._difference[3] * this._tweenProgress) * 0.01
			this._result[4] = this._current[4] + this._difference[4] * this._tweenProgress
			this._result[5] = this._current[5] + this._difference[5] * this._tweenProgress
			this._result[6] = this._current[6] + this._difference[6] * this._tweenProgress
			this._result[7] = this._current[7] + this._difference[7] * this._tweenProgress
		}
	}

	override fun fadeOut() {
		this._isTween = false
	}

	override fun update(passedTime: Double) {
		super.update(passedTime)
		// Fade animation.
		if (this._isTween || this.dirty) {
			val slot = this.targetSlot!!
			val result = slot._colorTransform

			if (this._animationState!!._fadeState != 0 || this._animationState!!._subFadeState != 0) {
				if (
					result.alphaMultiplier != this._result[0] ||
					result.redMultiplier != this._result[1] ||
					result.greenMultiplier != this._result[2] ||
					result.blueMultiplier != this._result[3] ||
					result.alphaOffset != this._result[4].toInt() ||
					result.redOffset != this._result[5].toInt() ||
					result.greenOffset != this._result[6].toInt() ||
					result.blueOffset != this._result[7].toInt()
				) {
					val fadeProgress = this._animationState!!._fadeProgress.pow(4.0)
					result.alphaMultiplier += (this._result[0] - result.alphaMultiplier) * fadeProgress
					result.redMultiplier += (this._result[1] - result.redMultiplier) * fadeProgress
					result.greenMultiplier += (this._result[2] - result.greenMultiplier) * fadeProgress
					result.blueMultiplier += (this._result[3] - result.blueMultiplier) * fadeProgress
					result.alphaOffset += ((this._result[4] - result.alphaOffset) * fadeProgress).toInt()
					result.redOffset += ((this._result[5] - result.redOffset) * fadeProgress).toInt()
					result.greenOffset += ((this._result[6] - result.greenOffset) * fadeProgress).toInt()
					result.blueOffset += ((this._result[7] - result.blueOffset) * fadeProgress).toInt()
					slot._colorDirty = true
				}
			} else if (this.dirty) {
				this.dirty = false

				if (
					result.alphaMultiplier != this._result[0] ||
					result.redMultiplier != this._result[1] ||
					result.greenMultiplier != this._result[2] ||
					result.blueMultiplier != this._result[3] ||
					result.alphaOffset != this._result[4].toInt() ||
					result.redOffset != this._result[5].toInt() ||
					result.greenOffset != this._result[6].toInt() ||
					result.blueOffset != this._result[7].toInt()
				) {
					result.alphaMultiplier = this._result[0]
					result.redMultiplier = this._result[1]
					result.greenMultiplier = this._result[2]
					result.blueMultiplier = this._result[3]
					result.alphaOffset = this._result[4].toInt()
					result.redOffset = this._result[5].toInt()
					result.greenOffset = this._result[6].toInt()
					result.blueOffset = this._result[7].toInt()
					slot._colorDirty = true
				}
			}
		}
	}
}

/**
 * @internal
 */
class SlotZIndexTimelineState(pool: SingleObjectPool<SlotZIndexTimelineState>) : SingleValueTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.SlotZIndexTimelineState]"
	}

	override fun _onArriveAtFrame() {
		super._onArriveAtFrame()

		if (this._timelineData == null) { // Pose.
			val blendState = this.targetBlendState!!
			val slot = blendState.targetSlot!!
			this._result = slot.slotData.zIndex.toDouble()
		}
	}

	override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		super.init(armature, animationState, timelineData)

		this._valueOffset = this._animationData!!.frameIntOffset
		this._valueArray = this._animationData!!.parent!!.parent!!.frameIntArray!!
	}

	override fun blend(_isDirty: Boolean) {
		val blendState = this.targetBlendState!!
		val slot = blendState.targetSlot!!
		val blendWeight = blendState.blendWeight

		if (blendState.dirty > 1) {
			// @TODO: Kotlin conversion original (no toInt): slot._zIndex += this._result * blendWeight
			slot._zIndex += (this._result * blendWeight).toInt()
		} else {
			// @TODO: Kotlin conversion original (no toInt): slot._zIndex = this._result * blendWeight
			slot._zIndex = (this._result * blendWeight).toInt()
		}

		if (_isDirty || this.dirty) {
			this.dirty = false
			this._armature?._zIndexDirty = true
		}
	}
}

/**
 * @internal
 */
class DeformTimelineState(pool: SingleObjectPool<DeformTimelineState>) : MutilpleValueTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.DeformTimelineState]"
	}

	var geometryOffset: Int = 0
	var displayFrame: DisplayFrame? = null

	private var _deformCount: Int = 0
	private var _deformOffset: Int = 0
	private var _sameValueOffset: Int = 0

	override fun _onClear() {
		super._onClear()

		this.geometryOffset = 0
		this.displayFrame = null

		this._deformCount = 0
		this._deformOffset = 0
		this._sameValueOffset = 0
	}

	override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		super.init(armature, animationState, timelineData)

		if (this._timelineData != null) {
			val frameIntOffset =
				this._animationData!!.frameIntOffset + this._timelineArray!![this._timelineData!!.offset + BinaryOffset.TimelineFrameValueCount]
			val dragonBonesData = this._animationData?.parent?.parent
			val frameIntArray = dragonBonesData!!.frameIntArray
			val slot = (this.targetBlendState)!!.targetSlot!!
			this.geometryOffset = frameIntArray!![frameIntOffset + BinaryOffset.DeformVertexOffset].toInt()

			if (this.geometryOffset < 0) {
				this.geometryOffset += 65536 // Fixed out of bounds bug.
			}

			for (i in 0 until slot.displayFrameCount) {
				val displayFrame = slot.getDisplayFrameAt(i)
				val geometryData = displayFrame.getGeometryData() ?: continue

				if (geometryData.offset == this.geometryOffset) {
					this.displayFrame = displayFrame
					this.displayFrame?.updateDeformVertices()
					break
				}
			}

			if (this.displayFrame == null) {
				this.returnToPool() //
				return
			}

			this._valueOffset = this._animationData!!.frameFloatOffset
			this._valueCount = frameIntArray[frameIntOffset + BinaryOffset.DeformValueCount].toInt()
			this._deformCount = frameIntArray[frameIntOffset + BinaryOffset.DeformCount].toInt()
			this._deformOffset = frameIntArray[frameIntOffset + BinaryOffset.DeformValueOffset].toInt()
			this._sameValueOffset = frameIntArray[frameIntOffset + BinaryOffset.DeformFloatOffset].toInt() +
					this._animationData!!.frameFloatOffset
			this._valueScale = this._armature!!.armatureData.scale
			this._valueArray = dragonBonesData.frameFloatArray!!
			this._rd = DoubleArray(this._valueCount * 2)
		} else {
			this._deformCount = this.displayFrame!!.deformVertices.size
		}
	}

	override fun blend(_isDirty: Boolean) {
		val blendState = this.targetBlendState!!
		val slot = blendState.targetSlot!!
		val blendWeight = blendState.blendWeight
		val result = this.displayFrame!!.deformVertices
		val valueArray = this._valueArray

		if (valueArray != null) {
			val valueCount = this._valueCount
			val deformOffset = this._deformOffset
			val sameValueOffset = this._sameValueOffset
			val rd = this._rd

			for (i in 0 until this._deformCount) {
				val value = when {
					i < deformOffset -> valueArray[sameValueOffset + i].toDouble()
					i < deformOffset + valueCount -> rd[i - deformOffset]
					else -> valueArray[sameValueOffset + i - valueCount].toDouble()
				}

				if (blendState.dirty > 1) {
					result[i] += value * blendWeight
				} else {
					result[i] = value * blendWeight
				}
			}
		} else if (blendState.dirty == 1) {
			//for (var i = 0; i < this._deformCount; ++i) {
			for (i in 0 until this._deformCount) {
				result[i] = 0.0
			}
		}

		if (_isDirty || this.dirty) {
			this.dirty = false

			if (slot._geometryData == this.displayFrame!!.getGeometryData()) {
				slot._verticesDirty = true
			}
		}
	}
}

/**
 * @internal
 */
class IKConstraintTimelineState(pool: SingleObjectPool<IKConstraintTimelineState>) : DoubleValueTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.IKConstraintTimelineState]"
	}

	override fun _onUpdateFrame() {
		super._onUpdateFrame()

		val ikConstraint = this.targetIKConstraint!!

		if (this._timelineData != null) {
			ikConstraint._bendPositive = this._currentA > 0.0
			ikConstraint._weight = this._currentB
		} else {
			val ikConstraintData = ikConstraint._constraintData as IKConstraintData
			ikConstraint._bendPositive = ikConstraintData.bendPositive
			ikConstraint._weight = ikConstraintData.weight
		}

		ikConstraint.invalidUpdate()
		this.dirty = false
	}

	override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		super.init(armature, animationState, timelineData)

		this._valueOffset = this._animationData!!.frameIntOffset
		this._valueScale = 0.01
		this._valueArray = this._animationData!!.parent!!.parent!!.frameIntArray!!
	}
}

/**
 * @internal
 */
class AnimationProgressTimelineState(pool: SingleObjectPool<AnimationProgressTimelineState>) : SingleValueTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.AnimationProgressTimelineState]"
	}

	override fun _onUpdateFrame() {
		super._onUpdateFrame()

		val animationState = this.targetAnimationState!!
		if (animationState._parent != null) {
			animationState.currentTime = this._result * animationState.totalTime
		}

		this.dirty = false
	}

	override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		super.init(armature, animationState, timelineData)

		this._valueOffset = this._animationData!!.frameIntOffset
		this._valueScale = 0.0001
		this._valueArray = this._animationData!!.parent!!.parent!!.frameIntArray!!
	}
}

/**
 * @internal
 */
class AnimationWeightTimelineState(pool: SingleObjectPool<AnimationWeightTimelineState>) : SingleValueTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.AnimationWeightTimelineState]"
	}

	override fun _onUpdateFrame() {
		super._onUpdateFrame()

		val animationState = this.targetAnimationState!!
		if (animationState._parent != null) {
			animationState.weight = this._result
		}

		this.dirty = false
	}

	override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		super.init(armature, animationState, timelineData)

		this._valueOffset = this._animationData!!.frameIntOffset
		this._valueScale = 0.0001
		this._valueArray = this._animationData!!.parent!!.parent!!.frameIntArray!!
	}
}

/**
 * @internal
 */
class AnimationParametersTimelineState(pool: SingleObjectPool<AnimationParametersTimelineState>) : DoubleValueTimelineState(pool) {
	override fun toString(): String {
		return "[class dragonBones.AnimationParametersTimelineState]"
	}

	override fun _onUpdateFrame() {
		super._onUpdateFrame()

		val animationState = this.targetAnimationState!!
		if (animationState._parent != null) {
			animationState.parameterX = this._resultA
			animationState.parameterY = this._resultB
		}

		this.dirty = false
	}

	override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		super.init(armature, animationState, timelineData)

		this._valueOffset = this._animationData!!.frameIntOffset
		this._valueScale = 0.0001
		this._valueArray = this._animationData!!.parent!!.parent!!.frameIntArray!!
	}
}
