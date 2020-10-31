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
import com.soywiz.korau.format.org.concentus.internal.*
import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korio.stream.*


/**
 * This comes before the audio data.
 * Made up of a series of:
 * 1 byte type
 * 3 byte length
 * <data>
</data> */
abstract class FlacMetadataBlock protected constructor(
	// 7-126 : reserved
	// 127 : invalid, to avoid confusion with a frame sync code

	private val type: Byte
) : FlacFrame() {
	val isLastMetadataBlock: Boolean
		get() = type < 0

	override// Type goes first
	// Pad length, will do later
	// Do the main data
	// Shouldn't ever happen!
	// Fix the length
	// All done
	val data: ByteArray?
		get() {
			val data = MemorySyncStreamToByteArray {
				val baos = this
				baos.write8(this@FlacMetadataBlock.type.toInt())
				baos.write(ByteArray(3))
				write(baos)
			}
			IOUtils.putInt3BE(data, 1, data.size.toLong())
			return data
		}

	fun getType(): Int = type and 0x7f

	protected abstract fun write(out: SyncOutputStream)

	companion object {
		val STREAMINFO: Byte = 0
		val PADDING: Byte = 1
		val APPLICATION: Byte = 2
		val SEEKTABLE: Byte = 3
		val VORBIS_COMMENT: Byte = 4
		val CUESHEET: Byte = 5
		val PICTURE: Byte = 6

		fun create(inp: SyncInputStream): FlacMetadataBlock {
			val typeI = inp.read()
			if (typeI == -1) {
				throw IllegalArgumentException()
			}
			val type = IOUtils.fromInt(typeI)

			val l = ByteArray(3)
			IOUtils.readFully(inp, l)
			val length = IOUtils.getInt3BE(l) as Int

			val data = ByteArray(length)
			IOUtils.readFully(inp, data)

			when (type) {
				STREAMINFO -> return FlacInfo(data, 0)
				VORBIS_COMMENT -> return FlacTags.FlacTagsAsMetadata(data!!)!!
				else -> return FlacUnhandledMetadataBlock(type, data)
			}
		}
	}
}
