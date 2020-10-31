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
import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korio.stream.*

/**
 * The Stream Info metadata block holds useful
 * information on the audio data of the file
 */
open class FlacInfo : FlacMetadataBlock {
	/**
	 * <16> The minimum block size (in samples) used in the stream.
	 */
	/**
	 * The minimum block size (in samples) used in the stream.
	 */
	var minimumBlockSize: Int = 0
	/**
	 * <16> The maximum block size (in samples) used in the stream. (Minimum blocksize == maximum blocksize) implies a fixed-blocksize stream.
	 */
	/**
	 * The maximum block size (in samples) used in the stream.
	 * (Minimum blocksize == maximum blocksize) implies a fixed-blocksize stream.
	 */
	var maximumBlockSize: Int = 0
	/**
	 * <24> The minimum frame size (in bytes) used in the stream. May be 0 to imply the value is not known.
	 */
	var minimumFrameSize: Int = 0
	/**
	 * <24> The maximum frame size (in bytes) used in the stream. May be 0 to imply the value is not known.
	 */
	var maximumFrameSize: Int = 0
	/**
	 * <20> Sample rate in Hz. Though 20 bits are available, the maximum sample
	 * rate is limited by the structure of frame headers to 655350Hz.
	 * Also, a value of 0 is invalid.
	 */
	var sampleRate: Int = 0
	/**
	 * <3> (number of channels)-1. FLAC supports from 1 to 8 channels
	 */
	var numChannels: Int = 0
	/**
	 * <5> (bits per sample)-1. FLAC supports from 4 to 32 bits per sample.
	 * Currently the reference encoder and decoders only support up to
	 * 24 bits per sample.
	 */
	var bitsPerSample: Int = 0
	/**
	 * <36> Total samples in stream. 'Samples' means inter-channel sample,
	 * i.e. one second of 44.1Khz audio will have 44100 samples regardless
	 * of the number of channels.
	 * A value of zero here means the number of total samples is unknown.
	 */
	var numberOfSamples: Long = 0

	/**
	 * <128> MD5 signature of the unencoded audio data.
	 */
	var signature: ByteArray? = null

	val preSkip: Int
		get() = 0

	/**
	 * Creates a new, empty info
	 */
	constructor() : super(FlacMetadataBlock.STREAMINFO) {
		signature = ByteArray(16)
	}

	/**
	 * Reads the Info from the specified data
	 */
	constructor(data: ByteArray, offset: Int) : super(FlacMetadataBlock.STREAMINFO) {
		var offset = offset

		// Grab the range numbers
		minimumBlockSize = IOUtils.getIntBE(
			IOUtils.toInt(data[offset++]),
			IOUtils.toInt(data[offset++])
		)
		maximumBlockSize = IOUtils.getIntBE(
			IOUtils.toInt(data[offset++]),
			IOUtils.toInt(data[offset++])
		)
		minimumFrameSize = IOUtils.getIntBE(
			IOUtils.toInt(data[offset++]),
			IOUtils.toInt(data[offset++]),
			IOUtils.toInt(data[offset++])
		).toInt()
		maximumFrameSize = IOUtils.getIntBE(
			IOUtils.toInt(data[offset++]),
			IOUtils.toInt(data[offset++]),
			IOUtils.toInt(data[offset++])
		).toInt()

		// The next bit is stored LE, bit packed
		// TODO Switch to using BitsReader to read this nicer
		val next = IntArray(8)
		for (i in 0..7) {
			next[i] = IOUtils.toInt(data[i + offset])
		}
		offset += 8
		sampleRate = (next[0] shl 12) + (next[1] shl 4) + (next[2] and 0xf0 shr 4)
		numChannels = (next[2] and 0x0e shr 1) + 1
		bitsPerSample = (next[2] and 0x01 shl 4) + (next[3] and 0xf0 shr 4) + 1
		numberOfSamples = ((next[3] and 0x0f shl 30) + (next[4] shl 24) +
				(next[5] shl 16) + (next[6] shl 8) + next[7]).toLong()

		// Get the signature
		signature = ByteArray(16)
		arraycopy(data, offset, signature!!, 0, 16)
	}

	protected override fun write(out: SyncOutputStream) {
		// Write the frame numbers
		IOUtils.writeInt2BE(out, minimumBlockSize)
		IOUtils.writeInt2BE(out, maximumBlockSize)
		IOUtils.writeInt3BE(out, minimumFrameSize.toLong())
		IOUtils.writeInt3BE(out, maximumFrameSize.toLong())

		// Write the rates/channels/samples
		// TODO
		out.write(ByteArray(8))

		// Write the signature
		out.write(signature!!)
	}
}
