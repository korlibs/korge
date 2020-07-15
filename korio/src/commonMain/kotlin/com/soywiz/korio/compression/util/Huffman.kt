package com.soywiz.korio.compression.util

import com.soywiz.kmem.*
import com.soywiz.korio.experimental.*

@UseExperimental(KorioExperimentalApi::class)
internal class HuffmanTree {
	companion object {
		private const val INVALID_VALUE = -1
		private const val NIL = 1023
		private const val FAST_BITS = 9
		private const val MAX_LEN = 16
		private const val MAX_CODES = 288

		//private const val ENABLE_EXPERIMENTAL_FAST_READ = true
		//private const val ENABLE_EXPERIMENTAL_FAST_READ = false
	}

	private val value = IntArray(1024)
	private val left = IntArray(1024)
	private val right = IntArray(1024)

	private var nodeOffset = 0
	private var root: Int = NIL
	private var ncodes: Int = 0

	// Low half-word contains the value, High half-word contains the len
	//val FAST_INFO = IntArray(1 shl FAST_BITS) { INVALID_VALUE }

	fun read(reader: BitReader): Int {
		var node = this.root
		do {
			node = if (reader.sreadBit()) node.right else node.left
		} while (node != NIL && node.value == INVALID_VALUE)
		return node.value
	}

	private fun resetAlloc() {
		nodeOffset = 0
	}

	private fun alloc(value: Int, left: Int, right: Int): Int {
		return (nodeOffset++).apply {
			this@HuffmanTree.value[this] = value
			this@HuffmanTree.left[this] = left
			this@HuffmanTree.right[this] = right
		}
	}

	private fun allocLeaf(value: Int): Int = alloc(value, NIL, NIL)
	private fun allocNode(left: Int, right: Int): Int = alloc(INVALID_VALUE, left, right)

	private inline val Int.value get() = this@HuffmanTree.value[this]
	private inline val Int.left get() = this@HuffmanTree.left[this]
	private inline val Int.right get() = this@HuffmanTree.right[this]

	private val COUNTS = IntArray(MAX_LEN + 1)
	private val OFFSETS = IntArray(MAX_LEN + 1)
	private val COFFSET = IntArray(MAX_LEN + 1)
	private val CODES = IntArray(MAX_CODES)
	private val ENCODED_VAL = IntArray(MAX_CODES)
	private val ENCODED_LEN = ByteArray(MAX_CODES)

	fun fromLengths(codeLengths: IntArray, start: Int = 0, end: Int = codeLengths.size): HuffmanTree {
		var oldOffset = 0
		var oldCount = 0
		val ncodes = end - start

		resetAlloc()

		COUNTS.fill(0)

		// Compute the count of codes per length
		for (n in start until end) {
			val codeLen = codeLengths[n]
			if (codeLen !in 0..MAX_LEN) error("Invalid HuffmanTree.codeLengths $codeLen")
			COUNTS[codeLen]++
		}

		// Compute the disposition using the counts per length
		var currentOffset = 0
		for (n in 0 until MAX_LEN) {
			val count = COUNTS[n]
			OFFSETS[n] = currentOffset
			COFFSET[n] = currentOffset
			currentOffset += count
		}

		// Place elements in the computed disposition
		for (n in start until end) {
			val codeLen = codeLengths[n]
			CODES[COFFSET[codeLen]++] = n - start
		}

		for (i in MAX_LEN downTo 1) {
			val newOffset = nodeOffset

			val OFFSET = OFFSETS[i]
			val SIZE = COUNTS[i]
			for (j in 0 until SIZE) allocLeaf(CODES[OFFSET + j])
			for (j in 0 until oldCount step 2) allocNode(oldOffset + j, oldOffset + j + 1)

			oldOffset = newOffset
			oldCount = SIZE + oldCount / 2
			if (oldCount >= 2 && oldCount % 2 != 0) {
                error("This canonical code does not represent a Huffman code tree: $oldCount")
            }
		}
		if (oldCount != 2) {
            error("This canonical code does not represent a Huffman code tree")
        }

		this.root = allocNode(nodeOffset - 2, nodeOffset - 1)
		this.ncodes = ncodes

		//if (ENABLE_EXPERIMENTAL_FAST_READ) {
		//	computeFastLookup()
		//}

		return this
	}

	//private fun computeFastLookup() {
	//	ENCODED_LEN.fill(0)
	//	FAST_INFO.fill(INVALID_VALUE)
	//	computeEncodedValues(root, 0, 0)
	//	//println("--------------------")
	//	for (n in 0 until ncodes) {
	//		val enc = ENCODED_VAL[n]
	//		val bits = ENCODED_LEN[n].toInt()
	//		check((enc and 0xFFFF) == enc)
	//		check((bits and 0xFF) == bits)
	//		if (bits in 1..FAST_BITS) {
	//			val remainingBits = FAST_BITS - bits
	//			val repeat = 1 shl remainingBits
	//			val info = enc or (bits shl 16)
//
	//			//println("n=$n  : enc=$enc : bits=$bits, repeat=$repeat")
//
	//			for (j in 0 until repeat) {
	//				FAST_INFO[enc or (j shl bits)] = info
	//			}
	//		}
	//	}
	//	//for (fv in FAST_INFO) check(fv != INVALID_VALUE)
	//}
	//private fun computeEncodedValues(node: Int, encoded: Int, encodedBits: Int) {
	//	if (node.value == INVALID_VALUE) {
	//		computeEncodedValues(node.left, encoded, encodedBits + 1)
	//		computeEncodedValues(node.right, encoded or (1 shl encodedBits), encodedBits + 1)
	//	} else {
	//		val nvalue = node.value
	//		ENCODED_VAL[nvalue] = encoded
	//		ENCODED_LEN[nvalue] = encodedBits.toByte()
	//	}
	//}
}
