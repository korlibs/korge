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
 * The first Flac packet stored in an Ogg stream is
 * special. This holds both the stream information,
 * and the [FlacFrame]
 */
class FlacFirstOggPacket : HighLevelOggStreamPacket {
	private var majorVersion: Int = 0
	private var minorVersion: Int = 0
	/**
	 * Gets the number of header blocks, excluding this one, or
	 * zero if not known
	 */
	var numberOfHeaderBlocks: Int = 0
	var info: FlacOggInfo? = null
		private set

	constructor(info: FlacOggInfo = FlacOggInfo()) : super() {
		majorVersion = 1
		minorVersion = 0
		numberOfHeaderBlocks = 0
		this.info = info
		this.info!!.setFlacFirstOggPacket(this)
	}

	constructor(oggPacket: OggPacket) : super(oggPacket, null) {

		// Extract the info
		val data = this.data
		// 0 = 0x7f
		// 1-4 = FLAC
		majorVersion = IOUtils.toInt(data!![5])
		minorVersion = IOUtils.toInt(data[6])
		numberOfHeaderBlocks = IOUtils.getInt2BE(data, 7)
		// 9-12 = fLaC
		// 13-16 = 0 + length

		// Then it's the info
		info = FlacOggInfo(data, 17, this)
	}

	override fun write(): OggPacket {
		val baos = byteArrayOf().openSync()
		try {
			baos.write("FLAC".toByteArray(ASCII))
			baos.write32BE(majorVersion)
			baos.write32BE(minorVersion)
			IOUtils.writeInt2BE(baos, numberOfHeaderBlocks)
			baos.write("fLaC".toByteArray(ASCII))
			baos.write(info!!.data!!)
		} catch (e: Throwable) {
			// Should never happen!
			throw RuntimeException(e)
		}

		this.data = baos.toByteArray()
		return super.write()
	}

	/**
	 * Returns the Major Version number
	 */
	fun getMajorVersion(): Int {
		return majorVersion
	}

	fun setMajorVersion(majorVersion: Int) {
		if (majorVersion > 255) {
			throw IllegalArgumentException("Version numbers must be in the range 0-255")
		}
		this.majorVersion = majorVersion
	}

	/**
	 * Returns the Minor Version number. Decoders should be able to
	 * handle anything at a given major number, no matter the minor one
	 */
	fun getMinorVersion(): Int {
		return minorVersion
	}

	fun setMinorVersion(minorVersion: Int) {
		if (minorVersion > 255) {
			throw IllegalArgumentException("Version numbers must be in the range 0-255")
		}
		this.minorVersion = minorVersion
	}

	companion object {

		/**
		 * Does this packet (the first in the stream) contain
		 * the magic string indicating that it's a FLAC
		 * one?
		 */
		fun isFlacStream(firstPacket: OggPacket): Boolean {
			return if (!firstPacket.isBeginningOfStream) {
				false
			} else isFlacSpecial(firstPacket)
		}

		protected fun isFlacSpecial(packet: OggPacket): Boolean {
			val d = packet.data
			val type = d[0]

			// Ensure 0x7f then "FLAC"
			if (type.toInt() == 0x7f) {
				if (d[1] == 'F'.toByte() &&
					d[2] == 'L'.toByte() &&
					d[3] == 'A'.toByte() &&
					d[4] == 'C'.toByte()
				) {

					return true
				}
			}
			return false
		}
	}
}
