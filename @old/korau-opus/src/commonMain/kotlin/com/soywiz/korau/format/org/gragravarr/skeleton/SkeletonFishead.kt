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
package com.soywiz.korau.format.org.gragravarr.skeleton

import com.soywiz.klock.*
import com.soywiz.korau.format.org.gragravarr.ogg.*

/**
 * The Fishead (note - one h) provides some basic information
 * on the Skeleton / Annodex stream
 */
class SkeletonFishead : HighLevelOggStreamPacket, SkeletonPacket {
	var versionMajor: Int = 0
	var versionMinor: Int = 0
	var presentationTimeNumerator: Long = 0
	var presentationTimeDenominator: Long = 0
	var baseTimeNumerator: Long = 0
	var baseTimeDenominator: Long = 0
	private var utc: String? = null
	var segmentLength: Long = 0 // v4 only
	var contentOffset: Long = 0 // v4 only

	val version: String
		get() = versionMajor.toString() + "." + versionMinor

	constructor() : super() {
		versionMajor = 4
		versionMinor = 0
	}

	constructor(pkt: OggPacket) : super(pkt, null) {

		// Verify the type
		val data = this.data!!
		if (!IOUtils.byteRangeMatches(SkeletonPacket.MAGIC_FISHEAD_BYTES, data, 0)) {
			throw IllegalArgumentException("Invalid type, not a Skeleton Fishead Header")
		}

		// Parse
		versionMajor = IOUtils.getInt2(data, 8)
		versionMinor = IOUtils.getInt2(data, 10)
		if (versionMajor < 3 || versionMajor > 4) {
			throw IllegalArgumentException("Unsupported Skeleton version $versionMajor detected")
		}

		presentationTimeNumerator = IOUtils.getInt8(data, 12)
		presentationTimeDenominator = IOUtils.getInt8(data, 20)
		baseTimeNumerator = IOUtils.getInt8(data, 28)
		baseTimeDenominator = IOUtils.getInt8(data, 36)

		// UTC is either all-null, or an ISO-8601 date string
		if (data[44].toInt() == 0 && data[45].toInt() == 0) {
			// Treat as empty
			utc = null
		} else {
			utc = IOUtils.getUTF8(data, 44, 20)
		}

		if (versionMajor == 4) {
			segmentLength = IOUtils.getInt8(data, 64)
			contentOffset = IOUtils.getInt8(data, 72)
		}
	}

	override fun write(): OggPacket {
		var len = 64
		if (versionMajor == 4) {
			len = 80
		}
		val data = ByteArray(len)

		IOUtils.putUTF8(data, 0, SkeletonPacket.MAGIC_FISHEAD_STR)
		IOUtils.putInt2(data, 8, versionMajor)
		IOUtils.putInt2(data, 10, versionMinor)

		IOUtils.putInt8(data, 12, presentationTimeNumerator)
		IOUtils.putInt8(data, 20, presentationTimeDenominator)
		IOUtils.putInt8(data, 28, baseTimeNumerator)
		IOUtils.putInt8(data, 36, baseTimeDenominator)

		if (utc != null) {
			IOUtils.putUTF8(data, 44, utc!!)
		} else {
			// Leave as all zeros
		}

		if (versionMajor == 4) {
			IOUtils.putInt8(data, 64, segmentLength)
			IOUtils.putInt8(data, 72, contentOffset)
		}

		this.data = data
		return super.write()
	}

	/**
	 * Returns the ISO-8601 UTC time of the file,
	 * YYYYMMDDTHHMMSS.sssZ, or null if unset
	 */
	fun getUtc(): String? {
		return utc
	}

	/**
	 * Sets the ISO-8601 UTC time of the file, which
	 * must be YYYYMMDDTHHMMSS.sssZ or null
	 */
	fun setUtc(utc: String?) {
		if (utc == null) {
			this.utc = null
		} else {
			if (utc.length != 20) {
				throw IllegalArgumentException("Must be of the form YYYYMMDDTHHMMSS.sssZ")
			}
		}
	}

	fun setUtc(utcDate: DateTime) {
		this.utc = utcDate.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
	}
}
