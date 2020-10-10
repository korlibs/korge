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
/**
 * @internal
 */

package com.dragonbones.animation

import com.dragonbones.armature.*
import com.dragonbones.core.*
import com.dragonbones.model.*
import com.soywiz.kds.*
import com.soywiz.kmem.*
import kotlin.math.*

abstract class TimelineState(pool: SingleObjectPool<out TimelineState>) : BaseObject(pool) {
	var dirty: Boolean = false
	/**
	 * -1: start, 0: play, 1: complete;
	 */
	var playState: Int = -1
	var currentPlayTimes: Int = -1
	var _currentTime: Double = -1.0
	//var target: BaseObject? = null
    var targetAnimationState: AnimationState? = null
    var targetBlendState: BlendState? = null
    var targetSlot: Slot? = null
    var targetIKConstraint: IKConstraint? = null

	protected var _isTween: Boolean = false
	protected var _valueOffset: Int = 0
	protected var _frameValueOffset: Int = 0
	protected var _frameOffset: Int = 0
	protected var _frameRate: Int = 0
	protected var _frameCount: Int = 0
	protected var _frameIndex: Int = -1
	protected var _frameRateR: Double = 0.0
	protected var _position: Double = 0.0
	protected var _duration: Double = 0.0
	protected var _timeScale: Double = 1.0
	protected var _timeOffset: Double = 0.0
	protected var _animationData: AnimationData? = null
	protected var _timelineData: TimelineData? = null
	protected var _armature: Armature? = null
	protected var _animationState: AnimationState? = null
	protected var _actionTimeline: TimelineState? = null

	protected var _timelineArray:  Uint16Buffer? = null
	protected var _frameArray:  Int16Buffer? = null
	protected var _valueArray:  Float32Buffer? = null
	protected var _frameIndices:  IntArrayList? = null

	override fun _onClear() {
		this.dirty = false
		this.playState = -1
		this.currentPlayTimes = -1
		this._currentTime = -1.0
		//this.target = null
        this.targetAnimationState = null
        this.targetBlendState = null
        this.targetSlot = null
        this.targetIKConstraint = null

		this._isTween = false
		this._valueOffset = 0
		this._frameValueOffset = 0
		this._frameOffset = 0
		this._frameRate = 0
		this._frameCount = 0
		this._frameIndex = -1
		this._frameRateR = 0.0
		this._position = 0.0
		this._duration = 0.0
		this._timeScale = 1.0
		this._timeOffset = 0.0
		this._animationData = null //
		this._timelineData = null //
		this._armature = null //
		this._animationState = null //
		this._actionTimeline = null //
		this._frameArray = null //
		this._valueArray = null //
		this._timelineArray = null //
		this._frameIndices = null //
	}

	protected abstract fun _onArriveAtFrame()
	protected abstract fun _onUpdateFrame()

	protected fun _setCurrentTime(passedTime: Double): Boolean {
		var passedTime = passedTime
		val prevState = this.playState
		val prevPlayTimes = this.currentPlayTimes
		val prevTime = this._currentTime

		if (this._actionTimeline != null && this._frameCount <= 1) { // No frame or only one frame.
			this.playState = if (this._actionTimeline!!.playState >= 0) 1 else -1
			this.currentPlayTimes = 1
			this._currentTime = this._actionTimeline!!._currentTime
		}
		else if (this._actionTimeline == null || this._timeScale != 1.0 || this._timeOffset != 0.0) { // Action timeline or has scale and offset.
			val playTimes = this._animationState!!.playTimes
			val totalTime = playTimes * this._duration

			passedTime *= this._timeScale
			if (this._timeOffset != 0.0) {
				passedTime += this._timeOffset * this._animationData!!.duration
			}

			if (playTimes > 0 && (passedTime >= totalTime || passedTime <= -totalTime)) {
				if (this.playState <= 0 && this._animationState!!._playheadState == 3) {
					this.playState = 1
				}

				this.currentPlayTimes = playTimes
				if (passedTime < 0.0) {
					this._currentTime = 0.0
				}
				else {
					this._currentTime = if (this.playState == 1) this._duration + 0.000001 else this._duration // Precision problem
				}
			}
			else {
				if (this.playState != 0 && this._animationState!!._playheadState == 3) {
					this.playState = 0
				}

				if (passedTime < 0.0) {
					passedTime = -passedTime
					this.currentPlayTimes = floor(passedTime / this._duration).toInt()
					this._currentTime = this._duration - (passedTime % this._duration)
				}
				else {
					this.currentPlayTimes = floor(passedTime / this._duration).toInt()
					this._currentTime = passedTime % this._duration
				}
			}

			this._currentTime += this._position
		}
		else { // Multi frames.
			this.playState = this._actionTimeline!!.playState
			this.currentPlayTimes = this._actionTimeline!!.currentPlayTimes
			this._currentTime = this._actionTimeline!!._currentTime
		}

		if (this.currentPlayTimes == prevPlayTimes && this._currentTime == prevTime) {
			return false
		}

		// Clear frame flag when timeline start or loopComplete.
		if (
			(prevState < 0 && this.playState != prevState) ||
			(this.playState <= 0 && this.currentPlayTimes != prevPlayTimes)
		) {
			this._frameIndex = -1
		}

		return true
	}

	open fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
		this._armature = armature
		this._animationState = animationState
		this._timelineData = timelineData
		this._actionTimeline = this._animationState!!._actionTimeline

		if (this == this._actionTimeline) {
			this._actionTimeline = null //
		}

		this._animationData = this._animationState!!.animationData
		//
		this._frameRate = this._animationData!!.parent!!.frameRate
		this._frameRateR = 1.0 / this._frameRate
		this._position = this._animationState!!._position
		this._duration = this._animationState!!._duration

		val _timelineData = this._timelineData
		if (_timelineData != null) {
			val dragonBonesData = this._animationData!!.parent!!.parent!! // May by the animation data is not belone to this armature data.
			this._frameArray = dragonBonesData.frameArray
			this._timelineArray = dragonBonesData.timelineArray
			this._frameIndices = dragonBonesData.frameIndices
			//
			this._frameCount = this._timelineArray!![_timelineData.offset + BinaryOffset.TimelineKeyFrameCount]
			this._frameValueOffset = this._timelineArray!![_timelineData.offset + BinaryOffset.TimelineFrameValueOffset]
			this._timeScale = 100.0 / this._timelineArray!![_timelineData.offset + BinaryOffset.TimelineScale]
			this._timeOffset = this._timelineArray!![_timelineData.offset + BinaryOffset.TimelineOffset] * 0.01
		}
	}

	open fun fadeOut() {
		this.dirty = false
	}

	open fun update(passedTime: Double) {
		if (this._setCurrentTime(passedTime)) {
			if (this._frameCount > 1) {
				val timelineFrameIndex = floor(this._currentTime * this._frameRate).toInt() // uint
				val frameIndex = this._frameIndices!![(this._timelineData!!).frameIndicesOffset + timelineFrameIndex]

				if (this._frameIndex != frameIndex) {
					this._frameIndex = frameIndex
					this._frameOffset = this._animationData!!.frameOffset + this._timelineArray!![(this._timelineData as TimelineData).offset + BinaryOffset.TimelineFrameOffset + this._frameIndex]

					this._onArriveAtFrame()
				}
			}
			else if (this._frameIndex < 0) {
				this._frameIndex = 0

				if (this._timelineData != null) { // May be pose timeline.
					this._frameOffset = this._animationData!!.frameOffset + this._timelineArray!![this._timelineData!!.offset + BinaryOffset.TimelineFrameOffset]
				}

				this._onArriveAtFrame()
			}

			if (this._isTween || this.dirty) {
				this._onUpdateFrame()
			}
		}
	}

	open fun blend(_isDirty: Boolean) {
	}
}
/**
 * @internal
 */
abstract class TweenTimelineState(pool: SingleObjectPool<out TweenTimelineState>) :  TimelineState(pool) {
	companion object {
		private fun _getEasingValue(tweenType: TweenType, progress: Double, easing: Double): Double {
			var value = progress

			when (tweenType) {
				TweenType.QuadIn -> value = progress.pow(2.0)
				TweenType.QuadOut -> value = 1.0 - (1.0 - progress).pow(2.0)
				TweenType.QuadInOut -> value = 0.5 * (1.0 - cos(progress * PI))
				else -> {
				}
			}

			return (value - progress) * easing + progress
		}

		private fun _getEasingCurveValue(progress: Double, samples: Int16Buffer, count: Int, offset: Int): Double {
			if (progress <= 0.0) {
				return 0.0
			}
			else if (progress >= 1.0) {
				return 1.0
			}

			val isOmited = count > 0
			val segmentCount = count + 1 // + 2 - 1
			val valueIndex = floor(progress * segmentCount).toInt()
			val fromValue: Double
			val toValue: Double

			if (isOmited) {
				fromValue = if (valueIndex == 0) 0.0 else samples[offset + valueIndex - 1].toDouble()
				toValue = if (valueIndex == segmentCount - 1) 10000.0 else samples[offset + valueIndex].toDouble()
			}
			else {
				fromValue = samples[offset + valueIndex - 1].toDouble()
				toValue = samples[offset + valueIndex].toDouble()
			}

			return (fromValue + (toValue - fromValue) * (progress * segmentCount - valueIndex)) * 0.0001
		}
	}

	protected var _tweenType: TweenType = TweenType.None
	protected var _curveCount: Int = 0
	protected var _framePosition: Double = 0.0
	protected var _frameDurationR: Double = 0.0
	protected var _tweenEasing: Double = 0.0
	protected var _tweenProgress: Double = 0.0
	protected var _valueScale: Double = 1.0

	override fun _onClear() {
		super._onClear()

		this._tweenType = TweenType.None
		this._curveCount = 0
		this._framePosition = 0.0
		this._frameDurationR = 0.0
		this._tweenEasing = 0.0
		this._tweenProgress = 0.0
		this._valueScale = 1.0
	}

	override fun _onArriveAtFrame() {
		if (
			this._frameCount > 1 &&
			(
				this._frameIndex != this._frameCount - 1 ||
				this._animationState!!.playTimes == 0 ||
				this._animationState!!.currentPlayTimes < this._animationState!!.playTimes - 1
			)
		) {
			this._tweenType = TweenType[this._frameArray!![this._frameOffset + BinaryOffset.FrameTweenType].toInt()]
			this._isTween = this._tweenType != TweenType.None

			if (this._isTween) {
				if (this._tweenType == TweenType.Curve) {
					this._curveCount = this._frameArray!![this._frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount].toInt()
				}
				else if (this._tweenType != TweenType.None && this._tweenType != TweenType.Line) {
					this._tweenEasing = this._frameArray!![this._frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount] * 0.01
				}
			}
			else {
				this.dirty = true
			}

			this._framePosition = this._frameArray!![this._frameOffset] * this._frameRateR

			if (this._frameIndex == this._frameCount - 1) {
				this._frameDurationR = 1.0 / (this._animationData!!.duration - this._framePosition)
			}
			else {
				val nextFrameOffset = this._animationData!!.frameOffset + this._timelineArray!![(this._timelineData!!).offset + BinaryOffset.TimelineFrameOffset + this._frameIndex + 1]
				val frameDuration = this._frameArray!![nextFrameOffset] * this._frameRateR - this._framePosition

				if (frameDuration > 0) {
					this._frameDurationR = 1.0 / frameDuration
				}
				else {
					this._frameDurationR = 0.0
				}
			}
		}
		else {
			this.dirty = true
			this._isTween = false
		}
	}

	override fun _onUpdateFrame() {
		if (this._isTween) {
			this.dirty = true
			this._tweenProgress = (this._currentTime - this._framePosition) * this._frameDurationR

			if (this._tweenType == TweenType.Curve) {
				this._tweenProgress = TweenTimelineState._getEasingCurveValue(this._tweenProgress, this._frameArray!!, this._curveCount, this._frameOffset + BinaryOffset.FrameCurveSamples)
			}
			else if (this._tweenType != TweenType.Line) {
				this._tweenProgress = TweenTimelineState._getEasingValue(this._tweenType, this._tweenProgress, this._tweenEasing)
			}
		}
	}
}
/**
 * @internal
 */
abstract class SingleValueTimelineState(pool: SingleObjectPool<out SingleValueTimelineState>) :  TweenTimelineState(pool) {
	protected var _current: Double = 0.0
	protected var _difference: Double = 0.0
	protected var _result: Double = 0.0

	override fun _onClear() {
		super._onClear()

		this._current = 0.0
		this._difference = 0.0
		this._result = 0.0
	}

	override fun _onArriveAtFrame() {
		super._onArriveAtFrame()

		if (this._timelineData != null) {
			val valueScale = this._valueScale
			val valueArray = this._valueArray!!
			//
			val valueOffset = this._valueOffset + this._frameValueOffset + this._frameIndex

			if (this._isTween) {
				val nextValueOffset = if (this._frameIndex == this._frameCount - 1)
					this._valueOffset + this._frameValueOffset else valueOffset + 1

				if (valueScale == 1.0) {
					this._current = valueArray[valueOffset].toDouble()
					this._difference = valueArray[nextValueOffset] - this._current
				}
				else {
					this._current = valueArray[valueOffset].toDouble() * valueScale
					this._difference = valueArray[nextValueOffset].toDouble() * valueScale - this._current
				}
			}
			else {
				this._result = valueArray[valueOffset].toDouble() * valueScale
			}
		}
		else {
			this._result = 0.0
		}
	}

	override fun _onUpdateFrame() {
		super._onUpdateFrame()

		if (this._isTween) {
			this._result = this._current + this._difference * this._tweenProgress
		}
	}
}
/**
 * @internal
 */
abstract class DoubleValueTimelineState(pool: SingleObjectPool<out DoubleValueTimelineState>) :  TweenTimelineState(pool) {
	protected var _currentA: Double = 0.0
	protected var _currentB: Double = 0.0
	protected var _differenceA: Double = 0.0
	protected var _differenceB: Double = 0.0
	protected var _resultA: Double = 0.0
	protected var _resultB: Double = 0.0

	override fun _onClear() {
		super._onClear()

		this._currentA = 0.0
		this._currentB = 0.0
		this._differenceA = 0.0
		this._differenceB = 0.0
		this._resultA = 0.0
		this._resultB = 0.0
	}

	override fun _onArriveAtFrame() {
		super._onArriveAtFrame()

		if (this._timelineData != null) {
			val valueScale = this._valueScale
			val valueArray = this._valueArray!!
			//
			val valueOffset = this._valueOffset + this._frameValueOffset + this._frameIndex * 2

			if (this._isTween) {
				val nextValueOffset = if (this._frameIndex == this._frameCount - 1)
					this._valueOffset + this._frameValueOffset else
					valueOffset + 2

				if (valueScale == 1.0) {
					this._currentA = valueArray[valueOffset].toDouble()
					this._currentB = valueArray[valueOffset + 1].toDouble()
					this._differenceA = valueArray[nextValueOffset] - this._currentA
					this._differenceB = valueArray[nextValueOffset + 1] - this._currentB
				}
				else {
					this._currentA = valueArray[valueOffset] * valueScale
					this._currentB = valueArray[valueOffset + 1] * valueScale
					this._differenceA = valueArray[nextValueOffset] * valueScale - this._currentA
					this._differenceB = valueArray[nextValueOffset + 1] * valueScale - this._currentB
				}
			}
			else {
				this._resultA = valueArray[valueOffset] * valueScale
				this._resultB = valueArray[valueOffset + 1] * valueScale
			}
		}
		else {
			this._resultA = 0.0
			this._resultB = 0.0
		}
	}

	override fun _onUpdateFrame() {
		super._onUpdateFrame()

		if (this._isTween) {
			this._resultA = this._currentA + this._differenceA * this._tweenProgress
			this._resultB = this._currentB + this._differenceB * this._tweenProgress
		}
	}
}
/**
 * @internal
 */
abstract class MutilpleValueTimelineState(pool: SingleObjectPool<out MutilpleValueTimelineState>) :  TweenTimelineState(pool) {
	protected var _valueCount: Int = 0
	protected var _rd:  DoubleArray = DoubleArray(0)

	override fun _onClear() {
		super._onClear()

		this._valueCount = 0
		this._rd = DoubleArray(0)
	}

	override fun _onArriveAtFrame() {
		super._onArriveAtFrame()

		val valueCount = this._valueCount
		this._rd = if (this._rd.size < valueCount) DoubleArray(valueCount * 2) else this._rd
		val rd = this._rd

		if (this._timelineData != null) {
			val valueScale = this._valueScale
			val valueArray = this._valueArray!!
			//
			val valueOffset = this._valueOffset + this._frameValueOffset + this._frameIndex * valueCount

			if (this._isTween) {
				val nextValueOffset = if (this._frameIndex == this._frameCount - 1)
					this._valueOffset + this._frameValueOffset else valueOffset + valueCount

				for (i in 0 until valueCount) {
                    rd[valueCount + i] = (valueArray[nextValueOffset + i] - valueArray[valueOffset + i]) * valueScale
                }
			}
			else {
				for (i in 0 until valueCount) rd[i] = valueArray[valueOffset + i] * valueScale
			}
		}
		else {
			for (i in 0 until valueCount) rd[i] = 0.0
		}
	}

	override fun _onUpdateFrame() {
		super._onUpdateFrame()

		if (this._isTween) {
			val valueCount = this._valueCount
			val valueScale = this._valueScale
			val tweenProgress = this._tweenProgress
			val valueArray = this._valueArray!!
			val rd = this._rd
			//
			val valueOffset = this._valueOffset + this._frameValueOffset + this._frameIndex * valueCount

			if (valueScale == 1.0) {
				for (i in 0 until valueCount) {
					rd[i] = valueArray[valueOffset + i] + rd[valueCount + i] * tweenProgress
				}
			}
			else {
				for (i in 0 until valueCount) {
					rd[i] = valueArray[valueOffset + i] * valueScale + rd[valueCount + i] * tweenProgress
				}
			}
		}
	}
}
