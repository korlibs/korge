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
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

/**
 * This wrapper lets you work with FLAC files,
 * whether they're Ogg or Native framed.
 */
abstract class FlacFile : Closeable {
	open var info2: FlacInfo? = null
		protected set
	var tags: FlacTags? = null
		protected set
	protected var otherMetadata: ArrayList<FlacMetadataBlock>? = null

	abstract val nextAudioPacket: FlacAudioFrame?

	/**
	 * Skips the audio data to the next packet with a granule
	 * of at least the given granule position.
	 * Note that skipping backwards is not currently supported!
	 */
	abstract fun skipToGranule(granulePosition: Long)

	/**
	 * In Reading mode, will close the underlying ogg/flac
	 * file and free its resources.
	 * In Writing mode, will write out the Info and
	 * Comments objects, and then the audio data.
	 */
	abstract override fun close()

	companion object {
		/**
		 * Opens the given file for reading.
		 * @param inp The InputStrem to read from, which must support mark/reset
		 */
		fun open(inp: SyncInputStream, warningProcessor: ((String) -> Unit)?): FlacFile {
			val header = (inp as SyncStream).sliceHere().readBytesExact(4)

			if (header[0] == 'O'.toByte() && header[1] == 'g'.toByte() &&
				header[2] == 'g'.toByte() && header[3] == 'S'.toByte()
			) {
				return FlacOggFile(OggFile(inp as SyncInputStream, warningProcessor))
			}
			if (header[0] == 'f'.toByte() && header[1] == 'L'.toByte() &&
				header[2] == 'a'.toByte() && header[3] == 'C'.toByte()
			) {
				return FlacNativeFile(inp)
			}
			throw IllegalArgumentException("File type not recognised")
		}

		/**
		 * Opens the given file for reading
		 */
		fun open(ogg: OggFile): FlacFile {
			return FlacOggFile(ogg)
		}
	}
}
