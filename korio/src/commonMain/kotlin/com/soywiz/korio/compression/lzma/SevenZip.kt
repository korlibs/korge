@file:Suppress("PrivatePropertyName", "NAME_SHADOWING", "PropertyName")

package com.soywiz.korio.compression.lzma

import com.soywiz.korio.experimental.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.internal.max2
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.checksum.*
import kotlin.math.*

// Ported from the public domain Java version of the LZMA SDK : https://www.7-zip.org/download.html

@UseExperimental(KorioExperimentalApi::class)
object SevenZip {
	interface ICodeProgress {
		fun setProgress(inSize: Long, outSize: Long)
	}

	class BitTreeEncoder(private var numBitLevels: Int) {
		private val models: ShortArray = ShortArray(1 shl numBitLevels)

		fun init() {
			RangeDecoder.initBitModels(models)
		}

		fun encode(rangeEncoder: RangeEncoder, symbol: Int) {
			var m = 1
			var bitIndex = numBitLevels
			while (bitIndex != 0) {
				bitIndex--
				val bit = symbol.ushr(bitIndex) and 1
				rangeEncoder.encode(models, m, bit)
				m = m shl 1 or bit
			}
		}

		fun reverseEncode(rangeEncoder: RangeEncoder, symbol: Int) {
			var symbol = symbol
			var m = 1
			for (i in 0 until numBitLevels) {
				val bit = symbol and 1
				rangeEncoder.encode(models, m, bit)
				m = m shl 1 or bit
				symbol = symbol shr 1
			}
		}

		fun getPrice(symbol: Int): Int {
			var price = 0
			var m = 1
			var bitIndex = numBitLevels
			while (bitIndex != 0) {
				bitIndex--
				val bit = symbol.ushr(bitIndex) and 1
				price += RangeEncoder.getPrice(
					models[m].toInt(),
					bit
				)
				m = (m shl 1) + bit
			}
			return price
		}

		fun reverseGetPrice(symbol: Int): Int {
			var symbol = symbol
			var price = 0
			var m = 1
			for (i in numBitLevels downTo 1) {
				val bit = symbol and 1
				symbol = symbol ushr 1
				price += RangeEncoder.getPrice(
					models[m].toInt(),
					bit
				)
				m = m shl 1 or bit
			}
			return price
		}

		companion object {
			fun reverseGetPrice(
				Models: ShortArray, startIndex: Int,
				NumBitLevels: Int, symbol: Int
			): Int {
				var symbol = symbol
				var price = 0
				var m = 1
				for (i in NumBitLevels downTo 1) {
					val bit = symbol and 1
					symbol = symbol ushr 1
					price += RangeEncoder.getPrice(
						Models[startIndex + m].toInt(),
						bit
					)
					m = m shl 1 or bit
				}
				return price
			}

			fun reverseEncode(
				Models: ShortArray, startIndex: Int,
				rangeEncoder: RangeEncoder, NumBitLevels: Int, symbol: Int
			) {
				var symbol = symbol
				var m = 1
				for (i in 0 until NumBitLevels) {
					val bit = symbol and 1
					rangeEncoder.encode(Models, startIndex + m, bit)
					m = m shl 1 or bit
					symbol = symbol shr 1
				}
			}
		}
	}

	class BitTreeDecoder(private var numBitLevels: Int) {
		private val models: ShortArray = ShortArray(1 shl numBitLevels)

		fun init() {
			RangeDecoder.initBitModels(models)
		}

		fun decode(rangeDecoder: RangeDecoder): Int {
			var m = 1
			for (bitIndex in numBitLevels downTo 1)
				m = (m shl 1) + rangeDecoder.decodeBit(models, m)
			return m - (1 shl numBitLevels)
		}

		fun reverseDecode(rangeDecoder: RangeDecoder): Int {
			var m = 1
			var symbol = 0
			for (bitIndex in 0 until numBitLevels) {
				val bit = rangeDecoder.decodeBit(models, m)
				m = m shl 1
				m += bit
				symbol = symbol or (bit shl bitIndex)
			}
			return symbol
		}

		companion object {
			fun reverseDecode(
				Models: ShortArray, startIndex: Int,
				rangeDecoder: RangeDecoder, NumBitLevels: Int
			): Int {
				var m = 1
				var symbol = 0
				for (bitIndex in 0 until NumBitLevels) {
					val bit = rangeDecoder.decodeBit(Models, startIndex + m)
					m = m shl 1
					m += bit
					symbol = symbol or (bit shl bitIndex)
				}
				return symbol
			}
		}
	}

	class RangeDecoder {

		internal var range: Int = 0
		internal var code: Int = 0

		internal var stream: SyncInputStream? = null

		fun setStream(stream: SyncInputStream) {
			this.stream = stream
		}

		fun releaseStream() {
			stream = null
		}

		fun init() {
			code = 0
			range = -1
			for (i in 0..4)
				code = code shl 8 or stream!!.read()
		}

		fun decodeDirectBits(numTotalBits: Int): Int {
			var result = 0
			for (i in numTotalBits downTo 1) {
				range = range ushr 1
				val t = (code - range).ushr(31)
				code -= range and t - 1
				result = result shl 1 or 1 - t

				if (range and kTopMask == 0) {
					code = code shl 8 or stream!!.read()
					range = range shl 8
				}
			}
			return result
		}

		fun decodeBit(probs: ShortArray, index: Int): Int {
			val prob = probs[index].toInt()
			val newBound = range.ushr(kNumBitModelTotalBits) * prob
			if (code xor -0x80000000 < newBound xor -0x80000000) {
				range = newBound
				probs[index] = (prob + (kBitModelTotal - prob).ushr(
					kNumMoveBits
				)).toShort()
				if (range and kTopMask == 0) {
					code = code shl 8 or stream!!.read()
					range = range shl 8
				}
				return 0
			} else {
				range -= newBound
				code -= newBound
				probs[index] = (prob - prob.ushr(kNumMoveBits)).toShort()
				if (range and kTopMask == 0) {
					code = code shl 8 or stream!!.read()
					range = range shl 8
				}
				return 1
			}
		}

		companion object {
			internal const val kTopMask = ((1 shl 24) - 1).inv()

			internal const val kNumBitModelTotalBits = 11
			internal const val kBitModelTotal = 1 shl kNumBitModelTotalBits
			internal const val kNumMoveBits = 5

			fun initBitModels(probs: ShortArray) {
				for (i in probs.indices)
					probs[i] = kBitModelTotal.ushr(1).toShort()
			}
		}
	}

	class RangeEncoder {

		internal var stream: SyncOutputStream? = null

		private var low: Long = 0
		internal var range: Int = 0
		private var _cacheSize: Int = 0
		private var _cache: Int = 0

		private var _position: Long = 0

		fun setStream(stream: SyncOutputStream) {
			this.stream = stream
		}

		fun releaseStream() {
			stream = null
		}

		fun init() {
			_position = 0
			low = 0
			range = -1
			_cacheSize = 1
			_cache = 0
		}

		fun flushData() {
			for (i in 0..4)
				shiftLow()
		}

		fun flushStream() {
			stream!!.flush()
		}

		private fun shiftLow() {
			val lowHi = low.ushr(32).toInt()
			if (lowHi != 0 || low < 0xFF000000L) {
				_position += _cacheSize.toLong()
				var temp = _cache
				do {
					stream!!.write8(temp + lowHi)
					temp = 0xFF
				} while (--_cacheSize != 0)
				_cache = low.toInt().ushr(24)
			}
			_cacheSize++
			low = low and 0xFFFFFF shl 8
		}

		fun encodeDirectBits(v: Int, numTotalBits: Int) {
			for (i in numTotalBits - 1 downTo 0) {
				range = range ushr 1
				if (v.ushr(i) and 1 == 1)
					low += range.toLong()
				if (range and kTopMask == 0) {
					range = range shl 8
					shiftLow()
				}
			}
		}


		fun getProcessedSizeAdd(): Long {
			return _cacheSize.toLong() + _position + 4
		}

		fun encode(probs: ShortArray, index: Int, symbol: Int) {
			val prob = probs[index].toInt()
			val newBound = range.ushr(kNumBitModelTotalBits) * prob
			if (symbol == 0) {
				range = newBound
				probs[index] = (prob + (kBitModelTotal - prob).ushr(
					kNumMoveBits
				)).toShort()
			} else {
				low += newBound and 0xFFFFFFFFL.toInt()
				range -= newBound
				probs[index] = (prob - prob.ushr(kNumMoveBits)).toShort()
			}
			if (range and kTopMask == 0) {
				range = range shl 8
				shiftLow()
			}
		}

		companion object {
			internal const val kTopMask = ((1 shl 24) - 1).inv()

			internal const val kNumBitModelTotalBits = 11
			internal const val kBitModelTotal = 1 shl kNumBitModelTotalBits
			internal const val kNumMoveBits = 5
			private const val kNumMoveReducingBits = 2
			const val kNumBitPriceShiftBits = 6

			fun initBitModels(probs: ShortArray) {
				for (i in probs.indices) probs[i] = kBitModelTotal.ushr(1).toShort()
			}

			private val probPrices = IntArray(kBitModelTotal ushr kNumMoveReducingBits)

			init {
				val kNumBits = kNumBitModelTotalBits - kNumMoveReducingBits
				for (i in kNumBits - 1 downTo 0) {
					val start = 1 shl kNumBits - i - 1
					val end = 1 shl kNumBits - i
					for (j in start until end)
						probPrices[j] = (i shl kNumBitPriceShiftBits) +
								(end - j shl kNumBitPriceShiftBits).ushr(kNumBits - i - 1)
				}
			}

			fun getPrice(Prob: Int, symbol: Int): Int = probPrices[(Prob - symbol xor -symbol and kBitModelTotal - 1).ushr(kNumMoveReducingBits)]
			fun getPrice0(Prob: Int): Int = probPrices[Prob ushr kNumMoveReducingBits]
			fun getPrice1(Prob: Int): Int = probPrices[(kBitModelTotal - Prob).ushr(kNumMoveReducingBits)]
		}
	}

	object LzmaBase {
		const val kNumRepDistances = 4
		const val kNumStates = 12

		const val kNumPosSlotBits = 6
		const val kDicLogSizeMin = 0
		// public static final int kDicLogSizeMax = 28;
		// public static final int kDistTableSizeMax = kDicLogSizeMax * 2;

		const val kNumLenToPosStatesBits = 2 // it's for speed optimization
		const val kNumLenToPosStates = 1 shl kNumLenToPosStatesBits

		const val kMatchMinLen = 2

		const val kNumAlignBits = 4
		const val kAlignTableSize = 1 shl kNumAlignBits
		const val kAlignMask = kAlignTableSize - 1

		const val kStartPosModelIndex = 4
		const val kEndPosModelIndex = 14
		//const val kNumPosModels = kEndPosModelIndex - kStartPosModelIndex

		const val kNumFullDistances = 1 shl kEndPosModelIndex / 2

		const val kNumLitPosStatesBitsEncodingMax = 4
		const val kNumLitContextBitsMax = 8

		const val kNumPosStatesBitsMax = 4
		const val kNumPosStatesMax = 1 shl kNumPosStatesBitsMax
		const val kNumPosStatesBitsEncodingMax = 4
		const val kNumPosStatesEncodingMax = 1 shl kNumPosStatesBitsEncodingMax

		const val kNumLowLenBits = 3
		const val kNumMidLenBits = 3
		const val kNumHighLenBits = 8
		const val kNumLowLenSymbols = 1 shl kNumLowLenBits
		const val kNumMidLenSymbols = 1 shl kNumMidLenBits
		const val kNumLenSymbols = kNumLowLenSymbols + kNumMidLenSymbols + (1 shl kNumHighLenBits)
		const val kMatchMaxLen = kMatchMinLen + kNumLenSymbols - 1

		fun stateInit(): Int = 0

		fun stateUpdateChar(index: Int): Int = when {
			index < 4 -> 0
			index < 10 -> index - 3
			else -> index - 6
		}

		fun stateUpdateMatch(index: Int): Int = if (index < 7) 7 else 10
		fun stateUpdateRep(index: Int): Int = if (index < 7) 8 else 11
		fun stateUpdateShortRep(index: Int): Int = if (index < 7) 9 else 11
		fun stateIsCharState(index: Int): Boolean = index < 7

		fun getLenToPosState(len: Int): Int {
			var len = len
			len -= kMatchMinLen
			return if (len < kNumLenToPosStates) len else kNumLenToPosStates - 1
		}
	}

	class LzmaDecoder {
		private var m_OutWindow = LzOutWindow()
		private var m_RangeDecoder = RangeDecoder()

		private var m_IsMatchDecoders = ShortArray(LzmaBase.kNumStates shl LzmaBase.kNumPosStatesBitsMax)
		private var m_IsRepDecoders = ShortArray(LzmaBase.kNumStates)
		private var m_IsRepG0Decoders = ShortArray(LzmaBase.kNumStates)
		private var m_IsRepG1Decoders = ShortArray(LzmaBase.kNumStates)
		private var m_IsRepG2Decoders = ShortArray(LzmaBase.kNumStates)
		private var m_IsRep0LongDecoders = ShortArray(LzmaBase.kNumStates shl LzmaBase.kNumPosStatesBitsMax)

		private var m_PosSlotDecoder = arrayOfNulls<BitTreeDecoder>(LzmaBase.kNumLenToPosStates)
		private var m_PosDecoders = ShortArray(LzmaBase.kNumFullDistances - LzmaBase.kEndPosModelIndex)
		private var m_PosAlignDecoder = BitTreeDecoder(LzmaBase.kNumAlignBits)

		private var m_LenDecoder = LenDecoder()
		private var m_RepLenDecoder = LenDecoder()

		private var m_LiteralDecoder = LiteralDecoder()

		private var m_DictionarySize = -1
		private var m_DictionarySizeCheck = -1

		private var m_PosStateMask: Int = 0

		internal inner class LenDecoder {
			private var m_Choice = ShortArray(2)
			private var m_LowCoder = arrayOfNulls<BitTreeDecoder>(LzmaBase.kNumPosStatesMax)
			private var m_MidCoder = arrayOfNulls<BitTreeDecoder>(LzmaBase.kNumPosStatesMax)
			private var m_HighCoder = BitTreeDecoder(LzmaBase.kNumHighLenBits)
			private var m_NumPosStates = 0

			fun create(numPosStates: Int) {
				while (m_NumPosStates < numPosStates) {
					m_LowCoder[m_NumPosStates] = BitTreeDecoder(LzmaBase.kNumLowLenBits)
					m_MidCoder[m_NumPosStates] = BitTreeDecoder(LzmaBase.kNumMidLenBits)
					m_NumPosStates++
				}
			}

			fun init() {
				RangeDecoder.initBitModels(m_Choice)
				for (posState in 0 until m_NumPosStates) {
					m_LowCoder[posState]!!.init()
					m_MidCoder[posState]!!.init()
				}
				m_HighCoder.init()
			}

			fun decode(rangeDecoder: RangeDecoder, posState: Int): Int {
				if (rangeDecoder.decodeBit(m_Choice, 0) == 0) return m_LowCoder[posState]!!.decode(rangeDecoder)
				var symbol = LzmaBase.kNumLowLenSymbols
				symbol += if (rangeDecoder.decodeBit(m_Choice, 1) == 0) m_MidCoder[posState]!!.decode(rangeDecoder) else LzmaBase.kNumMidLenSymbols + m_HighCoder.decode(rangeDecoder)
				return symbol
			}
		}

		internal inner class LiteralDecoder {
			private var m_Coders: Array<Decoder2>? = null
			private var m_NumPrevBits: Int = 0
			private var m_NumPosBits: Int = 0
			private var m_PosMask: Int = 0

			internal inner class Decoder2 {
				private var m_Decoders = ShortArray(0x300)

				fun init() {
					RangeDecoder.initBitModels(
						m_Decoders
					)
				}

				fun decodeNormal(rangeDecoder: RangeDecoder): Byte {
					var symbol = 1
					do { symbol = symbol shl 1 or rangeDecoder.decodeBit(m_Decoders, symbol) } while (symbol < 0x100)
					return symbol.toByte()
				}

				fun decodeWithMatchByte(rangeDecoder: RangeDecoder, matchByte: Byte): Byte {
					var matchByte = matchByte
					var symbol = 1
					do {
						val matchBit = (matchByte shr 7) and 1
						matchByte = ((matchByte shl 1).toByte())
						val bit = rangeDecoder.decodeBit(m_Decoders, (1 + matchBit shl 8) + symbol)
						symbol = symbol shl 1 or bit
						if (matchBit != bit) {
							while (symbol < 0x100) symbol = symbol shl 1 or rangeDecoder.decodeBit(m_Decoders, symbol)
							break
						}
					} while (symbol < 0x100)
					return symbol.toByte()
				}
			}

			fun create(numPosBits: Int, numPrevBits: Int) {
				if (m_Coders != null && m_NumPrevBits == numPrevBits && m_NumPosBits == numPosBits) return
				m_NumPosBits = numPosBits
				m_PosMask = (1 shl numPosBits) - 1
				m_NumPrevBits = numPrevBits
				val numStates = 1 shl m_NumPrevBits + m_NumPosBits
				m_Coders = Array(numStates) { Decoder2() }
			}

			fun init() {
				val numStates = 1 shl m_NumPrevBits + m_NumPosBits
				for (i in 0 until numStates)
					m_Coders!![i].init()
			}

			fun getDecoder(pos: Int, prevByte: Byte): Decoder2 = m_Coders!![(pos and m_PosMask shl m_NumPrevBits) + (prevByte and 0xFF).ushr(8 - m_NumPrevBits)]
		}

		init {
			for (i in 0 until LzmaBase.kNumLenToPosStates) m_PosSlotDecoder[i] = BitTreeDecoder(LzmaBase.kNumPosSlotBits)
		}

		private fun setDictionarySize(dictionarySize: Int): Boolean {
			if (dictionarySize < 0) return false
			if (m_DictionarySize != dictionarySize) {
				m_DictionarySize = dictionarySize
				m_DictionarySizeCheck = max2(m_DictionarySize, 1)
				m_OutWindow.create(max2(m_DictionarySizeCheck, 1 shl 12))
			}
			return true
		}

		private fun setLcLpPb(lc: Int, lp: Int, pb: Int): Boolean {
			if (lc > LzmaBase.kNumLitContextBitsMax || lp > 4 || pb > LzmaBase.kNumPosStatesBitsMax) return false
			m_LiteralDecoder.create(lp, lc)
			val numPosStates = 1 shl pb
			m_LenDecoder.create(numPosStates)
			m_RepLenDecoder.create(numPosStates)
			m_PosStateMask = numPosStates - 1
			return true
		}

		internal fun init() {
			m_OutWindow.init(false)

			RangeDecoder.initBitModels(m_IsMatchDecoders)
			RangeDecoder.initBitModels(m_IsRep0LongDecoders)
			RangeDecoder.initBitModels(m_IsRepDecoders)
			RangeDecoder.initBitModels(m_IsRepG0Decoders)
			RangeDecoder.initBitModels(m_IsRepG1Decoders)
			RangeDecoder.initBitModels(m_IsRepG2Decoders)
			RangeDecoder.initBitModels(m_PosDecoders)

			m_LiteralDecoder.init()
			for (i in 0 until LzmaBase.kNumLenToPosStates) m_PosSlotDecoder[i]!!.init()
			m_LenDecoder.init()
			m_RepLenDecoder.init()
			m_PosAlignDecoder.init()
			m_RangeDecoder.init()
		}

		fun code(inStream: SyncInputStream, outStream: SyncOutputStream, outSize: Long): Boolean {
			m_RangeDecoder.setStream(inStream)
			m_OutWindow.setStream(outStream)
			init()

			var state = LzmaBase.stateInit()
			var rep0 = 0
			var rep1 = 0
			var rep2 = 0
			var rep3 = 0

			var nowPos64: Long = 0
			var prevByte: Byte = 0
			while (outSize < 0 || nowPos64 < outSize) {
				val posState = nowPos64.toInt() and m_PosStateMask
				if (m_RangeDecoder.decodeBit(m_IsMatchDecoders, (state shl LzmaBase.kNumPosStatesBitsMax) + posState) == 0) {
					val decoder2 = m_LiteralDecoder.getDecoder(nowPos64.toInt(), prevByte)
					prevByte = if (!LzmaBase.stateIsCharState(state))
						decoder2.decodeWithMatchByte(m_RangeDecoder, m_OutWindow.getByte(rep0))
					else
						decoder2.decodeNormal(m_RangeDecoder)
					m_OutWindow.putByte(prevByte)
					state = LzmaBase.stateUpdateChar(state)
					nowPos64++
				} else {
					var len: Int
					if (m_RangeDecoder.decodeBit(m_IsRepDecoders, state) == 1) {
						len = 0
						if (m_RangeDecoder.decodeBit(m_IsRepG0Decoders, state) == 0) {
							if (m_RangeDecoder.decodeBit(
									m_IsRep0LongDecoders,
									(state shl LzmaBase.kNumPosStatesBitsMax) + posState
								) == 0
							) {
								state = LzmaBase.stateUpdateShortRep(
									state
								)
								len = 1
							}
						} else {
							val distance: Int
							if (m_RangeDecoder.decodeBit(m_IsRepG1Decoders, state) == 0)
								distance = rep1
							else {
								if (m_RangeDecoder.decodeBit(m_IsRepG2Decoders, state) == 0)
									distance = rep2
								else {
									distance = rep3
									rep3 = rep2
								}
								rep2 = rep1
							}
							rep1 = rep0
							rep0 = distance
						}
						if (len == 0) {
							len = m_RepLenDecoder.decode(m_RangeDecoder, posState) +
									LzmaBase.kMatchMinLen
							state = LzmaBase.stateUpdateRep(state)
						}
					} else {
						rep3 = rep2
						rep2 = rep1
						rep1 = rep0
						len = LzmaBase.kMatchMinLen + m_LenDecoder.decode(m_RangeDecoder, posState)
						state = LzmaBase.stateUpdateMatch(state)
						val posSlot = m_PosSlotDecoder[LzmaBase.getLenToPosState(
							len
						)]!!.decode(m_RangeDecoder)
						if (posSlot >= LzmaBase.kStartPosModelIndex) {
							val numDirectBits = (posSlot shr 1) - 1
							rep0 = 2 or (posSlot and 1) shl numDirectBits
							if (posSlot < LzmaBase.kEndPosModelIndex)
								rep0 += BitTreeDecoder.reverseDecode(
									m_PosDecoders,
									rep0 - posSlot - 1, m_RangeDecoder, numDirectBits
								)
							else {
								rep0 += m_RangeDecoder.decodeDirectBits(
									numDirectBits - LzmaBase.kNumAlignBits
								) shl LzmaBase.kNumAlignBits
								rep0 += m_PosAlignDecoder.reverseDecode(m_RangeDecoder)
								if (rep0 < 0) {
									if (rep0 == -1)
										break
									return false
								}
							}
						} else
							rep0 = posSlot
					}
					if (rep0 >= nowPos64 || rep0 >= m_DictionarySizeCheck) {
						// m_OutWindow.Flush();
						return false
					}
					m_OutWindow.copyBlock(rep0, len)
					nowPos64 += len.toLong()
					prevByte = m_OutWindow.getByte(0)
				}
			}
			m_OutWindow.flush()
			m_OutWindow.releaseStream()
			m_RangeDecoder.releaseStream()
			return true
		}

		fun setDecoderProperties(properties: ByteArray): Boolean {
			if (properties.size < 5) return false
			val `val` = properties[0] and 0xFF
			val lc = `val` % 9
			val remainder = `val` / 9
			val lp = remainder % 5
			val pb = remainder / 5
			var dictionarySize = 0
			for (i in 0..3) dictionarySize += properties[1 + i].toInt() and 0xFF shl i * 8
			return if (!setLcLpPb(lc, lp, pb)) false else setDictionarySize(dictionarySize)
		}
	}

	class LzmaEncoder {
		private var _state = LzmaBase.stateInit()
		private var _previousByte: Byte = 0
		private var _repDistances = IntArray(LzmaBase.kNumRepDistances)
		private var _optimum = Array(kNumOpts) { Optimal() }

		private var _matchFinder: LzBinTree? = null
		private var _rangeEncoder = RangeEncoder()

		private var _isMatch = ShortArray(LzmaBase.kNumStates shl LzmaBase.kNumPosStatesBitsMax)
		private var _isRep = ShortArray(LzmaBase.kNumStates)
		private var _isRepG0 = ShortArray(LzmaBase.kNumStates)
		private var _isRepG1 = ShortArray(LzmaBase.kNumStates)
		private var _isRepG2 = ShortArray(LzmaBase.kNumStates)
		private var _isRep0Long = ShortArray(LzmaBase.kNumStates shl LzmaBase.kNumPosStatesBitsMax)

		private var _posSlotEncoder = Array(LzmaBase.kNumLenToPosStates) { BitTreeEncoder(LzmaBase.kNumPosSlotBits) } // kNumPosSlotBits

		private var _posEncoders = ShortArray(LzmaBase.kNumFullDistances - LzmaBase.kEndPosModelIndex)
		private var _posAlignEncoder = BitTreeEncoder(LzmaBase.kNumAlignBits)

		private var _lenEncoder = LenPriceTableEncoder()
		private var _repMatchLenEncoder = LenPriceTableEncoder()

		private var _literalEncoder = LiteralEncoder()

		private var _matchDistances = IntArray(LzmaBase.kMatchMaxLen * 2 + 2)

		private var _numFastBytes = kNumFastBytesDefault
		private var _longestMatchLength: Int = 0
		private var _numDistancePairs: Int = 0

		private var _additionalOffset: Int = 0

		private var _optimumEndIndex: Int = 0
		private var _optimumCurrentIndex: Int = 0

		private var _longestMatchWasFound: Boolean = false

		private var _posSlotPrices = IntArray(1 shl LzmaBase.kNumPosSlotBits + LzmaBase.kNumLenToPosStatesBits)
		private var _distancesPrices = IntArray(LzmaBase.kNumFullDistances shl LzmaBase.kNumLenToPosStatesBits)
		private var _alignPrices = IntArray(LzmaBase.kAlignTableSize)
		private var _alignPriceCount: Int = 0

		private var _distTableSize = kDefaultDictionaryLogSize * 2

		private var _posStateBits = 2
		private var _posStateMask = 4 - 1
		private var _numLiteralPosStateBits = 0
		private var _numLiteralContextBits = 3

		private var _dictionarySize = 1 shl kDefaultDictionaryLogSize
		private var _dictionarySizePrev = -1
		private var _numFastBytesPrev = -1

		private var nowPos64: Long = 0
		private var _finished: Boolean = false
		private var _inStream: SyncInputStream? = null

		private var _matchFinderType = EMatchFinderTypeBT4
		private var _writeEndMark = false

		private var _needReleaseMFStream = false

		private var reps = IntArray(LzmaBase.kNumRepDistances)
		private var repLens = IntArray(LzmaBase.kNumRepDistances)
		private var backRes: Int = 0

		private var processedInSize = LongArray(1)
		private var processedOutSize = LongArray(1)
		private var finished = BooleanArray(1)
		private var properties = ByteArray(kPropSize)

		private var tempPrices = IntArray(LzmaBase.kNumFullDistances)
		private var _matchPriceCount: Int = 0

		private fun baseInit() {
			_state = LzmaBase.stateInit()
			_previousByte = 0
			for (i in 0 until LzmaBase.kNumRepDistances) _repDistances[i] = 0
		}

		internal inner class LiteralEncoder {
			private var m_Coders: Array<Encoder2>? = null
			private var m_NumPrevBits: Int = 0
			private var m_NumPosBits: Int = 0
			private var m_PosMask: Int = 0

			internal inner class Encoder2 {
				private var m_Encoders = ShortArray(0x300)

				fun init() {
					RangeEncoder.initBitModels(m_Encoders)
				}

				fun encode(rangeEncoder: RangeEncoder, symbol: Byte) {
					var context = 1
					for (i in 7 downTo 0) {
						val bit = (symbol shr i) and 1
						rangeEncoder.encode(m_Encoders, context, bit)
						context = context shl 1 or bit
					}
				}

				fun encodeMatched(rangeEncoder: RangeEncoder, matchByte: Byte, symbol: Byte) {
					var context = 1
					var same = true
					for (i in 7 downTo 0) {
						val bit = symbol shr i and 1
						var state = context
						if (same) {
							val matchBit = matchByte shr i and 1
							state += 1 + matchBit shl 8
							same = matchBit == bit
						}
						rangeEncoder.encode(m_Encoders, state, bit)
						context = context shl 1 or bit
					}
				}

				fun getPrice(matchMode: Boolean, matchByte: Byte, symbol: Byte): Int {
					var price = 0
					var context = 1
					var i = 7
					if (matchMode) {
						while (i >= 0) {
							val matchBit = matchByte shr i and 1
							val bit = symbol shr i and 1
							price += RangeEncoder.getPrice(
								m_Encoders[(1 + matchBit shl 8) + context].toInt(),
								bit
							)
							context = context shl 1 or bit
							if (matchBit != bit) {
								i--
								break
							}
							i--
						}
					}
					while (i >= 0) {
						val bit = symbol shr i and 1
						price += RangeEncoder.getPrice(
							m_Encoders[context].toInt(),
							bit
						)
						context = context shl 1 or bit
						i--
					}
					return price
				}
			}

			fun create(numPosBits: Int, numPrevBits: Int) {
				if (m_Coders != null && m_NumPrevBits == numPrevBits && m_NumPosBits == numPosBits)
					return
				m_NumPosBits = numPosBits
				m_PosMask = (1 shl numPosBits) - 1
				m_NumPrevBits = numPrevBits
				val numStates = 1 shl m_NumPrevBits + m_NumPosBits
				m_Coders = Array(numStates) { Encoder2() }
			}

			fun init() {
				val numStates = 1 shl m_NumPrevBits + m_NumPosBits
				for (i in 0 until numStates)
					m_Coders!![i].init()
			}

			fun getSubCoder(pos: Int, prevByte: Byte): Encoder2 =
				m_Coders!![(pos and m_PosMask shl m_NumPrevBits) + (prevByte and 0xFF).ushr(8 - m_NumPrevBits)]
		}

		internal open inner class LenEncoder {
			private var _choice = ShortArray(2)
			private var _lowCoder = Array(LzmaBase.kNumPosStatesEncodingMax) { BitTreeEncoder(LzmaBase.kNumLowLenBits) }
			private var _midCoder = Array(LzmaBase.kNumPosStatesEncodingMax) { BitTreeEncoder(LzmaBase.kNumMidLenBits) }
			private var _highCoder = BitTreeEncoder(LzmaBase.kNumHighLenBits)

			fun init(numPosStates: Int) {
				RangeEncoder.initBitModels(_choice)

				for (posState in 0 until numPosStates) {
					_lowCoder[posState].init()
					_midCoder[posState].init()
				}
				_highCoder.init()
			}

			open fun encode(rangeEncoder: RangeEncoder, symbol: Int, posState: Int) {
				var sym = symbol
				if (sym < LzmaBase.kNumLowLenSymbols) {
					rangeEncoder.encode(_choice, 0, 0)
					_lowCoder[posState].encode(rangeEncoder, sym)
				} else {
					sym -= LzmaBase.kNumLowLenSymbols
					rangeEncoder.encode(_choice, 0, 1)
					if (sym < LzmaBase.kNumMidLenSymbols) {
						rangeEncoder.encode(_choice, 1, 0)
						_midCoder[posState].encode(rangeEncoder, sym)
					} else {
						rangeEncoder.encode(_choice, 1, 1)
						_highCoder.encode(rangeEncoder, sym - LzmaBase.kNumMidLenSymbols)
					}
				}
			}

			fun setPrices(posState: Int, numSymbols: Int, prices: IntArray, st: Int) {
				val a0 = RangeEncoder.getPrice0(_choice[0].toInt())
				val a1 = RangeEncoder.getPrice1(_choice[0].toInt())
				val b0 = a1 + RangeEncoder.getPrice0(_choice[1].toInt())
				val b1 = a1 + RangeEncoder.getPrice1(_choice[1].toInt())
				var i = 0
				while (i < LzmaBase.kNumLowLenSymbols) {
					if (i >= numSymbols) return
					prices[st + i] = a0 + _lowCoder[posState].getPrice(i)
					i++
				}
				while (i < LzmaBase.kNumLowLenSymbols + LzmaBase.kNumMidLenSymbols) {
					if (i >= numSymbols) return
					prices[st + i] = b0 + _midCoder[posState].getPrice(i - LzmaBase.kNumLowLenSymbols)
					i++
				}
				while (i < numSymbols) {
					prices[st + i] = b1 + _highCoder.getPrice(i - LzmaBase.kNumLowLenSymbols - LzmaBase.kNumMidLenSymbols)
					i++
				}
			}
		}

		internal inner class LenPriceTableEncoder : LenEncoder() {
			private var _prices = IntArray(LzmaBase.kNumLenSymbols shl LzmaBase.kNumPosStatesBitsEncodingMax)
			private var _tableSize: Int = 0
			private var _counters = IntArray(LzmaBase.kNumPosStatesEncodingMax)

			fun setTableSize(tableSize: Int) {
				_tableSize = tableSize
			}

			fun getPrice(symbol: Int, posState: Int): Int = _prices[posState * LzmaBase.kNumLenSymbols + symbol]

			private fun updateTable(posState: Int) {
				setPrices(posState, _tableSize, _prices, posState * LzmaBase.kNumLenSymbols)
				_counters[posState] = _tableSize
			}

			fun updateTables(numPosStates: Int) = run { for (posState in 0 until numPosStates) updateTable(posState) }

			override fun encode(rangeEncoder: RangeEncoder, symbol: Int, posState: Int) {
				super.encode(rangeEncoder, symbol, posState)
				if (--_counters[posState] == 0)
					updateTable(posState)
			}
		}

		internal inner class Optimal {
			var state: Int = 0

			var prev1IsChar: Boolean = false
			var prev2: Boolean = false

			var posPrev2: Int = 0
			var backPrev2: Int = 0

			var price: Int = 0
			var posPrev: Int = 0
			var backPrev: Int = 0

			var backs0: Int = 0
			var backs1: Int = 0
			var backs2: Int = 0
			var backs3: Int = 0

			fun makeAsChar() {
				backPrev = -1
				prev1IsChar = false
			}

			fun makeAsShortRep() {
				backPrev = 0
				prev1IsChar = false
			}

			fun isShortRep(): Boolean = backPrev == 0
		}

		internal fun create() {
			if (_matchFinder == null) {
				val bt = LzBinTree()
				var numHashBytes = 4
				if (_matchFinderType == EMatchFinderTypeBT2)
					numHashBytes = 2
				bt.setType(numHashBytes)
				_matchFinder = bt
			}
			_literalEncoder.create(_numLiteralPosStateBits, _numLiteralContextBits)

			if (_dictionarySize == _dictionarySizePrev && _numFastBytesPrev == _numFastBytes)
				return
			_matchFinder!!.create(
				_dictionarySize,
				kNumOpts, _numFastBytes, LzmaBase.kMatchMaxLen + 1
			)
			_dictionarySizePrev = _dictionarySize
			_numFastBytesPrev = _numFastBytes
		}

		//internal fun setWriteEndMarkerMode(writeEndMarker: Boolean) { _writeEndMark = writeEndMarker }

		internal fun init() {
			baseInit()
			_rangeEncoder.init()

			RangeEncoder.initBitModels(_isMatch)
			RangeEncoder.initBitModels(_isRep0Long)
			RangeEncoder.initBitModels(_isRep)
			RangeEncoder.initBitModels(_isRepG0)
			RangeEncoder.initBitModels(_isRepG1)
			RangeEncoder.initBitModels(_isRepG2)
			RangeEncoder.initBitModels(_posEncoders)

			_literalEncoder.init()
			for (i in 0 until LzmaBase.kNumLenToPosStates) {
				_posSlotEncoder[i].init()
			}

			_lenEncoder.init(1 shl _posStateBits)
			_repMatchLenEncoder.init(1 shl _posStateBits)

			_posAlignEncoder.init()

			_longestMatchWasFound = false
			_optimumEndIndex = 0
			_optimumCurrentIndex = 0
			_additionalOffset = 0
		}

		private fun readMatchDistances(): Int {
			var lenRes = 0
			_numDistancePairs = _matchFinder!!.getMatches(_matchDistances)
			if (_numDistancePairs > 0) {
				lenRes = _matchDistances[_numDistancePairs - 2]
				if (lenRes == _numFastBytes)
					lenRes += _matchFinder!!.getMatchLen(
						lenRes - 1, _matchDistances[_numDistancePairs - 1],
						LzmaBase.kMatchMaxLen - lenRes
					)
			}
			_additionalOffset++
			return lenRes
		}

		private fun movePos(num: Int) {
			if (num > 0) {
				_matchFinder!!.skip(num)
				_additionalOffset += num
			}
		}

		private fun getRepLen1Price(state: Int, posState: Int): Int =
			RangeEncoder.getPrice0(_isRepG0[state].toInt()) + RangeEncoder.getPrice0(
				_isRep0Long[(state shl LzmaBase.kNumPosStatesBitsMax) + posState].toInt()
			)

		private fun getPureRepPrice(repIndex: Int, state: Int, posState: Int): Int {
			var price: Int
			if (repIndex == 0) {
				price = RangeEncoder.getPrice0(_isRepG0[state].toInt())
				price += RangeEncoder.getPrice1(
					_isRep0Long[(state shl LzmaBase.kNumPosStatesBitsMax) + posState].toInt()
				)
			} else {
				price = RangeEncoder.getPrice1(_isRepG0[state].toInt())
				if (repIndex == 1) {
					price += RangeEncoder.getPrice0(_isRepG1[state].toInt())
				} else {
					price += RangeEncoder.getPrice1(_isRepG1[state].toInt())
					price += RangeEncoder.getPrice(_isRepG2[state].toInt(), repIndex - 2)
				}
			}
			return price
		}

		private fun getRepPrice(repIndex: Int, len: Int, state: Int, posState: Int): Int {
			val price = _repMatchLenEncoder.getPrice(len - LzmaBase.kMatchMinLen, posState)
			return price + getPureRepPrice(repIndex, state, posState)
		}

		private fun getPosLenPrice(pos: Int, len: Int, posState: Int): Int {
			val price: Int
			val lenToPosState = LzmaBase.getLenToPosState(len)
			price = if (pos < LzmaBase.kNumFullDistances)
				_distancesPrices[lenToPosState * LzmaBase.kNumFullDistances + pos]
			else
				_posSlotPrices[(lenToPosState shl LzmaBase.kNumPosSlotBits) + getPosSlot2(pos)] + _alignPrices[pos and LzmaBase.kAlignMask]
			return price + _lenEncoder.getPrice(len - LzmaBase.kMatchMinLen, posState)
		}

		private fun backward(cur: Int): Int {
			var cc = cur
			_optimumEndIndex = cc
			var posMem = _optimum[cc].posPrev
			var backMem = _optimum[cc].backPrev
			do {
				if (_optimum[cc].prev1IsChar) {
					_optimum[posMem].makeAsChar()
					_optimum[posMem].posPrev = posMem - 1
					if (_optimum[cc].prev2) {
						_optimum[posMem - 1].prev1IsChar = false
						_optimum[posMem - 1].posPrev = _optimum[cc].posPrev2
						_optimum[posMem - 1].backPrev = _optimum[cc].backPrev2
					}
				}
				val posPrev = posMem
				val backCur = backMem

				backMem = _optimum[posPrev].backPrev
				posMem = _optimum[posPrev].posPrev

				_optimum[posPrev].backPrev = backCur
				_optimum[posPrev].posPrev = cc
				cc = posPrev
			} while (cc > 0)
			backRes = _optimum[0].backPrev
			_optimumCurrentIndex = _optimum[0].posPrev
			return _optimumCurrentIndex
		}

		private fun getOptimum(position: Int): Int {
			var ppos = position
			if (_optimumEndIndex != _optimumCurrentIndex) {
				val lenRes = _optimum[_optimumCurrentIndex].posPrev - _optimumCurrentIndex
				backRes = _optimum[_optimumCurrentIndex].backPrev
				_optimumCurrentIndex = _optimum[_optimumCurrentIndex].posPrev
				return lenRes
			}
			_optimumEndIndex = 0
			_optimumCurrentIndex = _optimumEndIndex

			val lenMain: Int
			var numDistancePairs: Int
			if (!_longestMatchWasFound) {
				lenMain = readMatchDistances()
			} else {
				lenMain = _longestMatchLength
				_longestMatchWasFound = false
			}
			numDistancePairs = _numDistancePairs

			var numAvailableBytes = _matchFinder!!.getNumAvailableBytes() + 1
			if (numAvailableBytes < 2) {
				backRes = -1
				return 1
			}
			if (numAvailableBytes > LzmaBase.kMatchMaxLen) {
				@Suppress("UNUSED_VALUE")
				numAvailableBytes = LzmaBase.kMatchMaxLen
			}

			var repMaxIndex = 0
			var i = 0
			while (i < LzmaBase.kNumRepDistances) {
				reps[i] = _repDistances[i]
				repLens[i] = _matchFinder!!.getMatchLen(
					0 - 1, reps[i],
					LzmaBase.kMatchMaxLen
				)
				if (repLens[i] > repLens[repMaxIndex])
					repMaxIndex = i
				i++
			}
			if (repLens[repMaxIndex] >= _numFastBytes) {
				backRes = repMaxIndex
				val lenRes = repLens[repMaxIndex]
				movePos(lenRes - 1)
				return lenRes
			}

			if (lenMain >= _numFastBytes) {
				backRes = _matchDistances[numDistancePairs - 1] +
						LzmaBase.kNumRepDistances
				movePos(lenMain - 1)
				return lenMain
			}

			var currentByte = _matchFinder!!.getIndexByte(0 - 1)
			var matchByte = _matchFinder!!.getIndexByte(0 - _repDistances[0] - 1 - 1)

			if (lenMain < 2 && currentByte != matchByte && repLens[repMaxIndex] < 2) {
				backRes = -1
				return 1
			}

			_optimum[0].state = _state

			var posState = ppos and _posStateMask

			_optimum[1].price = RangeEncoder.getPrice0(
				_isMatch[(_state shl LzmaBase.kNumPosStatesBitsMax) + posState].toInt()
			) +
					_literalEncoder.getSubCoder(ppos, _previousByte).getPrice(
						!LzmaBase.stateIsCharState(_state),
						matchByte,
						currentByte
					)
			_optimum[1].makeAsChar()

			var matchPrice =
				RangeEncoder.getPrice1(_isMatch[(_state shl LzmaBase.kNumPosStatesBitsMax) + posState].toInt())
			var repMatchPrice =
				matchPrice + RangeEncoder.getPrice1(
					_isRep[_state].toInt()
				)

			if (matchByte == currentByte) {
				val shortRepPrice = repMatchPrice + getRepLen1Price(_state, posState)
				if (shortRepPrice < _optimum[1].price) {
					_optimum[1].price = shortRepPrice
					_optimum[1].makeAsShortRep()
				}
			}

			var lenEnd = if (lenMain >= repLens[repMaxIndex]) lenMain else repLens[repMaxIndex]

			if (lenEnd < 2) {
				backRes = _optimum[1].backPrev
				return 1
			}

			_optimum[1].posPrev = 0

			_optimum[0].backs0 = reps[0]
			_optimum[0].backs1 = reps[1]
			_optimum[0].backs2 = reps[2]
			_optimum[0].backs3 = reps[3]

			var len = lenEnd
			do
				_optimum[len--].price =
						kIfinityPrice
			while (len >= 2)

			i = 0
			while (i < LzmaBase.kNumRepDistances) {
				var repLen = repLens[i]
				if (repLen < 2) {
					i++
					continue
				}
				val price = repMatchPrice + getPureRepPrice(i, _state, posState)
				do {
					val curAndLenPrice = price + _repMatchLenEncoder.getPrice(repLen - 2, posState)
					val optimum = _optimum[repLen]
					if (curAndLenPrice < optimum.price) {
						optimum.price = curAndLenPrice
						optimum.posPrev = 0
						optimum.backPrev = i
						optimum.prev1IsChar = false
					}
				} while (--repLen >= 2)
				i++
			}

			var normalMatchPrice =
				matchPrice + RangeEncoder.getPrice0(
					_isRep[_state].toInt()
				)

			len = if (repLens[0] >= 2) repLens[0] + 1 else 2
			if (len <= lenMain) {
				var offs = 0
				while (len > _matchDistances[offs])
					offs += 2
				while (true) {
					val distance = _matchDistances[offs + 1]
					val curAndLenPrice = normalMatchPrice + getPosLenPrice(distance, len, posState)
					val optimum = _optimum[len]
					if (curAndLenPrice < optimum.price) {
						optimum.price = curAndLenPrice
						optimum.posPrev = 0
						optimum.backPrev = distance +
								LzmaBase.kNumRepDistances
						optimum.prev1IsChar = false
					}
					if (len == _matchDistances[offs]) {
						offs += 2
						if (offs == numDistancePairs)
							break
					}
					len++
				}
			}

			var cur = 0

			while (true) {
				cur++
				if (cur == lenEnd)
					return backward(cur)
				var newLen = readMatchDistances()
				numDistancePairs = _numDistancePairs
				if (newLen >= _numFastBytes) {

					_longestMatchLength = newLen
					_longestMatchWasFound = true
					return backward(cur)
				}
				ppos++
				var posPrev = _optimum[cur].posPrev
				var state: Int
				if (_optimum[cur].prev1IsChar) {
					posPrev--
					if (_optimum[cur].prev2) {
						state = _optimum[_optimum[cur].posPrev2].state
						state = if (_optimum[cur].backPrev2 < LzmaBase.kNumRepDistances)
							LzmaBase.stateUpdateRep(state)
						else
							LzmaBase.stateUpdateMatch(state)
					} else
						state = _optimum[posPrev].state
					state = LzmaBase.stateUpdateChar(state)
				} else
					state = _optimum[posPrev].state
				if (posPrev == cur - 1) {
					state = if (_optimum[cur].isShortRep())
						LzmaBase.stateUpdateShortRep(state)
					else
						LzmaBase.stateUpdateChar(state)
				} else {
					val pos: Int
					if (_optimum[cur].prev1IsChar && _optimum[cur].prev2) {
						posPrev = _optimum[cur].posPrev2
						pos = _optimum[cur].backPrev2
						state = LzmaBase.stateUpdateRep(state)
					} else {
						pos = _optimum[cur].backPrev
						state = if (pos < LzmaBase.kNumRepDistances)
							LzmaBase.stateUpdateRep(state)
						else
							LzmaBase.stateUpdateMatch(state)
					}
					val opt = _optimum[posPrev]
					if (pos < LzmaBase.kNumRepDistances) {
						when (pos) {
							0 -> {
								reps[0] = opt.backs0
								reps[1] = opt.backs1
								reps[2] = opt.backs2
								reps[3] = opt.backs3
							}
							1 -> {
								reps[0] = opt.backs1
								reps[1] = opt.backs0
								reps[2] = opt.backs2
								reps[3] = opt.backs3
							}
							2 -> {
								reps[0] = opt.backs2
								reps[1] = opt.backs0
								reps[2] = opt.backs1
								reps[3] = opt.backs3
							}
							else -> {
								reps[0] = opt.backs3
								reps[1] = opt.backs0
								reps[2] = opt.backs1
								reps[3] = opt.backs2
							}
						}
					} else {
						reps[0] = pos - LzmaBase.kNumRepDistances
						reps[1] = opt.backs0
						reps[2] = opt.backs1
						reps[3] = opt.backs2
					}
				}
				_optimum[cur].state = state
				_optimum[cur].backs0 = reps[0]
				_optimum[cur].backs1 = reps[1]
				_optimum[cur].backs2 = reps[2]
				_optimum[cur].backs3 = reps[3]
				val curPrice = _optimum[cur].price

				currentByte = _matchFinder!!.getIndexByte(0 - 1)
				matchByte = _matchFinder!!.getIndexByte(0 - reps[0] - 1 - 1)

				posState = ppos and _posStateMask

				val curAnd1Price = curPrice +
						RangeEncoder.getPrice0(
							_isMatch[(state shl LzmaBase.kNumPosStatesBitsMax) + posState].toInt()
						) +
						_literalEncoder.getSubCoder(
							ppos,
							_matchFinder!!.getIndexByte(0 - 2)
						).getPrice(!LzmaBase.stateIsCharState(state), matchByte, currentByte)

				val nextOptimum = _optimum[cur + 1]

				var nextIsChar = false
				if (curAnd1Price < nextOptimum.price) {
					nextOptimum.price = curAnd1Price
					nextOptimum.posPrev = cur
					nextOptimum.makeAsChar()
					nextIsChar = true
				}

				matchPrice = curPrice +
						RangeEncoder.getPrice1(
							_isMatch[(state shl LzmaBase.kNumPosStatesBitsMax) + posState].toInt()
						)
				repMatchPrice = matchPrice +
						RangeEncoder.getPrice1(
							_isRep[state].toInt()
						)

				if (matchByte == currentByte && !(nextOptimum.posPrev < cur && nextOptimum.backPrev == 0)) {
					val shortRepPrice = repMatchPrice + getRepLen1Price(state, posState)
					if (shortRepPrice <= nextOptimum.price) {
						nextOptimum.price = shortRepPrice
						nextOptimum.posPrev = cur
						nextOptimum.makeAsShortRep()
						nextIsChar = true
					}
				}

				var numAvailableBytesFull = _matchFinder!!.getNumAvailableBytes() + 1
				numAvailableBytesFull = min2(kNumOpts - 1 - cur, numAvailableBytesFull)
				numAvailableBytes = numAvailableBytesFull

				if (numAvailableBytes < 2)
					continue
				if (numAvailableBytes > _numFastBytes)
					numAvailableBytes = _numFastBytes
				if (!nextIsChar && matchByte != currentByte) {
					// try Literal + rep0
					val t = min2(numAvailableBytesFull - 1, _numFastBytes)
					val lenTest2 = _matchFinder!!.getMatchLen(0, reps[0], t)
					if (lenTest2 >= 2) {
						val state2 = LzmaBase.stateUpdateChar(state)

						val posStateNext = ppos + 1 and _posStateMask
						val nextRepMatchPrice = curAnd1Price +
								RangeEncoder.getPrice1(
									_isMatch[(state2 shl LzmaBase.kNumPosStatesBitsMax) + posStateNext].toInt()
								) +
								RangeEncoder.getPrice1(
									_isRep[state2].toInt()
								)
						run {
							val offset = cur + 1 + lenTest2
							while (lenEnd < offset)
								_optimum[++lenEnd].price =
										kIfinityPrice
							val curAndLenPrice = nextRepMatchPrice + getRepPrice(
								0, lenTest2, state2, posStateNext
							)
							val optimum = _optimum[offset]
							if (curAndLenPrice < optimum.price) {
								optimum.price = curAndLenPrice
								optimum.posPrev = cur + 1
								optimum.backPrev = 0
								optimum.prev1IsChar = true
								optimum.prev2 = false
							}
						}
					}
				}

				var startLen = 2 // speed optimization

				for (repIndex in 0 until LzmaBase.kNumRepDistances) {
					var lenTest = _matchFinder!!.getMatchLen(0 - 1, reps[repIndex], numAvailableBytes)
					if (lenTest < 2)
						continue
					val lenTestTemp = lenTest
					do {
						while (lenEnd < cur + lenTest)
							_optimum[++lenEnd].price =
									kIfinityPrice
						val curAndLenPrice = repMatchPrice + getRepPrice(repIndex, lenTest, state, posState)
						val optimum = _optimum[cur + lenTest]
						if (curAndLenPrice < optimum.price) {
							optimum.price = curAndLenPrice
							optimum.posPrev = cur
							optimum.backPrev = repIndex
							optimum.prev1IsChar = false
						}
					} while (--lenTest >= 2)
					lenTest = lenTestTemp

					if (repIndex == 0)
						startLen = lenTest + 1

					// if (_maxMode)
					if (lenTest < numAvailableBytesFull) {
						val t = min2(numAvailableBytesFull - 1 - lenTest, _numFastBytes)
						val lenTest2 = _matchFinder!!.getMatchLen(lenTest, reps[repIndex], t)
						if (lenTest2 >= 2) {
							var state2 =
								LzmaBase.stateUpdateRep(state)

							var posStateNext = ppos + lenTest and _posStateMask
							val curAndLenCharPrice = repMatchPrice + getRepPrice(repIndex, lenTest, state, posState) +
									RangeEncoder.getPrice0(
										_isMatch[(state2 shl LzmaBase.kNumPosStatesBitsMax) + posStateNext].toInt()
									) +
									_literalEncoder.getSubCoder(
										ppos + lenTest,
										_matchFinder!!.getIndexByte(lenTest - 1 - 1)
									).getPrice(
										true,
										_matchFinder!!.getIndexByte(lenTest - 1 - (reps[repIndex] + 1)),
										_matchFinder!!.getIndexByte(lenTest - 1)
									)
							state2 = LzmaBase.stateUpdateChar(state2)
							posStateNext = ppos + lenTest + 1 and _posStateMask
							val nextMatchPrice =
								curAndLenCharPrice + RangeEncoder.getPrice1(
									_isMatch[(state2 shl LzmaBase.kNumPosStatesBitsMax) + posStateNext].toInt()
								)
							val nextRepMatchPrice =
								nextMatchPrice + RangeEncoder.getPrice1(
									_isRep[state2].toInt()
								)

							// for(; lenTest2 >= 2; lenTest2--)
							run {
								val offset = lenTest + 1 + lenTest2
								while (lenEnd < cur + offset)
									_optimum[++lenEnd].price =
											kIfinityPrice
								val curAndLenPrice = nextRepMatchPrice + getRepPrice(0, lenTest2, state2, posStateNext)
								val optimum = _optimum[cur + offset]
								if (curAndLenPrice < optimum.price) {
									optimum.price = curAndLenPrice
									optimum.posPrev = cur + lenTest + 1
									optimum.backPrev = 0
									optimum.prev1IsChar = true
									optimum.prev2 = true
									optimum.posPrev2 = cur
									optimum.backPrev2 = repIndex
								}
							}
						}
					}
				}

				if (newLen > numAvailableBytes) {
					newLen = numAvailableBytes
					numDistancePairs = 0
					while (newLen > _matchDistances[numDistancePairs]) {
						numDistancePairs += 2
					}
					_matchDistances[numDistancePairs] = newLen
					numDistancePairs += 2
				}
				if (newLen >= startLen) {
					normalMatchPrice = matchPrice +
							RangeEncoder.getPrice0(
								_isRep[state].toInt()
							)
					while (lenEnd < cur + newLen)
						_optimum[++lenEnd].price =
								kIfinityPrice

					var offs = 0
					while (startLen > _matchDistances[offs])
						offs += 2

					var lenTest = startLen
					while (true) {
						val curBack = _matchDistances[offs + 1]
						var curAndLenPrice = normalMatchPrice + getPosLenPrice(curBack, lenTest, posState)
						var optimum = _optimum[cur + lenTest]
						if (curAndLenPrice < optimum.price) {
							optimum.price = curAndLenPrice
							optimum.posPrev = cur
							optimum.backPrev = curBack +
									LzmaBase.kNumRepDistances
							optimum.prev1IsChar = false
						}

						if (lenTest == _matchDistances[offs]) {
							if (lenTest < numAvailableBytesFull) {
								val t = min2(numAvailableBytesFull - 1 - lenTest, _numFastBytes)
								val lenTest2 = _matchFinder!!.getMatchLen(lenTest, curBack, t)
								if (lenTest2 >= 2) {
									var state2 =
										LzmaBase.stateUpdateMatch(
											state
										)

									var posStateNext = ppos + lenTest and _posStateMask
									val curAndLenCharPrice = curAndLenPrice +
											RangeEncoder.getPrice0(
												_isMatch[(state2 shl LzmaBase.kNumPosStatesBitsMax) + posStateNext].toInt()
											) +
											_literalEncoder.getSubCoder(
												ppos + lenTest,
												_matchFinder!!.getIndexByte(lenTest - 1 - 1)
											).getPrice(
												true,
												_matchFinder!!.getIndexByte(lenTest - (curBack + 1) - 1),
												_matchFinder!!.getIndexByte(lenTest - 1)
											)
									state2 =
											LzmaBase.stateUpdateChar(
												state2
											)
									posStateNext = ppos + lenTest + 1 and _posStateMask
									val nextMatchPrice =
										curAndLenCharPrice + RangeEncoder.getPrice1(
											_isMatch[(state2 shl LzmaBase.kNumPosStatesBitsMax) + posStateNext].toInt()
										)
									val nextRepMatchPrice =
										nextMatchPrice + RangeEncoder.getPrice1(
											_isRep[state2].toInt()
										)

									val offset = lenTest + 1 + lenTest2
									while (lenEnd < cur + offset)
										_optimum[++lenEnd].price =
												kIfinityPrice
									curAndLenPrice = nextRepMatchPrice + getRepPrice(0, lenTest2, state2, posStateNext)
									optimum = _optimum[cur + offset]
									if (curAndLenPrice < optimum.price) {
										optimum.price = curAndLenPrice
										optimum.posPrev = cur + lenTest + 1
										optimum.backPrev = 0
										optimum.prev1IsChar = true
										optimum.prev2 = true
										optimum.posPrev2 = cur
										optimum.backPrev2 = curBack +
												LzmaBase.kNumRepDistances
									}
								}
							}
							offs += 2
							if (offs == numDistancePairs)
								break
						}
						lenTest++
					}
				}
			}
		}

		//internal fun changePair(smallDist: Int, bigDist: Int): Boolean {
		//	val kDif = 7
		//	return smallDist < 1 shl 32 - kDif && bigDist >= smallDist shl kDif
		//}

		private fun writeEndMarker(posState: Int) {
			if (!_writeEndMark) return
			_rangeEncoder.encode(_isMatch, (_state shl LzmaBase.kNumPosStatesBitsMax) + posState, 1)
			_rangeEncoder.encode(_isRep, _state, 0)
			_state = LzmaBase.stateUpdateMatch(_state)
			val len = LzmaBase.kMatchMinLen
			_lenEncoder.encode(_rangeEncoder, len - LzmaBase.kMatchMinLen, posState)
			val posSlot = (1 shl LzmaBase.kNumPosSlotBits) - 1
			val lenToPosState = LzmaBase.getLenToPosState(len)
			_posSlotEncoder[lenToPosState].encode(_rangeEncoder, posSlot)
			val footerBits = 30
			val posReduced = (1 shl footerBits) - 1
			_rangeEncoder.encodeDirectBits(posReduced shr LzmaBase.kNumAlignBits, footerBits - LzmaBase.kNumAlignBits)
			_posAlignEncoder.reverseEncode(_rangeEncoder, posReduced and LzmaBase.kAlignMask)
		}

		private fun flush(nowPos: Int) {
			releaseMFStream()
			writeEndMarker(nowPos and _posStateMask)
			_rangeEncoder.flushData()
			_rangeEncoder.flushStream()
		}

		private fun codeOneBlock(inSize: LongArray, outSize: LongArray, finished: BooleanArray) {
			inSize[0] = 0
			outSize[0] = 0
			finished[0] = true

			if (_inStream != null) {
				_matchFinder!!.setStream(_inStream!!)
				_matchFinder!!.init()
				_needReleaseMFStream = true
				_inStream = null
			}

			if (_finished)
				return
			_finished = true


			val progressPosValuePrev = nowPos64
			if (nowPos64 == 0L) {
				if (_matchFinder!!.getNumAvailableBytes() == 0) {
					flush(nowPos64.toInt())
					return
				}

				readMatchDistances()
				val posState = nowPos64.toInt() and _posStateMask
				_rangeEncoder.encode(_isMatch, (_state shl LzmaBase.kNumPosStatesBitsMax) + posState, 0)
				_state = LzmaBase.stateUpdateChar(_state)
				val curByte = _matchFinder!!.getIndexByte(0 - _additionalOffset)
				_literalEncoder.getSubCoder(nowPos64.toInt(), _previousByte).encode(_rangeEncoder, curByte)
				_previousByte = curByte
				_additionalOffset--
				nowPos64++
			}
			if (_matchFinder!!.getNumAvailableBytes() == 0) {
				flush(nowPos64.toInt())
				return
			}
			while (true) {

				val len = getOptimum(nowPos64.toInt())
				var pos = backRes
				val posState = nowPos64.toInt() and _posStateMask
				val complexState = (_state shl LzmaBase.kNumPosStatesBitsMax) + posState
				if (len == 1 && pos == -1) {
					_rangeEncoder.encode(_isMatch, complexState, 0)
					val curByte = _matchFinder!!.getIndexByte(0 - _additionalOffset)
					val subCoder = _literalEncoder.getSubCoder(nowPos64.toInt(), _previousByte)
					if (!LzmaBase.stateIsCharState(_state)) {
						val matchByte = _matchFinder!!.getIndexByte(0 - _repDistances[0] - 1 - _additionalOffset)
						subCoder.encodeMatched(_rangeEncoder, matchByte, curByte)
					} else
						subCoder.encode(_rangeEncoder, curByte)
					_previousByte = curByte
					_state = LzmaBase.stateUpdateChar(_state)
				} else {
					_rangeEncoder.encode(_isMatch, complexState, 1)
					if (pos < LzmaBase.kNumRepDistances) {
						_rangeEncoder.encode(_isRep, _state, 1)
						if (pos == 0) {
							_rangeEncoder.encode(_isRepG0, _state, 0)
							if (len == 1)
								_rangeEncoder.encode(_isRep0Long, complexState, 0)
							else
								_rangeEncoder.encode(_isRep0Long, complexState, 1)
						} else {
							_rangeEncoder.encode(_isRepG0, _state, 1)
							if (pos == 1)
								_rangeEncoder.encode(_isRepG1, _state, 0)
							else {
								_rangeEncoder.encode(_isRepG1, _state, 1)
								_rangeEncoder.encode(_isRepG2, _state, pos - 2)
							}
						}
						_state = if (len == 1) {
							LzmaBase.stateUpdateShortRep(_state)
						} else {
							_repMatchLenEncoder.encode(_rangeEncoder, len - LzmaBase.kMatchMinLen, posState)
							LzmaBase.stateUpdateRep(_state)
						}
						val distance = _repDistances[pos]
						if (pos != 0) {
							for (i in pos downTo 1)
								_repDistances[i] = _repDistances[i - 1]
							_repDistances[0] = distance
						}
					} else {
						_rangeEncoder.encode(_isRep, _state, 0)
						_state = LzmaBase.stateUpdateMatch(_state)
						_lenEncoder.encode(_rangeEncoder, len - LzmaBase.kMatchMinLen, posState)
						pos -= LzmaBase.kNumRepDistances
						val posSlot =
							getPosSlot(pos)
						val lenToPosState =
							LzmaBase.getLenToPosState(len)
						_posSlotEncoder[lenToPosState].encode(_rangeEncoder, posSlot)

						if (posSlot >= LzmaBase.kStartPosModelIndex) {
							val footerBits = (posSlot shr 1) - 1
							val baseVal = 2 or (posSlot and 1) shl footerBits
							val posReduced = pos - baseVal

							if (posSlot < LzmaBase.kEndPosModelIndex)
								BitTreeEncoder.reverseEncode(
									_posEncoders,
									baseVal - posSlot - 1, _rangeEncoder, footerBits, posReduced
								)
							else {
								_rangeEncoder.encodeDirectBits(
									posReduced shr LzmaBase.kNumAlignBits,
									footerBits - LzmaBase.kNumAlignBits
								)
								_posAlignEncoder.reverseEncode(_rangeEncoder, posReduced and LzmaBase.kAlignMask)
								_alignPriceCount++
							}
						}
						val distance = pos
						for (i in LzmaBase.kNumRepDistances - 1 downTo 1)
							_repDistances[i] = _repDistances[i - 1]
						_repDistances[0] = distance
						_matchPriceCount++
					}
					_previousByte = _matchFinder!!.getIndexByte(len - 1 - _additionalOffset)
				}
				_additionalOffset -= len
				nowPos64 += len.toLong()
				if (_additionalOffset == 0) {
					// if (!_fastMode)
					if (_matchPriceCount >= 1 shl 7)
						fillDistancesPrices()
					if (_alignPriceCount >= LzmaBase.kAlignTableSize)
						fillAlignPrices()
					inSize[0] = nowPos64
					outSize[0] = _rangeEncoder.getProcessedSizeAdd()
					if (_matchFinder!!.getNumAvailableBytes() == 0) {
						flush(nowPos64.toInt())
						return
					}

					if (nowPos64 - progressPosValuePrev >= 1 shl 12) {
						_finished = false
						finished[0] = false
						return
					}
				}
			}
		}

		private fun releaseMFStream() {
			if (_matchFinder != null && _needReleaseMFStream) {
				_matchFinder!!.releaseStream()
				_needReleaseMFStream = false
			}
		}

		private fun setOutStream(outStream: SyncOutputStream) {
			_rangeEncoder.setStream(outStream)
		}

		private fun releaseOutStream() {
			_rangeEncoder.releaseStream()
		}

		private fun releaseStreams() {
			releaseMFStream()
			releaseOutStream()
		}

		@Suppress("UNUSED_PARAMETER")
		private fun setStreams(inStream: SyncInputStream, outStream: SyncOutputStream, inSize: Long, outSize: Long) {
			_inStream = inStream
			_finished = false
			create()
			setOutStream(outStream)
			init()

			// if (!_fastMode)
			run {
				fillDistancesPrices()
				fillAlignPrices()
			}

			_lenEncoder.setTableSize(_numFastBytes + 1 - LzmaBase.kMatchMinLen)
			_lenEncoder.updateTables(1 shl _posStateBits)
			_repMatchLenEncoder.setTableSize(_numFastBytes + 1 - LzmaBase.kMatchMinLen)
			_repMatchLenEncoder.updateTables(1 shl _posStateBits)

			nowPos64 = 0
		}

		fun code(inStream: SyncInputStream, outStream: SyncOutputStream, inSize: Long, outSize: Long, progress: ICodeProgress?) {
			_needReleaseMFStream = false
			try {
				setStreams(inStream, outStream, inSize, outSize)
				while (true) {
					codeOneBlock(processedInSize, processedOutSize, finished)
					if (finished[0]) return
					progress?.setProgress(processedInSize[0], processedOutSize[0])
				}
			} finally {
				releaseStreams()
			}
		}

		fun writeCoderProperties(outStream: SyncOutputStream) {
			properties[0] = ((_posStateBits * 5 + _numLiteralPosStateBits) * 9 + _numLiteralContextBits).toByte()
			for (i in 0..3)
				properties[1 + i] = (_dictionarySize shr 8 * i).toByte()
			outStream.write(
				properties, 0,
				kPropSize
			)
		}

		private fun fillDistancesPrices() {
			for (i in LzmaBase.kStartPosModelIndex until LzmaBase.kNumFullDistances) {
				val posSlot = getPosSlot(i)
				val footerBits = (posSlot shr 1) - 1
				val baseVal = 2 or (posSlot and 1) shl footerBits
				tempPrices[i] = BitTreeEncoder.reverseGetPrice(_posEncoders, baseVal - posSlot - 1, footerBits, i - baseVal)
			}

			for (lenToPosState in 0 until LzmaBase.kNumLenToPosStates) {
				val encoder = _posSlotEncoder[lenToPosState]

				val st = lenToPosState shl LzmaBase.kNumPosSlotBits
				var posSlot = 0
				while (posSlot < _distTableSize) {
					_posSlotPrices[st + posSlot] = encoder.getPrice(posSlot)
					posSlot++
				}
				posSlot = LzmaBase.kEndPosModelIndex
				while (posSlot < _distTableSize) {
					_posSlotPrices[st + posSlot] += (posSlot shr 1) - 1 - LzmaBase.kNumAlignBits shl RangeEncoder.kNumBitPriceShiftBits
					posSlot++
				}

				val st2 = lenToPosState * LzmaBase.kNumFullDistances
				var i = 0
				while (i < LzmaBase.kStartPosModelIndex) {
					_distancesPrices[st2 + i] = _posSlotPrices[st + i]
					i++
				}
				while (i < LzmaBase.kNumFullDistances) {
					_distancesPrices[st2 + i] = _posSlotPrices[st + getPosSlot(
						i
					)] + tempPrices[i]
					i++
				}
			}
			_matchPriceCount = 0
		}

		private fun fillAlignPrices() {
			for (i in 0 until LzmaBase.kAlignTableSize)
				_alignPrices[i] = _posAlignEncoder.reverseGetPrice(i)
			_alignPriceCount = 0
		}


		@Suppress("UNUSED_PARAMETER")
		fun setAlgorithm(algorithm: Int): Boolean {
			/*
        _fastMode = (algorithm == 0);
        _maxMode = (algorithm >= 2);
        */
			return true
		}

		fun setDictionarySize(dictionarySize: Int): Boolean {
			val kDicLogSizeMaxCompress = 29
			val cond1 = dictionarySize < (1 shl LzmaBase.kDicLogSizeMin)
			val cond2 = dictionarySize > (1 shl kDicLogSizeMaxCompress)
			if (cond1 || cond2)
				return false
			_dictionarySize = dictionarySize
			var dicLogSize = 0
			while (dictionarySize > 1 shl dicLogSize) {
				dicLogSize++
			}
			_distTableSize = dicLogSize * 2
			return true
		}

		fun setNumFastBytes(numFastBytes: Int): Boolean {
			if (numFastBytes < 5 || numFastBytes > LzmaBase.kMatchMaxLen)
				return false
			_numFastBytes = numFastBytes
			return true
		}

		fun setMatchFinder(matchFinderIndex: Int): Boolean {
			if (matchFinderIndex < 0 || matchFinderIndex > 2)
				return false
			val matchFinderIndexPrev = _matchFinderType
			_matchFinderType = matchFinderIndex
			if (_matchFinder != null && matchFinderIndexPrev != _matchFinderType) {
				_dictionarySizePrev = -1
				_matchFinder = null
			}
			return true
		}

		fun setLcLpPb(lc: Int, lp: Int, pb: Int): Boolean {
			if (lp < 0 || lp > LzmaBase.kNumLitPosStatesBitsEncodingMax ||
				lc < 0 || lc > LzmaBase.kNumLitContextBitsMax ||
				pb < 0 || pb > LzmaBase.kNumPosStatesBitsEncodingMax
			)
				return false
			_numLiteralPosStateBits = lp
			_numLiteralContextBits = lc
			_posStateBits = pb
			_posStateMask = (1 shl _posStateBits) - 1
			return true
		}

		fun setEndMarkerMode(endMarkerMode: Boolean) {
			_writeEndMark = endMarkerMode
		}

		companion object {
			const val EMatchFinderTypeBT2 = 0
			const val EMatchFinderTypeBT4 = 1

			internal const val kIfinityPrice = 0xFFFFFFF

			private var g_FastPos = ByteArray(1 shl 11)

			init {
				val kFastSlots = 22
				var c = 2
				g_FastPos[0] = 0
				g_FastPos[1] = 1
				for (slotFast in 2 until kFastSlots) {
					val k = 1 shl (slotFast shr 1) - 1
					var j = 0
					while (j < k) {
						g_FastPos[c] = slotFast.toByte()
						j++
						c++
					}
				}
			}

			internal fun getPosSlot(pos: Int): Int {
				if (pos < 1 shl 11)
					return g_FastPos[pos].toInt()
				return if (pos < 1 shl 21) g_FastPos[pos shr 10] + 20 else g_FastPos[pos shr 20] + 40
			}

			internal fun getPosSlot2(pos: Int): Int {
				if (pos < 1 shl 17)
					return g_FastPos[pos shr 6] + 12
				return if (pos < 1 shl 27) g_FastPos[pos shr 16] + 32 else g_FastPos[pos shr 26] + 52
			}

			internal const val kDefaultDictionaryLogSize = 22
			internal const val kNumFastBytesDefault = 0x20

			//const val kNumLenSpecSymbols = LzmaBase.kNumLowLenSymbols + LzmaBase.kNumMidLenSymbols

			internal const val kNumOpts = 1 shl 12

			const val kPropSize = 5
		}
	}

	class LzBinTree : LzInWindow() {
		private var _cyclicBufferPos: Int = 0
		private var _cyclicBufferSize = 0
		private var _matchMaxLen: Int = 0

		private lateinit var _son: IntArray
		private lateinit var _hash: IntArray

		private var _cutValue = 0xFF
		private var _hashMask: Int = 0
		private var _hashSizeSum = 0

		private var HASH_ARRAY = true

		private var kNumHashDirectBytes = 0
		private var kMinMatchCheck = 4
		private var kFixHashSize = kHash2Size + kHash3Size

		fun setType(numHashBytes: Int) {
			HASH_ARRAY = numHashBytes > 2
			if (HASH_ARRAY) {
				kNumHashDirectBytes = 0
				kMinMatchCheck = 4
				kFixHashSize = kHash2Size +
						kHash3Size
			} else {
				kNumHashDirectBytes = 2
				kMinMatchCheck = 2 + 1
				kFixHashSize = 0
			}
		}


		override fun init() {
			super.init()
			for (i in 0 until _hashSizeSum)
				_hash[i] = kEmptyHashValue
			_cyclicBufferPos = 0
			reduceOffsets(-1)
		}

		override fun movePos() {
			if (++_cyclicBufferPos >= _cyclicBufferSize)
				_cyclicBufferPos = 0
			super.movePos()
			if (_pos == kMaxValForNormalize)
				normalize()
		}


		fun create(historySize: Int, keepAddBufferBefore: Int, matchMaxLen: Int, keepAddBufferAfter: Int): Boolean {
			if (historySize > kMaxValForNormalize - 256) return false
			_cutValue = 16 + (matchMaxLen shr 1)
			val windowReservSize = (historySize + keepAddBufferBefore + matchMaxLen + keepAddBufferAfter) / 2 + 256
			super.create(historySize + keepAddBufferBefore, matchMaxLen + keepAddBufferAfter, windowReservSize)
			_matchMaxLen = matchMaxLen
			val cyclicBufferSize = historySize + 1
			if (_cyclicBufferSize != cyclicBufferSize) _cyclicBufferSize = cyclicBufferSize
			_son = IntArray(_cyclicBufferSize * 2)
			var hs = kBT2HashSize
			if (HASH_ARRAY) {
				hs = historySize - 1
				hs = hs or (hs shr 1)
				hs = hs or (hs shr 2)
				hs = hs or (hs shr 4)
				hs = hs or (hs shr 8)
				hs = hs shr 1
				hs = hs or 0xFFFF
				if (hs > 1 shl 24) hs = hs shr 1
				_hashMask = hs
				hs++
				hs += kFixHashSize
			}
			if (hs != _hashSizeSum) {
				_hashSizeSum = hs
				_hash = IntArray(_hashSizeSum)
			}
			return true
		}

		fun getMatches(distances: IntArray): Int {
			val lenLimit: Int
			if (_pos + _matchMaxLen <= _streamPos) {
				lenLimit = _matchMaxLen
			} else {
				lenLimit = _streamPos - _pos
				if (lenLimit < kMinMatchCheck) {
					movePos()
					return 0
				}
			}

			var offset = 0
			val matchMinPos = if (_pos > _cyclicBufferSize) _pos - _cyclicBufferSize else 0
			val cur = _bufferOffset + _pos
			var maxLen =
				kStartMaxLen // to avoid items for len < hashSize;
			val hashValue: Int
			var hash2Value = 0
			var hash3Value = 0

			if (HASH_ARRAY) {
				var temp = CrcTable[_bufferBase!![cur] and 0xFF] xor (_bufferBase!![cur + 1] and 0xFF)
				hash2Value = temp and kHash2Size - 1
				temp = temp xor ((_bufferBase!![cur + 2] and 0xFF) shl 8)
				hash3Value = temp and kHash3Size - 1
				hashValue = temp xor (CrcTable[_bufferBase!![cur + 3] and 0xFF] shl 5) and _hashMask
			} else
				hashValue = _bufferBase!![cur] and 0xFF xor ((_bufferBase!![cur + 1] and 0xFF) shl 8)

			var curMatch = _hash[kFixHashSize + hashValue]
			if (HASH_ARRAY) {
				var curMatch2 = _hash[hash2Value]
				val curMatch3 = _hash[kHash3Offset + hash3Value]
				_hash[hash2Value] = _pos
				_hash[kHash3Offset + hash3Value] = _pos
				if (curMatch2 > matchMinPos)
					if (_bufferBase!![_bufferOffset + curMatch2] == _bufferBase!![cur]) {
						maxLen = 2
						distances[offset++] = maxLen
						distances[offset++] = _pos - curMatch2 - 1
					}
				if (curMatch3 > matchMinPos)
					if (_bufferBase!![_bufferOffset + curMatch3] == _bufferBase!![cur]) {
						if (curMatch3 == curMatch2)
							offset -= 2
						maxLen = 3
						distances[offset++] = maxLen
						distances[offset++] = _pos - curMatch3 - 1
						curMatch2 = curMatch3
					}
				if (offset != 0 && curMatch2 == curMatch) {
					offset -= 2
					maxLen = kStartMaxLen
				}
			}

			_hash[kFixHashSize + hashValue] = _pos

			var ptr0 = (_cyclicBufferPos shl 1) + 1
			var ptr1 = _cyclicBufferPos shl 1

			var len0: Int
			var len1: Int
			len1 = kNumHashDirectBytes
			len0 = len1

			if (kNumHashDirectBytes != 0) {
				if (curMatch > matchMinPos) {
					if (_bufferBase!![_bufferOffset + curMatch + kNumHashDirectBytes] != _bufferBase!![cur + kNumHashDirectBytes]) {
						maxLen = kNumHashDirectBytes
						distances[offset++] = maxLen
						distances[offset++] = _pos - curMatch - 1
					}
				}
			}

			var count = _cutValue

			while (true) {
				if (curMatch <= matchMinPos || count-- == 0) {
					_son[ptr1] = kEmptyHashValue
					_son[ptr0] = _son[ptr1]
					break
				}
				val delta = _pos - curMatch
				val cyclicPos = (if (delta <= _cyclicBufferPos)
					_cyclicBufferPos - delta
				else
					_cyclicBufferPos - delta + _cyclicBufferSize) shl 1

				val pby1 = _bufferOffset + curMatch
				var len = min2(len0, len1)
				if (_bufferBase!![pby1 + len] == _bufferBase!![cur + len]) {
					while (++len != lenLimit)
						if (_bufferBase!![pby1 + len] != _bufferBase!![cur + len])
							break
					if (maxLen < len) {
						maxLen = len
						distances[offset++] = maxLen
						distances[offset++] = delta - 1
						if (len == lenLimit) {
							_son[ptr1] = _son[cyclicPos]
							_son[ptr0] = _son[cyclicPos + 1]
							break
						}
					}
				}
				if (_bufferBase!![pby1 + len] and 0xFF < _bufferBase!![cur + len] and 0xFF) {
					_son[ptr1] = curMatch
					ptr1 = cyclicPos + 1
					curMatch = _son[ptr1]
					len1 = len
				} else {
					_son[ptr0] = curMatch
					ptr0 = cyclicPos
					curMatch = _son[ptr0]
					len0 = len
				}
			}
			movePos()
			return offset
		}

		fun skip(num: Int) {
			var nnum = num
			do {
				val lenLimit: Int
				if (_pos + _matchMaxLen <= _streamPos)
					lenLimit = _matchMaxLen
				else {
					lenLimit = _streamPos - _pos
					if (lenLimit < kMinMatchCheck) {
						movePos()
						continue
					}
				}

				val matchMinPos = if (_pos > _cyclicBufferSize) _pos - _cyclicBufferSize else 0
				val cur = _bufferOffset + _pos

				val hashValue: Int

				if (HASH_ARRAY) {
					var temp = CrcTable[_bufferBase!![cur] and 0xFF] xor (_bufferBase!![cur + 1] and 0xFF)
					val hash2Value = temp and kHash2Size - 1
					_hash[hash2Value] = _pos
					temp = temp xor ((_bufferBase!![cur + 2] and 0xFF) shl 8)
					val hash3Value = temp and kHash3Size - 1
					_hash[kHash3Offset + hash3Value] = _pos
					hashValue = temp xor (CrcTable[_bufferBase!![cur + 3] and 0xFF] shl 5) and _hashMask
				} else
					hashValue = _bufferBase!![cur] and 0xFF xor ((_bufferBase!![cur + 1] and 0xFF) shl 8)

				var curMatch = _hash[kFixHashSize + hashValue]
				_hash[kFixHashSize + hashValue] = _pos

				var ptr0 = (_cyclicBufferPos shl 1) + 1
				var ptr1 = _cyclicBufferPos shl 1

				var len0: Int
				var len1: Int
				len1 = kNumHashDirectBytes
				len0 = len1

				var count = _cutValue
				while (true) {
					if (curMatch <= matchMinPos || count-- == 0) {
						_son[ptr1] =
								kEmptyHashValue
						_son[ptr0] = _son[ptr1]
						break
					}

					val delta = _pos - curMatch
					val cyclicPos = (if (delta <= _cyclicBufferPos)
						_cyclicBufferPos - delta
					else
						_cyclicBufferPos - delta + _cyclicBufferSize) shl 1

					val pby1 = _bufferOffset + curMatch
					var len = min2(len0, len1)
					if (_bufferBase!![pby1 + len] == _bufferBase!![cur + len]) {
						while (++len != lenLimit)
							if (_bufferBase!![pby1 + len] != _bufferBase!![cur + len])
								break
						if (len == lenLimit) {
							_son[ptr1] = _son[cyclicPos]
							_son[ptr0] = _son[cyclicPos + 1]
							break
						}
					}
					if (_bufferBase!![pby1 + len] and 0xFF < _bufferBase!![cur + len] and 0xFF) {
						_son[ptr1] = curMatch
						ptr1 = cyclicPos + 1
						curMatch = _son[ptr1]
						len1 = len
					} else {
						_son[ptr0] = curMatch
						ptr0 = cyclicPos
						curMatch = _son[ptr0]
						len0 = len
					}
				}
				movePos()
			} while (--nnum != 0)
		}

		private fun normalizeLinks(items: IntArray, numItems: Int, subValue: Int) {
			for (i in 0 until numItems) {
				var value = items[i]
				if (value <= subValue)
					value = kEmptyHashValue
				else
					value -= subValue
				items[i] = value
			}
		}

		private fun normalize() {
			val subValue = _pos - _cyclicBufferSize
			normalizeLinks(_son, _cyclicBufferSize * 2, subValue)
			normalizeLinks(_hash, _hashSizeSum, subValue)
			reduceOffsets(subValue)
		}

		//fun setCutValue(cutValue: Int) = run { _cutValue = cutValue }

		companion object {
			internal const val kHash2Size = 1 shl 10
			internal const val kHash3Size = 1 shl 16
			internal const val kBT2HashSize = 1 shl 16
			internal const val kStartMaxLen = 1
			internal const val kHash3Offset = kHash2Size
			internal const val kEmptyHashValue = 0
			internal const val kMaxValForNormalize = (1 shl 30) - 1

			private val CrcTable = CRC32.TABLE
		}
	}

	open class LzInWindow {
		var _bufferBase: ByteArray? = null // pointer to buffer with data
		private var _stream: SyncInputStream? = null
		private var _posLimit: Int = 0  // offset (from _buffer) of first byte when new block reading must be done
		private var _streamEndWasReached: Boolean = false // if (true) then _streamPos shows real end of stream

		private var _pointerToLastSafePosition: Int = 0

		var _bufferOffset: Int = 0

		private var _blockSize: Int = 0  // Size of Allocated memory block
		var _pos: Int = 0             // offset (from _buffer) of curent byte
		private var _keepSizeBefore: Int = 0  // how many BYTEs must be kept in buffer before _pos
		private var _keepSizeAfter: Int = 0   // how many BYTEs must be kept buffer after _pos
		var _streamPos: Int = 0   // offset (from _buffer) of first not read byte from Stream

		private fun moveBlock() {
			var offset = _bufferOffset + _pos - _keepSizeBefore
			// we need one additional byte, since MovePos moves on 1 byte.
			if (offset > 0) offset--
			val numBytes = _bufferOffset + _streamPos - offset
			// check negative offset ????
			for (i in 0 until numBytes) _bufferBase!![i] = _bufferBase!![offset + i]
			_bufferOffset -= offset
		}

		private fun readBlock() {
			if (_streamEndWasReached) return
			while (true) {
				val size = 0 - _bufferOffset + _blockSize - _streamPos
				if (size == 0)
					return
				val numReadBytes = _stream!!.read(_bufferBase!!, _bufferOffset + _streamPos, size)
				if (numReadBytes <= 0) {
					_posLimit = _streamPos
					val pointerToPostion = _bufferOffset + _posLimit
					if (pointerToPostion > _pointerToLastSafePosition)
						_posLimit = _pointerToLastSafePosition - _bufferOffset

					_streamEndWasReached = true
					return
				}
				_streamPos += numReadBytes
				if (_streamPos >= _pos + _keepSizeAfter)
					_posLimit = _streamPos - _keepSizeAfter
			}
		}

		private fun free() {
			_bufferBase = null
		}

		fun create(keepSizeBefore: Int, keepSizeAfter: Int, keepSizeReserv: Int) {
			_keepSizeBefore = keepSizeBefore
			_keepSizeAfter = keepSizeAfter
			val blockSize = keepSizeBefore + keepSizeAfter + keepSizeReserv
			if (_bufferBase == null || _blockSize != blockSize) {
				free()
				_blockSize = blockSize
				_bufferBase = ByteArray(_blockSize)
			}
			_pointerToLastSafePosition = _blockSize - keepSizeAfter
		}

		fun setStream(stream: SyncInputStream) {
			_stream = stream
		}

		fun releaseStream() {
			_stream = null
		}

		open fun init() {
			_bufferOffset = 0
			_pos = 0
			_streamPos = 0
			_streamEndWasReached = false
			readBlock()
		}

		open fun movePos() {
			_pos++
			if (_pos > _posLimit) {
				val pointerToPostion = _bufferOffset + _pos
				if (pointerToPostion > _pointerToLastSafePosition) moveBlock()
				readBlock()
			}
		}

		fun getIndexByte(index: Int): Byte = _bufferBase!![_bufferOffset + _pos + index]

		// index + limit have not to exceed _keepSizeAfter;
		fun getMatchLen(index: Int, distance: Int, limit: Int): Int {
			var ddis = distance
			var dlim = limit
			if (_streamEndWasReached && _pos + index + dlim > _streamPos) dlim = _streamPos - (_pos + index)
			ddis++
			// Byte *pby = _buffer + (size_t)_pos + index;
			val pby = _bufferOffset + _pos + index

			var i = 0
			while (i < dlim && _bufferBase!![pby + i] == _bufferBase!![pby + i - ddis]) i++
			return i
		}

		fun getNumAvailableBytes(): Int {
			return _streamPos - _pos
		}

		fun reduceOffsets(subValue: Int) {
			_bufferOffset += subValue
			_posLimit -= subValue
			_pos -= subValue
			_streamPos -= subValue
		}
	}

	class LzOutWindow {
		private var _buffer: ByteArray? = null
		private var _pos: Int = 0
		private var _windowSize = 0
		private var _streamPos: Int = 0
		private var _stream: SyncOutputStream? = null

		fun create(windowSize: Int) {
			if (_buffer == null || _windowSize != windowSize)
				_buffer = ByteArray(windowSize)
			_windowSize = windowSize
			_pos = 0
			_streamPos = 0
		}

		fun setStream(stream: SyncOutputStream) {
			releaseStream()
			_stream = stream
		}

		fun releaseStream() {
			flush()
			_stream = null
		}

		fun init(solid: Boolean) {
			if (!solid) {
				_streamPos = 0
				_pos = 0
			}
		}

		fun flush() {
			val size = _pos - _streamPos
			if (size == 0) return
			_stream!!.write(_buffer!!, _streamPos, size)
			if (_pos >= _windowSize) _pos = 0
			_streamPos = _pos
		}

		fun copyBlock(distance: Int, len: Int) {
			var llen = len
			var pos = _pos - distance - 1
			if (pos < 0)
				pos += _windowSize
			while (llen != 0) {
				if (pos >= _windowSize) pos = 0
				_buffer!![_pos++] = _buffer!![pos++]
				if (_pos >= _windowSize) flush()
				llen--
			}
		}

		fun putByte(b: Byte) {
			_buffer!![_pos++] = b
			if (_pos >= _windowSize)
				flush()
		}

		fun getByte(distance: Int): Byte {
			var pos = _pos - distance - 1
			if (pos < 0) pos += _windowSize
			return _buffer!![pos]
		}
	}
}

private infix fun Byte.and(mask: Int): Int = this.toInt() and mask
private infix fun Byte.shl(that: Int): Int = this.toInt() shl that
private infix fun Byte.shr(that: Int): Int = this.toInt() shr that
