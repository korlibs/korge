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
import com.soywiz.korau.format.org.gragravarr.vorbis.VorbisPacket.Companion.TYPE_COMMENTS
import com.soywiz.korau.format.org.gragravarr.vorbis.VorbisPacket.Companion.TYPE_INFO
import com.soywiz.korau.format.org.gragravarr.vorbis.VorbisPacket.Companion.TYPE_SETUP

/**
 * Identifies the right kind of [VorbisPacket] for a given
 * incoming [OggPacket], and creates it
 */
object VorbisPacketFactory {
	/**
	 * Popupulates the metadata packet header,
	 * which is "#vorbis" where # is the type.
	 */
	internal fun populateMetadataHeader(b: ByteArray, type: Int, dataLength: Int) {
		b[0] = IOUtils.fromInt(type)
		b[1] = 'v'.toByte()
		b[2] = 'o'.toByte()
		b[3] = 'r'.toByte()
		b[4] = 'b'.toByte()
		b[5] = 'i'.toByte()
		b[6] = 's'.toByte()
	}

	/**
	 * Does this packet (the first in the stream) contain
	 * the magic string indicating that it's a vorbis
	 * one?
	 */
	fun isVorbisStream(firstPacket: OggPacket): Boolean {
		return if (!firstPacket.isBeginningOfStream) {
			false
		} else isVorbisSpecial(firstPacket)
	}

	internal fun isVorbisSpecial(packet: OggPacket): Boolean {
		val d = packet.data
		if (d.size < 16) return false

		// Ensure "vorbis" on the special types
		val type = d[0]
		if (type.toInt() == 1 || type.toInt() == 3 || type.toInt() == 5) {
			if (d[1] == 'v'.toByte() &&
				d[2] == 'o'.toByte() &&
				d[3] == 'r'.toByte() &&
				d[4] == 'b'.toByte() &&
				d[5] == 'i'.toByte() &&
				d[6] == 's'.toByte()
			) {
				return true
			}
		}
		return false
	}

	/**
	 * Creates the appropriate [VorbisPacket]
	 * instance based on the type.
	 */
	fun create(packet: OggPacket): VorbisPacket {
		// Special header types detection
		if (isVorbisSpecial(packet)) {
			val type = packet.data[0]
			when (type.toInt()) {
				TYPE_INFO -> return VorbisInfo(packet)
				TYPE_COMMENTS -> return VorbisComments(packet)
				TYPE_SETUP -> return VorbisSetup(packet)
			}
		}

		return VorbisAudioData(packet)
	}
}
