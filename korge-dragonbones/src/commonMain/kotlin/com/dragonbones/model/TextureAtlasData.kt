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
import com.soywiz.klogger.*
import com.soywiz.korma.geom.*

/**
 * - The texture atlas data.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 贴图集数据。
 * @version DragonBones 3.0
 * @language zh_CN
 */
abstract class TextureAtlasData(pool: SingleObjectPool<out TextureAtlasData>) :  BaseObject(pool) {
	/**
	 * @private
	 */
	var autoSearch: Boolean = false
	/**
	 * @private
	 */
	var width: Int = 0
	/**
	 * @private
	 */
	var height: Int = 0
	/**
	 * @private
	 */
	var scale: Double = 1.0
	/**
	 * - The texture atlas name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 贴图集名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var name: String = ""
	/**
	 * - The image path of the texture atlas.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 贴图集图片路径。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var imagePath: String = ""
	/**
	 * @private
	 */
	val textures: FastStringMap<TextureData> = FastStringMap()

	override fun _onClear() {
		this.textures.fastValueForEach { v ->
			v.returnToPool()
		}
		this.textures.clear()

		this.autoSearch = false
		this.width = 0
		this.height = 0
		this.scale = 1.0
		// this.textures.clear();
		this.name = ""
		this.imagePath = ""
	}

	/**
	 * @private
	 */
	fun copyFrom(value: TextureAtlasData) {
		this.autoSearch = value.autoSearch
		this.scale = value.scale
		this.width = value.width
		this.height = value.height
		this.name = value.name
		this.imagePath = value.imagePath

		this.textures.fastValueForEach { v ->
			v.returnToPool()
		}
		this.textures.clear()

		// this.textures.clear();

		value.textures.fastKeyForEach { k ->
			val texture = this.createTexture()
			texture.copyFrom(value.textures[k]!!)
			this.textures[k] = texture
		}
	}

	/**
	 * @internal
	 */
	abstract fun createTexture(): TextureData
	/**
	 * @internal
	 */
	fun addTexture(value: TextureData) {
		if (value.name in this.textures) {
			Console.warn("Same texture: " + value.name)
			return
		}

		value.parent = this
		this.textures[value.name] = value
	}

	/**
	 * @private
	 */
	fun getTexture(textureName: String): TextureData? {
		return if (textureName in this.textures) this.textures[textureName] else null
	}
}
/**
 * @private
 */
abstract class TextureData(pool: SingleObjectPool<out TextureData>) : BaseObject(pool) {
	companion object {
		fun createRectangle(): Rectangle {
			return Rectangle()
		}
	}

	var rotated: Boolean = false
	var name: String = ""
	val region: Rectangle = Rectangle()
	var parent: TextureAtlasData? = null
	var frame: Rectangle? = null // Initial value.

	override fun _onClear() {
		this.rotated = false
		this.name = ""
		this.region.clear()
		this.parent = null //
		this.frame = null
	}

	fun copyFrom(value: TextureData) {
		this.rotated = value.rotated
		this.name = value.name
		this.region.copyFrom(value.region)
		this.parent = value.parent

		if (this.frame == null && value.frame != null) {
			this.frame = TextureData.createRectangle()
		}
		else if (this.frame != null && value.frame == null) {
			this.frame = null
		}

		if (this.frame != null && value.frame != null) {
			this.frame!!.copyFrom(value.frame!!)
		}
	}
}
