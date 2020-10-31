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
package com.soywiz.korau.format.org.gragravarr.flac

import com.soywiz.kmem.*
import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*
import com.soywiz.korau.format.org.gragravarr.vorbis.*
import com.soywiz.korio.stream.*

/**
 * This is a [VorbisComments] with a Flac metadata
 * block header, rather than the usual vorbis one.
 */
class FlacTags : VorbisStyleComments, OggAudioTagsHeader {

	/**
	 * Type plus three byte length
	 */
	override val headerSize: Int
		get() = 4

	constructor(packet: OggPacket) : super(packet, 4) {

		// Verify the type
		val type = this.data!![0]
		if (type != FlacMetadataBlock.VORBIS_COMMENT) {
			throw IllegalArgumentException("Invalid type $type")
		}
	}

	constructor() : super() {}

	/**
	 * Flac doesn't do the framing bit if the tags are
	 * null padded.
	 */
	override fun hasFramingBit(): Boolean {
		return false
	}

	/**
	 * Type plus three byte length
	 */
	override fun populateMetadataHeader(b: ByteArray, dataLength: Int) {
		b[0] = FlacMetadataBlock.VORBIS_COMMENT
		IOUtils.putInt3BE(b, 1, dataLength.toLong())
	}

	override fun populateMetadataFooter(out: SyncOutputStream) {
		// No footer needed on FLAC Tag Packets
	}

	class FlacTagsAsMetadata(data: ByteArray) : FlacMetadataBlock(FlacMetadataBlock.VORBIS_COMMENT) {
		val tags: FlacTags

		override val data: ByteArray
			get() = tags.data!!

		init {

			// This is the only metadata which needs the type
			//  and length in addition to the main data
			val d = ByteArray(data.size + 4)
			d[0] = FlacMetadataBlock.VORBIS_COMMENT
			arraycopy(data, 0, d, 4, data.size)
			this.tags = FlacTags(OggPacket(d))
		}

		protected override fun write(out: SyncOutputStream) {
			throw IllegalStateException("Must not call directly")
		}
	}
}
