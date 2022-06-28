package com.soywiz.korfl.as3swf

import com.soywiz.kds.Extra
import com.soywiz.kmem.Float16
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlin.collections.set
import kotlin.math.*

@Suppress("unused")
open class SWF : SWFTimelineContainer(), Extra by Extra.Mixin() {
	private var bytes: SWFData = SWFData()

	var signature: String? = null
	var version = 0
	var fileLength = 0
	var fileLengthCompressed = 0
	var frameSize = SWFRectangle()
	var frameRate = 0.0
	var frameCount = 0

	var compressed = false
	var compressionMethod = COMPRESSION_METHOD_ZLIB

	companion object {
		const val COMPRESSION_METHOD_ZLIB = "zlib"
		const val COMPRESSION_METHOD_LZMA = "lzma"

		const val TOSTRING_FLAG_TIMELINE_STRUCTURE = 0x01
		const val TOSTRING_FLAG_AVM1_BYTECODE = 0x02

		protected const val FILE_LENGTH_POS = 4
		protected const val COMPRESSION_START_POS = 8
	}

	init {
		version = 10
		fileLength = 0
		fileLengthCompressed = 0
		frameSize = SWFRectangle()
		frameRate = 50.0
		frameCount = 1
		compressed = true
		compressionMethod = COMPRESSION_METHOD_ZLIB
	}

	suspend fun loadBytes(bytes: ByteArray): SWF = this.apply {
		val ba = bytes.toFlash()
		this.bytes.length = 0
		ba.position = 0
		ba.readBytes(this.bytes)
		parse(this.bytes)
	}

	suspend fun parse(data: SWFData) {
		bytes = data
		parseHeader()
		parseTags(data, version)
	}

	suspend protected fun parseHeader() {
		signature = ""
		compressed = false
		compressionMethod = COMPRESSION_METHOD_ZLIB
		bytes.position = 0
		var signatureByte = bytes.readUI8()
		when (signatureByte.toChar()) {
			'C' -> {
				compressed = true
				compressionMethod = COMPRESSION_METHOD_ZLIB
			}
			'Z' -> {
				compressed = true
				compressionMethod = COMPRESSION_METHOD_LZMA
			}
			'F' -> {
				compressed = false
			}
			else -> throw Error("Not a SWF. First signature byte is 0x" + signatureByte.toString(16) + " (expected: 0x43 or 0x5A or 0x46)")
		}

		signature += String_fromIntArray(intArrayOf(signatureByte.toChar().toInt()))
		signatureByte = bytes.readUI8()
		if (signatureByte != 0x57) throw Error("Not a SWF. Second signature byte is 0x" + signatureByte.toString(16) + " (expected: 0x57)")
		signature += String_fromIntArray(intArrayOf(signatureByte.toChar().toInt()))
		signatureByte = bytes.readUI8()
		if (signatureByte != 0x53) throw Error("Not a SWF. Third signature byte is 0x" + signatureByte.toString(16) + " (expected: 0x53)")
		signature += String_fromIntArray(intArrayOf(signatureByte.toChar().toInt()))
		version = bytes.readUI8()
		fileLength = bytes.readUI32()
		fileLengthCompressed = bytes.length

		if (fileLength >= fileLengthCompressed * 4) invalidOp("something went wrong! fileLength >= fileLengthCompressed * 4 : $fileLength >= $fileLengthCompressed * 4")

		if (compressed) {
			// The following data (up to end of file) is compressed, if header has CWS or ZWS signature
			bytes.swfUncompress(compressionMethod, fileLength)
		}
		frameSize = bytes.readRECT()
		frameRate = bytes.readFIXED8()
		frameCount = bytes.readUI16()
	}

	override fun toString(indent: Int, flags: Int): String {
		val indent0 = " ".repeat(indent)
		val indent2 = " ".repeat(indent + 2)
		val indent4 = " ".repeat(indent + 4)
		var s: String = indent0 + "[SWF]\n" +
				indent2 + "Header:\n" +
				indent4 + "Version: " + version + "\n" +
				indent4 + "Compression: "
		s += if (compressed) {
			when (compressionMethod) {
				COMPRESSION_METHOD_ZLIB -> "ZLIB"
				COMPRESSION_METHOD_LZMA -> "LZMA"
				else -> "Unknown"
			}
		} else {
			"None"
		}
		return s + "\n" + indent4 + "FileLength: " + fileLength + "\n" +
				indent4 + "FileLengthCompressed: " + fileLengthCompressed + "\n" +
				indent4 + "FrameSize: " + frameSize.toStringSize() + "\n" +
				indent4 + "FrameRate: " + frameRate + "\n" +
				indent4 + "FrameCount: " + frameCount +
				super.toString(indent, 0)
	}
}

@Suppress("unused")
class SWFData : BitArray() {
	companion object {

		fun dump(ba: FlashByteArray, length: Int, offset: Int = 0) {
			val posOrig = ba.position
			val pos = min(max(posOrig + offset, 0), ba.length - length)
			ba.position = pos
			var str = "[Dump] total length: " + ba.length + ", original position: " + posOrig
			for (i in 0 until length) {
				var b: String = ba.readUnsignedByte().toString(16)
				if (b.length == 1) {
					b = "0$b"
				}
				if (i % 16 == 0) {
					var addr: String = (pos + i).toString(16)
					addr = "00000000".substr(0, 8 - addr.length) + addr
					str += "\r$addr: "
				}
				b += " "
				str += b
			}
			ba.position = posOrig
			println(str)
		}
	}

	init {
		endian = Endian.LITTLE_ENDIAN
	}

	/////////////////////////////////////////////////////////
	// Integers
	/////////////////////////////////////////////////////////

	fun readSI8(): Int = resetBitsPending().readByte()
	fun writeSI8(value: Int) = resetBitsPending().writeByte(value)
	fun readSI16(): Int = resetBitsPending().readShort()
	fun writeSI16(value: Int) = resetBitsPending().writeShort(value)
	fun readSI32(): Int = resetBitsPending().readInt()
	fun writeSI32(value: Int) = resetBitsPending().writeInt(value)
	fun readUI8(): Int = resetBitsPending().readUnsignedByte()
	fun writeUI8(value: Int) = resetBitsPending().writeByte(value)
	fun writeUI8(value: Boolean) = writeUI8(if (value) 1 else 0)
	fun readUI16(): Int = resetBitsPending().readUnsignedShort()
	fun writeUI16(value: Int) = resetBitsPending().writeShort(value)

	fun readUI24(): Int {
		resetBitsPending()
		val loWord = readUnsignedShort()
		val hiByte = readUnsignedByte()
		return (hiByte shl 16) or loWord
	}

	fun writeUI24(value: Int) {
		resetBitsPending()
		writeShort(value and 0xffff)
		writeByte(value ushr 16)
	}

	fun readUI32(): Int = resetBitsPending().readUnsignedInt()
	fun writeUI32(value: Int) = resetBitsPending().writeUnsignedInt(value)

	/////////////////////////////////////////////////////////
	// Fixed-point numbers
	/////////////////////////////////////////////////////////

	fun readFIXED(): Double = resetBitsPending().readInt().toDouble() / 65536

	//fun writeFIXED(value: Int) = writeFIXED(value.toDouble())

	fun writeFIXED(value: Double) = resetBitsPending().writeInt((value * 65536).toInt())
	fun readFIXED8(): Double = resetBitsPending().readShort().toDouble() / 256.0
	fun writeFIXED8(value: Double) = resetBitsPending().writeShort((value * 256).toInt())

	/////////////////////////////////////////////////////////
	// Floating-point numbers
	/////////////////////////////////////////////////////////

	fun readFLOAT(): Double = resetBitsPending().readFloat()
	fun writeFLOAT(value: Double) = resetBitsPending().writeFloat(value)
	fun readDOUBLE(): Double = resetBitsPending().readDouble()
	fun writeDOUBLE(value: Double) = resetBitsPending().writeDouble(value)
	fun readFLOAT16(): Double = Float16.intBitsToDouble(resetBitsPending().readUnsignedShort().toUShort())
	fun writeFLOAT16(value: Double) = resetBitsPending().writeShort(Float16.doubleToIntBits(value).toInt())

	/////////////////////////////////////////////////////////
	// Encoded integer
	/////////////////////////////////////////////////////////

	fun readEncodedU32(): Int {
		resetBitsPending()
		var result = readUnsignedByte()
		if ((result and 0x80) != 0) {
			result = (result and 0x7f) or (readUnsignedByte() shl 7)
			if ((result and 0x4000) != 0) {
				result = (result and 0x3fff) or (readUnsignedByte() shl 14)
				if ((result and 0x200000) != 0) {
					result = (result and 0x1fffff) or (readUnsignedByte() shl 21)
					if ((result and 0x10000000) != 0) {
						result = (result and 0xfffffff) or (readUnsignedByte() shl 28)
					}
				}
			}
		}
		return result
	}

	fun writeEncodedU32(_value: Int) {
		var value = _value
		while (true) {
			val v = value and 0x7f
			value = value ushr 7
			if (value == 0) {
				writeUI8(v)
				break
			}
			writeUI8(v or 0x80)
		}
	}

	/////////////////////////////////////////////////////////
	// Bit values
	/////////////////////////////////////////////////////////

	fun readUB(bits: Int): Int = readBits(bits)
	fun writeUB(bits: Int, value: Int) = writeBits(bits, value)
	fun writeUB(bits: Int, value: Boolean) = writeUB(bits, if (value) 1 else 0)

	fun readSB(bits: Int): Int {
		val shift = 32 - bits
		return (readBits(bits) shl shift) shr shift
	}

	fun writeSB(bits: Int, value: Int) = writeBits(bits, value)
	fun readFB(bits: Int): Double = (readSB(bits)).toDouble() / 65536
	fun writeFB(bits: Int, value: Double) = writeSB(bits, (value * 65536).toInt())

	/////////////////////////////////////////////////////////
	// String
	/////////////////////////////////////////////////////////

	fun readString(): String {
		//var index = position
		//while (this[index++] != 0) Unit
		resetBitsPending()
		return this.data.readStringz()
		//return readUTFBytes(index - position)
	}

	fun writeString(value: String?) {
		if (value != null && value.isNotEmpty()) writeUTFBytes(value)
		writeByte(0)
	}

	/////////////////////////////////////////////////////////
	// Labguage code
	/////////////////////////////////////////////////////////

	fun readLANGCODE(): Int {
		resetBitsPending()
		return readUnsignedByte()
	}

	fun writeLANGCODE(value: Int) {
		resetBitsPending()
		writeByte(value)
	}

	/////////////////////////////////////////////////////////
	// Color records
	/////////////////////////////////////////////////////////

	fun readRGB(): Int {
		resetBitsPending()
		val r = readUnsignedByte()
		val g = readUnsignedByte()
		val b = readUnsignedByte()
		return 0xff000000.toInt() or (r shl 16) or (g shl 8) or b
	}

	fun writeRGB(value: Int) {
		resetBitsPending()
		writeByte((value ushr 16) and 0xff)
		writeByte((value ushr 8) and 0xff)
		writeByte((value ushr 0) and 0xff)
	}

	fun readRGBA(): Int {
		resetBitsPending()
		val rgb = readRGB() and 0x00ffffff
		val a = readUnsignedByte()
		return (a shl 24) or rgb
	}

	fun writeRGBA(value: Int) {
		resetBitsPending()
		writeRGB(value)
		writeByte((value ushr 24) and 0xff)
	}

	fun readARGB(): Int {
		resetBitsPending()
		val a = readUnsignedByte()
		val rgb = readRGB() and 0x00ffffff
		return (a shl 24) or rgb
	}

	fun writeARGB(value: Int) {
		resetBitsPending()
		writeByte((value ushr 24) and 0xff)
		writeRGB(value)
	}

	fun readRECT(): SWFRectangle = SWFRectangle().apply { parse(this@SWFData) }
	fun readMATRIX(): SWFMatrix = SWFMatrix().apply { parse(this@SWFData) }
	fun readCXFORM(): SWFColorTransform = SWFColorTransform().apply { parse(this@SWFData) }
	fun readCXFORMWITHALPHA(): SWFColorTransformWithAlpha = SWFColorTransformWithAlpha().apply { parse(this@SWFData) }
	fun readSHAPE(unitDivisor: Double = 20.0): SWFShape = SWFShape(unitDivisor).apply { parse(this@SWFData) }
	fun readSHAPEWITHSTYLE(level: Int = 1, unitDivisor: Double = 20.0): SWFShapeWithStyle =
		SWFShapeWithStyle(unitDivisor).apply { parse(this@SWFData, level) }

	fun readSTRAIGHTEDGERECORD(numBits: Int) = SWFShapeRecordStraightEdge(numBits).apply { parse(this@SWFData) }
	fun readCURVEDEDGERECORD(numBits: Int): SWFShapeRecordCurvedEdge =
		SWFShapeRecordCurvedEdge(numBits).apply { parse(this@SWFData) }

	fun readSTYLECHANGERECORD(states: Int, fillBits: Int, lineBits: Int, level: Int = 1): SWFShapeRecordStyleChange =
		SWFShapeRecordStyleChange(states, fillBits, lineBits).apply { parse(this@SWFData, level) }

	fun readFILLSTYLE(level: Int = 1): SWFFillStyle = SWFFillStyle().apply { parse(this@SWFData, level) }
	fun readLINESTYLE(level: Int = 1): SWFLineStyle = SWFLineStyle().apply { parse(this@SWFData, level) }
	fun readLINESTYLE2(level: Int = 1): SWFLineStyle2 = SWFLineStyle2().apply { parse(this@SWFData, level) }

	fun readBUTTONRECORD(level: Int = 1): SWFButtonRecord? {
		if (readUI8() == 0) {
			return null
		} else {
			position--
			return SWFButtonRecord().apply { parse(this@SWFData, level) }
		}
	}

	fun readBUTTONCONDACTION(): SWFButtonCondAction = SWFButtonCondAction().apply { parse(this@SWFData) }
	fun readFILTER(): IFilter {
		val filterId = readUI8()
		val filter = SWFFilterFactory.create(filterId)
		filter.parse(this)
		return filter
	}

	fun readTEXTRECORD(
		glyphBits: Int,
		advanceBits: Int,
		previousRecord: SWFTextRecord? = null,
		level: Int = 1
	): SWFTextRecord? {
		if (readUI8() == 0) {
			return null
		} else {
			position--
			return SWFTextRecord().apply { parse(this@SWFData, glyphBits, advanceBits, previousRecord, level) }
		}
	}

	fun readGLYPHENTRY(glyphBits: Int, advanceBits: Int): SWFGlyphEntry =
		SWFGlyphEntry().apply { parse(this@SWFData, glyphBits, advanceBits) }

	fun readZONERECORD(): SWFZoneRecord = SWFZoneRecord(this@SWFData)
	fun readZONEDATA(): SWFZoneData = SWFZoneData(this@SWFData)
	fun readKERNINGRECORD(wideCodes: Boolean): SWFKerningRecord =
		SWFKerningRecord().apply { parse(this@SWFData, wideCodes) }

	fun readGRADIENT(level: Int = 1): SWFGradient = SWFGradient().apply { parse(this@SWFData, level) }
	fun readFOCALGRADIENT(level: Int = 1): SWFFocalGradient = SWFFocalGradient().apply { parse(this@SWFData, level) }
	fun readGRADIENTRECORD(level: Int = 1): SWFGradientRecord = SWFGradientRecord().apply { parse(this@SWFData, level) }
	fun readMORPHFILLSTYLE(level: Int = 1) = SWFMorphFillStyle().apply { parse(this@SWFData, level) }
	fun readMORPHLINESTYLE(level: Int = 1) = SWFMorphLineStyle().apply { parse(this@SWFData, level) }
	fun readMORPHLINESTYLE2(level: Int = 1): SWFMorphLineStyle2 =
		SWFMorphLineStyle2().apply { parse(this@SWFData, level) }

	fun readMORPHGRADIENT(level: Int = 1) = SWFMorphGradient().apply { parse(this@SWFData, level) }
	fun readMORPHFOCALGRADIENT(level: Int = 1) = SWFMorphFocalGradient().apply { parse(this@SWFData, level) }
	fun readMORPHGRADIENTRECORD(): SWFMorphGradientRecord = SWFMorphGradientRecord().apply { parse(this@SWFData) }
	fun readACTIONRECORD(): IAction? {
		val pos: Int = position
		var action: IAction? = null
		val actionCode: Int = readUI8()
		if (actionCode != 0) {
			val actionLength: Int = if (actionCode >= 0x80) readUI16() else 0
			action = SWFActionFactory.create(actionCode, actionLength, pos)
			action.parse(this)
		}
		return action
	}

	fun readACTIONVALUE(): SWFActionValue = SWFActionValue().apply { parse(this@SWFData) }
	fun readREGISTERPARAM(): SWFRegisterParam = SWFRegisterParam().apply { parse(this@SWFData) }
	fun readSYMBOL(): SWFSymbol = SWFSymbol(this@SWFData)
	fun readSOUNDINFO(): SWFSoundInfo = SWFSoundInfo().apply { parse(this@SWFData) }
	fun readSOUNDENVELOPE(): SWFSoundEnvelope = SWFSoundEnvelope(this@SWFData)
	fun readCLIPACTIONS(version: Int): SWFClipActions = SWFClipActions().apply { parse(this@SWFData, version) }
	fun readCLIPACTIONRECORD(version: Int): SWFClipActionRecord? {
		val pos = position
		val flags = if (version >= 6) readUI32() else readUI16()
		if (flags == 0) {
			return null
		} else {
			position = pos
			return SWFClipActionRecord().apply { parse(this@SWFData, version) }
		}
	}

	fun readCLIPEVENTFLAGS(version: Int): SWFClipEventFlags = SWFClipEventFlags().apply { parse(this@SWFData, version) }
	fun readTagHeader(): SWFRecordHeader {
		val pos = position
		val tagTypeAndLength = readUI16()
		var tagLength = tagTypeAndLength and 0x003f
		if (tagLength == 0x3f) {
			// The SWF10 spec sez that this is a signed int.
			// Shouldn't it be an unsigned int?
			tagLength = readSI32()
		}
		return SWFRecordHeader(tagTypeAndLength ushr 6, tagLength, position - pos)
	}


	suspend fun swfUncompress(compressionMethod: String, uncompressedLength: Int = 0) {
		val pos = position
		val ba = FlashByteArray()

		when (compressionMethod) {
			SWF.COMPRESSION_METHOD_ZLIB -> {
				readBytes(ba)
				ba.position = 0
				ba.uncompressInWorker()
			}
			SWF.COMPRESSION_METHOD_LZMA -> {

				// LZMA compressed SWF:
				//   0000 5A 57 53 0F   (ZWS, Version 15)
				//   0004 DF 52 00 00   (Uncompressed size: 21215)
				//   0008 94 3B 00 00   (Compressed size: 15252)
				//   000C 5D 00 00 00 01   (LZMA Properties)
				//   0011 00 3B FF FC A6 14 16 5A ...   (15252 bytes of LZMA Compressed Data, until EOF)
				// 7z LZMA format:
				//   0000 5D 00 00 00 01   (LZMA Properties)
				//   0005 D7 52 00 00 00 00 00 00   (Uncompressed size: 21207, 64 bit)
				//   000D 00 3B FF FC A6 14 16 5A ...   (15252 bytes of LZMA Compressed Data, until EOF)
				// (see also https://github.com/claus/as3swf/pull/23#issuecomment-7203861)

				// Write LZMA properties
				for (i in 0 until 5) ba.writeByte(this[i + 12])

				// Write uncompressed length (64 bit)
				ba.endian = Endian.LITTLE_ENDIAN
				ba.writeUnsignedInt(uncompressedLength - 8)
				ba.writeUnsignedInt(0)

				// Write compressed data
				position = 17

				ba.position = 13
				ba.writeBytes(this.readBytes(this.bytesAvailable))
				ba.position = 13

				// Uncompress
				ba.position = 0
				ba.uncompressInWorker(compressionMethod)
			}
			else -> error("Unknown compression method: $compressionMethod")
		}

		length = pos
		position = pos
		writeBytes(ba)
		position = pos
	}

	/////////////////////////////////////////////////////////
	// etc
	/////////////////////////////////////////////////////////

	fun readRawTag(): SWFRawTag = SWFRawTag().apply { parse(this@SWFData) }

	fun skipBytes(length: Int) { position += length }
}

@Suppress("unused", "UNUSED_PARAMETER")
open class SWFTimelineContainer {
	// We're just being lazy here.
	companion object {
		var EXTRACT_SOUND_STREAM: Boolean = true
	}

	val tags = ArrayList<ITag>()
	var tagsRaw = ArrayList<SWFRawTag>()
	var dictionary = hashMapOf<Int, Int>()
	var scenes = ArrayList<Scene>()
	var frames = ArrayList<Frame>()
	var layers = ArrayList<Layer>()
	var soundStream: SoundStream? = null

	lateinit var currentFrame: Frame
	protected var frameLabels = hashMapOf<Int, String>()
	protected var hasSoundStream: Boolean = false

	protected var eof: Boolean = false

	protected var _tmpData: SWFData? = null
	protected var _tmpVersion: Int = 0
	protected var _tmpTagIterator: Int = 0

	protected var tagFactory: ISWFTagFactory = SWFTagFactory()

	internal var rootTimelineContainer: SWFTimelineContainer = this

	var backgroundColor: Int = 0xffffff
	var jpegTablesTag: TagJPEGTables? = null

    fun getCharacterIds(): List<Int> = rootTimelineContainer.dictionary.keys.sorted()

	fun getCharacter(characterId: Int): IDefinitionTag? {
		val tagIndex = rootTimelineContainer.dictionary[characterId] ?: 0
		if (tagIndex >= 0 && tagIndex < rootTimelineContainer.tags.size) {
			return rootTimelineContainer.tags[tagIndex] as IDefinitionTag
		}
		return null
	}

	suspend fun parseTags(data: SWFData, version: Int) {
		parseTagsInit(data, version)
		while (data.bytesAvailable > 0) {
			dispatchProgress(_tmpData!!.position, _tmpData!!.length)
			val tag = parseTag(_tmpData!!, true) ?: break
			//println(tag)
			if (tag.type == TagEnd.TYPE) break
		}
		parseTagsFinalize()
	}

	private fun dispatchProgress(position: Int, length: Int) {
	}

	private fun dispatchWarning(msg: String) {
	}

	private fun parseTagsInit(data: SWFData, version: Int) {
		tags.clear()
		frames.clear()
		layers.clear()
		dictionary = hashMapOf()
		this.currentFrame = Frame()
		frameLabels = hashMapOf()
		hasSoundStream = false
		_tmpData = data
		_tmpVersion = version
	}

	suspend protected fun parseTag(data: SWFData, async: Boolean = false): ITag? {
		val pos: Int = data.position
		// Bail out if eof
		eof = (pos >= data.length)
		if (eof) {
			println("WARNING: end of file encountered, no end tag.")
			return null
		}
		val tagRaw = data.readRawTag()
		val tagHeader = tagRaw.header
		val tag: ITag = tagFactory.create(tagHeader.type)
		try {
			if (tag is SWFTimelineContainer) {
				val timelineContainer: SWFTimelineContainer = tag
				// Currently, the only SWFTimelineContainer (other than the SWF root
				// itself) is TagDefineSprite (MovieClips have their own timeline).
				// Inject the current tag factory there.
				timelineContainer.tagFactory = tagFactory
				timelineContainer.rootTimelineContainer = this
			}
			// Parse tag
			tag.parse(data, tagHeader.contentLength, _tmpVersion, async)
		} catch (e: Throwable) {
			// If we get here there was a problem parsing this particular tag.
			// Corrupted SWF, possible SWF exploit, or obfuscated SWF.
			// TODO: register errors and warnings
			println("ERROR: parse error: " + e.message + ", Tag: " + tag.name + ", Index: " + tags.size)
            e.printStackTrace()
			//throw(e)
		}
		// Register tag
		tags.add(tag)
		tagsRaw.add(tagRaw)
		// Build dictionary and display list etc
		processTag(tag)
		// Adjust position (just in case the parser under- or overflows)
		if (data.position != pos + tagHeader.tagLength) {
			val index: Int = tags.size - 1
			val excessBytes = data.position - (pos + tagHeader.tagLength)
			//var eventType: String = if (excessBytes < 0) SWFWarningEvent.UNDERFLOW else SWFWarningEvent.OVERFLOW;
			//var eventDataPos = pos
			//var eventDataBytes = if (excessBytes < 0) -excessBytes else excessBytes
			if (rootTimelineContainer == this) {
				println(
					"WARNING: excess bytes: " + excessBytes + ", " +
							"Tag: " + tag.name + ", " +
							"Index: " + index
				)
			} else {
				//eventData.indexRoot = rootTimelineContainer.tags.length;
				println(
					"WARNING: excess bytes: " + excessBytes + ", " +
							"Tag: " + tag.name + ", " +
							"Index: " + index + ", " +
							"IndexRoot: " + rootTimelineContainer.tags.size
				)
			}
			data.position = pos + tagHeader.tagLength
		}
		return tag
	}

	private fun parseTagsFinalize() {
		val soundStream = soundStream
		if (soundStream != null && soundStream.data.length == 0) this.soundStream = null
	}

	private fun processTag(tag: ITag) {
		val currentTagIndex: Int = tags.size - 1
		if (tag is IDefinitionTag) {
			processDefinitionTag(tag, currentTagIndex)
			return
		} else if (tag is IDisplayListTag) {
			processDisplayListTag(tag, currentTagIndex)
			return
		}

		when (tag.type) {
			TagFrameLabel.TYPE, TagDefineSceneAndFrameLabelData.TYPE -> {
				// Frame labels and scenes
				processFrameLabelTag(tag, currentTagIndex)
			}
			TagSoundStreamHead.TYPE, TagSoundStreamHead2.TYPE, TagSoundStreamBlock.TYPE -> {
				// Sound stream
				if (EXTRACT_SOUND_STREAM) processSoundStreamTag(tag, currentTagIndex)
			}
			TagSetBackgroundColor.TYPE -> {
				// Background color
				processBackgroundColorTag(tag as TagSetBackgroundColor, currentTagIndex)
			}
			TagJPEGTables.TYPE -> {
				// Global JPEG Table
				processJPEGTablesTag(tag as TagJPEGTables, currentTagIndex)
			}
		}
	}

	private fun processDefinitionTag(tag: IDefinitionTag, currentTagIndex: Int) {
		if (tag.characterId > 0) {
			// Register definition tag in dictionary
			// key: character id
			// value: definition tag index
			dictionary[tag.characterId] = currentTagIndex
			// Register character id in the current frame's character array
			currentFrame.characters.add(tag.characterId)
		}
	}

	private fun processDisplayListTag(tag: IDisplayListTag, currentTagIndex: Int) {
		when (tag.type) {
			TagShowFrame.TYPE -> {
				currentFrame.tagIndexEnd = currentTagIndex
				if (currentFrame.label == null && currentFrame.frameNumber in frameLabels) {
					currentFrame.label = frameLabels[currentFrame.frameNumber]
				}
				frames.add(currentFrame)
				currentFrame = currentFrame.clone()
				currentFrame.frameNumber = frames.size
				currentFrame.tagIndexStart = currentTagIndex + 1
			}
			TagPlaceObject.TYPE, TagPlaceObject2.TYPE, TagPlaceObject3.TYPE -> {
				currentFrame.placeObject(currentTagIndex, tag as TagPlaceObject)
			}
			TagRemoveObject.TYPE, TagRemoveObject2.TYPE -> {
				currentFrame.removeObject(tag as TagRemoveObject)
			}
		}
	}

	private fun processFrameLabelTag(tag: ITag, currentTagIndex: Int) {
		when (tag.type) {
			TagDefineSceneAndFrameLabelData.TYPE -> {
				val tagSceneAndFrameLabelData: TagDefineSceneAndFrameLabelData = tag as TagDefineSceneAndFrameLabelData
				for (i in 0 until tagSceneAndFrameLabelData.frameLabels.size) {
					val frameLabel = tagSceneAndFrameLabelData.frameLabels[i]
					frameLabels[frameLabel.frameNumber] = frameLabel.name
				}
				for (i in 0 until tagSceneAndFrameLabelData.scenes.size) {
					val scene: SWFScene = tagSceneAndFrameLabelData.scenes[i]
					scenes.add(Scene(scene.offset, scene.name))
				}
			}
			TagFrameLabel.TYPE -> {
				val tagFrameLabel = tag as TagFrameLabel
				currentFrame.label = tagFrameLabel.frameName
			}
		}
	}

	private fun processSoundStreamTag(tag: ITag, currentTagIndex: Int) {
		when (tag.type) {
			TagSoundStreamHead.TYPE, TagSoundStreamHead2.TYPE -> {
				val tagSoundStreamHead = tag as TagSoundStreamHead
				soundStream = SoundStream()
				val soundStream = soundStream!!
				soundStream.compression = tagSoundStreamHead.streamSoundCompression
				soundStream.rate = tagSoundStreamHead.streamSoundRate
				soundStream.size = tagSoundStreamHead.streamSoundSize
				soundStream.type = tagSoundStreamHead.streamSoundType
				soundStream.numFrames = 0
				soundStream.numSamples = 0
			}
			TagSoundStreamBlock.TYPE -> {
				if (soundStream != null) {
					val soundStream = soundStream!!
					if (!hasSoundStream) {
						hasSoundStream = true
						soundStream.startFrame = currentFrame.frameNumber
					}
					val tagSoundStreamBlock = tag as TagSoundStreamBlock
					val soundData = tagSoundStreamBlock.soundData
					soundData.endian = Endian.LITTLE_ENDIAN
					soundData.position = 0
					when (soundStream.compression) {
						SoundCompression.ADPCM -> {
							// ADPCM
							// TODO
						}
						SoundCompression.MP3 -> {
							// MP3
							val numSamples: Int = soundData.readUnsignedShort()
							@Suppress("UNUSED_VARIABLE")
							var seekSamples: Int = soundData.readShort()
							if (numSamples > 0) {
								soundStream.numSamples += numSamples
								soundStream.data.writeBytes(soundData, 4)
							}
						}
					}
					soundStream.numFrames++
				}
			}
		}
	}

	protected fun processBackgroundColorTag(tag: TagSetBackgroundColor, currentTagIndex: Int) {
		backgroundColor = tag.color
	}

	protected fun processJPEGTablesTag(tag: TagJPEGTables, currentTagIndex: Int) {
		jpegTablesTag = tag
	}

	open fun toString(indent: Int = 0, flags: Int = 0): String {
		var str = ""
		if (tags.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Tags:"
			for (i in 0 until tags.size) {
				str += "\n" + tags[i].toString(indent + 4)
			}
		}
		if ((flags and SWF.TOSTRING_FLAG_TIMELINE_STRUCTURE) != 0) {
			if (scenes.size > 0) {
				str += "\n" + " ".repeat(indent + 2) + "Scenes:"
				for (i in 0 until scenes.size) {
					str += "\n" + scenes[i].toString(indent + 4)
				}
			}
			if (frames.size > 0) {
				str += "\n" + " ".repeat(indent + 2) + "Frames:"
				for (i in 0 until frames.size) {
					str += "\n" + frames[i].toString(indent + 4)
				}
			}
			if (layers.size > 0) {
				str += "\n" + " ".repeat(indent + 2) + "Layers:"
				for (i in 0 until layers.size) {
					str += "\n" + " ".repeat(indent + 4) +
							"[" + i + "] " + layers[i].toString(indent + 4)
				}
			}
		}
		return str
	}

	override fun toString() = toString(0, 0)
}
