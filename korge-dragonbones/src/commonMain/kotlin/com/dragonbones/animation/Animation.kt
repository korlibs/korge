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

@file:Suppress("KDocUnresolvedReference")

package com.dragonbones.animation

import com.dragonbones.armature.*
import com.dragonbones.core.*
import com.soywiz.kds.iterators.*
import com.dragonbones.model.*
import com.dragonbones.util.*
import com.soywiz.kds.*
import com.soywiz.klogger.*

/**
 * - The animation player is used to play the animation data and manage the animation states.
 * @see dragonBones.AnimationData
 * @see dragonBones.AnimationState
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 动画播放器用来播放动画数据和管理动画状态。
 * @see dragonBones.AnimationData
 * @see dragonBones.AnimationState
 * @version DragonBones 3.0
 * @language zh_CN
 */
class Animation(pool: SingleObjectPool<Animation>) : BaseObject(pool) {
	override fun toString(): String {
		return "[class dragonBones.Animation]"
	}
	/**
	 * - The play speed of all animations. [0: Stop, (0~1): Slow, 1: Normal, (1~N): Fast]
	 * @default 1.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 所有动画的播放速度。 [0: 停止播放, (0~1): 慢速播放, 1: 正常播放, (1~N): 快速播放]
	 * @default 1.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var timeScale: Double = 1.0
	/**
	 * Update bones and slots cachedFrameIndices.
	 */
	private var _animationDirty: Boolean = false //
	private var _inheritTimeScale: Double = 1.0
	private val _animationNames: FastArrayList<String> = FastArrayList()
	private val _animationStates: FastArrayList<AnimationState> = FastArrayList()
	private val _animations: FastStringMap<AnimationData> = FastStringMap()
	private val _blendStates: FastStringMap<FastStringMap<BlendState>> = FastStringMap()
	private var _armature: Armature? = null
	private var _animationConfig: AnimationConfig? = null // Initial value.
	private var _lastAnimationState: AnimationState? = null

	override fun _onClear() {
		this._animationStates.fastForEach { animationState ->
			animationState.returnToPool()
		}

		this._animations.clear()

		this._blendStates.fastValueForEach { blendStates ->
			blendStates.fastKeyForEach { kB ->
				blendStates[kB]?.returnToPool()
			}
		}

		this._blendStates.clear()

		this._animationConfig?.returnToPool()

		this.timeScale = 1.0

		this._animationDirty = false
		this._inheritTimeScale = 1.0
		this._animationNames.clear()
		this._animationStates.clear()
		//this._animations.clear();
		this._armature = null //
		this._animationConfig = null //
		this._lastAnimationState = null
	}

	private fun _fadeOut(animationConfig: AnimationConfig) {
		when (animationConfig.fadeOutMode) {
			AnimationFadeOutMode.SameLayer -> {
				this._animationStates.fastForEach { animationState ->
					if (animationState._parent == null) {
						if (animationState.layer == animationConfig.layer) {
							animationState.fadeOut(animationConfig.fadeOutTime, animationConfig.pauseFadeOut)
						}
					}
				}
			}

			AnimationFadeOutMode.SameGroup -> {

				this._animationStates.fastForEach { animationState ->
					if (animationState._parent == null) {
						if (animationState.group == animationConfig.group) {
							animationState.fadeOut(animationConfig.fadeOutTime, animationConfig.pauseFadeOut)
						}
					}
				}
			}

			AnimationFadeOutMode.SameLayerAndGroup -> {
				this._animationStates.fastForEach { animationState ->
					if (animationState._parent == null) {
						if (
							animationState.layer == animationConfig.layer &&
							animationState.group == animationConfig.group
						) {
							animationState.fadeOut(animationConfig.fadeOutTime, animationConfig.pauseFadeOut)
						}
					}
				}
			}

			AnimationFadeOutMode.All -> {
				this._animationStates.fastForEach { animationState ->
					if (animationState._parent == null) {
						animationState.fadeOut(animationConfig.fadeOutTime, animationConfig.pauseFadeOut)
					}
				}
			}

			AnimationFadeOutMode.Single -> { // TODO
			}
			else -> {
			}
		}
	}

	/**
	 * @internal
	 */
	fun init(armature: Armature) {
		if (this._armature != null) {
			return
		}

		this._armature = armature
		this._animationConfig = pool.animationConfig.borrow()
	}

	/**
	 * @internal
	 */
	fun advanceTime(passedTime: Double) {
		//println(this.animationNames)
		var passedTime = passedTime
		if (passedTime < 0.0) { // Only animationState can reverse play.
			passedTime = -passedTime
		}

		if (this._armature!!.inheritAnimation && this._armature?._parent != null) { // Inherit parent animation timeScale.
			this._inheritTimeScale = this._armature!!._parent!!._armature!!.animation._inheritTimeScale * this.timeScale
		}
		else {
			this._inheritTimeScale = this.timeScale
		}

		if (this._inheritTimeScale != 1.0) {
			passedTime *= this._inheritTimeScale
		}

		this._blendStates.fastValueForEach { blendStates ->
			blendStates.fastValueForEach { state ->
				state.reset()
			}
		}

		val animationStateCount = this._animationStates.length
		if (animationStateCount == 1) {
			val animationState = this._animationStates[0]
			if (animationState._fadeState > 0 && animationState._subFadeState > 0) {
				//this._armature?._dragonBones?.bufferObject(animationState)
				animationState.returnToPool()
				this._animationStates.length = 0
				this._lastAnimationState = null
			}
			else {
				val animationData = animationState.animationData
				val cacheFrameRate = animationData.cacheFrameRate

				if (this._animationDirty && cacheFrameRate > 0.0) { // Update cachedFrameIndices.
					this._animationDirty = false

					this._armature!!.getBones().fastForEach { bone ->
						bone._cachedFrameIndices = animationData.getBoneCachedFrameIndices(bone.name)
					}

					this._armature!!.getSlots().fastForEach { slot ->
						if (slot.displayFrameCount > 0) {
							val rawDisplayData = slot.getDisplayFrameAt(0).rawDisplayData
							if (
								rawDisplayData != null &&
								rawDisplayData.parent == this._armature!!.armatureData.defaultSkin
							) {
								slot._cachedFrameIndices = animationData.getSlotCachedFrameIndices(slot.name)
								return@fastForEach
							}
						}

						slot._cachedFrameIndices = null
					}
				}

				animationState.advanceTime(passedTime, cacheFrameRate)
			}
		}
		else if (animationStateCount > 1) {
			// for (let i = 0, r = 0; i < animationStateCount; ++i) {
			var r = 0
			for (i in 0 until animationStateCount) {
				val animationState = this._animationStates[i]
				if (animationState._fadeState > 0 && animationState._subFadeState > 0) {
					r++
					//this._armature?._dragonBones?.bufferObject(animationState)
					animationState.returnToPool()
					this._animationDirty = true

					if (this._lastAnimationState == animationState) { // Update last animation state.
						this._lastAnimationState = null
					}
				}
				else {
					if (r > 0) {
						this._animationStates[i - r] = animationState
					}

					animationState.advanceTime(passedTime, 0.0)
				}

				if (i == animationStateCount - 1 && r > 0) { // Modify animation states size.
					this._animationStates.length -= r

					if (this._lastAnimationState == null && this._animationStates.length > 0) {
						this._lastAnimationState = this._animationStates[this._animationStates.length - 1]
					}
				}
			}

			this._armature?._cacheFrameIndex = -1
		}
		else {
			this._armature?._cacheFrameIndex = -1
		}
	}
	/**
	 * - Clear all animations states.
	 * @see dragonBones.AnimationState
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 清除所有的动画状态。
	 * @see dragonBones.AnimationState
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun reset() {
		this._animationStates.fastForEach { animationState ->
			animationState.returnToPool()
		}

		this._animationDirty = false
		this._animationConfig?.clear()
		this._animationStates.clear()
		this._lastAnimationState = null
	}
	/**
	 * - Pause a specific animation state.
	 * @param animationName - The name of animation state. (If not set, it will pause all animations)
	 * @see dragonBones.AnimationState
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 暂停指定动画状态的播放。
	 * @param animationName - 动画状态名称。 （如果未设置，则暂停所有动画）
	 * @see dragonBones.AnimationState
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun stop(animationName: String? = null) {
		if (animationName != null) {
			val animationState = this.getState(animationName)
			if (animationState != null) {
				animationState.stop()
			}
		}
		else {
			this._animationStates.fastForEach { animationState ->
				animationState.stop()
			}
		}
	}
	/**
	 * - Play animation with a specific animation config.
	 * The API is still in the experimental phase and may encounter bugs or stability or compatibility issues when used.
	 * @param animationConfig - The animation config.
	 * @returns The playing animation state.
	 * @see dragonBones.AnimationConfig
	 * @beta
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 通过指定的动画配置来播放动画。
	 * 该 API 仍在实验阶段，使用时可能遭遇 bug 或稳定性或兼容性问题。
	 * @param animationConfig - 动画配置。
	 * @returns 播放的动画状态。
	 * @see dragonBones.AnimationConfig
	 * @beta
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	fun playConfig(animationConfig: AnimationConfig): AnimationState? {
		val animationName = animationConfig.animation
		if (animationName !in this._animations) {
			Console.warn(
				"Non-existent animation.\n",
				"DragonBones name: " + this._armature?.armatureData?.parent?.name,
				"Armature name: " + this._armature?.name,
				"Animation name: $animationName"
			)

			return null
		}

		val animationData = this._animations[animationName]!!

		if (animationConfig.fadeOutMode == AnimationFadeOutMode.Single) {
			this._animationStates.forEach { animationState ->
				if (
					animationState._fadeState < 1 &&
					animationState.layer == animationConfig.layer &&
					animationState.animationData == animationData
				) {
					return animationState
				}
			}
		}

		if (this._animationStates.size == 0) {
			animationConfig.fadeInTime = 0.0
		}
		else if (animationConfig.fadeInTime < 0.0) {
			animationConfig.fadeInTime = animationData.fadeInTime
		}

		if (animationConfig.fadeOutTime < 0.0) {
			animationConfig.fadeOutTime = animationConfig.fadeInTime
		}

		if (animationConfig.timeScale <= -100.0) {
			animationConfig.timeScale = 1.0 / animationData.scale
		}

		if (animationData.frameCount > 0) {
			if (animationConfig.position < 0.0) {
				animationConfig.position %= animationData.duration
				animationConfig.position = animationData.duration - animationConfig.position
			}
			else if (animationConfig.position == animationData.duration) {
				animationConfig.position -= 0.000001 // Play a little time before end.
			}
			else if (animationConfig.position > animationData.duration) {
				animationConfig.position %= animationData.duration
			}

			if (animationConfig.duration > 0.0 && animationConfig.position + animationConfig.duration > animationData.duration) {
				animationConfig.duration = animationData.duration - animationConfig.position
			}

			if (animationConfig.playTimes < 0) {
				animationConfig.playTimes = animationData.playTimes
			}
		}
		else {
			animationConfig.playTimes = 1
			animationConfig.position = 0.0

			if (animationConfig.duration > 0.0) {
				animationConfig.duration = 0.0
			}
		}

		if (animationConfig.duration == 0.0) {
			animationConfig.duration = -1.0
		}

		this._fadeOut(animationConfig)
		//
		val animationState = pool.animationState.borrow()
		animationState.init(this._armature!!, animationData, animationConfig)
		this._animationDirty = true
		this._armature?._cacheFrameIndex = -1

		if (this._animationStates.size > 0) { // Sort animation state.
			var  added = false

			val l = this._animationStates.size
			// for (let i = 0, l = this._animationStates.length; i < l; ++i) {
			for (i in 0 until this._animationStates.size) {
				if (animationState.layer > this._animationStates[i].layer) {
					added = true
					this._animationStates.splice(i, 0, animationState)
					break
				}
				else if (i != l - 1 && animationState.layer > this._animationStates[i + 1].layer) {
					added = true
					this._animationStates.splice(i + 1, 0, animationState)
					break
				}
			}

			if (!added) {
				this._animationStates.add(animationState)
			}
		}
		else {
			this._animationStates.add(animationState)
		}

		this._armature!!.getSlots().fastForEach { slot ->
			// Child armature play same name animation.
			val childArmature = slot.childArmature
			if (
				childArmature != null && childArmature.inheritAnimation &&
				childArmature.animation.hasAnimation(animationName) &&
				childArmature.animation.getState(animationName) == null
			) {
				childArmature.animation.fadeIn(animationName) //
			}
		}

		animationData.animationTimelines.fastKeyForEach { k ->
			// Blend animation node.
			val childAnimationState = this.fadeIn(k, 0.0, 1, animationState.layer, "", AnimationFadeOutMode.Single) ?: return@fastKeyForEach

			val timelines = animationData.animationTimelines[k]
			childAnimationState.actionEnabled = false
			childAnimationState.resetToPose = false
			childAnimationState.stop()
			animationState.addState(childAnimationState, timelines)
			//
			val index = this._animationStates.indexOf(animationState)
			val childIndex = this._animationStates.indexOf(childAnimationState)
			if (childIndex < index) {
				this._animationStates.splice(index, 1)
				this._animationStates.splice(childIndex, 0, animationState)
			}
		}

		// if (!this._armature._lockUpdate && animationConfig.fadeInTime <= 0.0) { // Blend animation state, update armature.
		//     this._armature.advanceTime(0.0);
		// }

		this._lastAnimationState = animationState

		return animationState
	}
	/**
	 * - Play a specific animation.
	 * @param animationName - The name of animation data. (If not set, The default animation will be played, or resume the animation playing from pause status, or replay the last playing animation)
	 * @param playTimes - Playing repeat times. [-1: Use default value of the animation data, 0: No end loop playing, [1~N]: Repeat N times] (default: -1)
	 * @returns The playing animation state.
	 * @example
	 * <pre>
	 *     armature.animation.play("walk");
	 * </pre>
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 播放指定动画。
	 * @param animationName - 动画数据名称。 （如果未设置，则播放默认动画，或将暂停状态切换为播放状态，或重新播放之前播放的动画）
	 * @param playTimes - 循环播放次数。 [-1: 使用动画数据默认值, 0: 无限循环播放, [1~N]: 循环播放 N 次] （默认: -1）
	 * @returns 播放的动画状态。
	 * @example
	 * <pre>
	 *     armature.animation.play("walk");
	 * </pre>
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun play(animationName: String? = null, playTimes: Int = -1): AnimationState? {
		val _animationConfig1 = this._animationConfig!!
		_animationConfig1.clear()
		_animationConfig1.resetToPose = true
		_animationConfig1.playTimes = playTimes
		_animationConfig1.fadeInTime = 0.0
		_animationConfig1.animation = animationName ?: ""

		if (animationName != null && animationName.isNotEmpty()) {
			this.playConfig(_animationConfig1)
		}
		else if (this._lastAnimationState == null) {
			val defaultAnimation = this._armature?.armatureData?.defaultAnimation
			if (defaultAnimation != null) {
				_animationConfig1.animation = defaultAnimation.name
				this.playConfig(_animationConfig1)
			}
		}
		else if (!this._lastAnimationState!!.isPlaying && !this._lastAnimationState!!.isCompleted) {
			this._lastAnimationState?.play()
		}
		else {
			_animationConfig1.animation = this._lastAnimationState!!.name
			this.playConfig(_animationConfig1)
		}

		return this._lastAnimationState
	}
	/**
	 * - Fade in a specific animation.
	 * @param animationName - The name of animation data.
	 * @param fadeInTime - The fade in time. [-1: Use the default value of animation data, [0~N]: The fade in time (In seconds)] (Default: -1)
	 * @param playTimes - playing repeat times. [-1: Use the default value of animation data, 0: No end loop playing, [1~N]: Repeat N times] (Default: -1)
	 * @param layer - The blending layer, the animation states in high level layer will get the blending weights with high priority, when the total blending weights are more than 1.0, there will be no more weights can be allocated to the other animation states. (Default: 0)
	 * @param group - The blending group name, it is typically used to specify the substitution of multiple animation states blending. (Default: null)
	 * @param fadeOutMode - The fade out mode, which is typically used to specify alternate mode of multiple animation states blending. (Default: AnimationFadeOutMode.SameLayerAndGroup)
	 * @returns The playing animation state.
	 * @example
	 * <pre>
	 *     armature.animation.fadeIn("walk", 0.3, 0, 0, "normalGroup").resetToPose = false;
	 *     armature.animation.fadeIn("attack", 0.3, 1, 0, "attackGroup").resetToPose = false;
	 * </pre>
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 淡入播放指定的动画。
	 * @param animationName - 动画数据名称。
	 * @param fadeInTime - 淡入时间。 [-1: 使用动画数据默认值, [0~N]: 淡入时间 (以秒为单位)] （默认: -1）
	 * @param playTimes - 播放次数。 [-1: 使用动画数据默认值, 0: 无限循环播放, [1~N]: 循环播放 N 次] （默认: -1）
	 * @param layer - 混合图层，图层高的动画状态会优先获取混合权重，当混合权重分配总和超过 1.0 时，剩余的动画状态将不能再获得权重分配。 （默认: 0）
	 * @param group - 混合组名称，该属性通常用来指定多个动画状态混合时的相互替换关系。 （默认: null）
	 * @param fadeOutMode - 淡出模式，该属性通常用来指定多个动画状态混合时的相互替换模式。 （默认: AnimationFadeOutMode.SameLayerAndGroup）
	 * @returns 播放的动画状态。
	 * @example
	 * <pre>
	 *     armature.animation.fadeIn("walk", 0.3, 0, 0, "normalGroup").resetToPose = false;
	 *     armature.animation.fadeIn("attack", 0.3, 1, 0, "attackGroup").resetToPose = false;
	 * </pre>
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun fadeIn(
		animationName: String, fadeInTime: Double = -1.0, playTimes: Int = -1,
		layer: Int = 0, group: String? = null, fadeOutMode: AnimationFadeOutMode = AnimationFadeOutMode.SameLayerAndGroup
	): AnimationState? {
		val _animationConfig1 = this._animationConfig!!
		_animationConfig1.clear()
		_animationConfig1.fadeOutMode = fadeOutMode
		_animationConfig1.playTimes = playTimes
		_animationConfig1.layer = layer
		_animationConfig1.fadeInTime = fadeInTime
		_animationConfig1.animation = animationName
		_animationConfig1.group = group ?: ""

		return this.playConfig(_animationConfig1)
	}
	/**
	 * - Play a specific animation from the specific time.
	 * @param animationName - The name of animation data.
	 * @param time - The start time point of playing. (In seconds)
	 * @param playTimes - Playing repeat times. [-1: Use the default value of animation data, 0: No end loop playing, [1~N]: Repeat N times] (Default: -1)
	 * @returns The played animation state.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 从指定时间开始播放指定的动画。
	 * @param animationName - 动画数据名称。
	 * @param time - 播放开始的时间。 (以秒为单位)
	 * @param playTimes - 循环播放次数。 [-1: 使用动画数据默认值, 0: 无限循环播放, [1~N]: 循环播放 N 次] （默认: -1）
	 * @returns 播放的动画状态。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun gotoAndPlayByTime(animationName: String, time: Double = 0.0, playTimes: Int = -1): AnimationState? {
		val _animationConfig1 = this._animationConfig!!
		_animationConfig1.clear()
		_animationConfig1.resetToPose = true
		_animationConfig1.playTimes = playTimes
		_animationConfig1.position = time
		_animationConfig1.fadeInTime = 0.0
		_animationConfig1.animation = animationName

		return this.playConfig(_animationConfig1)
	}
	/**
	 * - Play a specific animation from the specific frame.
	 * @param animationName - The name of animation data.
	 * @param frame - The start frame of playing.
	 * @param playTimes - Playing repeat times. [-1: Use the default value of animation data, 0: No end loop playing, [1~N]: Repeat N times] (Default: -1)
	 * @returns The played animation state.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 从指定帧开始播放指定的动画。
	 * @param animationName - 动画数据名称。
	 * @param frame - 播放开始的帧数。
	 * @param playTimes - 播放次数。 [-1: 使用动画数据默认值, 0: 无限循环播放, [1~N]: 循环播放 N 次] （默认: -1）
	 * @returns 播放的动画状态。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun gotoAndPlayByFrame(animationName: String, frame: Int = 0, playTimes: Int = -1): AnimationState? {
		val _animationConfig1 = this._animationConfig!!
		_animationConfig1.clear()
		_animationConfig1.resetToPose = true
		_animationConfig1.playTimes = playTimes
		_animationConfig1.fadeInTime = 0.0
		_animationConfig1.animation = animationName

		val animationData = if (animationName in this._animations) this._animations[animationName] else null
		if (animationData != null) {
			_animationConfig1.position = if (animationData.frameCount > 0) animationData.duration * frame / animationData.frameCount else 0.0
		}

		return this.playConfig(_animationConfig1!!)
	}
	/**
	 * - Play a specific animation from the specific progress.
	 * @param animationName - The name of animation data.
	 * @param progress - The start progress value of playing.
	 * @param playTimes - Playing repeat times. [-1: Use the default value of animation data, 0: No end loop playing, [1~N]: Repeat N times] (Default: -1)
	 * @returns The played animation state.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 从指定进度开始播放指定的动画。
	 * @param animationName - 动画数据名称。
	 * @param progress - 开始播放的进度。
	 * @param playTimes - 播放次数。 [-1: 使用动画数据默认值, 0: 无限循环播放, [1~N]: 循环播放 N 次] （默认: -1）
	 * @returns 播放的动画状态。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun gotoAndPlayByProgress(animationName: String, progress: Double = 0.0, playTimes: Int = -1): AnimationState? {
		val _animationConfig1 = this._animationConfig!!
		_animationConfig1.clear()
		_animationConfig1.resetToPose = true
		_animationConfig1.playTimes = playTimes
		_animationConfig1.fadeInTime = 0.0
		_animationConfig1.animation = animationName

		val animationData = if (animationName in this._animations) this._animations[animationName] else null
		if (animationData != null) {
			_animationConfig1.position = animationData.duration * (if (progress > 0.0) progress else 0.0)
		}

		return this.playConfig(_animationConfig1!!)
	}
	/**
	 * - Stop a specific animation at the specific time.
	 * @param animationName - The name of animation data.
	 * @param time - The stop time. (In seconds)
	 * @returns The played animation state.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 在指定时间停止指定动画播放
	 * @param animationName - 动画数据名称。
	 * @param time - 停止的时间。 (以秒为单位)
	 * @returns 播放的动画状态。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun gotoAndStopByTime(animationName: String, time: Double = 0.0): AnimationState? {
		val animationState = this.gotoAndPlayByTime(animationName, time, 1)
		if (animationState != null) {
			animationState.stop()
		}

		return animationState
	}
	/**
	 * - Stop a specific animation at the specific frame.
	 * @param animationName - The name of animation data.
	 * @param frame - The stop frame.
	 * @returns The played animation state.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 在指定帧停止指定动画的播放
	 * @param animationName - 动画数据名称。
	 * @param frame - 停止的帧数。
	 * @returns 播放的动画状态。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun gotoAndStopByFrame(animationName: String, frame: Int = 0): AnimationState? {
		val animationState = this.gotoAndPlayByFrame(animationName, frame, 1)
		if (animationState != null) {
			animationState.stop()
		}

		return animationState
	}
	/**
	 * - Stop a specific animation at the specific progress.
	 * @param animationName - The name of animation data.
	 * @param progress - The stop progress value.
	 * @returns The played animation state.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 在指定的进度停止指定的动画播放。
	 * @param animationName - 动画数据名称。
	 * @param progress - 停止进度。
	 * @returns 播放的动画状态。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun gotoAndStopByProgress(animationName: String, progress: Double = 0.0): AnimationState? {
		val animationState = this.gotoAndPlayByProgress(animationName, progress, 1)
		animationState?.stop()
		return animationState
	}

	/**
	 * @internal
	 */
	fun getBlendState(type: String, name: String, target: TransformObject): BlendState {
		if (type !in this._blendStates) this._blendStates[type] = FastStringMap()

		val blendStates = this._blendStates[type]!!
		if (name !in blendStates) {
			val res = pool.blendState.borrow()
			val blendState = res
			blendStates[name] = res
            blendState.targetTransformObject = target
            if (target is Slot) blendState.targetSlot = target
            if (target is Surface) blendState.targetSurface = target
            if (target is Bone) blendState.targetBone = target
		}

		return blendStates[name]!!
	}
	/**
	 * - Get a specific animation state.
	 * @param animationName - The name of animation state.
	 * @param layer - The layer of find animation states. [-1: Find all layers, [0~N]: Specified layer] (default: -1)
	 * @example
	 * <pre>
	 *     armature.animation.play("walk");
	 *     var walkState = armature.animation.getState("walk");
	 *     walkState.timeScale = 0.5;
	 * </pre>
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取指定的动画状态。
	 * @param animationName - 动画状态名称。
	 * @param layer - 查找动画状态的层级。 [-1: 查找所有层级, [0~N]: 指定层级] （默认: -1）
	 * @example
	 * <pre>
	 *     armature.animation.play("walk");
	 *     var walkState = armature.animation.getState("walk");
	 *     walkState.timeScale = 0.5;
	 * </pre>
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getState(animationName: String, layer: Int = -1): AnimationState? {
		for (i in this._animationStates.size - 1 downTo 0) {
			val animationState = this._animationStates[i]
			if (animationState.name == animationName && (layer < 0 || animationState.layer == layer)) {
				return animationState
			}
		}

		return null
	}
	/**
	 * - Check whether a specific animation data is included.
	 * @param animationName - The name of animation data.
	 * @see dragonBones.AnimationData
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 检查是否包含指定的动画数据
	 * @param animationName - 动画数据名称。
	 * @see dragonBones.AnimationData
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun hasAnimation(animationName: String): Boolean = animationName in this._animations
	/**
	 * - Get all the animation states.
	 * @version DragonBones 5.1
	 * @language en_US
	 */
	/**
	 * - 获取所有的动画状态
	 * @version DragonBones 5.1
	 * @language zh_CN
	 */
	fun getStates(): List<AnimationState> = this._animationStates
	/**
	 * - Check whether there is an animation state is playing
	 * @see dragonBones.AnimationState
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 检查是否有动画状态正在播放
	 * @see dragonBones.AnimationState
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val isPlaying: Boolean get() {
		this._animationStates.fastForEach { animationState ->
			if (animationState.isPlaying) {
				return true
			}
		}

		return false
	}
	/**
	 * - Check whether all the animation states' playing were finished.
	 * @see dragonBones.AnimationState
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 检查是否所有的动画状态均已播放完毕。
	 * @see dragonBones.AnimationState
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val isCompleted: Boolean get() {
		this._animationStates.fastForEach { animationState ->
			if (!animationState.isCompleted) {
				return false
			}
		}

		return this._animationStates.length > 0
	}
	/**
	 * - The name of the last playing animation state.
	 * @see #lastAnimationState
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 上一个播放的动画状态名称
	 * @see #lastAnimationState
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val lastAnimationName: String get() {
		return this._lastAnimationState?.name ?: ""
	}
	/**
	 * - The name of all animation data
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 所有动画数据的名称
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	val animationNames: List<String> get() {
		return this._animationNames
	}
	/**
	 * - All animation data.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 所有的动画数据。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	var animations: FastStringMap<AnimationData>
		get() {
			return this._animations
		}
		set(value) {
			if (this._animations == value) {
				return
			}

			this._animationNames.length = 0

			this._animations.clear()

			value.fastKeyForEach { k ->
				this._animationNames.add(k)
				this._animations[k] = value[k]!!
			}
		}
	/**
	 * - An AnimationConfig instance that can be used quickly.
	 * @see dragonBones.AnimationConfig
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 一个可以快速使用的动画配置实例。
	 * @see dragonBones.AnimationConfig
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	val animationConfig: AnimationConfig get() {
		this._animationConfig?.clear()
		return this._animationConfig!!
	}
	/**
	 * - The last playing animation state
	 * @see dragonBones.AnimationState
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 上一个播放的动画状态
	 * @see dragonBones.AnimationState
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val lastAnimationState: AnimationState
		get() {
		return this._lastAnimationState!!
	}
}
