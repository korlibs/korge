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
package com.soywiz.korau.format.org.gragravarr.theora

import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.theora.TheoraPacket.Companion.TYPE_COMMENTS
import com.soywiz.korau.format.org.gragravarr.theora.TheoraPacket.Companion.TYPE_IDENTIFICATION
import com.soywiz.korau.format.org.gragravarr.theora.TheoraPacket.Companion.TYPE_SETUP

/**
 * Identifies the right kind of [TheoraPacket] for a given
 * incoming [OggPacket], and creates it
 */
class TheoraPacketFactory : HighLevelOggStreamPacket() {
	companion object {
		/**
		 * Popupulates the metadata packet header,
		 * which is "#theora" where # is the type.
		 */
		fun populateMetadataHeader(b: ByteArray, type: Int, dataLength: Int) {
			b[0] = IOUtils.fromInt(type)
			b[1] = 't'.toByte()
			b[2] = 'h'.toByte()
			b[3] = 'e'.toByte()
			b[4] = 'o'.toByte()
			b[5] = 'r'.toByte()
			b[6] = 'a'.toByte()
		}

		/**
		 * Does this packet (the first in the stream) contain
		 * the magic string indicating that it's an theora
		 * one?
		 */
		fun isTheoraStream(firstPacket: OggPacket): Boolean {
			return if (!firstPacket.isBeginningOfStream) {
				false
			} else isTheoraSpecial(firstPacket)
		}

		protected fun isTheoraSpecial(packet: OggPacket): Boolean {
			val d = packet.data
			if (d.size < 16) return false

			// Ensure it's the right special type, then theora
			val type = d[0]
			if (type == TYPE_IDENTIFICATION as Byte ||
				type == TYPE_COMMENTS as Byte ||
				type == TYPE_SETUP as Byte
			) {
				if (d[1] == 't'.toByte() &&
					d[2] == 'h'.toByte() &&
					d[3] == 'e'.toByte() &&
					d[4] == 'o'.toByte() &&
					d[5] == 'r'.toByte() &&
					d[6] == 'a'.toByte()
				) {
					return true
				}
			}
			return false
		}

		/**
		 * Creates the appropriate [TheoraPacket]
		 * instance based on the type.
		 */
		fun create(packet: OggPacket): TheoraPacket {
			val type = packet.data[0]

			// Special header types detection
			if (isTheoraSpecial(packet)) {
				when (type) {
					TYPE_IDENTIFICATION as Byte -> return TheoraInfo(packet)
					TYPE_COMMENTS as Byte -> return TheoraComments(packet)
					TYPE_SETUP as Byte -> return TheoraSetup(packet)
				}
			}

			return TheoraVideoData(packet)
		}
	}
}
