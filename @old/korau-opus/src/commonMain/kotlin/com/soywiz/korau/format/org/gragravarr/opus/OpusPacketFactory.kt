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

import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.opus.OpusPacket.Companion.MAGIC_HEADER_BYTES
import com.soywiz.korau.format.org.gragravarr.opus.OpusPacket.Companion.MAGIC_TAGS_BYTES

/**
 * Identifies the right kind of [OpusPacket] for a given
 * incoming [OggPacket], and creates it
 */
class OpusPacketFactory : HighLevelOggStreamPacket() {
	companion object {
		/**
		 * Does this packet (the first in the stream) contain
		 * the magic string indicating that it's an opus
		 * one?
		 */
		fun isOpusStream(firstPacket: OggPacket): Boolean {
			return if (!firstPacket.isBeginningOfStream) {
				false
			} else isOpusSpecial(firstPacket)
		}

		protected fun isOpusSpecial(packet: OggPacket): Boolean {
			val d = packet.data

			// Is it an Opus Header or Tags packet?
			if (d.size < 12) return false
			if (IOUtils.byteRangeMatches(MAGIC_HEADER_BYTES, d, 0)) return true
			return if (IOUtils.byteRangeMatches(MAGIC_TAGS_BYTES, d, 0)) true else false

			// Not a known Opus special packet
		}

		/**
		 * Creates the appropriate [OpusPacket]
		 * instance based on the type.
		 */
		fun create(packet: OggPacket): OpusPacket {
			// Special header types detection
			if (isOpusSpecial(packet)) {
				val type = packet.data!![4]
				when (type) {
					'H'.toByte() // OpusHead
					-> return OpusInfo(packet)
					'T'.toByte() // OpusTags
					-> return OpusTags(packet)
				}
			}

			return OpusAudioData(packet)
		}
	}
}