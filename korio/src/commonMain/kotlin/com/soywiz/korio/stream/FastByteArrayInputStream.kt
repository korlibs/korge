package com.soywiz.korio.stream

import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.nextAlignedTo
import com.soywiz.kmem.readByteArray
import com.soywiz.kmem.readCharArrayBE
import com.soywiz.kmem.readCharArrayLE
import com.soywiz.kmem.readDoubleArrayBE
import com.soywiz.kmem.readDoubleArrayLE
import com.soywiz.kmem.readF32BE
import com.soywiz.kmem.readF32LE
import com.soywiz.kmem.readF64BE
import com.soywiz.kmem.readF64LE
import com.soywiz.kmem.readFloatArrayBE
import com.soywiz.kmem.readFloatArrayLE
import com.soywiz.kmem.readIntArrayBE
import com.soywiz.kmem.readIntArrayLE
import com.soywiz.kmem.readLongArrayBE
import com.soywiz.kmem.readLongArrayLE
import com.soywiz.kmem.readS16BE
import com.soywiz.kmem.readS16LE
import com.soywiz.kmem.readS24BE
import com.soywiz.kmem.readS24LE
import com.soywiz.kmem.readS32BE
import com.soywiz.kmem.readS32LE
import com.soywiz.kmem.readS64BE
import com.soywiz.kmem.readS64LE
import com.soywiz.kmem.readS8
import com.soywiz.kmem.readShortArrayBE
import com.soywiz.kmem.readShortArrayLE
import com.soywiz.kmem.readU16BE
import com.soywiz.kmem.readU16LE
import com.soywiz.kmem.readU24BE
import com.soywiz.kmem.readU24LE
import com.soywiz.kmem.readU32BE
import com.soywiz.kmem.readU32LE
import com.soywiz.kmem.readU8
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toString
import com.soywiz.korio.util.indexOf

class FastByteArrayInputStream(val ba: ByteArray, offset: Int = 0, val start: Int = 0, val end: Int = ba.size) {
    private var offset = offset + start
    var position: Int
        get() = offset - start
        set(value) {
            offset = start + value
        }
	val length: Int get() = end - start
	val available: Int get() = end - offset
	val hasMore: Boolean get() = available > 0
	val eof: Boolean get() = !hasMore

    fun Int.coerceRange() = this.coerceIn(start, end)

    fun extractBytes(offset: Int, length: Int): ByteArray {
        val start = this.start + offset
        val end = start + length
        return ba.copyOfRange(start, end)
    }

    fun extractString(offset: Int, length: Int, charset: Charset = UTF8): String {
        //return extractBytes(offset, length).toString(charset)
        val start = this.start + offset
        val end = start + length
        return ba.toString(charset, start, end)
    }

    fun readSlice(size: Int): FastByteArrayInputStream {
        val out = sliceWithSize(position, size)
        offset += size
        return out
    }
    fun sliceStart(offset: Int = 0) = FastByteArrayInputStream(ba, 0, (start + offset).coerceRange(), end)
    fun clone() = FastByteArrayInputStream(ba, position, start, end)
    fun sliceWithSize(offset: Int, len: Int) = FastByteArrayInputStream(ba, 0, (start + offset).coerceRange(), (start + offset + len).coerceRange())

    private fun offset(count: Int): Int {
        val out = offset
        offset += count
        return out
    }

	// Skipping
	fun skip(count: Int) { offset = (offset + count).coerceIn(start, end) }
    fun unread(count: Int): Unit = skip(-count)

	fun skipToAlign(count: Int) {
		val nextPosition = offset.nextAlignedTo(count)
        skip(nextPosition - offset)
	}

    fun readBytesExact(len: Int) = increment(len) { ba.copyOfRange(offset, offset + len) }
    fun readAll() = readBytesExact(available)

	// 8 bit
	fun readS8() = ba.readS8(offset(1))
	fun readU8() = ba.readU8(offset(1))

	// 16 bits
	fun readS16LE() = ba.readS16LE(offset(2))

	fun readS16BE() = ba.readS16BE(offset(2))
	fun readU16LE() = ba.readU16LE(offset(2))
	fun readU16BE() = ba.readU16BE(offset(2))

	// 24 bits
	fun readS24LE() = ba.readS24LE(offset(3))
	fun readS24BE() = ba.readS24BE(offset(3))
	fun readU24LE() = ba.readU24LE(offset(3))
	fun readU24BE() = ba.readU24BE(offset(3))

	// 32 bits
	fun readS32LE() = ba.readS32LE(offset(4))
	fun readS32BE() = ba.readS32BE(offset(4))
	fun readU32LE() = ba.readU32LE(offset(4))
	fun readU32BE() = ba.readU32BE(offset(4))

	// 32 bits FLOAT
	fun readF32LE() = ba.readF32LE(offset(4))
	fun readF32BE() = ba.readF32BE(offset(4))

	// 64 bits FLOAT
	fun readF64LE() = ba.readF64LE(offset(8))
	fun readF64BE() = ba.readF64BE(offset(8))

    // 64 bits Long
    fun readS64LE() = ba.readS64LE(offset(8))
    fun readS64BE() = ba.readS64BE(offset(8))

    // Bytes
    fun read(data: ByteArray, offset: Int = 0, count: Int = data.size - offset): Int {
        val readCount = count.coerceAtMost(available)
        arraycopy(this.ba, this.offset, data, offset, readCount)
        this.offset += count
        return readCount
    }
    fun readBytes(count: Int) = ba.readByteArray(offset(count), count)

	// Arrays
	fun readShortArrayLE(count: Int): ShortArray = ba.readShortArrayLE(offset(count * 2), count)
	fun readShortArrayBE(count: Int): ShortArray = ba.readShortArrayBE(offset(count * 2), count)

	fun readCharArrayLE(count: Int): CharArray = ba.readCharArrayLE(offset(count * 2), count)
	fun readCharArrayBE(count: Int): CharArray = ba.readCharArrayBE(offset(count * 2), count)

	fun readIntArrayLE(count: Int): IntArray = ba.readIntArrayLE(offset(count * 4), count)
	fun readIntArrayBE(count: Int): IntArray = ba.readIntArrayBE(offset(count * 4), count)

	fun readLongArrayLE(count: Int): LongArray = ba.readLongArrayLE(offset(count * 8), count)
	fun readLongArrayBE(count: Int): LongArray = ba.readLongArrayBE(offset(count * 8), count)

	fun readFloatArrayLE(count: Int): FloatArray = ba.readFloatArrayLE(offset(count * 4), count)
	fun readFloatArrayBE(count: Int): FloatArray = ba.readFloatArrayBE(offset(count * 4), count)

	fun readDoubleArrayLE(count: Int): DoubleArray = ba.readDoubleArrayLE(offset(count * 8), count)
	fun readDoubleArrayBE(count: Int): DoubleArray = ba.readDoubleArrayBE(offset(count * 8), count)

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
    @Deprecated("")
	private inline fun <T> increment(count: Int, callback: () -> T): T {
        //if (offset + count > end) throw EOFException("${offset + count} > $end")
		val out = callback()
		offset += count
		return out
	}

    fun getAllBytes() = ba.copyOfRange(start, end)
    fun getBackingArrayUnsafe() = ba
    //fun toByteArray(): ByteArray = ba.copyOfRange(start, end)
}

fun ByteArray.openFastStream(offset: Int = 0) = FastByteArrayInputStream(this, offset)
fun FastByteArrayInputStream.toSyncStream() = ReadonlySyncStreamBase(ba, start, end - start).open()
fun FastByteArrayInputStream.toAsyncStream() = toSyncStream().toAsync()
