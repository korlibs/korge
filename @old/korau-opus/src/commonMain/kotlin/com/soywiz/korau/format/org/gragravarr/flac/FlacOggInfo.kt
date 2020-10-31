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
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*

/**
 * The [FlacInfo] plus the version data from
 * [FlacFirstOggPacket]
 */
open class FlacOggInfo : FlacInfo, OggAudioInfoHeader {
	private var parent: FlacFirstOggPacket? = null

	/**
	 * The version comes from the parent packet
	 */
	override val versionString: String
		get() = "" + parent!!.getMajorVersion() + "." + parent!!.getMinorVersion()
	/**
	 * The overhead comes from the parent packet
	 */
	override val oggOverheadSize: Int
		get() = parent!!.oggOverheadSize

	/**
	 * Creates a new, empty info
	 */
	constructor() : super() {}

	/**
	 * Reads the Info from the specified data
	 */
	constructor(data: ByteArray, offset: Int, parent: FlacFirstOggPacket) : super(data, offset) {
		this.parent = parent
	}

	/**
	 * Supplies the FlacFirstOggPacket for a new info
	 */
	fun setFlacFirstOggPacket(parent: FlacFirstOggPacket) {
		this.parent = parent
	}

	override var data: ByteArray?
		set(value) {
			throw IllegalStateException("Not supported for FLAC")
		}
		get() = super.data

	/**
	 * Data writing passes through to the parent packet
	 */
	override fun write(): OggPacket {
		return parent!!.write()
	}
}
