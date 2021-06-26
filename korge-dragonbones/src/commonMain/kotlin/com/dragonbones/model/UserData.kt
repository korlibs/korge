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

/**
 * - The user custom data.
 * @version DragonBones 5.0
 * @language en_US
 */
/**
 * - 用户自定义数据。
 * @version DragonBones 5.0
 * @language zh_CN
 */
class UserData(pool: SingleObjectPool<UserData>) : BaseObject(pool) {
	override fun toString(): String {
		return "[class dragonBones.UserData]"
	}
	/**
	 * - The custom int numbers.
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 自定义整数。
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	val ints: IntArrayList = IntArrayList()
	/**
	 * - The custom float numbers.
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 自定义浮点数。
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	val floats: DoubleArrayList = DoubleArrayList()
	/**
	 * - The custom strings.
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 自定义字符串。
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	val strings: FastArrayList<String> = FastArrayList()

	override fun _onClear() {
		this.ints.clear()
		this.floats.clear()
		this.strings.clear()
	}

	/**
	 * @internal
	 */
	fun addInt(value: Int) {
		this.ints.push(value)
	}

	/**
	 * @internal
	 */
	fun addFloat(value: Double) {
		this.floats.push(value)
	}

	/**
	 * @internal
	 */
	fun addString(value: String) {
        this.strings.add(value)
	}
	/**
	 * - Get the custom int number.
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 获取自定义整数。
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	fun getInt(index: Int = 0): Int {
		return if (index >= 0 && index < this.ints.length) this.ints[index] else 0
	}
	/**
	 * - Get the custom float number.
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 获取自定义浮点数。
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	fun getFloat(index: Int = 0): Double = if (index >= 0 && index < this.floats.length) this.floats[index] else 0.0
	/**
	 * - Get the custom string.
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 获取自定义字符串。
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	fun getString(index: Int = 0): String = if (index >= 0 && index < this.strings.length) this.strings[index] else ""
}

/**
 * @private
 */
class ActionData(pool: SingleObjectPool<ActionData>) : BaseObject(pool) {
	override fun toString(): String {
		return "[class dragonBones.ActionData]"
	}

	var type: ActionType = ActionType.Play
	var name: String = "" // Frame event name | Sound event name | Animation name
	var bone: BoneData? = null
	var slot: SlotData? = null
	var data: UserData? = null //

	override fun _onClear() {
		this.data?.returnToPool()

		this.type = ActionType.Play
		this.name = ""
		this.bone = null
		this.slot = null
		this.data = null
	}
}
