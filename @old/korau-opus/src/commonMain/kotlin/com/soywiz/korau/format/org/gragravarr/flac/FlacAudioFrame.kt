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
import kotlin.math.*

/**
 * Raw, compressed audio data.
 */
class FlacAudioFrame
/**
 * Creates the frame from the pre-read 2 bytes and stream, no sync checks.
 * Info is needed, as values of 0 often mean "as per info defaults".
 */
constructor(first2: Int, rawStream: SyncInputStream, info: FlacInfo) : FlacFrame() {
	/**
	 * Fixed or Variable block size?
	 * Fixed = frame header encodes the frame number
	 * Variable = frame header encodes the sample number
	 */
	/**
	 * Is the block size fixed (frame header encodes the frame number)
	 * or variable (frame header encodes the sample number)
	 */
	val isBlockSizeVariable: Boolean
	/**
	 * If [.isBlockSizeVariable], then this is the
	 * sample number, otherwise the frame number
	 */
	val codedNumber: Long

	private val blockSizeRaw: Int
	/**
	 * Block size in inter-channel samples
	 */
	var blockSize: Int = 0
		private set
	private val sampleRateRaw: Int = 0
	/**
	 * Sample rate in Hz
	 */
	var sampleRate: Int = 0
		private set

	/**
	 * Number of channels
	 */
	var numChannels: Int = 0
		private set
	val channelType: Int
	private val sampleSizeRaw: Int
	/**
	 * Sample size in bits
	 */
	var bitsPerSample: Int = 0
		private set

	/**
	 * SubFrames hold the encoded audio data on a per-channel basis
	 */
	val subFrames: Array<FlacAudioSubFrame?>

	private val frameData: ByteArray

	/**
	 * Returns the contents, including the sync header
	 */
	override val data: ByteArray
		get() {
			val data = ByteArray(frameData.size + 2)

			var first2 = FRAME_SYNC shl 2
			if (isBlockSizeVariable) first2++

			IOUtils.putInt2BE(data, 0, first2)
			arraycopy(frameData, 0, data, 2, frameData.size)

			return data
		}

	constructor(data: ByteArray, info: FlacInfo) : this((data).openSync(), info)

	/**
	 * Creates the frame from the stream, with header sync checking
	 */
	constructor(stream: SyncInputStream, info: FlacInfo) : this(getAndCheckFirstTwo(stream), stream, info)

	/**
	 * Creates the frame from the pre-read 2 bytes and stream, with header sync checking
	 */
	constructor(byte1: Int, byte2: Int, stream: SyncInputStream, info: FlacInfo) : this(
		getAndCheckFirstTwo(byte1, byte2),
		stream,
		info
	)

	init {
		val ab = ByteArrayBuilder()
		val mem = MemorySyncStream(ab)

		// Wrap the InputStream so that it captures the contents
		val stream = BytesCapturingInputStream(rawStream, mem)
		//val stream = rawStream

		// First 14 bits are the sync, 15 is reserved, 16 is block size
		isBlockSizeVariable = first2 and 1 == 1

		// Mostly, this works in bits not nicely padded bytes
		val br = BitsReader(stream)

		// Block Size + Sample Rate
		blockSizeRaw = br.read(4)
		sampleRate = br.read(4)

		// Decode those, as best we can
		var readBlockSize8 = false
		var readBlockSize16 = false
		when (blockSizeRaw) {
			0 -> // Reserved
				blockSize = 0
			1 -> blockSize = 192
			in 2..4 -> blockSize = 576 * 2.0.pow((blockSizeRaw - 2).toDouble()).toInt()
			6 -> readBlockSize8 = true
			7 -> readBlockSize16 = true
			else -> blockSize = 256 * 2.0.pow((blockSizeRaw - 8).toDouble()).toInt()
		}

		if (sampleRateRaw == 0) {
			sampleRate = info.sampleRate
		} else if (sampleRateRaw < RATES.size) {
			sampleRate = RATES[sampleRateRaw].Hz
		}

		// Channel Assignment + Sample Size + Res
		channelType = br.read(4)
		if (channelType < 8) {
			numChannels = channelType + 1
		} else {
			numChannels = 2
		}

		sampleSizeRaw = br.read(3)
		br.read(1)
		if (sampleSizeRaw == 0) {
			bitsPerSample = info.bitsPerSample
		} else if (sampleSizeRaw == 1) {
			bitsPerSample = 8
		} else if (sampleSizeRaw == 2) {
			bitsPerSample = 12
		} else if (sampleSizeRaw == 3) {
			// Reserved
			bitsPerSample = 0
		} else if (sampleSizeRaw == 4) {
			bitsPerSample = 16
		} else if (sampleSizeRaw == 5) {
			bitsPerSample = 20
		} else if (sampleSizeRaw == 6) {
			bitsPerSample = 24
		} else if (sampleSizeRaw == 7) {
			// Reserved
			bitsPerSample = 0
		}

		// Coded Number - either sample or frame, based on blockSizeVariable
		codedNumber = IOUtils.readUE7(stream)

		// Ext block size
		if (readBlockSize8) {
			blockSize = stream.read() + 1
		}
		if (readBlockSize16) {
			blockSize = IOUtils.getIntBE(stream.read(), stream.read()) + 1
		}

		// Ext sample rate
		if (sampleRateRaw == 12) {
			// 8 bit Hz
			sampleRate = stream.read()
		}
		if (sampleRateRaw == 13) {
			// 16 bit Hz
			sampleRate = IOUtils.getIntBE(stream.read(), stream.read())
		}
		if (sampleRateRaw == 14) {
			// 16 bit tens-of-Hz
			sampleRate = 10 * IOUtils.getIntBE(stream.read(), stream.read())
		}

		// Header CRC, not checked
		stream.read()

		// One sub-frame per channel
		subFrames = arrayOfNulls<FlacAudioSubFrame>(numChannels)
		for (cn in 0 until numChannels) {
			// Zero
			br.read(1)
			// Type
			val type = br.read(6)
			// Wasted Bits per Sample
			var wb = br.read(1)
			if (wb == 1) {
				wb = br.bitsToNextOne() + 1
			}
			// Check there's data
			if (br.isEOF)
				throw IllegalArgumentException(
					"No data left to read subframe for channel "
							+ (cn + 1) + " of " + numChannels
				)

			// Sub-Frame data
			subFrames[cn] = FlacAudioSubFrame.create(type, cn, wb, this, br)
		}

		// Skip any remaining bits, to hit the boundary
		br.readToByteBoundary()

		// Footer CRC, not checked
		stream.read()
		stream.read()

		// Capture the raw bytes read
		frameData = ab.toByteArray()
	}

	protected class SampleRate(protected val kHz: Double) {
		val Hz: Int

		init {
			this.Hz = rint((kHz * 1000).toFloat()).toInt()
		}
	}

	companion object {
		private fun getAndCheckFirstTwo(stream: SyncInputStream): Int {
			val byte1 = stream.read()
			val byte2 = stream.read()
			return getAndCheckFirstTwo(byte1, byte2)
		}

		private fun getAndCheckFirstTwo(byte1: Int, byte2: Int): Int {
			val first2 = IOUtils.getIntBE(byte1, byte2)
			if (!isFrameHeaderStart(first2)) {
				throw IllegalArgumentException("Frame Header start sync not found")
			}
			return first2
		}

		fun isFrameHeaderStart(byte1: Int, byte2: Int): Boolean {
			return isFrameHeaderStart(IOUtils.getIntBE(byte1, byte2))
		}

		fun isFrameHeaderStart(first2: Int): Boolean {
			// First 14 bytes must be 11111111111110
			return first2 shr 2 == FRAME_SYNC
		}

		private val FRAME_SYNC = 0x3ffe
		private val RATES = arrayOf(
			SampleRate(0.0),
			SampleRate(88.2),
			SampleRate(176.4),
			SampleRate(192.0),
			SampleRate(8.0),
			SampleRate(16.0),
			SampleRate(22.05),
			SampleRate(24.0),
			SampleRate(32.0),
			SampleRate(44.1),
			SampleRate(48.0),
			SampleRate(96.0)
		)
		val CHANNEL_TYPE_LEFT = 0x9
		val CHANNEL_TYPE_RIGHT = 0xa
		val CHANNEL_TYPE_MID = 0xb
	}
}
