package com.soywiz.korim.format.jpg

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import kotlin.math.*

// https://github.com/eugeneware/jpeg-js/blob/652bfced3ead53808285b1b5fa9c0b589d00bbf0/lib/decoder.js

/*
   Copyright 2011 notmasteryet

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

// - The JPEG specification can be found in the ITU CCITT Recommendation T.81
//   (www.w3.org/Graphics/JPEG/itu-t81.pdf)
// - The JFIF specification can be found in the JPEG File Interchange Format
//   (www.w3.org/Graphics/JPEG/jfif3.pdf)
// - The Adobe Application-Specific JPEG markers in the Supporting the DCT Filters
//   in PostScript Level 2, Technical Note #5116
//   (partners.adobe.com/public/developer/en/ps/sdk/5116.DCT_Filter.pdf)

class JPEGDecoder {
	private var dctZigZag = intArrayOf(
		0,
		1, 8,
		16, 9, 2,
		3, 10, 17, 24,
		32, 25, 18, 11, 4,
		5, 12, 19, 26, 33, 40,
		48, 41, 34, 27, 20, 13, 6,
		7, 14, 21, 28, 35, 42, 49, 56,
		57, 50, 43, 36, 29, 22, 15,
		23, 30, 37, 44, 51, 58,
		59, 52, 45, 38, 31,
		39, 46, 53, 60,
		61, 54, 47,
		55, 62,
		63
	)

	private var dctCos1 = 4017     // cos(pi/16)
	private var dctSin1 = 799      // sin(pi/16)
	private var dctCos3 = 3406     // cos(3*pi/16)
	private var dctSin3 = 2276     // sin(3*pi/16)
	private var dctCos6 = 1567     // cos(6*pi/16)
	private var dctSin6 = 3784     // sin(6*pi/16)
	private var dctSqrt2 = 5793    // sqrt(2)
	private var dctSqrt1d2 = 2896  // sqrt(2) / 2

	@Suppress("UNUSED_PARAMETER")
	class Jfif(
		versionMajor: Int,
		versionMinor: Int,
		densityUnits: Int,
		xDensity: Int,
		yDensity: Int,
		thumbWidth: Int,
		thumbHeight: Int,
		thumbData: UByteArrayInt
	)

	@Suppress("unused")
	class Adobe(
		val version: Int,
		val flags0: Int,
		val flags1: Int,
		val transformCode: Boolean
	)

	private var width: Int = 0
	private var height: Int = 0
	private var jfif: Jfif? = null
	private var adobe: Adobe? = null
	private var components = arrayListOf<Component>()
	private var colorTransform: Boolean? = null

	private fun mceil(v: Float): Int = ceil(v).toInt()

	data class HuffmanNode(var children: ArrayList<Any> = arrayListOf(), var index: Int = 0) {
		fun setChildAt(index: Int, value: Any) {
			while (children.size <= index) children.add(Unit) // Todo use other thing?
			children[index] = value
		}
	}

	private fun buildHuffmanTable(codeLengths: UByteArrayInt, values: UByteArrayInt): List<Any> {
		var k = 0
		val code = arrayListOf<HuffmanNode>()
		var length = 16
		while (length > 0 && (0 == codeLengths[length - 1])) length--
		code.add(HuffmanNode())
		var p: HuffmanNode = code[0]
		var q: HuffmanNode
		for (i in 0 until length) {
			for (j in 0 until codeLengths[i]) {
				p = code.removeAt(code.size - 1)
				p.setChildAt(p.index, values[k])
				while (p.index > 0) {
					p = code.removeAt(code.size - 1)
				}
				p.index++
				code.add(p)
				while (code.size <= i) {
					q = HuffmanNode()
					code.add(q)
					p.setChildAt(p.index, q.children)
					p = q
				}
				k++
			}
			if (i + 1 < length) {
				// p here points to last code
				q = HuffmanNode()
				code.add(q)
				p.setChildAt(p.index, q.children)
				p = q
			}
		}
		return code[0].children
	}

	private fun decodeScan(
		data: UByteArrayInt, offset: Int,
		frame: Frame, components: List<FrameComponent>, resetInterval: Int,
		spectralStart: Int, spectralEnd: Int,
		successivePrev: Int, successive: Int
	): Int {
		@Suppress("NAME_SHADOWING")
		var resetInterval = resetInterval
		@Suppress("NAME_SHADOWING")
		var offset = offset
		val mcusPerLine = frame.mcusPerLine
		val progressive = frame.progressive
		//var precision = frame.precision
		//var samplesPerLine = frame.samplesPerLine
		//var scanLines = frame.scanLines
		//var maxH = frame.maxH
		//var maxV = frame.maxV

		val startOffset = offset
		var bitsData = 0
		var bitsCount = 0

		fun readBit(): Int {
			if (bitsCount > 0) {
				bitsCount--
				return (bitsData shr bitsCount) and 1
			}
			bitsData = data[offset++]
			if (bitsData == 0xFF) {
				val nextByte = data[offset++]
				if (nextByte != 0) {
					invalidOp("unexpected marker: " + ((bitsData shl 8) or nextByte).toString(16))
				}
				// unstuff 0
			}
			bitsCount = 7
			return bitsData ushr 7
		}

		fun decodeHuffman(tree: List<Any>): Int {
			var node: List<Any> = tree
			while (true) {
				val bit = readBit()
				val res = node[bit]
				@Suppress("UNCHECKED_CAST")
				when (res) {
					is Int -> return res
					is List<*> -> node = res as List<Any>
					else -> invalidOp("invalid huffman sequence")
				}
			}
		}

		fun receive(length: Int): Int {
			var len = length
			var n = 0
			while (len > 0) {
				val bit = readBit()
				n = (n shl 1) or bit
				len--
			}
			return n
		}

		fun receiveAndExtend(length: Int): Int {
			val n = receive(length)
			if (n >= (1 shl (length - 1))) return n
			return n + (-1 shl length) + 1
		}

		fun decodeBaseline(component: FrameComponent, zz: IntArray) {
			val t = decodeHuffman(component.huffmanTableDC)
			val diff = if (t == 0) 0 else receiveAndExtend(t)
			component.pred += diff
			zz[0] = component.pred
			var k = 1
			while (k < 64) {
				val rs = decodeHuffman(component.huffmanTableAC)
				val s = rs and 15
				val r = rs shr 4
				if (s == 0) {
					if (r < 15) break
					k += 16
					continue
				}
				k += r
				val z = dctZigZag[k]
				zz[z] = receiveAndExtend(s)
				k++
			}
		}

		fun decodeDCFirst(component: FrameComponent, zz: IntArray) {
			val t = decodeHuffman(component.huffmanTableDC)
			val diff = if (t == 0) 0 else (receiveAndExtend(t) shl successive)
			component.pred += diff
			zz[0] = component.pred
		}

		fun decodeDCSuccessive(@Suppress("UNUSED_PARAMETER") component: FrameComponent, zz: IntArray) {
			zz[0] = zz[0] or (readBit() shl successive)
		}

		var eobrun = 0
		fun decodeACFirst(component: FrameComponent, zz: IntArray) {
			if (eobrun > 0) {
				eobrun--
				return
			}
			var k = spectralStart
			@Suppress("UnnecessaryVariable")
			val e = spectralEnd
			while (k <= e) {
				val rs = decodeHuffman(component.huffmanTableAC)
				val s = rs and 15
				val r = rs shr 4
				if (s == 0) {
					if (r < 15) {
						eobrun = receive(r) + (1 shl r) - 1
						break
					}
					k += 16
					continue
				}
				k += r
				val z = dctZigZag[k]
				zz[z] = receiveAndExtend(s) * (1 shl successive)
				k++
			}
		}

		var successiveACState = 0
		var successiveACNextValue = 0
		fun decodeACSuccessive(component: FrameComponent, zz: IntArray) {
			var k = spectralStart
			@Suppress("UnnecessaryVariable")
			val e = spectralEnd
			var r = 0
			loop@ while (k <= e) {
				val z = dctZigZag[k]
				val direction = if (zz[z] < 0) -1 else 1
				when (successiveACState) {
					0 -> {// initial state
						val rs = decodeHuffman(component.huffmanTableAC)
						val s = rs and 15
						r = rs shr 4
						if (s == 0) {
							if (r < 15) {
								eobrun = receive(r) + (1 shl r)
								successiveACState = 4
							} else {
								r = 16
								successiveACState = 1
							}
						} else {
							if (s != 1) invalidOp("invalid ACn encoding")
							successiveACNextValue = receiveAndExtend(s)
							successiveACState = if (r != 0) 2 else 3
						}
						continue@loop
					}
					1, 2 -> { // skipping r zero items
						if (zz[z] != 0)
							zz[z] += (readBit() shl successive) * direction
						else {
							r--
							if (r == 0) successiveACState = if (successiveACState == 2) 3 else 0
						}
					}
					3 -> { // set value for a zero item
						if (zz[z] != 0)
							zz[z] += (readBit() shl successive) * direction
						else {
							zz[z] = successiveACNextValue shl successive
							successiveACState = 0
						}
					}
					4 -> { // eob
						if (zz[z] != 0)
							zz[z] += (readBit() shl successive) * direction
					}
				}
				k++
			}
			if (successiveACState == 4) {
				eobrun--
				if (eobrun == 0)
					successiveACState = 0
			}
		}

		fun decodeMcu(
			component: FrameComponent,
			decode: (FrameComponent, IntArray) -> Unit,
			mcu: Int,
			row: Int,
			col: Int
		) {
			val mcuRow = (mcu / mcusPerLine) or 0
			val mcuCol = mcu % mcusPerLine
			val blockRow = mcuRow * component.v + row
			val blockCol = mcuCol * component.h + col
			decode(component, component.blocks[blockRow][blockCol])
		}

		fun decodeBlock(component: FrameComponent, decode: (FrameComponent, IntArray) -> Unit, mcu: Int) {
			val blockRow = (mcu / component.blocksPerLine) or 0
			val blockCol = mcu % component.blocksPerLine
			decode(component, component.blocks[blockRow][blockCol])
		}

		val componentsLength = components.size
		var component: FrameComponent
		val decodeFn = if (progressive) {
			if (spectralStart == 0)
				if (successivePrev == 0) ::decodeDCFirst else ::decodeDCSuccessive
			else
				if (successivePrev == 0) ::decodeACFirst else ::decodeACSuccessive
		} else {
			::decodeBaseline
		}

		var mcu = 0
		val mcuExpected = if (componentsLength == 1) {
			components[0].blocksPerLine * components[0].blocksPerColumn
		} else {
			mcusPerLine * frame.mcusPerColumn
		}
		if (resetInterval == 0) {
			resetInterval = mcuExpected
		}

		var h: Int
		var v: Int
		while (mcu < mcuExpected) {
			// reset interval stuff
			for (i in 0 until componentsLength) components[i].pred = 0
			eobrun = 0

			if (componentsLength == 1) {
				component = components[0]
				for (n in 0 until resetInterval) {
					decodeBlock(component, decodeFn, mcu)
					mcu++
				}
			} else {
				for (n in 0 until resetInterval) {
					for (i in 0 until componentsLength) {
						component = components[i]
						h = component.h
						v = component.v
						for (j in 0 until v) {
							for (k in 0 until h) {
								decodeMcu(component, decodeFn, mcu, j, k)
							}
						}
					}
					mcu++

					// If we've reached our expected MCU's, stop decoding
					if (mcu == mcuExpected) break
				}
			}

			// find marker
			bitsCount = 0
			val marker = (data[offset] shl 8) or data[offset + 1]
			if (marker < 0xFF00) {
				invalidOp("marker was not found")
			}

			if (marker in 0xFFD0..0xFFD7) { // RSTx
				offset += 2
			} else {
				break
			}
		}

		return offset - startOffset
	}

	private fun buildComponentData(
		@Suppress("UNUSED_PARAMETER") frame: Frame,
		component: FrameComponent
	): List<UByteArrayInt> {
		val lines = arrayListOf<UByteArrayInt>()
		val blocksPerLine = component.blocksPerLine
		val blocksPerColumn = component.blocksPerColumn
		val samplesPerLine = blocksPerLine shl 3
		val rr = IntArray(64)
		val r = UByteArrayInt(64)

		// A port of poppler's IDCT method which in turn is taken from:
		//   Christoph Loeffler, Adriaan Ligtenberg, George S. Moschytz,
		//   "Practical Fast 1-D DCT Algorithms with 11 Multiplications",
		//   IEEE Intl. Conf. on Acoustics, Speech and Signal Processing, 1989,
		//   988-991.
		fun quantizeAndInverse(zz: IntArray, dataOut: UByteArrayInt, dataIn: IntArray) {
			val qt = component.quantizationTable
			@Suppress("UnnecessaryVariable")
			val p = dataIn

			// dequant
			for (i in 0 until 64) p[i] = zz[i] * qt[i]

			// inverse DCT on rows
			for (i in 0 until 8) {
				val row = 8 * i

				// check for all-zero AC coefficients
				if (p[1 + row] == 0 && p[2 + row] == 0 && p[3 + row] == 0 &&
					p[4 + row] == 0 && p[5 + row] == 0 && p[6 + row] == 0 &&
					p[7 + row] == 0
				) {
					val t = (dctSqrt2 * p[0 + row] + 512) shr 10
					p[0 + row] = t
					p[1 + row] = t
					p[2 + row] = t
					p[3 + row] = t
					p[4 + row] = t
					p[5 + row] = t
					p[6 + row] = t
					p[7 + row] = t
					continue
				}

				// stage 4
				var v0 = (dctSqrt2 * p[0 + row] + 128) shr 8
				var v1 = (dctSqrt2 * p[4 + row] + 128) shr 8
				var v2 = p[2 + row]
				var v3 = p[6 + row]
				var v4 = (dctSqrt1d2 * (p[1 + row] - p[7 + row]) + 128) shr 8
				var v7 = (dctSqrt1d2 * (p[1 + row] + p[7 + row]) + 128) shr 8
				var v5 = p[3 + row] shl 4
				var v6 = p[5 + row] shl 4

				// stage 3
				var t = (v0 - v1 + 1) shr 1
				v0 = (v0 + v1 + 1) shr 1
				v1 = t
				t = (v2 * dctSin6 + v3 * dctCos6 + 128) shr 8
				v2 = (v2 * dctCos6 - v3 * dctSin6 + 128) shr 8
				v3 = t
				t = (v4 - v6 + 1) shr 1
				v4 = (v4 + v6 + 1) shr 1
				v6 = t
				t = (v7 + v5 + 1) shr 1
				v5 = (v7 - v5 + 1) shr 1
				v7 = t

				// stage 2
				t = (v0 - v3 + 1) shr 1
				v0 = (v0 + v3 + 1) shr 1
				v3 = t
				t = (v1 - v2 + 1) shr 1
				v1 = (v1 + v2 + 1) shr 1
				v2 = t
				t = (v4 * dctSin3 + v7 * dctCos3 + 2048) shr 12
				v4 = (v4 * dctCos3 - v7 * dctSin3 + 2048) shr 12
				v7 = t
				t = (v5 * dctSin1 + v6 * dctCos1 + 2048) shr 12
				v5 = (v5 * dctCos1 - v6 * dctSin1 + 2048) shr 12
				v6 = t

				// stage 1
				p[0 + row] = v0 + v7
				p[7 + row] = v0 - v7
				p[1 + row] = v1 + v6
				p[6 + row] = v1 - v6
				p[2 + row] = v2 + v5
				p[5 + row] = v2 - v5
				p[3 + row] = v3 + v4
				p[4 + row] = v3 - v4
			}

			// inverse DCT on columns
			for (col in 0 until 8) {
				// check for all-zero AC coefficients
				if (p[1 * 8 + col] == 0 && p[2 * 8 + col] == 0 && p[3 * 8 + col] == 0 &&
					p[4 * 8 + col] == 0 && p[5 * 8 + col] == 0 && p[6 * 8 + col] == 0 &&
					p[7 * 8 + col] == 0
				) {
					val t = (dctSqrt2 * dataIn[col + 0] + 8192) shr 14
					p[0 * 8 + col] = t
					p[1 * 8 + col] = t
					p[2 * 8 + col] = t
					p[3 * 8 + col] = t
					p[4 * 8 + col] = t
					p[5 * 8 + col] = t
					p[6 * 8 + col] = t
					p[7 * 8 + col] = t
					continue
				}

				// stage 4
				var v0 = (dctSqrt2 * p[0 * 8 + col] + 2048) shr 12
				var v1 = (dctSqrt2 * p[4 * 8 + col] + 2048) shr 12
				var v2 = p[2 * 8 + col]
				var v3 = p[6 * 8 + col]
				var v4 = (dctSqrt1d2 * (p[1 * 8 + col] - p[7 * 8 + col]) + 2048) shr 12
				var v7 = (dctSqrt1d2 * (p[1 * 8 + col] + p[7 * 8 + col]) + 2048) shr 12
				var v5 = p[3 * 8 + col]
				var v6 = p[5 * 8 + col]

				// stage 3
				var t = (v0 - v1 + 1) shr 1
				v0 = (v0 + v1 + 1) shr 1
				v1 = t
				t = (v2 * dctSin6 + v3 * dctCos6 + 2048) shr 12
				v2 = (v2 * dctCos6 - v3 * dctSin6 + 2048) shr 12
				v3 = t
				t = (v4 - v6 + 1) shr 1
				v4 = (v4 + v6 + 1) shr 1
				v6 = t
				t = (v7 + v5 + 1) shr 1
				v5 = (v7 - v5 + 1) shr 1
				v7 = t

				// stage 2
				t = (v0 - v3 + 1) shr 1
				v0 = (v0 + v3 + 1) shr 1
				v3 = t
				t = (v1 - v2 + 1) shr 1
				v1 = (v1 + v2 + 1) shr 1
				v2 = t
				t = (v4 * dctSin3 + v7 * dctCos3 + 2048) shr 12
				v4 = (v4 * dctCos3 - v7 * dctSin3 + 2048) shr 12
				v7 = t
				t = (v5 * dctSin1 + v6 * dctCos1 + 2048) shr 12
				v5 = (v5 * dctCos1 - v6 * dctSin1 + 2048) shr 12
				v6 = t

				// stage 1
				p[0 * 8 + col] = v0 + v7
				p[7 * 8 + col] = v0 - v7
				p[1 * 8 + col] = v1 + v6
				p[6 * 8 + col] = v1 - v6
				p[2 * 8 + col] = v2 + v5
				p[5 * 8 + col] = v2 - v5
				p[3 * 8 + col] = v3 + v4
				p[4 * 8 + col] = v3 - v4
			}

			// convert to 8-bit integers
			for (i in 0 until 64) {
				val sample = 128 + ((p[i] + 8) shr 4)
				dataOut[i] = if (sample < 0) 0 else if (sample > 0xFF) 0xFF else sample
			}
		}

		for (blockRow in 0 until blocksPerColumn) {
			val scanLine = blockRow shl 3
			for (i in 0 until 8)
				lines.add(UByteArrayInt(samplesPerLine))
			for (blockCol in 0 until blocksPerLine) {
				quantizeAndInverse(component.blocks[blockRow][blockCol], r, rr)

				var offset = 0
				val sample = blockCol shl 3
				for (j in 0 until 8) {
					val line = lines[scanLine + j]
					for (i in 0 until 8)
						line[sample + i] = r[offset++]
				}
			}
		}
		return lines
	}

	private fun clampTo8bit(a: Int): Int = if (a < 0) 0 else if (a > 255) 255 else a
	//private fun clampTo8bit(a: Float): Float = if (a < 0f) 0f else if (a > 255f) 255f else a

	private fun UByteArrayInt.subarray(from: Int, to: Int) = UByteArrayInt(this.asByteArray().copyOfRange(from, to))

	class FrameComponent(
		val h: Int,
		val v: Int,
		var quantizationIdx: Int
	) {
		var huffmanTableDC: List<Any> = emptyList()
		var huffmanTableAC: List<Any> = emptyList()
		var quantizationTable: IntArray = IntArray(0)
		var pred: Int = 0
		var blocksPerLine: Int = 0
		var blocksPerColumn: Int = 0
		var blocks = emptyList<List<IntArray>>()
	}

	@Suppress("unused")
	class Frame(
		var extended: Boolean,
		var progressive: Boolean,
		var precision: Int,
		var scanLines: Int,
		var samplesPerLine: Int,
		var components: ArrayList<FrameComponent>,
		var componentsOrder: ArrayList<Int>,
		var maxH: Int = 0,
		var maxV: Int = 0
	) {
		var mcusPerLine: Int = 0
		var mcusPerColumn: Int = 0
	}

	fun parse(data: UByteArrayInt) {
		var offset = 0
		//var length = data.size
		fun readUint16(): Int {
			val value = (data[offset] shl 8) or data[offset + 1]
			offset += 2
			return value
		}

		fun readDataBlock(): UByteArrayInt {
			val len = readUint16()
			val array = UByteArrayInt(data.asByteArray().copyOfRange(offset, offset + len - 2))
			offset += array.size
			return array
		}

		fun prepareComponents(frame: Frame) {
			var maxH = 0
			var maxV = 0
			for (component in frame.components) {
				if (maxH < component.h) maxH = component.h
				if (maxV < component.v) maxV = component.v
			}
			val mcusPerLine = mceil(frame.samplesPerLine.toFloat() / 8f / maxH.toFloat())
			val mcusPerColumn = mceil(frame.scanLines.toFloat() / 8f / maxV.toFloat())
			for (component in frame.components) {
				val blocksPerLine =
					mceil(mceil(frame.samplesPerLine.toFloat() / 8f) * component.h.toFloat() / maxH.toFloat())
				val blocksPerColumn =
					mceil(mceil(frame.scanLines.toFloat() / 8f) * component.v.toFloat() / maxV.toFloat())
				val blocksPerLineForMcu = mcusPerLine * component.h
				val blocksPerColumnForMcu = mcusPerColumn * component.v
				val blocks = arrayListOf<ArrayList<IntArray>>()
				for (i in 0 until blocksPerColumnForMcu) {
					val row = arrayListOf<IntArray>()
					for (j in 0 until blocksPerLineForMcu)
						row.add(IntArray(64))
					blocks.add(row)
				}
				component.blocksPerLine = blocksPerLine
				component.blocksPerColumn = blocksPerColumn
				component.blocks = blocks
			}
			frame.maxH = maxH
			frame.maxV = maxV
			frame.mcusPerLine = mcusPerLine
			frame.mcusPerColumn = mcusPerColumn
		}

		var jfif: Jfif? = null
		var adobe: Adobe? = null
		//var pixels = null
		lateinit var frame: Frame
		var resetInterval = 0
		val quantizationTables = ArrayList((0 until 16).map { IntArray(0) })
		val frames = arrayListOf<Frame>()
		val huffmanTablesAC = ArrayList((0 until 16).map { emptyList<Any>() })
		val huffmanTablesDC = ArrayList((0 until 16).map { emptyList<Any>() })
		var fileMarker = readUint16()
		if (fileMarker != 0xFFD8) { // SOI (Start of Image)
			invalidOp("SOI not found")
		}

		fileMarker = readUint16()
		while (fileMarker != 0xFFD9) { // EOI (End of image)
			when (fileMarker) {
				0xFF00 -> Unit
			// APP0-15 (Application Specific), COM (Comment)
				0xFFE0, 0xFFE1, 0xFFE2, 0xFFE3, 0xFFE4, 0xFFE5, 0xFFE6, 0xFFE7, 0xFFE8,
				0xFFE9, 0xFFEA, 0xFFEB, 0xFFEC, 0xFFED, 0xFFEE, 0xFFEF, 0xFFFE -> {
					val appData = readDataBlock()

					if (fileMarker == 0xFFE0) {
						if (appData[0] == 0x4A && appData[1] == 0x46 && appData[2] == 0x49 &&
							appData[3] == 0x46 && appData[4] == 0
						) { // 'JFIF\x00'
							jfif = Jfif(
								versionMajor = appData[5],
								versionMinor = appData[6],
								densityUnits = appData[7],
								xDensity = (appData[8] shl 8) or appData[9],
								yDensity = (appData[10] shl 8) or appData[11],
								thumbWidth = appData[12],
								thumbHeight = appData[13],
								thumbData = appData.subarray(14, 14 + 3 * appData[12] * appData[13])
							)
						}
					}
					// TODO APP1 - Exif
					if (fileMarker == 0xFFEE) {
						if (appData[0] == 0x41 && appData[1] == 0x64 && appData[2] == 0x6F &&
							appData[3] == 0x62 && appData[4] == 0x65 && appData[5] == 0
						) { // 'Adobe\x00'
							adobe = Adobe(
								version = appData[6],
								flags0 = (appData[7] shl 8) or appData[8],
								flags1 = (appData[9] shl 8) or appData[10],
								transformCode = appData[11] != 0
							)
						}
					}
				}
				0xFFDB -> { // DQT (Define Quantization Tables)
					val quantizationTablesLength = readUint16()
					val quantizationTablesEnd = quantizationTablesLength + offset - 2
					while (offset < quantizationTablesEnd) {
						val quantizationTableSpec = data[offset++]
						val tableData = IntArray(64)
						when (quantizationTableSpec shr 4) {
							0 -> for (j in 0 until 64) tableData[dctZigZag[j]] = data[offset++] // 8 bit values
							1 -> for (j in 0 until 64) tableData[dctZigZag[j]] = readUint16() //16 bit
							else -> invalidOp("DQT: invalid table spec")
						}
						quantizationTables[quantizationTableSpec and 15] = tableData
					}
				}
				0xFFC0, 0xFFC1, 0xFFC2 -> {
					// SOF0 (Start of Frame, Baseline DCT), SOF1 (Start of Frame, Extended DCT), SOF2 (Start of Frame, Progressive DCT)
					readUint16() // skip data length
					frame = Frame(
						extended = (fileMarker == 0xFFC1),
						progressive = (fileMarker == 0xFFC2),
						precision = data[offset++],
						scanLines = readUint16(),
						samplesPerLine = readUint16(),
						components = ArrayList(),
						componentsOrder = arrayListOf()
					)
					val componentsCount = data[offset++]
					var componentId: Int
					//var maxH = 0
					//var maxV = 0
					for (i in 0 until componentsCount) {
						componentId = data[offset]
						val h = data[offset + 1] shr 4
						val v = data[offset + 1] and 15
						val qId = data[offset + 2]
						frame.componentsOrder.add(componentId)
						while (frame.components.size <= componentId) frame.components.add(FrameComponent(0, 0, 0))
						frame.components[componentId] = FrameComponent(h = h, v = v, quantizationIdx = qId)
						offset += 3
					}
					prepareComponents(frame)
					frames.add(frame)
				}
				0xFFC4 -> { // DHT (Define Huffman Tables)
					val huffmanLength = readUint16()
					var i = 2
					while (i < huffmanLength) {
						val huffmanTableSpec = data[offset++]
						val codeLengths = UByteArrayInt(16)
						var codeLengthSum = 0
						for (j in 0 until 16) {
							codeLengths[j] = data[offset]
							codeLengthSum += codeLengths[j]
							offset++
						}
						val huffmanValues = UByteArrayInt(codeLengthSum)
						for (j in 0 until codeLengthSum) {
							huffmanValues[j] = data[offset]
							offset++
						}

						i += 17 + codeLengthSum

						val table = if ((huffmanTableSpec shr 4) == 0) huffmanTablesDC else huffmanTablesAC
						table[huffmanTableSpec and 15] = buildHuffmanTable(codeLengths, huffmanValues)
					}
				}

				0xFFDD -> { // DRI (Define Restart Interval)
					readUint16() // skip data length
					resetInterval = readUint16()
				}
				0xFFDA -> { // SOS (Start of Scan)
					@Suppress("UNUSED_VARIABLE")
					var scanLength = readUint16()
					val selectorsCount = data[offset++]
					val components = arrayListOf<FrameComponent>()
					var component: FrameComponent
					for (i in 0 until selectorsCount) {
						component = frame.components[data[offset++]]
						val tableSpec = data[offset++]
						component.huffmanTableDC = huffmanTablesDC[tableSpec shr 4]
						component.huffmanTableAC = huffmanTablesAC[tableSpec and 15]
						components.add(component)
					}
					val spectralStart = data[offset++]
					val spectralEnd = data[offset++]
					val successiveApproximation = data[offset++]
					val processed = decodeScan(
						data, offset,
						frame, components, resetInterval,
						spectralStart, spectralEnd,
						successiveApproximation shr 4,
						successiveApproximation and 15
					)
					offset += processed
				}
				else -> {
					if (data[offset - 3] == 0xFF &&
						data[offset - 2] >= 0xC0 && data[offset - 2] <= 0xFE
					) {
						// could be incorrect encoding -- last 0xFF byte of the previous
						// block was eaten by the encoder
						offset -= 3
					} else {
						invalidOp("unknown JPEG marker " + fileMarker.toString(16))
					}
				}
			}
			fileMarker = readUint16()
		}

		if (frames.size != 1) invalidOp("only single frame JPEGs supported")

		// set each frame's components quantization table
		for (i in 0 until frames.size) {
			val cp = frames[i].components
			for (c in cp) {
				c.quantizationTable = quantizationTables[c.quantizationIdx]
				c.quantizationIdx = -1
			}
		}

		this.width = frame.samplesPerLine
		this.height = frame.scanLines
		this.jfif = jfif
		this.adobe = adobe
		this.components = arrayListOf()
		for (i in 0 until frame.componentsOrder.size) {
			val component = frame.components[frame.componentsOrder[i]]
			this.components.add(
				Component(
					lines = buildComponentData(frame, component),
					scaleX = component.h.toFloat() / frame.maxH.toFloat(),
					scaleY = component.v.toFloat() / frame.maxV.toFloat()
				)
			)
		}
	}

	class Component(val lines: List<UByteArrayInt>, val scaleX: Float, val scaleY: Float)

	private fun getData(width: Int, height: Int): UByteArrayInt {
		val scaleX = this.width / width
		val scaleY = this.height / height

		var offset = 0
		val dataLength = width * height * this.components.size
		val data = UByteArrayInt(dataLength)

		when (this.components.size) {
			1 -> {
				val component1 = this.components[0]
				for (y in 0 until height) {
					val component1Line = component1.lines[((y * component1.scaleY * scaleY).toInt())]
					for (x in 0 until width) {
						data[offset++] = component1Line[((x * component1.scaleX * scaleX).toInt())]
					}
				}
			}
			2 -> {
				// PDF might compress two component data in custom colorspace
				val component1 = this.components[0]
				val component2 = this.components[1]
				for (y in 0 until height) {
					val component1Line = component1.lines[((y * component1.scaleY * scaleY).toInt())]
					val component2Line = component2.lines[((y * component2.scaleY * scaleY).toInt())]
					for (x in 0 until width) {
						data[offset++] = component1Line[((x * component1.scaleX * scaleX).toInt())]
						data[offset++] = component2Line[((x * component2.scaleX * scaleX).toInt())]
					}
				}
			}
			3 -> {
				// The adobe transform marker overrides any previous setting
				val colorTransform = when {
					this.adobe?.transformCode == true -> true
					else -> this.colorTransform ?: true
				}

				val component1 = this.components[0]
				val component2 = this.components[1]
				val component3 = this.components[2]
				for (y in 0 until height) {
					val component1Line = component1.lines[((y * component1.scaleY * scaleY).toInt())]
					val component2Line = component2.lines[((y * component2.scaleY * scaleY).toInt())]
					val component3Line = component3.lines[((y * component3.scaleY * scaleY).toInt())]

					if (!colorTransform) {
						for (x in 0 until width) {
							data[offset++] = component1Line[((x * component1.scaleX * scaleX).toInt())]
							data[offset++] = component2Line[((x * component2.scaleX * scaleX).toInt())]
							data[offset++] = component3Line[((x * component3.scaleX * scaleX).toInt())]
						}
					} else {
						for (x in 0 until width) {
							val yy = component1Line[((x * component1.scaleX * scaleX).toInt())]
							val cb = component2Line[((x * component2.scaleX * scaleX).toInt())]
							val cr = component3Line[((x * component3.scaleX * scaleX).toInt())]
							data[offset++] = clampTo8bit((yy + 1.402f * (cr - 128f)).toInt())
							data[offset++] =
									clampTo8bit((yy - 0.3441363f * (cb - 128f) - 0.71413636f * (cr - 128f)).toInt())
							data[offset++] = clampTo8bit((yy + 1.772f * (cb - 128f)).toInt())
						}
					}
				}
			}
			4 -> {
				if (this.adobe == null) invalidOp("Unsupported color mode (4 components)")
				// The default transform for four components is false
				// The adobe transform marker overrides any previous setting
				val colorTransform = when {
					this.adobe?.transformCode == true -> true
					else -> this.colorTransform ?: false
				}
				val component1 = this.components[0]
				val component2 = this.components[1]
				val component3 = this.components[2]
				val component4 = this.components[3]
				for (y in 0 until height) {
					val component1Line = component1.lines[(y * component1.scaleY * scaleY).toInt()]
					val component2Line = component2.lines[(y * component2.scaleY * scaleY).toInt()]
					val component3Line = component3.lines[(y * component3.scaleY * scaleY).toInt()]
					val component4Line = component4.lines[(y * component4.scaleY * scaleY).toInt()]
					for (x in 0 until width) {
						val c: Int
						val m: Int
						val ye: Int
						val k: Int

						if (!colorTransform) {
							c = component1Line[(x * component1.scaleX * scaleX).toInt()]
							m = component2Line[(x * component2.scaleX * scaleX).toInt()]
							ye = component3Line[(x * component3.scaleX * scaleX).toInt()]
							k = component4Line[(x * component4.scaleX * scaleX).toInt()]
						} else {
							val yy = component1Line[(x * component1.scaleX * scaleX).toInt()]
							val cb = component2Line[(x * component2.scaleX * scaleX).toInt()]
							val cr = component3Line[(x * component3.scaleX * scaleX).toInt()]

							k = component4Line[(x * component4.scaleX * scaleX).toInt()]

							c = 255 - clampTo8bit((yy + 1.402 * (cr - 128)).toInt())
							m = 255 - clampTo8bit((yy - 0.3441363 * (cb - 128) - 0.71413636 * (cr - 128)).toInt())
							ye = 255 - clampTo8bit((yy + 1.772 * (cb - 128)).toInt())
						}
						data[offset++] = 255 - c
						data[offset++] = 255 - m
						data[offset++] = 255 - ye
						data[offset++] = 255 - k
					}
				}
			}
			else -> invalidOp("Unsupported color mode")
		}
		return data
	}

	fun copyToImageData(imageData: ImageData) {
		val width = imageData.width
		val height = imageData.height
		val imageDataArray = imageData.data
		val data = this.getData(width, height)
		var i = 0
		var j = 0
		when (this.components.size) {
			1 -> {
				for (n in 0 until width * height) {
					val yy = data[i++]
					imageDataArray[j++] = yy
					imageDataArray[j++] = yy
					imageDataArray[j++] = yy
					imageDataArray[j++] = 255
				}
			}
			3 -> {
				for (n in 0 until width * height) {
					imageDataArray[j++] = data[i++]
					imageDataArray[j++] = data[i++]
					imageDataArray[j++] = data[i++]
					imageDataArray[j++] = 255
				}
			}
			4 -> {
				for (n in 0 until width * height) {
					val c = data[i++]
					val m = data[i++]
					val y = data[i++]
					val k = data[i++]

					imageDataArray[j++] = 255 - clampTo8bit(c * (1 - k / 255) + k)
					imageDataArray[j++] = 255 - clampTo8bit(m * (1 - k / 255) + k)
					imageDataArray[j++] = 255 - clampTo8bit(y * (1 - k / 255) + k)
					imageDataArray[j++] = 255
				}
			}
			else -> invalidOp("Unsupported color mode")
		}
	}

	data class ImageInfo(val width: Int, val height: Int)
	class ImageData(val width: Int, val height: Int, val data: UByteArrayInt)

	companion object {
		fun decodeInfo(jpegData: ByteArray): ImageInfo {
			val arr = UByteArrayInt(jpegData)
			val decoder = JPEGDecoder()
			decoder.parse(arr)
			return JPEGDecoder.ImageInfo(decoder.width, decoder.height)
		}

		fun decode(jpegData: ByteArray): Bitmap32 {
			val data = decodeToData(jpegData)
			return RGBA.decodeToBitmap32(data.width, data.height, data.data.asByteArray())
		}

		private fun decodeToData(jpegData: ByteArray): ImageData {
			val arr = UByteArrayInt(jpegData)
			val decoder = JPEGDecoder()
			decoder.parse(arr)

			val image = ImageData(
				width = decoder.width,
				height = decoder.height,
				data = UByteArrayInt(decoder.width * decoder.height * 4)
			)

			decoder.copyToImageData(image)

			return image
		}
	}
}
