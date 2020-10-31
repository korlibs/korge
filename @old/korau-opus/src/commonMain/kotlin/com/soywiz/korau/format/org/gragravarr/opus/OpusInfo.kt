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
package com.soywiz.korau.format.org.gragravarr.opus

import com.soywiz.kmem.*
import com.soywiz.korau.format.org.concentus.internal.*
import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*
import com.soywiz.korio.lang.*

/**
 * The identification header identifies the bitstream as Opus,
 * and includes the Opus version, the simple audio characteristics
 * of the stream such as sample rate and number of channels etc.
 */
class OpusInfo : HighLevelOggStreamPacket, OpusPacket, OggAudioInfoHeader {
	var version: Byte = 0
		private set
	var majorVersion: Int = 0
		private set
	var minorVersion: Int = 0
		private set

	override var numChannels: Int = 0
	override var preSkip: Int = 0
	var rate: Long = 0
		private set
	var outputGain: Int = 0
	var channelMappingFamily: Byte = 0.toByte()
	var streamCount: Byte = 0
		private set
	var twoChannelStreamCount: Byte = 0
		private set
	var channelMapping: ByteArray? = null
		private set
	override val versionString: String
		get() = majorVersion.toString() + "." + minorVersion
	override val sampleRate: Int
		get() = rate.toInt()

	constructor() : super() {
		version = 1
	}

	constructor(pkt: OggPacket) : super(pkt, null) {

		// Verify the type
		val data = this.data!!
		if (!IOUtils.byteRangeMatches(OpusPacket.MAGIC_HEADER_BYTES, data, 0)) {
			throw IllegalArgumentException("Invalid type, not a Opus Header")
		}

		// Parse
		version = data[8]
		parseVersion()
		if (majorVersion != 0) {
			throw IllegalArgumentException("Unsupported Opus version $version at major version $majorVersion detected")
		}

		numChannels = data[9].toInt()
		preSkip = IOUtils.getInt2(data, 10)
		rate = IOUtils.getInt4(data, 12)
		outputGain = IOUtils.getInt2(data, 16)

		channelMappingFamily = data[18]
		if (channelMappingFamily.toInt() != 0) {
			streamCount = data[19]
			twoChannelStreamCount = data[20]
			channelMapping = ByteArray(numChannels)
			arraycopy(data, 21, channelMapping!!, 0, numChannels)
		}
	}

	override fun write(): OggPacket {
		var length = 19
		if (channelMappingFamily.toInt() != 0) {
			length += 2
			length += numChannels
		}
		val data = ByteArray(length)
		arraycopy(OpusPacket.MAGIC_HEADER_BYTES, 0, data, 0, 8)

		data[8] = version
		data[9] = numChannels.toByte()
		IOUtils.putInt2(data, 10, preSkip)
		IOUtils.putInt4(data, 12, rate)
		IOUtils.putInt2(data, 16, outputGain)

		data[18] = channelMappingFamily
		if (channelMappingFamily.toInt() != 0) {
			data[19] = streamCount
			data[20] = twoChannelStreamCount
			arraycopy(channelMapping!!, 0, data, 21, numChannels)
		}

		this.data = data
		return super.write()
	}

	private fun parseVersion() {
		minorVersion = version and 0xf
		majorVersion = version shr 4
	}

	fun setSampleRate(rate: Long) {
		this.rate = rate
	}
}
