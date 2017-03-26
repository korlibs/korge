package com.codeazur.as3swf.tags

import com.codeazur.as3swf.SWFData
import com.codeazur.as3swf.exporters.ShapeExporter
import com.codeazur.as3swf.utils.ColorUtils
import com.codeazur.as3swf.utils.FlashByteArray


interface ITag {
	val type: Int
	val name: String
	val version: Int
	val level: Int

	suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean = false)
	suspend fun publish(data: SWFData, version: Int)
	fun toString(indent: Int = 0, flags: Int = 0): String
}

abstract class _BaseTag : ITag {
	override abstract fun toString(indent: Int, flags: Int): String
	override fun toString() = toString(0, 0)
}

interface IDefinitionTag : ITag {
	var characterId: Int
	fun clone(): com.codeazur.as3swf.tags.IDefinitionTag
}

interface IDisplayListTag : ITag


class Tag {
	companion object {
		fun toStringCommon(type: Int, name: String, indent: Int = 0): String {
			return " ".repeat(indent) + "[" + "%02d".format(type) + ":" + name + "] "
		}
	}
}

class TagCSMTextSettings : _BaseTag() {
	companion object {
		const val TYPE = 74
	}

	var textId: Int = 0
	var useFlashType: Int = 0
	var gridFit: Int = 0
	var thickness = 0.0
	var sharpness = 0.0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		textId = data.readUI16()
		useFlashType = data.readUB(2)
		gridFit = data.readUB(3)
		data.readUB(3) // reserved, always 0
		thickness = data.readFIXED()
		sharpness = data.readFIXED()
		data.readUI8() // reserved, always 0
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, 12)
		data.writeUI16(textId)
		data.writeUB(2, useFlashType)
		data.writeUB(3, gridFit)
		data.writeUB(3, 0) // reserved, always 0
		data.writeFIXED(thickness)
		data.writeFIXED(sharpness)
		data.writeUI8(0) // reserved, always 0
	}

	override val type = com.codeazur.as3swf.tags.TagCSMTextSettings.Companion.TYPE
	override val name = "CSMTextSettings"
	override val version = 8
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"TextID: " + textId + ", " +
			"UseFlashType: " + useFlashType + ", " +
			"GridFit: " + gridFit + ", " +
			"Thickness: " + thickness + ", " +
			"Sharpness: " + sharpness
	}
}

class TagDebugID : _BaseTag() {
	companion object {
		const val TYPE = 63
	}

	protected var uuid = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		if (length > 0) {
			data.readBytes(uuid, 0, length)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, uuid.length)
		if (uuid.length > 0) {
			data.writeBytes(uuid)
		}
	}

	override val type = com.codeazur.as3swf.tags.TagDebugID.Companion.TYPE
	override val name = "DebugID"
	override val version = 6
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) + "UUID: "
		if (uuid.length == 16) {
			str += "%02x%02x%02x%02x-".format(uuid[0], uuid[1], uuid[2], uuid[3])
			str += "%02x%02x-".format(uuid[4], uuid[5])
			str += "%02x%02x-".format(uuid[6], uuid[7])
			str += "%02x%02x-".format(uuid[8], uuid[9])
			str += "%02x%02x%02x%02x%02x%02x".format(uuid[10], uuid[11], uuid[12], uuid[13], uuid[14], uuid[15])
		} else {
			str += "(invalid length: " + uuid.length + ")"
		}
		return str
	}
}

class TagDefineBinaryData : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 87
	}

	override var characterId: Int = 0

	var binaryData = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		data.readUI32() // reserved, always 0
		if (length > 6) {
			data.readBytes(binaryData, 0, length - 6)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeUI32(0) // reserved, always 0
		if (binaryData.length > 0) {
			body.writeBytes(binaryData)
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineBinaryData = com.codeazur.as3swf.tags.TagDefineBinaryData()
		tag.characterId = characterId
		if (binaryData.length > 0) {
			tag.binaryData.writeBytes(binaryData)
		}
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineBinaryData.Companion.TYPE
	override val name = "DefineBinaryData"
	override val version = 9
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Length: " + binaryData.length
	}
}

open class TagDefineBits : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 6
	}

	var bitmapType: Int = com.codeazur.as3swf.data.consts.BitmapType.JPEG

	override var characterId: Int = 0

	protected var bitmapData = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		if (length > 2) {
			data.readBytes(bitmapData, 0, length - 2)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, bitmapData.length + 2, true)
		data.writeUI16(characterId)
		if (bitmapData.length > 0) {
			data.writeBytes(bitmapData)
		}
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineBits = com.codeazur.as3swf.tags.TagDefineBits()
		tag.characterId = characterId
		tag.bitmapType = bitmapType
		if (bitmapData.length > 0) {
			tag.bitmapData.writeBytes(bitmapData)
		}
		return tag
	}

	/*
	protected var loader: Loader;
	protected var onCompleteCallback: Function;

	fun exportBitmapData(onComplete: Function): Unit {
		onCompleteCallback = onComplete;
		loader = Loader();
		loader.contentLoaderInfo.addEventListener(Event.COMPLETE, exportCompleteHandler);
		loader.loadBytes(bitmapData);
	}

	protected fun exportCompleteHandler(event: Event): Unit {
		var loader: Loader = event.target.loader as Loader;
		var bitmapData: BitmapData = BitmapData(loader.content.width, loader.content.height);
		bitmapData.draw(loader);
		onCompleteCallback(bitmapData);
	}
	*/

	override val type = com.codeazur.as3swf.tags.TagDefineBits.Companion.TYPE
	override val name = "DefineBits"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"BitmapLength: " + bitmapData.length
	}
}

open class TagDefineBitsJPEG2 : com.codeazur.as3swf.tags.TagDefineBits(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 21
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		super.parse(data, length, version, async)
		if (bitmapData[0] == 0xff && (bitmapData[1] == 0xd8 || bitmapData[1] == 0xd9)) {
			bitmapType = com.codeazur.as3swf.data.consts.BitmapType.JPEG
		} else if (bitmapData[0] == 0x89 && bitmapData[1] == 0x50 && bitmapData[2] == 0x4e && bitmapData[3] == 0x47 && bitmapData[4] == 0x0d && bitmapData[5] == 0x0a && bitmapData[6] == 0x1a && bitmapData[7] == 0x0a) {
			bitmapType = com.codeazur.as3swf.data.consts.BitmapType.PNG
		} else if (bitmapData[0] == 0x47 && bitmapData[1] == 0x49 && bitmapData[2] == 0x46 && bitmapData[3] == 0x38 && bitmapData[4] == 0x39 && bitmapData[5] == 0x61) {
			bitmapType = com.codeazur.as3swf.data.consts.BitmapType.GIF89A
		}
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineBitsJPEG2 = com.codeazur.as3swf.tags.TagDefineBitsJPEG2()
		tag.characterId = characterId
		tag.bitmapType = bitmapType
		if (bitmapData.length > 0) {
			tag.bitmapData.writeBytes(bitmapData)
		}
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineBitsJPEG2.Companion.TYPE
	override val name = "DefineBitsJPEG2"
	override val version = if (bitmapType == com.codeazur.as3swf.data.consts.BitmapType.JPEG) 2 else 8
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		val str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Type: " + com.codeazur.as3swf.data.consts.BitmapType.toString(bitmapType) + ", " +
			"BitmapLength: " + bitmapData.length
		return str
	}
}

open class TagDefineBitsJPEG3 : com.codeazur.as3swf.tags.TagDefineBitsJPEG2(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 35
	}

	protected var bitmapAlphaData = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		val alphaDataOffset: Int = data.readUI32()
		data.readBytes(bitmapData, 0, alphaDataOffset)
		if (bitmapData[0] == 0xff && (bitmapData[1] == 0xd8 || bitmapData[1] == 0xd9)) {
			bitmapType = com.codeazur.as3swf.data.consts.BitmapType.JPEG
		} else if (bitmapData[0] == 0x89 && bitmapData[1] == 0x50 && bitmapData[2] == 0x4e && bitmapData[3] == 0x47 && bitmapData[4] == 0x0d && bitmapData[5] == 0x0a && bitmapData[6] == 0x1a && bitmapData[7] == 0x0a) {
			bitmapType = com.codeazur.as3swf.data.consts.BitmapType.PNG
		} else if (bitmapData[0] == 0x47 && bitmapData[1] == 0x49 && bitmapData[2] == 0x46 && bitmapData[3] == 0x38 && bitmapData[4] == 0x39 && bitmapData[5] == 0x61) {
			bitmapType = com.codeazur.as3swf.data.consts.BitmapType.GIF89A
		}
		val alphaDataSize: Int = length - alphaDataOffset - 6
		if (alphaDataSize > 0) {
			data.readBytes(bitmapAlphaData, 0, alphaDataSize)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, bitmapData.length + bitmapAlphaData.length + 6, true)
		data.writeUI16(characterId)
		data.writeUI32(bitmapData.length)
		if (bitmapData.length > 0) {
			data.writeBytes(bitmapData)
		}
		if (bitmapAlphaData.length > 0) {
			data.writeBytes(bitmapAlphaData)
		}
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineBitsJPEG3 = com.codeazur.as3swf.tags.TagDefineBitsJPEG3()
		tag.characterId = characterId
		tag.bitmapType = bitmapType
		if (bitmapData.length > 0) {
			tag.bitmapData.writeBytes(bitmapData)
		}
		if (bitmapAlphaData.length > 0) {
			tag.bitmapAlphaData.writeBytes(bitmapAlphaData)
		}
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineBitsJPEG3.Companion.TYPE
	override val name = "DefineBitsJPEG3"
	override val version = if (bitmapType == com.codeazur.as3swf.data.consts.BitmapType.JPEG) 3 else 8
	override val level = 3

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Type: " + com.codeazur.as3swf.data.consts.BitmapType.toString(bitmapType) + ", " +
			"HasAlphaData: " + (bitmapAlphaData.length > 0) + ", " +
			(if (bitmapAlphaData.length > 0) "BitmapAlphaLength: " + bitmapAlphaData.length + ", " else "") +
			"BitmapLength: " + bitmapData.length
		return str
	}
}

class TagDefineBitsJPEG4 : com.codeazur.as3swf.tags.TagDefineBitsJPEG3(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 90
	}

	var deblockParam: Double = 0.0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		val alphaDataOffset: Int = data.readUI32()
		deblockParam = data.readFIXED8()
		data.readBytes(bitmapData, 0, alphaDataOffset)
		if (bitmapData[0] == 0xff && (bitmapData[1] == 0xd8 || bitmapData[1] == 0xd9)) {
			bitmapType = com.codeazur.as3swf.data.consts.BitmapType.JPEG
		} else if (bitmapData[0] == 0x89 && bitmapData[1] == 0x50 && bitmapData[2] == 0x4e && bitmapData[3] == 0x47 && bitmapData[4] == 0x0d && bitmapData[5] == 0x0a && bitmapData[6] == 0x1a && bitmapData[7] == 0x0a) {
			bitmapType = com.codeazur.as3swf.data.consts.BitmapType.PNG
		} else if (bitmapData[0] == 0x47 && bitmapData[1] == 0x49 && bitmapData[2] == 0x46 && bitmapData[3] == 0x38 && bitmapData[4] == 0x39 && bitmapData[5] == 0x61) {
			bitmapType = com.codeazur.as3swf.data.consts.BitmapType.GIF89A
		}
		val alphaDataSize: Int = length - alphaDataOffset - 6
		if (alphaDataSize > 0) {
			data.readBytes(bitmapAlphaData, 0, alphaDataSize)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, bitmapData.length + bitmapAlphaData.length + 6, true)
		data.writeUI16(characterId)
		data.writeUI32(bitmapData.length)
		data.writeFIXED8(deblockParam)
		if (bitmapData.length > 0) {
			data.writeBytes(bitmapData)
		}
		if (bitmapAlphaData.length > 0) {
			data.writeBytes(bitmapAlphaData)
		}
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineBitsJPEG4 = com.codeazur.as3swf.tags.TagDefineBitsJPEG4()
		tag.characterId = characterId
		tag.bitmapType = bitmapType
		tag.deblockParam = deblockParam
		if (bitmapData.length > 0) {
			tag.bitmapData.writeBytes(bitmapData)
		}
		if (bitmapAlphaData.length > 0) {
			tag.bitmapAlphaData.writeBytes(bitmapAlphaData)
		}
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineBitsJPEG4.Companion.TYPE
	override val name = "DefineBitsJPEG4"
	override val version = 10
	override val level = 4

	override fun toString(indent: Int, flags: Int): String {
		val str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Type: " + com.codeazur.as3swf.data.consts.BitmapType.toString(bitmapType) + ", " +
			"DeblockParam: " + deblockParam + ", " +
			"HasAlphaData: " + (bitmapAlphaData.length > 0) + ", " +
			(if (bitmapAlphaData.length > 0) "BitmapAlphaLength: " + bitmapAlphaData.length + ", " else "") +
			"BitmapLength: " + bitmapData.length
		return str
	}
}

open class TagDefineBitsLossless : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 20
	}

	var bitmapFormat: Int = 0
	var bitmapWidth: Int = 0
	var bitmapHeight: Int = 0
	var bitmapColorTableSize: Int = 0

	override var characterId: Int = 0

	protected var zlibBitmapData = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		bitmapFormat = data.readUI8()
		bitmapWidth = data.readUI16()
		bitmapHeight = data.readUI16()
		if (bitmapFormat == com.codeazur.as3swf.data.consts.BitmapFormat.BIT_8) {
			bitmapColorTableSize = data.readUI8()
		}
		data.readBytes(zlibBitmapData, 0, length - (if (bitmapFormat == com.codeazur.as3swf.data.consts.BitmapFormat.BIT_8) 8 else 7))
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeUI8(bitmapFormat)
		body.writeUI16(bitmapWidth)
		body.writeUI16(bitmapHeight)
		if (bitmapFormat == com.codeazur.as3swf.data.consts.BitmapFormat.BIT_8) {
			body.writeUI8(bitmapColorTableSize)
		}
		if (zlibBitmapData.length > 0) {
			body.writeBytes(zlibBitmapData)
		}
		data.writeTagHeader(type, body.length, true)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineBitsLossless = com.codeazur.as3swf.tags.TagDefineBitsLossless()
		tag.characterId = characterId
		tag.bitmapFormat = bitmapFormat
		tag.bitmapWidth = bitmapWidth
		tag.bitmapHeight = bitmapHeight
		if (zlibBitmapData.length > 0) {
			tag.zlibBitmapData.writeBytes(zlibBitmapData)
		}
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineBitsLossless.Companion.TYPE
	override val name = "DefineBitsLossless"
	override val version = 2
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Format: " + com.codeazur.as3swf.data.consts.BitmapFormat.toString(bitmapFormat) + ", " +
			"Size: (" + bitmapWidth + "," + bitmapHeight + ")"
	}
}

class TagDefineBitsLossless2 : com.codeazur.as3swf.tags.TagDefineBitsLossless(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 36
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineBitsLossless2 = com.codeazur.as3swf.tags.TagDefineBitsLossless2()
		tag.characterId = characterId
		tag.bitmapFormat = bitmapFormat
		tag.bitmapWidth = bitmapWidth
		tag.bitmapHeight = bitmapHeight
		if (zlibBitmapData.length > 0) {
			tag.zlibBitmapData.writeBytes(zlibBitmapData)
		}
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineBitsLossless2.Companion.TYPE
	override val name = "DefineBitsLossless2"
	override val version = 3
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Format: " + com.codeazur.as3swf.data.consts.BitmapFormat.toString(bitmapFormat) + ", " +
			"Size: (" + bitmapWidth + "," + bitmapHeight + ")"
	}
}

class TagDefineButton : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 7
		val STATE_UP = "up"
		val STATE_OVER = "over"
		val STATE_DOWN = "down"
		val STATE_HIT = "hit"
	}

	override var characterId: Int = 0

	protected var characters = java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>()
	protected var actions = java.util.ArrayList<com.codeazur.as3swf.data.actions.IAction>()

	protected var frames = hashMapOf<String, java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>>()

	protected var labelCount: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		var record: com.codeazur.as3swf.data.SWFButtonRecord?
		while (true) {
			record = data.readBUTTONRECORD()
			if (record == null) break
			characters.add(record)
		}
		var action: com.codeazur.as3swf.data.actions.IAction?
		while (true) {
			action = data.readACTIONRECORD()
			if (action == null) break
			actions.add(action)
		}
		labelCount = com.codeazur.as3swf.data.actions.Action.Companion.resolveOffsets(actions)
		processRecords()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body = SWFData()
		body.writeUI16(characterId)
		for (i in 0 until characters.size) {
			data.writeBUTTONRECORD(characters[i])
		}
		data.writeUI8(0)
		for (i in 0 until actions.size) {
			data.writeACTIONRECORD(actions[i])
		}
		data.writeUI8(0)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag = com.codeazur.as3swf.tags.TagDefineButton()
		tag.characterId = characterId
		for (i in 0 until characters.size) {
			tag.characters.add(characters[i].clone())
		}
		for (i in 0 until actions.size) {
			tag.actions.add(actions[i].clone())
		}
		return tag
	}

	fun getRecordsByState(state: String): java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord> {
		return frames[state] as java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>
	}

	override val type = com.codeazur.as3swf.tags.TagDefineButton.Companion.TYPE
	override val name = "DefineButton"
	override val version = 1
	override val level = 1

	protected fun processRecords(): Unit {
		val upState = java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>()
		val overState = java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>()
		val downState = java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>()
		val hitState = java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>()
		for (i in 0 until characters.size) {
			val record = characters[i]
			if (record.stateUp) upState.add(record)
			if (record.stateOver) overState.add(record)
			if (record.stateDown) downState.add(record)
			if (record.stateHitTest) hitState.add(record)
		}
		frames[com.codeazur.as3swf.tags.TagDefineButton.Companion.STATE_UP] = java.util.ArrayList(upState.sortedBy { it.placeDepth })
		frames[com.codeazur.as3swf.tags.TagDefineButton.Companion.STATE_OVER] = java.util.ArrayList(overState.sortedBy { it.placeDepth })
		frames[com.codeazur.as3swf.tags.TagDefineButton.Companion.STATE_DOWN] = java.util.ArrayList(downState.sortedBy { it.placeDepth })
		frames[com.codeazur.as3swf.tags.TagDefineButton.Companion.STATE_HIT] = java.util.ArrayList(hitState.sortedBy { it.placeDepth })
	}

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId
		if (characters.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Characters:"
			for (i in 0 until characters.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + characters[i].toString(indent + 4)
			}
		}
		if (actions.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Actions:"
			if ((flags and com.codeazur.as3swf.SWF.Companion.TOSTRING_FLAG_AVM1_BYTECODE) == 0) {
				for (i in 0 until actions.size) {
					str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + actions[i].toString(indent + 4)
				}
			} else {
				val context: com.codeazur.as3swf.data.actions.ActionExecutionContext = com.codeazur.as3swf.data.actions.ActionExecutionContext(actions, arrayListOf(), labelCount)
				for (i in 0 until actions.size) {
					str += "\n" + " ".repeat(indent + 4) + actions[i].toBytecode(indent + 4, context)
				}
				if (context.endLabel != null) {
					str += "\n" + " ".repeat(indent + 6) + context.endLabel + ":"
				}
			}
		}
		return str
	}
}

open class TagDefineButton2 : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 34
	}

	var trackAsMenu: Boolean = false

	override var characterId: Int = 0

	var characters = java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>()
	protected var condActions = java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonCondAction>()

	protected var frames = hashMapOf<String, java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		trackAsMenu = ((data.readUI8() and 0x01) != 0)
		val actionOffset: Int = data.readUI16()
		var record: com.codeazur.as3swf.data.SWFButtonRecord?
		while (true) {
			record = data.readBUTTONRECORD(2)
			if (record == null) break
			characters.add(record)
		}
		if (actionOffset != 0) {
			var condActionSize: Int = 0
			do {
				condActionSize = data.readUI16()
				condActions.add(data.readBUTTONCONDACTION())
			} while (condActionSize != 0)
		}
		processRecords()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeUI8(trackAsMenu)
		val hasCondActions: Boolean = (condActions.size > 0)
		val buttonRecordsBytes: SWFData = SWFData()
		for (i in 0 until characters.size) {
			buttonRecordsBytes.writeBUTTONRECORD(characters[i], 2)
		}
		buttonRecordsBytes.writeUI8(0)
		body.writeUI16(if (hasCondActions) buttonRecordsBytes.length + 2 else 0)
		body.writeBytes(buttonRecordsBytes)
		if (hasCondActions) {
			for (i in 0 until condActions.size) {
				val condActionBytes: SWFData = SWFData()
				condActionBytes.writeBUTTONCONDACTION(condActions[i])
				body.writeUI16(if (i < condActions.size - 1) condActionBytes.length + 2 else 0)
				body.writeBytes(condActionBytes)
			}
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineButton2 = com.codeazur.as3swf.tags.TagDefineButton2()
		tag.characterId = characterId
		tag.trackAsMenu = trackAsMenu
		for (i in 0 until characters.size) tag.characters.add(characters[i].clone())
		for (i in 0 until condActions.size) {
			tag.condActions.add(condActions[i].clone())
		}
		return tag
	}

	fun getRecordsByState(state: String): java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord> {
		return frames[state]!!
	}

	override val type = com.codeazur.as3swf.tags.TagDefineButton2.Companion.TYPE
	override val name = "DefineButton2"
	override val version = 3
	override val level = 2

	protected fun processRecords(): Unit {
		val upState: java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord> = java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>()
		val overState: java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord> = java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>()
		val downState: java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord> = java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>()
		val hitState: java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord> = java.util.ArrayList<com.codeazur.as3swf.data.SWFButtonRecord>()
		for (i in 0 until characters.size) {
			val record: com.codeazur.as3swf.data.SWFButtonRecord = characters[i]
			if (record.stateUp) upState.add(record)
			if (record.stateOver) overState.add(record)
			if (record.stateDown) downState.add(record)
			if (record.stateHitTest) hitState.add(record)
		}
		frames[com.codeazur.as3swf.tags.TagDefineButton.Companion.STATE_UP] = java.util.ArrayList(upState.sortedBy { it.placeDepth })
		frames[com.codeazur.as3swf.tags.TagDefineButton.Companion.STATE_OVER] = java.util.ArrayList(overState.sortedBy { it.placeDepth })
		frames[com.codeazur.as3swf.tags.TagDefineButton.Companion.STATE_DOWN] = java.util.ArrayList(downState.sortedBy { it.placeDepth })
		frames[com.codeazur.as3swf.tags.TagDefineButton.Companion.STATE_HIT] = java.util.ArrayList(hitState.sortedBy { it.placeDepth })
	}

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", TrackAsMenu: " + trackAsMenu
		if (characters.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Characters:"
			for (i in 0 until characters.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + characters[i].toString(indent + 4)
			}
		}
		if (condActions.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "CondActions:"
			for (i in 0 until condActions.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + condActions[i].toString(indent + 4, flags)
			}
		}
		return str
	}
}

class TagDefineButtonCxform : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		val TYPE = 23
	}

	lateinit var buttonColorTransform: com.codeazur.as3swf.data.SWFColorTransform

	override var characterId: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		buttonColorTransform = data.readCXFORM()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeCXFORM(buttonColorTransform)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag = com.codeazur.as3swf.tags.TagDefineButtonCxform()
		tag.characterId = characterId
		tag.buttonColorTransform = buttonColorTransform.clone()
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineButtonCxform.Companion.TYPE
	override val name = "DefineButtonCxform"
	override val version = 2
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"ColorTransform: " + buttonColorTransform
		return str
	}
}

class TagDefineButtonSound : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 17
	}

	var buttonSoundChar0: Int = 0
	var buttonSoundChar1: Int = 0
	var buttonSoundChar2: Int = 0
	var buttonSoundChar3: Int = 0
	lateinit var buttonSoundInfo0: com.codeazur.as3swf.data.SWFSoundInfo
	lateinit var buttonSoundInfo1: com.codeazur.as3swf.data.SWFSoundInfo
	lateinit var buttonSoundInfo2: com.codeazur.as3swf.data.SWFSoundInfo
	lateinit var buttonSoundInfo3: com.codeazur.as3swf.data.SWFSoundInfo

	override var characterId: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		buttonSoundChar0 = data.readUI16()
		if (buttonSoundChar0 != 0) {
			buttonSoundInfo0 = data.readSOUNDINFO()
		}
		buttonSoundChar1 = data.readUI16()
		if (buttonSoundChar1 != 0) {
			buttonSoundInfo1 = data.readSOUNDINFO()
		}
		buttonSoundChar2 = data.readUI16()
		if (buttonSoundChar2 != 0) {
			buttonSoundInfo2 = data.readSOUNDINFO()
		}
		buttonSoundChar3 = data.readUI16()
		if (buttonSoundChar3 != 0) {
			buttonSoundInfo3 = data.readSOUNDINFO()
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeUI16(buttonSoundChar0)
		if (buttonSoundChar0 != 0) body.writeSOUNDINFO(buttonSoundInfo0)
		body.writeUI16(buttonSoundChar1)
		if (buttonSoundChar1 != 0) body.writeSOUNDINFO(buttonSoundInfo1)
		body.writeUI16(buttonSoundChar2)
		if (buttonSoundChar2 != 0) body.writeSOUNDINFO(buttonSoundInfo2)
		body.writeUI16(buttonSoundChar3)
		if (buttonSoundChar3 != 0) body.writeSOUNDINFO(buttonSoundInfo3)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag = com.codeazur.as3swf.tags.TagDefineButtonSound()
		tag.characterId = characterId
		tag.buttonSoundChar0 = buttonSoundChar0
		tag.buttonSoundChar1 = buttonSoundChar1
		tag.buttonSoundChar2 = buttonSoundChar2
		tag.buttonSoundChar3 = buttonSoundChar3
		tag.buttonSoundInfo0 = buttonSoundInfo0.clone()
		tag.buttonSoundInfo1 = buttonSoundInfo1.clone()
		tag.buttonSoundInfo2 = buttonSoundInfo2.clone()
		tag.buttonSoundInfo3 = buttonSoundInfo3.clone()
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineButtonSound.Companion.TYPE
	override val name = "DefineButtonSound"
	override val version = 2
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ButtonID: " + characterId + ", " +
			"ButtonSoundChars: " + buttonSoundChar0 + "," + buttonSoundChar1 + "," + buttonSoundChar2 + "," + buttonSoundChar3
		return str
	}
}

class TagDefineEditText : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 37
	}

	lateinit var bounds: com.codeazur.as3swf.data.SWFRectangle
	var variableName: String? = null

	var hasText: Boolean = false
	var wordWrap: Boolean = false
	var multiline: Boolean = false
	var password: Boolean = false
	var readOnly: Boolean = false
	var hasTextColor: Boolean = false
	var hasMaxLength: Boolean = false
	var hasFont: Boolean = false
	var hasFontClass: Boolean = false
	var autoSize: Boolean = false
	var hasLayout: Boolean = false
	var noSelect: Boolean = false
	var border: Boolean = false
	var wasStatic: Boolean = false
	var html: Boolean = false
	var useOutlines: Boolean = false

	var fontId: Int = 0
	var fontClass: String? = null
	var fontHeight: Int = 0
	var textColor: Int = 0
	var maxLength: Int = 0
	var align: Int = 0
	var leftMargin: Int = 0
	var rightMargin: Int = 0
	var indent: Int = 0
	var leading: Int = 0
	var initialText: String? = null

	override var characterId: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		bounds = data.readRECT()
		val flags1: Int = data.readUI8()
		hasText = ((flags1 and 0x80) != 0)
		wordWrap = ((flags1 and 0x40) != 0)
		multiline = ((flags1 and 0x20) != 0)
		password = ((flags1 and 0x10) != 0)
		readOnly = ((flags1 and 0x08) != 0)
		hasTextColor = ((flags1 and 0x04) != 0)
		hasMaxLength = ((flags1 and 0x02) != 0)
		hasFont = ((flags1 and 0x01) != 0)
		val flags2: Int = data.readUI8()
		hasFontClass = ((flags2 and 0x80) != 0)
		autoSize = ((flags2 and 0x40) != 0)
		hasLayout = ((flags2 and 0x20) != 0)
		noSelect = ((flags2 and 0x10) != 0)
		border = ((flags2 and 0x08) != 0)
		wasStatic = ((flags2 and 0x04) != 0)
		html = ((flags2 and 0x02) != 0)
		useOutlines = ((flags2 and 0x01) != 0)
		if (hasFont) fontId = data.readUI16()
		if (hasFontClass) fontClass = data.readString()
		if (hasFont) fontHeight = data.readUI16()
		if (hasTextColor) textColor = data.readRGBA()
		if (hasMaxLength) maxLength = data.readUI16()
		if (hasLayout) {
			align = data.readUI8()
			leftMargin = data.readUI16()
			rightMargin = data.readUI16()
			indent = data.readUI16()
			leading = data.readSI16()
		}
		variableName = data.readString()
		if (hasText) {
			initialText = data.readString()
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body = SWFData()
		body.writeUI16(characterId)
		body.writeRECT(bounds)
		var flags1: Int = 0
		if (hasText) flags1 = flags1 or 0x80
		if (wordWrap) flags1 = flags1 or 0x40
		if (multiline) flags1 = flags1 or 0x20
		if (password) flags1 = flags1 or 0x10
		if (readOnly) flags1 = flags1 or 0x08
		if (hasTextColor) flags1 = flags1 or 0x04
		if (hasMaxLength) flags1 = flags1 or 0x02
		if (hasFont) flags1 = flags1 or 0x01
		body.writeUI8(flags1)
		var flags2: Int = 0
		if (hasFontClass) flags2 = flags2 or 0x80
		if (autoSize) flags2 = flags2 or 0x40
		if (hasLayout) flags2 = flags2 or 0x20
		if (noSelect) flags2 = flags2 or 0x10
		if (border) flags2 = flags2 or 0x08
		if (wasStatic) flags2 = flags2 or 0x04
		if (html) flags2 = flags2 or 0x02
		if (useOutlines) flags2 = flags2 or 0x01
		body.writeUI8(flags2)
		if (hasFont) body.writeUI16(fontId)
		if (hasFontClass) body.writeString(fontClass)
		if (hasFont) body.writeUI16(fontHeight)
		if (hasTextColor) body.writeRGBA(textColor)
		if (hasMaxLength) body.writeUI16(maxLength)
		if (hasLayout) {
			body.writeUI8(align)
			body.writeUI16(leftMargin)
			body.writeUI16(rightMargin)
			body.writeUI16(indent)
			body.writeSI16(leading)
		}
		body.writeString(variableName)
		if (hasText) body.writeString(initialText)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag = com.codeazur.as3swf.tags.TagDefineEditText()
		tag.characterId = characterId
		tag.bounds = bounds.clone()
		tag.variableName = variableName
		tag.hasText = hasText
		tag.wordWrap = wordWrap
		tag.multiline = multiline
		tag.password = password
		tag.readOnly = readOnly
		tag.hasTextColor = hasTextColor
		tag.hasMaxLength = hasMaxLength
		tag.hasFont = hasFont
		tag.hasFontClass = hasFontClass
		tag.autoSize = autoSize
		tag.hasLayout = hasLayout
		tag.noSelect = noSelect
		tag.border = border
		tag.wasStatic = wasStatic
		tag.html = html
		tag.useOutlines = useOutlines
		tag.fontId = fontId
		tag.fontClass = fontClass
		tag.fontHeight = fontHeight
		tag.textColor = textColor
		tag.maxLength = maxLength
		tag.align = align
		tag.leftMargin = leftMargin
		tag.rightMargin = rightMargin
		tag.indent = indent
		tag.leading = leading
		tag.initialText = initialText
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineEditText.Companion.TYPE
	override val name = "DefineEditText"
	override val version = 4
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			(if (hasText && initialText!!.isNotEmpty()) "Text: " + initialText + ", " else "") +
			(if (variableName!!.isNotEmpty()) "VariableName: " + variableName + ", " else "") +
			"Bounds: " + bounds
		return str
	}
}

open class TagDefineFont : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 10
	}

	override var characterId = 0
	var glyphShapeTable = java.util.ArrayList<com.codeazur.as3swf.data.SWFShape>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		// Because the glyph shape table immediately follows the offset table,
		// the number of entries in each table (the number of glyphs in the font) can be inferred by
		// dividing the first entry in the offset table by two.
		val numGlyphs: Int = data.readUI16() ushr 1
		// Skip offsets. We don't need them here.
		data.skipBytes((numGlyphs - 1) shl 1)
		// Read glyph shape table
		for (i in 0 until numGlyphs) {
			glyphShapeTable.add(data.readSHAPE(unitDivisor))
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		var prevPtr: Int = 0
		val len: Int = glyphShapeTable.size
		val shapeTable: SWFData = SWFData()
		body.writeUI16(characterId)
		val offsetTableLength: Int = (len shl 1)
		for (i in 0 until len) {
			// Write out the offset table for the current glyph
			body.writeUI16(shapeTable.position + offsetTableLength)
			// Serialize the glyph's shape to a separate bytearray
			shapeTable.writeSHAPE(glyphShapeTable[i])
		}
		// Now concatenate the glyph shape table to the end (after
		// the offset table that we were previously writing inside
		// the for loop above).
		body.writeBytes(shapeTable)
		// Now write the tag with the known body length, and the
		// actual contents out to the provided SWFData instance.
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag = throw(Error("Not implemented yet."))

	fun export(handler: ShapeExporter, glyphIndex: Int): Unit {
		glyphShapeTable[glyphIndex].export(handler)
	}

	override val type = com.codeazur.as3swf.tags.TagDefineFont.Companion.TYPE
	override val name = "DefineFont"
	override val version = 1
	override val level = 1

	protected open val unitDivisor = 1.0

	override fun toString(indent: Int, flags: Int): String {
		val str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Glyphs: " + glyphShapeTable.size
		return str + toStringCommon(indent)
	}

	protected open fun toStringCommon(indent: Int): String {
		var str: String = ""
		for (i in 0 until glyphShapeTable.size) {
			str += "\n" + " ".repeat(indent + 2) + "[" + i + "] GlyphShapes:"
			str += glyphShapeTable[i].toString(indent + 4)
		}
		return str
	}
}

open class TagDefineFont2 : com.codeazur.as3swf.tags.TagDefineFont(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 48
	}

	var hasLayout: Boolean = false
	var shiftJIS: Boolean = false
	var smallText: Boolean = false
	var ansi: Boolean = false
	var wideOffsets: Boolean = false
	var wideCodes: Boolean = false
	var italic: Boolean = false
	var bold: Boolean = false
	var languageCode: Int = 0
	lateinit var fontName: String
	var ascent: Int = 0
	var descent: Int = 0
	var leading: Int = 0

	val codeTable = java.util.ArrayList<Int>()
	val fontAdvanceTable = java.util.ArrayList<Int>()
	val fontBoundsTable = java.util.ArrayList<com.codeazur.as3swf.data.SWFRectangle>()
	val fontKerningTable = java.util.ArrayList<com.codeazur.as3swf.data.SWFKerningRecord>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		val flags: Int = data.readUI8()
		hasLayout = ((flags and 0x80) != 0)
		shiftJIS = ((flags and 0x40) != 0)
		smallText = ((flags and 0x20) != 0)
		ansi = ((flags and 0x10) != 0)
		wideOffsets = ((flags and 0x08) != 0)
		wideCodes = ((flags and 0x04) != 0)
		italic = ((flags and 0x02) != 0)
		bold = ((flags and 0x01) != 0)
		languageCode = data.readLANGCODE()
		val fontNameLen: Int = data.readUI8()
		val fontNameRaw: FlashByteArray = FlashByteArray()
		data.readBytes(fontNameRaw, 0, fontNameLen)
		fontName = fontNameRaw.readUTFBytes(fontNameLen)
		val numGlyphs: Int = data.readUI16()
		if (numGlyphs > 0) {
			// Skip offsets. We don't need them.
			data.skipBytes(numGlyphs shl (if (wideOffsets) 2 else 1))
			// Not used
			var codeTableOffset: Int = (if (wideOffsets) data.readUI32() else data.readUI16())
			for (i in 0 until numGlyphs) {
				glyphShapeTable.add(data.readSHAPE())
			}
			for (i in 0 until numGlyphs) {
				codeTable.add(if (wideCodes) data.readUI16() else data.readUI8())
			}
		}
		if (hasLayout) {
			ascent = data.readUI16()
			descent = data.readUI16()
			leading = data.readSI16()
			for (i in 0 until numGlyphs) {
				fontAdvanceTable.add(data.readSI16())
			}
			for (i in 0 until numGlyphs) {
				fontBoundsTable.add(data.readRECT())
			}
			val kerningCount: Int = data.readUI16()
			for (i in 0 until kerningCount) {
				fontKerningTable.add(data.readKERNINGRECORD(wideCodes))
			}
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		val numGlyphs: Int = glyphShapeTable.size
		body.writeUI16(characterId)
		var flags: Int = 0
		if (hasLayout) flags = flags or 0x80
		if (shiftJIS) flags = flags or 0x40
		if (smallText) flags = flags or 0x20
		if (ansi) flags = flags or 0x10
		if (wideOffsets) flags = flags or 0x08
		if (wideCodes) flags = flags or 0x04
		if (italic) flags = flags or 0x02
		if (bold) flags = flags or 0x01
		body.writeUI8(flags)
		body.writeLANGCODE(languageCode)
		val fontNameRaw: FlashByteArray = FlashByteArray()
		fontNameRaw.writeUTFBytes(fontName)
		body.writeUI8(fontNameRaw.length)
		body.writeBytes(fontNameRaw)
		body.writeUI16(numGlyphs)
		if (numGlyphs > 0) {
			val offsetTableLength: Int = (numGlyphs shl (if (wideOffsets) 2 else 1))
			val codeTableOffsetLength: Int = (if (wideOffsets) 4 else 2)
			var codeTableLength: Int = (if (wideOffsets) (numGlyphs shl 1) else numGlyphs)
			val offset: Int = offsetTableLength + codeTableOffsetLength
			val shapeTable: SWFData = SWFData()
			for (i in 0 until numGlyphs) {
				// Write out the offset table for the current glyph
				if (wideOffsets) {
					body.writeUI32(offset + shapeTable.position)
				} else {
					body.writeUI16(offset + shapeTable.position)
				}
				// Serialize the glyph's shape to a separate bytearray
				shapeTable.writeSHAPE(glyphShapeTable[i])
			}
			// Code table offset
			if (wideOffsets) {
				body.writeUI32(offset + shapeTable.length)
			} else {
				body.writeUI16(offset + shapeTable.length)
			}
			// Now concatenate the glyph shape table to the end (after
			// the offset table that we were previously writing inside
			// the for loop above).
			body.writeBytes(shapeTable)
			// Write the code table
			for (i in 0 until numGlyphs) {
				if (wideCodes) {
					body.writeUI16(codeTable[i])
				} else {
					body.writeUI8(codeTable[i])
				}
			}
		}
		if (hasLayout) {
			body.writeUI16(ascent)
			body.writeUI16(descent)
			body.writeSI16(leading)
			for (i in 0 until numGlyphs) {
				body.writeSI16(fontAdvanceTable[i])
			}
			for (i in 0 until numGlyphs) {
				body.writeRECT(fontBoundsTable[i])
			}
			val kerningCount: Int = fontKerningTable.size
			body.writeUI16(kerningCount)
			for (i in 0 until kerningCount) {
				body.writeKERNINGRECORD(fontKerningTable[i], wideCodes)
			}
		}
		// Now write the tag with the known body length, and the
		// actual contents out to the provided SWFData instance.
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagDefineFont2.Companion.TYPE
	override val name = "DefineFont2"
	override val version = 3
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		val str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"FontName: " + fontName + ", " +
			"Italic: " + italic + ", " +
			"Bold: " + bold + ", " +
			"Glyphs: " + glyphShapeTable.size
		return str + toStringCommon(indent)
	}

	override fun toStringCommon(indent: Int): String {
		var str: String = super.toStringCommon(indent)
		if (hasLayout) {
			str += "\n" + " ".repeat(indent + 2) + "Ascent: " + ascent
			str += "\n" + " ".repeat(indent + 2) + "Descent: " + descent
			str += "\n" + " ".repeat(indent + 2) + "Leading: " + leading
		}
		if (codeTable.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "CodeTable:"
			for (i in 0 until codeTable.size) {
				if ((i and 0x0f) == 0) {
					str += "\n" + " ".repeat(indent + 4) + codeTable[i].toString()
				} else {
					str += ", " + codeTable[i].toString()
				}
			}
		}
		if (fontAdvanceTable.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "FontAdvanceTable:"
			for (i in 0 until fontAdvanceTable.size) {
				if ((i and 0x07) == 0) {
					str += "\n" + " ".repeat(indent + 4) + fontAdvanceTable[i].toString()
				} else {
					str += ", " + fontAdvanceTable[i].toString()
				}
			}
		}
		if (fontBoundsTable.size > 0) {
			var hasNonNullBounds: Boolean = false
			for (i in 0 until fontBoundsTable.size) {
				val rect: com.codeazur.as3swf.data.SWFRectangle = fontBoundsTable[i]
				if (rect.xmin != 0 || rect.xmax != 0 || rect.ymin != 0 || rect.ymax != 0) {
					hasNonNullBounds = true
					break
				}
			}
			if (hasNonNullBounds) {
				str += "\n" + " ".repeat(indent + 2) + "FontBoundsTable:"
				for (i in 0 until fontBoundsTable.size) {
					str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + fontBoundsTable[i].toString()
				}
			}
		}
		if (fontKerningTable.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "KerningTable:"
			for (i in 0 until fontKerningTable.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + fontKerningTable[i].toString()
			}
		}
		return str
	}
}

class TagDefineFont3 : com.codeazur.as3swf.tags.TagDefineFont2(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 75
	}

	override val type = com.codeazur.as3swf.tags.TagDefineFont3.Companion.TYPE
	override val name = "DefineFont3"
	override val version = 8
	override val level = 2

	override val unitDivisor = 20.0

	override fun toString(indent: Int, flags: Int): String {
		val str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"FontName: " + fontName + ", " +
			"Italic: " + italic + ", " +
			"Bold: " + bold + ", " +
			"Glyphs: " + glyphShapeTable.size
		return str + toStringCommon(indent)
	}
}

class TagDefineFont4 : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 91
	}

	var hasFontData: Boolean = false
	var italic: Boolean = false
	var bold: Boolean = false
	lateinit var fontName: String

	override var characterId: Int = 0

	protected var fontData = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val pos: Int = data.position
		characterId = data.readUI16()
		val flags: Int = data.readUI8()
		hasFontData = ((flags and 0x04) != 0)
		italic = ((flags and 0x02) != 0)
		bold = ((flags and 0x01) != 0)
		fontName = data.readString()
		if (hasFontData && length > data.position - pos) {
			data.readBytes(fontData, 0, length - (data.position - pos))
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		var flags: Int = 0
		if (hasFontData) {
			flags = flags or 0x04
		}
		if (italic) {
			flags = flags or 0x02
		}
		if (bold) {
			flags = flags or 0x01
		}
		body.writeUI8(flags)
		body.writeString(fontName)
		if (hasFontData && fontData.length > 0) {
			body.writeBytes(fontData)
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineFont4 = com.codeazur.as3swf.tags.TagDefineFont4()
		tag.characterId = characterId
		tag.hasFontData = hasFontData
		tag.italic = italic
		tag.bold = bold
		tag.fontName = fontName
		if (fontData.length > 0) {
			tag.fontData.writeBytes(fontData)
		}
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineFont4.Companion.TYPE
	override val name = "DefineFont4"
	override val version = 10
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"FontName: " + fontName + ", " +
			"HasFontData: " + hasFontData + ", " +
			"Italic: " + italic + ", " +
			"Bold: " + bold
		return str
	}
}

class TagDefineFontAlignZones : _BaseTag(), ITag {
	companion object {
		const val TYPE = 73
	}

	var fontId: Int = 0
	var csmTableHint: Int = 0

	protected var _zoneTable = java.util.ArrayList<com.codeazur.as3swf.data.SWFZoneRecord>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		fontId = data.readUI16()
		csmTableHint = (data.readUI8() ushr 6)
		val recordsEndPos: Int = data.position + length - 3
		while (data.position < recordsEndPos) {
			_zoneTable.add(data.readZONERECORD())
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(fontId)
		body.writeUI8(csmTableHint shl 6)
		for (i in 0 until _zoneTable.size) {
			body.writeZONERECORD(_zoneTable[i])
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagDefineFontAlignZones.Companion.TYPE
	override val name = "DefineFontAlignZones"
	override val version = 8
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"FontID: " + fontId + ", " +
			"CSMTableHint: " + com.codeazur.as3swf.data.consts.CSMTableHint.toString(csmTableHint) + ", " +
			"Records: " + _zoneTable.size
		for (i in 0 until _zoneTable.size) {
			str += "\n" + " ".repeat(indent + 2) + "[" + i + "] " + _zoneTable[i].toString(indent + 2)
		}
		return str
	}
}

open class TagDefineFontInfo : _BaseTag(), ITag {
	companion object {
		const val TYPE = 13
	}

	var fontId: Int = 0
	lateinit var fontName: String
	var smallText: Boolean = false
	var shiftJIS: Boolean = false
	var ansi: Boolean = false
	var italic: Boolean = false
	var bold: Boolean = false
	var wideCodes: Boolean = false
	var langCode: Int = 0

	protected var _codeTable = java.util.ArrayList<Int>()

	protected var langCodeLength: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		fontId = data.readUI16()

		val fontNameLen: Int = data.readUI8()
		val fontNameRaw: FlashByteArray = FlashByteArray()
		data.readBytes(fontNameRaw, 0, fontNameLen)
		fontName = fontNameRaw.readUTFBytes(fontNameLen)

		val flags: Int = data.readUI8()
		smallText = ((flags and 0x20) != 0)
		shiftJIS = ((flags and 0x10) != 0)
		ansi = ((flags and 0x08) != 0)
		italic = ((flags and 0x04) != 0)
		bold = ((flags and 0x02) != 0)
		wideCodes = ((flags and 0x01) != 0)

		parseLangCode(data)

		val numGlyphs: Int = length - fontNameLen - langCodeLength - 4
		for (i in 0 until numGlyphs) {
			_codeTable.add(if (wideCodes) data.readUI16() else data.readUI8())
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(fontId)

		val fontNameRaw: FlashByteArray = FlashByteArray()
		fontNameRaw.writeUTFBytes(fontName)
		body.writeUI8(fontNameRaw.length)
		body.writeBytes(fontNameRaw)

		var flags: Int = 0
		if (smallText) {
			flags = flags or 0x20
		}
		if (shiftJIS) {
			flags = flags or 0x10
		}
		if (ansi) {
			flags = flags or 0x08
		}
		if (italic) {
			flags = flags or 0x04
		}
		if (bold) {
			flags = flags or 0x02
		}
		if (wideCodes) {
			flags = flags or 0x01
		}
		body.writeUI8(flags)

		publishLangCode(body)

		val numGlyphs: Int = _codeTable.size
		for (i in 0 until numGlyphs) {
			if (wideCodes) {
				body.writeUI16(_codeTable[i])
			} else {
				body.writeUI8(_codeTable[i])
			}
		}

		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	protected open fun parseLangCode(data: SWFData): Unit {
		// Does nothing here.
		// Overridden in TagDefineFontInfo2, where it:
		// - reads langCode
		// - sets langCodeLength to 1
	}

	protected open fun publishLangCode(data: SWFData): Unit {
		// Does nothing here.
		// Overridden in TagDefineFontInfo2
	}

	override val type = com.codeazur.as3swf.tags.TagDefineFontInfo.Companion.TYPE
	override val name = "DefineFontInfo"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"FontID: " + fontId + ", " +
			"FontName: " + fontName + ", " +
			"Italic: " + italic + ", " +
			"Bold: " + bold + ", " +
			"Codes: " + _codeTable.size
	}
}

class TagDefineFontInfo2 : com.codeazur.as3swf.tags.TagDefineFontInfo(), ITag {
	companion object {
		const val TYPE = 62
	}

	override fun parseLangCode(data: SWFData): Unit {
		langCode = data.readUI8()
		langCodeLength = 1
	}

	override fun publishLangCode(data: SWFData): Unit {
		data.writeUI8(langCode)
	}

	override val type = com.codeazur.as3swf.tags.TagDefineFontInfo2.Companion.TYPE
	override val name = "DefineFontInfo2"
	override val version = 6
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"FontID: " + fontId + ", " +
			"FontName: " + fontName + ", " +
			"Italic: " + italic + ", " +
			"Bold: " + bold + ", " +
			"LanguageCode: " + langCode + ", " +
			"Codes: " + _codeTable.size
	}
}

class TagDefineFontName : _BaseTag(), ITag {
	companion object {
		const val TYPE = 88
	}

	var fontId: Int = 0
	lateinit var fontName: String
	lateinit var fontCopyright: String

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		fontId = data.readUI16()
		fontName = data.readString()
		fontCopyright = data.readString()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(fontId)
		body.writeString(fontName)
		body.writeString(fontCopyright)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagDefineFontName.Companion.TYPE
	override val name = "DefineFontName"
	override val version = 9
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"FontID: " + fontId + ", " +
			"Name: " + fontName + ", " +
			"Copyright: " + fontCopyright
	}
}

open class TagDefineMorphShape : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 46
	}

	lateinit var startBounds: com.codeazur.as3swf.data.SWFRectangle
	lateinit var endBounds: com.codeazur.as3swf.data.SWFRectangle
	lateinit var startEdges: com.codeazur.as3swf.data.SWFShape
	lateinit var endEdges: com.codeazur.as3swf.data.SWFShape

	override var characterId: Int = 0

	protected var morphFillStyles = java.util.ArrayList<com.codeazur.as3swf.data.SWFMorphFillStyle>()
	protected var morphLineStyles = java.util.ArrayList<com.codeazur.as3swf.data.SWFMorphLineStyle>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		startBounds = data.readRECT()
		endBounds = data.readRECT()
		var offset: Int = data.readUI32()
		// MorphFillStyleArray
		var fillStyleCount: Int = data.readUI8()
		if (fillStyleCount == 0xff) {
			fillStyleCount = data.readUI16()
		}
		for (i in 0 until fillStyleCount) {
			morphFillStyles.add(data.readMORPHFILLSTYLE())
		}
		// MorphLineStyleArray
		var lineStyleCount: Int = data.readUI8()
		if (lineStyleCount == 0xff) {
			lineStyleCount = data.readUI16()
		}
		for (i in 0 until lineStyleCount) {
			morphLineStyles.add(data.readMORPHLINESTYLE())
		}
		startEdges = data.readSHAPE()
		endEdges = data.readSHAPE()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeRECT(startBounds)
		body.writeRECT(endBounds)
		val startBytes: SWFData = SWFData()
		// MorphFillStyleArray
		val fillStyleCount: Int = morphFillStyles.size
		if (fillStyleCount > 0xfe) {
			startBytes.writeUI8(0xff)
			startBytes.writeUI16(fillStyleCount)
		} else {
			startBytes.writeUI8(fillStyleCount)
		}
		for (i in 0 until fillStyleCount) {
			startBytes.writeMORPHFILLSTYLE(morphFillStyles[i])
		}
		// MorphLineStyleArray
		val lineStyleCount: Int = morphLineStyles.size
		if (lineStyleCount > 0xfe) {
			startBytes.writeUI8(0xff)
			startBytes.writeUI16(lineStyleCount)
		} else {
			startBytes.writeUI8(lineStyleCount)
		}
		for (i in 0 until lineStyleCount) {
			startBytes.writeMORPHLINESTYLE(morphLineStyles[i])
		}
		startBytes.writeSHAPE(startEdges)
		body.writeUI32(startBytes.length)
		body.writeBytes(startBytes)
		body.writeSHAPE(endEdges)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineMorphShape = com.codeazur.as3swf.tags.TagDefineMorphShape()
		throw(Error("Not implemented yet."))
		return tag
	}

	fun export(handler: ShapeExporter? = null, ratio: Double = 0.0): Unit {
		var j: Int = 0
		val exportShape: com.codeazur.as3swf.data.SWFShape = com.codeazur.as3swf.data.SWFShape()
		val numEdges: Int = startEdges.records.size
		for (i in 0 until numEdges) {
			var startRecord: com.codeazur.as3swf.data.SWFShapeRecord = startEdges.records[i]
			// Ignore start records that are style change records and don't have moveTo
			// The end record index is not incremented, because end records do not have
			// style change records without moveTo's.
			if (startRecord.type == com.codeazur.as3swf.data.SWFShapeRecord.Companion.TYPE_STYLECHANGE && !(startRecord as com.codeazur.as3swf.data.SWFShapeRecordStyleChange).stateMoveTo) {
				exportShape.records.add(startRecord.clone())
				continue
			}
			var endRecord: com.codeazur.as3swf.data.SWFShapeRecord = endEdges.records[j++]
			var exportRecord: com.codeazur.as3swf.data.SWFShapeRecord? = null
			// It is possible for an edge to change type over the course of a morph sequence.
			// A straight edge can become a curved edge and vice versa
			// Convert straight edge to curved edge, if needed:
			if (startRecord.type == com.codeazur.as3swf.data.SWFShapeRecord.Companion.TYPE_CURVEDEDGE && endRecord.type == com.codeazur.as3swf.data.SWFShapeRecord.Companion.TYPE_STRAIGHTEDGE) {
				endRecord = convertToCurvedEdge(endRecord as com.codeazur.as3swf.data.SWFShapeRecordStraightEdge)
			} else if (startRecord.type == com.codeazur.as3swf.data.SWFShapeRecord.Companion.TYPE_STRAIGHTEDGE && endRecord.type == com.codeazur.as3swf.data.SWFShapeRecord.Companion.TYPE_CURVEDEDGE) {
				startRecord = convertToCurvedEdge(startRecord as com.codeazur.as3swf.data.SWFShapeRecordStraightEdge)
			}
			when (startRecord.type) {
				com.codeazur.as3swf.data.SWFShapeRecord.Companion.TYPE_STYLECHANGE -> {
					val startStyleChange: com.codeazur.as3swf.data.SWFShapeRecordStyleChange = startRecord.clone() as com.codeazur.as3swf.data.SWFShapeRecordStyleChange
					val endStyleChange: com.codeazur.as3swf.data.SWFShapeRecordStyleChange = endRecord as com.codeazur.as3swf.data.SWFShapeRecordStyleChange
					startStyleChange.moveDeltaX += ((endStyleChange.moveDeltaX - startStyleChange.moveDeltaX) * ratio).toInt()
					startStyleChange.moveDeltaY += ((endStyleChange.moveDeltaY - startStyleChange.moveDeltaY) * ratio).toInt()
					exportRecord = startStyleChange
				}
				com.codeazur.as3swf.data.SWFShapeRecord.Companion.TYPE_STRAIGHTEDGE -> {
					val startStraightEdge: com.codeazur.as3swf.data.SWFShapeRecordStraightEdge = startRecord.clone() as com.codeazur.as3swf.data.SWFShapeRecordStraightEdge
					val endStraightEdge: com.codeazur.as3swf.data.SWFShapeRecordStraightEdge = endRecord as com.codeazur.as3swf.data.SWFShapeRecordStraightEdge
					startStraightEdge.deltaX += ((endStraightEdge.deltaX - startStraightEdge.deltaX) * ratio).toInt()
					startStraightEdge.deltaY += ((endStraightEdge.deltaY - startStraightEdge.deltaY) * ratio).toInt()
					if (startStraightEdge.deltaX != 0 && startStraightEdge.deltaY != 0) {
						startStraightEdge.generalLineFlag = true
						startStraightEdge.vertLineFlag = false
					} else {
						startStraightEdge.generalLineFlag = false
						startStraightEdge.vertLineFlag = (startStraightEdge.deltaX == 0)
					}
					exportRecord = startStraightEdge
				}
				com.codeazur.as3swf.data.SWFShapeRecord.Companion.TYPE_CURVEDEDGE -> {
					val startCurvedEdge: com.codeazur.as3swf.data.SWFShapeRecordCurvedEdge = startRecord.clone() as com.codeazur.as3swf.data.SWFShapeRecordCurvedEdge
					val endCurvedEdge: com.codeazur.as3swf.data.SWFShapeRecordCurvedEdge = endRecord as com.codeazur.as3swf.data.SWFShapeRecordCurvedEdge
					startCurvedEdge.controlDeltaX += ((endCurvedEdge.controlDeltaX - startCurvedEdge.controlDeltaX) * ratio).toInt()
					startCurvedEdge.controlDeltaY += ((endCurvedEdge.controlDeltaY - startCurvedEdge.controlDeltaY) * ratio).toInt()
					startCurvedEdge.anchorDeltaX += ((endCurvedEdge.anchorDeltaX - startCurvedEdge.anchorDeltaX) * ratio).toInt()
					startCurvedEdge.anchorDeltaY += ((endCurvedEdge.anchorDeltaY - startCurvedEdge.anchorDeltaY) * ratio).toInt()
					exportRecord = startCurvedEdge
				}
				com.codeazur.as3swf.data.SWFShapeRecord.Companion.TYPE_END -> {
					exportRecord = startRecord.clone()
				}
			}
			exportShape.records.add(exportRecord!!)
		}
		for (i in 0 until morphFillStyles.size) {
			exportShape.fillStyles.add(morphFillStyles[i].getMorphedFillStyle(ratio))
		}
		for (i in 0 until morphLineStyles.size) {
			exportShape.lineStyles.add(morphLineStyles[i].getMorphedLineStyle(ratio))
		}
		exportShape.export(handler)
	}

	protected fun convertToCurvedEdge(straightEdge: com.codeazur.as3swf.data.SWFShapeRecordStraightEdge): com.codeazur.as3swf.data.SWFShapeRecordCurvedEdge {
		val curvedEdge: com.codeazur.as3swf.data.SWFShapeRecordCurvedEdge = com.codeazur.as3swf.data.SWFShapeRecordCurvedEdge()
		curvedEdge.controlDeltaX = straightEdge.deltaX / 2
		curvedEdge.controlDeltaY = straightEdge.deltaY / 2
		curvedEdge.anchorDeltaX = straightEdge.deltaX
		curvedEdge.anchorDeltaY = straightEdge.deltaY
		return curvedEdge
	}

	override val type = com.codeazur.as3swf.tags.TagDefineMorphShape.Companion.TYPE
	override val name = "DefineMorphShape"
	override val version = 3
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val indent2: String = " ".repeat(indent + 2)
		val indent4: String = " ".repeat(indent + 4)
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) + "ID: " + characterId
		str += "\n" + indent2 + "Bounds:"
		str += "\n" + indent4 + "StartBounds: " + startBounds.toString()
		str += "\n" + indent4 + "EndBounds: " + endBounds.toString()
		if (morphFillStyles.size > 0) {
			str += "\n" + indent2 + "FillStyles:"
			for (i in 0 until morphFillStyles.size) {
				str += "\n" + indent4 + "[" + (i + 1) + "] " + morphFillStyles[i].toString()
			}
		}
		if (morphLineStyles.size > 0) {
			str += "\n" + indent2 + "LineStyles:"
			for (i in 0 until morphLineStyles.size) {
				str += "\n" + indent4 + "[" + (i + 1) + "] " + morphLineStyles[i].toString()
			}
		}
		str += startEdges.toString(indent + 2)
		str += endEdges.toString(indent + 2)
		return str
	}
}

class TagDefineMorphShape2 : com.codeazur.as3swf.tags.TagDefineMorphShape(), ITag {
	companion object {
		const val TYPE = 84
	}

	lateinit var startEdgeBounds: com.codeazur.as3swf.data.SWFRectangle
	lateinit var endEdgeBounds: com.codeazur.as3swf.data.SWFRectangle
	var usesNonScalingStrokes: Boolean = false
	var usesScalingStrokes: Boolean = false

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		startBounds = data.readRECT()
		endBounds = data.readRECT()
		startEdgeBounds = data.readRECT()
		endEdgeBounds = data.readRECT()
		val flags: Int = data.readUI8()
		usesNonScalingStrokes = ((flags and 0x02) != 0)
		usesScalingStrokes = ((flags and 0x01) != 0)
		var offset: Int = data.readUI32()
		// MorphFillStyleArray
		var fillStyleCount: Int = data.readUI8()
		if (fillStyleCount == 0xff) {
			fillStyleCount = data.readUI16()
		}
		for (i in 0 until fillStyleCount) {
			morphFillStyles.add(data.readMORPHFILLSTYLE())
		}
		// MorphLineStyleArray
		var lineStyleCount: Int = data.readUI8()
		if (lineStyleCount == 0xff) {
			lineStyleCount = data.readUI16()
		}
		for (i in 0 until lineStyleCount) {
			morphLineStyles.add(data.readMORPHLINESTYLE2())
		}
		startEdges = data.readSHAPE()
		endEdges = data.readSHAPE()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeRECT(startBounds)
		body.writeRECT(endBounds)
		body.writeRECT(startEdgeBounds)
		body.writeRECT(endEdgeBounds)
		var flags: Int = 0
		if (usesNonScalingStrokes) {
			flags = flags or 0x02
		}
		if (usesScalingStrokes) {
			flags = flags or 0x01
		}
		body.writeUI8(flags)
		val startBytes: SWFData = SWFData()
		// MorphFillStyleArray
		val fillStyleCount: Int = morphFillStyles.size
		if (fillStyleCount > 0xfe) {
			startBytes.writeUI8(0xff)
			startBytes.writeUI16(fillStyleCount)
		} else {
			startBytes.writeUI8(fillStyleCount)
		}
		for (i in 0 until fillStyleCount) {
			startBytes.writeMORPHFILLSTYLE(morphFillStyles[i])
		}
		// MorphLineStyleArray
		val lineStyleCount: Int = morphLineStyles.size
		if (lineStyleCount > 0xfe) {
			startBytes.writeUI8(0xff)
			startBytes.writeUI16(lineStyleCount)
		} else {
			startBytes.writeUI8(lineStyleCount)
		}
		for (i in 0 until lineStyleCount) {
			startBytes.writeMORPHLINESTYLE2(morphLineStyles[i] as com.codeazur.as3swf.data.SWFMorphLineStyle2)
		}
		startBytes.writeSHAPE(startEdges)
		body.writeUI32(startBytes.length)
		body.writeBytes(startBytes)
		body.writeSHAPE(endEdges)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagDefineMorphShape2.Companion.TYPE
	override val name = "DefineMorphShape2"
	override val version = 8
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		val indent2: String = " ".repeat(indent + 2)
		val indent4: String = " ".repeat(indent + 4)
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) + "ID: " + characterId
		str += "\n" + indent2 + "Bounds:"
		str += "\n" + indent4 + "StartBounds: " + startBounds.toString()
		str += "\n" + indent4 + "EndBounds: " + endBounds.toString()
		str += "\n" + indent4 + "StartEdgeBounds: " + startEdgeBounds.toString()
		str += "\n" + indent4 + "EndEdgeBounds: " + endEdgeBounds.toString()
		if (morphFillStyles.size > 0) {
			str += "\n" + indent2 + "FillStyles:"
			for (i in 0 until morphFillStyles.size) {
				str += "\n" + indent4 + "[" + (i + 1) + "] " + morphFillStyles[i].toString()
			}
		}
		if (morphLineStyles.size > 0) {
			str += "\n" + indent2 + "LineStyles:"
			for (i in 0 until morphLineStyles.size) {
				str += "\n" + indent4 + "[" + (i + 1) + "] " + morphLineStyles[i].toString()
			}
		}
		str += startEdges.toString(indent + 2)
		str += endEdges.toString(indent + 2)
		return str
	}
}

class TagDefineScalingGrid : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 78
	}

	lateinit var splitter: com.codeazur.as3swf.data.SWFRectangle

	override var characterId: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		splitter = data.readRECT()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeRECT(splitter)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineScalingGrid = com.codeazur.as3swf.tags.TagDefineScalingGrid()
		tag.characterId = characterId
		tag.splitter = splitter.clone()
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineScalingGrid.Companion.TYPE
	override val name = "DefineScalingGrid"
	override val version = 8
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"CharacterID: " + characterId + ", " +
			"Splitter: " + splitter
	}
}

class TagDefineSceneAndFrameLabelData : _BaseTag(), ITag {
	companion object {
		const val TYPE = 86
	}

	var scenes = java.util.ArrayList<com.codeazur.as3swf.data.SWFScene>()
	var frameLabels = java.util.ArrayList<com.codeazur.as3swf.data.SWFFrameLabel>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val sceneCount: Int = data.readEncodedU32()
		for (i in 0 until sceneCount) {
			val sceneOffset: Int = data.readEncodedU32()
			val sceneName: String = data.readString()
			scenes.add(com.codeazur.as3swf.data.SWFScene(sceneOffset, sceneName))
		}
		val frameLabelCount: Int = data.readEncodedU32()
		for (i in 0 until frameLabelCount) {
			val frameNumber: Int = data.readEncodedU32()
			val frameLabel: String = data.readString()
			frameLabels.add(com.codeazur.as3swf.data.SWFFrameLabel(frameNumber, frameLabel))
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeEncodedU32(this.scenes.size)
		for (i in 0 until this.scenes.size) {
			val scene: com.codeazur.as3swf.data.SWFScene = this.scenes[i]
			body.writeEncodedU32(scene.offset)
			body.writeString(scene.name)
		}
		body.writeEncodedU32(this.frameLabels.size)
		for (i in 0 until this.frameLabels.size) {
			val label: com.codeazur.as3swf.data.SWFFrameLabel = this.frameLabels[i]
			body.writeEncodedU32(label.frameNumber)
			body.writeString(label.name)
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagDefineSceneAndFrameLabelData.Companion.TYPE
	override val name = "DefineSceneAndFrameLabelData"
	override val version = 9
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
		if (scenes.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Scenes:"
			for (i in 0 until scenes.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + scenes[i].toString()
			}
		}
		if (frameLabels.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "FrameLabels:"
			for (i in 0 until frameLabels.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + frameLabels[i].toString()
			}
		}
		return str
	}
}

open class TagDefineShape : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 2
	}

	lateinit var shapeBounds: com.codeazur.as3swf.data.SWFRectangle
	lateinit var shapes: com.codeazur.as3swf.data.SWFShapeWithStyle

	override var characterId: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		shapeBounds = data.readRECT()
		shapes = data.readSHAPEWITHSTYLE(level)
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeRECT(shapeBounds)
		body.writeSHAPEWITHSTYLE(shapes, level)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineShape = com.codeazur.as3swf.tags.TagDefineShape()
		throw(Error("Not implemented yet."))
		return tag
	}

	fun export(handler: ShapeExporter): Unit {
		shapes.export(handler)
	}

	override val type = com.codeazur.as3swf.tags.TagDefineShape.Companion.TYPE
	override val name = "DefineShape"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Bounds: " + shapeBounds
		str += shapes.toString(indent + 2)
		return str
	}
}

open class TagDefineShape2 : com.codeazur.as3swf.tags.TagDefineShape(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 22
	}

	override val type = com.codeazur.as3swf.tags.TagDefineShape2.Companion.TYPE
	override val name = "DefineShape2"
	override val version = 2
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Bounds: " + shapeBounds
		str += shapes.toString(indent + 2)
		return str
	}
}

open class TagDefineShape3 : com.codeazur.as3swf.tags.TagDefineShape2(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 32
	}

	override val type = com.codeazur.as3swf.tags.TagDefineShape3.Companion.TYPE
	override val name = "DefineShape3"
	override val version = 3
	override val level = 3

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Bounds: " + shapeBounds
		str += shapes.toString(indent + 2)
		return str
	}
}

class TagDefineShape4 : com.codeazur.as3swf.tags.TagDefineShape3(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 83
	}

	lateinit var edgeBounds: com.codeazur.as3swf.data.SWFRectangle
	var usesFillWindingRule: Boolean = false
	var usesNonScalingStrokes: Boolean = false
	var usesScalingStrokes: Boolean = false

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		shapeBounds = data.readRECT()
		edgeBounds = data.readRECT()
		val flags: Int = data.readUI8()
		usesFillWindingRule = ((flags and 0x04) != 0)
		usesNonScalingStrokes = ((flags and 0x02) != 0)
		usesScalingStrokes = ((flags and 0x01) != 0)
		shapes = data.readSHAPEWITHSTYLE(level)
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeRECT(shapeBounds)
		body.writeRECT(edgeBounds)
		var flags: Int = 0
		if (usesFillWindingRule) {
			flags = flags or 0x04
		}
		if (usesNonScalingStrokes) {
			flags = flags or 0x02
		}
		if (usesScalingStrokes) {
			flags = flags or 0x01
		}
		body.writeUI8(flags)
		body.writeSHAPEWITHSTYLE(shapes, level)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagDefineShape4.Companion.TYPE
	override val name = "DefineShape4"
	override val version = 8
	override val level = 4

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) + "ID: " + characterId + ", "
		if (usesFillWindingRule) {
			str += "UsesFillWindingRule, "
		}
		if (usesNonScalingStrokes) {
			str += "UsesNonScalingStrokes, "
		}
		if (usesScalingStrokes) {
			str += "UsesScalingStrokes, "
		}
		str += "ShapeBounds: " + shapeBounds + ", EdgeBounds: " + edgeBounds
		str += shapes.toString(indent + 2)
		return str
	}
}

class TagDefineSound : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 14

		fun create(id: Int, format: Int = com.codeazur.as3swf.data.consts.SoundCompression.MP3, rate: Int = com.codeazur.as3swf.data.consts.SoundRate.KHZ_44, size: Int = com.codeazur.as3swf.data.consts.SoundSize.BIT_16, type: Int = com.codeazur.as3swf.data.consts.SoundType.STEREO, sampleCount: Int = 0, aSoundData: FlashByteArray? = null): com.codeazur.as3swf.tags.TagDefineSound {
			val defineSound: com.codeazur.as3swf.tags.TagDefineSound = com.codeazur.as3swf.tags.TagDefineSound()
			defineSound.characterId = id
			defineSound.soundFormat = format
			defineSound.soundRate = rate
			defineSound.soundSize = size
			defineSound.soundType = type
			defineSound.soundSampleCount = sampleCount
			if (aSoundData != null && aSoundData.length > 0) {
				defineSound.soundData.writeBytes(aSoundData)
			}
			return defineSound
		}

		fun createWithMP3(id: Int, mp3: FlashByteArray): com.codeazur.as3swf.tags.TagDefineSound {
			if (mp3 != null && mp3.length > 0) {
				val defineSound: com.codeazur.as3swf.tags.TagDefineSound = com.codeazur.as3swf.tags.TagDefineSound()
				defineSound.characterId = id
				defineSound.processMP3(mp3)
				return defineSound
			} else {
				throw(Error("No MP3 data."))
			}
		}
	}

	var soundFormat: Int = 0
	var soundRate: Int = 0
	var soundSize: Int = 0
	var soundType: Int = 0
	var soundSampleCount: Int = 0

	override var characterId: Int = 0

	protected var soundData: FlashByteArray = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		soundFormat = data.readUB(4)
		soundRate = data.readUB(2)
		soundSize = data.readUB(1)
		soundType = data.readUB(1)
		soundSampleCount = data.readUI32()
		data.readBytes(soundData, 0, length - 7)
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeUB(4, soundFormat)
		body.writeUB(2, soundRate)
		body.writeUB(1, soundSize)
		body.writeUB(1, soundType)
		body.writeUI32(soundSampleCount)
		if (soundData.length > 0) {
			body.writeBytes(soundData)
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag = com.codeazur.as3swf.tags.TagDefineSound()
		tag.characterId = characterId
		tag.soundFormat = soundFormat
		tag.soundRate = soundRate
		tag.soundSize = soundSize
		tag.soundType = soundType
		tag.soundSampleCount = soundSampleCount
		if (soundData.length > 0) {
			tag.soundData.writeBytes(soundData)
		}
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineSound.Companion.TYPE
	override val name = "DefineSound"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"SoundID: " + characterId + ", " +
			"Format: " + com.codeazur.as3swf.data.consts.SoundCompression.toString(soundFormat) + ", " +
			"Rate: " + com.codeazur.as3swf.data.consts.SoundRate.toString(soundRate) + ", " +
			"Size: " + com.codeazur.as3swf.data.consts.SoundSize.toString(soundSize) + ", " +
			"Type: " + com.codeazur.as3swf.data.consts.SoundType.toString(soundType) + ", " +
			"Samples: " + soundSampleCount
		return str
	}

	internal fun processMP3(mp3: FlashByteArray): Unit {
		var i: Int = 0
		var beginIdx: Int = 0
		var endIdx: Int = mp3.length
		var samples: Int = 0
		var firstFrame: Boolean = true
		var samplingrate: Int = 0
		var channelmode: Int = 0
		val frame: com.codeazur.as3swf.data.etc.MPEGFrame = com.codeazur.as3swf.data.etc.MPEGFrame()
		var state: String = "id3v2"
		while (i < mp3.length) {
			when (state) {
				"id3v2" -> {
					if (mp3[i] == 0x49 && mp3[i + 1] == 0x44 && mp3[i + 2] == 0x33) {
						i += 10 + ((mp3[i + 6] shl 21)
							or (mp3[i + 7] shl 14)
							or (mp3[i + 8] shl 7)
							or mp3[i + 9])
					}
					beginIdx = i
					state = "sync"
				}
				"sync" -> {
					if (mp3[i] == 0xff && (mp3[i + 1] and 0xe0) == 0xe0) {
						state = "frame"
					} else if (mp3[i] == 0x54 && mp3[i + 1] == 0x41 && mp3[i + 2] == 0x47) {
						endIdx = i
						i = mp3.length
					} else {
						i++
					}
				}
				"frame" -> {
					frame.setHeaderByteAt(0, mp3[i++])
					frame.setHeaderByteAt(1, mp3[i++])
					frame.setHeaderByteAt(2, mp3[i++])
					frame.setHeaderByteAt(3, mp3[i++])
					if (frame.hasCRC) {
						frame.setCRCByteAt(0, mp3[i++])
						frame.setCRCByteAt(1, mp3[i++])
					}
					if (firstFrame) {
						firstFrame = false
						samplingrate = frame.samplingrate
						channelmode = frame.channelMode
					}
					samples += frame.samples
					i += frame.size
					state = "sync"
				}
			}
		}
		soundSampleCount = samples
		soundFormat = com.codeazur.as3swf.data.consts.SoundCompression.MP3
		soundSize = com.codeazur.as3swf.data.consts.SoundSize.BIT_16
		soundType = if (channelmode == com.codeazur.as3swf.data.etc.MPEGFrame.Companion.CHANNEL_MODE_MONO) com.codeazur.as3swf.data.consts.SoundType.MONO else com.codeazur.as3swf.data.consts.SoundType.STEREO
		when (samplingrate) {
			44100 -> soundRate = com.codeazur.as3swf.data.consts.SoundRate.KHZ_44
			22050 -> soundRate = com.codeazur.as3swf.data.consts.SoundRate.KHZ_22
			11025 -> soundRate = com.codeazur.as3swf.data.consts.SoundRate.KHZ_11
			else -> throw(Error("Unsupported sampling rate: " + samplingrate + " Hz"))
		}
		// Clear ByteArray
		soundData.length = 0
		// Write SeekSamples (here always 0)
		soundData.writeShort(0)
		// Write raw MP3 (without ID3 metadata)
		soundData.writeBytes(mp3, beginIdx, endIdx - beginIdx)
	}
}

class TagDefineSprite : com.codeazur.as3swf.SWFTimelineContainer(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 39
	}

	var frameCount: Int = 0

	override var characterId: Int = 0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		frameCount = data.readUI16()
		/*
		if(async) {
			parseTagsAsync(data, version);
		} else {
			parseTags(data, version);
		}
		*/
		parseTags(data, version)
	}

	override suspend fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeUI16(frameCount) // TODO: get the real number of frames from controlTags
		publishTags(body, version)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineSprite = com.codeazur.as3swf.tags.TagDefineSprite()
		throw(Error("Not implemented yet."))
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineSprite.Companion.TYPE
	override val name = "DefineSprite"
	override val version = 3
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"FrameCount: " + frameCount +
			super.toString(indent, flags)
	}
}

open class TagDefineText : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 11
	}

	lateinit var textBounds: com.codeazur.as3swf.data.SWFRectangle
	lateinit var textMatrix: com.codeazur.as3swf.data.SWFMatrix

	override var characterId: Int = 0

	var records = java.util.ArrayList<com.codeazur.as3swf.data.SWFTextRecord>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		textBounds = data.readRECT()
		textMatrix = data.readMATRIX()
		val glyphBits: Int = data.readUI8()
		val advanceBits: Int = data.readUI8()
		var record: com.codeazur.as3swf.data.SWFTextRecord? = null
		while (true) {
			record = data.readTEXTRECORD(glyphBits, advanceBits, record, level)
			if (record == null) break
			records.add(record)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		var record: com.codeazur.as3swf.data.SWFTextRecord? = null
		body.writeUI16(characterId)
		body.writeRECT(textBounds)
		body.writeMATRIX(textMatrix)
		// Calculate glyphBits and advanceBits values
		val glyphBitsValues = arrayListOf<Int>()
		val advanceBitsValues = arrayListOf<Int>()
		val recordsLen: Int = records.size
		for (i in 0 until recordsLen) {
			record = records[i]
			val glyphCount: Int = record.glyphEntries.size
			for (j in 0 until glyphCount) {
				val glyphEntry: com.codeazur.as3swf.data.SWFGlyphEntry = record.glyphEntries[j]
				glyphBitsValues.add(glyphEntry.index)
				advanceBitsValues.add(glyphEntry.advance)
			}
		}
		val glyphBits: Int = body.calculateMaxBits(false, glyphBitsValues)
		val advanceBits: Int = body.calculateMaxBits(true, advanceBitsValues)
		body.writeUI8(glyphBits)
		body.writeUI8(advanceBits)
		// Write text records
		record = null
		for (i in 0 until recordsLen) {
			body.writeTEXTRECORD(records[i], glyphBits, advanceBits, record, level)
			record = records[i]
		}
		body.writeUI8(0)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineText = com.codeazur.as3swf.tags.TagDefineText()
		tag.characterId = characterId
		tag.textBounds = textBounds.clone()
		tag.textMatrix = textMatrix.clone()
		for (i in 0 until records.size) {
			tag.records.add(records[i].clone())
		}
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineText.Companion.TYPE
	override val name = "DefineText"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Bounds: " + textBounds + ", " +
			"Matrix: " + textMatrix
		if (records.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "TextRecords:"
			for (i in 0 until records.size) {
				str += "\n" +
					" ".repeat(indent + 4) +
					"[" + i + "] " +
					records[i].toString(indent + 4)
			}
		}
		return str
	}
}

class TagDefineText2 : com.codeazur.as3swf.tags.TagDefineText(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 33
	}

	override val type = com.codeazur.as3swf.tags.TagDefineText2.Companion.TYPE
	override val name = "DefineText2"
	override val version = 3
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Bounds: " + textBounds + ", " +
			"Matrix: " + textMatrix
		if (records.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "TextRecords:"
			for (i in 0 until records.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + records[i].toString()
			}
		}
		return str
	}
}

class TagDefineVideoStream : _BaseTag(), com.codeazur.as3swf.tags.IDefinitionTag {
	companion object {
		const val TYPE = 60
	}

	var numFrames: Int = 0
	var width: Int = 0
	var height: Int = 0
	var deblocking: Int = 0
	var smoothing: Boolean = false
	var codecId: Int = 0

	override var characterId: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		numFrames = data.readUI16()
		width = data.readUI16()
		height = data.readUI16()
		data.readUB(4)
		deblocking = data.readUB(3)
		smoothing = (data.readUB(1) == 1)
		codecId = data.readUI8()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, 10)
		data.writeUI16(characterId)
		data.writeUI16(numFrames)
		data.writeUI16(width)
		data.writeUI16(height)
		data.writeUB(4, 0) // Reserved
		data.writeUB(3, deblocking)
		data.writeUB(1, smoothing)
		data.writeUI8(codecId)
	}

	override fun clone(): com.codeazur.as3swf.tags.IDefinitionTag {
		val tag: com.codeazur.as3swf.tags.TagDefineVideoStream = com.codeazur.as3swf.tags.TagDefineVideoStream()
		tag.characterId = characterId
		tag.numFrames = numFrames
		tag.width = width
		tag.height = height
		tag.deblocking = deblocking
		tag.smoothing = smoothing
		tag.codecId = codecId
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagDefineVideoStream.Companion.TYPE
	override val name = "DefineVideoStream"
	override val version = 6
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Frames: " + numFrames + ", " +
			"Width: " + width + ", " +
			"Height: " + height + ", " +
			"Deblocking: " + com.codeazur.as3swf.data.consts.VideoDeblockingType.toString(deblocking) + ", " +
			"Smoothing: " + smoothing + ", " +
			"Codec: " + com.codeazur.as3swf.data.consts.VideoCodecID.toString(codecId)
	}
}

class TagDoABC : _BaseTag(), ITag {
	companion object {
		const val TYPE = 82

		fun create(abcData: FlashByteArray? = null, aName: String = "", aLazyInitializeFlag: Boolean = true): com.codeazur.as3swf.tags.TagDoABC {
			val doABC: com.codeazur.as3swf.tags.TagDoABC = com.codeazur.as3swf.tags.TagDoABC()
			if (abcData != null && abcData.length > 0) {
				doABC.bytes.writeBytes(abcData)
			}
			doABC.abcName = aName
			doABC.lazyInitializeFlag = aLazyInitializeFlag
			return doABC
		}
	}

	var lazyInitializeFlag: Boolean = false
	var abcName: String = ""

	val bytes: FlashByteArray = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val pos: Int = data.position
		val flags: Int = data.readUI32()
		lazyInitializeFlag = ((flags and 0x01) != 0)
		abcName = data.readString()
		data.readBytes(bytes, 0, length - (data.position - pos))
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI32(if (lazyInitializeFlag) 1 else 0)
		body.writeString(abcName)
		if (bytes.length > 0) {
			body.writeBytes(bytes)
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagDoABC.Companion.TYPE
	override val name = "DoABC"
	override val version = 9
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"Lazy: " + lazyInitializeFlag + ", " +
			(if (abcName.length > 0) "Name: " + abcName + ", " else "") +
			"Length: " + bytes.length
	}
}

class TagDoABCDeprecated : _BaseTag(), ITag {
	companion object {
		const val TYPE = 72
		fun create(abcData: FlashByteArray? = null): com.codeazur.as3swf.tags.TagDoABCDeprecated {
			val doABC: com.codeazur.as3swf.tags.TagDoABCDeprecated = com.codeazur.as3swf.tags.TagDoABCDeprecated()
			if (abcData != null && abcData.length > 0) {
				doABC.bytes.writeBytes(abcData)
			}
			return doABC
		}
	}

	protected var bytes = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val pos: Int = data.position
		data.readBytes(bytes, 0, length - (data.position - pos))
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		if (bytes.length > 0) {
			body.writeBytes(bytes)
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagDoABCDeprecated.Companion.TYPE
	override val name = "DoABCDeprecated"
	override val version = 9
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"Length: " + bytes.length
	}
}

open class TagDoAction : _BaseTag(), ITag {
	companion object {
		const val TYPE = 12
	}

	var actions = java.util.ArrayList<com.codeazur.as3swf.data.actions.IAction>()

	protected var labelCount: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		var action: com.codeazur.as3swf.data.actions.IAction?
		while (true) {
			action = data.readACTIONRECORD()
			if (action == null) break
			actions.add(action)
		}
		labelCount = com.codeazur.as3swf.data.actions.Action.Companion.resolveOffsets(actions)
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		for (i in 0 until actions.size) {
			body.writeACTIONRECORD(actions[i])
		}
		body.writeUI8(0)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagDoAction.Companion.TYPE
	override val name = "DoAction"
	override val version = 3
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) + "Records: " + actions.size
		if ((flags and com.codeazur.as3swf.SWF.Companion.TOSTRING_FLAG_AVM1_BYTECODE) == 0) {
			for (i in 0 until actions.size) {
				str += "\n" + " ".repeat(indent + 2) + "[" + i + "] " + actions[i].toString(indent + 2)
			}
		} else {
			val context: com.codeazur.as3swf.data.actions.ActionExecutionContext = com.codeazur.as3swf.data.actions.ActionExecutionContext(actions, arrayListOf(), labelCount)
			for (i in 0 until actions.size) {
				str += "\n" + " ".repeat(indent + 2) + actions[i].toBytecode(indent + 2, context)
			}
			if (context.endLabel != null) {
				str += "\n" + " ".repeat(indent + 4) + context.endLabel + ":"
			}
		}
		return str
	}
}

class TagDoInitAction : com.codeazur.as3swf.tags.TagDoAction(), ITag {
	companion object {
		const val TYPE = 59
	}

	var spriteId: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		spriteId = data.readUI16()
		var action: com.codeazur.as3swf.data.actions.IAction?
		while (true) {
			action = data.readACTIONRECORD()
			if (action == null) continue
			actions.add(action)
		}
		labelCount = com.codeazur.as3swf.data.actions.Action.Companion.resolveOffsets(actions)
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(spriteId)
		for (i in 0 until actions.size) {
			body.writeACTIONRECORD(actions[i])
		}
		body.writeUI8(0)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagDoInitAction.Companion.TYPE
	override val name = "DoInitAction"
	override val version = 6
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"SpriteID: " + spriteId + ", " +
			"Records: " + actions.size
		if ((flags and com.codeazur.as3swf.SWF.Companion.TOSTRING_FLAG_AVM1_BYTECODE) == 0) {
			for (i in 0 until actions.size) {
				str += "\n" + " ".repeat(indent + 2) + "[" + i + "] " + actions[i].toString(indent + 2)
			}
		} else {
			val context: com.codeazur.as3swf.data.actions.ActionExecutionContext = com.codeazur.as3swf.data.actions.ActionExecutionContext(actions, arrayListOf(), labelCount)
			for (i in 0 until actions.size) {
				str += "\n" + " ".repeat(indent + 2) + actions[i].toBytecode(indent + 2, context)
			}
			if (context.endLabel != null) {
				str += "\n" + " ".repeat(indent + 4) + context.endLabel + ":"
			}
		}
		return str
	}
}

open class TagEnableDebugger : _BaseTag(), ITag {
	companion object {
		const val TYPE = 58
	}

	protected var password = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		if (length > 0) {
			data.readBytes(password, 0, length)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, password.length)
		if (password.length > 0) {
			data.writeBytes(password)
		}
	}

	override val type = com.codeazur.as3swf.tags.TagEnableDebugger.Companion.TYPE
	override val name = "EnableDebugger"
	override val version = 5
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
	}
}

class TagEnableDebugger2 : com.codeazur.as3swf.tags.TagEnableDebugger(), ITag {
	companion object {
		const val TYPE = 64
	}

	// Reserved, SWF File Format v10 says this is always zero.
	// Observed other values from generated SWFs, e.g. 0x1975.
	protected var reserved: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		reserved = data.readUI16()
		if (length > 2) {
			data.readBytes(password, 0, length - 2)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, password.length + 2)
		data.writeUI16(reserved)
		if (password.length > 0) {
			data.writeBytes(password)
		}
	}

	override val type = com.codeazur.as3swf.tags.TagEnableDebugger2.Companion.TYPE
	override val name = "EnableDebugger2"
	override val version = 6
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"Password: " + (if (password.length == 0) "null" else password.readUTF()) + ", " +
			"Reserved: 0x" + reserved.toString(16)
	}
}

class TagEnableTelemetry : _BaseTag(), ITag {
	companion object {
		const val TYPE = 93
	}

	protected var password = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		if (length > 2) {
			data.readByte()
			data.readByte()
			data.readBytes(password, 0, length - 2)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, password.length + 2)
		data.writeByte(0)
		data.writeByte(0)
		if (password.length > 0) {
			data.writeBytes(password)
		}
	}

	override val type = com.codeazur.as3swf.tags.TagEnableTelemetry.Companion.TYPE
	override val name = "EnableTelemetry"
	override val version = 19
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
	}
}

class TagEnd : _BaseTag(), ITag {
	companion object {
		val TYPE = 0
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		// Do nothing. The End tag has no body.
	}

	suspend override fun publish(data: SWFData, version: Int) {
		data.writeTagHeader(type, 0)
	}

	override val type = com.codeazur.as3swf.tags.TagEnd.Companion.TYPE
	override val name = "End"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int) = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
}

class TagExportAssets : _BaseTag(), ITag {
	companion object {
		const val TYPE = 56
	}

	val symbols = java.util.ArrayList<com.codeazur.as3swf.data.SWFSymbol>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val numSymbols: Int = data.readUI16()
		for (i in 0 until numSymbols) {
			symbols.add(data.readSYMBOL())
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		val numSymbols: Int = symbols.size
		body.writeUI16(numSymbols)
		for (i in 0 until numSymbols) {
			body.writeSYMBOL(symbols[i])
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagExportAssets.Companion.TYPE
	override val name = "ExportAssets"
	override val version = 5
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
		if (symbols.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Assets:"
			for (i in 0 until symbols.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + symbols[i].toString()
			}
		}
		return str
	}
}

class TagFileAttributes : _BaseTag(), ITag {
	companion object {
		const val TYPE = 69
	}

	var useDirectBlit: Boolean = false
	var useGPU: Boolean = false
	var hasMetadata: Boolean = false
	var actionscript3: Boolean = true
	var useNetwork: Boolean = false

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val flags: Int = data.readUI8()
		useDirectBlit = ((flags and 0x40) != 0)
		useGPU = ((flags and 0x20) != 0)
		hasMetadata = ((flags and 0x10) != 0)
		actionscript3 = ((flags and 0x08) != 0)
		useNetwork = ((flags and 0x01) != 0)
		data.skipBytes(3)
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, 4)
		var flags: Int = 0
		if (useNetwork) flags = flags or 0x01
		if (actionscript3) flags = flags or 0x08
		if (hasMetadata) flags = flags or 0x10
		if (useGPU) flags = flags or 0x20
		if (useDirectBlit) flags = flags or 0x40
		data.writeUI8(flags)
		data.writeUI8(0)
		data.writeUI8(0)
		data.writeUI8(0)
	}

	override val type = com.codeazur.as3swf.tags.TagFileAttributes.Companion.TYPE
	override val name = "FileAttributes"
	override val version = 8
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"AS3: " + actionscript3 + ", " +
			"HasMetadata: " + hasMetadata + ", " +
			"UseDirectBlit: " + useDirectBlit + ", " +
			"UseGPU: " + useGPU + ", " +
			"UseNetwork: " + useNetwork
	}

	override fun toString() = toString(0, 0)
}

class TagFrameLabel : _BaseTag(), ITag {
	companion object {
		const val TYPE = 43
	}

	lateinit var frameName: String
	var namedAnchorFlag: Boolean = false

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val start: Int = data.position
		frameName = data.readString()
		if ((data.position - start) < length) {
			data.readUI8()    // Named anchor flag, always 1
			namedAnchorFlag = true
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeString(frameName)

		if (namedAnchorFlag) {
			data.writeUI8(1)
		}

		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagFrameLabel.Companion.TYPE
	override val name = "FrameLabel"
	override val version = 3
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = "Name: " + frameName
		if (namedAnchorFlag) {
			str += ", NamedAnchor = true"
		}
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) + str
	}
}

open class TagImportAssets : _BaseTag(), ITag {
	companion object {
		const val TYPE = 57
	}

	lateinit var url: String

	protected var symbols = java.util.ArrayList<com.codeazur.as3swf.data.SWFSymbol>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		url = data.readString()
		val numSymbols: Int = data.readUI16()
		for (i in 0 until numSymbols) {
			symbols.add(data.readSYMBOL())
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeString(url)
		val numSymbols: Int = symbols.size
		body.writeUI16(numSymbols)
		for (i in 0 until numSymbols) {
			body.writeSYMBOL(symbols[i])
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagImportAssets.Companion.TYPE
	override val name = "ImportAssets"
	override val version = 5
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
		if (symbols.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Assets:"
			for (i in 0 until symbols.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + symbols[i].toString()
			}
		}
		return str
	}
}

class TagImportAssets2 : com.codeazur.as3swf.tags.TagImportAssets(), ITag {
	companion object {
		const val TYPE = 71
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		url = data.readString()
		data.readUI8() // reserved, always 1
		data.readUI8() // reserved, always 0
		val numSymbols: Int = data.readUI16()
		for (i in 0 until numSymbols) {
			symbols.add(data.readSYMBOL())
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeString(url)
		body.writeUI8(1)
		body.writeUI8(0)
		val numSymbols: Int = symbols.size
		body.writeUI16(numSymbols)
		for (i in 0 until numSymbols) {
			body.writeSYMBOL(symbols[i])
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagImportAssets2.Companion.TYPE
	override val name = "ImportAssets2"
	override val version = 8
	override val level = 2
}

class TagJPEGTables : _BaseTag(), ITag {
	companion object {
		const val TYPE = 8
	}

	var jpegTables = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		if (length > 0) {
			data.readBytes(jpegTables, 0, length)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, jpegTables.length)
		if (jpegTables.length > 0) {
			data.writeBytes(jpegTables)
		}
	}

	override val type = com.codeazur.as3swf.tags.TagJPEGTables.Companion.TYPE
	override val name = "JPEGTables"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) + "Length: " + jpegTables.length
	}
}

class TagMetadata : _BaseTag(), ITag {
	companion object {
		const val TYPE = 77
	}

	lateinit var xmlString: String

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		xmlString = data.readString()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeString(xmlString)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagMetadata.Companion.TYPE
	override val name = "Metadata"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
		str += " " + xmlString
		return str
	}
}

class TagNameCharacter : _BaseTag(), ITag {
	companion object {
		const val TYPE = 40
	}

	protected var characterId: Int = 0

	protected var binaryData = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		if (length > 2) {
			data.readBytes(binaryData, 0, length - 2)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		if (binaryData.length > 0) {
			body.writeBytes(binaryData)
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	fun clone(): ITag {
		val tag: com.codeazur.as3swf.tags.TagNameCharacter = com.codeazur.as3swf.tags.TagNameCharacter()
		tag.characterId = characterId
		if (binaryData.length > 0) {
			tag.binaryData.writeBytes(binaryData)
		}
		return tag
	}

	override val type = com.codeazur.as3swf.tags.TagNameCharacter.Companion.TYPE
	override val name = "NameCharacter"
	override val version = 3
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ID: " + characterId
		if (binaryData.length > 0) {
			binaryData.position = 0
			str += ", Name: " + binaryData.readUTFBytes(binaryData.length - 1)
			binaryData.position = 0
		}
		return str
	}
}

open class TagPlaceObject : _BaseTag(), com.codeazur.as3swf.tags.IDisplayListTag {
	companion object {
		const val TYPE = 4
	}

	var hasClipActions = false
	var hasClipDepth = false
	var hasName = false
	var hasRatio = false
	var hasColorTransform = false
	var hasMatrix = false
	var hasCharacter = false
	var hasMove = false
	var hasOpaqueBackground = false
	var hasVisible = false
	var hasImage = false
	var hasClassName = false
	var hasCacheAsBitmap = false
	var hasBlendMode = false
	var hasFilterList = false

	var characterId = 0
	var depth = 0
	var matrix: com.codeazur.as3swf.data.SWFMatrix? = null
	var colorTransform: com.codeazur.as3swf.data.SWFColorTransform? = null

	// Forward declarations for TagPlaceObject2
	var ratio = 0
	var instanceName: String? = null
	var clipDepth = 0
	var clipActions: com.codeazur.as3swf.data.SWFClipActions? = null

	// Forward declarations for TagPlaceObject3
	var className: String? = null
	var blendMode = 0
	var bitmapCache = 0
	var bitmapBackgroundColor = 0
	var visible = 0

	// Forward declarations for TagPlaceObject4
	var metaData: Any? = null

	val surfaceFilterList = arrayListOf<com.codeazur.as3swf.data.filters.IFilter>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		val pos = data.position
		characterId = data.readUI16()
		depth = data.readUI16()
		matrix = data.readMATRIX()
		hasCharacter = true
		hasMatrix = true
		if (data.position - pos < length) {
			colorTransform = data.readCXFORM()
			hasColorTransform = true
		}
	}

	suspend override fun publish(data: SWFData, version: Int) {
		val body: SWFData = SWFData()
		body.writeUI16(characterId)
		body.writeUI16(depth)
		body.writeMATRIX(matrix!!)
		if (hasColorTransform) {
			body.writeCXFORM(colorTransform!!)
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagPlaceObject.Companion.TYPE
	override val name = "PlaceObject"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"Depth: " + depth
		if (hasCharacter) {
			str += ", CharacterID: " + characterId
		}
		if (hasMatrix) {
			str += ", Matrix: " + matrix
		}
		if (hasColorTransform) {
			str += ", ColorTransform: " + colorTransform
		}
		return str
	}
}

open class TagPlaceObject2 : com.codeazur.as3swf.tags.TagPlaceObject(), com.codeazur.as3swf.tags.IDisplayListTag {
	companion object {
		const val TYPE = 26
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val flags: Int = data.readUI8()
		hasClipActions = (flags and 0x80) != 0
		hasClipDepth = (flags and 0x40) != 0
		hasName = (flags and 0x20) != 0
		hasRatio = (flags and 0x10) != 0
		hasColorTransform = (flags and 0x08) != 0
		hasMatrix = (flags and 0x04) != 0
		hasCharacter = (flags and 0x02) != 0
		hasMove = (flags and 0x01) != 0
		depth = data.readUI16()
		if (hasCharacter) {
			characterId = data.readUI16()
		}
		if (hasMatrix) {
			matrix = data.readMATRIX()
		}
		if (hasColorTransform) {
			colorTransform = data.readCXFORMWITHALPHA()
		}
		if (hasRatio) {
			ratio = data.readUI16()
		}
		if (hasName) {
			instanceName = data.readString()
		}
		if (hasClipDepth) {
			clipDepth = data.readUI16()
		}
		if (hasClipActions) {
			clipActions = data.readCLIPACTIONS(version)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		var flags: Int = 0
		val body: SWFData = SWFData()
		if (hasMove) {
			flags = flags or 0x01
		}
		if (hasCharacter) {
			flags = flags or 0x02
		}
		if (hasMatrix) {
			flags = flags or 0x04
		}
		if (hasColorTransform) {
			flags = flags or 0x08
		}
		if (hasRatio) {
			flags = flags or 0x10
		}
		if (hasName) {
			flags = flags or 0x20
		}
		if (hasClipDepth) {
			flags = flags or 0x40
		}
		if (hasClipActions) {
			flags = flags or 0x80
		}
		body.writeUI8(flags)
		body.writeUI16(depth)
		if (hasCharacter) {
			body.writeUI16(characterId)
		}
		if (hasMatrix) {
			body.writeMATRIX(matrix!!)
		}
		if (hasColorTransform) {
			body.writeCXFORM(colorTransform!!)
		}
		if (hasRatio) {
			body.writeUI16(ratio)
		}
		if (hasName) {
			body.writeString(instanceName)
		}
		if (hasClipDepth) {
			body.writeUI16(clipDepth)
		}
		if (hasClipActions) {
			body.writeCLIPACTIONS(clipActions!!, version)
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagPlaceObject2.Companion.TYPE
	override val name = "PlaceObject2"
	override val version = 3
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"Depth: " + depth
		if (hasCharacter) {
			str += ", CharacterID: " + characterId
		}
		if (hasMatrix) {
			str += ", Matrix: " + matrix.toString()
		}
		if (hasColorTransform) {
			str += ", ColorTransform: " + colorTransform
		}
		if (hasRatio) {
			str += ", Ratio: " + ratio
		}
		if (hasName) {
			str += ", Name: " + instanceName
		}
		if (hasClipDepth) {
			str += ", ClipDepth: " + clipDepth
		}
		if (hasClipActions && clipActions != null) {
			str += "\n" + " ".repeat(indent + 2) + clipActions!!.toString(indent + 2, flags)
		}
		return str
	}
}

open class TagPlaceObject3 : com.codeazur.as3swf.tags.TagPlaceObject2(), com.codeazur.as3swf.tags.IDisplayListTag {
	companion object {
		const val TYPE = 70
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val flags1: Int = data.readUI8()
		hasClipActions = (flags1 and 0x80) != 0
		hasClipDepth = (flags1 and 0x40) != 0
		hasName = (flags1 and 0x20) != 0
		hasRatio = (flags1 and 0x10) != 0
		hasColorTransform = (flags1 and 0x08) != 0
		hasMatrix = (flags1 and 0x04) != 0
		hasCharacter = (flags1 and 0x02) != 0
		hasMove = (flags1 and 0x01) != 0
		val flags2: Int = data.readUI8()
		hasOpaqueBackground = (flags2 and 0x40) != 0
		hasVisible = (flags2 and 0x20) != 0
		hasImage = (flags2 and 0x10) != 0
		hasClassName = (flags2 and 0x08) != 0
		hasCacheAsBitmap = (flags2 and 0x04) != 0
		hasBlendMode = (flags2 and 0x02) != 0
		hasFilterList = (flags2 and 0x01) != 0
		depth = data.readUI16()
		if (hasClassName) {
			className = data.readString()
		}
		if (hasCharacter) {
			characterId = data.readUI16()
		}
		if (hasMatrix) {
			matrix = data.readMATRIX()
		}
		if (hasColorTransform) {
			colorTransform = data.readCXFORMWITHALPHA()
		}
		if (hasRatio) {
			ratio = data.readUI16()
		}
		if (hasName) {
			instanceName = data.readString()
		}
		if (hasClipDepth) {
			clipDepth = data.readUI16()
		}
		if (hasFilterList) {
			val numberOfFilters: Int = data.readUI8()
			for (i in 0 until numberOfFilters) {
				surfaceFilterList.add(data.readFILTER())
			}
		}
		if (hasBlendMode) {
			blendMode = data.readUI8()
		}
		if (hasCacheAsBitmap) {
			bitmapCache = data.readUI8()
		}
		if (hasVisible) {
			visible = data.readUI8()
		}
		if (hasOpaqueBackground) {
			bitmapBackgroundColor = data.readRGBA()
		}
		if (hasClipActions) {
			clipActions = data.readCLIPACTIONS(version)
		}
	}

	protected fun prepareBody(): SWFData {
		val body: SWFData = SWFData()
		var flags1: Int = 0
		if (hasClipActions) {
			flags1 = flags1 or 0x80
		}
		if (hasClipDepth) {
			flags1 = flags1 or 0x40
		}
		if (hasName) {
			flags1 = flags1 or 0x20
		}
		if (hasRatio) {
			flags1 = flags1 or 0x10
		}
		if (hasColorTransform) {
			flags1 = flags1 or 0x08
		}
		if (hasMatrix) {
			flags1 = flags1 or 0x04
		}
		if (hasCharacter) {
			flags1 = flags1 or 0x02
		}
		if (hasMove) {
			flags1 = flags1 or 0x01
		}
		body.writeUI8(flags1)
		var flags2: Int = 0
		if (hasOpaqueBackground) {
			flags2 = flags2 or 0x40
		}
		if (hasVisible) {
			flags2 = flags2 or 0x20
		}
		if (hasImage) {
			flags2 = flags2 or 0x10
		}
		if (hasClassName) {
			flags2 = flags2 or 0x08
		}
		if (hasCacheAsBitmap) {
			flags2 = flags2 or 0x04
		}
		if (hasBlendMode) {
			flags2 = flags2 or 0x02
		}
		if (hasFilterList) {
			flags2 = flags2 or 0x01
		}
		body.writeUI8(flags2)
		body.writeUI16(depth)
		if (hasClassName) {
			body.writeString(className)
		}
		if (hasCharacter) {
			body.writeUI16(characterId)
		}
		if (hasMatrix) {
			body.writeMATRIX(matrix!!)
		}
		if (hasColorTransform) {
			body.writeCXFORM(colorTransform!!)
		}
		if (hasRatio) {
			body.writeUI16(ratio)
		}
		if (hasName) {
			body.writeString(instanceName)
		}
		if (hasClipDepth) {
			body.writeUI16(clipDepth)
		}
		if (hasFilterList) {
			val numberOfFilters: Int = surfaceFilterList.size
			body.writeUI8(numberOfFilters)
			for (i in 0 until numberOfFilters) {
				body.writeFILTER(surfaceFilterList[i])
			}
		}
		if (hasBlendMode) {
			body.writeUI8(blendMode)
		}
		if (hasCacheAsBitmap) {
			body.writeUI8(bitmapCache)
		}
		if (hasVisible) {
			body.writeUI8(visible)
		}
		if (hasOpaqueBackground) {
			body.writeRGBA(bitmapBackgroundColor)
		}
		if (hasClipActions) {
			body.writeCLIPACTIONS(clipActions!!, version)
		}

		return body
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = prepareBody()

		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagPlaceObject3.Companion.TYPE
	override val name = "PlaceObject3"
	override val version = 8
	override val level = 3

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"Depth: " + depth
		if (hasClassName /*|| (hasImage && hasCharacter)*/) {
			str += ", ClassName: " + className
		}
		if (hasCharacter) {
			str += ", CharacterID: " + characterId
		}
		if (hasMatrix) {
			str += ", Matrix: " + matrix.toString()
		}
		if (hasColorTransform) {
			str += ", ColorTransform: " + colorTransform
		}
		if (hasRatio) {
			str += ", Ratio: " + ratio
		}
		if (hasName) {
			str += ", Name: " + instanceName
		}
		if (hasClipDepth) {
			str += ", ClipDepth: " + clipDepth
		}
		if (hasBlendMode) {
			str += ", BlendMode: " + com.codeazur.as3swf.data.consts.BlendMode.toString(blendMode)
		}
		if (hasCacheAsBitmap) {
			str += ", CacheAsBitmap: " + bitmapCache
		}
		if (hasVisible) {
			str += ", Visible: " + visible
		}
		if (hasOpaqueBackground) {
			str += ", BackgroundColor: " + ColorUtils.rgbaToString(bitmapBackgroundColor)
		}
		if (hasFilterList) {
			str += "\n" + " ".repeat(indent + 2) + "Filters:"
			for (i in 0 until surfaceFilterList.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + surfaceFilterList[i].toString(indent + 4)
			}
		}
		if (hasClipActions) {
			str += "\n" + " ".repeat(indent + 2) + clipActions!!.toString(indent + 2)
		}
		return str
	}
}

/**
 * PlaceObject4 is essentially identical to PlaceObject3 except it has a different
 * swf tag value of course (94 instead of 70) and at the end of the tag, if there are
 * additional bytes, those bytes will be interpreted as AMF binary data that will be
 * used as the metadata attached to the instance.
 *
 * @see http://help.adobe.com/en_US/FlashPlatform/reference/actionscript/3/flash/display/DisplayObject.html#metaData
 */
class TagPlaceObject4 : com.codeazur.as3swf.tags.TagPlaceObject3(), com.codeazur.as3swf.tags.IDisplayListTag {
	companion object {
		const val TYPE = 94
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		super.parse(data, length, version, async)
		if (data.bytesAvailable > 0) {
			metaData = data.readObject()
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = prepareBody()

		if (metaData != null) {
			body.writeObject(metaData)
		}

		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagPlaceObject4.Companion.TYPE
	override val name = "PlaceObject4"
	override val version = 19
	override val level = 4

	override fun toString(indent: Int, flags: Int): String {
		var str: String = super.toString(indent, 0)
		if (metaData != null) {
			str += "\n" + " ".repeat(indent + 2) + "MetaData: yes"
		}
		return str
	}
}

class TagProductInfo : _BaseTag(), ITag {
	companion object {
		const val TYPE = 41
	}

	var productId: Int = 0
	var edition: Int = 0
	var majorVersion: Int = 0
	var minorVersion: Int = 0
	var build: Long = 0L
	lateinit var compileDate: java.util.Date

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		productId = data.readUI32()
		edition = data.readUI32()
		majorVersion = data.readUI8()
		minorVersion = data.readUI8()

		build = data.readUI32().toLong() + data.readUI32().toLong() shl 32
		val sec: Long = data.readUI32().toLong() + data.readUI32().toLong() shl 32
		compileDate = java.util.Date(sec)
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI32(productId)
		body.writeUI32(edition)
		body.writeUI8(majorVersion)
		body.writeUI8(minorVersion)
		body.writeUI32((build.toLong() ushr 0).toInt())
		body.writeUI32((build.toLong() ushr 32).toInt())
		body.writeUI32((compileDate.time ushr 0).toInt())
		body.writeUI32((compileDate.time ushr 32).toInt())
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagProductInfo.Companion.TYPE
	override val name = "ProductInfo"
	override val version = 3
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"ProductID: " + productId + ", " +
			"Edition: " + edition + ", " +
			"Version: " + majorVersion + "." + minorVersion + " r" + build + ", " +
			"CompileDate: " + compileDate.toString()
	}
}

class TagProtect : _BaseTag(), ITag {
	companion object {
		const val TYPE = 24
	}

	protected var password = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		if (length > 0) {
			data.readBytes(password, 0, length)
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, password.length)
		if (password.length > 0) {
			data.writeBytes(password)
		}
	}

	override val type = com.codeazur.as3swf.tags.TagProtect.Companion.TYPE
	override val name = "Protect"
	override val version = 2
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
	}
}

open class TagRemoveObject : _BaseTag(), com.codeazur.as3swf.tags.IDisplayListTag {
	companion object {
		const val TYPE = 5
	}

	var characterId: Int = 0
	var depth: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		depth = data.readUI16()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, 4)
		data.writeUI16(characterId)
		data.writeUI16(depth)
	}

	override val type = com.codeazur.as3swf.tags.TagRemoveObject.Companion.TYPE
	override val name = "RemoveObject"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"CharacterID: " + characterId + ", " +
			"Depth: " + depth
	}
}

class TagRemoveObject2 : com.codeazur.as3swf.tags.TagRemoveObject(), com.codeazur.as3swf.tags.IDisplayListTag {
	companion object {
		const val TYPE = 28
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		depth = data.readUI16()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, 2)
		data.writeUI16(depth)
	}

	override val type = com.codeazur.as3swf.tags.TagRemoveObject2.Companion.TYPE
	override val name = "RemoveObject2"
	override val version = 3
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"Depth: " + depth
	}
}

class TagScriptLimits : _BaseTag(), ITag {
	companion object {
		const val TYPE = 65
	}

	var maxRecursionDepth: Int = 0
	var scriptTimeoutSeconds: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		maxRecursionDepth = data.readUI16()
		scriptTimeoutSeconds = data.readUI16()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, 4)
		data.writeUI16(maxRecursionDepth)
		data.writeUI16(scriptTimeoutSeconds)
	}

	override val type = com.codeazur.as3swf.tags.TagScriptLimits.Companion.TYPE
	override val name = "ScriptLimits"
	override val version = 7
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"MaxRecursionDepth: " + maxRecursionDepth + ", " +
			"ScriptTimeoutSeconds: " + scriptTimeoutSeconds
	}
}

class TagSetBackgroundColor : _BaseTag(), ITag {
	companion object {
		const val TYPE = 9

		fun create(aColor: Int = 0xffffff): com.codeazur.as3swf.tags.TagSetBackgroundColor {
			val setBackgroundColor: com.codeazur.as3swf.tags.TagSetBackgroundColor = com.codeazur.as3swf.tags.TagSetBackgroundColor()
			setBackgroundColor.color = aColor
			return setBackgroundColor
		}
	}

	var color: Int = 0xffffff

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		color = data.readRGB()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, 3)
		data.writeRGB(color)
	}

	override val type = com.codeazur.as3swf.tags.TagSetBackgroundColor.Companion.TYPE
	override val name = "SetBackgroundColor"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) + "Color: " + ColorUtils.rgbToString(color)
	}
}

class TagSetTabIndex : _BaseTag(), ITag {
	companion object {
		const val TYPE = 66
	}

	var depth: Int = 0
	var tabIndex: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		depth = data.readUI16()
		tabIndex = data.readUI16()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, 4)
		data.writeUI16(depth)
		data.writeUI16(tabIndex)
	}

	override val type = com.codeazur.as3swf.tags.TagSetTabIndex.Companion.TYPE
	override val name = "SetTabIndex"
	override val version = 7
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"Depth: " + depth + ", " +
			"TabIndex: " + tabIndex
	}
}

class TagShowFrame : _BaseTag(), com.codeazur.as3swf.tags.IDisplayListTag {
	companion object {
		const val TYPE = 1
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		// Do nothing. The End tag has no body.
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, 0)
	}

	override val type = com.codeazur.as3swf.tags.TagShowFrame.Companion.TYPE
	override val name = "ShowFrame"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
	}
}

class TagSoundStreamBlock : _BaseTag(), ITag {
	companion object {
		const val TYPE = 19
	}

	var soundData = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		data.readBytes(soundData, 0, length)
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, soundData.length, true)
		if (soundData.length > 0) {
			data.writeBytes(soundData)
		}
	}

	override val type = com.codeazur.as3swf.tags.TagSoundStreamBlock.Companion.TYPE
	override val name = "SoundStreamBlock"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) + "Length: " + soundData.length
	}
}

open class TagSoundStreamHead : _BaseTag(), ITag {
	companion object {
		const val TYPE = 18
	}

	var playbackSoundRate: Int = 0
	var playbackSoundSize: Int = 0
	var playbackSoundType: Int = 0
	var streamSoundCompression: Int = 0
	var streamSoundRate: Int = 0
	var streamSoundSize: Int = 0
	var streamSoundType: Int = 0
	var streamSoundSampleCount: Int = 0
	var latencySeek: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		data.readUB(4)
		playbackSoundRate = data.readUB(2)
		playbackSoundSize = data.readUB(1)
		playbackSoundType = data.readUB(1)
		streamSoundCompression = data.readUB(4)
		streamSoundRate = data.readUB(2)
		streamSoundSize = data.readUB(1)
		streamSoundType = data.readUB(1)
		streamSoundSampleCount = data.readUI16()
		if (streamSoundCompression == com.codeazur.as3swf.data.consts.SoundCompression.MP3) {
			latencySeek = data.readSI16()
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUB(4, 0)
		body.writeUB(2, playbackSoundRate)
		body.writeUB(1, playbackSoundSize)
		body.writeUB(1, playbackSoundType)
		body.writeUB(4, streamSoundCompression)
		body.writeUB(2, streamSoundRate)
		body.writeUB(1, streamSoundSize)
		body.writeUB(1, streamSoundType)
		body.writeUI16(streamSoundSampleCount)
		if (streamSoundCompression == com.codeazur.as3swf.data.consts.SoundCompression.MP3) {
			body.writeSI16(latencySeek)
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagSoundStreamHead.Companion.TYPE
	override val name = "SoundStreamHead"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
		if (streamSoundSampleCount > 0) {
			str += "Format: " + com.codeazur.as3swf.data.consts.SoundCompression.toString(streamSoundCompression) + ", " +
				"Rate: " + com.codeazur.as3swf.data.consts.SoundRate.toString(streamSoundRate) + ", " +
				"Size: " + com.codeazur.as3swf.data.consts.SoundSize.toString(streamSoundSize) + ", " +
				"Type: " + com.codeazur.as3swf.data.consts.SoundType.toString(streamSoundType) + ", "
		}
		str += "Samples: " + streamSoundSampleCount + ", "
		str += "LatencySeek: " + latencySeek
		return str
	}
}

class TagSoundStreamHead2 : com.codeazur.as3swf.tags.TagSoundStreamHead(), ITag {
	companion object {
		const val TYPE = 45
	}

	override val type = com.codeazur.as3swf.tags.TagSoundStreamHead2.Companion.TYPE
	override val name = "SoundStreamHead2"
	override val version = 3
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
		if (streamSoundSampleCount > 0) {
			str += "Format: " + com.codeazur.as3swf.data.consts.SoundCompression.toString(streamSoundCompression) + ", " +
				"Rate: " + com.codeazur.as3swf.data.consts.SoundRate.toString(streamSoundRate) + ", " +
				"Size: " + com.codeazur.as3swf.data.consts.SoundSize.toString(streamSoundSize) + ", " +
				"Type: " + com.codeazur.as3swf.data.consts.SoundType.toString(streamSoundType) + ", "
		}
		str += "Samples: " + streamSoundSampleCount
		return str
	}
}

class TagStartSound : _BaseTag(), ITag {
	companion object {
		const val TYPE = 15
	}

	var soundId: Int = 0
	lateinit var soundInfo: com.codeazur.as3swf.data.SWFSoundInfo

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		soundId = data.readUI16()
		soundInfo = data.readSOUNDINFO()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeUI16(soundId)
		body.writeSOUNDINFO(soundInfo)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagStartSound.Companion.TYPE
	override val name = "StartSound"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"SoundID: " + soundId + ", " +
			"SoundInfo: " + soundInfo
		return str
	}
}

class TagStartSound2 : _BaseTag(), ITag {
	companion object {
		const val TYPE = 89
	}

	lateinit var soundClassName: String
	lateinit var soundInfo: com.codeazur.as3swf.data.SWFSoundInfo

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		soundClassName = data.readString()
		soundInfo = data.readSOUNDINFO()
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		body.writeString(soundClassName)
		body.writeSOUNDINFO(soundInfo)
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagStartSound2.Companion.TYPE
	override val name = "StartSound2"
	override val version = 9
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		val str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"SoundClassName: " + soundClassName + ", " +
			"SoundInfo: " + soundInfo
		return str
	}
}

class TagSymbolClass : _BaseTag(), ITag {
	companion object {
		const val TYPE = 76
	}

	val symbols = java.util.ArrayList<com.codeazur.as3swf.data.SWFSymbol>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val numSymbols: Int = data.readUI16()
		for (i in 0 until numSymbols) {
			symbols.add(data.readSYMBOL())
		}
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		val body: SWFData = SWFData()
		val numSymbols: Int = symbols.size
		body.writeUI16(numSymbols)
		for (i in 0 until numSymbols) {
			body.writeSYMBOL(symbols[i])
		}
		data.writeTagHeader(type, body.length)
		data.writeBytes(body)
	}

	override val type = com.codeazur.as3swf.tags.TagSymbolClass.Companion.TYPE
	override val name = "SymbolClass"
	override val version = 9 // educated guess (not specified in SWF10 spec)
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
		if (symbols.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Symbols:"
			for (i in 0 until symbols.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + symbols[i].toString()
			}
		}
		return str
	}
}

open class TagUnknown(override val type: Int = 0) : _BaseTag(), ITag {
	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		data.skipBytes(length)
	}

	suspend override fun publish(data: SWFData, version: Int) {
		throw Error("No raw tag data available.")
	}

	override val name = "????"
	override val version = 0
	override val level = 1

	override fun toString(indent: Int, flags: Int) = com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent)
}

class TagVideoFrame : _BaseTag(), ITag {
	companion object {
		const val TYPE = 61
	}

	var streamId: Int = 0
	var frameNum: Int = 0

	protected var _videoData = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		streamId = data.readUI16()
		frameNum = data.readUI16()
		data.readBytes(_videoData, 0, length - 4)
	}

	suspend override fun publish(data: SWFData, version: Int): Unit {
		data.writeTagHeader(type, _videoData.length + 4)
		data.writeUI16(streamId)
		data.writeUI16(frameNum)
		if (_videoData.length > 0) {
			data.writeBytes(_videoData)
		}
	}

	override val type = com.codeazur.as3swf.tags.TagVideoFrame.Companion.TYPE
	override val name = "VideoFrame"
	override val version = 6
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return com.codeazur.as3swf.tags.Tag.Companion.toStringCommon(type, name, indent) +
			"StreamID: " + streamId + ", " +
			"Frame: " + frameNum
	}
}
