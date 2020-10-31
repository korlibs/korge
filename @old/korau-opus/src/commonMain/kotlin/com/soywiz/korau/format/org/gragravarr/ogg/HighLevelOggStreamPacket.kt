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

/**
 * A high level stream packet sat atop
 * of an OggPacket.
 * Provides support for reading and writing
 * new and existing OggPacket instances.
 */
abstract class HighLevelOggStreamPacket() : OggStreamPacket {

	protected var oggPacket: OggPacket? = null
		private set

	override var data: ByteArray? = null
		get() {
			if (field != null) {
				return field
			}
			return if (oggPacket != null) {
				oggPacket!!.data
			} else null
		}

	constructor(pkt: OggPacket?, data: ByteArray?) : this() {
		this.oggPacket = pkt
		this.data = data
	}

	/**
	 * Returns the approximate number of bytes overhead
	 * from the underlying [OggPacket] / [OggPage]
	 * structures into which this data is stored.
	 *
	 * Will return 0 for packets not yet associated with a page.
	 *
	 * This information is normally only of interest to information,
	 * diagnostic and debugging tools.
	 */
	override val oggOverheadSize: Int
		get() = if (oggPacket != null) {
			oggPacket!!.overheadBytes
		} else {
			0
		}

	override fun write(): OggPacket {
		this.oggPacket = OggPacket(data!!)
		return this.oggPacket!!
	}
}
