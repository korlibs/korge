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
package com.soywiz.korau.format.org.gragravarr.ogg.audio

import com.soywiz.korau.format.org.gragravarr.flac.*
import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.opus.*
import com.soywiz.korau.format.org.gragravarr.speex.*
import com.soywiz.korau.format.org.gragravarr.vorbis.*


/**
 * Streaming friendly way to get at the headers at the
 * start of an [OggAudioStream], allowing for the fact
 * that they may be interspersed with other streams' data.
 */
class OggAudioStreamHeaders private constructor(
	/**
	 * @return The stream id of the overall audio stream
	 */
	override val sid: Int,
	/**
	 * @return The type of the audio stream
	 */
	override val type: OggStreamIdentifier.OggStreamType,
	/**
	 * @return The information / identification of the stream and audio encoding
	 */
	override val info: OggAudioInfoHeader
) : OggAudioHeaders {
	/**
	 * @return The Tags / Comments describing the stream
	 */
	override var tags: OggAudioTagsHeader? = null; private set
	/**
	 * @return The Setup information for the audio encoding, if used in the format
	 */
	override var setup: OggAudioSetupHeader? = null; private set

	/**
	 * Creates an appropriate high level packet
	 */
	protected fun createNext(packet: OggPacket): OggStreamPacket? {
		return if (type === OggStreamIdentifier.OGG_VORBIS) {
			VorbisPacketFactory.create(packet)
		} else if (type === OggStreamIdentifier.SPEEX_AUDIO) {
			SpeexPacketFactory.create(packet)
		} else if (type === OggStreamIdentifier.OPUS_AUDIO) {
			OpusPacketFactory.create(packet)
		} else if (type === OggStreamIdentifier.OGG_FLAC) {
			// TODO Finish FLAC support
			null
		} else {
			throw IllegalArgumentException("Unsupported stream of type $type")
		}
	}

	/**
	 * Populates with the next header
	 *
	 * @return Do any more headers remain to be populated?
	 */
	fun populate(packet: OggPacket): Boolean {
		// TODO Finish the flac support properly
		if (type === OggStreamIdentifier.OGG_FLAC) {
			if (tags == null) {
				tags = FlacTags(packet)
				return true
			} else {
				// TODO Finish FLAC support
				return false
			}
		}

		val sPacket = createNext(packet)
		if (sPacket is OggAudioTagsHeader) {
			tags = sPacket as OggAudioTagsHeader?

			// Are there more headers to come?
			return if (type === OggStreamIdentifier.OGG_VORBIS) {
				true
			} else {
				false
			}
		}
		if (sPacket is OggAudioSetupHeader) {
			setup = sPacket as OggAudioSetupHeader?

			// Setup is always last
			return false
		}

		throw IllegalArgumentException("Expecting header packet but got " + sPacket!!)
	}

	/**
	 * Creates the Audio Data for a given audio packet
	 */
	fun createAudio(packet: OggPacket): OggStreamAudioData {
		return createNext(packet) as OggStreamAudioData
	}

	companion object {

		/**
		 * Identifies the type, and returns a partially filled
		 * [OggAudioHeaders] for the new stream
		 */
		fun create(firstPacket: OggPacket): OggAudioStreamHeaders {
			if (firstPacket.isBeginningOfStream &&
				firstPacket.data != null &&
				firstPacket.data.size > 10
			) {
				val sid = firstPacket.sid
				if (VorbisPacketFactory.isVorbisStream(firstPacket)) {
					return OggAudioStreamHeaders(
						sid,
						OggStreamIdentifier.OGG_VORBIS,
						VorbisPacketFactory.create(firstPacket) as VorbisInfo
					)
				}
				if (SpeexPacketFactory.isSpeexStream(firstPacket)) {
					return OggAudioStreamHeaders(
						sid,
						OggStreamIdentifier.SPEEX_AUDIO,
						SpeexPacketFactory.create(firstPacket) as SpeexInfo
					)
				}
				if (OpusPacketFactory.isOpusStream(firstPacket)) {
					return OggAudioStreamHeaders(
						sid,
						OggStreamIdentifier.OPUS_AUDIO,
						OpusPacketFactory.create(firstPacket) as OpusInfo
					)
				}
				if (FlacFirstOggPacket.isFlacStream(firstPacket)) {
					val flac = FlacFirstOggPacket(firstPacket)
					return OggAudioStreamHeaders(
						sid,
						OggStreamIdentifier.OGG_FLAC,
						flac.info!!
					)
				}
				throw IllegalArgumentException(
					"Unsupported stream of type " + OggStreamIdentifier.identifyType(
						firstPacket
					)
				)
			} else {
				throw IllegalArgumentException("May only be called for the first packet in a stream, with data")
			}
		}
	}
}
