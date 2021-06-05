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

package com.dragonbones.factory

import com.dragonbones.animation.*
import com.dragonbones.armature.*
import com.dragonbones.core.*
import com.dragonbones.event.*
import com.dragonbones.model.*
import com.dragonbones.parser.*
import com.dragonbones.util.*
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*

/**
 * - Base class for the factory that create the armatures. (Typically only one global factory instance is required)
 * The factory instance create armatures by parsed and added DragonBonesData instances and TextureAtlasData instances.
 * Once the data has been parsed, it has been cached in the factory instance and does not need to be parsed again until it is cleared by the factory instance.
 * @see dragonBones.DragonBonesData
 * @see dragonBones.TextureAtlasData
 * @see dragonBones.ArmatureData
 * @see dragonBones.Armature
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 创建骨架的工厂基类。 （通常只需要一个全局工厂实例）
 * 工厂通过解析并添加的 DragonBonesData 实例和 TextureAtlasData 实例来创建骨架。
 * 当数据被解析过之后，已经添加到工厂中，在没有被工厂清理之前，不需要再次解析。
 * @see dragonBones.DragonBonesData
 * @see dragonBones.TextureAtlasData
 * @see dragonBones.ArmatureData
 * @see dragonBones.Armature
 * @version DragonBones 3.0
 * @language zh_CN
 */
/**
 * - Create a factory instance. (typically only one global factory instance is required)
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 创建一个工厂实例。 （通常只需要一个全局工厂实例）
 * @version DragonBones 3.0
 * @language zh_CN
 */
@Suppress("unused", "MemberVisibilityCanBePrivate", "PropertyName", "FunctionName")
abstract class BaseFactory(val pool: BaseObjectPool, dataParser: DataParser = ObjectDataParser(pool)) {
	/**
	 * @private
	 */
	var autoSearch: Boolean = false

	private val _binaryDataParser by lazy { BinaryDataParser(pool) }

	protected val _dragonBonesDataMap: FastStringMap<DragonBonesData> = FastStringMap()
	protected val _textureAtlasDataMap: FastStringMap<FastArrayList<TextureAtlasData>> = FastStringMap()
	protected lateinit var _dragonBones: DragonBones
	protected var _dataParser: DataParser = dataParser


	protected fun _isSupportMesh(): Boolean {
		return true
	}

	protected fun _getTextureData(textureAtlasName: String, textureName: String): TextureData? {
		if (textureAtlasName in this._textureAtlasDataMap) {
			this._textureAtlasDataMap[textureAtlasName]!!.fastForEach { textureAtlasData ->
				val textureData = textureAtlasData.getTexture(textureName)
				if (textureData != null) {
					return textureData
				}
			}
		}

		if (this.autoSearch) { // Will be search all data, if the autoSearch is true.
			this._textureAtlasDataMap.fastKeyForEach { k ->
				this._textureAtlasDataMap[k]!!.fastForEach { textureAtlasData ->
					if (textureAtlasData.autoSearch) {
						val textureData = textureAtlasData.getTexture(textureName)
						if (textureData != null) {
							return textureData
						}
					}
				}
			}
		}

		return null
	}

	private fun _fillBuildArmaturePackage(
		dataPackage: BuildArmaturePackage,
		dragonBonesName: String, armatureName: String, skinName: String, textureAtlasName: String
	): Boolean {
		var mdragonBonesName = dragonBonesName
		var dragonBonesData: DragonBonesData? = null
		var armatureData: ArmatureData? = null

		if (mdragonBonesName.isNotEmpty()) {
			if (mdragonBonesName in this._dragonBonesDataMap) {
				dragonBonesData = this._dragonBonesDataMap[mdragonBonesName]
				armatureData = dragonBonesData?.getArmature(armatureName)
			}
		}

		if (armatureData == null && (mdragonBonesName.isEmpty() || this.autoSearch)) { // Will be search all data, if do not give a data name or the autoSearch is true.
			var completed = false
			this._dragonBonesDataMap.fastKeyForEach { k ->
				if (completed) return@fastKeyForEach
				dragonBonesData = this._dragonBonesDataMap[k]
				if (mdragonBonesName.isEmpty() || dragonBonesData!!.autoSearch) {
					armatureData = dragonBonesData?.getArmature(armatureName)
					if (armatureData != null) {
						mdragonBonesName = k
						completed = true
					}
				}
			}
		}

		if (armatureData != null) {
			dataPackage.dataName = mdragonBonesName
			dataPackage.textureAtlasName = textureAtlasName
			dataPackage.data = dragonBonesData
			dataPackage.armature = armatureData
			dataPackage.skin = null

			if (skinName.isNotEmpty()) {
				dataPackage.skin = armatureData!!.getSkin(skinName)
				if (dataPackage.skin == null && this.autoSearch) {
					dataPackage.skin = __findDataPackageSkin(skinName)
				}
			}

			if (dataPackage.skin == null) {
				dataPackage.skin = armatureData!!.defaultSkin
			}

			return true
		}

		return false
	}

	private fun __findDataPackageSkin(skinName: String): SkinData? {
		this._dragonBonesDataMap.fastKeyForEach { k ->
			val skinDragonBonesData = this._dragonBonesDataMap[k]!!
			val skinArmatureData = skinDragonBonesData.getArmature(skinName)
			if (skinArmatureData != null) {
				return skinArmatureData.defaultSkin
			}
		}
		return null
	}

	private fun _buildBones(dataPackage: BuildArmaturePackage, armature: Armature) {
		dataPackage.armature!!.sortedBones.fastForEach { boneData ->
			val bone = if (boneData.isBone) pool.bone.borrow() else pool.surface.borrow()
			bone.init(boneData, armature)
		}
	}
	/**
	 * @private
	 */
	private fun _buildSlots(dataPackage: BuildArmaturePackage, armature: Armature) {
		val currentSkin = dataPackage.skin
		val defaultSkin = dataPackage.armature?.defaultSkin
		if (currentSkin == null || defaultSkin == null) {
			return
		}

		val skinSlots: FastStringMap<FastArrayList<DisplayData?>> = FastStringMap()
		defaultSkin.displays.fastKeyForEach { k ->
			val displays = defaultSkin.getDisplays(k)
			skinSlots[k] = displays!!
		}

		if (currentSkin != defaultSkin) {
			currentSkin.displays.fastKeyForEach { k ->
				val displays = currentSkin.getDisplays(k)
				skinSlots[k] = displays!!
			}
		}

		dataPackage.armature!!.sortedSlots.fastForEach { slotData ->
			val displayDatas = skinSlots[slotData.name]
			val slot = this._buildSlot(dataPackage, slotData, armature)

			if (displayDatas != null) {
				slot.displayFrameCount = displayDatas.length
				for (i in 0 until slot.displayFrameCount) {
					val displayData = displayDatas[i]
					slot.replaceRawDisplayData(displayData, i)

					if (displayData != null) {
						if (dataPackage.textureAtlasName.isNotEmpty()) {
							val textureData = this._getTextureData(dataPackage.textureAtlasName, displayData.path)
							slot.replaceTextureData(textureData, i)
						}

						val display = this._getSlotDisplay(dataPackage, displayData, slot)
						slot.replaceDisplay(display, i)
					} else {
						slot.replaceDisplay(null)
					}
				}
			}

			slot._setDisplayIndex(slotData.displayIndex, true)
		}
	}

	private fun _buildConstraints(dataPackage: BuildArmaturePackage, armature: Armature) {
		val constraints = dataPackage.armature!!.constraints
		constraints.fastKeyForEach { k ->
			val constraintData = constraints[k]
			// TODO more constraint type.
			when (constraintData!!.type) {
				ConstraintType.IK -> {
					val ikConstraint = pool.iKConstraint.borrow()
					ikConstraint.init(constraintData, armature)
					armature._addConstraint(ikConstraint)
				}
				ConstraintType.Path -> {
					val pathConstraint = pool.pathConstraint.borrow()
					pathConstraint.init(constraintData, armature)
					armature._addConstraint(pathConstraint)
				}
			}
		}
	}

	private fun _buildChildArmature(dataPackage: BuildArmaturePackage?, @Suppress("UNUSED_PARAMETER") _slot: Slot, displayData: ArmatureDisplayData): Armature? {
		return this.buildArmature(displayData.path, dataPackage!!.dataName, "", dataPackage.textureAtlasName)
	}

	private fun _getSlotDisplay(dataPackage: BuildArmaturePackage?, displayData: DisplayData, slot: Slot): Any {
		val dataName = dataPackage?.dataName ?: displayData.parent.parent!!.parent!!.name
		var display: Any? = null
		when (displayData.type) {
			DisplayType.Image -> {
				val imageDisplayData = displayData as ImageDisplayData
				if (imageDisplayData.texture == null) {
					imageDisplayData.texture = this._getTextureData(dataName, displayData.path)
				}

				display = slot.rawDisplay
			}

			DisplayType.Mesh -> {
				val meshDisplayData = displayData as MeshDisplayData
				if (meshDisplayData.texture == null) {
					meshDisplayData.texture = this._getTextureData(dataName, meshDisplayData.path)
				}

				display = (if (this._isSupportMesh()) slot.meshDisplay else slot.rawDisplay)
			}

			DisplayType.Armature -> {
				val armatureDisplayData = displayData as ArmatureDisplayData
				val childArmature = this._buildChildArmature(dataPackage, slot, armatureDisplayData)
				if (childArmature != null) {
					childArmature.inheritAnimation = armatureDisplayData.inheritAnimation
					if (!childArmature.inheritAnimation) {
						val actions = if (armatureDisplayData.actions.length > 0) armatureDisplayData.actions else childArmature.armatureData.defaultActions
						if (actions.length > 0) {
							actions.fastForEach { action ->
								val eventObject = pool.eventObject.borrow()
								EventObject.actionDataToInstance(action, eventObject, slot.armature)
								eventObject.slot = slot
								slot.armature._bufferAction(eventObject, false)
							}
						}
						else {
							childArmature.animation.play()
						}
					}

					armatureDisplayData.armature = childArmature.armatureData //
				}

				display = childArmature
			}

			DisplayType.BoundingBox -> {

			}
			else -> {

			}
		}

		return display!!
	}

	protected abstract fun _buildTextureAtlasData(textureAtlasData: TextureAtlasData?, textureAtlas: Any?): TextureAtlasData
	protected abstract fun _buildArmature(dataPackage: BuildArmaturePackage): Armature
	protected abstract fun _buildSlot(dataPackage: BuildArmaturePackage, slotData: SlotData, armature: Armature): Slot
	/**
	 * - Parse the raw data to a DragonBonesData instance and cache it to the factory.
	 * @param rawData - The raw data.
	 * @param name - Specify a cache name for the instance so that the instance can be obtained through this name. (If not set, use the instance name instead)
	 * @param scale - Specify a scaling value for all armatures. (Default: 1.0)
	 * @returns DragonBonesData instance
	 * @see #getDragonBonesData()
	 * @see #addDragonBonesData()
	 * @see #removeDragonBonesData()
	 * @see dragonBones.DragonBonesData
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 将原始数据解析为 DragonBonesData 实例，并缓存到工厂中。
	 * @param rawData - 原始数据。
	 * @param name - 为该实例指定一个缓存名称，以便可以通过此名称获取该实例。 （如果未设置，则使用该实例中的名称）
	 * @param scale - 为所有的骨架指定一个缩放值。 （默认: 1.0）
	 * @returns DragonBonesData 实例
	 * @see #getDragonBonesData()
	 * @see #addDragonBonesData()
	 * @see #removeDragonBonesData()
	 * @see dragonBones.DragonBonesData
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun parseDragonBonesData(rawData: Any, name: String? = null, scale: Double = 1.0): DragonBonesData? {
		val dataParser = if (rawData is MemBuffer) _binaryDataParser else this._dataParser
		val dragonBonesData = dataParser.parseDragonBonesData(rawData, scale)

		while (true) {
			val textureAtlasData = this._buildTextureAtlasData(null, null)
			if (dataParser.parseTextureAtlasData(null, textureAtlasData, scale)) {
				this.addTextureAtlasData(textureAtlasData, name)
			}
			else {
				textureAtlasData.returnToPool()
				break
			}
		}

		if (dragonBonesData != null) {
			this.addDragonBonesData(dragonBonesData, name)
		}

		return dragonBonesData
	}

	fun parseDragonBonesDataJson(json: String, name: String? = null, scale: Double = 1.0): DragonBonesData? {
		/*
		val dragonBonesData = DataParser.parseDragonBonesDataJson(json)

		while (true) {
			val textureAtlasData = this._buildTextureAtlasData(null, null)
			if (dataParser.parseTextureAtlasData(null, textureAtlasData, scale)) {
				this.addTextureAtlasData(textureAtlasData, name)
			}
			else {
				textureAtlasData.returnToPool()
				break
			}
		}

		if (dragonBonesData != null) {
			this.addDragonBonesData(dragonBonesData, name)
		}

		return dragonBonesData
		*/
		return parseDragonBonesData(json, name, scale)
	}

	/**
	 * - Parse the raw texture atlas data and the texture atlas object to a TextureAtlasData instance and cache it to the factory.
	 * @param rawData - The raw texture atlas data.
	 * @param textureAtlas - The texture atlas object.
	 * @param name - Specify a cache name for the instance so that the instance can be obtained through this name. (If not set, use the instance name instead)
	 * @param scale - Specify a scaling value for the map set. (Default: 1.0)
	 * @returns TextureAtlasData instance
	 * @see #getTextureAtlasData()
	 * @see #addTextureAtlasData()
	 * @see #removeTextureAtlasData()
	 * @see dragonBones.TextureAtlasData
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 将原始贴图集数据和贴图集对象解析为 TextureAtlasData 实例，并缓存到工厂中。
	 * @param rawData - 原始贴图集数据。
	 * @param textureAtlas - 贴图集对象。
	 * @param name - 为该实例指定一个缓存名称，以便可以通过此名称获取该实例。 （如果未设置，则使用该实例中的名称）
	 * @param scale - 为贴图集指定一个缩放值。 （默认: 1.0）
	 * @returns TextureAtlasData 实例
	 * @see #getTextureAtlasData()
	 * @see #addTextureAtlasData()
	 * @see #removeTextureAtlasData()
	 * @see dragonBones.TextureAtlasData
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun parseTextureAtlasData(rawData: Any, textureAtlas: Any, name: String? = null, scale: Double = 1.0): TextureAtlasData {
		val textureAtlasData = this._buildTextureAtlasData(null, null)
		this._dataParser.parseTextureAtlasData(rawData, textureAtlasData, scale)
		this._buildTextureAtlasData(textureAtlasData, textureAtlas)
		this.addTextureAtlasData(textureAtlasData, name)

		return textureAtlasData
	}
	/**
	 * - Update texture atlases.
	 * @param textureAtlases - The texture atlas objects.
	 * @param name - The texture atlas name.
	 * @version DragonBones 5.7
	 * @language en_US
	 */
	/**
	 * - 更新贴图集对象。
	 * @param textureAtlases - 多个贴图集对象。
	 * @param name - 贴图集名称。
	 * @version DragonBones 5.7
	 * @language zh_CN
	 */
	fun updateTextureAtlases(textureAtlases: Array<Any>, name: String) {
		val textureAtlasDatas = this.getTextureAtlasData(name)
		if (textureAtlasDatas != null) {
			for (i in 0 until textureAtlasDatas.size) {
				if (i < textureAtlases.size) {
					this._buildTextureAtlasData(textureAtlasDatas[i], textureAtlases[i])
				}
			}
		}
	}
	/**
	 * - Get a specific DragonBonesData instance.
	 * @param name - The DragonBonesData instance cache name.
	 * @returns DragonBonesData instance
	 * @see #parseDragonBonesData()
	 * @see #addDragonBonesData()
	 * @see #removeDragonBonesData()
	 * @see dragonBones.DragonBonesData
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定的 DragonBonesData 实例。
	 * @param name - DragonBonesData 实例的缓存名称。
	 * @returns DragonBonesData 实例
	 * @see #parseDragonBonesData()
	 * @see #addDragonBonesData()
	 * @see #removeDragonBonesData()
	 * @see dragonBones.DragonBonesData
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getDragonBonesData(name: String): DragonBonesData? = this._dragonBonesDataMap[name]
	/**
	 * - Cache a DragonBonesData instance to the factory.
	 * @param data - The DragonBonesData instance.
	 * @param name - Specify a cache name for the instance so that the instance can be obtained through this name. (if not set, use the instance name instead)
	 * @see #parseDragonBonesData()
	 * @see #getDragonBonesData()
	 * @see #removeDragonBonesData()
	 * @see dragonBones.DragonBonesData
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 将 DragonBonesData 实例缓存到工厂中。
	 * @param data - DragonBonesData 实例。
	 * @param name - 为该实例指定一个缓存名称，以便可以通过此名称获取该实例。 （如果未设置，则使用该实例中的名称）
	 * @see #parseDragonBonesData()
	 * @see #getDragonBonesData()
	 * @see #removeDragonBonesData()
	 * @see dragonBones.DragonBonesData
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun addDragonBonesData(data: DragonBonesData, name: String? = null) {
		val rname = name ?: data.name
		if (rname in this._dragonBonesDataMap) {
			if (this._dragonBonesDataMap[rname] == data) return
			Console.warn("Can not add same name data: $rname")
			return
		}

		this._dragonBonesDataMap[rname] = data
	}
	/**
	 * - Remove a DragonBonesData instance.
	 * @param name - The DragonBonesData instance cache name.
	 * @param disposeData - Whether to dispose data. (Default: true)
	 * @see #parseDragonBonesData()
	 * @see #getDragonBonesData()
	 * @see #addDragonBonesData()
	 * @see dragonBones.DragonBonesData
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 移除 DragonBonesData 实例。
	 * @param name - DragonBonesData 实例缓存名称。
	 * @param disposeData - 是否释放数据。 （默认: true）
	 * @see #parseDragonBonesData()
	 * @see #getDragonBonesData()
	 * @see #addDragonBonesData()
	 * @see dragonBones.DragonBonesData
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun removeDragonBonesData(name: String, disposeData: Boolean = true) {
		if (name in this._dragonBonesDataMap) {
			if (disposeData) {
				//this._dragonBones.bufferObject(this._dragonBonesDataMap[name])
				this._dragonBonesDataMap[name]?.returnToPool()
			}

			this._dragonBonesDataMap.remove(name)
		}
	}
	/**
	 * - Get a list of specific TextureAtlasData instances.
	 * @param name - The TextureAtlasData cahce name.
	 * @see #parseTextureAtlasData()
	 * @see #addTextureAtlasData()
	 * @see #removeTextureAtlasData()
	 * @see dragonBones.TextureAtlasData
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定的 TextureAtlasData 实例列表。
	 * @param name - TextureAtlasData 实例缓存名称。
	 * @see #parseTextureAtlasData()
	 * @see #addTextureAtlasData()
	 * @see #removeTextureAtlasData()
	 * @see dragonBones.TextureAtlasData
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun getTextureAtlasData(name: String): FastArrayList<TextureAtlasData>? = this._textureAtlasDataMap[name]
	/**
	 * - Cache a TextureAtlasData instance to the factory.
	 * @param data - The TextureAtlasData instance.
	 * @param name - Specify a cache name for the instance so that the instance can be obtained through this name. (if not set, use the instance name instead)
	 * @see #parseTextureAtlasData()
	 * @see #getTextureAtlasData()
	 * @see #removeTextureAtlasData()
	 * @see dragonBones.TextureAtlasData
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 将 TextureAtlasData 实例缓存到工厂中。
	 * @param data - TextureAtlasData 实例。
	 * @param name - 为该实例指定一个缓存名称，以便可以通过此名称获取该实例。 （如果未设置，则使用该实例中的名称）
	 * @see #parseTextureAtlasData()
	 * @see #getTextureAtlasData()
	 * @see #removeTextureAtlasData()
	 * @see dragonBones.TextureAtlasData
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun addTextureAtlasData(data: TextureAtlasData, name: String? = null) {
		val rname = name ?: data.name
		val textureAtlasList = this._textureAtlasDataMap.getOrPut(rname) { FastArrayList() }
		if (textureAtlasList.indexOf(data) < 0) {
            textureAtlasList.add(data)
		}
	}
	/**
	 * - Remove a TextureAtlasData instance.
	 * @param name - The TextureAtlasData instance cache name.
	 * @param disposeData - Whether to dispose data.
	 * @see #parseTextureAtlasData()
	 * @see #getTextureAtlasData()
	 * @see #addTextureAtlasData()
	 * @see dragonBones.TextureAtlasData
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 移除 TextureAtlasData 实例。
	 * @param name - TextureAtlasData 实例的缓存名称。
	 * @param disposeData - 是否释放数据。
	 * @see #parseTextureAtlasData()
	 * @see #getTextureAtlasData()
	 * @see #addTextureAtlasData()
	 * @see dragonBones.TextureAtlasData
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun removeTextureAtlasData(name: String, disposeData: Boolean = true) {
		if (name in this._textureAtlasDataMap) {
			val textureAtlasDataList = this._textureAtlasDataMap[name]!!
			if (disposeData) {
				textureAtlasDataList.fastForEach { textureAtlasData ->
					//this._dragonBones.bufferObject(textureAtlasData)
					textureAtlasData.returnToPool()
				}
			}

			this._textureAtlasDataMap.remove(name)
		}
	}
	/**
	 * - Get a specific armature data.
	 * @param name - The armature data name.
	 * @param dragonBonesName - The cached name for DragonbonesData instance.
	 * @see dragonBones.ArmatureData
	 * @version DragonBones 5.1
	 * @language en_US
	 */
	/**
	 * - 获取特定的骨架数据。
	 * @param name - 骨架数据名称。
	 * @param dragonBonesName - DragonBonesData 实例的缓存名称。
	 * @see dragonBones.ArmatureData
	 * @version DragonBones 5.1
	 * @language zh_CN
	 */
	fun getArmatureData(name: String, dragonBonesName: String = ""): ArmatureData? {
		val dataPackage = BuildArmaturePackage()
		if (!this._fillBuildArmaturePackage(dataPackage, dragonBonesName, name, "", "")) {
			return null
		}

		return dataPackage.armature
	}
	/**
	 * - Clear all cached DragonBonesData instances and TextureAtlasData instances.
	 * @param disposeData - Whether to dispose data.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 清除缓存的所有 DragonBonesData 实例和 TextureAtlasData 实例。
	 * @param disposeData - 是否释放数据。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun clear(disposeData: Boolean = true) {
		this._dragonBonesDataMap.fastKeyForEach { k ->
			if (disposeData) {
				//this._dragonBones.bufferObject(this._dragonBonesDataMap[k])
				this._dragonBonesDataMap[k]?.returnToPool()
			}
		}
		this._dragonBonesDataMap.clear()

		this._textureAtlasDataMap.fastKeyForEach { k ->
			if (disposeData) {
				val textureAtlasDataList = this._textureAtlasDataMap[k]!!
				textureAtlasDataList.fastForEach { textureAtlasData ->
					//this._dragonBones.bufferObject(textureAtlasData)
					textureAtlasData.returnToPool()
				}
			}
		}
		this._textureAtlasDataMap.clear()
	}
	/**
	 * - Create a armature from cached DragonBonesData instances and TextureAtlasData instances.
	 * Note that when the created armature that is no longer in use, you need to explicitly dispose {@link #dragonBones.Armature#dispose()}.
	 * @param armatureName - The armature data name.
	 * @param dragonBonesName - The cached name of the DragonBonesData instance. (If not set, all DragonBonesData instances are retrieved, and when multiple DragonBonesData instances contain a the same name armature data, it may not be possible to accurately create a specific armature)
	 * @param skinName - The skin name, you can set a different ArmatureData name to share it's skin data. (If not set, use the default skin data)
	 * @returns The armature.
	 * @example
	 * <pre>
	 *     var armature = factory.buildArmature("armatureName", "dragonBonesName");
	 *     armature.clock = factory.clock;
	 * </pre>
	 * @see dragonBones.DragonBonesData
	 * @see dragonBones.ArmatureData
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 通过缓存的 DragonBonesData 实例和 TextureAtlasData 实例创建一个骨架。
	 * 注意，创建的骨架不再使用时，需要显式释放 {@link #dragonBones.Armature#dispose()}。
	 * @param armatureName - 骨架数据名称。
	 * @param dragonBonesName - DragonBonesData 实例的缓存名称。 （如果未设置，将检索所有的 DragonBonesData 实例，当多个 DragonBonesData 实例中包含同名的骨架数据时，可能无法准确的创建出特定的骨架）
	 * @param skinName - 皮肤名称，可以设置一个其他骨架数据名称来共享其皮肤数据。（如果未设置，则使用默认的皮肤数据）
	 * @returns 骨架。
	 * @example
	 * <pre>
	 *     var armature = factory.buildArmature("armatureName", "dragonBonesName");
	 *     armature.clock = factory.clock;
	 * </pre>
	 * @see dragonBones.DragonBonesData
	 * @see dragonBones.ArmatureData
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun buildArmature(armatureName: String, dragonBonesName: String = "", skinName: String = "", textureAtlasName: String = ""): Armature? {
		val dataPackage = BuildArmaturePackage()
		if (!this._fillBuildArmaturePackage(dataPackage, dragonBonesName, armatureName, skinName,
				textureAtlasName
			)) {
			Console.warn("No armature data: $armatureName, $dragonBonesName")
			return null
		}

		val armature = this._buildArmature(dataPackage)
		this._buildBones(dataPackage, armature)
		this._buildSlots(dataPackage, armature)
		this._buildConstraints(dataPackage, armature)
		armature.invalidUpdate(null, true)
		armature.advanceTime(0.0) // Update armature pose.

		return armature
	}

	/**
	 * @private
	 */
	fun replaceDisplay(slot: Slot, displayData: DisplayData?, displayIndex: Int = -1) {
		var mdisplayIndex = displayIndex
		if (mdisplayIndex < 0) {
			mdisplayIndex = slot.displayIndex
		}

		if (mdisplayIndex < 0) {
			mdisplayIndex = 0
		}

		slot.replaceDisplayData(displayData, mdisplayIndex)

		if (displayData != null) {
			var display = this._getSlotDisplay(null, displayData, slot)
			if (displayData.type == DisplayType.Image) {
				val rawDisplayData = slot.getDisplayFrameAt(mdisplayIndex).rawDisplayData
				if (
					rawDisplayData != null &&
					rawDisplayData.type == DisplayType.Mesh
				) {
					display = slot.meshDisplay
				}
			}

			slot.replaceDisplay(display, mdisplayIndex)
		}
		else {
			slot.replaceDisplay(null, mdisplayIndex)
		}
	}
	/**
	 * - Replaces the current display data for a particular slot with a specific display data.
	 * Specify display data with "dragonBonesName/armatureName/slotName/displayName".
	 * @param dragonBonesName - The DragonBonesData instance cache name.
	 * @param armatureName - The armature data name.
	 * @param slotName - The slot data name.
	 * @param displayName - The display data name.
	 * @param slot - The slot.
	 * @param displayIndex - The index of the display data that is replaced. (If it is not set, replaces the current display data)
	 * @example
	 * <pre>
	 *     var slot = armature.getSlot("weapon");
	 *     factory.replaceSlotDisplay("dragonBonesName", "armatureName", "slotName", "displayName", slot);
	 * </pre>
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 用特定的显示对象数据替换特定插槽当前的显示对象数据。
	 * 用 "dragonBonesName/armatureName/slotName/displayName" 指定显示对象数据。
	 * @param dragonBonesName - DragonBonesData 实例的缓存名称。
	 * @param armatureName - 骨架数据名称。
	 * @param slotName - 插槽数据名称。
	 * @param displayName - 显示对象数据名称。
	 * @param slot - 插槽。
	 * @param displayIndex - 被替换的显示对象数据的索引。 （如果未设置，则替换当前的显示对象数据）
	 * @example
	 * <pre>
	 *     var slot = armature.getSlot("weapon");
	 *     factory.replaceSlotDisplay("dragonBonesName", "armatureName", "slotName", "displayName", slot);
	 * </pre>
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun replaceSlotDisplay(
		dragonBonesName: String, armatureName: String, slotName: String, displayName: String,
		slot: Slot, displayIndex: Int = -1
	): Boolean {
		val armatureData = this.getArmatureData(armatureName, dragonBonesName)
		if (armatureData?.defaultSkin == null) {
			return false
		}

		val displayData = armatureData.defaultSkin!!.getDisplay(slotName, displayName)
		this.replaceDisplay(slot, displayData, displayIndex)

		return true
	}

	/**
	 * @private
	 */
	fun replaceSlotDisplayList(
		dragonBonesName: String?, armatureName: String, slotName: String,
		slot: Slot
	): Boolean {
		val armatureData = this.getArmatureData(armatureName, dragonBonesName ?: "")
		if (armatureData?.defaultSkin == null) {
			return false
		}

		val displayDatas = armatureData.defaultSkin!!.getDisplays(slotName) ?: return false

		slot.displayFrameCount = displayDatas.size
		for (i in 0 until slot.displayFrameCount) {
			val displayData = displayDatas[i]!!
			this.replaceDisplay(slot, displayData, i)
		}

		return true
	}
	/**
	 * - Share specific skin data with specific armature.
	 * @param armature - The armature.
	 * @param skin - The skin data.
	 * @param isOverride - Whether it completely override the original skin. (Default: false)
	 * @param exclude - A list of slot names that do not need to be replace.
	 * @example
	 * <pre>
	 *     var armatureA = factory.buildArmature("armatureA", "dragonBonesA");
	 *     var armatureDataB = factory.getArmatureData("armatureB", "dragonBonesB");
	 *     if (armatureDataB && armatureDataB.defaultSkin) {
	 *     factory.replaceSkin(armatureA, armatureDataB.defaultSkin, false, ["arm_l", "weapon_l"]);
	 *     }
	 * </pre>
	 * @see dragonBones.Armature
	 * @see dragonBones.SkinData
	 * @version DragonBones 5.6
	 * @language en_US
	 */
	/**
	 * - 将特定的皮肤数据共享给特定的骨架使用。
	 * @param armature - 骨架。
	 * @param skin - 皮肤数据。
	 * @param isOverride - 是否完全覆盖原来的皮肤。 （默认: false）
	 * @param exclude - 不需要被替换的插槽名称列表。
	 * @example
	 * <pre>
	 *     var armatureA = factory.buildArmature("armatureA", "dragonBonesA");
	 *     var armatureDataB = factory.getArmatureData("armatureB", "dragonBonesB");
	 *     if (armatureDataB && armatureDataB.defaultSkin) {
	 *     factory.replaceSkin(armatureA, armatureDataB.defaultSkin, false, ["arm_l", "weapon_l"]);
	 *     }
	 * </pre>
	 * @see dragonBones.Armature
	 * @see dragonBones.SkinData
	 * @version DragonBones 5.6
	 * @language zh_CN
	 */
	fun replaceSkin(armature: Armature, skin: SkinData, isOverride: Boolean = false, exclude: Array<String>? = null): Boolean {
		var success = false
		val defaultSkin = skin.parent!!.defaultSkin

		armature.getSlots().fastForEach { slot ->
			if (exclude != null && exclude.indexOf(slot.name) >= 0) {
				return@fastForEach
			}

			var displayDatas = skin.getDisplays(slot.name)
			if (displayDatas == null) {
				if (defaultSkin != null && skin != defaultSkin) {
					displayDatas = defaultSkin.getDisplays(slot.name)
				}

				if (displayDatas == null) {
					if (isOverride) {
						slot.displayFrameCount = 0
					}
					return@fastForEach
				}
			}

			slot.displayFrameCount = displayDatas.lengthSet
			//for (var i = 0, l = slot.displayFrameCount; i < l; ++i) {
			for (i  in 0 until slot.displayFrameCount) {
				val displayData = displayDatas[i]
				slot.replaceRawDisplayData(displayData, i)

				if (displayData != null) {
					slot.replaceDisplay(this._getSlotDisplay(null, displayData, slot), i)
				} else {
					slot.replaceDisplay(null, i)
				}
			}

			success = true
		}

		return success
	}
	/**
	 * - Replaces the existing animation data for a specific armature with the animation data for the specific armature data.
	 * This enables you to make a armature template so that other armature without animations can share it's animations.
	 * @param armature - The armtaure.
	 * @param armatureData - The armature data.
	 * @param isOverride - Whether to completely overwrite the original animation. (Default: false)
	 * @example
	 * <pre>
	 *     var armatureA = factory.buildArmature("armatureA", "dragonBonesA");
	 *     var armatureDataB = factory.getArmatureData("armatureB", "dragonBonesB");
	 *     if (armatureDataB) {
	 *     factory.replaceAnimation(armatureA, armatureDataB);
	 *     }
	 * </pre>
	 * @see dragonBones.Armature
	 * @see dragonBones.ArmatureData
	 * @version DragonBones 5.6
	 * @language en_US
	 */
	/**
	 * - 用特定骨架数据的动画数据替换特定骨架现有的动画数据。
	 * 这样就能实现制作一个骨架动画模板，让其他没有制作动画的骨架共享该动画。
	 * @param armature - 骨架。
	 * @param armatureData - 骨架数据。
	 * @param isOverride - 是否完全覆盖原来的动画。（默认: false）
	 * @example
	 * <pre>
	 *     var armatureA = factory.buildArmature("armatureA", "dragonBonesA");
	 *     var armatureDataB = factory.getArmatureData("armatureB", "dragonBonesB");
	 *     if (armatureDataB) {
	 *     factory.replaceAnimation(armatureA, armatureDataB);
	 *     }
	 * </pre>
	 * @see dragonBones.Armature
	 * @see dragonBones.ArmatureData
	 * @version DragonBones 5.6
	 * @language zh_CN
	 */
	fun replaceAnimation(armature: Armature, armatureData: ArmatureData, isOverride: Boolean = true): Boolean {
		val skinData = armatureData.defaultSkin ?: return false

		if (isOverride) {
			armature.animation.animations = armatureData.animations
		}
		else {
			val rawAnimations = armature.animation.animations
			val animations: FastStringMap<AnimationData> = FastStringMap()

			rawAnimations.fastKeyForEach { k ->
				animations[k] = rawAnimations[k]!!
			}

			armatureData.animations.fastKeyForEach { k ->
				animations[k] = armatureData.animations[k]!!
			}

			armature.animation.animations = animations
		}

		armature.getSlots().fastForEach { slot ->
			for ((index, display) in slot.displayList.withIndex()) {
				if (display is Armature) {
					val displayDatas = skinData.getDisplays(slot.name)
					if (displayDatas != null && index < displayDatas.lengthSet) {
						val displayData = displayDatas[index]
						if (displayData != null && displayData.type == DisplayType.Armature) {
							val childArmatureData = this.getArmatureData(displayData.path, displayData.parent.parent!!.parent!!.name)
							if (childArmatureData != null) {
								this.replaceAnimation(display, childArmatureData, isOverride)
							}
						}
					}
				}
			}
		}

		return true
	}

	/**
	 * @private
	 */
	fun getAllDragonBonesData(): FastStringMap<DragonBonesData> {
		return this._dragonBonesDataMap
	}

	/**
	 * @private
	 */
	fun getAllTextureAtlasData(): FastStringMap<FastArrayList<TextureAtlasData>> {
		return this._textureAtlasDataMap
	}
	/**
	 * - An Worldclock instance updated by engine.
	 * @version DragonBones 5.7
	 * @language en_US
	 */
	/**
	 * - 由引擎驱动的 WorldClock 实例。
	 * @version DragonBones 5.7
	 * @language zh_CN
	 */
	val clock: WorldClock
		get() {
		return this._dragonBones.clock
	}
	/**
	 * @private
	 */
	val dragonBones: DragonBones get() {
		return this._dragonBones
	}
}
/**
 * @private
 */
class BuildArmaturePackage {
	var dataName: String = ""
	var textureAtlasName: String = ""
	var data: DragonBonesData? = null
	var armature: ArmatureData? = null
	var skin: SkinData? = null
}
