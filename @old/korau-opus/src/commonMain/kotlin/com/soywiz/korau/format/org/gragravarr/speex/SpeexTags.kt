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
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*
import com.soywiz.korau.format.org.gragravarr.vorbis.*
import com.soywiz.korio.stream.*

/**
 * This is a [VorbisComments] with an Speex metadata
 * block header, rather than the usual vorbis one.
 */
class SpeexTags : VorbisStyleComments, SpeexPacket, OggAudioTagsHeader {

	/**
	 * 0 byte header
	 */
	override protected val headerSize: Int
		get() = 0

	constructor(packet: OggPacket) : super(packet, 0) {

		// Verify the Packet # and Granule Position
		if (packet.sequenceNumber !== 1 && packet.granulePosition != 0L) {
			throw IllegalArgumentException("Invalid packet details, not Speex Tags")
		}
	}

	constructor() : super() {}

	/**
	 * We think that Speex doesn't do a framing bit if the
	 * tags are null padded
	 */
	override protected fun hasFramingBit(): Boolean {
		return false
	}

	/**
	 * There is no header on Speex tags
	 */
	override protected fun populateMetadataHeader(b: ByteArray, dataLength: Int) {}

	override protected fun populateMetadataFooter(out: SyncOutputStream) {
		// No footer needed on Speex Tag Packets
	}
}
