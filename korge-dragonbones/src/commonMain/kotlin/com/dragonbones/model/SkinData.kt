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
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*

/**
 * - The skin data, typically a armature data instance contains at least one skinData.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 皮肤数据，通常一个骨架数据至少包含一个皮肤数据。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class SkinData(pool: SingleObjectPool<SkinData>) : BaseObject(pool) {
	/**
	 * - The skin name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 皮肤名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var name: String = ""
	/**
	 * @private
	 */
	val displays: FastStringMap<FastArrayList<DisplayData?>> = FastStringMap()
	/**
	 * @private
	 */
	var parent: ArmatureData? = null

	override fun _onClear() {
		this.displays.fastValueForEach { slotDisplays ->
			slotDisplays.fastForEach { display ->
                display?.returnToPool()
			}
		}
		this.displays.clear()

		this.name = ""
		//this.parent = null //
	}

	/**
	 * @internal
	 */
	fun addDisplay(slotName: String, value: DisplayData?) {
		if (!(slotName in this.displays)) {
			this.displays[slotName] = FastArrayList()
		}

		if (value != null) {
			value.parent = this
		}

		val slotDisplays = this.displays[slotName] // TODO clear prev
		slotDisplays?.add(value)
	}

	/**
	 * @private
	 */
	fun getDisplay(slotName: String, displayName: String): DisplayData? {
		getDisplays(slotName)?.fastForEach { display ->
			if (display != null && display.name == displayName) {
				return display
			}
		}

		return null
	}

	/**
	 * @private
	 */
	fun getDisplays(slotName: String?): FastArrayList<DisplayData?>? = this.displays.getNull(slotName)

    override fun toString(): String = "[class dragonBones.SkinData]"
}
