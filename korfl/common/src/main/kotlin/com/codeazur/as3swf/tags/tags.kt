package com.codeazur.as3swf.tags

import com.codeazur.as3swf.SWFData
import com.codeazur.as3swf.data.*
import com.codeazur.as3swf.data.actions.Action
import com.codeazur.as3swf.data.actions.ActionExecutionContext
import com.codeazur.as3swf.data.actions.IAction
import com.codeazur.as3swf.data.consts.*
import com.codeazur.as3swf.data.etc.MPEGFrame
import com.codeazur.as3swf.data.filters.IFilter
import com.codeazur.as3swf.exporters.ShapeExporter
import com.codeazur.as3swf.utils.ColorUtils
import com.codeazur.as3swf.utils.FlashByteArray
import com.codeazur.as3swf.utils.toFlash
import com.soywiz.klock.DateTime
import com.soywiz.korfl.abc.ABC
import com.soywiz.korio.lang.format
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.stream.readBytes
import com.soywiz.korio.util.nextAlignedTo
import com.soywiz.korio.util.toString

interface ITag {
	val type: Int
	val name: String
	val version: Int
	val level: Int

	suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean = false)
	fun toString(indent: Int = 0, flags: Int = 0): String
}

abstract class _BaseTag : ITag {
	override abstract fun toString(indent: Int, flags: Int): String
	override fun toString() = toString(0, 0)
}

interface IDefinitionTag : ITag {
	var characterId: Int
}

interface IDisplayListTag : ITag

class Tag {
	companion object {
		fun toStringCommon(type: Int, name: String, indent: Int = 0): String = " ".repeat(indent) + "[" + "%02d".format(type) + ":" + name + "] "
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

	override val type = TagCSMTextSettings.TYPE
	override val name = "CSMTextSettings"
	override val version = 8
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
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

	protected var uuid = ByteArray(0)

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		if (length > 0) uuid = data.data.readBytes(length)
	}

	override val type = TagDebugID.TYPE
	override val name = "DebugID"
	override val version = 6
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) + "UUID: "
		if (uuid.size == 16) {
			str += "%02x%02x%02x%02x-".format(uuid[0], uuid[1], uuid[2], uuid[3])
			str += "%02x%02x-".format(uuid[4], uuid[5])
			str += "%02x%02x-".format(uuid[6], uuid[7])
			str += "%02x%02x-".format(uuid[8], uuid[9])
			str += "%02x%02x%02x%02x%02x%02x".format(uuid[10], uuid[11], uuid[12], uuid[13], uuid[14], uuid[15])
		} else {
			str += "(invalid length: " + uuid.size + ")"
		}
		return str
	}
}

class TagDefineBinaryData : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 87
	}

	override var characterId: Int = 0

	var binaryData = ByteArray(0)

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		data.readUI32() // reserved, always 0
		if (length > 6) binaryData = data.readBytes(length - 6)
	}

	override val type = TagDefineBinaryData.TYPE
	override val name = "DefineBinaryData"
	override val version = 9
	override val level = 1

	override fun toString(indent: Int, flags: Int): String = Tag.toStringCommon(type, name, indent) + "ID: " + characterId + ", " + "Length: " + binaryData.size
}

open class TagDefineBits : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 6
	}

	var bitmapType: Int = BitmapType.JPEG

	override var characterId: Int = 0

	var bitmapData = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		if (length > 2) bitmapData = FlashByteArray(data.readBytes(length - 2))
	}

	override val type = TagDefineBits.TYPE
	override val name = "DefineBits"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) + "ID: " + characterId + ", " + "BitmapLength: " + bitmapData.length
	}
}

open class TagDefineBitsJPEG2 : TagDefineBits(), IDefinitionTag {
	companion object {
		const val TYPE = 21
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		super.parse(data, length, version, async)
		if (bitmapData[0] == 0xff && (bitmapData[1] == 0xd8 || bitmapData[1] == 0xd9)) {
			bitmapType = BitmapType.JPEG
		} else if (bitmapData[0] == 0x89 && bitmapData[1] == 0x50 && bitmapData[2] == 0x4e && bitmapData[3] == 0x47 && bitmapData[4] == 0x0d && bitmapData[5] == 0x0a && bitmapData[6] == 0x1a && bitmapData[7] == 0x0a) {
			bitmapType = BitmapType.PNG
		} else if (bitmapData[0] == 0x47 && bitmapData[1] == 0x49 && bitmapData[2] == 0x46 && bitmapData[3] == 0x38 && bitmapData[4] == 0x39 && bitmapData[5] == 0x61) {
			bitmapType = BitmapType.GIF89A
		}
	}

	override val type = TagDefineBitsJPEG2.TYPE
	override val name = "DefineBitsJPEG2"
	override val version = if (bitmapType == BitmapType.JPEG) 2 else 8
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Type: " + BitmapType.toString(bitmapType) + ", " +
			"BitmapLength: " + bitmapData.length
		return str
	}
}

open class TagDefineBitsJPEG3 : TagDefineBitsJPEG2(), IDefinitionTag {
	companion object {
		const val TYPE = 35
	}

	var bitmapAlphaData = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		val alphaDataOffset: Int = data.readUI32()
		bitmapData = data.readBytes(alphaDataOffset).toFlash()
		if (bitmapData[0] == 0xff && (bitmapData[1] == 0xd8 || bitmapData[1] == 0xd9)) {
			bitmapType = BitmapType.JPEG
		} else if (bitmapData[0] == 0x89 && bitmapData[1] == 0x50 && bitmapData[2] == 0x4e && bitmapData[3] == 0x47 && bitmapData[4] == 0x0d && bitmapData[5] == 0x0a && bitmapData[6] == 0x1a && bitmapData[7] == 0x0a) {
			bitmapType = BitmapType.PNG
		} else if (bitmapData[0] == 0x47 && bitmapData[1] == 0x49 && bitmapData[2] == 0x46 && bitmapData[3] == 0x38 && bitmapData[4] == 0x39 && bitmapData[5] == 0x61) {
			bitmapType = BitmapType.GIF89A
		}
		val alphaDataSize: Int = length - alphaDataOffset - 6
		if (alphaDataSize > 0) {
			bitmapAlphaData = data.readBytes(alphaDataSize).toFlash()
		}
	}

	override val type = TagDefineBitsJPEG3.TYPE
	override val name = "DefineBitsJPEG3"
	override val version = if (bitmapType == BitmapType.JPEG) 3 else 8
	override val level = 3

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Type: " + BitmapType.toString(bitmapType) + ", " +
			"HasAlphaData: " + (bitmapAlphaData.length > 0) + ", " +
			(if (bitmapAlphaData.length > 0) "BitmapAlphaLength: " + bitmapAlphaData.length + ", " else "") +
			"BitmapLength: " + bitmapData.length
		return str
	}
}

class TagDefineBitsJPEG4 : TagDefineBitsJPEG3(), IDefinitionTag {
	companion object {
		const val TYPE = 90
	}

	var deblockParam: Double = 0.0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		val alphaDataOffset: Int = data.readUI32()
		deblockParam = data.readFIXED8()
		bitmapData = data.readBytes(alphaDataOffset).toFlash()
		if (bitmapData[0] == 0xff && (bitmapData[1] == 0xd8 || bitmapData[1] == 0xd9)) {
			bitmapType = BitmapType.JPEG
		} else if (bitmapData[0] == 0x89 && bitmapData[1] == 0x50 && bitmapData[2] == 0x4e && bitmapData[3] == 0x47 && bitmapData[4] == 0x0d && bitmapData[5] == 0x0a && bitmapData[6] == 0x1a && bitmapData[7] == 0x0a) {
			bitmapType = BitmapType.PNG
		} else if (bitmapData[0] == 0x47 && bitmapData[1] == 0x49 && bitmapData[2] == 0x46 && bitmapData[3] == 0x38 && bitmapData[4] == 0x39 && bitmapData[5] == 0x61) {
			bitmapType = BitmapType.GIF89A
		}
		val alphaDataSize: Int = length - alphaDataOffset - 6
		if (alphaDataSize > 0) {
			bitmapAlphaData = data.readBytes(alphaDataSize).toFlash()
		}
	}

	override val type = TagDefineBitsJPEG4.TYPE
	override val name = "DefineBitsJPEG4"
	override val version = 10
	override val level = 4

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Type: " + BitmapType.toString(bitmapType) + ", " +
			"DeblockParam: " + deblockParam + ", " +
			"HasAlphaData: " + (bitmapAlphaData.length > 0) + ", " +
			(if (bitmapAlphaData.length > 0) "BitmapAlphaLength: " + bitmapAlphaData.length + ", " else "") +
			"BitmapLength: " + bitmapData.length
		return str
	}
}

open class TagDefineBitsLossless : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 20
	}

	open val hasAlpha = false
	var bitmapFormat: BitmapFormat = BitmapFormat.UNKNOWN
	var bitmapWidth: Int = 0
	var bitmapHeight: Int = 0
	var bitmapColorTableSizeM1: Int = 0
	val bitmapColorTableSize: Int get() = bitmapColorTableSizeM1 + 1

	val bytesPerPixel: Int
		get() = when (bitmapFormat) {
			BitmapFormat.BIT_8 -> 1
			BitmapFormat.BIT_15 -> 2
			BitmapFormat.BIT_24_32 -> 4
			BitmapFormat.UNKNOWN -> 1
		}

	val alignment: Int
		get() = when (bitmapFormat) {
			BitmapFormat.BIT_8 -> 4
			BitmapFormat.BIT_15 -> 2
			BitmapFormat.BIT_24_32 -> 1
			BitmapFormat.UNKNOWN -> 1
		}

	val actualWidth: Int get() = bitmapWidth.nextAlignedTo(alignment)
	val actualHeight: Int get() = bitmapHeight.nextAlignedTo(alignment)

	override var characterId: Int = 0

	var zlibBitmapData = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		val rawFormat = data.readUI8()
		bitmapFormat = BitmapFormat[rawFormat]
		bitmapWidth = data.readUI16()
		bitmapHeight = data.readUI16()
		if (bitmapFormat == BitmapFormat.BIT_8) bitmapColorTableSizeM1 = data.readUI8()
		zlibBitmapData = data.readBytes(length - (if (bitmapFormat == BitmapFormat.BIT_8) 8 else 7)).toFlash()
		//zlibBitmapData = data.readBytes(data.bytesAvailable).toFlash()
	}

	override val type = TagDefineBitsLossless.TYPE
	override val name = "DefineBitsLossless"
	override val version = 2
	override val level = 1

	override fun toString(indent: Int, flags: Int) = "${Tag.toStringCommon(type, name, indent)}ID: $characterId, Format: $bitmapFormat, Size: ($bitmapWidth,$bitmapHeight)"
}

class TagDefineBitsLossless2 : TagDefineBitsLossless(), IDefinitionTag {
	companion object {
		const val TYPE = 36
	}

	override val hasAlpha = true
	override val type = TagDefineBitsLossless2.TYPE
	override val name = "DefineBitsLossless2"
	override val version = 3
	override val level = 2

	override fun toString(indent: Int, flags: Int): String = "${Tag.toStringCommon(type, name, indent)}ID: $characterId, Format: $bitmapFormat, Size: ($bitmapWidth,$bitmapHeight)"
}

class TagDefineButton : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 7
		val STATE_UP = "up"
		val STATE_OVER = "over"
		val STATE_DOWN = "down"
		val STATE_HIT = "hit"
	}

	override var characterId: Int = 0

	protected var characters = ArrayList<SWFButtonRecord>()
	protected var actions = ArrayList<IAction>()

	protected var frames = hashMapOf<String, ArrayList<SWFButtonRecord>>()

	protected var labelCount: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		var record: SWFButtonRecord?
		while (true) {
			record = data.readBUTTONRECORD()
			if (record == null) break
			characters.add(record)
		}
		var action: IAction?
		while (true) {
			action = data.readACTIONRECORD()
			if (action == null) break
			actions.add(action)
		}
		labelCount = Action.resolveOffsets(actions)
		processRecords()
	}

	fun getRecordsByState(state: String): ArrayList<SWFButtonRecord> {
		return frames[state] as ArrayList<SWFButtonRecord>
	}

	override val type = TagDefineButton.TYPE
	override val name = "DefineButton"
	override val version = 1
	override val level = 1

	protected fun processRecords(): Unit {
		val upState = ArrayList<SWFButtonRecord>()
		val overState = ArrayList<SWFButtonRecord>()
		val downState = ArrayList<SWFButtonRecord>()
		val hitState = ArrayList<SWFButtonRecord>()
		for (i in 0 until characters.size) {
			val record = characters[i]
			if (record.stateUp) upState.add(record)
			if (record.stateOver) overState.add(record)
			if (record.stateDown) downState.add(record)
			if (record.stateHitTest) hitState.add(record)
		}
		frames[TagDefineButton.STATE_UP] = ArrayList(upState.sortedBy { it.placeDepth })
		frames[TagDefineButton.STATE_OVER] = ArrayList(overState.sortedBy { it.placeDepth })
		frames[TagDefineButton.STATE_DOWN] = ArrayList(downState.sortedBy { it.placeDepth })
		frames[TagDefineButton.STATE_HIT] = ArrayList(hitState.sortedBy { it.placeDepth })
	}

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) +
			"ID: " + characterId
		if (characters.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Characters:"
			for (i in 0 until characters.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + characters[i].toString(indent + 4)
			}
		}
		if (actions.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Actions:"
			if ((flags and com.codeazur.as3swf.SWF.TOSTRING_FLAG_AVM1_BYTECODE) == 0) {
				for (i in 0 until actions.size) {
					str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + actions[i].toString(indent + 4)
				}
			} else {
				val context: ActionExecutionContext = ActionExecutionContext(actions, arrayListOf(), labelCount)
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

open class TagDefineButton2 : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 34
	}

	var trackAsMenu: Boolean = false

	override var characterId: Int = 0

	var characters = ArrayList<SWFButtonRecord>()
	protected var condActions = ArrayList<SWFButtonCondAction>()

	protected var frames = hashMapOf<String, ArrayList<SWFButtonRecord>>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		trackAsMenu = ((data.readUI8() and 0x01) != 0)
		val actionOffset: Int = data.readUI16()
		var record: SWFButtonRecord?
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

	fun getRecordsByState(state: String): ArrayList<SWFButtonRecord> {
		return frames[state]!!
	}

	override val type = TagDefineButton2.TYPE
	override val name = "DefineButton2"
	override val version = 3
	override val level = 2

	protected fun processRecords(): Unit {
		val upState: ArrayList<SWFButtonRecord> = ArrayList<SWFButtonRecord>()
		val overState: ArrayList<SWFButtonRecord> = ArrayList<SWFButtonRecord>()
		val downState: ArrayList<SWFButtonRecord> = ArrayList<SWFButtonRecord>()
		val hitState: ArrayList<SWFButtonRecord> = ArrayList<SWFButtonRecord>()
		for (i in 0 until characters.size) {
			val record: SWFButtonRecord = characters[i]
			if (record.stateUp) upState.add(record)
			if (record.stateOver) overState.add(record)
			if (record.stateDown) downState.add(record)
			if (record.stateHitTest) hitState.add(record)
		}
		frames[TagDefineButton.STATE_UP] = ArrayList(upState.sortedBy { it.placeDepth })
		frames[TagDefineButton.STATE_OVER] = ArrayList(overState.sortedBy { it.placeDepth })
		frames[TagDefineButton.STATE_DOWN] = ArrayList(downState.sortedBy { it.placeDepth })
		frames[TagDefineButton.STATE_HIT] = ArrayList(hitState.sortedBy { it.placeDepth })
	}

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) +
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

class TagDefineButtonCxform : _BaseTag(), IDefinitionTag {
	companion object {
		val TYPE = 23
	}

	lateinit var buttonColorTransform: SWFColorTransform

	override var characterId: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		buttonColorTransform = data.readCXFORM()
	}

	override val type = TagDefineButtonCxform.TYPE
	override val name = "DefineButtonCxform"
	override val version = 2
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"ColorTransform: " + buttonColorTransform
		return str
	}
}

class TagDefineButtonSound : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 17
	}

	var buttonSoundChar0: Int = 0
	var buttonSoundChar1: Int = 0
	var buttonSoundChar2: Int = 0
	var buttonSoundChar3: Int = 0
	lateinit var buttonSoundInfo0: SWFSoundInfo
	lateinit var buttonSoundInfo1: SWFSoundInfo
	lateinit var buttonSoundInfo2: SWFSoundInfo
	lateinit var buttonSoundInfo3: SWFSoundInfo

	override var characterId: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		buttonSoundChar0 = data.readUI16()
		if (buttonSoundChar0 != 0) buttonSoundInfo0 = data.readSOUNDINFO()
		buttonSoundChar1 = data.readUI16()
		if (buttonSoundChar1 != 0) buttonSoundInfo1 = data.readSOUNDINFO()
		buttonSoundChar2 = data.readUI16()
		if (buttonSoundChar2 != 0) buttonSoundInfo2 = data.readSOUNDINFO()
		buttonSoundChar3 = data.readUI16()
		if (buttonSoundChar3 != 0) buttonSoundInfo3 = data.readSOUNDINFO()
	}

	override val type = TagDefineButtonSound.TYPE
	override val name = "DefineButtonSound"
	override val version = 2
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) +
			"ButtonID: " + characterId + ", " +
			"ButtonSoundChars: " + buttonSoundChar0 + "," + buttonSoundChar1 + "," + buttonSoundChar2 + "," + buttonSoundChar3
		return str
	}
}

class TagDefineEditText : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 37
	}

	lateinit var bounds: SWFRectangle
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

	override val type = TagDefineEditText.TYPE
	override val name = "DefineEditText"
	override val version = 4
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			(if (hasText && initialText!!.isNotEmpty()) "Text: " + initialText + ", " else "") +
			(if (variableName!!.isNotEmpty()) "VariableName: " + variableName + ", " else "") +
			"Bounds: " + bounds
		return str
	}
}

open class TagDefineFont : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 10
	}

	override var characterId = 0
	var glyphShapeTable = ArrayList<SWFShape>()

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

	fun export(handler: ShapeExporter, glyphIndex: Int): Unit {
		glyphShapeTable[glyphIndex].export(handler)
	}

	override val type = TagDefineFont.TYPE
	override val name = "DefineFont"
	override val version = 1
	override val level = 1

	protected open val unitDivisor = 1.0

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) +
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

open class TagDefineFont2 : TagDefineFont(), IDefinitionTag {
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

	val codeTable = ArrayList<Int>()
	val fontAdvanceTable = ArrayList<Int>()
	val fontBoundsTable = ArrayList<SWFRectangle>()
	val fontKerningTable = ArrayList<SWFKerningRecord>()

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
		fontName = data.readUTFBytes(fontNameLen)
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

	override val type = TagDefineFont2.TYPE
	override val name = "DefineFont2"
	override val version = 3
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) +
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
				val rect: SWFRectangle = fontBoundsTable[i]
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

class TagDefineFont3 : TagDefineFont2(), IDefinitionTag {
	companion object {
		const val TYPE = 75
	}

	override val type = TagDefineFont3.TYPE
	override val name = "DefineFont3"
	override val version = 8
	override val level = 2

	override val unitDivisor = 20.0

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"FontName: " + fontName + ", " +
			"Italic: " + italic + ", " +
			"Bold: " + bold + ", " +
			"Glyphs: " + glyphShapeTable.size
		return str + toStringCommon(indent)
	}
}

class TagDefineFont4 : _BaseTag(), IDefinitionTag {
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
			fontData = data.readBytes(length - (data.position - pos)).toFlash()
		}
	}

	override val type = TagDefineFont4.TYPE
	override val name = "DefineFont4"
	override val version = 10
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) +
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

	protected var _zoneTable = ArrayList<SWFZoneRecord>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		fontId = data.readUI16()
		csmTableHint = (data.readUI8() ushr 6)
		val recordsEndPos: Int = data.position + length - 3
		while (data.position < recordsEndPos) {
			_zoneTable.add(data.readZONERECORD())
		}
	}

	override val type = TagDefineFontAlignZones.TYPE
	override val name = "DefineFontAlignZones"
	override val version = 8
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) +
			"FontID: " + fontId + ", " +
			"CSMTableHint: " + CSMTableHint.toString(csmTableHint) + ", " +
			"Records: " + _zoneTable.size
		for (i in 0 until _zoneTable.size) {
			str += "\n" + " ".repeat(indent + 2) + "[" + i + "] " + _zoneTable[i].toString()
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

	protected var _codeTable = ArrayList<Int>()

	protected var langCodeLength: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		fontId = data.readUI16()

		val fontNameLen: Int = data.readUI8()
		fontName = data.readUTFBytes(fontNameLen)

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

	protected open fun parseLangCode(data: SWFData): Unit {
		// Does nothing here.
		// Overridden in TagDefineFontInfo2, where it:
		// - reads langCode
		// - sets langCodeLength to 1
	}

	override val type = TagDefineFontInfo.TYPE
	override val name = "DefineFontInfo"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
			"FontID: " + fontId + ", " +
			"FontName: " + fontName + ", " +
			"Italic: " + italic + ", " +
			"Bold: " + bold + ", " +
			"Codes: " + _codeTable.size
	}
}

class TagDefineFontInfo2 : TagDefineFontInfo(), ITag {
	companion object {
		const val TYPE = 62
	}

	override fun parseLangCode(data: SWFData): Unit {
		langCode = data.readUI8()
		langCodeLength = 1
	}

	override val type = TagDefineFontInfo2.TYPE
	override val name = "DefineFontInfo2"
	override val version = 6
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
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

	override val type = TagDefineFontName.TYPE
	override val name = "DefineFontName"
	override val version = 9
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
			"FontID: " + fontId + ", " +
			"Name: " + fontName + ", " +
			"Copyright: " + fontCopyright
	}
}

//interface IDefineBaseShape : IDefinitionTag

open class TagDefineMorphShape : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 46
	}

	lateinit var startBounds: SWFRectangle
	lateinit var endBounds: SWFRectangle
	lateinit var startEdges: SWFShape
	lateinit var endEdges: SWFShape
	var startEdgeBounds: SWFRectangle = SWFRectangle()
	var endEdgeBounds: SWFRectangle = SWFRectangle()

	override var characterId: Int = 0

	protected var morphFillStyles = ArrayList<SWFMorphFillStyle>()
	protected var morphLineStyles = ArrayList<SWFMorphLineStyle>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		startBounds = data.readRECT()
		endBounds = data.readRECT()
		var offset: Int = data.readUI32()
		// MorphFillStyleArray
		var fillStyleCount: Int = data.readUI8()
		if (fillStyleCount == 0xff) fillStyleCount = data.readUI16()
		for (i in 0 until fillStyleCount) {
			morphFillStyles.add(data.readMORPHFILLSTYLE())
		}
		// MorphLineStyleArray
		var lineStyleCount: Int = data.readUI8()
		if (lineStyleCount == 0xff) lineStyleCount = data.readUI16()
		for (i in 0 until lineStyleCount) morphLineStyles.add(data.readMORPHLINESTYLE())
		startEdges = data.readSHAPE()
		endEdges = data.readSHAPE()
	}

	fun export(handler: ShapeExporter, ratio: Double = 0.0): Unit {
		var j: Int = 0
		val exportShape: SWFShape = SWFShape()
		val numEdges: Int = startEdges.records.size
		if (startEdges.records.size != endEdges.records.size) {
			TODO("Not implemented different startEdges.records.size(${startEdges.records.size}) != endEdges.records.size(${endEdges.records.size})")
		}
		for (i in 0 until numEdges) {
			var startRecord = startEdges.records[i]
			// Ignore start records that are style change records and don't have moveTo
			// The end record index is not incremented, because end records do not have
			// style change records without moveTo's.
			if (startRecord.type == SWFShapeRecord.TYPE_STYLECHANGE && !(startRecord as SWFShapeRecordStyleChange).stateMoveTo) {
				exportShape.records.add(startRecord.clone())
				continue
			}
			var endRecord = endEdges.records[j++]
			var exportRecord: SWFShapeRecord? = null
			// It is possible for an edge to change type over the course of a morph sequence.
			// A straight edge can become a curved edge and vice versa
			// Convert straight edge to curved edge, if needed:
			if (startRecord.type == SWFShapeRecord.TYPE_CURVEDEDGE && endRecord.type == SWFShapeRecord.TYPE_STRAIGHTEDGE) {
				endRecord = convertToCurvedEdge(endRecord as SWFShapeRecordStraightEdge)
			} else if (startRecord.type == SWFShapeRecord.TYPE_STRAIGHTEDGE && endRecord.type == SWFShapeRecord.TYPE_CURVEDEDGE) {
				startRecord = convertToCurvedEdge(startRecord as SWFShapeRecordStraightEdge)
			}
			when (startRecord.type) {
				SWFShapeRecord.TYPE_STYLECHANGE -> {
					val startStyleChange = startRecord.clone() as SWFShapeRecordStyleChange
					val endStyleChange = endRecord as SWFShapeRecordStyleChange
					startStyleChange.moveDeltaX += ((endStyleChange.moveDeltaX - startStyleChange.moveDeltaX) * ratio).toInt()
					startStyleChange.moveDeltaY += ((endStyleChange.moveDeltaY - startStyleChange.moveDeltaY) * ratio).toInt()
					exportRecord = startStyleChange
				}
				SWFShapeRecord.TYPE_STRAIGHTEDGE -> {
					val startStraightEdge = startRecord.clone() as SWFShapeRecordStraightEdge
					val endStraightEdge = endRecord as SWFShapeRecordStraightEdge
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
				SWFShapeRecord.TYPE_CURVEDEDGE -> {
					val startCurvedEdge = startRecord.clone() as SWFShapeRecordCurvedEdge
					val endCurvedEdge = endRecord as SWFShapeRecordCurvedEdge
					startCurvedEdge.controlDeltaX += ((endCurvedEdge.controlDeltaX - startCurvedEdge.controlDeltaX) * ratio).toInt()
					startCurvedEdge.controlDeltaY += ((endCurvedEdge.controlDeltaY - startCurvedEdge.controlDeltaY) * ratio).toInt()
					startCurvedEdge.anchorDeltaX += ((endCurvedEdge.anchorDeltaX - startCurvedEdge.anchorDeltaX) * ratio).toInt()
					startCurvedEdge.anchorDeltaY += ((endCurvedEdge.anchorDeltaY - startCurvedEdge.anchorDeltaY) * ratio).toInt()
					exportRecord = startCurvedEdge
				}
				SWFShapeRecord.TYPE_END -> {
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

	protected fun convertToCurvedEdge(straightEdge: SWFShapeRecordStraightEdge): SWFShapeRecordCurvedEdge {
		val curvedEdge: SWFShapeRecordCurvedEdge = SWFShapeRecordCurvedEdge()
		curvedEdge.controlDeltaX = straightEdge.deltaX / 2
		curvedEdge.controlDeltaY = straightEdge.deltaY / 2
		curvedEdge.anchorDeltaX = straightEdge.deltaX
		curvedEdge.anchorDeltaY = straightEdge.deltaY
		return curvedEdge
	}

	override val type = TagDefineMorphShape.TYPE
	override val name = "DefineMorphShape"
	override val version = 3
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val indent2: String = " ".repeat(indent + 2)
		val indent4: String = " ".repeat(indent + 4)
		var str: String = Tag.toStringCommon(type, name, indent) + "ID: " + characterId
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

class TagDefineMorphShape2 : TagDefineMorphShape(), ITag {
	companion object {
		const val TYPE = 84
	}

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

	override val type = TagDefineMorphShape2.TYPE
	override val name = "DefineMorphShape2"
	override val version = 8
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		val indent2: String = " ".repeat(indent + 2)
		val indent4: String = " ".repeat(indent + 4)
		var str: String = Tag.toStringCommon(type, name, indent) + "ID: " + characterId
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

class TagDefineScalingGrid : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 78
	}

	lateinit var splitter: SWFRectangle

	override var characterId: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		splitter = data.readRECT()
	}

	override val type = TagDefineScalingGrid.TYPE
	override val name = "DefineScalingGrid"
	override val version = 8
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
			"CharacterID: " + characterId + ", " +
			"Splitter: " + splitter
	}
}

class TagDefineSceneAndFrameLabelData : _BaseTag(), ITag {
	companion object {
		const val TYPE = 86
	}

	var scenes = ArrayList<SWFScene>()
	var frameLabels = ArrayList<SWFFrameLabel>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val sceneCount: Int = data.readEncodedU32()
		for (i in 0 until sceneCount) {
			val sceneOffset: Int = data.readEncodedU32()
			val sceneName: String = data.readString()
			scenes.add(SWFScene(sceneOffset, sceneName))
		}
		val frameLabelCount: Int = data.readEncodedU32()
		for (i in 0 until frameLabelCount) {
			val frameNumber: Int = data.readEncodedU32()
			val frameLabel: String = data.readString()
			frameLabels.add(SWFFrameLabel(frameNumber, frameLabel))
		}
	}

	override val type = TagDefineSceneAndFrameLabelData.TYPE
	override val name = "DefineSceneAndFrameLabelData"
	override val version = 9
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent)
		if (scenes.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Scenes:"
			for (i in 0 until scenes.size) str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + scenes[i].toString()
		}
		if (frameLabels.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "FrameLabels:"
			for (i in 0 until frameLabels.size) str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + frameLabels[i].toString()
		}
		return str
	}
}

open class TagDefineShape : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 2
	}

	lateinit var shapeBounds: SWFRectangle
	lateinit var shapes: SWFShapeWithStyle

	override var characterId: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		shapeBounds = data.readRECT()
		shapes = data.readSHAPEWITHSTYLE(level)
	}

	fun export(handler: ShapeExporter): Unit {
		shapes.export(handler)
	}

	override val type = TagDefineShape.TYPE
	override val name = "DefineShape"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) + "ID: " + characterId + ", " + "Bounds: " + shapeBounds
		str += shapes.toString(indent + 2)
		return str
	}
}

open class TagDefineShape2 : TagDefineShape(), IDefinitionTag {
	companion object {
		const val TYPE = 22
	}

	override val type = TagDefineShape2.TYPE
	override val name = "DefineShape2"
	override val version = 2
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) + "ID: " + characterId + ", " + "Bounds: " + shapeBounds
		str += shapes.toString(indent + 2)
		return str
	}
}

open class TagDefineShape3 : TagDefineShape2(), IDefinitionTag {
	companion object {
		const val TYPE = 32
	}

	override val type = TagDefineShape3.TYPE
	override val name = "DefineShape3"
	override val version = 3
	override val level = 3

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) + "ID: " + characterId + ", " + "Bounds: " + shapeBounds
		str += shapes.toString(indent + 2)
		return str
	}
}

class TagDefineShape4 : TagDefineShape3(), IDefinitionTag {
	companion object {
		const val TYPE = 83
	}

	lateinit var edgeBounds: SWFRectangle
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

	override val type = TagDefineShape4.TYPE
	override val name = "DefineShape4"
	override val version = 8
	override val level = 4

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) + "ID: " + characterId + ", "
		if (usesFillWindingRule) str += "UsesFillWindingRule, "
		if (usesNonScalingStrokes) str += "UsesNonScalingStrokes, "
		if (usesScalingStrokes) str += "UsesScalingStrokes, "
		str += "ShapeBounds: $shapeBounds, EdgeBounds: $edgeBounds"
		str += shapes.toString(indent + 2)
		return str
	}
}

class TagDefineSound : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 14
	}

	var soundFormat: Int = 0
	var soundRate: Int = 0
	var soundSize: Int = 0
	var soundType: Int = 0
	var soundSampleCount: Int = 0

	override var characterId: Int = 0

	var soundData: FlashByteArray = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		soundFormat = data.readUB(4)
		soundRate = data.readUB(2)
		soundSize = data.readUB(1)
		soundType = data.readUB(1)
		soundSampleCount = data.readUI32()
		soundData = data.readBytes(length - 7).toFlash()
	}

	override val type = TagDefineSound.TYPE
	override val name = "DefineSound"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) +
			"SoundID: " + characterId + ", " +
			"Format: " + SoundCompression.toString(soundFormat) + ", " +
			"Rate: " + SoundRate.toString(soundRate) + ", " +
			"Size: " + SoundSize.toString(soundSize) + ", " +
			"Type: " + SoundType.toString(soundType) + ", " +
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
		val frame: MPEGFrame = MPEGFrame()
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
		soundFormat = SoundCompression.MP3
		soundSize = SoundSize.BIT_16
		soundType = if (channelmode == MPEGFrame.CHANNEL_MODE_MONO) SoundType.MONO else SoundType.STEREO
		when (samplingrate) {
			44100 -> soundRate = SoundRate.KHZ_44
			22050 -> soundRate = SoundRate.KHZ_22
			11025 -> soundRate = SoundRate.KHZ_11
			else -> throw Error("Unsupported sampling rate: $samplingrate Hz")
		}
		// Clear ByteArray
		soundData.length = 0
		// Write SeekSamples (here always 0)
		soundData.writeShort(0)
		// Write raw MP3 (without ID3 metadata)
		soundData.writeBytes(mp3, beginIdx, endIdx - beginIdx)
	}
}

class TagDefineSprite : com.codeazur.as3swf.SWFTimelineContainer(), IDefinitionTag {
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

	override val type = TagDefineSprite.TYPE
	override val name = "DefineSprite"
	override val version = 3
	override val level = 1

	override fun toString(indent: Int, flags: Int): String = "${Tag.toStringCommon(type, name, indent)}ID: $characterId, FrameCount: $frameCount${super.toString(indent, flags)}"
}

open class TagDefineText : _BaseTag(), IDefinitionTag {
	companion object {
		const val TYPE = 11
	}

	lateinit var textBounds: SWFRectangle
	lateinit var textMatrix: SWFMatrix

	override var characterId: Int = 0

	var records = ArrayList<SWFTextRecord>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		textBounds = data.readRECT()
		textMatrix = data.readMATRIX()
		val glyphBits: Int = data.readUI8()
		val advanceBits: Int = data.readUI8()
		var record: SWFTextRecord? = null
		while (true) {
			record = data.readTEXTRECORD(glyphBits, advanceBits, record, level)
			if (record == null) break
			records.add(record)
		}
	}

	override val type = TagDefineText.TYPE
	override val name = "DefineText"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) +
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

class TagDefineText2 : TagDefineText(), IDefinitionTag {
	companion object {
		const val TYPE = 33
	}

	override val type = TagDefineText2.TYPE
	override val name = "DefineText2"
	override val version = 3
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) +
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

class TagDefineVideoStream : _BaseTag(), IDefinitionTag {
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

	override val type = TagDefineVideoStream.TYPE
	override val name = "DefineVideoStream"
	override val version = 6
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
			"ID: " + characterId + ", " +
			"Frames: " + numFrames + ", " +
			"Width: " + width + ", " +
			"Height: " + height + ", " +
			"Deblocking: " + VideoDeblockingType.toString(deblocking) + ", " +
			"Smoothing: " + smoothing + ", " +
			"Codec: " + VideoCodecID.toString(codecId)
	}
}

class TagDoABC : _BaseTag(), ITag {
	companion object {
		const val TYPE = 82

		fun create(abcData: FlashByteArray? = null, aName: String = "", aLazyInitializeFlag: Boolean = true): TagDoABC {
			val doABC = TagDoABC()
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

	var bytes: FlashByteArray = FlashByteArray()
	private var _abc: ABC? = null

	val abc: ABC
		get() {
			if (_abc == null) {
				_abc = ABC()
				//try {
				_abc?.readFile(bytes.cloneToNewByteArray().openSync())
				//} catch (e: Throwable) {
				//	e.printStackTrace()
				//}
			}
			return _abc!!
		}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val pos: Int = data.position
		val flags: Int = data.readUI32()
		lazyInitializeFlag = ((flags and 0x01) != 0)
		abcName = data.readString()
		bytes = data.readBytes(length - (data.position - pos)).toFlash()
		_abc = null
	}

	override val type = TagDoABC.TYPE
	override val name = "DoABC"
	override val version = 9
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
			"Lazy: " + lazyInitializeFlag + ", " +
			(if (abcName.isNotEmpty()) "Name: $abcName, " else "") +
			"Length: " + bytes.length
	}
}

class TagDoABCDeprecated : _BaseTag(), ITag {
	companion object {
		const val TYPE = 72
		fun create(abcData: FlashByteArray? = null): TagDoABCDeprecated {
			val doABC: TagDoABCDeprecated = TagDoABCDeprecated()
			if (abcData != null && abcData.length > 0) {
				doABC.bytes.writeBytes(abcData)
			}
			return doABC
		}
	}

	protected var bytes = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val pos: Int = data.position
		bytes = FlashByteArray(data.readBytes(length - (data.position - pos)))
	}

	override val type = TagDoABCDeprecated.TYPE
	override val name = "DoABCDeprecated"
	override val version = 9
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
			"Length: " + bytes.length
	}
}

open class TagDoAction : _BaseTag(), ITag {
	companion object {
		const val TYPE = 12
	}

	var actions = ArrayList<IAction>()

	protected var labelCount: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		var action: IAction?
		while (true) {
			action = data.readACTIONRECORD()
			if (action == null) break
			actions.add(action)
		}
		labelCount = Action.resolveOffsets(actions)
	}

	override val type = TagDoAction.TYPE
	override val name = "DoAction"
	override val version = 3
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) + "Records: " + actions.size
		if ((flags and com.codeazur.as3swf.SWF.TOSTRING_FLAG_AVM1_BYTECODE) == 0) {
			for (i in 0 until actions.size) {
				str += "\n" + " ".repeat(indent + 2) + "[" + i + "] " + actions[i].toString(indent + 2)
			}
		} else {
			val context: ActionExecutionContext = ActionExecutionContext(actions, arrayListOf(), labelCount)
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

class TagDoInitAction : TagDoAction(), ITag {
	companion object {
		const val TYPE = 59
	}

	var spriteId: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		spriteId = data.readUI16()
		while (true) actions.add(data.readACTIONRECORD() ?: break)
		labelCount = Action.resolveOffsets(actions)
	}

	override val type = TagDoInitAction.TYPE
	override val name = "DoInitAction"
	override val version = 6
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) +
			"SpriteID: " + spriteId + ", " +
			"Records: " + actions.size
		if ((flags and com.codeazur.as3swf.SWF.TOSTRING_FLAG_AVM1_BYTECODE) == 0) {
			for (i in 0 until actions.size) {
				str += "\n" + " ".repeat(indent + 2) + "[" + i + "] " + actions[i].toString(indent + 2)
			}
		} else {
			val context: ActionExecutionContext = ActionExecutionContext(actions, arrayListOf(), labelCount)
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
			password = FlashByteArray(data.readBytes(length))
		}
	}

	override val type = TagEnableDebugger.TYPE
	override val name = "EnableDebugger"
	override val version = 5
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent)
	}
}

class TagEnableDebugger2 : TagEnableDebugger(), ITag {
	companion object {
		const val TYPE = 64
	}

	// Reserved, SWF File Format v10 says this is always zero.
	// Observed other values from generated SWFs, e.g. 0x1975.
	protected var reserved: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		reserved = data.readUI16()
		if (length > 2) {
			password = data.readBytes(length - 2).toFlash()
		}
	}

	override val type = TagEnableDebugger2.TYPE
	override val name = "EnableDebugger2"
	override val version = 6
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
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
			password = data.readBytes(length - 2).toFlash()
		}
	}

	override val type = TagEnableTelemetry.TYPE
	override val name = "EnableTelemetry"
	override val version = 19
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent)
	}
}

class TagEnd : _BaseTag(), ITag {
	companion object {
		val TYPE = 0
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		// Do nothing. The End tag has no body.
	}

	override val type = TagEnd.TYPE
	override val name = "End"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int) = Tag.toStringCommon(type, name, indent)
}

class TagExportAssets : _BaseTag(), ITag {
	companion object {
		const val TYPE = 56
	}

	val symbols = ArrayList<SWFSymbol>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		val numSymbols: Int = data.readUI16()
		for (i in 0 until numSymbols) {
			symbols.add(data.readSYMBOL())
		}
	}

	override val type = TagExportAssets.TYPE
	override val name = "ExportAssets"
	override val version = 5
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent)
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

	override val type = TagFileAttributes.TYPE
	override val name = "FileAttributes"
	override val version = 8
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
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

	override val type = TagFrameLabel.TYPE
	override val name = "FrameLabel"
	override val version = 3
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = "Name: " + frameName
		if (namedAnchorFlag) {
			str += ", NamedAnchor = true"
		}
		return Tag.toStringCommon(type, name, indent) + str
	}
}

open class TagImportAssets : _BaseTag(), ITag {
	companion object {
		const val TYPE = 57
	}

	lateinit var url: String

	protected var symbols = ArrayList<SWFSymbol>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		url = data.readString()
		val numSymbols: Int = data.readUI16()
		for (i in 0 until numSymbols) {
			symbols.add(data.readSYMBOL())
		}
	}

	override val type = TagImportAssets.TYPE
	override val name = "ImportAssets"
	override val version = 5
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent)
		if (symbols.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Assets:"
			for (i in 0 until symbols.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + symbols[i].toString()
			}
		}
		return str
	}
}

class TagImportAssets2 : TagImportAssets(), ITag {
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

	override val type = TagImportAssets2.TYPE
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
			jpegTables = data.readBytes(length).toFlash()
		}
	}

	override val type = TagJPEGTables.TYPE
	override val name = "JPEGTables"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) + "Length: " + jpegTables.length
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

	override val type = TagMetadata.TYPE
	override val name = "Metadata"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent)
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
			binaryData = data.readBytes(length - 2).toFlash()
		}
	}

	override val type = TagNameCharacter.TYPE
	override val name = "NameCharacter"
	override val version = 3
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) +
			"ID: " + characterId
		if (binaryData.length > 0) {
			binaryData.position = 0
			str += ", Name: " + binaryData.readUTFBytes(binaryData.length - 1)
			binaryData.position = 0
		}
		return str
	}
}

open class TagPlaceObject : _BaseTag(), IDisplayListTag {
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
	var matrix: SWFMatrix? = null
	var colorTransform: SWFColorTransform? = null

	// Forward declarations for TagPlaceObject2
	var ratio = 0
	val ratiod get() = ratio.toDouble() / 65536.0
	var instanceName: String? = null
	var clipDepth = 0
	var clipActions: SWFClipActions? = null

	// Forward declarations for TagPlaceObject3
	var className: String? = null
	var blendMode = 0
	var bitmapCache = 0
	var bitmapBackgroundColor = 0
	var visible = 0

	// Forward declarations for TagPlaceObject4
	var metaData: Any? = null

	val surfaceFilterList = arrayListOf<IFilter>()

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

	override val type = TagPlaceObject.TYPE
	override val name = "PlaceObject"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) +
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

open class TagPlaceObject2 : TagPlaceObject(), IDisplayListTag {
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

	override val type = TagPlaceObject2.TYPE
	override val name = "PlaceObject2"
	override val version = 3
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) + "Depth: " + depth
		if (hasCharacter) str += ", CharacterID: " + characterId
		if (hasMatrix) str += ", Matrix: " + matrix.toString()
		if (hasColorTransform) str += ", ColorTransform: " + colorTransform
		if (hasRatio) str += ", Ratio: " + ratio
		if (hasName) str += ", Name: " + instanceName
		if (hasClipDepth) str += ", ClipDepth: " + clipDepth
		if (hasClipActions && clipActions != null) str += "\n" + " ".repeat(indent + 2) + clipActions!!.toString(indent + 2, flags)
		return str
	}
}

open class TagPlaceObject3 : TagPlaceObject2(), IDisplayListTag {
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
		if (hasClassName) className = data.readString()
		if (hasCharacter) characterId = data.readUI16()
		if (hasMatrix) matrix = data.readMATRIX()
		if (hasColorTransform) colorTransform = data.readCXFORMWITHALPHA()
		if (hasRatio) ratio = data.readUI16()
		if (hasName) instanceName = data.readString()
		if (hasClipDepth) clipDepth = data.readUI16()
		if (hasFilterList) for (i in 0 until data.readUI8()) surfaceFilterList.add(data.readFILTER())
		if (hasBlendMode) blendMode = data.readUI8()
		if (hasCacheAsBitmap) bitmapCache = data.readUI8()
		if (hasVisible) visible = data.readUI8()
		if (hasOpaqueBackground) bitmapBackgroundColor = data.readRGBA()
		if (hasClipActions) clipActions = data.readCLIPACTIONS(version)
	}

	override val type = TagPlaceObject3.TYPE
	override val name = "PlaceObject3"
	override val version = 8
	override val level = 3

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent) + "Depth: " + depth
		if (hasClassName) str += ", ClassName: " + className
		if (hasCharacter) str += ", CharacterID: " + characterId
		if (hasMatrix) str += ", Matrix: " + matrix.toString()
		if (hasColorTransform) str += ", ColorTransform: " + colorTransform
		if (hasRatio) str += ", Ratio: " + ratio
		if (hasName) str += ", Name: " + instanceName
		if (hasClipDepth) str += ", ClipDepth: " + clipDepth
		if (hasBlendMode) str += ", BlendMode: " + BlendMode.toString(blendMode)
		if (hasCacheAsBitmap) str += ", CacheAsBitmap: " + bitmapCache
		if (hasVisible) str += ", Visible: " + visible
		if (hasOpaqueBackground) str += ", BackgroundColor: " + ColorUtils.rgbaToString(bitmapBackgroundColor)
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
class TagPlaceObject4 : TagPlaceObject3(), IDisplayListTag {
	companion object {
		const val TYPE = 94
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		super.parse(data, length, version, async)
		if (data.bytesAvailable > 0) {
			metaData = data.readObject()
		}
	}

	override val type = TagPlaceObject4.TYPE
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
	lateinit var compileDate: DateTime

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		productId = data.readUI32()
		edition = data.readUI32()
		majorVersion = data.readUI8()
		minorVersion = data.readUI8()

		build = data.readUI32().toLong() + data.readUI32().toLong() shl 32
		val sec: Long = data.readUI32().toLong() + data.readUI32().toLong() shl 32
		compileDate = DateTime(sec)
	}

	override val type = TagProductInfo.TYPE
	override val name = "ProductInfo"
	override val version = 3
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
			"ProductID: " + productId + ", " +
			"Edition: " + edition + ", " +
			"Version: " + majorVersion + "." + minorVersion + " r" + build + ", " +
			"CompileDate: " + compileDate.toString()
	}
}


class TagPathsArePostScript : _BaseTag(), ITag {
	companion object {
		const val TYPE = 25
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
	}

	override val type = TagPathsArePostScript.TYPE
	override val name = "PathsArePostScript"
	override val version = 2
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent)
	}
}

class TagProtect : _BaseTag(), ITag {
	companion object {
		const val TYPE = 24
	}

	protected var password = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		if (length > 0) {
			password = data.readBytes(length).toFlash()
		}
	}

	override val type = TagProtect.TYPE
	override val name = "Protect"
	override val version = 2
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent)
	}
}

open class TagRemoveObject : _BaseTag(), IDisplayListTag {
	companion object {
		const val TYPE = 5
	}

	var characterId: Int = 0
	var depth: Int = 0

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		characterId = data.readUI16()
		depth = data.readUI16()
	}

	override val type = TagRemoveObject.TYPE
	override val name = "RemoveObject"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
			"CharacterID: " + characterId + ", " +
			"Depth: " + depth
	}
}

class TagRemoveObject2 : TagRemoveObject(), IDisplayListTag {
	companion object {
		const val TYPE = 28
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		depth = data.readUI16()
	}

	override val type = TagRemoveObject2.TYPE
	override val name = "RemoveObject2"
	override val version = 3
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
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

	override val type = TagScriptLimits.TYPE
	override val name = "ScriptLimits"
	override val version = 7
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) +
			"MaxRecursionDepth: " + maxRecursionDepth + ", " +
			"ScriptTimeoutSeconds: " + scriptTimeoutSeconds
	}
}

class TagSetBackgroundColor : _BaseTag(), ITag {
	companion object {
		const val TYPE = 9

		fun create(aColor: Int = 0xffffff): TagSetBackgroundColor {
			val setBackgroundColor: TagSetBackgroundColor = TagSetBackgroundColor()
			setBackgroundColor.color = aColor
			return setBackgroundColor
		}
	}

	var color: Int = 0xffffff

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		color = data.readRGB()
	}

	override val type = TagSetBackgroundColor.TYPE
	override val name = "SetBackgroundColor"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) + "Color: " + ColorUtils.rgbToString(color)
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

	override val type = TagSetTabIndex.TYPE
	override val name = "SetTabIndex"
	override val version = 7
	override val level = 1

	override fun toString(indent: Int, flags: Int): String = Tag.toStringCommon(type, name, indent) + "Depth: " + depth + ", " + "TabIndex: " + tabIndex
}

class TagShowFrame : _BaseTag(), IDisplayListTag {
	companion object {
		const val TYPE = 1
	}

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		// Do nothing. The End tag has no body.
	}

	override val type = TagShowFrame.TYPE
	override val name = "ShowFrame"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String = Tag.toStringCommon(type, name, indent)
}

class TagSoundStreamBlock : _BaseTag(), ITag {
	companion object {
		const val TYPE = 19
	}

	var soundData = FlashByteArray()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		soundData = data.readBytes(length).toFlash()
	}

	override val type = TagSoundStreamBlock.TYPE
	override val name = "SoundStreamBlock"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String = Tag.toStringCommon(type, name, indent) + "Length: " + soundData.length
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
		if (streamSoundCompression == SoundCompression.MP3) latencySeek = data.readSI16()
	}

	override val type = TagSoundStreamHead.TYPE
	override val name = "SoundStreamHead"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent)
		if (streamSoundSampleCount > 0) {
			str += "Format: " + SoundCompression.toString(streamSoundCompression) + ", " +
				"Rate: " + SoundRate.toString(streamSoundRate) + ", " +
				"Size: " + SoundSize.toString(streamSoundSize) + ", " +
				"Type: " + SoundType.toString(streamSoundType) + ", "
		}
		str += "Samples: $streamSoundSampleCount, "
		str += "LatencySeek: " + latencySeek
		return str
	}
}

class TagSoundStreamHead2 : TagSoundStreamHead(), ITag {
	companion object {
		const val TYPE = 45
	}

	override val type = TagSoundStreamHead2.TYPE
	override val name = "SoundStreamHead2"
	override val version = 3
	override val level = 2

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent)
		if (streamSoundSampleCount > 0) {
			str += "Format: " + SoundCompression.toString(streamSoundCompression) + ", " +
				"Rate: " + SoundRate.toString(streamSoundRate) + ", " +
				"Size: " + SoundSize.toString(streamSoundSize) + ", " +
				"Type: " + SoundType.toString(streamSoundType) + ", "
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
	lateinit var soundInfo: SWFSoundInfo

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		soundId = data.readUI16()
		soundInfo = data.readSOUNDINFO()
	}

	override val type = TagStartSound.TYPE
	override val name = "StartSound"
	override val version = 1
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		val str: String = Tag.toStringCommon(type, name, indent) + "SoundID: " + soundId + ", " + "SoundInfo: " + soundInfo
		return str
	}
}

class TagStartSound2 : _BaseTag(), ITag {
	companion object {
		const val TYPE = 89
	}

	lateinit var soundClassName: String
	lateinit var soundInfo: SWFSoundInfo

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		soundClassName = data.readString()
		soundInfo = data.readSOUNDINFO()
	}

	override val type = TagStartSound2.TYPE
	override val name = "StartSound2"
	override val version = 9
	override val level = 2

	override fun toString(indent: Int, flags: Int): String = Tag.toStringCommon(type, name, indent) + "SoundClassName: " + soundClassName + ", " + "SoundInfo: " + soundInfo
}

class TagSymbolClass : _BaseTag(), ITag {
	companion object {
		const val TYPE = 76
	}

	val symbols = ArrayList<SWFSymbol>()

	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit {
		for (i in 0 until data.readUI16()) symbols.add(data.readSYMBOL())
	}

	override val type = TagSymbolClass.TYPE
	override val name = "SymbolClass"
	override val version = 9 // educated guess (not specified in SWF10 spec)
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		var str: String = Tag.toStringCommon(type, name, indent)
		if (symbols.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Symbols:"
			for (i in 0 until symbols.size) str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + symbols[i].toString()
		}
		return str
	}
}

open class TagUnknown(override val type: Int = 0) : _BaseTag(), ITag {
	suspend override fun parse(data: SWFData, length: Int, version: Int, async: Boolean) = data.skipBytes(length)

	override val name = "????"
	override val version = 0
	override val level = 1

	override fun toString(indent: Int, flags: Int) = Tag.toStringCommon(type, name, indent)
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
		_videoData = data.readBytes(length - 4).toFlash()
	}

	override val type = TagVideoFrame.TYPE
	override val name = "VideoFrame"
	override val version = 6
	override val level = 1

	override fun toString(indent: Int, flags: Int): String {
		return Tag.toStringCommon(type, name, indent) + "StreamID: " + streamId + ", " + "Frame: " + frameNum
	}
}

class TagSWFEncryptActions(type: Int = 0) : TagUnknown(), ITag {
	companion object {
		const val TYPE = 253
	}

	override val type = TYPE
	override val name = "SWFEncryptActions"
}

class TagSWFEncryptSignature(type: Int = 0) : TagUnknown(), ITag {
	companion object {
		const val TYPE = 255
	}

	override val type = TYPE
	override val name = "SWFEncryptSignature"
}
