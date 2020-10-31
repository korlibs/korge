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

/**
 * Includes extensive CODEC setup information as well as the
 * complete VQ and Huffman codebooks needed for decode
 */
class VorbisSetup : HighLevelOggStreamPacket, VorbisPacket, OggAudioSetupHeader {

	override val headerSize: Int
		get() = VorbisPacket.HEADER_LENGTH_METADATA

	// Example first bit of decoding
	val numberOfCodebooks: Int
		get() {
			val data = this.data
			var number = -1
			if (data != null && data!!.size >= 10) {
				number = IOUtils.toInt(data!![8])
			}
			return number + 1
		}

	constructor(pkt: OggPacket) : super(pkt, null) {

		// Made up of:
		//  Codebooks
		//  Time Domain Transforms
		//  Floors
		//  Residues
		//  Mappings
		//  Modes
	}

	constructor() : super() {}

	override fun populateMetadataHeader(b: ByteArray, dataLength: Int) {
		VorbisPacketFactory.populateMetadataHeader(b, VorbisPacket.TYPE_SETUP, dataLength)
	}
}
