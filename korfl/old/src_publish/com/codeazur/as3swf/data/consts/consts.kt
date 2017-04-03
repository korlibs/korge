package com.codeazur.as3swf.data.consts

object ActionValueType {
	val STRING: Int = 0
	val FLOAT: Int = 1
	val NULL: Int = 2
	val UNDEFINED: Int = 3
	val REGISTER: Int = 4
	val BOOLEAN: Int = 5
	val DOUBLE: Int = 6
	val INTEGER: Int = 7
	val CONSTANT_8: Int = 8
	val CONSTANT_16: Int = 9

	fun toString(bitmapFormat: Int) = when (bitmapFormat) {
		STRING -> "string"
		FLOAT -> "float"
		NULL -> "null"
		UNDEFINED -> "undefined"
		REGISTER -> "register"
		BOOLEAN -> "boolean"
		DOUBLE -> "double"
		INTEGER -> "integer"
		CONSTANT_8 -> "constant8"
		CONSTANT_16 -> "constant16"
		else -> "unknown"
	}
}

enum class BitmapFormat(val id: Int) {
	BIT_8(3), BIT_15(4), BIT_24_32(5), UNKNOWN(-1);

	companion object {
		val BY_ID = values().map { it.id to it }.toMap()
		operator fun get(index: Int) = BY_ID[index] ?: UNKNOWN
	}
}

object BitmapType {
	const val JPEG = 1
	const val GIF89A = 2
	const val PNG = 3

	fun toString(bitmapFormat: Int): String = when (bitmapFormat) {
		JPEG -> "JPEG"
		GIF89A -> "GIF89a"
		PNG -> "PNG"
		else -> "unknown"
	}
}

object BlendMode {
	const val NORMAL_0 = 0
	const val NORMAL_1 = 1
	const val LAYER = 2
	const val MULTIPLY = 3
	const val SCREEN = 4
	const val LIGHTEN = 5
	const val DARKEN = 6
	const val DIFFERENCE = 7
	const val ADD = 8
	const val SUBTRACT = 9
	const val INVERT = 10
	const val ALPHA = 11
	const val ERASE = 12
	const val OVERLAY = 13
	const val HARDLIGHT = 14

	fun toString(blendMode: Int): String = when (blendMode) {
		NORMAL_0, NORMAL_1 -> "normal"
		LAYER -> "layer"
		MULTIPLY -> "multiply"
		SCREEN -> "screen"
		LIGHTEN -> "lighten"
		DARKEN -> "darken"
		DIFFERENCE -> "difference"
		ADD -> "add"
		SUBTRACT -> "subtract"
		INVERT -> "invert"
		ALPHA -> "alpha"
		ERASE -> "erase"
		OVERLAY -> "overlay"
		HARDLIGHT -> "hardlight"
		else -> "unknown"
	}
}

object CSMTableHint {
	const val THIN = 0
	const val MEDIUM = 1
	const val THICK = 2

	fun toString(csmTableHint: Int): String = when (csmTableHint) {
		THIN -> "thin"
		MEDIUM -> "medium"
		THICK -> "thick"
		else -> "unknown"
	}
}

enum class GradientInterpolationMode(val id: Int) {
	NORMAL(0), LINEAR(1);

	companion object {
		val BY_ID = values().map { it.id to it }.toMap()
		operator fun get(index: Int) = BY_ID[index] ?: NORMAL
	}
}

enum class GradientSpreadMode(val id: Int) {
	PAD(0), REFLECT(1), REPEAT(2);

	companion object {
		val BY_ID = values().map { it.id to it }.toMap()
		operator fun get(index: Int) = BY_ID[index] ?: PAD
	}
}

enum class ScaleMode(val id: Int) {
	NONE(0), HORIZONTAL(1), VERTICAL(2), NORMAL(3);

	companion object {
		val BY_ID = values().map { it.id to it }.toMap()
		operator fun get(index: Int) = BY_ID[index] ?: NORMAL
	}
}

enum class LineCapsStyle(val id: Int) {
	ROUND(0), NO(1), SQUARE(2);

	companion object {
		val BY_ID = values().map { it.id to it }.toMap()
		operator fun get(index: Int) = BY_ID[index] ?: LineCapsStyle.ROUND
	}
}

object LineJointStyle {
	const val ROUND = 0
	const val BEVEL = 1
	const val MITER = 2

	fun toString(lineJointStyle: Int) = when (lineJointStyle) {
		ROUND -> "round"
		BEVEL -> "bevel"
		MITER -> "miter"
		else -> "null"
	}
}

object SoundCompression {
	const val UNCOMPRESSED_NATIVE_ENDIAN = 0
	const val ADPCM = 1
	const val MP3 = 2
	const val UNCOMPRESSED_LITTLE_ENDIAN = 3
	const val NELLYMOSER_16_KHZ = 4
	const val NELLYMOSER_8_KHZ = 5
	const val NELLYMOSER = 6
	const val SPEEX = 11

	fun toString(soundCompression: Int): String {
		return when (soundCompression) {
			UNCOMPRESSED_NATIVE_ENDIAN -> "Uncompressed Native Endian"
			ADPCM -> "ADPCM"
			MP3 -> "MP3"
			UNCOMPRESSED_LITTLE_ENDIAN -> "Uncompressed Little Endian"
			NELLYMOSER_16_KHZ -> "Nellymoser 16kHz"
			NELLYMOSER_8_KHZ -> "Nellymoser 8kHz"
			NELLYMOSER -> "Nellymoser"
			SPEEX -> "Speex"
			else -> return "unknown"
		}
	}
}

object SoundRate {
	const val KHZ_5 = 0
	const val KHZ_11 = 1
	const val KHZ_22 = 2
	const val KHZ_44 = 3

	fun toString(soundRate: Int): String {
		return when (soundRate) {
			KHZ_5 -> "5.5kHz"
			KHZ_11 -> "11kHz"
			KHZ_22 -> "22kHz"
			KHZ_44 -> "44kHz"
			else -> "unknown"
		}
	}
}

object SoundSize {
	const val BIT_8 = 0
	const val BIT_16 = 1

	fun toString(soundSize: Int): String {
		return when (soundSize) {
			BIT_8 -> "8bit"
			BIT_16 -> "16bit"
			else -> "unknown"
		}
	}
}

object SoundType {
	const val MONO = 0
	const val STEREO = 1

	fun toString(soundType: Int): String {
		return when (soundType) {
			MONO -> "mono"
			STEREO -> "stereo"
			else -> "unknown"
		}
	}
}

object VideoCodecID {
	const val H263 = 2
	const val SCREEN = 3
	const val VP6 = 4
	const val VP6ALPHA = 5
	const val SCREENV2 = 6

	fun toString(codecId: Int): String {
		return when (codecId) {
			H263 -> "H.263"
			SCREEN -> "Screen Video"
			VP6 -> "VP6"
			VP6ALPHA -> "VP6 With Alpha"
			SCREENV2 -> "Screen Video V2"
			else -> "unknown"
		}
	}
}

object VideoDeblockingType {
	const val VIDEOPACKET = 0
	const val OFF = 1
	const val LEVEL1 = 2
	const val LEVEL2 = 3
	const val LEVEL3 = 4
	const val LEVEL4 = 5

	fun toString(deblockingType: Int): String {
		return when (deblockingType) {
			VIDEOPACKET -> "videopacket"
			OFF -> "off"
			LEVEL1 -> "level 1"
			LEVEL2 -> "level 2"
			LEVEL3 -> "level 3"
			LEVEL4 -> "level 4"
			else -> "unknown"
		}
	}
}
