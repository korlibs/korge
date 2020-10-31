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

/**
 * Per-channel, compressed audio
 */
abstract class FlacAudioSubFrame protected constructor(
	val predictorOrder: Int,
	channelNumber: Int,
	protected val audioFrame: FlacAudioFrame
) {
	val sampleSizeBits: Int
	val blockSize: Int
	/**
	 * The number of wasted bits per sample
	 */
	var wastedBits: Int = 0
		private set
	abstract val type: String

	init {
		this.blockSize = audioFrame.blockSize

		// Adjust sample size for channel number, if needed
		// TODO Is this the right adjustment amount?
		var sampleSizeBits = audioFrame.bitsPerSample
		if (audioFrame.channelType == FlacAudioFrame.CHANNEL_TYPE_LEFT && channelNumber == 1) {
			sampleSizeBits++
		}
		if (audioFrame.channelType == FlacAudioFrame.CHANNEL_TYPE_RIGHT && channelNumber == 0) {
			sampleSizeBits++
		}
		if (audioFrame.channelType == FlacAudioFrame.CHANNEL_TYPE_MID && channelNumber == 1) {
			sampleSizeBits++
		}
		this.sampleSizeBits = sampleSizeBits
	}

	class SubFrameConstant
	constructor(
		channelNumber: Int, audioFrame: FlacAudioFrame,
		data: BitsReader
	) : FlacAudioSubFrame(-1, channelNumber, audioFrame) {
		override val type: String
			get() = "CONSTANT"

		init {
			data.read(sampleSizeBits)
		}

		companion object {
			fun matchesType(type: Int): Boolean {
				return if (type == 0) true else false
			}
		}
	}

	class SubFrameVerbatim
	constructor(
		channelNumber: Int, audioFrame: FlacAudioFrame,
		data: BitsReader
	) : FlacAudioSubFrame(-1, channelNumber, audioFrame) {
		override val type: String
			get() = "VERBATIM"

		init {
			for (i in 0 until blockSize) {
				data.read(sampleSizeBits)
			}
		}

		companion object {
			fun matchesType(type: Int): Boolean {
				return if (type == 1) true else false
			}
		}
	}

	open class SubFrameWithResidual protected constructor(
		predictorOrder: Int,
		channelNumber: Int,
		audioFrame: FlacAudioFrame
	) : FlacAudioSubFrame(predictorOrder, channelNumber, audioFrame) {
		var warmUpSamples: IntArray? = null
			protected set
		var residual: SubFrameResidual? = null
			protected set
		override val type: String
			get() = "UNKNOWN"
	}

	class SubFrameFixed
	constructor(
		type: Int, channelNumber: Int, audioFrame: FlacAudioFrame,
		data: BitsReader
	) : SubFrameWithResidual(type and 7, channelNumber, audioFrame) {
		override val type: String
			get() = "FIXED"

		init {
			warmUpSamples = IntArray(predictorOrder) { data.read(sampleSizeBits) }
			residual = createResidual(data)
		}

		companion object {
			fun matchesType(type: Int): Boolean {
				return if (type >= 8 && type <= 15) true else false
			}
		}
	}

	class SubFrameLPC
	constructor(
		type: Int, channelNumber: Int, audioFrame: FlacAudioFrame,
		data: BitsReader
	) : SubFrameWithResidual((type and 31) + 1, channelNumber, audioFrame) {
		val linearPredictorCoefficientPrecision: Int
		val linearPredictorCoefficientShift: Int

		val coefficients: IntArray
		override val type: String
			get() = "LPC"

		init {

			warmUpSamples = IntArray(predictorOrder) { data.read(sampleSizeBits) }

			this.linearPredictorCoefficientPrecision = data.read(4) + 1
			this.linearPredictorCoefficientShift = data.read(5)

			coefficients = IntArray(predictorOrder)
			for (i in 0 until predictorOrder) {
				coefficients[i] = data.read(linearPredictorCoefficientPrecision)
			}

			residual = createResidual(data)
		}

		companion object {
			fun matchesType(type: Int): Boolean {
				return if (type >= 32) true else false
			}
		}
	}

	class SubFrameReserved(audioFrame: FlacAudioFrame) : FlacAudioSubFrame(-1, -1, audioFrame) {
		override val type: String
			get() = "RESERVED"

		companion object {
			fun matchesType(type: Int): Boolean {
				if (type >= 2 && type <= 7) return true
				return if (type >= 16 && type <= 31) true else false
			}
		}
	}

	protected fun createResidual(data: BitsReader): SubFrameResidual? {
		val type = data.read(2)
		if (type > 1) {
			// Un-supported / reserved type
			return null
		}

		val partitionOrder = data.read(4)
		return if (type == 0) {
			SubFrameResidualRice(partitionOrder, data)
		} else {
			SubFrameResidualRice2(partitionOrder, data)
		}
	}

	open inner class SubFrameResidual
	constructor(val partitionOrder: Int, bits: Int, escapeCode: Int, data: BitsReader) {
		val numPartitions: Int
		val riceParams: IntArray
		open val type: String
			get() = "UNKNOWN"

		init {
			numPartitions = 1 shl partitionOrder
			riceParams = IntArray(numPartitions)

			var numSamples = 0
			if (partitionOrder > 0) {
				numSamples = blockSize shr partitionOrder
			} else {
				numSamples = blockSize - predictorOrder
			}

			for (pn in 0 until numPartitions) {
				var riceParam = data.read(bits)

				var partitionSamples = 0
				if (partitionOrder == 0 || pn > 0) {
					partitionSamples = numSamples
				} else {
					partitionSamples = numSamples - predictorOrder
				}

				if (riceParam == escapeCode) {
					// Partition holds un-encoded binary form
					riceParam = data.read(5)
					for (i in 0 until partitionSamples) {
						data.read(riceParam)
					}
				} else {
					// Partition holds Rice encoded data
					for (sn in 0 until numSamples) {
						// Q value stored as zero-based unary
						data.bitsToNextOne()
						// R value stored as truncated binary
						data.read(riceParam)
					}
				}

				// Record the Rice Parameter for use in unit tests etc
				riceParams[pn] = riceParam
			}
		}
	}

	inner class SubFrameResidualRice
	constructor(partitionOrder: Int, data: BitsReader) :
		SubFrameResidual(partitionOrder, 4, 15, data) {
		override val type: String
			get() = "RICE"
	}

	inner class SubFrameResidualRice2
	constructor(partitionOrder: Int, data: BitsReader) :
		SubFrameResidual(partitionOrder, 5, 31, data) {
		override val type: String
			get() = "RICE2"
	}

	companion object {
		fun create(
			type: Int, channelNumber: Int, wastedBits: Int,
			audioFrame: FlacAudioFrame, data: BitsReader
		): FlacAudioSubFrame {
			// Sanity check
			if (type < 0 || type >= 64) {
				throw IllegalArgumentException("Type must be a un-signed 6 bit number, found $type")
			}

			// Create the right type
			val subFrame: FlacAudioSubFrame
			if (SubFrameConstant.matchesType(type))
				subFrame = SubFrameConstant(channelNumber, audioFrame, data)
			else if (SubFrameVerbatim.matchesType(type))
				subFrame = SubFrameVerbatim(channelNumber, audioFrame, data)
			else if (SubFrameFixed.matchesType(type))
				subFrame = SubFrameFixed(type, channelNumber, audioFrame, data)
			else if (SubFrameLPC.matchesType(type))
				subFrame = SubFrameLPC(type, channelNumber, audioFrame, data)
			else
				subFrame = SubFrameReserved(audioFrame)

			// Record details, and return
			subFrame.wastedBits = wastedBits
			return subFrame
		}
	}
}
