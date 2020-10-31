/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.soywiz.korau.format.org.gragravarr.vorbis

import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*
import kotlin.math.*

/**
 * The identification header identifies the bitstream as Vorbis,
 * Vorbis version, and the simple audio characteristics of the
 * stream such as sample rate and number of channels.
 */
class VorbisInfo : HighLevelOggStreamPacket, VorbisPacket, OggAudioInfoHeader {
	var version: Int = 0
		private set
	var channels: Int = 0
	var rate: Long = 0
	var bitrateUpper: Int = 0
	var bitrateNominal: Int = 0
	var bitrateLower: Int = 0
	private var blocksizes: Int = 0

	override val headerSize: Int get() = VorbisPacket.HEADER_LENGTH_METADATA
	override val versionString: String get() = "$version"
	override val sampleRate: Int get() = rate.toInt()
	override val preSkip: Int get() = 0

	var blocksize0: Int
		get() {
			val part = blocksizes and 0x0f
			return 2.0.pow(part.toDouble()).toInt()
		}
		set(blocksize) {
			val part = log2(blocksize.toDouble()).toInt()
			blocksizes = (blocksizes and 0xf0) + part
		}

	var blocksize1: Int
		get() {
			val part = blocksizes and 0xf0 shr 4
			return 2.0.pow(part.toDouble()).toInt()
		}
		set(blocksize) {
			val part = log2(blocksize.toDouble()).toInt()
			blocksizes = (blocksizes and 0x0f) + (part shl 4)
		}

	constructor() : super() {
		version = 0
	}

	constructor(pkt: OggPacket) : super(pkt, null) {

		// Parse
		val data = this.data!!
		version = IOUtils.getInt4(data, 7).toInt()
		if (version != 0) {
			throw IllegalArgumentException("Unsupported vorbis version $version detected")
		}

		channels = data[11].toInt()
		rate = IOUtils.getInt4(data, 12)
		bitrateUpper = IOUtils.getInt4(data, 16).toInt()
		bitrateNominal = IOUtils.getInt4(data, 20).toInt()
		bitrateLower = IOUtils.getInt4(data, 24).toInt()

		blocksizes = IOUtils.toInt(data[28])
		val framingBit = data[29]
		if (framingBit.toInt() == 0) {
			throw IllegalArgumentException("Framing bit not set, invalid")
		}
	}

	override fun populateMetadataHeader(b: ByteArray, dataLength: Int) {
		VorbisPacketFactory.populateMetadataHeader(b, VorbisPacket.TYPE_INFO, dataLength)
	}

	override fun write(): OggPacket {
		val data = ByteArray(30)
		populateMetadataHeader(data, data.size)

		IOUtils.putInt4(data, 7, version)
		data[11] = IOUtils.fromInt(channels)
		IOUtils.putInt4(data, 12, rate)
		IOUtils.putInt4(data, 16, bitrateUpper)
		IOUtils.putInt4(data, 20, bitrateNominal)
		IOUtils.putInt4(data, 24, bitrateLower)
		data[28] = IOUtils.fromInt(blocksizes)
		data[29] = 1

		this.data = data
		return super.write()
	}

	override val numChannels get() = channels
}
