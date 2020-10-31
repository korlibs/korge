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

/**
 * Raw, compressed audio data
 */
class OpusAudioData(pkt: OggPacket?, data: ByteArray?) : OggStreamAudioData(pkt, data), OpusPacket {
	constructor(pkt: OggPacket) : this(pkt, null) {}
	constructor(data: ByteArray) : this(null, data) {}


	private var numFrames = -1
	private var numSamples = -1

	val isEndOfStream: Boolean get() = oggPacket!!.isEndOfStream

	val numberOfFrames: Int
		get() {
			if (numFrames == -1) calculateStructure()
			return numFrames
		}
	val numberOfSamples: Int
		get() {
			if (numSamples == -1) calculateStructure()
			return numSamples
		}

	private fun calculateStructure() {
		val d = this.data
		numFrames = packet_get_nb_frames(d!!)
		numSamples = numFrames * packet_get_samples_per_frame(d, OPUS_GRANULE_RATE)
	}

	companion object {
		/** Opus is special - granule always runs at 48kHz  */
		val OPUS_GRANULE_RATE = 48000

		private fun packet_get_samples_per_frame(data: ByteArray, fs: Int): Int {
			var audiosize: Int
			if (data[0] and 0x80 != 0) {
				audiosize = data[0] shr 3 and 0x3
				audiosize = (fs shl audiosize) / 400
			} else if (data[0] and 0x60 == 0x60) {
				audiosize = if (data[0] and 0x08 != 0) fs / 50 else fs / 100
			} else {
				audiosize = data[0] shr 3 and 0x3
				if (audiosize == 3)
					audiosize = fs * 60 / 1000
				else
					audiosize = (fs shl audiosize) / 100
			}
			return audiosize
		}

		private fun packet_get_nb_frames(packet: ByteArray): Int {
			var count = 0
			if (packet.size < 1) {
				return -1
			}
			count = packet[0] and 0x3
			return if (count == 0)
				1
			else if (count != 3)
				2
			else if (packet.size < 2)
				-4
			else
				packet[1] and 0x3F
		}
	}
}
