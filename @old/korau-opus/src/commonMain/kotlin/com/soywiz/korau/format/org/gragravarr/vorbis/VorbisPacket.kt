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

/**
 * Parent of all Vorbis packets
 */
interface VorbisPacket : OggStreamPacket {

	/**
	 * How big is the header on this packet?
	 * For Metadata packets it's normally 7 bytes,
	 * otherwise for audio packets there is no header.
	 */
	val headerSize: Int

	/**
	 * Have the metadata header populated into the data,
	 * normally used when writing out.
	 * See [VorbisPacketFactory.populateMetadataHeader]
	 */
	fun populateMetadataHeader(b: ByteArray, dataLength: Int)

	companion object {
		val TYPE_INFO = 1
		val TYPE_COMMENTS = 3
		val TYPE_SETUP = 5

		val HEADER_LENGTH_METADATA = 7
		val HEADER_LENGTH_AUDIO = 0
	}
}