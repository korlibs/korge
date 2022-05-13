package com.dragonbones.animation

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

import com.dragonbones.armature.*
import com.dragonbones.core.*
import com.dragonbones.event.*
import com.soywiz.kds.iterators.*
import com.dragonbones.model.*
import com.dragonbones.util.*
import com.soywiz.kds.*
import kotlin.math.*

/**
 * - The animation state is generated when the animation data is played.
 * @see dragonBones.Animation
 * @see dragonBones.AnimationData
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 动画状态由播放动画数据时产生。
 * @see dragonBones.Animation
 * @see dragonBones.AnimationData
 * @version DragonBones 3.0
 * @language zh_CN
 */
class AnimationState(pool: SingleObjectPool<AnimationState>) : BaseObject(pool) {
	override fun toString(): String {
		return "[class dragonBones.AnimationState]"
	}

	/**
	 * @private
	 */
	var actionEnabled: Boolean = false
	/**
	 * @private
	 */
	var additive: Boolean = false
	/**
	 * - Whether the animation state has control over the display object properties of the slots.
	 * Sometimes blend a animation state does not want it to control the display object properties of the slots,
	 * especially if other animation state are controlling the display object properties of the slots.
	 * @default true
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 动画状态是否对插槽的显示对象属性有控制权。
	 * 有时混合一个动画状态并不希望其控制插槽的显示对象属性，
	 * 尤其是其他动画状态正在控制这些插槽的显示对象属性时。
	 * @default true
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	var displayControl: Boolean = false
	/**
	 * - Whether to reset the objects without animation to the armature pose when the animation state is start to play.
	 * This property should usually be set to false when blend multiple animation states.
	 * @default true
	 * @version DragonBones 5.1
	 * @language en_US
	 */
	/**
	 * - 开始播放动画状态时是否将没有动画的对象重置为骨架初始值。
	 * 通常在混合多个动画状态时应该将该属性设置为 false。
	 * @default true
	 * @version DragonBones 5.1
	 * @language zh_CN
	 */
	var resetToPose: Boolean = false
	/**
	 * @private
	 */
	var blendType: AnimationBlendType = AnimationBlendType.None
	/**
	 * - The play times. [0: Loop play, [1~N]: Play N times]
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 播放次数。 [0: 无限循环播放, [1~N]: 循环播放 N 次]
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var playTimes: Int = 1
	/**
	 * - The blend layer.
	 * High layer animation state will get the blend weight first.
	 * When the blend weight is assigned more than 1, the remaining animation states will no longer get the weight assigned.
	 * @readonly
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 混合图层。
	 * 图层高的动画状态会优先获取混合权重。
	 * 当混合权重分配超过 1 时，剩余的动画状态将不再获得权重分配。
	 * @readonly
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	var layer: Int = 0
	/**
	 * - The play speed.
	 * The value is an overlay relationship with {@link dragonBones.Animation#timeScale}.
	 * [(-N~0): Reverse play, 0: Stop play, (0~1): Slow play, 1: Normal play, (1~N): Fast play]
	 * @default 1.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 播放速度。
	 * 该值与 {@link dragonBones.Animation#timeScale} 是叠加关系。
	 * [(-N~0): 倒转播放, 0: 停止播放, (0~1): 慢速播放, 1: 正常播放, (1~N): 快速播放]
	 * @default 1.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var timeScale: Double = 1.0
	/**
	 * @private
	 */
	var parameterX: Double = 0.0
	/**
	 * @private
	 */
	var parameterY: Double = 0.0
	/**
	 * @private
	 */
	var positionX: Double = 0.0
	/**
	 * @private
	 */
	var positionY: Double = 0.0
	/**
	 * - The auto fade out time when the animation state play completed.
	 * [-1: Do not fade out automatically, [0~N]: The fade out time] (In seconds)
	 * @default -1.0
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 动画状态播放完成后的自动淡出时间。
	 * [-1: 不自动淡出, [0~N]: 淡出时间] （以秒为单位）
	 * @default -1.0
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	var autoFadeOutTime: Double = 0.0
	/**
	 * @private
	 */
	var fadeTotalTime: Double = 0.0
	/**
	 * - The name of the animation state. (Can be different from the name of the animation data)
	 * @readonly
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 动画状态名称。 （可以不同于动画数据）
	 * @readonly
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	var name: String = ""
	/**
	 * - The blend group name of the animation state.
	 * This property is typically used to specify the substitution of multiple animation states blend.
	 * @readonly
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 混合组名称。
	 * 该属性通常用来指定多个动画状态混合时的相互替换关系。
	 * @readonly
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	var group: String = ""
	private var _timelineDirty: Int = 2
	/**
	 * - xx: Play Enabled, Fade Play Enabled
	 * @internal
	 */
	var _playheadState: Int = 0
	/**
	 * -1: Fade in, 0: Fade complete, 1: Fade out;
	 * @internal
	 */
	var _fadeState: Int = -1
	/**
	 * -1: Fade start, 0: Fading, 1: Fade complete;
	 * @internal
	 */
	var _subFadeState: Int = -1
	/**
	 * @internal
	 */
	var _position: Double = 0.0
	/**
	 * @internal
	 */
	var _duration: Double = 0.0
	private var _weight: Double = 1.0
	private var _fadeTime: Double = 0.0
	private var _time: Double = 0.0
	/**
	 * @internal
	 */
	var _fadeProgress: Double = 0.0
	/**
	 * @internal
	 */
	var _weightResult: Double = 0.0
	private val _boneMask: FastArrayList<String> = FastArrayList()
	private val _boneTimelines: FastArrayList<TimelineState> = FastArrayList()
	private val _boneBlendTimelines: FastArrayList<TimelineState> = FastArrayList()
	private val _slotTimelines: FastArrayList<TimelineState> = FastArrayList()
	private val _slotBlendTimelines: FastArrayList<TimelineState> = FastArrayList()
	private val _constraintTimelines: FastArrayList<TimelineState> = FastArrayList()
	private val _animationTimelines: FastArrayList<TimelineState> = FastArrayList()
	private val _poseTimelines: FastArrayList<TimelineState> = FastArrayList()
	private var _animationData: AnimationData? = null
	private var _armature: Armature? = null
	/**
	 * @internal
	 */
	var _actionTimeline: ActionTimelineState? = null // Initial value.
	private var _zOrderTimeline: ZOrderTimelineState? = null // Initial value.
	private var _activeChildA: AnimationState? = null
	private var _activeChildB: AnimationState? = null
	/**
	 * @internal
	 */
	var _parent: AnimationState? = null

	override fun _onClear() {
		this._boneTimelines.fastForEach { timeline ->
			timeline.returnToPool()
		}

		this._boneBlendTimelines.fastForEach { timeline ->
			timeline.returnToPool()
		}

		this._slotTimelines.fastForEach { timeline ->
			timeline.returnToPool()
		}

		this._slotBlendTimelines.fastForEach { timeline ->
			timeline.returnToPool()
		}

		this._constraintTimelines.fastForEach { timeline ->
			timeline.returnToPool()
		}

		this._animationTimelines.fastForEach { timeline ->
			val animationState = timeline.targetAnimationState!!
			if (animationState._parent == this) {
				animationState._fadeState = 1
				animationState._subFadeState = 1
				animationState._parent = null
			}

			timeline.returnToPool()
		}

		this._actionTimeline?.returnToPool()
		this._zOrderTimeline?.returnToPool()

		this.actionEnabled = false
		this.additive = false
		this.displayControl = false
		this.resetToPose = false
		this.blendType = AnimationBlendType.None
		this.playTimes = 1
		this.layer = 0
		this.timeScale = 1.0
		this._weight = 1.0
		this.parameterX = 0.0
		this.parameterY = 0.0
		this.positionX = 0.0
		this.positionY = 0.0
		this.autoFadeOutTime = 0.0
		this.fadeTotalTime = 0.0
		this.name = ""
		this.group = ""

		this._timelineDirty = 2
		this._playheadState = 0
		this._fadeState = -1
		this._subFadeState = -1
		this._position = 0.0
		this._duration = 0.0
		this._fadeTime = 0.0
		this._time = 0.0
		this._fadeProgress = 0.0
		this._weightResult = 0.0
		this._boneMask.clear()
		this._boneTimelines.clear()
		this._boneBlendTimelines.clear()
		this._slotTimelines.clear()
		this._slotBlendTimelines.clear()
		this._constraintTimelines.clear()
		this._animationTimelines.clear()
		this._poseTimelines.clear()
		// this._bonePoses.clear();
		this._animationData = null //
		this._armature = null //
		this._actionTimeline = null //
		this._zOrderTimeline = null
		this._activeChildA = null
		this._activeChildB = null
		this._parent = null
	}

	private fun _updateTimelines() {
		// Update constraint timelines.
		this._armature!!._constraints.fastForEach { constraint ->
			val timelineDatas = this._animationData?.getConstraintTimelines(constraint.name)

			if (timelineDatas != null) {
				timelineDatas.fastForEach { timelineData ->
					when (timelineData.type) {
						TimelineType.IKConstraint -> {
							val timeline = pool.iKConstraintTimelineState.borrow()
							timeline.targetIKConstraint = constraint as IKConstraint
							timeline.init(this._armature!!, this, timelineData)
                            this._constraintTimelines.add(timeline)
						}

						else -> Unit
					}
				}
			} else if (this.resetToPose) { // Pose timeline.
				val timeline = pool.iKConstraintTimelineState.borrow()
				timeline.targetIKConstraint = constraint as IKConstraint
				timeline.init(this._armature!!, this, null)
                this._constraintTimelines.add(timeline)
                this._poseTimelines.add(timeline)
			}
		}

	}

	private fun _updateBoneAndSlotTimelines() {
		run {
			// Update bone and surface timelines.
			val boneTimelines: FastStringMap<FastArrayList<TimelineState>> = FastStringMap()
			// Create bone timelines map.
			this._boneTimelines.fastForEach { timeline ->
				val timelineName = ((timeline.targetBlendState)!!.targetBone)!!.name
				if (timelineName !in boneTimelines) {
					boneTimelines[timelineName] = FastArrayList()
				}

                boneTimelines[timelineName]?.add(timeline)
			}

			this._boneBlendTimelines.fastForEach { timeline ->
				val timelineName = ((timeline.targetBlendState)!!.targetBone)!!.name
				if (timelineName !in boneTimelines) {
					boneTimelines[timelineName] = FastArrayList()
				}

                boneTimelines[timelineName]?.add(timeline)
			}
			//
			this._armature!!.getBones().fastForEach { bone ->
				val timelineName = bone.name
				if (!this.containsBoneMask(timelineName)) {
					return@fastForEach
				}

				if (timelineName in boneTimelines) { // Remove bone timeline from map.
					boneTimelines.remove(timelineName)
				} else { // Create new bone timeline.
					val timelineDatas = this._animationData?.getBoneTimelines(timelineName)
					val blendState =
						this._armature!!.animation.getBlendState(BlendState.BONE_TRANSFORM, bone.name, bone)

					if (timelineDatas != null) {
						timelineDatas.fastForEach { timelineData ->
							when (timelineData.type) {
								TimelineType.BoneAll -> {
									val timeline = pool.boneAllTimelineState.borrow()
									timeline.targetBlendState = blendState
									timeline.init(this._armature!!, this, timelineData)
                                    this._boneTimelines.add(timeline)
								}

								TimelineType.BoneTranslate -> {
									val timeline = pool.boneTranslateTimelineState.borrow()
									timeline.targetBlendState = blendState
									timeline.init(this._armature!!, this, timelineData)
                                    this._boneTimelines.add(timeline)
								}

								TimelineType.BoneRotate -> {
									val timeline = pool.boneRotateTimelineState.borrow()
									timeline.targetBlendState = blendState
									timeline.init(this._armature!!, this, timelineData)
                                    this._boneTimelines.add(timeline)
								}

								TimelineType.BoneScale -> {
									val timeline = pool.boneScaleTimelineState.borrow()
									timeline.targetBlendState = blendState
									timeline.init(this._armature!!, this, timelineData)
                                    this._boneTimelines.add(timeline)
								}

								TimelineType.BoneAlpha -> {
									val timeline = pool.alphaTimelineState.borrow()
									timeline.targetBlendState = this._armature!!.animation.getBlendState(BlendState.BONE_ALPHA, bone.name, bone)
									timeline.init(this._armature!!, this, timelineData)
                                    this._boneBlendTimelines.add(timeline)
								}

								TimelineType.Surface -> {
									val timeline = pool.surfaceTimelineState.borrow()
									timeline.targetBlendState = this._armature!!.animation.getBlendState(BlendState.SURFACE, bone.name, bone)
									timeline.init(this._armature!!, this, timelineData)
                                    this._boneBlendTimelines.add(timeline)
								}

								else -> Unit
							}
						}
					} else if (this.resetToPose) { // Pose timeline.
						if (bone._boneData?.isBone == true) {
							val timeline = pool.boneAllTimelineState.borrow()
							timeline.targetBlendState = blendState
							timeline.init(this._armature!!, this, null)
                            this._boneTimelines.add(timeline)
                            this._poseTimelines.add(timeline)
						} else {
							val timeline = pool.surfaceTimelineState.borrow()
							timeline.targetBlendState =
								this._armature!!.animation.getBlendState(BlendState.SURFACE, bone.name, bone)
							timeline.init(this._armature!!, this, null)
                            this._boneBlendTimelines.add(timeline)
                            this._poseTimelines.add(timeline)
						}
					}
				}
			}

			boneTimelines.fastKeyForEach { k ->
				// Remove bone timelines.
				boneTimelines[k]!!.fastForEach { timeline ->
					var index = this._boneTimelines.indexOf(timeline)
					if (index >= 0) {
						this._boneTimelines.splice(index, 1)
						timeline.returnToPool()
					}

					index = this._boneBlendTimelines.indexOf(timeline)
					if (index >= 0) {
						this._boneBlendTimelines.splice(index, 1)
						timeline.returnToPool()
					}
				}
			}
		}

		run {
			// Update slot timelines.
			val slotTimelines: FastStringMap<FastArrayList<TimelineState>> = FastStringMap()
			val ffdFlags = IntArrayList()
			// Create slot timelines map.
			this._slotTimelines.fastForEach { timeline ->
				val timelineName = (timeline.targetSlot)!!.name
				if (timelineName !in slotTimelines) {
					slotTimelines[timelineName] = FastArrayList()
				}

                slotTimelines[timelineName]?.add(timeline)
			}

			this._slotBlendTimelines.fastForEach { timeline ->
				val timelineName = ((timeline.targetBlendState)!!.targetSlot)!!.name
				if (timelineName !in slotTimelines) {
					slotTimelines[timelineName] = FastArrayList()
				}

                slotTimelines[timelineName]?.add(timeline)
			}
			//
			this._armature!!.getSlots().fastForEach { slot ->
				val boneName = slot.parent.name
				if (!this.containsBoneMask(boneName)) {
					return@fastForEach
				}

				val timelineName = slot.name
				if (timelineName in slotTimelines) { // Remove slot timeline from map.
					slotTimelines.remove(timelineName)
				} else { // Create new slot timeline.
					var displayIndexFlag = false
					var colorFlag = false
					ffdFlags.length = 0

					val timelineDatas = this._animationData?.getSlotTimelines(timelineName)
					if (timelineDatas != null) {
						timelineDatas.fastForEach { timelineData ->
							when (timelineData.type) {
								TimelineType.SlotDisplay -> {
									val timeline = pool.slotDisplayTimelineState.borrow()
									timeline.targetSlot = slot
									timeline.init(this._armature!!, this, timelineData)
                                    this._slotTimelines.add(timeline)
									displayIndexFlag = true
								}

								TimelineType.SlotZIndex -> {
									val timeline = pool.slotZIndexTimelineState.borrow()
									timeline.targetBlendState = this._armature!!.animation.getBlendState(
										BlendState.SLOT_Z_INDEX,
										slot.name,
										slot
									)
									timeline.init(this._armature!!, this, timelineData)
                                    this._slotBlendTimelines.add(timeline)
								}

								TimelineType.SlotColor -> {
									val timeline = pool.slotColorTimelineState.borrow()
									timeline.targetSlot = slot
									timeline.init(this._armature!!, this, timelineData)
                                    this._slotTimelines.add(timeline)
									colorFlag = true
								}

								TimelineType.SlotDeform -> {
									val timeline = pool.deformTimelineState.borrow()
									timeline.targetBlendState = this._armature!!.animation.getBlendState(
										BlendState.SLOT_DEFORM,
										slot.name,
										slot
									)
									timeline.init(this._armature!!, this, timelineData)

									if (timeline.targetBlendState != null) {
                                        this._slotBlendTimelines.add(timeline)
										ffdFlags.push(timeline.geometryOffset)
									} else {
										timeline.returnToPool()
									}
								}
								TimelineType.SlotAlpha -> {
									val timeline = pool.alphaTimelineState.borrow()
									timeline.targetBlendState = this._armature!!.animation.getBlendState(
										BlendState.SLOT_ALPHA,
										slot.name,
										slot
									)
									timeline.init(this._armature!!, this, timelineData)
                                    this._slotBlendTimelines.add(timeline)
								}

								else -> {

								}
							}
						}
					}

					if (this.resetToPose) { // Pose timeline.
						if (!displayIndexFlag) {
							val timeline = pool.slotDisplayTimelineState.borrow()
							timeline.targetSlot = slot
							timeline.init(this._armature!!, this, null)
                            this._slotTimelines.add(timeline)
                            this._poseTimelines.add(timeline)
						}

						if (!colorFlag) {
							val timeline = pool.slotColorTimelineState.borrow()
							timeline.targetSlot = slot
							timeline.init(this._armature!!, this, null)
                            this._slotTimelines.add(timeline)
                            this._poseTimelines.add(timeline)
						}

						for (i in 0 until slot.displayFrameCount) {
							val displayFrame = slot.getDisplayFrameAt(i)
							if (displayFrame.deformVertices.isEmpty()) {
								continue
							}

							val geometryData = displayFrame.getGeometryData()
							if (geometryData != null && ffdFlags.indexOf(geometryData.offset) < 0) {
								val timeline = pool.deformTimelineState.borrow()
								timeline.geometryOffset = geometryData.offset //
								timeline.displayFrame = displayFrame //
								timeline.targetBlendState = this._armature!!.animation.getBlendState(
									BlendState.SLOT_DEFORM,
									slot.name,
									slot
								)
								timeline.init(this._armature!!, this, null)
                                this._slotBlendTimelines.add(timeline)
                                this._poseTimelines.add(timeline)
							}
						}
					}
				}
			}

			slotTimelines.fastValueForEach { slotTimelines2 ->
				// Remove slot timelines.
				slotTimelines2.fastForEach { timeline ->
					var index = this._slotTimelines.indexOf(timeline)
					if (index >= 0) {
						this._slotTimelines.splice(index, 1)
						timeline.returnToPool()
					}

					index = this._slotBlendTimelines.indexOf(timeline)
					if (index >= 0) {
						this._slotBlendTimelines.splice(index, 1)
						timeline.returnToPool()
					}
				}
			}
		}
	}

	private fun _advanceFadeTime(passedTime: Double) {
		var passedTime = passedTime
		val isFadeOut = this._fadeState > 0

		if (this._subFadeState < 0) { // Fade start event.
			this._subFadeState = 0

			val eventActive = this._parent == null && this.actionEnabled
			if (eventActive) {
				val eventType = if (isFadeOut) EventObject.FADE_OUT else EventObject.FADE_IN
				if (this._armature!!.eventDispatcher.hasDBEventListener(eventType)) {
					val eventObject = pool.eventObject.borrow()
					eventObject.type = eventType
					eventObject.armature = this._armature!!
					eventObject.animationState = this
					this._armature!!.eventDispatcher!!.queueEvent(eventObject)
				}
			}
		}

		if (passedTime < 0.0) {
			passedTime = -passedTime
		}

		this._fadeTime += passedTime

		if (this._fadeTime >= this.fadeTotalTime) { // Fade complete.
			this._subFadeState = 1
			this._fadeProgress = if (isFadeOut) 0.0 else 1.0
		} else if (this._fadeTime > 0.0) { // Fading.
			this._fadeProgress =
					if (isFadeOut) (1.0 - this._fadeTime / this.fadeTotalTime) else (this._fadeTime / this.fadeTotalTime)
		} else { // Before fade.
			this._fadeProgress = if (isFadeOut) 1.0 else 0.0
		}

		if (this._subFadeState > 0) { // Fade complete event.
			if (!isFadeOut) {
				this._playheadState = this._playheadState or 1 // x1
				this._fadeState = 0
			}

			val eventActive = this._parent == null && this.actionEnabled
			if (eventActive) {
				val eventType = if (isFadeOut) EventObject.FADE_OUT_COMPLETE else EventObject.FADE_IN_COMPLETE
				if (this._armature!!.eventDispatcher.hasDBEventListener(eventType)) {
					val eventObject = pool.eventObject.borrow()
					eventObject.type = eventType
					eventObject.armature = this._armature!!
					eventObject.animationState = this
					this._armature!!.eventDispatcher!!.queueEvent(eventObject)
				}
			}
		}
	}

	/**
	 * @internal
	 */
	fun init(armature: Armature, animationData: AnimationData, animationConfig: AnimationConfig) {
		if (this._armature != null) {
			return
		}

		this._armature = armature
		this._animationData = animationData
		//
		this.resetToPose = animationConfig.resetToPose
		this.additive = animationConfig.additive
		this.displayControl = animationConfig.displayControl
		this.actionEnabled = animationConfig.actionEnabled
		this.blendType = animationData.blendType
		this.layer = animationConfig.layer
		this.playTimes = animationConfig.playTimes
		this.timeScale = animationConfig.timeScale
		this.fadeTotalTime = animationConfig.fadeInTime
		this.autoFadeOutTime = animationConfig.autoFadeOutTime
		this.name = if (animationConfig.name.isNotEmpty()) animationConfig.name else animationConfig.animation
		this.group = animationConfig.group
		//
		this._weight = animationConfig.weight

		if (animationConfig.pauseFadeIn) {
			this._playheadState = 2 // 10
		} else {
			this._playheadState = 3 // 11
		}

		if (animationConfig.duration < 0.0) {
			this._position = 0.0
			this._duration = this._animationData!!.duration

			if (animationConfig.position != 0.0) {
				if (this.timeScale >= 0.0) {
					this._time = animationConfig.position
				} else {
					this._time = animationConfig.position - this._duration
				}
			} else {
				this._time = 0.0
			}
		} else {
			this._position = animationConfig.position
			this._duration = animationConfig.duration
			this._time = 0.0
		}

		if (this.timeScale < 0.0 && this._time == 0.0) {
			this._time = -0.000001 // Turn to end.
		}

		if (this.fadeTotalTime <= 0.0) {
			this._fadeProgress = 0.999999 // Make different.
		}

		if (animationConfig.boneMask.length > 0) {
			this._boneMask.lengthSet = animationConfig.boneMask.length
			//for (var i = 0, l = this._boneMask.length; i < l; ++i) {
			for (i in 0 until this._boneMask.length) {
				this._boneMask[i] = animationConfig.boneMask[i]
			}
		}

		this._actionTimeline = pool.actionTimelineState.borrow()
		this._actionTimeline!!.init(this._armature!!, this, this._animationData!!.actionTimeline)
		this._actionTimeline!!._currentTime = this._time

		if (this._actionTimeline!!._currentTime < 0.0) {
			this._actionTimeline!!._currentTime = this._duration - this._actionTimeline!!._currentTime
		}

		if (this._animationData!!.zOrderTimeline != null) {
			this._zOrderTimeline = pool.zOrderTimelineState.borrow()
			this._zOrderTimeline!!.init(this._armature!!, this, this._animationData!!.zOrderTimeline)
		}
	}

	/**
	 * @internal
	 */
	fun advanceTime(passedTime: Double, cacheFrameRate: Double) {
		var passedTime = passedTime

		// Update fade time.
		if (this._fadeState != 0 || this._subFadeState != 0) {
			this._advanceFadeTime(passedTime)
		}
		// Update time.
		if (this._playheadState == 3) { // 11
			if (this.timeScale != 1.0) {
				passedTime *= this.timeScale
			}

			this._time += passedTime
		}
		// Update timeline.
		if (this._timelineDirty != 0) {
			if (this._timelineDirty == 2) {
				this._updateTimelines()
			}

			this._timelineDirty = 0
			this._updateBoneAndSlotTimelines()
		}

		val isBlendDirty = this._fadeState != 0 || this._subFadeState == 0
		val isCacheEnabled = this._fadeState == 0 && cacheFrameRate > 0.0
		var isUpdateTimeline = true
		var isUpdateBoneTimeline = true
		var time = this._time
		this._weightResult = this._weight * this._fadeProgress

		if (this._parent != null) {
			this._weightResult *= this._parent!!._weightResult
		}

		if (this._actionTimeline!!.playState <= 0) { // Update main timeline.
			this._actionTimeline?.update(time)
		}

		if (this._weight == 0.0) {
			return
		}

		if (isCacheEnabled) { // Cache time internval.
			val internval = cacheFrameRate * 2.0
			this._actionTimeline!!._currentTime = floor(this._actionTimeline!!._currentTime * internval) / internval
		}

		if (this._zOrderTimeline != null && this._zOrderTimeline!!.playState <= 0) { // Update zOrder timeline.
			this._zOrderTimeline?.update(time)
		}

		if (isCacheEnabled) { // Update cache.
			val cacheFrameIndex = floor(this._actionTimeline!!._currentTime * cacheFrameRate).toInt() // uint
			if (this._armature?._cacheFrameIndex == cacheFrameIndex) { // Same cache.
				isUpdateTimeline = false
				isUpdateBoneTimeline = false
			} else {
				this._armature?._cacheFrameIndex = cacheFrameIndex

				if (this._animationData!!.cachedFrames[cacheFrameIndex]) { // Cached.
					isUpdateBoneTimeline = false
				} else { // Cache.
					this._animationData!!.cachedFrames[cacheFrameIndex] = true
				}
			}
		}

		if (isUpdateTimeline) {
			var isBlend = false
			var prevTarget: BlendState? = null //

			if (isUpdateBoneTimeline) {
				//for (var i = 0, l = this._boneTimelines.length; i < l; ++i) {
				for (i in 0 until this._boneTimelines.length) {
					val timeline = this._boneTimelines[i]

					if (timeline.playState <= 0) {
						timeline.update(time)
					}

					if (timeline.targetBlendState != prevTarget) {
						val blendState = timeline.targetBlendState
						isBlend = blendState!!.update(this)
						prevTarget = blendState

						if (blendState.dirty == 1) {
							val pose = (blendState.targetBone)!!.animationPose
							pose.xf = 0f
							pose.yf = 0f
							pose.rotation = 0f
							pose.skew = 0f
							pose.scaleX = 1f
							pose.scaleY = 1f
						}
					}

					if (isBlend) {
						timeline.blend(isBlendDirty)
					}
				}
			}

			//for (var i = 0, l = this._boneBlendTimelines.length; i < l; ++i) {
			for (i in 0 until this._boneBlendTimelines.length) {
				val timeline = this._boneBlendTimelines[i]

				if (timeline.playState <= 0) {
					timeline.update(time)
				}

				if ((timeline.targetBlendState)!!.update(this)) {
					timeline.blend(isBlendDirty)
				}
			}

			if (this.displayControl) {
				//for (var i = 0, l = this._slotTimelines.length; i < l; ++i) {
				for (i in 0 until this._slotTimelines.length) {
					val timeline = this._slotTimelines[i]
					if (timeline.playState <= 0) {
						val slot = timeline.targetSlot
						val displayController = slot!!.displayController

						if (
							displayController == null ||
							displayController == this.name ||
							displayController == this.group
						) {
							timeline.update(time)
						}
					}
				}
			}

			//for (var i = 0, l = this._slotBlendTimelines.length; i < l; ++i) {
			for (i in 0 until this._slotBlendTimelines.length) {
				val timeline = this._slotBlendTimelines[i]
				if (timeline.playState <= 0) {
					val blendState = timeline.targetBlendState
					timeline.update(time)

					if (blendState!!.update(this)) {
						timeline.blend(isBlendDirty)
					}
				}
			}

			//for (var i = 0, l = this._constraintTimelines.length; i < l; ++i) {
			for (i in 0 until this._constraintTimelines.length) {
				val timeline = this._constraintTimelines[i]
				if (timeline.playState <= 0) {
					timeline.update(time)
				}
			}

			if (this._animationTimelines.lengthSet > 0) {
				var dL = 100.0
				var dR = 100.0
				var leftState: AnimationState? = null
				var rightState: AnimationState? = null

				//for (var i = 0, l = this._animationTimelines.length; i < l; ++i) {
				for (i in 0 until this._animationTimelines.length) {
					val timeline = this._animationTimelines[i]
					if (timeline.playState <= 0) {
						timeline.update(time)
					}

					if (this.blendType == AnimationBlendType.E1D) { // TODO
						val animationState = timeline.targetAnimationState
						val d = this.parameterX - animationState!!.positionX

						if (d >= 0.0) {
							if (d < dL) {
								dL = d
								leftState = animationState
							}
						} else {
							if (-d < dR) {
								dR = -d
								rightState = animationState
							}
						}
					}
				}

				if (leftState != null) {
					if (this._activeChildA != leftState) {
						if (this._activeChildA != null) {
							this._activeChildA!!.weight = 0.0
						}

						this._activeChildA = leftState
						this._activeChildA!!.activeTimeline()
					}

					if (this._activeChildB != rightState) {
						if (this._activeChildB != null) {
							this._activeChildB!!.weight = 0.0
						}

						this._activeChildB = rightState
					}

					leftState.weight = dR / (dL + dR)

					if (rightState != null) {
						rightState.weight = 1.0 - leftState.weight
					}
				}
			}
		}

		if (this._fadeState == 0) {
			if (this._subFadeState > 0) {
				this._subFadeState = 0

				if (this._poseTimelines.lengthSet > 0) { // Remove pose timelines.
					this._poseTimelines.fastForEach { timeline ->
						var index = this._boneTimelines.indexOf(timeline)
						if (index >= 0) {
							this._boneTimelines.splice(index, 1)
							timeline.returnToPool()
							return@fastForEach
						}

						index = this._boneBlendTimelines.indexOf(timeline)
						if (index >= 0) {
							this._boneBlendTimelines.splice(index, 1)
							timeline.returnToPool()
							return@fastForEach
						}

						index = this._slotTimelines.indexOf(timeline)
						if (index >= 0) {
							this._slotTimelines.splice(index, 1)
							timeline.returnToPool()
							return@fastForEach
						}

						index = this._slotBlendTimelines.indexOf(timeline)
						if (index >= 0) {
							this._slotBlendTimelines.splice(index, 1)
							timeline.returnToPool()
							return@fastForEach
						}

						index = this._constraintTimelines.indexOf(timeline)
						if (index >= 0) {
							this._constraintTimelines.splice(index, 1)
							timeline.returnToPool()
							return@fastForEach
						}
					}

					this._poseTimelines.lengthSet = 0
				}
			}

			if (this._actionTimeline!!.playState > 0) {
				if (this.autoFadeOutTime >= 0.0) { // Auto fade out.
					this.fadeOut(this.autoFadeOutTime)
				}
			}
		}
	}
	/**
	 * - Continue play.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 继续播放。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun play() {
		this._playheadState = 3 // 11
	}
	/**
	 * - Stop play.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 暂停播放。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun stop() {
		this._playheadState = this._playheadState and 1 // 0x
	}
	/**
	 * - Fade out the animation state.
	 * @param fadeOutTime - The fade out time. (In seconds)
	 * @param pausePlayhead - Whether to pause the animation playing when fade out.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 淡出动画状态。
	 * @param fadeOutTime - 淡出时间。 （以秒为单位）
	 * @param pausePlayhead - 淡出时是否暂停播放。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun fadeOut(fadeOutTime: Double, pausePlayhead: Boolean = true) {
		var fadeOutTime = fadeOutTime
		if (fadeOutTime < 0.0) {
			fadeOutTime = 0.0
		}

		if (pausePlayhead) {
			this._playheadState = this._playheadState and 2 // x0
		}

		if (this._fadeState > 0) {
			if (fadeOutTime > this.fadeTotalTime - this._fadeTime) { // If the animation is already in fade out, the new fade out will be ignored.
				return
			}
		} else {
			this._fadeState = 1
			this._subFadeState = -1

			if (fadeOutTime <= 0.0 || this._fadeProgress <= 0.0) {
				this._fadeProgress = 0.000001 // Modify fade progress to different value.
			}

			this._boneTimelines.fastForEach { timeline ->
				timeline.fadeOut()
			}

			this._boneBlendTimelines.fastForEach { timeline ->
				timeline.fadeOut()
			}

			this._slotTimelines.fastForEach { timeline ->
				timeline.fadeOut()
			}

			this._slotBlendTimelines.fastForEach { timeline ->
				timeline.fadeOut()
			}

			this._constraintTimelines.fastForEach { timeline ->
				timeline.fadeOut()
			}

			this._animationTimelines.fastForEach { timeline ->
				timeline.fadeOut()
				//
				val animaitonState = timeline.targetAnimationState
				animaitonState!!.fadeOut(999999.0, true)
			}
		}

		this.displayControl = false //
		this.fadeTotalTime = if (this._fadeProgress > 0.000001) fadeOutTime / this._fadeProgress else 0.0
		this._fadeTime = this.fadeTotalTime * (1.0 - this._fadeProgress)
	}
	/**
	 * - Check if a specific bone mask is included.
	 * @param boneName - The bone name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 检查是否包含特定骨骼遮罩。
	 * @param boneName - 骨骼名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun containsBoneMask(boneName: String): Boolean {
		return this._boneMask.lengthSet == 0 || this._boneMask.indexOf(boneName) >= 0
	}
	/**
	 * - Add a specific bone mask.
	 * @param boneName - The bone name.
	 * @param recursive - Whether or not to add a mask to the bone's sub-bone.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 添加特定的骨骼遮罩。
	 * @param boneName - 骨骼名称。
	 * @param recursive - 是否为该骨骼的子骨骼添加遮罩。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun addBoneMask(boneName: String, recursive: Boolean = true) {
		val currentBone = this._armature?.getBone(boneName) ?: return

		if (this._boneMask.indexOf(boneName) < 0) { // Add mixing
            this._boneMask.add(boneName)
		}

		if (recursive) { // Add recursive mixing.
			this._armature!!.getBones().fastForEach { bone ->
				if (this._boneMask.indexOf(bone.name) < 0 && currentBone.contains(bone)) {
                    this._boneMask.add(bone.name)
				}
			}
		}

		this._timelineDirty = 1
	}
	/**
	 * - Remove the mask of a specific bone.
	 * @param boneName - The bone name.
	 * @param recursive - Whether to remove the bone's sub-bone mask.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 删除特定骨骼的遮罩。
	 * @param boneName - 骨骼名称。
	 * @param recursive - 是否删除该骨骼的子骨骼遮罩。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun removeBoneMask(boneName: String, recursive: Boolean = true) {
		val index = this._boneMask.indexOf(boneName)
		if (index >= 0) { // Remove mixing.
			this._boneMask.splice(index, 1)
		}

		if (recursive) {
			val currentBone = this._armature?.getBone(boneName)
			if (currentBone != null) {
				val bones = this._armature!!.getBones()
				if (this._boneMask.lengthSet > 0) { // Remove recursive mixing.
					bones.fastForEach { bone ->
						val index = this._boneMask.indexOf(bone.name)
						if (index >= 0 && currentBone.contains(bone)) {
							this._boneMask.splice(index, 1)
						}
					}
				} else { // Add unrecursive mixing.
					bones.fastForEach { bone ->
						if (bone == currentBone) {
							return@fastForEach
						}

						if (!currentBone.contains(bone)) {
                            this._boneMask.add(bone.name)
						}
					}
				}
			}
		}

		this._timelineDirty = 1
	}
	/**
	 * - Remove all bone masks.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 删除所有骨骼遮罩。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun removeAllBoneMask() {
		this._boneMask.lengthSet = 0
		this._timelineDirty = 1
	}

	/**
	 * @private
	 */
	fun addState(animationState: AnimationState, timelineDatas: FastArrayList<TimelineData>? = null) {
        timelineDatas?.fastForEach { timelineData ->
            when (timelineData.type) {
                TimelineType.AnimationProgress -> {
                    val timeline = pool.animationProgressTimelineState.borrow()
                    timeline.targetAnimationState = animationState
                    timeline.init(this._armature!!, this, timelineData)
                    this._animationTimelines.add(timeline)

                    if (this.blendType != AnimationBlendType.None) {
                        val animaitonTimelineData = timelineData as AnimationTimelineData
                        animationState.positionX = animaitonTimelineData.x
                        animationState.positionY = animaitonTimelineData.y
                        animationState.weight = 0.0
                    }

                    animationState._parent = this
                    this.resetToPose = false
                }

                TimelineType.AnimationWeight -> {
                    val timeline = pool.animationWeightTimelineState.borrow()
                    timeline.targetAnimationState = animationState
                    timeline.init(this._armature!!, this, timelineData)
                    this._animationTimelines.add(timeline)
                }

                TimelineType.AnimationParameter -> {
                    val timeline = pool.animationParametersTimelineState.borrow()
                    timeline.targetAnimationState = animationState
                    timeline.init(this._armature!!, this, timelineData)
                    this._animationTimelines.add(timeline)
                }

                else -> {

                }
            }
        }

		if (animationState._parent == null) {
			animationState._parent = this
		}
	}

	/**
	 * @internal
	 */
	fun activeTimeline() {
		this._slotTimelines.fastForEach { timeline ->
			timeline.dirty = true
			timeline._currentTime = -1.0
		}
	}
	/**
	 * - Whether the animation state is fading in.
	 * @version DragonBones 5.1
	 * @language en_US
	 */
	/**
	 * - 是否正在淡入。
	 * @version DragonBones 5.1
	 * @language zh_CN
	 */
	val isFadeIn: Boolean
		get() {
			return this._fadeState < 0
		}
	/**
	 * - Whether the animation state is fading out.
	 * @version DragonBones 5.1
	 * @language en_US
	 */
	/**
	 * - 是否正在淡出。
	 * @version DragonBones 5.1
	 * @language zh_CN
	 */
	val isFadeOut: Boolean
		get() {
			return this._fadeState > 0
		}
	/**
	 * - Whether the animation state is fade completed.
	 * @version DragonBones 5.1
	 * @language en_US
	 */
	/**
	 * - 是否淡入或淡出完毕。
	 * @version DragonBones 5.1
	 * @language zh_CN
	 */
	val isFadeComplete: Boolean
		get() {
			return this._fadeState == 0
		}
	/**
	 * - Whether the animation state is playing.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 是否正在播放。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val isPlaying: Boolean
		get() {
			return (this._playheadState and 2) != 0 && this._actionTimeline!!.playState <= 0
		}
	/**
	 * - Whether the animation state is play completed.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 是否播放完毕。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val isCompleted: Boolean
		get() {
			return this._actionTimeline!!.playState > 0
		}
	/**
	 * - The times has been played.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 已经循环播放的次数。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val currentPlayTimes: Int
		get() {
			return this._actionTimeline!!.currentPlayTimes
		}
	/**
	 * - The total time. (In seconds)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 总播放时间。 （以秒为单位）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val totalTime: Double
		get() {
			return this._duration
		}
	/**
	 * - The time is currently playing. (In seconds)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 当前播放的时间。 （以秒为单位）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var currentTime: Double
		get() = this._actionTimeline!!._currentTime
		set(value) {
			var value = value
			val currentPlayTimes =
				this._actionTimeline!!.currentPlayTimes - (if (this._actionTimeline!!.playState > 0) 1 else 0)
			if (value < 0 || this._duration < value) {
				value = (value % this._duration) + currentPlayTimes * this._duration
				if (value < 0) {
					value += this._duration
				}
			}

			if (
				this.playTimes > 0 && currentPlayTimes == this.playTimes - 1 &&
				value == this._duration && this._parent == null
			) {
				value = this._duration - 0.000001 //
			}

			if (this._time == value) {
				return
			}

			this._time = value
			this._actionTimeline!!.setCurrentTime(this._time)

			this._zOrderTimeline?.playState = -1

			this._boneTimelines.fastForEach { timeline ->
				timeline.playState = -1
			}
			this._slotTimelines.fastForEach { timeline ->
				timeline.playState = -1
			}
		}
	/**
	 * - The blend weight.
	 * @default 1.0
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 混合权重。
	 * @default 1.0
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	/**
	 * - The animation data.
	 * @see dragonBones.AnimationData
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	var weight: Double
		get() = this._weight
		set(value) {
			if (this._weight == value) return
			this._weight = value
			for (n in 0 until _boneTimelines.size) _boneTimelines[n].dirty = true
			for (n in 0 until _boneBlendTimelines.size) _boneBlendTimelines[n].dirty = true
			for (n in 0 until _slotBlendTimelines.size) _slotBlendTimelines[n].dirty = true
		}
	/**
	 * - 动画数据。
	 * @see dragonBones.AnimationData
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val animationData: AnimationData get() = this._animationData!!
}

/**
 * @internal
 */
class BlendState(pool: SingleObjectPool<BlendState>) : BaseObject(pool) {
	companion object {
		const val BONE_TRANSFORM: String = "boneTransform"
		const val BONE_ALPHA: String = "boneAlpha"
		const val SURFACE: String = "surface"
		const val SLOT_DEFORM: String = "slotDeform"
		const val SLOT_ALPHA: String = "slotAlpha"
		const val SLOT_Z_INDEX: String = "slotZIndex"
	}

	override fun toString(): String {
		return "[class dragonBones.BlendState]"
	}

	var dirty: Int = 0
	var layer: Int = 0
	var leftWeight: Double = 0.0
	var layerWeight: Double = 0.0
	var blendWeight: Double = 0.0
	//var target: BaseObject? = null
    var targetSlot: Slot? = null
    var targetBone: Bone? = null
    var targetSurface: Surface? = null
    var targetTransformObject: TransformObject? = null
    val targetCommon: TransformObject? get() = targetSlot ?: targetBone ?: targetSurface ?: targetTransformObject

	override fun _onClear() {
		this.reset()
		this.targetSlot = null
        this.targetBone = null
        this.targetSurface = null
        this.targetTransformObject = null
	}

	fun reset() {
		this.dirty = 0
		this.layer = 0
		this.leftWeight = 0.0
		this.layerWeight = 0.0
		this.blendWeight = 0.0
	}

	fun update(animationState: AnimationState): Boolean {
		val animationLayer = animationState.layer
		var animationWeight = animationState._weightResult

		if (this.dirty > 0) {
			if (this.leftWeight > 0.0) {
				if (this.layer != animationLayer) {
					if (this.layerWeight >= this.leftWeight) {
						this.dirty++
						this.layer = animationLayer
						this.leftWeight = 0.0
						this.blendWeight = 0.0

						return false
					}

					this.layer = animationLayer
					this.leftWeight -= this.layerWeight
					this.layerWeight = 0.0
				}

				animationWeight *= this.leftWeight
				this.dirty++
				this.blendWeight = animationWeight
				this.layerWeight += this.blendWeight

				return true
			}

			return false
		}

		this.dirty++
		this.layer = animationLayer
		this.leftWeight = 1.0
		this.blendWeight = animationWeight
		this.layerWeight = animationWeight

		return true
	}
}
