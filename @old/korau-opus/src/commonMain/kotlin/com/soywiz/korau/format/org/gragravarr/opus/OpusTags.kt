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
import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*
import com.soywiz.korau.format.org.gragravarr.vorbis.*
import com.soywiz.korio.stream.*

/**
 * This is a [VorbisComments] with an Opus metadata
 * block header, rather than the usual vorbis one.
 */
class OpusTags : VorbisStyleComments, OpusPacket, OggAudioTagsHeader {

	/**
	 * 8 byte OpusTags
	 */
	override protected val headerSize: Int
		get() = 8

	constructor(packet: OggPacket) : super(packet, OpusPacket.MAGIC_TAGS_BYTES.size) {

		// Verify the type
		if (!IOUtils.byteRangeMatches(OpusPacket.MAGIC_TAGS_BYTES, data!!, 0)) {
			throw IllegalArgumentException("Invalid type, not a Opus Header")
		}
	}

	constructor() : super() {}

	/**
	 * Opus doesn't do the framing bit if the tags are
	 * null padded.
	 */
	override protected fun hasFramingBit(): Boolean {
		return false
	}

	/**
	 * Magic string
	 */
	override protected fun populateMetadataHeader(b: ByteArray, dataLength: Int) {
		arraycopy(OpusPacket.MAGIC_TAGS_BYTES, 0, b, 0, OpusPacket.MAGIC_TAGS_BYTES.size)
	}

	override protected fun populateMetadataFooter(out: SyncOutputStream) {
		// No footer needed on Opus Tag Packets
	}
}
