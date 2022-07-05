@file:Suppress(
	"RedundantUnitReturnType", "unused", "UNUSED_VARIABLE", "UNUSED_PARAMETER",
	"VARIABLE_WITH_REDUNDANT_INITIALIZER", "KDocUnresolvedReference", "MemberVisibilityCanBePrivate", "ClassName",
	"PropertyName"
)

package com.soywiz.korfl.as3swf

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korfl.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.collections.set

interface ITag {
	val type: Int
	val name: String
	val version: Int
	val level: Int

	suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean = false)
	fun toString(indent: Int = 0, flags: Int = 0): String
}

open class _BaseTag(
    override val type: Int,
    override val name: String,
    override val version: Int,
    override val level: Int = 1,
) : ITag {
    override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean): Unit = Unit
	override fun toString(indent: Int, flags: Int): String = Tag.toStringCommon(type, name, indent)
	override fun toString() = toString(0, 0)
}

open class _BaseDefinitionTag(
    type: Int,
    name: String,
    version: Int,
    level: Int = 1,
    override var characterId: Int = 0
) : _BaseTag(type, name, version, level), IDefinitionTag

interface IDefinitionTag : ITag {
	var characterId: Int
}

interface IDisplayListTag : ITag

class Tag {
	companion object {
		fun toStringCommon(type: Int, name: String, indent: Int = 0): String = "${" ".repeat(indent)}[${"%02d".format(type)}:$name] "
	}
}

class TagCSMTextSettings : _BaseTag(74, "CSMTextSettings", 8, 1) {
	var textId: Int = 0
	var useFlashType: Int = 0
	var gridFit: Int = 0
	var thickness = 0.0
	var sharpness = 0.0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		textId = data.readUI16()
		useFlashType = data.readUB(2)
		gridFit = data.readUB(3)
		data.readUB(3) // reserved, always 0
		thickness = data.readFIXED()
		sharpness = data.readFIXED()
		data.readUI8() // reserved, always 0
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}TextID: $textId, UseFlashType: $useFlashType, GridFit: $gridFit, Thickness: $thickness, Sharpness: $sharpness"
}

class TagDebugID : _BaseTag(63, "DebugID", 6, 1) {
	private var uuid = ByteArray(0)

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		if (length > 0) uuid = data.data.readBytes(length)
	}

	override fun toString(indent: Int, flags: Int): String = buildString {
		append("${Tag.toStringCommon(type, name, indent)}UUID: ")
		if (uuid.size == 16) {
			append("%02x%02x%02x%02x-".format(uuid[0], uuid[1], uuid[2], uuid[3]))
			append("%02x%02x-".format(uuid[4], uuid[5]))
			append("%02x%02x-".format(uuid[6], uuid[7]))
			append("%02x%02x-".format(uuid[8], uuid[9]))
			append("%02x%02x%02x%02x%02x%02x".format(uuid[10], uuid[11], uuid[12], uuid[13], uuid[14], uuid[15]))
		} else {
            append("(invalid length: ${uuid.size})")
		}
	}
}

class TagDefineBinaryData : _BaseDefinitionTag(87, "DefineBinaryData", 9, 1) {
	var binaryData = ByteArray(0)

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		data.readUI32() // reserved, always 0
		if (length > 6) binaryData = data.readBytes(length - 6)
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, Length: ${binaryData.size}"
}

open class TagDefineBits(
    type: Int = 6,
    name: String = "DefineBits",
    version: Int = 1,
    level: Int = 1,
) : _BaseDefinitionTag(type, name, version, level) {
	var bitmapType: Int = BitmapType.JPEG

	var bitmapData = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		if (length > 2) bitmapData = FlashByteArray(data.readBytes(length - 2))
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, BitmapLength: ${bitmapData.length}"
}

open class TagDefineBitsJPEG2(
    type: Int = 21,
    name: String = "DefineBitsJPEG2",
    version: Int = 2,
    level: Int = 2,
) : TagDefineBits(type, name, version, level) {
	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		super.parse(data, length, version, async)
        setBitmapType()
	}

    fun setBitmapType() {
        when {
            bitmapData[0] == 0xff && (bitmapData[1] == 0xd8 || bitmapData[1] == 0xd9) -> bitmapType = BitmapType.JPEG
            bitmapData[0] == 0x89 && bitmapData[1] == 0x50 && bitmapData[2] == 0x4e && bitmapData[3] == 0x47 && bitmapData[4] == 0x0d && bitmapData[5] == 0x0a && bitmapData[6] == 0x1a && bitmapData[7] == 0x0a -> bitmapType = BitmapType.PNG
            bitmapData[0] == 0x47 && bitmapData[1] == 0x49 && bitmapData[2] == 0x46 && bitmapData[3] == 0x38 && bitmapData[4] == 0x39 && bitmapData[5] == 0x61 -> bitmapType = BitmapType.GIF89A
        }
    }

	override val version get() = if (bitmapType == BitmapType.JPEG) 2 else 8

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, Type: ${BitmapType.toString(bitmapType)}, BitmapLength: ${bitmapData.length}"
}

open class TagDefineBitsJPEG3(
    type: Int = 35,
    name: String = "DefineBitsJPEG3",
    version: Int = 3,
    level: Int = 0,
) : TagDefineBitsJPEG2(type, name, version, level) {
	var bitmapAlphaData = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		val alphaDataOffset: Int = data.readUI32()
		bitmapData = data.readBytes(alphaDataOffset).toFlash()
        setBitmapType()
		val alphaDataSize: Int = length - alphaDataOffset - 6
		if (alphaDataSize > 0) {
			bitmapAlphaData = data.readBytes(alphaDataSize).toFlash()
		}
	}

	override val version get() = if (bitmapType == BitmapType.JPEG) 3 else 8

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, Type: ${BitmapType.toString(bitmapType)}, HasAlphaData: ${bitmapAlphaData.length > 0}, ${if (bitmapAlphaData.length > 0) "BitmapAlphaLength: ${bitmapAlphaData.length}, " else ""}BitmapLength: ${bitmapData.length}"
}

class TagDefineBitsJPEG4() : TagDefineBitsJPEG3(90, "DefineBitsJPEG4", 10, 4) {
	var deblockParam: Double = 0.0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		val alphaDataOffset: Int = data.readUI32()
		deblockParam = data.readFIXED8()
		bitmapData = data.readBytes(alphaDataOffset).toFlash()
        setBitmapType()
		val alphaDataSize: Int = length - alphaDataOffset - 6
		if (alphaDataSize > 0) {
			bitmapAlphaData = data.readBytes(alphaDataSize).toFlash()
		}
	}

	override fun toString(indent: Int, flags: Int): String {
		return "${Tag.toStringCommon(
			type,
			name,
			indent
		)}ID: $characterId, Type: ${BitmapType.toString(bitmapType)}, DeblockParam: $deblockParam, HasAlphaData: ${bitmapAlphaData.length > 0}, ${if (bitmapAlphaData.length > 0) "BitmapAlphaLength: ${bitmapAlphaData.length}, " else ""}BitmapLength: ${bitmapData.length}"
	}
}

open class TagDefineBitsLossless(
    type: Int = 20,
    name: String = "DefineBitsLossless",
    version: Int = 2,
    level: Int = 1,
    characterId: Int = 0
) : _BaseDefinitionTag(type, name, version, level, characterId) {
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

	var zlibBitmapData = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		val rawFormat = data.readUI8()
		bitmapFormat = BitmapFormat[rawFormat]
		bitmapWidth = data.readUI16()
		bitmapHeight = data.readUI16()
		if (bitmapFormat == BitmapFormat.BIT_8) bitmapColorTableSizeM1 = data.readUI8()
		zlibBitmapData = data.readBytes(length - (if (bitmapFormat == BitmapFormat.BIT_8) 8 else 7)).toFlash()
		//zlibBitmapData = data.readBytes(data.bytesAvailable).toFlash()
	}

	override fun toString(indent: Int, flags: Int) = "${Tag.toStringCommon(
		type,
		name,
		indent
	)}ID: $characterId, Format: $bitmapFormat, Size: ($bitmapWidth,$bitmapHeight)"
}

class TagDefineBitsLossless2 : TagDefineBitsLossless(36, "DefineBitsLossless2", 3, 2) {
    override val hasAlpha = true
}

open class TagDefineButton : _BaseDefinitionTag(7, "DefineButton", 1) {
	companion object {
		const val STATE_UP = "up"
		const val STATE_OVER = "over"
		const val STATE_DOWN = "down"
		const val STATE_HIT = "hit"
	}

	protected var characters = ArrayList<SWFButtonRecord>()
	protected var actions = ArrayList<IAction>()

	protected var frames = hashMapOf<String, ArrayList<SWFButtonRecord>>()

	protected var labelCount: Int = 0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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

	fun getRecordsByState(state: String): ArrayList<SWFButtonRecord> = frames[state]!!

	protected fun processRecords() {
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
		frames[STATE_UP] = ArrayList(upState.sortedBy { it.placeDepth })
		frames[STATE_OVER] = ArrayList(overState.sortedBy { it.placeDepth })
		frames[STATE_DOWN] = ArrayList(downState.sortedBy { it.placeDepth })
		frames[STATE_HIT] = ArrayList(hitState.sortedBy { it.placeDepth })
	}

	override fun toString(indent: Int, flags: Int): String = buildString {
        append("${Tag.toStringCommon(type, name, indent)}ID: $characterId")
		if (characters.size > 0) {
            append("\n${" ".repeat(indent + 2)}Characters:")
			for (i in 0 until characters.size) {
                append("\n${" ".repeat(indent + 4)}[$i] ${characters[i].toString(indent + 4)}")
			}
		}
		if (actions.size > 0) {
            append("\n${" ".repeat(indent + 2)}Actions:")
			if ((flags and SWF.TOSTRING_FLAG_AVM1_BYTECODE) == 0) {
				for (i in 0 until actions.size) {
                    append("\n${" ".repeat(indent + 4)}[$i] ${actions[i].toString(indent + 4)}")
				}
			} else {
				val context = ActionExecutionContext(actions, arrayListOf(), labelCount)
				for (i in 0 until actions.size) {
                    append("\n${" ".repeat(indent + 4)}${actions[i].toBytecode(indent + 4, context)}")
				}
				if (context.endLabel != null) {
                    append("\n${" ".repeat(indent + 6)}${context.endLabel}:")
				}
			}
		}
	}
}

open class TagDefineButton2 : _BaseDefinitionTag(34, "DefineButton2", 3, 2) {
	var trackAsMenu = false

	var characters = ArrayList<SWFButtonRecord>()
	protected var condActions = ArrayList<SWFButtonCondAction>()

	protected var frames = hashMapOf<String, ArrayList<SWFButtonRecord>>()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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
			var condActionSize = 0
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

	protected fun processRecords() {
		val upState = ArrayList<SWFButtonRecord>()
		val overState = ArrayList<SWFButtonRecord>()
		val downState = ArrayList<SWFButtonRecord>()
		val hitState = ArrayList<SWFButtonRecord>()
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

	override fun toString(indent: Int, flags: Int): String = buildString {
        append("${Tag.toStringCommon(type, name, indent)}ID: $characterId, TrackAsMenu: $trackAsMenu")
		if (characters.size > 0) {
            append("\n${" ".repeat(indent + 2)}Characters:")
			for (i in 0 until characters.size) {
                append("\n${" ".repeat(indent + 4)}[$i] ${characters[i].toString(indent + 4)}")
			}
		}
		if (condActions.size > 0) {
            append("\n${" ".repeat(indent + 2)}CondActions:")
			for (i in 0 until condActions.size) {
                append("\n${" ".repeat(indent + 4)}[$i] ${condActions[i].toString(indent + 4, flags)}")
			}
		}
	}
}

class TagDefineButtonCxform : _BaseDefinitionTag(23, "DefineButtonCxform", 2) {
	lateinit var buttonColorTransform: SWFColorTransform

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		buttonColorTransform = data.readCXFORM()
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, ColorTransform: $buttonColorTransform"
}

class TagDefineButtonSound : _BaseDefinitionTag(17, "DefineButtonSound", 2) {
	var buttonSoundChar0 = 0
	var buttonSoundChar1 = 0
	var buttonSoundChar2 = 0
	var buttonSoundChar3 = 0
	lateinit var buttonSoundInfo0: SWFSoundInfo
	lateinit var buttonSoundInfo1: SWFSoundInfo
	lateinit var buttonSoundInfo2: SWFSoundInfo
	lateinit var buttonSoundInfo3: SWFSoundInfo

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ButtonID: $characterId, ButtonSoundChars: $buttonSoundChar0,$buttonSoundChar1,$buttonSoundChar2,$buttonSoundChar3"
}

class TagDefineEditText : _BaseDefinitionTag(37, "DefineEditText", 4) {
	lateinit var bounds: SWFRectangle
	var variableName: String? = null

	var hasText = false
	var wordWrap = false
	var multiline = false
	var password = false
	var readOnly = false
	var hasTextColor = false
	var hasMaxLength = false
	var hasFont = false
	var hasFontClass = false
	var autoSize = false
	var hasLayout = false
	var noSelect = false
	var border = false
	var wasStatic = false
	var html = false
	var useOutlines = false

	var fontId = 0
	var fontClass: String? = null
	var fontHeight = 0
	var textColor = 0
	var maxLength = 0
	var align = 0
	var leftMargin = 0
	var rightMargin = 0
	var indent = 0
	var leading = 0
	var initialText: String? = null

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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
		if (hasFont || hasFontClass) fontHeight = data.readUI16()
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

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, ${if (hasText && initialText!!.isNotEmpty()) "Text: $initialText, " else ""}${if (variableName!!.isNotEmpty()) "VariableName: $variableName, " else ""}Bounds: $bounds"
}

open class TagDefineFont(
    type: Int = 10,
    name: String = "DefineFont",
    version: Int = 1,
    level: Int = 1,
    characterId: Int = 0
) : _BaseDefinitionTag(type, name, version, level, characterId) {
	var glyphShapeTable = ArrayList<SWFShape>()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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

	fun export(handler: ShapeExporter, glyphIndex: Int) {
		glyphShapeTable[glyphIndex].export(handler)
	}

	protected open val unitDivisor = 1.0

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, Glyphs: ${glyphShapeTable.size}${toStringCommon(indent)}"

	protected open fun toStringCommon(indent: Int): String = buildString {
		for (i in 0 until glyphShapeTable.size) {
            append("\n${" ".repeat(indent + 2)}[$i] GlyphShapes:")
            append(glyphShapeTable[i].toString(indent + 4))
		}
	}
}

open class TagDefineFont2(
    type: Int = 48,
    name: String = "DefineFont2",
    version: Int = 3,
    level: Int = 2,
    characterId: Int = 0
) : TagDefineFont(type, name, version, level, characterId) {
	var hasLayout = false
	var shiftJIS = false
	var smallText = false
	var ansi = false
	var wideOffsets = false
	var wideCodes = false
	var italic = false
	var bold = false
	var languageCode = 0
	lateinit var fontName: String
	var ascent = 0
	var descent = 0
	var leading = 0

	val codeTable = ArrayList<Int>()
	val fontAdvanceTable = ArrayList<Int>()
	val fontBoundsTable = ArrayList<SWFRectangle>()
	val fontKerningTable = ArrayList<SWFKerningRecord>()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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
			for (i in 0 until numGlyphs) fontAdvanceTable.add(data.readSI16())
			for (i in 0 until numGlyphs) fontBoundsTable.add(data.readRECT())
			val kerningCount = data.readUI16()
			for (i in 0 until kerningCount) fontKerningTable.add(data.readKERNINGRECORD(wideCodes))
		}
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, FontName: $fontName, Italic: $italic, Bold: $bold, Glyphs: ${glyphShapeTable.size}${toStringCommon(indent)}"

	override fun toStringCommon(indent: Int): String = buildString {
        append(super.toStringCommon(indent))
		if (hasLayout) {
			append("\n${" ".repeat(indent + 2)}Ascent: $ascent")
			append("\n${" ".repeat(indent + 2)}Descent: $descent")
			append("\n${" ".repeat(indent + 2)}Leading: $leading")
		}
		if (codeTable.size > 0) {
            append("\n${" ".repeat(indent + 2)}CodeTable:")
			for (i in 0 until codeTable.size) {
                append(if ((i and 0x0f) == 0) "\n${" ".repeat(indent + 4)}${codeTable[i]}" else ", ${codeTable[i]}")
			}
		}
		if (fontAdvanceTable.size > 0) {
            append("\n${" ".repeat(indent + 2)}FontAdvanceTable:")
			for (i in 0 until fontAdvanceTable.size) {
                append(if ((i and 0x07) == 0) "\n${" ".repeat(indent + 4)}${fontAdvanceTable[i]}" else ", ${fontAdvanceTable[i]}")
			}
		}
		if (fontBoundsTable.size > 0) {
			var hasNonNullBounds = false
			for (i in 0 until fontBoundsTable.size) {
				val rect: SWFRectangle = fontBoundsTable[i]
				if (rect.xmin != 0 || rect.xmax != 0 || rect.ymin != 0 || rect.ymax != 0) {
					hasNonNullBounds = true
					break
				}
			}
			if (hasNonNullBounds) {
                append("\n${" ".repeat(indent + 2)}FontBoundsTable:")
				for (i in 0 until fontBoundsTable.size) {
                    append("\n${" ".repeat(indent + 4)}[$i] ${fontBoundsTable[i]}")
				}
			}
		}
		if (fontKerningTable.size > 0) {
            append("\n${" ".repeat(indent + 2)}KerningTable:")
			for (i in 0 until fontKerningTable.size) {
                append("\n${" ".repeat(indent + 4)}[$i] ${fontKerningTable[i]}")
			}
		}
	}
}

class TagDefineFont3 : TagDefineFont2(75, "DefineFont3", 8, 2) {
	override val unitDivisor = 20.0

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, FontName: $fontName, Italic: $italic, Bold: $bold, Glyphs: ${glyphShapeTable.size}${toStringCommon(indent)}"
}

class TagDefineFont4 : _BaseDefinitionTag(91, "DefineFont4", 10) {
	var hasFontData = false
	var italic = false
	var bold = false
	lateinit var fontName: String

	private var fontData = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, FontName: $fontName, HasFontData: $hasFontData, Italic: $italic, Bold: $bold"
}

class TagDefineFontAlignZones : _BaseTag(73, "DefineFontAlignZones", 8) {
	var fontId: Int = 0
	var csmTableHint: Int = 0

	private var _zoneTable = ArrayList<SWFZoneRecord>()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		fontId = data.readUI16()
		csmTableHint = (data.readUI8() ushr 6)
		val recordsEndPos: Int = data.position + length - 3
		while (data.position < recordsEndPos) {
			_zoneTable.add(data.readZONERECORD())
		}
	}

	override fun toString(indent: Int, flags: Int): String = buildString {
        append("${Tag.toStringCommon(type, name, indent)}FontID: $fontId, CSMTableHint: ${CSMTableHint.toString(csmTableHint)}, Records: ${_zoneTable.size}")
		for (i in 0 until _zoneTable.size) {
            append("\n${" ".repeat(indent + 2)}[$i] ${_zoneTable[i]}")
		}
	}
}

open class TagDefineFontInfo(
    type: Int = 13,
    name: String = "DefineFontInfo",
    version: Int = 1,
    level: Int = 1,
) : _BaseTag(type, name, version, level) {
	var fontId: Int = 0
	lateinit var fontName: String
	var smallText = false
	var shiftJIS = false
	var ansi = false
	var italic = false
	var bold = false
	var wideCodes = false
	var langCode = 0

	protected var _codeTable = ArrayList<Int>()

	protected var langCodeLength: Int = 0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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

	protected open fun parseLangCode(data: SWFData) {
		// Does nothing here.
		// Overridden in TagDefineFontInfo2, where it:
		// - reads langCode
		// - sets langCodeLength to 1
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}FontID: $fontId, FontName: $fontName, Italic: $italic, Bold: $bold, Codes: ${_codeTable.size}"
}

class TagDefineFontInfo2 : TagDefineFontInfo(62, "DefineFontInfo2", 6, 2) {
	override fun parseLangCode(data: SWFData) {
		langCode = data.readUI8()
		langCodeLength = 1
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}FontID: $fontId, FontName: $fontName, Italic: $italic, Bold: $bold, LanguageCode: $langCode, Codes: ${_codeTable.size}"
}

class TagDefineFontName : _BaseTag(88, "DefineFontName", 9) {
	var fontId: Int = 0
	lateinit var fontName: String
	lateinit var fontCopyright: String

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		fontId = data.readUI16()
		fontName = data.readString()
		fontCopyright = data.readString()
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}FontID: $fontId, Name: $fontName, Copyright: $fontCopyright"
}

open class TagDefineMorphShape(
    type: Int = 46,
    name: String = "DefineMorphShape",
    version: Int = 3,
    level: Int = 1,
    characterId: Int = 0
) : _BaseDefinitionTag(type, name, version, level, characterId) {
	lateinit var startBounds: SWFRectangle
	lateinit var endBounds: SWFRectangle
	lateinit var startEdges: SWFShape
	lateinit var endEdges: SWFShape
	var startEdgeBounds: SWFRectangle = SWFRectangle()
	var endEdgeBounds: SWFRectangle = SWFRectangle()

	protected var morphFillStyles = ArrayList<SWFMorphFillStyle>()
	protected var morphLineStyles = ArrayList<SWFMorphLineStyle>()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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

	fun export(handler: ShapeExporter, ratio: Double = 0.0) {
		var j = 0
		val exportShape = SWFShape()
		val numEdges: Int = startEdges.records.size
        try {
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
        } catch (e: Throwable) {
            e.printStackTrace()
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
		val curvedEdge = SWFShapeRecordCurvedEdge()
		curvedEdge.controlDeltaX = straightEdge.deltaX / 2
		curvedEdge.controlDeltaY = straightEdge.deltaY / 2
		curvedEdge.anchorDeltaX = straightEdge.deltaX
		curvedEdge.anchorDeltaY = straightEdge.deltaY
		return curvedEdge
	}

	override fun toString(indent: Int, flags: Int): String = buildString {
		val indent2: String = " ".repeat(indent + 2)
		val indent4: String = " ".repeat(indent + 4)
        append("${Tag.toStringCommon(type, name, indent)}ID: $characterId")
        append("\n${indent2}Bounds:")
        append("\n${indent4}StartBounds: $startBounds")
        append("\n${indent4}EndBounds: $endBounds")
		if (morphFillStyles.size > 0) {
            append("\n${indent2}FillStyles:")
			for (i in 0 until morphFillStyles.size) {
                append("\n$indent4[${i + 1}] ${morphFillStyles[i]}")
			}
		}
		if (morphLineStyles.size > 0) {
            append("\n${indent2}LineStyles:")
			for (i in 0 until morphLineStyles.size) {
                append("\n$indent4[${i + 1}] ${morphLineStyles[i]}")
			}
		}
        append(startEdges.toString(indent + 2))
        append(endEdges.toString(indent + 2))
	}
}

class TagDefineMorphShape2 : TagDefineMorphShape(84, "DefineMorphShape2", 8, 2) {
	var usesNonScalingStrokes = false
	var usesScalingStrokes = false

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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

	override fun toString(indent: Int, flags: Int): String = buildString {
		val indent2: String = " ".repeat(indent + 2)
		val indent4: String = " ".repeat(indent + 4)
		append("${Tag.toStringCommon(type, name, indent)}ID: $characterId")
		append("\n${indent2}Bounds:")
		append("\n${indent4}StartBounds: $startBounds")
		append("\n${indent4}EndBounds: $endBounds")
		append("\n${indent4}StartEdgeBounds: $startEdgeBounds")
		append("\n${indent4}EndEdgeBounds: $endEdgeBounds")
		if (morphFillStyles.size > 0) {
            append("\n${indent2}FillStyles:")
			for (i in 0 until morphFillStyles.size) {
                append("\n$indent4[${i + 1}] ${morphFillStyles[i]}")
			}
		}
		if (morphLineStyles.size > 0) {
            append("\n${indent2}LineStyles:")
			for (i in 0 until morphLineStyles.size) {
                append("\n$indent4[${i + 1}] ${morphLineStyles[i]}")
			}
		}
        append(startEdges.toString(indent + 2))
        append(endEdges.toString(indent + 2))
	}
}

class TagDefineScalingGrid : _BaseDefinitionTag(78, "DefineScalingGrid", 8, 1) {
	lateinit var splitter: SWFRectangle

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		splitter = data.readRECT()
	}

	override fun toString(indent: Int, flags: Int): String = "${Tag.toStringCommon(type, name, indent)}CharacterID: $characterId, Splitter: $splitter"
}

class TagDefineSceneAndFrameLabelData : _BaseTag(TYPE, "DefineSceneAndFrameLabelData", 9) {
	companion object : TagObj(86)

	var scenes = ArrayList<SWFScene>()
	var frameLabels = ArrayList<SWFFrameLabel>()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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

	override fun toString(indent: Int, flags: Int): String = buildString {
		append(Tag.toStringCommon(type, name, indent))
		if (scenes.size > 0) {
            append("\n${" ".repeat(indent + 2)}Scenes:")
			for (i in 0 until scenes.size) append("\n${" ".repeat(indent + 4)}[$i] ${scenes[i]}")
		}
		if (frameLabels.size > 0) {
            append("\n${" ".repeat(indent + 2)}FrameLabels:")
			for (i in 0 until frameLabels.size) append("\n${" ".repeat(indent + 4)}[$i] ${frameLabels[i]}")
		}
	}
}

open class TagDefineShape(
    type: Int = 2,
    name: String = "DefineShape",
    version: Int = 1,
    level: Int = 1,
    characterId: Int = 0
) : _BaseDefinitionTag(type, name, version, level, characterId) {
	lateinit var shapeBounds: SWFRectangle
	lateinit var shapes: SWFShapeWithStyle

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		shapeBounds = data.readRECT()
		shapes = data.readSHAPEWITHSTYLE(level)
	}

	fun export(handler: ShapeExporter) {
		shapes.export(handler)
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, Bounds: $shapeBounds${shapes.toString(indent + 2)}"
}

open class TagDefineShape2(
    type: Int = 22,
    name: String = "DefineShape2",
    version: Int = 2,
    level: Int = 2,
    characterId: Int = 0
) : TagDefineShape(type, name, version, level, characterId) {
	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, Bounds: $shapeBounds${shapes.toString(indent + 2)}"
}

open class TagDefineShape3(
    type: Int = 32,
    name: String = "DefineShape3",
    version: Int = 3,
    level: Int = 3,
    characterId: Int = 0
) : TagDefineShape2(type, name, version, level, characterId) {
	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, Bounds: $shapeBounds${shapes.toString(indent + 2)}"
}

class TagDefineShape4 : TagDefineShape3(83, "DefineShape4", 8, 4) {
	lateinit var edgeBounds: SWFRectangle
	var usesFillWindingRule = false
	var usesNonScalingStrokes = false
	var usesScalingStrokes = false

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		shapeBounds = data.readRECT()
		edgeBounds = data.readRECT()
		val flags: Int = data.readUI8()
		usesFillWindingRule = ((flags and 0x04) != 0)
		usesNonScalingStrokes = ((flags and 0x02) != 0)
		usesScalingStrokes = ((flags and 0x01) != 0)
		shapes = data.readSHAPEWITHSTYLE(level)
	}

	override fun toString(indent: Int, flags: Int): String = buildString {
        append("${Tag.toStringCommon(type, name, indent)}ID: $characterId, ")
		if (usesFillWindingRule) append("UsesFillWindingRule, ")
		if (usesNonScalingStrokes) append("UsesNonScalingStrokes, ")
		if (usesScalingStrokes) append("UsesScalingStrokes, ")
        append("ShapeBounds: $shapeBounds, EdgeBounds: $edgeBounds")
        append(shapes.toString(indent + 2))
	}
}

class TagDefineSound : _BaseDefinitionTag(14, "DefineSound", 1) {
	var soundFormat: Int = 0
	var soundRate: Int = 0
	var soundSize: Int = 0
	var soundType: Int = 0
	var soundSampleCount: Int = 0

	var soundData: FlashByteArray = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		soundFormat = data.readUB(4)
		soundRate = data.readUB(2)
		soundSize = data.readUB(1)
		soundType = data.readUB(1)
		soundSampleCount = data.readUI32()
		soundData = data.readBytes(length - 7).toFlash()
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}SoundID: $characterId, Format: ${SoundCompression.toString(soundFormat)}, Rate: ${SoundRate.toString(soundRate)}, Size: ${SoundSize.toString(soundSize)}, Type: ${SoundType.toString(soundType)}, Samples: $soundSampleCount"

	internal fun processMP3(mp3: FlashByteArray) {
		var i = 0
		var beginIdx = 0
		var endIdx: Int = mp3.length
		var samples = 0
		var firstFrame = true
		var samplingrate = 0
		var channelmode = 0
		val frame = MPEGFrame()
		var state = "id3v2"
		while (i < mp3.length) {
			when (state) {
				"id3v2" -> {
					if (mp3[i] == 0x49 && mp3[i + 1] == 0x44 && mp3[i + 2] == 0x33) {
						i += 10 + ((mp3[i + 6] shl 21) or (mp3[i + 7] shl 14) or (mp3[i + 8] shl 7) or mp3[i + 9])
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
		soundRate = when (samplingrate) {
			44100 -> SoundRate.KHZ_44
			22050 -> SoundRate.KHZ_22
			11025 -> SoundRate.KHZ_11
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

class TagDefineSprite : com.soywiz.korfl.as3swf.SWFTimelineContainer(), IDefinitionTag by _BaseDefinitionTag(39, "DefineSprite", 3) {
	var frameCount: Int = 0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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

	override fun toString(indent: Int, flags: Int): String =
		"${Tag.toStringCommon(type, name, indent)}ID: $characterId, FrameCount: $frameCount${super.toString(indent, flags)}"
}

open class TagDefineText(
    type: Int = 11,
    name: String = "DefineText",
    version: Int = 1,
    level: Int = 1,
    characterId: Int = 0
) : _BaseDefinitionTag(type, name, version, level, characterId) {
	lateinit var textBounds: SWFRectangle
	lateinit var textMatrix: SWFMatrix

	var records = ArrayList<SWFTextRecord>()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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

	override fun toString(indent: Int, flags: Int): String = buildString {
        append("${Tag.toStringCommon(type, name, indent)}ID: $characterId, Bounds: $textBounds, Matrix: $textMatrix")
		if (records.size > 0) {
            append("\n${" ".repeat(indent + 2)}TextRecords:")
			for (i in 0 until records.size) {
                append("\n${" ".repeat(indent + 4)}[$i] ${records[i].toString(indent + 4)}")
            }
		}
	}
}

class TagDefineText2 : TagDefineText(33, "DefineText2", 3, 2) {
	override fun toString(indent: Int, flags: Int): String = buildString {
        append("${Tag.toStringCommon(type, name, indent)}ID: $characterId, Bounds: $textBounds, Matrix: $textMatrix")
		if (records.size > 0) {
            append("\n${" ".repeat(indent + 2)}TextRecords:")
			for (i in 0 until records.size) {
                append("\n${" ".repeat(indent + 4)}[$i] ${records[i]}")
			}
		}
	}
}

class TagDefineVideoStream : _BaseDefinitionTag(60, "DefineVideoStream", 6) {
	var numFrames = 0
	var width = 0
	var height = 0
	var deblocking = 0
	var smoothing = false
	var codecId = 0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		numFrames = data.readUI16()
		width = data.readUI16()
		height = data.readUI16()
		data.readUB(4)
		deblocking = data.readUB(3)
		smoothing = (data.readUB(1) == 1)
		codecId = data.readUI8()
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ID: $characterId, Frames: $numFrames, Width: $width, Height: $height, Deblocking: ${VideoDeblockingType.toString(deblocking)}, Smoothing: $smoothing, Codec: ${VideoCodecID.toString(codecId)}"
}

class TagDoABC : _BaseTag(82, "DoABC", 9) {
    companion object {
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

	var lazyInitializeFlag = false
	var abcName = ""

	var bytes = FlashByteArray()
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

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		val pos = data.position
		val flags = data.readUI32()
		lazyInitializeFlag = ((flags and 0x01) != 0)
		abcName = data.readString()
		bytes = data.readBytes(length - (data.position - pos)).toFlash()
		_abc = null
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}Lazy: $lazyInitializeFlag, ${if (abcName.isNotEmpty()) "Name: $abcName, " else ""}Length: ${bytes.length}"
}

class TagDoABCDeprecated : _BaseTag(72, "DoABCDeprecated", 9) {
	companion object {
		fun create(abcData: FlashByteArray? = null): TagDoABCDeprecated = TagDoABCDeprecated().also {
            if (abcData != null && abcData.length > 0) it.bytes.writeBytes(abcData)
        }
	}

	private var bytes = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		val pos = data.position
		bytes = FlashByteArray(data.readBytes(length - (data.position - pos)))
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}Length: ${bytes.length}"
}

open class TagDoAction(
    type: Int = 12,
    name: String = "DoAction",
    version: Int = 3,
    level: Int = 1,
) : _BaseTag(type, name, version, level) {
	var actions = ArrayList<IAction>()

	protected var labelCount: Int = 0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		var action: IAction?
		while (true) {
			action = data.readACTIONRECORD()
			if (action == null) break
			actions.add(action)
		}
		labelCount = Action.resolveOffsets(actions)
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}Records: ${actions.size}${toStringAction(indent, flags)}"

    fun toStringAction(indent: Int, flags: Int): String = buildString {
        if ((flags and SWF.TOSTRING_FLAG_AVM1_BYTECODE) == 0) {
            for (i in 0 until actions.size) {
                append("\n${" ".repeat(indent + 2)}[$i] ${actions[i].toString(indent + 2)}")
            }
        } else {
            val context = ActionExecutionContext(actions, arrayListOf(), labelCount)
            for (i in 0 until actions.size) {
                append("\n${" ".repeat(indent + 2)}${actions[i].toBytecode(indent + 2, context)}")
            }
            if (context.endLabel != null) {
                append("\n${" ".repeat(indent + 4)}${context.endLabel}:")
            }
        }
    }
}

class TagDoInitAction : TagDoAction(59, "DoInitAction", 6) {
	var spriteId = 0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		spriteId = data.readUI16()
		while (true) actions.add(data.readACTIONRECORD() ?: break)
		labelCount = Action.resolveOffsets(actions)
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}SpriteID: $spriteId, Records: ${actions.size}${toStringAction(indent, flags)}"
}

open class TagEnableDebugger(
    type: Int = 58,
    name: String = "EnableDebugger",
    version: Int = 5,
    level: Int = 1,
) : _BaseTag(type, name, version, level) {
	protected var password = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		if (length > 0) password = FlashByteArray(data.readBytes(length))
	}
}

class TagEnableDebugger2 : TagEnableDebugger(64, "EnableDebugger2", 6, 2) {
	// Reserved, SWF File Format v10 says this is always zero.
	// Observed other values from generated SWFs, e.g. 0x1975.
	private var reserved = 0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		reserved = data.readUI16()
		if (length > 2) password = data.readBytes(length - 2).toFlash()
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}Password: ${if (password.length == 0) "null" else password.readUTF()}, Reserved: 0x${reserved.toString(16)}"
}

class TagEnableTelemetry : _BaseTag(93, "EnableTelemetry", 19) {
	private var password = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		if (length > 2) {
			data.readByte()
			data.readByte()
			password = data.readBytes(length - 2).toFlash()
		}
	}
}

class TagEnd : _BaseTag(TYPE, "End", 1) {
	companion object : TagObj(0)
}

class TagFileAttributes : _BaseTag(69, "FileAttributes", 8) {
	var useDirectBlit = false
	var useGPU = false
	var hasMetadata = false
	var actionscript3 = true
	var useNetwork = false

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		val flags = data.readUI8()
		useDirectBlit = ((flags and 0x40) != 0)
		useGPU = ((flags and 0x20) != 0)
		hasMetadata = ((flags and 0x10) != 0)
		actionscript3 = ((flags and 0x08) != 0)
		useNetwork = ((flags and 0x01) != 0)
		data.skipBytes(3)
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}AS3: $actionscript3, HasMetadata: $hasMetadata, UseDirectBlit: $useDirectBlit, UseGPU: $useGPU, UseNetwork: $useNetwork"

	override fun toString() = toString(0, 0)
}

class TagFrameLabel : _BaseTag(TYPE, "FrameLabel", 3) {
	companion object : TagObj(43)

	lateinit var frameName: String
	var namedAnchorFlag = false

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		val start = data.position
		frameName = data.readString()
		if ((data.position - start) < length) {
			data.readUI8()    // Named anchor flag, always 1
			namedAnchorFlag = true
		}
	}

	override fun toString(indent: Int, flags: Int): String = buildString {
        append(Tag.toStringCommon(type, name, indent))
        append("Name: $frameName")
		if (namedAnchorFlag) append(", NamedAnchor = true")
	}
}


open class TagImportExportAssets(
    override val type: Int,
    override val name: String,
    override val version: Int,
    override val level: Int = 1,
) : _BaseTag(type, name, version, level) {
    val symbols = ArrayList<SWFSymbol>()

    override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
        val numSymbols = data.readUI16()
        for (i in 0 until numSymbols) {
            symbols.add(data.readSYMBOL())
        }
    }

    override fun toString(indent: Int, flags: Int): String = buildString {
        append(Tag.toStringCommon(type, name, indent))
        if (symbols.size > 0) {
            append("\n${" ".repeat(indent + 2)}Assets:")
            for (i in 0 until symbols.size) {
                append("\n${" ".repeat(indent + 4)}[$i] ${symbols[i]}")
            }
        }
    }
}

class TagExportAssets : TagImportExportAssets(56, "ExportAssets", 5)

open class TagImportAssets(
    type: Int = 57,
    name: String = "ImportAssets",
    version: Int = 5,
    level: Int = 1,
) : TagImportExportAssets(type, name, version, level) {
	lateinit var url: String

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		url = data.readString()
        super.parse(data, length, version, async)
	}
}

class TagImportAssets2 : TagImportAssets(71, "ImportAssets2", 8, 2) {
	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		url = data.readString()
		data.readUI8() // reserved, always 1
		data.readUI8() // reserved, always 0
		val numSymbols: Int = data.readUI16()
		for (i in 0 until numSymbols) {
			symbols.add(data.readSYMBOL())
		}
	}
}

class TagJPEGTables : _BaseTag(TYPE, "JPEGTables", 1) {
	companion object : TagObj(8)

	var jpegTables = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		if (length > 0) {
			jpegTables = data.readBytes(length).toFlash()
		}
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}Length: ${jpegTables.length}"
}

class TagMetadata : _BaseTag(77, "Metadata", 1) {
	lateinit var xmlString: String

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		xmlString = data.readString()
	}

	override fun toString(indent: Int, flags: Int): String = "${Tag.toStringCommon(type, name, indent)} $xmlString"
}

class TagNameCharacter : _BaseTag(40, "NameCharacter", 3) {

	private var characterId: Int = 0

	private var binaryData = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		if (length > 2) {
			binaryData = data.readBytes(length - 2).toFlash()
		}
	}

	override fun toString(indent: Int, flags: Int): String = buildString {
        append("${Tag.toStringCommon(type, name, indent)}ID: $characterId")
		if (binaryData.length > 0) {
			binaryData.position = 0
            append(", Name: ${binaryData.readUTFBytes(binaryData.length - 1)}")
			binaryData.position = 0
		}
	}
}

open class TagPlaceObject(
    type: Int = TagPlaceObject.TYPE,
    name: String = "PlaceObject",
    version: Int = 1,
    level: Int = 1,
) : _BaseTag(type, name, version, level), IDisplayListTag {
	companion object : TagObj(4)

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

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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

	override fun toString(indent: Int, flags: Int): String = buildString {
        append("${Tag.toStringCommon(type, name, indent)}Depth: $depth")
		if (hasCharacter) append(", CharacterID: $characterId")
		if (hasMatrix) append(", Matrix: $matrix")
		if (hasColorTransform) append(", ColorTransform: $colorTransform")
	}
}

open class TagPlaceObject2(
    type: Int = TagPlaceObject2.TYPE,
    name: String = "PlaceObject2",
    version: Int = 3,
    level: Int = 2,
) : TagPlaceObject(), IDisplayListTag {
	companion object : TagObj(26)

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
        readFlags1(data)
		depth = data.readUI16()
		if (hasCharacter) characterId = data.readUI16()
		if (hasMatrix) matrix = data.readMATRIX()
		if (hasColorTransform) colorTransform = data.readCXFORMWITHALPHA()
		if (hasRatio) ratio = data.readUI16()
		if (hasName) instanceName = data.readString()
		if (hasClipDepth) clipDepth = data.readUI16()
		if (hasClipActions) clipActions = data.readCLIPACTIONS(version)
	}

    fun readFlags1(data: SWFData) {
        val flags: Int = data.readUI8()
        hasClipActions = (flags and 0x80) != 0
        hasClipDepth = (flags and 0x40) != 0
        hasName = (flags and 0x20) != 0
        hasRatio = (flags and 0x10) != 0
        hasColorTransform = (flags and 0x08) != 0
        hasMatrix = (flags and 0x04) != 0
        hasCharacter = (flags and 0x02) != 0
        hasMove = (flags and 0x01) != 0
    }

	override fun toString(indent: Int, flags: Int): String = buildString {
        append("${Tag.toStringCommon(type, name, indent)}Depth: $depth")
		if (hasCharacter) append(", CharacterID: $characterId")
		if (hasMatrix) append(", Matrix: ${matrix.toString()}")
		if (hasColorTransform) append(", ColorTransform: $colorTransform")
		if (hasRatio) append(", Ratio: $ratio")
		if (hasName) append(", Name: $instanceName")
		if (hasClipDepth) append(", ClipDepth: $clipDepth")
		if (hasClipActions && clipActions != null) append("\n${" ".repeat(indent + 2)}${clipActions!!.toString(indent + 2, flags)}")
	}
}

open class TagPlaceObject3(
    type: Int = TagPlaceObject3.TYPE,
    name: String = "PlaceObject3",
    version: Int = 8,
    level: Int = 3,
) : TagPlaceObject2(), IDisplayListTag {
	companion object : TagObj(70)

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
        readFlags1(data)
        readFlags2(data)
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

    fun readFlags2(data: SWFData) {
        val flags2: Int = data.readUI8()
        hasOpaqueBackground = (flags2 and 0x40) != 0
        hasVisible = (flags2 and 0x20) != 0
        hasImage = (flags2 and 0x10) != 0
        hasClassName = (flags2 and 0x08) != 0
        hasCacheAsBitmap = (flags2 and 0x04) != 0
        hasBlendMode = (flags2 and 0x02) != 0
        hasFilterList = (flags2 and 0x01) != 0
    }

	override fun toString(indent: Int, flags: Int): String = buildString {
        append("${Tag.toStringCommon(type, name, indent)}Depth: $depth")
		if (hasClassName) append(", ClassName: $className")
		if (hasCharacter) append(", CharacterID: $characterId")
		if (hasMatrix) append(", Matrix: $matrix")
		if (hasColorTransform) append(", ColorTransform: $colorTransform")
		if (hasRatio) append(", Ratio: $ratio")
		if (hasName) append(", Name: $instanceName")
		if (hasClipDepth) append(", ClipDepth: $clipDepth")
		if (hasBlendMode) append(", BlendMode: ${BlendMode.toString(blendMode)}")
		if (hasCacheAsBitmap) append(", CacheAsBitmap: $bitmapCache")
		if (hasVisible) append(", Visible: $visible")
		if (hasOpaqueBackground) append(", BackgroundColor: ${ColorUtils.rgbaToString(bitmapBackgroundColor)}")
		if (hasFilterList) {
            append("\n${" ".repeat(indent + 2)}Filters:")
			for (i in 0 until surfaceFilterList.size) {
                append("\n${" ".repeat(indent + 4)}[$i] ${surfaceFilterList[i].toString(indent + 4)}")
			}
		}
		if (hasClipActions) {
            append("\n${" ".repeat(indent + 2)}${clipActions!!.toString(indent + 2)}")
		}
	}
}

/**
 * PlaceObject4 is essentially identical to PlaceObject3 except it has a different
 * swf tag value of course (94 instead of 70) and at the end of the tag, if there are
 * additional bytes, those bytes will be interpreted as AMF binary data that will be
 * used as the metadata attached to the instance.
 *
 * http://help.adobe.com/en_US/FlashPlatform/reference/actionscript/3/flash/display/DisplayObject.html#metaData
 */
class TagPlaceObject4 : TagPlaceObject3(94, "PlaceObject4", 19, 4), IDisplayListTag {
	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		super.parse(data, length, version, async)
		if (data.bytesAvailable > 0) {
			metaData = data.readObject()
		}
	}

	override fun toString(indent: Int, flags: Int): String = buildString {
        append(super.toString(indent, 0))
		if (metaData != null) {
            append("\n${" ".repeat(indent + 2)}MetaData: yes")
		}
	}
}

class TagProductInfo : _BaseTag(41, "ProductInfo", 3) {
	var productId: Int = 0
	var edition: Int = 0
	var majorVersion: Int = 0
	var minorVersion: Int = 0
	var build: Long = 0L
	var compileDate: DateTime = DateTime.EPOCH

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		productId = data.readUI32()
		edition = data.readUI32()
		majorVersion = data.readUI8()
		minorVersion = data.readUI8()

		build = data.readUI32().toLong() + data.readUI32().toLong() shl 32
		val sec: Long = data.readUI32().toLong() + data.readUI32().toLong() shl 32
		compileDate = DateTime(sec)
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}ProductID: $productId, Edition: $edition, Version: $majorVersion.$minorVersion r$build, CompileDate: $compileDate"
}


class TagPathsArePostScript : _BaseTag(25, "PathsArePostScript", 2)

class TagProtect : _BaseTag(24, "Protect", 2) {
	private var password = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		if (length > 0) {
			password = data.readBytes(length).toFlash()
		}
	}
}

open class TagRemoveObject(
    type: Int = TagRemoveObject.TYPE,
    name: String = "RemoveObject",
    version: Int = 1,
    level: Int = 1,
) : _BaseTag(type, name, version, level), IDisplayListTag {
	companion object : TagObj(5)

	var characterId: Int = 0
	var depth: Int = 0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		characterId = data.readUI16()
		depth = data.readUI16()
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}CharacterID: $characterId, Depth: $depth"
}

class TagRemoveObject2 : TagRemoveObject(TagRemoveObject2.TYPE, "RemoveObject2", 3, 2), IDisplayListTag {
	companion object : TagObj(28)

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		depth = data.readUI16()
	}

	override fun toString(indent: Int, flags: Int): String = "${Tag.toStringCommon(type, name, indent)}Depth: $depth"
}

class TagScriptLimits : _BaseTag(65, "ScriptLimits", 7, 1) {
	var maxRecursionDepth: Int = 0
	var scriptTimeoutSeconds: Int = 0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		maxRecursionDepth = data.readUI16()
		scriptTimeoutSeconds = data.readUI16()
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}MaxRecursionDepth: $maxRecursionDepth, ScriptTimeoutSeconds: $scriptTimeoutSeconds"
}

class TagSetBackgroundColor : _BaseTag(TYPE, "SetBackgroundColor", 1) {
	companion object : TagObj(9) {
		fun create(aColor: Int = 0xffffff): TagSetBackgroundColor {
			val setBackgroundColor = TagSetBackgroundColor()
			setBackgroundColor.color = aColor
			return setBackgroundColor
		}
	}

	var color: Int = 0xffffff

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		color = data.readRGB()
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}Color: ${ColorUtils.rgbToString(color)}"
}

class TagSetTabIndex : _BaseTag(66, "SetTabIndex", 7) {
	var depth: Int = 0
	var tabIndex: Int = 0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		depth = data.readUI16()
		tabIndex = data.readUI16()
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}Depth: $depth, TabIndex: $tabIndex"
}

class TagShowFrame : _BaseTag(TYPE, "ShowFrame", 1), IDisplayListTag {
	companion object : TagObj(1)
}

class TagSoundStreamBlock : _BaseTag(TYPE, "SoundStreamBlock", 1) {
	companion object : TagObj(19)

	var soundData = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		soundData = data.readBytes(length).toFlash()
	}

	override fun toString(indent: Int, flags: Int): String = "${Tag.toStringCommon(type, name, indent)}Length: ${soundData.length}"
}

open class TagSoundStreamHead(
    type: Int = TagSoundStreamHead.TYPE,
    name: String = "SoundStreamHead",
    version: Int = 1,
    level: Int = 1,
) : _BaseTag(type, name, version, level) {
	companion object : TagObj(18)

	var playbackSoundRate: Int = 0
	var playbackSoundSize: Int = 0
	var playbackSoundType: Int = 0
	var streamSoundCompression: Int = 0
	var streamSoundRate: Int = 0
	var streamSoundSize: Int = 0
	var streamSoundType: Int = 0
	var streamSoundSampleCount: Int = 0
	var latencySeek: Int = 0

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
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

	override fun toString(indent: Int, flags: Int): String = buildString {
        append(Tag.toStringCommon(type, name, indent))
		if (streamSoundSampleCount > 0) {
            append("Format: ${SoundCompression.toString(streamSoundCompression)}, Rate: ${SoundRate.toString(streamSoundRate)}, Size: ${SoundSize.toString(streamSoundSize)}, Type: ${SoundType.toString(streamSoundType)}, ")
		}
        append("Samples: $streamSoundSampleCount, ")
        append("LatencySeek: $latencySeek")
	}
}

class TagSoundStreamHead2 : TagSoundStreamHead(TagSoundStreamHead2.TYPE, "SoundStreamHead2", 3, 2) {
	companion object : TagObj(45)

	override fun toString(indent: Int, flags: Int): String = buildString {
        append(Tag.toStringCommon(type, name, indent))
		if (streamSoundSampleCount > 0) {
            append("Format: ${SoundCompression.toString(streamSoundCompression)}, Rate: ${SoundRate.toString(streamSoundRate)}, Size: ${SoundSize.toString(streamSoundSize)}, Type: ${SoundType.toString(streamSoundType)}, ")
		}
        append("Samples: $streamSoundSampleCount")
	}
}

class TagStartSound : _BaseTag(15, "StartSound", 1, 1) {
	var soundId: Int = 0
	lateinit var soundInfo: SWFSoundInfo

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		soundId = data.readUI16()
		soundInfo = data.readSOUNDINFO()
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}SoundID: $soundId, SoundInfo: $soundInfo"
}

class TagStartSound2 : _BaseTag(89, "StartSound2", 9, 2) {
	lateinit var soundClassName: String
	lateinit var soundInfo: SWFSoundInfo

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		soundClassName = data.readString()
		soundInfo = data.readSOUNDINFO()
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}SoundClassName: $soundClassName, SoundInfo: $soundInfo"
}

class TagSymbolClass : _BaseTag(76, "SymbolClass", 9, 1) { // educated guess (not specified in SWF10 spec)
	val symbols = ArrayList<SWFSymbol>()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		for (i in 0 until data.readUI16()) symbols.add(data.readSYMBOL())
	}

	override fun toString(indent: Int, flags: Int): String = buildString {
        append(Tag.toStringCommon(type, name, indent))
		if (symbols.size > 0) {
            append("\n${" ".repeat(indent + 2)}Symbols:")
			for (i in 0 until symbols.size) append("\n${" ".repeat(indent + 4)}[$i] ${symbols[i]}")
		}
	}
}

open class TagUnknown(type: Int, name: String = "????", version: Int = 0, level: Int = 1) : _BaseTag(type, name, version, level) {
	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) = data.skipBytes(length)
}

class TagVideoFrame : _BaseTag(61, "VideoFrame", 6, 1) {
	var streamId: Int = 0
	var frameNum: Int = 0

	private var _videoData = FlashByteArray()

	override suspend fun parse(data: SWFData, length: Int, version: Int, async: Boolean) {
		streamId = data.readUI16()
		frameNum = data.readUI16()
		_videoData = data.readBytes(length - 4).toFlash()
	}

	override fun toString(indent: Int, flags: Int): String =
        "${Tag.toStringCommon(type, name, indent)}StreamID: $streamId, Frame: $frameNum"
}

class TagSWFEncryptActions() : TagUnknown(253, "SWFEncryptActions")
class TagSWFEncryptSignature() : TagUnknown(255, "SWFEncryptSignature")

open class TagObj(val TYPE: Int)
