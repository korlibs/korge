package com.soywiz.korio.stream

import com.soywiz.kmem.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*

class FastByteArrayInputStream(val ba: ByteArray, var offset: Int = 0) {
	val length: Int get() = ba.size
	val available: Int get() = ba.size - offset
	val hasMore: Boolean get() = available > 0
	val eof: Boolean get() = !hasMore

	// Skipping
	fun skip(count: Int) = run { offset += count }

	fun skipToAlign(count: Int) {
		val nextPosition = offset.nextAlignedTo(offset)
		readBytes((nextPosition - offset).toInt())
	}

	// 8 bit
	fun readS8() = increment(1) { ba.readS8(offset) }
	fun readU8() = increment(1) { ba.readU8(offset) }

	// 16 bits
	fun readS16LE() = increment(2) { ba.readS16LE(offset) }

	fun readS16BE() = increment(2) { ba.readS16BE(offset) }
	fun readU16LE() = increment(2) { ba.readU16LE(offset) }
	fun readU16BE() = increment(2) { ba.readU16BE(offset) }

	// 24 bits
	fun readS24LE() = increment(3) { ba.readS24LE(offset) }
	fun readS24BE() = increment(3) { ba.readS24BE(offset) }
	fun readU24LE() = increment(3) { ba.readU24LE(offset) }
	fun readU24BE() = increment(3) { ba.readU24BE(offset) }

	// 32 bits
	fun readS32LE() = increment(4) { ba.readS32LE(offset) }
	fun readS32BE() = increment(4) { ba.readS32BE(offset) }
	fun readU32LE() = increment(4) { ba.readU32LE(offset) }
	fun readU32BE() = increment(4) { ba.readU32BE(offset) }

	// 32 bits FLOAT
	fun readF32LE() = increment(4) { ba.readF32LE(offset) }
	fun readF32BE() = increment(4) { ba.readF32BE(offset) }

	// 64 bits FLOAT
	fun readF64LE() = increment(8) { ba.readF64LE(offset) }
	fun readF64BE() = increment(8) { ba.readF64BE(offset) }

	// Bytes
    fun read(data: ByteArray, offset: Int = 0, count: Int = data.size - offset) = increment(count) {
        val readCount = count.coerceAtMost(available)
        arraycopy(this.ba, this.offset, data, offset, readCount)
        readCount
    }
    fun readBytes(count: Int) = increment(count) { ba.readByteArray(offset, count) }

	// Arrays
	fun readShortArrayLE(count: Int): ShortArray = increment(count * 2) { ba.readShortArrayLE(offset, count) }
	fun readShortArrayBE(count: Int): ShortArray = increment(count * 2) { ba.readShortArrayBE(offset, count) }

	fun readCharArrayLE(count: Int): CharArray = increment(count * 2) { ba.readCharArrayLE(offset, count) }
	fun readCharArrayBE(count: Int): CharArray = increment(count * 2) { ba.readCharArrayBE(offset, count) }

	fun readIntArrayLE(count: Int): IntArray = increment(count * 4) { ba.readIntArrayLE(offset, count) }
	fun readIntArrayBE(count: Int): IntArray = increment(count * 4) { ba.readIntArrayBE(offset, count) }

	fun readLongArrayLE(count: Int): LongArray = increment(count * 8) { ba.readLongArrayLE(offset, count) }
	fun readLongArrayBE(count: Int): LongArray = increment(count * 8) { ba.readLongArrayBE(offset, count) }

	fun readFloatArrayLE(count: Int): FloatArray = increment(count * 4) { ba.readFloatArrayLE(offset, count) }
	fun readFloatArrayBE(count: Int): FloatArray = increment(count * 4) { ba.readFloatArrayBE(offset, count) }

	fun readDoubleArrayLE(count: Int): DoubleArray = increment(count * 8) { ba.readDoubleArrayLE(offset, count) }
	fun readDoubleArrayBE(count: Int): DoubleArray = increment(count * 8) { ba.readDoubleArrayBE(offset, count) }

	// Variable Length
	fun readU_VL(): Int {
		var result = readU8()
		if ((result and 0x80) == 0) return result
		result = (result and 0x7f) or (readU8() shl 7)
		if ((result and 0x4000) == 0) return result
		result = (result and 0x3fff) or (readU8() shl 14)
		if ((result and 0x200000) == 0) return result
		result = (result and 0x1fffff) or (readU8() shl 21)
		if ((result and 0x10000000) == 0) return result
		result = (result and 0xfffffff) or (readU8() shl 28)
		return result
	}

	fun readS_VL(): Int {
		val v = readU_VL()
		val sign = ((v and 1) != 0)
		val uvalue = v ushr 1
		return if (sign) -uvalue - 1 else uvalue
	}

	// String
	fun readString(len: Int, charset: Charset = UTF8) = readBytes(len).toString(charset)

	fun readStringz(len: Int, charset: Charset = UTF8): String {
		val res = readBytes(len)
		val index = res.indexOf(0.toByte())
		return res.copyOf(if (index < 0) len else index).toString(charset)
	}

	fun readStringz(charset: Charset = UTF8): String {
		val startOffset = offset
		val index = ba.indexOf(0.toByte(), offset)
		val end = if (index >= 0) index else ba.size
		val str = ba.copyOfRange(startOffset, end - startOffset).toString(charset)
		offset = if (index >= 0) end + 1 else end
		return str
	}

	fun readStringVL(charset: Charset = UTF8): String = readString(readU_VL(), charset)

	// Tools
	private inline fun <T> increment(count: Int, callback: () -> T): T {
		val out = callback()
		offset += count
		return out
	}
}

fun ByteArray.openFastStream(offset: Int = 0) = FastByteArrayInputStream(this, offset)
