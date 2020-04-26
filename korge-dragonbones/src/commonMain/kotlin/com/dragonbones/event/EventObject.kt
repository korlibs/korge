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
package com.dragonbones.event

import com.dragonbones.animation.*
import com.dragonbones.armature.*
import com.dragonbones.core.*
import com.dragonbones.model.*

/**
 * - The properties of the object carry basic information about an event,
 * which are passed as parameter or parameter's parameter to event listeners when an event occurs.
 * @version DragonBones 4.5
 * @language en_US
 */
@Suppress("KDocUnresolvedReference")
/**
 * - 事件对象，包含有关事件的基本信息，当发生事件时，该实例将作为参数或参数的参数传递给事件侦听器。
 * @version DragonBones 4.5
 * @language zh_CN
 */
class EventObject(pool: SingleObjectPool<EventObject>) : BaseObject(pool) {
	companion object {
		/**
		 * - Animation start play.
		 * @version DragonBones 4.5
		 * @language en_US
		 */
		/**
		 * - 动画开始播放。
		 * @version DragonBones 4.5
		 * @language zh_CN
		 */
		val START: String = "start"
		/**
		 * - Animation loop play complete once.
		 * @version DragonBones 4.5
		 * @language en_US
		 */
		/**
		 * - 动画循环播放完成一次。
		 * @version DragonBones 4.5
		 * @language zh_CN
		 */
		val LOOP_COMPLETE: String = "loopComplete"
		/**
		 * - Animation play complete.
		 * @version DragonBones 4.5
		 * @language en_US
		 */
		/**
		 * - 动画播放完成。
		 * @version DragonBones 4.5
		 * @language zh_CN
		 */
		val COMPLETE: String = "complete"
		/**
		 * - Animation fade in start.
		 * @version DragonBones 4.5
		 * @language en_US
		 */
		/**
		 * - 动画淡入开始。
		 * @version DragonBones 4.5
		 * @language zh_CN
		 */
		val FADE_IN: String = "fadeIn"
		/**
		 * - Animation fade in complete.
		 * @version DragonBones 4.5
		 * @language en_US
		 */
		/**
		 * - 动画淡入完成。
		 * @version DragonBones 4.5
		 * @language zh_CN
		 */
		val FADE_IN_COMPLETE: String = "fadeInComplete"
		/**
		 * - Animation fade out start.
		 * @version DragonBones 4.5
		 * @language en_US
		 */
		/**
		 * - 动画淡出开始。
		 * @version DragonBones 4.5
		 * @language zh_CN
		 */
		val FADE_OUT: String = "fadeOut"
		/**
		 * - Animation fade out complete.
		 * @version DragonBones 4.5
		 * @language en_US
		 */
		/**
		 * - 动画淡出完成。
		 * @version DragonBones 4.5
		 * @language zh_CN
		 */
		val FADE_OUT_COMPLETE: String = "fadeOutComplete"
		/**
		 * - Animation frame event.
		 * @version DragonBones 4.5
		 * @language en_US
		 */
		/**
		 * - 动画帧事件。
		 * @version DragonBones 4.5
		 * @language zh_CN
		 */
		val FRAME_EVENT: String = "frameEvent"
		/**
		 * - Animation frame sound event.
		 * @version DragonBones 4.5
		 * @language en_US
		 */
		/**
		 * - 动画帧声音事件。
		 * @version DragonBones 4.5
		 * @language zh_CN
		 */
		val SOUND_EVENT: String = "soundEvent"

		/**
		 * @internal
		 * @private
		 */
		fun actionDataToInstance(data: ActionData, instance: EventObject, armature: Armature) {
			if (data.type == ActionType.Play) {
				instance.type = EventObject.FRAME_EVENT
			} else {
				instance.type = if (data.type == ActionType.Frame) EventObject.FRAME_EVENT else EventObject.SOUND_EVENT
			}

			instance.name = data.name
			instance.armature = armature
			instance.actionData = data
			instance.data = data.data

			if (data.bone != null) {
				instance.bone = armature.getBone(data.bone?.name)
			}

			if (data.slot != null) {
				instance.slot = armature.getSlot(data.slot?.name)
			}
		}
	}

	override fun toString(): String = "[class dragonBones.EventObject]"
	/**
	 * - If is a frame event, the value is used to describe the time that the event was in the animation timeline. (In seconds)
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 如果是帧事件，此值用来描述该事件在动画时间轴中所处的时间。（以秒为单位）
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	var time: Double = 0.0
	/**
	 * - The event type。
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 事件类型。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	var type: EventStringType = ""
	/**
	 * - The event name. (The frame event name or the frame sound name)
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 事件名称。 (帧事件的名称或帧声音的名称)
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	var name: String = ""
	/**
	 * - The armature that dispatch the event.
	 * @see dragonBones.Armature
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 发出该事件的骨架。
	 * @see dragonBones.Armature
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	lateinit var armature: Armature
	/**
	 * - The bone that dispatch the event.
	 * @see dragonBones.Bone
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 发出该事件的骨骼。
	 * @see dragonBones.Bone
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	var bone: Bone? = null
	/**
	 * - The slot that dispatch the event.
	 * @see dragonBones.Slot
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 发出该事件的插槽。
	 * @see dragonBones.Slot
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	var slot: Slot? = null
	/**
	 * - The animation state that dispatch the event.
	 * @see dragonBones.AnimationState
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 发出该事件的动画状态。
	 * @see dragonBones.AnimationState
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	lateinit var animationState: AnimationState
	/**
	 * @private
	 */
	var actionData: ActionData? = null
	/**
	 * @private
	 */
	/**
	 * - The custom data.
	 * @see dragonBones.CustomData
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 自定义数据。
	 * @see dragonBones.CustomData
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	var data: UserData? = null

	override fun _onClear() {
		this.time = 0.0
		this.type = ""
		this.name = ""
		//this.armature = null
		this.bone = null
		this.slot = null
		//this.animationState = null
		this.actionData = null
		this.data = null
	}
}
