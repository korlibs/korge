package com.soywiz.korio.compression.util

import com.soywiz.kmem.*
import com.soywiz.korio.experimental.*

@UseExperimental(KorioExperimentalApi::class)
internal class HuffmanTree {
	companion object {
		private const val INVALID_VALUE = -1
        private const val INCOMPLETE_VALUE = -2
		private const val NIL = 1023
		private const val MAX_LEN = 16
		private const val MAX_CODES = 288

        //private const val ENABLE_EXPERIMENTAL_FAST_READ = false
		private const val ENABLE_EXPERIMENTAL_FAST_READ = true
        private const val ENABLE_EXPERIMENTAL_FAST_READ_V2 = true
		//private const val ENABLE_EXPERIMENTAL_FAST_READ = false

        //private const val FAST_BITS = 9
        private const val FAST_BITS = 10
        //private const val FAST_BITS = 11
        //private const val FAST_BITS = 12
        //private const val FAST_BITS = 14
    }

    //private val data = IntArray(3 * 1024)

    private val value = IntArray(1024)
	private val left = IntArray(1024)
	private val right = IntArray(1024)

	private var nodeOffset = 0
	private var root: Int = NIL
	private var ncodes: Int = 0

	// Low half-word contains the value, High half-word contains the len
	val FAST_INFO = IntArray(1 shl FAST_BITS) { INVALID_VALUE }
    val FAST_NODE = IntArray(1 shl FAST_BITS) { 0 }

    //var fastReadCount = 0
    //var slowReadCount = 0

	fun read(reader: BitReader): Int {
        if (ENABLE_EXPERIMENTAL_FAST_READ) reader.ensureBits(FAST_BITS)
        var node = this.root
        if (ENABLE_EXPERIMENTAL_FAST_READ && reader.bitsavailable >= FAST_BITS) {
            //println("${reader.bitsavailable} >= $FAST_BITS")
            val bits = reader.peekBits(FAST_BITS)
            val raw = FAST_INFO[bits]
            val value = raw.toShort().toInt()
            val len = raw shr 16
            if (len > 0) {
                //println("BITS1[raw=${raw.hex}]: len=$len, value=$value")
                reader.skipBits(len)
                if (value == INCOMPLETE_VALUE) {
                    node = FAST_NODE[bits]
                } else {
                    //fastReadCount++
                    return value
                }
            }
        }
		do {
			node = if (reader.sreadBit()) node.right else node.left
		} while (node != NIL && node.value == INVALID_VALUE)
		//println("BITS2: ${node.value}")
        //slowReadCount++
		return node.value
	}

	private fun resetAlloc() {
		nodeOffset = 0
	}

	private fun alloc(value: Int, left: Int, right: Int): Int {
		return (nodeOffset++).apply {
			//this@HuffmanTree.data[0 + this] = value
			//this@HuffmanTree.data[1024 + this] = left
			//this@HuffmanTree.data[2048 + this] = right
            this@HuffmanTree.value[this] = value
            this@HuffmanTree.left[this] = left
            this@HuffmanTree.right[this] = right
		}
	}

    //private inline val Int.value get() = this@HuffmanTree.data[0 + this]
    //private inline val Int.left get() = this@HuffmanTree.data[1024 + this]
    //private inline val Int.right get() = this@HuffmanTree.data[2048 + this]

    private inline val Int.value get() = this@HuffmanTree.value[this]
    private inline val Int.left get() = this@HuffmanTree.left[this]
    private inline val Int.right get() = this@HuffmanTree.right[this]

    private fun allocLeaf(value: Int): Int = alloc(value, NIL, NIL)
	private fun allocNode(left: Int, right: Int): Int = alloc(INVALID_VALUE, left, right)

	private val COUNTS = IntArray(MAX_LEN + 1)
	private val OFFSETS = IntArray(MAX_LEN + 1)
	private val COFFSET = IntArray(MAX_LEN + 1)
	private val CODES = IntArray(MAX_CODES)

    fun fromLengths(codeLengths: IntArray, start: Int = 0, end: Int = codeLengths.size): HuffmanTree {
        setFromLengths(codeLengths, start, end)
        return this
    }

	fun setFromLengths(codeLengths: IntArray, start: Int = 0, end: Int = codeLengths.size) {
		var oldOffset = 0
		var oldCount = 0
		val ncodes = end - start

        //println("fastReadCount=$fastReadCount, slowReadCount=$slowReadCount")
        //fastReadCount = 0
        //slowReadCount = 0

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

		if (ENABLE_EXPERIMENTAL_FAST_READ) {
			computeFastLookup()
		}
	}

    // @TODO: Optimize this
    private fun computeFastLookup() {
        if (ENABLE_EXPERIMENTAL_FAST_READ_V2) {
            FAST_INFO.fill(INVALID_VALUE)
            computeEncodedValues(root, 0, 0)
        } else {
            //println("computeEncodedValues: " + computeEncodedValues(root, 0, 0))
            //println("FAST_INFO.size=${FAST_INFO.size}")
            for (value in FAST_INFO.indices) {
                var node = this.root
                var bitcount = 0
                do {
                    node = if (value.extractBool(bitcount++)) node.right else node.left
                } while (node != NIL && node.value == INVALID_VALUE)
                FAST_INFO[value] = if (bitcount > FAST_BITS) -1 else node.value or (bitcount shl 16)
            }
        }
    }

	private fun computeEncodedValues(node: Int, encoded: Int, encodedBits: Int) {
		if (node.value == INVALID_VALUE) {
		    if (encodedBits < FAST_BITS) {
                computeEncodedValues(node.left, encoded, encodedBits + 1)
                computeEncodedValues(node.right, encoded or (1 shl encodedBits), encodedBits + 1)
            } else {
                writeVariants(encoded, encodedBits, node, INCOMPLETE_VALUE)
            }
		} else {
            writeVariants(encoded, encodedBits, node, node.value)
            //println("encoded=$encoded, encodedBits=$encodedBits, nvalue=$nvalue, rangeCount=$rangeCount")
		}
	}

    private fun writeVariants(encoded: Int, encodedBits: Int, node: Int, nvalue: Int) {
        val encodedInfo = (nvalue and 0xFFFF) or (encodedBits shl 16)
        val rangeCount = 1 shl (FAST_BITS - encodedBits)
        for (n in 0 until rangeCount) {
            val i = encoded or (n shl encodedBits)
            FAST_INFO[i] = encodedInfo
            FAST_NODE[i] = node
        }
    }
}
