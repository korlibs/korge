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

package com.soywiz.korge.dragonbones

import com.dragonbones.core.*
import com.dragonbones.model.*
import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*

/**
 * - The Dragonbones texture atlas data.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - Dragonbones 贴图集数据。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class KorgeDbTextureAtlasData(pool: SingleObjectPool<KorgeDbTextureAtlasData>) : TextureAtlasData(pool) {
	override fun toString(): String {
		return "[class DragonbonesTextureAtlasData]"
	}

	private var _renderTexture: Bitmap? = null // Initial value.

	override fun _onClear() {
		super._onClear()

		if (this._renderTexture !== null) {
			// this._renderTexture.dispose();
		}

		this._renderTexture = null
	}

	/**
	 * @inheritDoc
	 */
	override fun createTexture(): TextureData = pool.textureData.borrow()
	/**
	 * - The Dragonbones texture.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - Dragonbones 贴图。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var renderTexture: Bitmap?
		get() = this._renderTexture
		set(value) {
			if (this._renderTexture == value) {
				return
			}

			this._renderTexture = value

			if (this._renderTexture !== null) {
				this.textures.fastKeyForEach { k ->
					val textureData = this.textures[k] as KorgeDbTextureData

					textureData.renderTexture = BitmapSliceCompat(
						this._renderTexture!!,
						Rectangle(textureData.region.x, textureData.region.y, textureData.region.width, textureData.region.height),
						Rectangle(textureData.region.x, textureData.region.y, textureData.region.width, textureData.region.height),
						Rectangle(0.0, 0.0, textureData.region.width, textureData.region.height),
						textureData.rotated,
						name = k
					)
				}
			}
			else {
				this.textures.fastKeyForEach { k ->
					val textureData = this.textures[k] as KorgeDbTextureData
					textureData.renderTexture = null
				}
			}
		}
}
/**
 * @internal
 */
class KorgeDbTextureData(pool: SingleObjectPool<KorgeDbTextureData>) : TextureData(pool) {
	override fun toString(): String = "[class DragonbonesTextureData]"

	var renderTexture: BmpSlice? = null // Initial value.

	override fun _onClear() {
		super._onClear()

		if (this.renderTexture !== null) {
			//this.renderTexture.dispose(false) // Not requires on Korge
		}

		this.renderTexture = null
	}
}
