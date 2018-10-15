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
package com.dragonbones.model

import com.dragonbones.core.*
import com.dragonbones.geom.*
import com.dragonbones.util.*
import com.soywiz.kds.*

/**
 * - The armature data.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 骨架数据。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class ArmatureData(pool: BaseObjectPool) : BaseObject(pool) {
	override fun toString(): String {
		return "[class dragonBones.ArmatureData]"
	}

	/**
	 * @private
	 */
	var type: ArmatureType = ArmatureType.Armature
	/**
	 * - The animation frame rate.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画帧率。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var frameRate: Int = 0
	/**
	 * @private
	 */
	var cacheFrameRate: Int = 0
	/**
	 * @private
	 */
	var scale: Double = 1.0
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
	var name: String = ""
	/**
	 * @private
	 */
	val aabb: Rectangle = Rectangle()
	/**
	 * - The names of all the animation data.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 所有的动画数据名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val animationNames: ArrayList<String> = arrayListOf()
	/**
	 * @private
	 */
	val sortedBones: ArrayList<BoneData> = arrayListOf()
	/**
	 * @private
	 */
	val sortedSlots: ArrayList<SlotData> = arrayListOf()
	/**
	 * @private
	 */
	val defaultActions: ArrayList<ActionData> = arrayListOf()
	/**
	 * @private
	 */
	val actions: ArrayList<ActionData> = arrayListOf()
	/**
	 * @private
	 */
	val bones: FastStringMap<BoneData> = FastStringMap()
	/**
	 * @private
	 */
	val slots: FastStringMap<SlotData> = FastStringMap()
	/**
	 * @private
	 */
	val constraints: FastStringMap<ConstraintData> = FastStringMap()
	/**
	 * @private
	 */
	val skins: FastStringMap<SkinData> = FastStringMap()
	/**
	 * @private
	 */
	val animations: FastStringMap<AnimationData> = FastStringMap()
	/**
	 * - The default skin data.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 默认插槽数据。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	var defaultSkin: SkinData? = null
	/**
	 * - The default animation data.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 默认动画数据。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	var defaultAnimation: AnimationData? = null
	/**
	 * @private
	 */
	var canvas: CanvasData? = null // Initial value.
	/**
	 * @private
	 */
	var userData: UserData? = null // Initial value.
	/**
	 * @private
	 */
	var parent: DragonBonesData? = null

	override fun _onClear() {
		for (action in this.defaultActions) {
			action.returnToPool()
		}

		for (action in this.actions) {
			action.returnToPool()
		}

		for (v in this.bones.values) v.returnToPool()
		this.bones.clear()

		for (v in this.slots.values) v.returnToPool()
		this.slots.clear()

		for (v in this.constraints.values) v.returnToPool()
		this.constraints.clear()

		for (v in this.skins.values) v.returnToPool()
		this.skins.clear()

		for (v in this.animations.values) v.returnToPool()
		this.animations.clear()

		this.canvas?.returnToPool()

		this.userData?.returnToPool()

		this.type = ArmatureType.Armature
		this.frameRate = 0
		this.cacheFrameRate = 0
		this.scale = 1.0
		this.name = ""
		this.aabb.clear()
		this.animationNames.clear()
		this.sortedBones.clear()
		this.sortedSlots.clear()
		this.defaultActions.clear()
		this.actions.clear()
		// this.bones.clear();
		// this.slots.clear();
		// this.constraints.clear();
		// this.skins.clear();
		// this.animations.clear();
		this.defaultSkin = null
		this.defaultAnimation = null
		this.canvas = null
		this.userData = null
		this.parent = null //
	}

	/**
	 * @internal
	 */
	fun sortBones() {
		val total = this.sortedBones.size
		if (total <= 0) {
			return
		}

		val sortHelper = this.sortedBones.toList()
		var index = 0
		var count = 0
		this.sortedBones.lengthSet = 0
		while (count < total) {
			val bone = sortHelper[index++]
			if (index >= total) {
				index = 0
			}

			if (this.sortedBones.indexOf(bone) >= 0) {
				continue
			}

			var flag = false
			for (constraint in this.constraints.values) { // Wait constraint.
				if (constraint.root == bone && this.sortedBones.indexOf(constraint.target) < 0) {
					flag = true
					break
				}
			}

			if (flag) {
				continue
			}

			if (bone.parent != null && this.sortedBones.indexOf(bone.parent!!) < 0) { // Wait parent.
				continue
			}

			this.sortedBones.add(bone)
			count++
		}
	}

	/**
	 * @internal
	 */
	fun cacheFrames(frameRate: Int) {
		if (this.cacheFrameRate > 0) { // TODO clear cache.
			return
		}

		this.cacheFrameRate = frameRate
		for (k in this.animations.keys) {
			this.animations[k]!!.cacheFrames(this.cacheFrameRate)
		}
	}

	/**
	 * @internal
	 */
	fun setCacheFrame(globalTransformMatrix: Matrix, transform: Transform): Int {
		val dataArray = this.parent!!.cachedFrames
		val arrayOffset = dataArray.size

		dataArray.lengthSet += 10
		dataArray[arrayOffset] = globalTransformMatrix.a.toDouble()
		dataArray[arrayOffset + 1] = globalTransformMatrix.b.toDouble()
		dataArray[arrayOffset + 2] = globalTransformMatrix.c.toDouble()
		dataArray[arrayOffset + 3] = globalTransformMatrix.d.toDouble()
		dataArray[arrayOffset + 4] = globalTransformMatrix.tx.toDouble()
		dataArray[arrayOffset + 5] = globalTransformMatrix.ty.toDouble()
		dataArray[arrayOffset + 6] = transform.rotation.toDouble()
		dataArray[arrayOffset + 7] = transform.skew.toDouble()
		dataArray[arrayOffset + 8] = transform.scaleX.toDouble()
		dataArray[arrayOffset + 9] = transform.scaleY.toDouble()

		return arrayOffset
	}

	/**
	 * @internal
	 */
	fun getCacheFrame(globalTransformMatrix: Matrix, transform: Transform, arrayOffset: Int) {
		val dataArray = this.parent!!.cachedFrames
		globalTransformMatrix.a = dataArray[arrayOffset].toFloat()
		globalTransformMatrix.b = dataArray[arrayOffset + 1].toFloat()
		globalTransformMatrix.c = dataArray[arrayOffset + 2].toFloat()
		globalTransformMatrix.d = dataArray[arrayOffset + 3].toFloat()
		globalTransformMatrix.tx = dataArray[arrayOffset + 4].toFloat()
		globalTransformMatrix.ty = dataArray[arrayOffset + 5].toFloat()
		transform.rotation = dataArray[arrayOffset + 6].toFloat()
		transform.skew = dataArray[arrayOffset + 7].toFloat()
		transform.scaleX = dataArray[arrayOffset + 8].toFloat()
		transform.scaleY = dataArray[arrayOffset + 9].toFloat()
		transform.x = globalTransformMatrix.tx
		transform.y = globalTransformMatrix.ty
	}

	/**
	 * @internal
	 */
	fun addBone(value: BoneData) {
		if (value.name in this.bones) {
			console.warn("Same bone: " + value.name)
			return
		}

		this.bones[value.name] = value
		this.sortedBones.push(value)
	}

	/**
	 * @internal
	 */
	fun addSlot(value: SlotData) {
		if (value.name in this.slots) {
			console.warn("Same slot: " + value.name)
			return
		}

		this.slots[value.name] = value
		this.sortedSlots.push(value)
	}

	/**
	 * @internal
	 */
	fun addConstraint(value: ConstraintData) {
		if (value.name in this.constraints) {
			console.warn("Same constraint: " + value.name)
			return
		}

		this.constraints[value.name] = value
	}

	/**
	 * @internal
	 */
	fun addSkin(value: SkinData) {
		if (value.name in this.skins) {
			console.warn("Same skin: " + value.name)
			return
		}

		value.parent = this
		this.skins[value.name] = value
		if (this.defaultSkin == null) {
			this.defaultSkin = value
		}

		if (value.name == "default") {
			this.defaultSkin = value
		}
	}

	/**
	 * @internal
	 */
	fun addAnimation(value: AnimationData) {
		if (value.name in this.animations) {
			console.warn("Same animation: " + value.name)
			return
		}

		value.parent = this
		this.animations[value.name] = value
		this.animationNames.push(value.name)
		if (this.defaultAnimation == null) {
			this.defaultAnimation = value
		}
	}

	/**
	 * @internal
	 */
	fun addAction(value: ActionData, isDefault: Boolean) {
		if (isDefault) {
			this.defaultActions.push(value)
		} else {
			this.actions.push(value)
		}
	}
	/**
	 * - Get a specific done data.
	 * @param boneName - The bone name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定的骨骼数据。
	 * @param boneName - 骨骼名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getBone(boneName: String?): BoneData? = this.bones.getNull(boneName)
	/**
	 * - Get a specific slot data.
	 * @param slotName - The slot name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定的插槽数据。
	 * @param slotName - 插槽名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getSlot(slotName: String?): SlotData? = this.slots.getNull(slotName)

	/**
	 * @private
	 */
	fun getConstraint(constraintName: String): ConstraintData? = this.constraints[constraintName]
	/**
	 * - Get a specific skin data.
	 * @param skinName - The skin name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定皮肤数据。
	 * @param skinName - 皮肤名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getSkin(skinName: String): SkinData? = this.skins[skinName]

	/**
	 * @private
	 */
	fun getMesh(skinName: String, slotName: String, meshName: String): MeshDisplayData? {
		val skin = this.getSkin(skinName) ?: return null
		return skin.getDisplay(slotName, meshName) as MeshDisplayData?
	}
	/**
	 * - Get a specific animation data.
	 * @param animationName - The animation animationName.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定的动画数据。
	 * @param animationName - 动画名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getAnimation(animationName: String): AnimationData? =
		if (animationName in this.animations) this.animations[animationName] else null
}
/**
 * - The bone data.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 骨骼数据。
 * @version DragonBones 3.0
 * @language zh_CN
 */
open class BoneData(pool: BaseObjectPool) : BaseObject(pool) {
	override fun toString(): String {
		return "[class dragonBones.BoneData]"
	}

	/**
	 * @private
	 */
	var inheritTranslation: Boolean = false
	/**
	 * @private
	 */
	var inheritRotation: Boolean = false
	/**
	 * @private
	 */
	var inheritScale: Boolean = false
	/**
	 * @private
	 */
	var inheritReflection: Boolean = false
	/**
	 * @private
	 */
	var isSurface = false
	/**
	 * - The bone length.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 骨骼长度。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var length: Double = 0.0
	/**
	 * @private
	 */
	var alpha: Double = 1.0
	/**
	 * - The bone name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 骨骼名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var name: String = ""
	/**
	 * @private
	 */
	val transform: Transform = Transform()
	/**
	 * @private
	 */
	var userData: UserData? = null // Initial value.
	/**
	 * - The parent bone data.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 父骨骼数据。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var parent: BoneData? = null

	// SurfaceData
	var segmentX: Int = 0
	var segmentY: Int = 0
	val geometry: GeometryData = GeometryData()

	val isBone get() = !isSurface

	override fun _onClear() {
		this.userData?.returnToPool()
		this.inheritTranslation = false
		this.inheritRotation = false
		this.inheritScale = false
		this.inheritReflection = false
		this.isSurface = false
		this.length = 0.0
		this.alpha = 1.0
		this.name = ""
		this.transform.identity()
		this.userData = null
		this.parent = null
	}
}

/**
 * @internal
 */
class SurfaceData(pool: BaseObjectPool) : BoneData(pool) {
	override fun toString(): String {
		return "[class dragonBones.SurfaceData]"
	}

	override fun _onClear() {
		super._onClear()

		this.isSurface = true
		this.segmentX = 0
		this.segmentY = 0
		this.geometry.clear()
	}
}
/**
 * - The slot data.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 插槽数据。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class SlotData(pool: BaseObjectPool) : BaseObject(pool) {
	companion object {
		/**
		 * @internal
		 */
		fun createColor(): ColorTransform {
			return ColorTransform()
		}

	}

	override fun toString(): String {
		return "[class dragonBones.SlotData]"
	}

	/**
	 * @private
	 */
	var blendMode: BlendMode = BlendMode.Normal
	/**
	 * @private
	 */
	var displayIndex: Int = 0
	/**
	 * @private
	 */
	var zOrder: Int = 0
	/**
	 * @private
	 */
	var zIndex: Int = 0
	/**
	 * @private
	 */
	var alpha: Double = 1.0
	/**
	 * - The slot name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 插槽名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var name: String = ""
	/**
	 * @private
	 */
	var color: ColorTransform? = null // Initial value.
	/**
	 * @private
	 */
	var userData: UserData? = null // Initial value.
	/**
	 * - The parent bone data.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 父骨骼数据。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var parent: BoneData? = null

	override fun _onClear() {
		this.userData?.returnToPool()
		this.blendMode = BlendMode.Normal
		this.displayIndex = 0
		this.zOrder = 0
		this.zIndex = 0
		this.alpha = 1.0
		this.name = ""
		this.color = null //
		this.userData = null
		this.parent = null //
	}
}
