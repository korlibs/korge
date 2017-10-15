package com.codeazur.as3swf.data.etc

import com.codeazur.as3swf.utils.FlashByteArray
import com.soywiz.korma.geom.Point2d
import kotlin.math.floor

class CurvedEdge(aFrom: Point2d, var control: Point2d, aTo: Point2d, aLineStyleIdx: Int = 0, aFillStyleIdx: Int = 0) : StraightEdge(aFrom, aTo, aLineStyleIdx, aFillStyleIdx), com.codeazur.as3swf.data.etc.IEdge {
	override fun reverseWithNewFillStyle(newFillStyleIdx: Int) = CurvedEdge(to, control, from, lineStyleIdx, newFillStyleIdx)
	override fun toString(): String = "stroke:$lineStyleIdx, fill:$fillStyleIdx, start:$from, control:$control, end:$to"
}

open class StraightEdge(
	override var from: Point2d,
	override var to: Point2d,
	override var lineStyleIdx: Int = 0,
	override var fillStyleIdx: Int = 0
) : IEdge {
	override fun reverseWithNewFillStyle(newFillStyleIdx: Int): IEdge = StraightEdge(to, from, lineStyleIdx, newFillStyleIdx)
	override fun toString() = "stroke:$lineStyleIdx, fill:$fillStyleIdx, start:$from, end:$to"
}

interface IEdge {
	val from: Point2d
	val to: Point2d
	val lineStyleIdx: Int
	val fillStyleIdx: Int
	fun reverseWithNewFillStyle(newFillStyleIdx: Int): com.codeazur.as3swf.data.etc.IEdge
}

@Suppress("unused")
open class MPEGFrame(
	var data: FlashByteArray = FlashByteArray()
) {
	companion object {
		val MPEG_VERSION_1_0 = 0
		val MPEG_VERSION_2_0 = 1
		val MPEG_VERSION_2_5 = 2

		val MPEG_LAYER_I = 0
		val MPEG_LAYER_II = 1
		val MPEG_LAYER_III = 2

		val CHANNEL_MODE_STEREO = 0
		val CHANNEL_MODE_JOINT_STEREO = 1
		val CHANNEL_MODE_DUAL = 2
		val CHANNEL_MODE_MONO = 3

		protected val mpegBitrates = listOf(
			listOf(
				listOf(0, 32, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, -1),
				listOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, -1),
				listOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, -1)
			),

			listOf(
				listOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, -1),
				listOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1),
				listOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1)
			)
		)

		protected val mpegSamplingRates = listOf(
			listOf(44100, 48000, 32000),
			listOf(22050, 24000, 16000),
			listOf(11025, 12000, 8000)
		)
	}

	var version: Int = 0
	var layer: Int = 0
	var bitrate: Int = 0
	var samplingrate: Int = 0
	var padding: Boolean = false
	var channelMode: Int = 0
	var channelModeExt = 0
	var copyright = false
	var original = false
	var emphasis = 0

	protected var _header: FlashByteArray = FlashByteArray()

	protected var _crc: FlashByteArray

	var hasCRC: Boolean = false

	var samples: Int = 1152

	val crc: Int get() {
		_crc.position = 0
		return _crc.readUnsignedShort()
	}

	val size: Int get() {
		var ret: Int
		if (layer == MPEGFrame.Companion.MPEG_LAYER_I) {
			ret = floor((12000.0 * bitrate) / samplingrate).toInt()
			if (padding) {
				ret++
			}
			// one slot is 4 bytes long
			ret = ret shl 2
		} else {
			ret = floor((if (version == MPEGFrame.Companion.MPEG_VERSION_1_0) 144000.0 else 72000.0) * bitrate / samplingrate).toInt()
			if (padding) {
				ret++
			}
		}
		// subtract header size and (if present) crc size
		return ret - 4 - (if (hasCRC) 2 else 0)
	}

	fun setHeaderByteAt(index: Int, value: Int): Unit {
		when (index) {
			0 -> {
				if (value != 0xff) throw Error("Not a MPEG header.")
			}
			1 -> {
				if ((value and 0xe0) != 0xe0) throw Error("Not a MPEG header.")
				// get the mpeg version (we only support mpeg 1.0 and 2.0)
				val mpegVersionBits = (value and 0x18) ushr 3
				when (mpegVersionBits) {
					3 -> version = MPEGFrame.Companion.MPEG_VERSION_1_0
					2 -> version = MPEGFrame.Companion.MPEG_VERSION_2_0
					else -> throw Error("Unsupported MPEG version.")
				}
				// get the mpeg layer version (we only support layer III)
				val mpegLayerBits = (value and 0x06) ushr 1
				when (mpegLayerBits) {
					1 -> layer = MPEGFrame.Companion.MPEG_LAYER_III
					else -> throw Error("Unsupported MPEG layer.")
				}
				// is the frame secured by crc?
				hasCRC = (value and 0x01) == 0
			}
			2 -> {
				val bitrateIndex = ((value and 0xf0) ushr 4)
				// get the frame's bitrate
				if (bitrateIndex == 0 || bitrateIndex == 0x0f) {
					throw Error("Unsupported bitrate index.")
				}
				bitrate = MPEGFrame.Companion.mpegBitrates[version][layer][bitrateIndex]
				// get the frame's samplingrate
				val samplingrateIndex = ((value and 0x0c) ushr 2)
				if (samplingrateIndex == 3) {
					throw Error("Unsupported samplingrate index.")
				}
				samplingrate = MPEGFrame.Companion.mpegSamplingRates[version][samplingrateIndex]
				// is the frame padded?
				padding = ((value and 0x02) == 0x02)
			}
			3 -> {
				// get the frame's channel mode:
				// 0: stereo
				// 1: joint stereo
				// 2: dual channel
				// 3: mono
				channelMode = ((value and 0xc0) ushr 6)
				// get the frame's extended channel mode (only for joint stereo):
				channelModeExt = ((value and 0x30) ushr 4)
				// get the copyright flag
				copyright = ((value and 0x08) == 0x08)
				// get the original flag
				original = ((value and 0x04) == 0x04)
				// get the emphasis:
				// 0: none
				// 1: 50/15 ms
				// 2: reserved
				// 3: ccit j.17
				emphasis = (value and 0x02)
			}
			else -> throw Error("Index out of bounds.")
		}
		// store the raw header byte for easy access
		_header[index] = value
	}

	fun setCRCByteAt(index: Int, value: Int): Unit {
		if (index > 1) {
			throw Error("Index out of bounds.")
		}
		_crc[index] = value
	}

	init {
		_header.writeByte(0)
		_header.writeByte(0)
		_header.writeByte(0)
		_header.writeByte(0)
		_crc = FlashByteArray()
		_crc.writeByte(0)
		_crc.writeByte(0)
	}

	fun getFrame(): FlashByteArray {
		val ba = FlashByteArray()
		ba.writeBytes(_header, 0, 4)
		if (hasCRC) {
			ba.writeBytes(_crc, 0, 2)
		}
		ba.writeBytes(data)
		return ba
	}

	override fun toString(): String {
		var str: String = "MPEG "
		str += when (version) {
			MPEGFrame.MPEG_VERSION_1_0 -> "1.0 "
			MPEGFrame.MPEG_VERSION_2_0 -> "2.0 "
			MPEGFrame.MPEG_VERSION_2_5 -> "2.5 "
			else -> "?.? "
		}
		str += when (layer) {
			MPEGFrame.MPEG_LAYER_I -> "Layer I"
			MPEGFrame.MPEG_LAYER_II -> "Layer II"
			MPEGFrame.MPEG_LAYER_III -> "Layer III"
			else -> "Layer ?"
		}
		val channel = when (channelMode) {
			0 -> "Stereo"
			1 -> "Joint stereo"
			2 -> "Dual channel"
			3 -> "Mono"
			else -> "unknown"
		}
		return "$str, $bitrate kbit/s, $samplingrate Hz, $channel, $size bytes"
	}
}
