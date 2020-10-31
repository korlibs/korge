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
package com.soywiz.korau.format.org.gragravarr.speex

import com.soywiz.kmem.*
import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*

/**
 * The identification header identifies the bitstream as Speex,
 * and includes the Speex version, the simple audio characteristics
 * of the stream such as sample rate and number of channels etc.
 */
class SpeexInfo : HighLevelOggStreamPacket, SpeexPacket, OggAudioInfoHeader {
	override var versionString: String = ""
	set(value) {
		var versionString = value
		if (versionString.length > 20) {
			versionString = versionString.substring(0, 20)
		}
		field = versionString
	}
	var versionId: Int = 0
	var rate: Long = 0
	var mode: Int = 0
	var modeBitstreamVersion: Int = 0
	override var numChannels: Int = 0
	var bitrate: Int = 0
	var frameSize: Int = 0
	var vbr: Int = 0
	var framesPerPacket: Int = 0
	var extraHeaders: Int = 0
	var reserved1: Int = 0
	var reserved2: Int = 0
	override val sampleRate: Int
		get() = rate.toInt()

	override val preSkip: Int
		get() = 0

	constructor() : super() {
		versionString = "Gagravarr Ogg v0.8"
		versionId = 1
	}

	constructor(pkt: OggPacket) : super(pkt, null) {

		// Verify the type
		val data = this.data!!
		if (!IOUtils.byteRangeMatches(SpeexPacket.MAGIC_HEADER_BYTES, data, 0)) {
			throw IllegalArgumentException("Invalid type, not a Speex Header")
		}

		// Parse
		versionString = IOUtils.removeNullPadding(IOUtils.getUTF8(data, 8, 20))
		versionId = IOUtils.getInt4(data, 28).toInt()

		val headerSize = IOUtils.getInt4(data, 32) as Int
		if (headerSize != data.size) {
			throw IllegalArgumentException("Invalid Speex Header, expected " + headerSize + " bytes, found " + data.size)
		}

		rate = IOUtils.getInt4(data, 36)
		mode = IOUtils.getInt4(data, 40).toInt()
		modeBitstreamVersion = IOUtils.getInt4(data, 44).toInt()
		numChannels = IOUtils.getInt4(data, 48).toInt()
		bitrate = IOUtils.getInt4(data, 52).toInt()
		frameSize = IOUtils.getInt4(data, 56).toInt()
		vbr = IOUtils.getInt4(data, 60).toInt()
		framesPerPacket = IOUtils.getInt4(data, 64).toInt()
		extraHeaders = IOUtils.getInt4(data, 68).toInt()
		reserved1 = IOUtils.getInt4(data, 72).toInt()
		reserved2 = IOUtils.getInt4(data, 76).toInt()
	}

	override fun write(): OggPacket {
		val data = ByteArray(80)
		arraycopy(SpeexPacket.MAGIC_HEADER_BYTES, 0, data, 0, 8)

		IOUtils.putUTF8(data, 8, versionString!!)
		IOUtils.putInt4(data, 28, versionId)

		IOUtils.putInt4(data, 32, data.size)

		IOUtils.putInt4(data, 36, rate)
		IOUtils.putInt4(data, 40, mode)
		IOUtils.putInt4(data, 44, modeBitstreamVersion)
		IOUtils.putInt4(data, 48, numChannels)
		IOUtils.putInt4(data, 52, bitrate)
		IOUtils.putInt4(data, 56, frameSize)
		IOUtils.putInt4(data, 60, vbr)
		IOUtils.putInt4(data, 64, framesPerPacket)
		IOUtils.putInt4(data, 68, extraHeaders)
		IOUtils.putInt4(data, 72, reserved1)
		IOUtils.putInt4(data, 76, reserved2.toLong())

		this.data = data
		return super.write()
	}
}
