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
package com.soywiz.korau.format.org.gragravarr.ogg

import com.soywiz.korau.format.org.gragravarr.flac.*
import com.soywiz.korau.format.org.gragravarr.opus.*
import com.soywiz.korau.format.org.gragravarr.skeleton.*
import com.soywiz.korau.format.org.gragravarr.speex.*
import com.soywiz.korau.format.org.gragravarr.theora.*
import com.soywiz.korau.format.org.gragravarr.vorbis.*

/**
 * Detector for identifying the kind of data stored in a given stream.
 * This is normally used on the first packet in a stream, to work out
 * the type, if recognised.
 * Note - the mime types and descriptions should be kept roughly in sync
 * with those in Apache Tika
 */
object OggStreamIdentifier {

	// General types
	val OGG_GENERAL = OggStreamType(
		"application/ogg", "Ogg", OggStreamType.Kind.GENERAL
	)
	val OGG_VIDEO = OggStreamType(
		"video/ogg", "Ogg Video", OggStreamType.Kind.VIDEO
	)
	val OGG_AUDIO = OggStreamType(
		"audio/ogg", "Ogg Audio", OggStreamType.Kind.AUDIO
	)
	val UNKNOWN = OggStreamType(
		"application/octet-stream", "Unknown", OggStreamType.Kind.GENERAL
	)

	// Audio types
	val OGG_VORBIS = OggStreamType(
		"audio/vorbis", "Vorbis", OggStreamType.Kind.AUDIO
	)
	val OPUS_AUDIO = OggStreamType(
		"audio/opus", "Opus", OggStreamType.Kind.AUDIO
	)
	val OPUS_AUDIO_ALT = OggStreamType(
		"audio/ogg; codecs=opus", "Opus", OggStreamType.Kind.AUDIO
	)
	val SPEEX_AUDIO = OggStreamType(
		"audio/speex", "Speex", OggStreamType.Kind.AUDIO
	)
	val SPEEX_AUDIO_ALT = OggStreamType(
		"audio/ogg; codecs=speex", "Speex", OggStreamType.Kind.AUDIO
	)
	val OGG_PCM = OggStreamType(
		"audio/x-oggpcm", "Ogg PCM", OggStreamType.Kind.AUDIO
	)

	val NATIVE_FLAC = OggStreamType(
		"audio/x-flac", "FLAC", OggStreamType.Kind.AUDIO
	)
	val OGG_FLAC = OggStreamType(
		"audio/x-oggflac", "FLAC", OggStreamType.Kind.AUDIO
	)

	// Video types
	val THEORA_VIDEO = OggStreamType(
		"video/theora", "Theora", OggStreamType.Kind.VIDEO
	)
	val THEORA_VIDEO_ALT = OggStreamType(
		"video/x-theora", "Theora", OggStreamType.Kind.VIDEO
	)
	val DAALA_VIDEO = OggStreamType(
		"video/daala", "Daala", OggStreamType.Kind.VIDEO
	)
	val DIRAC_VIDEO = OggStreamType(
		"video/x-dirac", "Dirac", OggStreamType.Kind.VIDEO
	)
	val OGM_VIDEO = OggStreamType(
		"video/x-ogm", "Ogg OGM", OggStreamType.Kind.VIDEO
	)

	val OGG_UVS = OggStreamType(
		"video/x-ogguvs", "Ogg UVS", OggStreamType.Kind.VIDEO
	)
	val OGG_YUV = OggStreamType(
		"video/x-oggyuv", "Ogg YUV", OggStreamType.Kind.VIDEO
	)
	val OGG_RGB = OggStreamType(
		"video/x-oggrgb", "Ogg RGB", OggStreamType.Kind.VIDEO
	)

	// Metadata types
	val SKELETON = OggStreamType(
		"application/annodex", "Skeleton Annodex", OggStreamType.Kind.METADATA
	)
	val CMML = OggStreamType(
		"text/x-cmml", "CMML", OggStreamType.Kind.METADATA
	)
	val KATE = OggStreamType(
		"application/kate", "Kate", OggStreamType.Kind.METADATA
	)

	// These methods provide first packet type detection for the
	//  various Ogg-based formats we lack general support for
	internal val MAGIC_OGG_PCM = IOUtils.toUTF8Bytes("PCM     ")

	internal val MAGIC_DAALA = ByteArray(8)
	internal val MAGIC_DIRAC = IOUtils.toUTF8Bytes("BBCD")
	internal val MAGIC_OGG_OGM = IOUtils.toUTF8Bytes("video")
	internal val MAGIC_OGG_UVS = IOUtils.toUTF8Bytes("UVS ")
	internal val MAGIC_OGG_YUV = IOUtils.toUTF8Bytes("\u0001YUV")
	internal val MAGIC_OGG_RGB = IOUtils.toUTF8Bytes("\u0001GBP")

	internal val MAGIC_CMML = IOUtils.toUTF8Bytes("CMML\u0000\u0000\u0000\u0000")
	internal val MAGIC_KATE = ByteArray(8)
	internal val MAGIC_ANNODEX2 = IOUtils.toUTF8Bytes("Annodex\u0000")

	class OggStreamType(val mimetype: String, val description: String, val kind: Kind) {
		enum class Kind {
			GENERAL, AUDIO, VIDEO, METADATA
		}

		override fun toString(): String {
			return kind.toString() + " - " + description + " as " + mimetype
		}
	}

	fun identifyType(p: OggPacket): OggStreamType {
		if (!p.isBeginningOfStream) {
			// All streams so far can be identified from their first packet
			// Very few can be identified past about their 2nd or 3rd
			// So, we only support identifying off the first one
			throw IllegalArgumentException("Can only Identify from the first packet in a stream")
		} else {
			if (p.data != null && p.data.size > 10) {
				// Is it a Metadata related stream?
				if (SkeletonPacketFactory.isSkeletonStream(p)) {
					return SKELETON
				}
				if (isAnnodex2Stream(p)) {
					return SKELETON
				}
				if (isCMMLStream(p)) {
					return CMML
				}
				if (isKateStream(p)) {
					return KATE
				}

				// Is it an Audio stream?
				if (VorbisPacketFactory.isVorbisStream(p)) {
					// Vorbis Audio stream
					return OGG_VORBIS
				}
				if (SpeexPacketFactory.isSpeexStream(p)) {
					// Speex Audio stream
					return SPEEX_AUDIO
				}
				if (OpusPacketFactory.isOpusStream(p)) {
					// Opus Audio stream
					return OPUS_AUDIO
				}
				if (FlacFirstOggPacket.isFlacStream(p)) {
					// FLAC-in-Ogg Audio stream
					return OGG_FLAC
				}
				if (isOggPCMStream(p)) {
					// PCM-in-Ogg Audio stream
					return OGG_PCM
				}

				// Is it a video stream?
				if (TheoraPacketFactory.isTheoraStream(p)) {
					return THEORA_VIDEO
				}
				if (isDaalaStream(p)) {
					return DAALA_VIDEO
				}
				if (isDiracStream(p)) {
					return DIRAC_VIDEO
				}
				if (isOggOGMStream(p)) {
					return OGM_VIDEO
				}
				if (isOggUVSStream(p)) {
					return OGG_UVS
				}
				if (isOggYUVStream(p)) {
					return OGG_YUV
				}
				if (isOggRGBStream(p)) {
					return OGG_RGB
				}
			}
			// Couldn't determine what it is
			return UNKNOWN
		}
	}

	internal fun isOggPCMStream(p: OggPacket): Boolean {
		return IOUtils.byteRangeMatches(MAGIC_OGG_PCM, p.data, 0)
	}

	init {
		MAGIC_DAALA[0] = 0x80.toByte()
		IOUtils.putUTF8(MAGIC_DAALA, 1, "daala")
		// Remaining 2 bytes are all zero
	}

	internal fun isDaalaStream(p: OggPacket): Boolean {
		return IOUtils.byteRangeMatches(MAGIC_DAALA, p.data, 0)
	}

	internal fun isDiracStream(p: OggPacket): Boolean {
		return IOUtils.byteRangeMatches(MAGIC_DIRAC, p.data, 0)
	}

	internal fun isOggOGMStream(p: OggPacket): Boolean {
		return IOUtils.byteRangeMatches(MAGIC_OGG_OGM, p.data, 0)
	}

	internal fun isOggUVSStream(p: OggPacket): Boolean {
		return IOUtils.byteRangeMatches(MAGIC_OGG_UVS, p.data, 0)
	}

	internal fun isOggYUVStream(p: OggPacket): Boolean {
		return IOUtils.byteRangeMatches(MAGIC_OGG_YUV, p.data, 0)
	}

	internal fun isOggRGBStream(p: OggPacket): Boolean {
		return IOUtils.byteRangeMatches(MAGIC_OGG_RGB, p.data, 0)
	}

	internal fun isCMMLStream(p: OggPacket): Boolean {
		return IOUtils.byteRangeMatches(MAGIC_CMML, p.data, 0)
	}

	init {
		MAGIC_KATE[0] = 0x80.toByte()
		IOUtils.putUTF8(MAGIC_KATE, 1, "kate")
		// Remaining 3 bytes are all zero
	}

	internal fun isKateStream(p: OggPacket): Boolean {
		return IOUtils.byteRangeMatches(MAGIC_KATE, p.data, 0)
	}

	internal fun isAnnodex2Stream(p: OggPacket): Boolean {
		return IOUtils.byteRangeMatches(MAGIC_ANNODEX2, p.data, 0)
	}
}
