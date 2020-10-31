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
import com.soywiz.korau.format.org.gragravarr.vorbis.*
import com.soywiz.korio.stream.*

/**
 * This is a [VorbisComments] with an Theora metadata
 * block header, rather than the usual Vorbis one.
 */
class TheoraComments : VorbisStyleComments, TheoraPacket {

	/**
	 * 8 bytes - type + theora
	 */
	override protected val headerSize: Int
		get() = 8

	constructor(packet: OggPacket) : super(packet, 7) {

		// Verify the type
		if (data!![0] !== TheoraPacket.TYPE_COMMENTS.toByte()) {
			throw IllegalArgumentException("Invalid type, not a Theora Commetns")
		}
	}

	constructor() : super() {}

	/**
	 * We think that Theora follows the Vorbis model, and has
	 * a framing bit if the comments are null-padded
	 */
	override protected fun hasFramingBit(): Boolean {
		return true
	}

	/**
	 * Magic string
	 */
	override protected fun populateMetadataHeader(b: ByteArray, dataLength: Int) {
		TheoraPacketFactory.populateMetadataHeader(b, TheoraPacket.TYPE_COMMENTS, dataLength)
	}

	override protected fun populateMetadataFooter(out: SyncOutputStream) {
		// No footer needed on Theora Comment Packets
	}
}
