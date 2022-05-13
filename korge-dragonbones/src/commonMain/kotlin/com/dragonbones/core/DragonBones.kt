package com.dragonbones.core

import com.dragonbones.animation.*
import com.dragonbones.event.*
import com.dragonbones.util.*

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

interface IntEnum {
	val id: Int
}

// @TODO: Associate by index
open class IntEnumCompanion<T : IntEnum>(private val rawValues: Array<T>) {
	private val values = rawValues.associateBy { it.id }
	operator fun get(index: Int): T = values[index] ?: rawValues.first()
}

/**
 * @private
 */
object BinaryOffset {
	const val WeigthBoneCount = 0
	const val WeigthFloatOffset = 1
	const val WeigthBoneIndices = 2

	const val GeometryVertexCount = 0
	const val GeometryTriangleCount = 1
	const val GeometryFloatOffset = 2
	const val GeometryWeightOffset = 3
	const val GeometryVertexIndices = 4

	const val TimelineScale = 0
	const val TimelineOffset = 1
	const val TimelineKeyFrameCount = 2
	const val TimelineFrameValueCount = 3
	const val TimelineFrameValueOffset = 4
	const val TimelineFrameOffset = 5

	const val FramePosition = 0
	const val FrameTweenType = 1
	const val FrameTweenEasingOrCurveSampleCount = 2
	const val FrameCurveSamples = 3

	const val DeformVertexOffset = 0
	const val DeformCount = 1
	const val DeformValueCount = 2
	const val DeformValueOffset = 3
	const val DeformFloatOffset = 4
}
/**
 * @private
 */
enum class ArmatureType(override val id: Int) : IntEnum {
	Armature(0),
	MovieClip(1),
	Stage(2);

	companion object : IntEnumCompanion<ArmatureType>(values())
}
/**
 * @private
 */
enum class DisplayType(override val id: Int) : IntEnum {
	None(-1),
	Image(0),
	Armature(1),
	Mesh(2),
	BoundingBox(3),
	Path(4);

	companion object : IntEnumCompanion<DisplayType>(values())
}
/**
 * - Bounding box type.
 * @version DragonBones 5.0
 * @language en_US
 */
/**
 * - 边界框类型。
 * @version DragonBones 5.0
 * @language zh_CN
 */
enum class BoundingBoxType(override val id: Int) : IntEnum {
	None(-1),
	Rectangle(0),
	Ellipse(1),
	Polygon(2);

	companion object : IntEnumCompanion<BoundingBoxType>(values())
}
/**
 * @private
 */
enum class ActionType(override val id: Int) : IntEnum {
	Play(0),
	Frame(10),
	Sound(11);

	companion object : IntEnumCompanion<ActionType>(values())
}
/**
 * @private
 */
enum class BlendMode(override val id: Int) : IntEnum {
	Normal(0),
	Add(1),
	Alpha(2),
	Darken(3),
	Difference(4),
	Erase(5),
	HardLight(6),
	Invert(7),
	Layer(8),
	Lighten(9),
	Multiply(10),
	Overlay(11),
	Screen(12),
	Subtract(13);

	companion object : IntEnumCompanion<BlendMode>(values())
}
/**
 * @private
 */
enum class TweenType(override val id: Int) : IntEnum {
	None(0),
	Line(1),
	Curve(2),
	QuadIn(3),
	QuadOut(4),
	QuadInOut(5);

	companion object : IntEnumCompanion<TweenType>(values())
}
/**
 * @private
 */
enum class TimelineType(override val id: Int) : IntEnum {
	Action(0),
	ZOrder(1),

	BoneAll(10),
	BoneTranslate(11),
	BoneRotate(12),
	BoneScale(13),

	Surface(50),
	BoneAlpha(60),

	SlotDisplay(20),
	SlotColor(21),
	SlotDeform(22),
	SlotZIndex(23),
	SlotAlpha(24),

	IKConstraint(30),

	AnimationProgress(40),
	AnimationWeight(41),
	AnimationParameter(42);

	companion object : IntEnumCompanion<TimelineType>(values())
}
/**
 * - Offset mode.
 * @version DragonBones 5.5
 * @language en_US
 */
/**
 * - 偏移模式。
 * @version DragonBones 5.5
 * @language zh_CN
 */
enum class OffsetMode {
	None,
	Additive,
	Override,
}
/**
 * - Animation fade out mode.
 * @version DragonBones 4.5
 * @language en_US
 */
/**
 * - 动画淡出模式。
 * @version DragonBones 4.5
 * @language zh_CN
 */
enum class AnimationFadeOutMode(override val id: Int) : IntEnum {
	/**
	 * - Fade out the animation states of the same layer.
	 * @language en_US
	 */
	/**
	 * - 淡出同层的动画状态。
	 * @language zh_CN
	 */
	SameLayer(1),
	/**
	 * - Fade out the animation states of the same group.
	 * @language en_US
	 */
	/**
	 * - 淡出同组的动画状态。
	 * @language zh_CN
	 */
	SameGroup(2),
	/**
	 * - Fade out the animation states of the same layer and group.
	 * @language en_US
	 */
	/**
	 * - 淡出同层并且同组的动画状态。
	 * @language zh_CN
	 */
	SameLayerAndGroup(3),
	/**
	 * - Fade out of all animation states.
	 * @language en_US
	 */
	/**
	 * - 淡出所有的动画状态。
	 * @language zh_CN
	 */
	All(4),
	/**
	 * - Does not replace the animation state with the same name.
	 * @language en_US
	 */
	/**
	 * - 不替换同名的动画状态。
	 * @language zh_CN
	 */
	Single(5);

	companion object : IntEnumCompanion<AnimationFadeOutMode>(values())
}
/**
 * @private
 */
enum class AnimationBlendType {
	None,
	E1D,
}
/**
 * @private
 */
enum class AnimationBlendMode {
	Additive,
	Override,
}
/**
 * @private
 */
enum class ConstraintType {
	IK,
	Path
}
/**
 * @private
 */
enum class PositionMode {
	Fixed,
	Percent
}
/**
 * @private
 */
enum class SpacingMode {
	Length,
	Fixed,
	Percent
}
/**
 * @private
 */
enum class RotateMode {
	Tangent,
	Chain,
	ChainScale
}
/**
 * @private
 */
//interface Map<T> {
//	[key: String]: T
//}
/**
 * @private
 */
class DragonBones(eventManager: IEventDispatcher) {
	companion object {
		const val VERSION: String = "5.7.000"

		val yDown: Boolean = true
		val debug: Boolean = false
		val debugDraw: Boolean = false
	}

	private val _clock: WorldClock = WorldClock()
	//private val _events: FastArrayList<EventObject> = FastArrayList()
	//private val _objects: FastArrayList<BaseObject> = FastArrayList()
	private var _eventManager: IEventDispatcher = eventManager

	//init {
	//	println("DragonBones: ${DragonBones.VERSION}\nWebsite: http://dragonbones.com/\nSource and Demo: https://github.com/DragonBones/")
	//}

	//fun advanceTime(passedTime: Double) {
	//	if (this._objects.size > 0) {
	//		for (obj in this._objects) {
	//			obj.returnToPool()
	//		}
//
	//		this._objects.clear()
	//	}
//
	//	this._clock.advanceTime(passedTime)
//
	//	flushEvents()
	//}

	//fun flushEvents() {
	//	if (this._events.size > 0) {
	//		for (i in 0 until this._events.size) {
	//			val eventObject = this._events[i]
	//			val armature = eventObject.armature
//
	//			if (armature._armatureData != null) { // May be armature disposed before advanceTime.
	//				armature.eventDispatcher.dispatchDBEvent(eventObject.type, eventObject)
	//				if (eventObject.type == EventObject.SOUND_EVENT) {
	//					this._eventManager.dispatchDBEvent(eventObject.type, eventObject)
	//				}
	//			}
//
	//			this.bufferObject(eventObject)
	//		}
//
	//		this._events.lengthSet = 0
	//	}
	//}

	//fun bufferEvent(value: EventObject) {
	//	if (this._events.indexOf(value) < 0) {
	//		this._events.add(value)
	//	}
	//}

	//fun bufferObject(obj: BaseObject?) {
	//	if (this._objects.indexOf(obj) < 0) {
	//		if (obj != null) this._objects.add(obj)
	//	}
	//}

	val clock: WorldClock get() = this._clock

	val eventManager: IEventDispatcher get() = this._eventManager
}

/*
//
if (!console.warn) {
    console.warn = function () { };
}

if (!console.assert) {
    console.assert = function () { };
}
//
if (!Date.now) {
    Date.now = function now() {
        return new Date().getTime();
    };
}
// Weixin can not support typescript  : .
var __extends: Any = function (t: Any, e: Any) {
    function r(this: Any) {
        this.constructor = t;
    }
    for (var i in e) {
        if ((e as any).hasOwnProperty(i)) {
            t[i] = e[i];
        }
    }
    r.prototype = e.prototype, t.prototype = new (r as any)();
};
//
if (typeof global == "undefined" && typeof window != "undefined") {
    var global = window as any;
}
//
declare var exports: Any;
declare var module: Any;
declare var define: Any;
if (typeof exports == "object" && typeof module == "object") {
    module.exports = dragonBones;
}
else if (typeof define == "function" && define["amd"]) {
    define(["dragonBones"], function () { return dragonBones; });
}
else if (typeof exports == "object") {
    exports = dragonBones;
}
else if (typeof global != "undefined") {
    global.dragonBones = dragonBones;
}
*/
