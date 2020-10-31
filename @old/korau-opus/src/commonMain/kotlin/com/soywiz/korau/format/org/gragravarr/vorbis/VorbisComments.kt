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
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*
import com.soywiz.korio.*
import com.soywiz.korio.stream.*

/**
 * Holds encoder information and user specified tags
 */
class VorbisComments : VorbisStyleComments, VorbisPacket, OggAudioTagsHeader {
	override val headerSize: Int
		get() = VorbisPacket.HEADER_LENGTH_METADATA

	constructor(pkt: OggPacket) : super(pkt, VorbisPacket.HEADER_LENGTH_METADATA) {}
	constructor() : super() {}

	/**
	 * Vorbis Comments have framing bits if there's padding
	 * after the end of the defined comments
	 */
	protected override fun hasFramingBit(): Boolean {
		return true
	}

	override fun populateMetadataHeader(b: ByteArray, dataLength: Int) {
		VorbisPacketFactory.populateMetadataHeader(b, VorbisPacket.TYPE_COMMENTS, dataLength)
	}

	override fun populateMetadataFooter(out: SyncOutputStream) {
		// Vorbis requires a single framing bit at the end
		try {
			out.write8(1)
		} catch (e: IOException) {
			// Shouldn't happen here!
			throw RuntimeException(e)
		}

	}
}
