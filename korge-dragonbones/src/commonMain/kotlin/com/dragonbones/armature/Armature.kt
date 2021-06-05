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

package com.dragonbones.armature

import com.dragonbones.animation.*
import com.dragonbones.core.*
import com.dragonbones.event.*
import com.soywiz.kds.iterators.*
import com.dragonbones.model.*
import com.dragonbones.util.*
import com.dragonbones.util.length
import com.soywiz.kds.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.*

/**
 * - Armature is the core of the skeleton animation system.
 * @see dragonBones.ArmatureData
 * @see dragonBones.Bone
 * @see dragonBones.Slot
 * @see dragonBones.Animation
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 骨架是骨骼动画系统的核心。
 * @see dragonBones.ArmatureData
 * @see dragonBones.Bone
 * @see dragonBones.Slot
 * @see dragonBones.Animation
 * @version DragonBones 3.0
 * @language zh_CN
 */
class Armature(pool: SingleObjectPool<Armature>) : BaseObject(pool), IAnimatable {
	override fun toString(): String {
		return "[class dragonBones.Armature]"
	}
	companion object {
		fun _onSortSlots(a: Slot, b: Slot): Int {
			// @TODO: Circumvents kotlin-native bug
			val aa = a._zIndex * 1000 + a._zOrder
			val bb = b._zIndex * 1000 + b._zOrder
			return aa.compareTo(bb)
			//return if (a._zIndex * 1000 + a._zOrder > b._zIndex * 1000 + b._zOrder) 1 else -1

		}
	}
	/**
	 * - Whether to inherit the animation control of the parent armature.
	 * True to try to have the child armature play an animation with the same name when the parent armature play the animation.
	 * @default true
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 是否继承父骨架的动画控制。
	 * 如果该值为 true，当父骨架播放动画时，会尝试让子骨架播放同名动画。
	 * @default true
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	var inheritAnimation: Boolean = true
	/**
	 * @private
	 */
	var userData: Any? = null
	/**
	 * @internal
	 */
	var _lockUpdate: Boolean = false
	private var _slotsDirty: Boolean = true
	private var _zOrderDirty: Boolean = false
	/**
	 * @internal
	 */
	var _zIndexDirty: Boolean = false
	/**
	 * @internal
	 */
	var _alphaDirty: Boolean = true
	private var _flipX: Boolean = false
	private var _flipY: Boolean = false
	/**
	 * @internal
	 */
	var _cacheFrameIndex: Int = -1
	private var _alpha: Double = 1.0
	/**
	 * @internal
	 */
	var _globalAlpha: Double = 1.0
	private val _bones: FastArrayList<Bone> = FastArrayList()
	private val _slots: FastArrayList<Slot> = FastArrayList()
	/**
	 * @internal
	 */
	val _constraints: FastArrayList<Constraint> = FastArrayList()
	private val _actions: FastArrayList<EventObject> = FastArrayList()
	/**
	 * @internal
	 */
	var _armatureData: ArmatureData? = null
	private var _animation: Animation? = null // Initial value.
	private var _proxy: IArmatureProxy? = null// Initial value.
	private var _display: Any? = null
	/**
	 * @internal
	 */
	var _replaceTextureAtlasData: TextureAtlasData? = null // Initial value.
	private var _replacedTexture: Any? = null
	/**
	 * @internal
	 */
	var _dragonBones: DragonBones? = null
	private var _clock: WorldClock? = null // Initial value.
	/**
	 * @internal
	 */
	var _parent: Slot? = null

	override fun _onClear() {
		this._clock?.remove(this)

		this._bones.fastForEach { bone ->
			bone.returnToPool()
		}
		this._slots.fastForEach { slot ->
			slot.returnToPool()
		}
		this._constraints.fastForEach { constraint ->
			constraint.returnToPool()
		}
		this._actions.fastForEach { action ->
			action.returnToPool()
		}

		this._animation?.returnToPool()
		this._proxy?.dbClear()
		this._replaceTextureAtlasData?.returnToPool()

		this.inheritAnimation = true
		this.userData = null

		this._lockUpdate = false
		this._slotsDirty = true
		this._zOrderDirty = false
		this._zIndexDirty = false
		this._alphaDirty = true
		this._flipX = false
		this._flipY = false
		this._cacheFrameIndex = -1
		this._alpha = 1.0
		this._globalAlpha = 1.0
		this._bones.lengthSet = 0
		this._slots.lengthSet = 0
		this._constraints.lengthSet = 0
		this._actions.lengthSet = 0
		this._armatureData = null  //
		this._animation = null  //
		this._proxy = null  //
		this._display = null
		this._replaceTextureAtlasData = null
		this._replacedTexture = null
		this._dragonBones = null  //
		this._clock = null
		this._parent = null
	}

	/**
	 * @internal
	 */
	fun _sortZOrder(slotIndices: Int16Buffer?, offset: Int) {
		val slotDatas = this._armatureData!!.sortedSlots

		if (this._zOrderDirty || slotIndices != null) {
			val l = slotDatas.lengthSet
			for (i in 0 until l) {
				val slotIndex: Int = if (slotIndices == null) i else slotIndices[offset + i].toInt()
				if (slotIndex < 0 || slotIndex >= l) {
					continue
				}

				val slotData = slotDatas[slotIndex]
				val slot = this.getSlot(slotData.name)

				if (slot != null) {
					slot._setZOrder(i)
				}
			}

			this._slotsDirty = true
			this._zOrderDirty = slotIndices != null
		}
	}

	/**
	 * @internal
	 */
	fun _addBone(value: Bone) {
		if (this._bones.indexOf(value) < 0) {
			this._bones.add(value)
		}
	}

	/**
	 * @internal
	 */
	fun _addSlot(value: Slot) {
		if (this._slots.indexOf(value) < 0) {
			this._slots.add(value)
		}
	}

	/**
	 * @internal
	 */
	fun _addConstraint(value: Constraint) {
		if (this._constraints.indexOf(value) < 0) {
			this._constraints.add(value)
		}
	}

	/**
	 * @internal
	 */
	fun _bufferAction(action: EventObject, append: Boolean) {
		if (this._actions.indexOf(action) < 0) {
			if (append) {
                this._actions.add(action)
			}
			else {
				this._actions.add(0, action)
			}
		}
	}
	/**
	 * - Dispose the armature. (Return to the object pool)
	 * @example
	 * <pre>
	 *     removeChild(armature.display);
	 *     armature.dispose();
	 * </pre>
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 释放骨架。 （回收到对象池）
	 * @example
	 * <pre>
	 *     removeChild(armature.display);
	 *     armature.dispose();
	 * </pre>
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun dispose() {
		if (this._armatureData != null) {
			this._lockUpdate = true
			//this._dragonBones?.bufferObject(this)
			this.returnToPool()
		}
	}

	/**
	 * @internal
	 */
	fun init(
		armatureData: ArmatureData,
		proxy: IArmatureProxy, display: Any, dragonBones: DragonBones
	) {
		if (this._armatureData != null) {
			return
		}

		this._armatureData = armatureData
		this._animation = pool.animation.borrow()
		this._proxy = proxy
		this._display = display
		this._dragonBones = dragonBones

		this._proxy?.dbInit(this)
		this._animation?.init(this)
		this._animation?.animations = this._armatureData?.animations!!
	}

	override fun advanceTime(passedTime: Double) {
		_advanceTime(passedTime)
		_slots.fastForEach { slot ->
			slot.childArmature?.advanceTime(passedTime)
			//slot._armature?.advanceTime(passedTime)
		}
	}

	/**
	 * @inheritDoc
	 */
	private fun _advanceTime(passedTime: Double) {
		if (this._lockUpdate) {
			return
		}

		this._lockUpdate = true

		if (this._armatureData == null) {
			Console.warn("The armature has been disposed.")
			return
		}
		else if (this._armatureData?.parent == null) {
			Console.warn("The armature data has been disposed.\nPlease make sure dispose armature before call factory.clear().")
			return
		}

		val prevCacheFrameIndex = this._cacheFrameIndex
		// Update animation.
		this._animation?.advanceTime(passedTime)
		// Sort slots.
		if (this._slotsDirty || this._zIndexDirty) {
			this._slots.sortWith(Comparator(Armature.Companion::_onSortSlots))

			if (this._zIndexDirty) {
				for (i in 0 until this._slots.size) {
					this._slots[i]._setZOrder(i) //
				}
			}

			this._slotsDirty = false
			this._zIndexDirty = false
		}
		// Update alpha.
		if (this._alphaDirty) {
			this._alphaDirty = false
			this._globalAlpha = this._alpha * (this._parent?._globalAlpha ?: 1.0)

			for (n in 0 until _bones.size) {
				_bones[n]._updateAlpha()
			}

			for (n in 0 until _slots.size) {
				_slots[n]._updateAlpha()
			}
		}
		// Update bones and slots.
		if (this._cacheFrameIndex < 0 || this._cacheFrameIndex != prevCacheFrameIndex) {
			for (i in 0 until this._bones.length) {
				this._bones[i].update(this._cacheFrameIndex)
			}

			for (i in 0 until this._slots.length) {
				this._slots[i].update(this._cacheFrameIndex)
			}
		}
		// Do actions.
		if (this._actions.lengthSet > 0) {
			this._actions.fastForEach { action ->
				val actionData = action.actionData
				if (actionData != null) {
					if (actionData.type == ActionType.Play) {
						when {
							action.slot != null -> {
								val childArmature = action.slot?.childArmature
								childArmature?.animation?.fadeIn(actionData.name)
							}
							action.bone != null -> {
								for (n in 0 until this._slots.size) {
									val slot = this._slots[n]
									if (slot.parent == action.bone) {
										val childArmature = slot.childArmature
										childArmature?.animation?.fadeIn(actionData.name)
									}
								}
							}
							else -> {
								this._animation?.fadeIn(actionData.name)
							}
						}
					}
				}

				action.returnToPool()
			}

			this._actions.lengthSet = 0
		}

		this._lockUpdate = false
		this._proxy?.dbUpdate()
	}
	/**
	 * - Forces a specific bone or its owning slot to update the transform or display property in the next frame.
	 * @param boneName - The bone name. (If not set, all bones will be update)
	 * @param updateSlot - Whether to update the bone's slots. (Default: false)
	 * @see dragonBones.Bone#invalidUpdate()
	 * @see dragonBones.Slot#invalidUpdate()
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 强制特定骨骼或其拥有的插槽在下一帧更新变换或显示属性。
	 * @param boneName - 骨骼名称。 （如果未设置，将更新所有骨骼）
	 * @param updateSlot - 是否更新骨骼的插槽。 （默认: false）
	 * @see dragonBones.Bone#invalidUpdate()
	 * @see dragonBones.Slot#invalidUpdate()
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun invalidUpdate(boneName: String? = null, updateSlot: Boolean = false) {
		if (boneName != null && boneName.length > 0) {
			val bone = this.getBone(boneName)
			if (bone != null) {
				bone.invalidUpdate()

				if (updateSlot) {
					this._slots.fastForEach { slot ->
						if (slot.parent == bone) {
							slot.invalidUpdate()
						}
					}
				}
			}
		}
		else {
			this._bones.fastForEach { bone ->
				bone.invalidUpdate()
			}

			if (updateSlot) {
				this._slots.fastForEach { slot ->
					slot.invalidUpdate()
				}
			}
		}
	}
	/**
	 * - Check whether a specific point is inside a custom bounding box in a slot.
	 * The coordinate system of the point is the inner coordinate system of the armature.
	 * Custom bounding boxes need to be customized in Dragonbones Pro.
	 * @param x - The horizontal coordinate of the point.
	 * @param y - The vertical coordinate of the point.
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 检查特定点是否在某个插槽的自定义边界框内。
	 * 点的坐标系为骨架内坐标系。
	 * 自定义边界框需要在 DragonBones Pro 中自定义。
	 * @param x - 点的水平坐标。
	 * @param y - 点的垂直坐标。
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	fun containsPoint(x: Double, y: Double): Slot? {
		this._slots.fastForEach { slot ->
			if (slot.containsPoint(x, y)) {
				return slot
			}
		}

		return null
	}
	/**
	 * - Check whether a specific segment intersects a custom bounding box for a slot in the armature.
	 * The coordinate system of the segment and intersection is the inner coordinate system of the armature.
	 * Custom bounding boxes need to be customized in Dragonbones Pro.
	 * @param xA - The horizontal coordinate of the beginning of the segment.
	 * @param yA - The vertical coordinate of the beginning of the segment.
	 * @param xB - The horizontal coordinate of the end point of the segment.
	 * @param yB - The vertical coordinate of the end point of the segment.
	 * @param intersectionPointA - The first intersection at which a line segment intersects the bounding box from the beginning to the end. (If not set, the intersection point will not calculated)
	 * @param intersectionPointB - The first intersection at which a line segment intersects the bounding box from the end to the beginning. (If not set, the intersection point will not calculated)
	 * @param normalRadians - The normal radians of the tangent of the intersection boundary box. [x: Normal radian of the first intersection tangent, y: Normal radian of the second intersection tangent] (If not set, the normal will not calculated)
	 * @returns The slot of the first custom bounding box where the segment intersects from the start point to the end point.
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 检查特定线段是否与骨架的某个插槽的自定义边界框相交。
	 * 线段和交点的坐标系均为骨架内坐标系。
	 * 自定义边界框需要在 DragonBones Pro 中自定义。
	 * @param xA - 线段起点的水平坐标。
	 * @param yA - 线段起点的垂直坐标。
	 * @param xB - 线段终点的水平坐标。
	 * @param yB - 线段终点的垂直坐标。
	 * @param intersectionPointA - 线段从起点到终点与边界框相交的第一个交点。 （如果未设置，则不计算交点）
	 * @param intersectionPointB - 线段从终点到起点与边界框相交的第一个交点。 （如果未设置，则不计算交点）
	 * @param normalRadians - 交点边界框切线的法线弧度。 [x: 第一个交点切线的法线弧度, y: 第二个交点切线的法线弧度] （如果未设置，则不计算法线）
	 * @returns 线段从起点到终点相交的第一个自定义边界框的插槽。
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	fun intersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		intersectionPointA: Point? = null,
		intersectionPointB: Point? = null,
		normalRadians: Point? = null
	): Slot? {
		val isV = xA == xB
		var dMin = 0.0
		var dMax = 0.0
		var intXA = 0.0
		var intYA = 0.0
		var intXB = 0.0
		var intYB = 0.0
		var intAN = 0.0
		var intBN = 0.0
		var intSlotA: Slot? = null
		var intSlotB: Slot? = null

		for (n in 0 until this._slots.size) {
			val slot = this._slots[n]
			val intersectionCount = slot.intersectsSegment(xA, yA, xB, yB, intersectionPointA, intersectionPointB, normalRadians)
			if (intersectionCount > 0) {
				if (intersectionPointA != null || intersectionPointB != null) {
					if (intersectionPointA != null) {
						var d = if (isV) intersectionPointA.yf - yA else intersectionPointA.xf - xA
						if (d < 0.0) {
							d = -d
						}

						if (intSlotA == null || d < dMin) {
							dMin = d
							intXA = intersectionPointA.xf.toDouble()
							intYA = intersectionPointA.yf.toDouble()
							intSlotA = slot

							if (normalRadians != null) {
								intAN = normalRadians.xf.toDouble()
							}
						}
					}

					if (intersectionPointB != null) {
						var d = intersectionPointB.xf - xA
						if (d < 0.0) {
							d = -d
						}

						if (intSlotB == null || d > dMax) {
							dMax = d
							intXB = intersectionPointB.xf.toDouble()
							intYB = intersectionPointB.yf.toDouble()
							intSlotB = slot

							if (normalRadians != null) {
								intBN = normalRadians.yf.toDouble()
							}
						}
					}
				}
				else {
					intSlotA = slot
					break
				}
			}
		}

		if (intSlotA != null && intersectionPointA != null) {
			intersectionPointA.xf = intXA.toFloat()
			intersectionPointA.yf = intYA.toFloat()

			if (normalRadians != null) {
				normalRadians.xf = intAN.toFloat()
			}
		}

		if (intSlotB != null && intersectionPointB != null) {
			intersectionPointB.xf = intXB.toFloat()
			intersectionPointB.yf = intYB.toFloat()

			if (normalRadians != null) {
				normalRadians.yf = intBN.toFloat()
			}
		}

		return intSlotA
	}
	/**
	 * - Get a specific bone.
	 * @param name - The bone name.
	 * @see dragonBones.Bone
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定的骨骼。
	 * @param name - 骨骼名称。
	 * @see dragonBones.Bone
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getBone(name: String?): Bone? {
		this._bones.fastForEach { bone ->
			if (bone.name == name) {
				return bone
			}
		}

		return null
	}
	/**
	 * - Get a specific bone by the display.
	 * @param display - The display object.
	 * @see dragonBones.Bone
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 通过显示对象获取特定的骨骼。
	 * @param display - 显示对象。
	 * @see dragonBones.Bone
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getBoneByDisplay(display: Any): Bone? {
		val slot = this.getSlotByDisplay(display)
		return if (slot != null) slot.parent else null
	}
	/**
	 * - Get a specific slot.
	 * @param name - The slot name.
	 * @see dragonBones.Slot
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定的插槽。
	 * @param name - 插槽名称。
	 * @see dragonBones.Slot
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getSlot(name: String?): Slot? {
		this._slots.fastForEach { slot ->
			if (slot.name == name) {
				return slot
			}
		}

		return null
	}
	/**
	 * - Get a specific slot by the display.
	 * @param display - The display object.
	 * @see dragonBones.Slot
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 通过显示对象获取特定的插槽。
	 * @param display - 显示对象。
	 * @see dragonBones.Slot
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getSlotByDisplay(display: Any?): Slot? {
		if (display != null) {
			this._slots.fastForEach { slot ->
				if (slot.display == display) {
					return slot
				}
			}
		}

		return null
	}
	/**
	 * - Get all bones.
	 * @see dragonBones.Bone
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取所有的骨骼。
	 * @see dragonBones.Bone
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getBones(): FastArrayList<Bone> {
		return this._bones
	}
	/**
	 * - Get all slots.
	 * @see dragonBones.Slot
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取所有的插槽。
	 * @see dragonBones.Slot
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getSlots(): FastArrayList<Slot> {
		return this._slots
	}
	/**
	 * - Whether to flip the armature horizontally.
	 * @version DragonBones 5.5
	 * @language en_US
	 */
	/**
	 * - 是否将骨架水平翻转。
	 * @version DragonBones 5.5
	 * @language zh_CN
	 */
	var flipX: Boolean get() = this._flipX
		set(value) {
			if (this._flipX == value) {
				return
			}

			this._flipX = value
			this.invalidUpdate()
		}
	/**
	 * - Whether to flip the armature vertically.
	 * @version DragonBones 5.5
	 * @language en_US
	 */
	/**
	 * - 是否将骨架垂直翻转。
	 * @version DragonBones 5.5
	 * @language zh_CN
	 */
	var flipY: Boolean get() = this._flipY
		set(value) {
			if (this._flipY == value) {
				return
			}

			this._flipY = value
			this.invalidUpdate()
		}
	/**
	 * - The animation cache frame rate, which turns on the animation cache when the set value is greater than 0.
	 * There is a certain amount of memory overhead to improve performance by caching animation data in memory.
	 * The frame rate should not be set too high, usually with the frame rate of the animation is similar and lower than the program running frame rate.
	 * When the animation cache is turned on, some features will fail, such as the offset property of bone.
	 * @example
	 * <pre>
	 *     armature.cacheFrameRate = 24;
	 * </pre>
	 * @see dragonBones.DragonBonesData#frameRate
	 * @see dragonBones.ArmatureData#frameRate
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 动画缓存帧率，当设置的值大于 0 的时，将会开启动画缓存。
	 * 通过将动画数据缓存在内存中来提高运行性能，会有一定的内存开销。
	 * 帧率不宜设置的过高，通常跟动画的帧率相当且低于程序运行的帧率。
	 * 开启动画缓存后，某些功能将会失效，比如骨骼的 offset 属性等。
	 * @example
	 * <pre>
	 *     armature.cacheFrameRate = 24;
	 * </pre>
	 * @see dragonBones.DragonBonesData#frameRate
	 * @see dragonBones.ArmatureData#frameRate
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	var cacheFrameRate: Int get() = this._armatureData!!.cacheFrameRate
		set(value) {
			if (this._armatureData!!.cacheFrameRate != value) {
				this._armatureData!!.cacheFrames(value)

				// Set child armature frameRate.
				this._slots.fastForEach { slot ->
					val childArmature = slot.childArmature
					if (childArmature != null) {
						childArmature.cacheFrameRate = value
					}
				}
			}
		}
	/**
	 * - The armature name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 骨架名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val name: String get() = this._armatureData?.name ?: ""
	/**
	 * - The armature data.
	 * @see dragonBones.ArmatureData
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 骨架数据。
	 * @see dragonBones.ArmatureData
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	val armatureData: ArmatureData get() = this._armatureData!!
	/**
	 * - The animation player.
	 * @see dragonBones.Animation
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画播放器。
	 * @see dragonBones.Animation
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val animation: Animation get() = this._animation!!
	/**
	 * @pivate
	 */
	val proxy: IArmatureProxy get() = this._proxy!!
	/**
	 * - The EventDispatcher instance of the armature.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 该骨架的 EventDispatcher 实例。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	val eventDispatcher: IEventDispatcher get() = this._proxy!!
	/**
	 * - The display container.
	 * The display of the slot is displayed as the parent.
	 * Depending on the rendering engine, the type will be different, usually the DisplayObjectContainer type.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 显示容器实例。
	 * 插槽的显示对象都会以此显示容器为父级。
	 * 根据渲染引擎的不同，类型会不同，通常是 DisplayObjectContainer 类型。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val display: Any get() = this._display!!
	/**
	 * @private
	 */
	var replacedTexture: Any? get() = this._replacedTexture
		set (value) {
			if (this._replacedTexture != value) {
				this._replaceTextureAtlasData?.returnToPool()
				this._replaceTextureAtlasData = null

				this._replacedTexture = value

				this._slots.fastForEach { slot ->
					slot.invalidUpdate()
					slot.update(-1)
				}
			}
		}
	/**
	 * @inheritDoc
	 */
	override var clock: WorldClock? get() = this._clock
		set(value) {
			if (this._clock == value) {
				return
			}

			this._clock?.remove(this)

			this._clock = value
			this._clock?.add(this)

			// Update childArmature clock.
			this._slots.fastForEach { slot ->
				val childArmature = slot.childArmature
				if (childArmature != null) {
					childArmature.clock = this._clock
				}
			}
		}
	///**
	// * - Get the parent slot which the armature belongs to.
	// * @see dragonBones.Slot
	// * @version DragonBones 4.5
	// * @language en_US
	// */
	///**
	// * - 该骨架所属的父插槽。
	// * @see dragonBones.Slot
	// * @version DragonBones 4.5
	// * @language zh_CN
	// */
	//val parent: Slot? get() {
	//	return this._parent
	//}
	//fun getDisplay(): Any {
	//	return this._display
	//}
}
