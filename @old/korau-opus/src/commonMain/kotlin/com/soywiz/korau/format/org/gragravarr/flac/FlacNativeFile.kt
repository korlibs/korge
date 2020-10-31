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

import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korio.stream.*

/**
 * This lets you work with FLAC files that
 * are contained in a native FLAC Stream
 */
class FlacNativeFile
/**
 * Opens the given FLAC file
 */
constructor(private var input: SyncInputStream?) : FlacFile() {


	override val nextAudioPacket: FlacAudioFrame?
		get() {
			var skipped = 0
			var b1 = 0
			var b2 = input!!.read()
			while (b1 != -1 && b2 != -1) {
				b1 = b2
				b2 = input!!.read()
				if (FlacAudioFrame.isFrameHeaderStart(b1, b2)) {
					if (skipped > 0)
						//println("Warning - had to skip $skipped bytes of junk data before finding the next packet header")
					return FlacAudioFrame(b1, b2, input!!, info2!!)
				}
				skipped++
			}
			return null
		}

	init {
		// Check the header
		val header = ByteArray(4)
		IOUtils.readFully(input!!, header)
		if (header[0] == 'f'.toByte() && header[1] == 'L'.toByte() &&
			header[2] == 'a'.toByte() && header[3] == 'C'.toByte()
		) {
			// Good
		} else {
			throw IllegalArgumentException("Not a FLAC file")
		}

		// First must be the FLAC info
		info2 = FlacMetadataBlock.create(input!!) as FlacInfo

		// Read the rest of the Metadata blocks
		otherMetadata = ArrayList<FlacMetadataBlock>()
		while (true) {
			val m = FlacMetadataBlock.create(input!!)
			if (m is FlacTags.FlacTagsAsMetadata) {
				tags = (m as FlacTags.FlacTagsAsMetadata).tags
			} else {
				otherMetadata!!.add(m)
			}

			if (m.isLastMetadataBlock) {
				break
			}
		}
	}// Rest is audio

	/**
	 * Skips the audio data to the next packet with a granule
	 * of at least the given granule position.
	 * Note that skipping backwards is not currently supported!
	 */
	override fun skipToGranule(granulePosition: Long) {
		throw RuntimeException("Not supported")
	}

	/**
	 * In Reading mode, will close the underlying ogg/flac
	 * file and free its resources.
	 * In Writing mode, will write out the Info and
	 * Comments objects, and then the audio data.
	 */
	override fun close() {
		if (input != null) {
			input!!.close()
			input = null
		} else {
			throw RuntimeException("Not supported")
		}
	}
}
