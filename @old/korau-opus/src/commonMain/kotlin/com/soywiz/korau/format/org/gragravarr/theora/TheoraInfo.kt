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
package com.soywiz.korau.format.org.gragravarr.theora

import com.soywiz.korau.format.org.gragravarr.ogg.*

/**
 * The identification header identifies the bitstream as Theora,
 * and includes the Theora version, the frame details, the picture
 * region details and similar.
 */
class TheoraInfo : HighLevelOggStreamPacket, TheoraPacket {
	var majorVersion: Int = 0
		private set
	var minorVersion: Int = 0
		private set
	var revisionVersion: Int = 0
		private set

	/**
	 * The width of a frame, in Macro Blocks
	 */
	var frameWidthMB: Int = 0
	/**
	 * The height of a frame, in Macro Blocks
	 */
	var frameHeightMB: Int = 0
	/**
	 * The number of super blocks in a frame
	 */
	var frameNumSuperBlocks: Long = 0
	/**
	 * The number of blocks in a frame
	 */
	var frameNumBlocks: Long = 0
	/**
	 * The number of marco blocks in a frame
	 */
	var frameNumMacroBlocks: Long = 0

	/**
	 * The width of the picture region, in pixels
	 */
	var pictureRegionWidth: Long = 0
	/**
	 * The height of the picture region, in pixels
	 */
	var pictureRegionHeight: Long = 0
	/**
	 * The x offset to the start of the picture region, in pixels
	 */
	var pictureRegionXOffset: Int = 0
	/**
	 * The y offset to the start of the picture region, in pixels
	 */
	var pictureRegionYOffset: Int = 0

	/**
	 * The frame rate numerator
	 */
	var frameRateNumerator: Long = 0
	/**
	 * The frame rate denominator
	 */
	var frameRateDenominator: Long = 0

	/**
	 * Pixel aspect ratio numerator
	 */
	var pixelAspectNumerator: Long = 0
	/**
	 * Pixel aspect ratio denomerator
	 */
	var pixelAspectDenomerator: Long = 0

	/**
	 * Colour space, from the indexed list
	 */
	var colourSpace: Int = 0
	/**
	 * ????
	 */
	var pixelFormat: Int = 0

	/**
	 * Nominal bitrate, in bits per second, or zero if the
	 * encoder couldn't guess
	 */
	var nominalBitrate: Long = 0
	/**
	 * Quality hint - higher is better
	 */
	var qualityHint: Int = 0
	/**
	 * Shift for splitting the granule position between
	 * the frame number of the last frame, and the number
	 * of frames since then
	 */
	var keyFrameNumberGranuleShift: Int = 0

	val version: String
		get() = majorVersion.toString() + "." + minorVersion + "." + revisionVersion

	/**
	 * The width of a frame, in Pixels
	 */
	val frameWidth: Int
		get() = frameWidthMB shl 4
	/**
	 * The height of a frame, in Pixels
	 */
	val frameHeight: Int
		get() = frameHeightMB shl 4

	constructor() : super() {
		majorVersion = 3
		minorVersion = 2
		revisionVersion = 1
	}

	constructor(pkt: OggPacket) : super(pkt, null) {

		// Parse
		val data = this.data!!

		majorVersion = data[7].toInt()
		minorVersion = data[8].toInt()
		revisionVersion = data[9].toInt()
		if (majorVersion != 3) {
			throw IllegalArgumentException("Unsupported Theora version $version detected")
		}

		frameWidthMB = IOUtils.getInt2BE(data, 10)
		frameHeightMB = IOUtils.getInt2BE(data, 12)

		pictureRegionWidth = IOUtils.getInt3BE(data, 14)
		pictureRegionHeight = IOUtils.getInt3BE(data, 17)
		pictureRegionXOffset = data[20].toInt()
		pictureRegionYOffset = data[21].toInt()

		frameRateNumerator = IOUtils.getInt4BE(data, 22)
		frameRateDenominator = IOUtils.getInt4BE(data, 26)

		pixelAspectNumerator = IOUtils.getInt3BE(data, 30)
		pixelAspectDenomerator = IOUtils.getInt3BE(data, 33)

		colourSpace = data[36].toInt()
		nominalBitrate = IOUtils.getInt3BE(data, 37)

		// Last two bytes are complicated...
		val lastTwo = IOUtils.getInt2BE(data, 40)
		qualityHint = lastTwo shr 10 // 6 bits
		keyFrameNumberGranuleShift = lastTwo shr 5 and 31 // 5 bits
		pixelFormat = lastTwo shr 3 and 3 // 2 bits
	}

	override fun write(): OggPacket {
		val data = ByteArray(42)
		TheoraPacketFactory.populateMetadataHeader(data, TheoraPacket.TYPE_IDENTIFICATION, data.size)

		data[7] = IOUtils.fromInt(majorVersion)
		data[8] = IOUtils.fromInt(minorVersion)
		data[9] = IOUtils.fromInt(revisionVersion)

		IOUtils.putInt2BE(data, 10, frameWidthMB)
		IOUtils.putInt2BE(data, 12, frameHeightMB)
		IOUtils.putInt3BE(data, 14, pictureRegionWidth)
		IOUtils.putInt3BE(data, 17, pictureRegionHeight)
		data[20] = IOUtils.fromInt(pictureRegionXOffset)
		data[21] = IOUtils.fromInt(pictureRegionYOffset)

		IOUtils.putInt4BE(data, 22, frameRateNumerator)
		IOUtils.putInt4BE(data, 26, frameRateDenominator)

		IOUtils.putInt3BE(data, 30, pixelAspectNumerator)
		IOUtils.putInt3BE(data, 33, pixelAspectDenomerator)

		data[36] = IOUtils.fromInt(colourSpace)
		IOUtils.putInt3BE(data, 37, nominalBitrate)

		// Last two bytes are complicated...
		var lastTwo = (qualityHint shl 6) + keyFrameNumberGranuleShift
		lastTwo = (lastTwo shl 2) + pixelFormat
		lastTwo = lastTwo shl 3 // last 3 bits padding
		IOUtils.putInt2BE(data, 40, lastTwo)

		this.data = (data)
		return super.write()
	}
}
