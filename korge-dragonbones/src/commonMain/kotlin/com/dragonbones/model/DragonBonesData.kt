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
import com.dragonbones.util.*
import com.soywiz.kds.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*

/**
 * - The DragonBones data.
 * A DragonBones data contains multiple armature data.
 * @see dragonBones.ArmatureData
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 龙骨数据。
 * 一个龙骨数据包含多个骨架数据。
 * @see dragonBones.ArmatureData
 * @version DragonBones 3.0
 * @language zh_CN
 */
class DragonBonesData(pool: SingleObjectPool<DragonBonesData>) : BaseObject(pool) {
	override fun toString(): String {
		return "[class dragonBones.DragonBonesData]"
	}

	/**
	 * @private
	 */
	var autoSearch: Boolean = false
	/**
	 * - The animation frame rate.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画帧频。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var frameRate: Int = 0
	/**
	 * - The data version.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 数据版本。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var version: String = ""
	/**
	 * - The DragonBones data name.
	 * The name is consistent with the DragonBones project name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 龙骨数据名称。
	 * 该名称与龙骨项目名保持一致。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var name: String = ""
	/**
	 * @private
	 */
	var stage: ArmatureData? = null
	/**
	 * @internal
	 */
	val frameIndices:  IntArrayList = IntArrayList()
	/**
	 * @internal
	 */
	val cachedFrames:  DoubleArrayList = DoubleArrayList()
	/**
	 * - All armature data names.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 所有的骨架数据名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val armatureNames: FastArrayList<String> = FastArrayList()
	/**
	 * @private
	 */
	val armatures: FastStringMap<ArmatureData> = FastStringMap()
	/**
	 * @internal
	 */
	var binary: MemBuffer? = null
	/**
	 * @internal
	 */
	var intArray: Int16Buffer? = null // Int16Array
	//var intArray: Int32Buffer? = null // Int16Array
	//var intArray: IntArrayList? = null // Int16Array
	/**
	 * @internal
	 */
	var floatArray: Float32Buffer? = null
	/**
	 * @internal
	 */
	//var frameIntArray: Int16Buffer? = null
	//var frameIntArray: Float32BufferAsInt? = null
	var frameIntArray: Float32Buffer? = null
	//var frameIntArray: IntArrayList? = null
	/**
	 * @internal
	 */
	var frameFloatArray: Float32Buffer? = null
	//var frameFloatArray: DoubleArrayList? = null
	/**
	 * @internal
	 */
	var frameArray: Int16Buffer? = null
	//var frameArray: IntArrayList? = null
	//var frameArray: DoubleArrayList? = null
	/**
	 * @internal
	 */
	//var timelineArray: IntArrayList? = null
	var timelineArray: Uint16Buffer? = null
	/**
	 * @internal
	 */
	//var colorArray: IntArrayList? = null
	//var colorArray: IntArrayList? = null
	var colorArray: Int16Buffer? = null
	/**
	 * @private
	 */
	var userData: UserData? = null // Initial value.

	override fun _onClear() {
		this.armatures.fastValueForEach { a ->
			a.returnToPool()
		}
		this.armatures.clear()

		this.userData?.returnToPool()

		this.autoSearch = false
		this.frameRate = 0
		this.version = ""
		this.name = ""
		this.stage = null
		this.frameIndices.clear()
		this.cachedFrames.clear()
		this.armatureNames.clear()
		this.binary = null //
		this.intArray = null //
		this.floatArray = null //
		this.frameIntArray = null //
		this.frameFloatArray = null //
		this.frameArray = null //
		this.timelineArray = null //
		this.colorArray = null //
		this.userData = null
	}

	/**
	 * @internal
	 */
	fun addArmature(value: ArmatureData) {
		if (value.name in this.armatures) {
			Console.warn("Same armature: " + value.name)
			return
		}

		value.parent = this
		this.armatures[value.name] = value
		this.armatureNames.add(value.name)
	}
	/**
	 * - Get a specific armature data.
	 * @param armatureName - The armature data name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定的骨架数据。
	 * @param armatureName - 骨架数据名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getArmature(armatureName: String): ArmatureData? {
		return if (armatureName in this.armatures) this.armatures[armatureName] else null
	}
}
