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

import com.dragonbones.armature.*
import com.dragonbones.core.*
import com.dragonbones.factory.*
import com.dragonbones.model.*
import com.dragonbones.parser.*
import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import kotlin.native.concurrent.ThreadLocal

/**
 * - The Dragobones factory.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - Dragobones 工厂。
 * @version DragonBones 3.0
 * @language zh_CN
 */
open class KorgeDbFactory(pool: BaseObjectPool = BaseObjectPool(), dataParser: DataParser = ObjectDataParser(pool)) : BaseFactory(pool, dataParser) {
	init {
	}

	val eventManager = KorgeDbArmatureDisplay()
	private val _dragonBonesInstance: DragonBones = DragonBones(eventManager)
	//private fun _clockHandler(passedTime: Double) {
	//	this._dragonBonesInstance.advanceTime(Klock.currentTimeMillisDouble() * passedTime * 0.001)
	//}
	/**
	 * - A global factory instance that can be used directly.
	 * @version DragonBones 4.7
	 * @language en_US
	 */
	/**
	 * - 一个可以直接使用的全局工厂实例。
	 * @version DragonBones 4.7
	 * @language zh_CN
	 */

	/**
	 * @inheritDoc
	 */
	init {
		this._dragonBones = _dragonBonesInstance
		//Dragonbones.ticker.shared.add(DragonbonesFactory._clockHandler, DragonbonesFactory)
		println("@TODO: Dragonbones.ticker.shared.add(DragonbonesFactory._clockHandler, DragonbonesFactory)") // @TODO: Dragonbones.ticker.shared.add(DragonbonesFactory._clockHandler, DragonbonesFactory)

	}

	override fun _buildTextureAtlasData(
		textureAtlasData: TextureAtlasData?,
		textureAtlas: Any?
	): KorgeDbTextureAtlasData {
		var textureAtlasData = textureAtlasData as? KorgeDbTextureAtlasData?
		val textureAtlas = textureAtlas as? Bitmap?

		if (textureAtlasData != null) {
			textureAtlasData.renderTexture = textureAtlas
		} else {
			textureAtlasData = pool.textureAtlasData.borrow()
		}

		return textureAtlasData
	}

	override fun _buildArmature(dataPackage: BuildArmaturePackage): Armature {
		val armature = pool.armature.borrow()
		val armatureDisplay = KorgeDbArmatureDisplay()

		armature.init(
			dataPackage.armature!!,
			armatureDisplay, armatureDisplay, this._dragonBones
		)

		return armature
	}

	override fun _buildSlot(dataPackage: BuildArmaturePackage, slotData: SlotData, armature: Armature): Slot {
		val slot = pool.slot.borrow()
		slot.init(
			slotData, armature,
			Image(Bitmaps.transparent), Mesh(drawMode = Mesh.DrawModes.Triangles)
		)

		return slot
	}
	/**
	 * - Create an armature from cached DragonBonesData instances and TextureAtlasData instances, then use the {@link #clock} to update it.
	 * The difference is that the armature created by {@link #buildArmature} is not WorldClock instance update.
	 * @param armatureName - The armature data name.
	 * @param dragonBonesName - The cached name of the DragonBonesData instance. (If not set, all DragonBonesData instances are retrieved, and when multiple DragonBonesData instances contain a the same name armature data, it may not be possible to accurately create a specific armature)
	 * @param skinName - The skin name, you can set a different ArmatureData name to share it's skin data. (If not set, use the default skin data)
	 * @returns The armature display container.
	 * @see dragonBones.IArmatureProxy
	 * @see dragonBones.BaseFactory#buildArmature
	 * @version DragonBones 4.5
	 * @example
	 * <pre>
	 *     let armatureDisplay = factory.buildArmatureDisplay("armatureName", "dragonBonesName");
	 * </pre>
	 * @language en_US
	 */
	/**
	 * - 通过缓存的 DragonBonesData 实例和 TextureAtlasData 实例创建一个骨架，并用 {@link #clock} 更新该骨架。
	 * 区别在于由 {@link #buildArmature} 创建的骨架没有 WorldClock 实例驱动。
	 * @param armatureName - 骨架数据名称。
	 * @param dragonBonesName - DragonBonesData 实例的缓存名称。 （如果未设置，将检索所有的 DragonBonesData 实例，当多个 DragonBonesData 实例中包含同名的骨架数据时，可能无法准确的创建出特定的骨架）
	 * @param skinName - 皮肤名称，可以设置一个其他骨架数据名称来共享其皮肤数据。 （如果未设置，则使用默认的皮肤数据）
	 * @returns 骨架的显示容器。
	 * @see dragonBones.IArmatureProxy
	 * @see dragonBones.BaseFactory#buildArmature
	 * @version DragonBones 4.5
	 * @example
	 * <pre>
	 *     let armatureDisplay = factory.buildArmatureDisplay("armatureName", "dragonBonesName");
	 * </pre>
	 * @language zh_CN
	 */
	fun buildArmatureDisplay(
		armatureName: String,
		dragonBonesName: String = "",
		skinName: String = "",
		textureAtlasName: String = ""
	): KorgeDbArmatureDisplay? {
		val armature = this.buildArmature(armatureName, dragonBonesName, skinName, textureAtlasName)
		if (armature !== null) {
			this._dragonBones.clock.add(armature)

			return armature.display as KorgeDbArmatureDisplay
		}

		return null
	}
	/**
	 * - Create the display object with the specified texture.
	 * @param textureName - The texture data name.
	 * @param textureAtlasName - The texture atlas data name (Of not set, all texture atlas data will be searched)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 创建带有指定贴图的显示对象。
	 * @param textureName - 贴图数据名称。
	 * @param textureAtlasName - 贴图集数据名称。 （如果未设置，将检索所有的贴图集数据）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getTextureDisplay(textureName: String, textureAtlasName: String? = null): Image? {
		val textureData = this._getTextureData(
			if (textureAtlasName !== null) textureAtlasName else "",
			textureName
		) as? KorgeDbTextureData?
		if (textureData != null && textureData.renderTexture !== null) {
			return Image(textureData.renderTexture!!)
		}

		return null
	}
	/**
	 * - A global sound event manager.
	 * Sound events can be listened to uniformly from the manager.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 全局声音事件管理器。
	 * 声音事件可以从该管理器统一侦听。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	val soundEventManager: KorgeDbArmatureDisplay
		get() {
			return this._dragonBones.eventManager as KorgeDbArmatureDisplay
		}
}

@ThreadLocal
val Views.dragonbonsFactory by Extra.Property { KorgeDbFactory() }
