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

import com.soywiz.korau.format.org.gragravarr.ogg.*

/**
 * Identifies the right kind of [SpeexPacket] for a given
 * incoming [OggPacket], and creates it
 */
class SpeexPacketFactory : HighLevelOggStreamPacket() {
	companion object {
		/**
		 * Does this packet (the first in the stream) contain
		 * the magic string indicating that it's an speex
		 * one?
		 */
		fun isSpeexStream(firstPacket: OggPacket): Boolean {
			return if (!firstPacket.isBeginningOfStream) {
				false
			} else isSpeexSpecial(firstPacket)
		}

		protected fun isSpeexSpecial(packet: OggPacket): Boolean {
			val d = packet.data

			// Is it a Speex Info packet?
			if (d.size < 72) return false
			return if (IOUtils.byteRangeMatches(SpeexPacket.MAGIC_HEADER_BYTES, d, 0)) true else false

			// Not a known Speex special packet
		}

		/**
		 * Creates the appropriate [SpeexPacket]
		 * instance based on the type.
		 */
		fun create(packet: OggPacket): SpeexPacket {
			// Special header types detection
			if (isSpeexSpecial(packet)) {
				return SpeexInfo(packet)
			}
			return if (packet.sequenceNumber == 1 && packet.granulePosition == 0L) {
				SpeexTags(packet)
			} else SpeexAudioData(packet)

		}
	}
}