package korlibs.io.stream

import korlibs.memory.arraycopy
import korlibs.memory.nextAlignedTo
import korlibs.memory.readByteArray
import korlibs.memory.readCharArrayBE
import korlibs.memory.readCharArrayLE
import korlibs.memory.readDoubleArrayBE
import korlibs.memory.readDoubleArrayLE
import korlibs.memory.readF32BE
import korlibs.memory.readF32LE
import korlibs.memory.readF64BE
import korlibs.memory.readF64LE
import korlibs.memory.readFloatArrayBE
import korlibs.memory.readFloatArrayLE
import korlibs.memory.readIntArrayBE
import korlibs.memory.readIntArrayLE
import korlibs.memory.readLongArrayBE
import korlibs.memory.readLongArrayLE
import korlibs.memory.readS16BE
import korlibs.memory.readS16LE
import korlibs.memory.readS24BE
import korlibs.memory.readS24LE
import korlibs.memory.readS32BE
import korlibs.memory.readS32LE
import korlibs.memory.readS64BE
import korlibs.memory.readS64LE
import korlibs.memory.readS8
import korlibs.memory.readShortArrayBE
import korlibs.memory.readShortArrayLE
import korlibs.memory.readU16BE
import korlibs.memory.readU16LE
import korlibs.memory.readU24BE
import korlibs.memory.readU24LE
import korlibs.memory.readU32BE
import korlibs.memory.readU32LE
import korlibs.memory.readU8
import korlibs.io.lang.Charset
import korlibs.io.lang.UTF8
import korlibs.io.lang.toString
import korlibs.io.util.indexOf

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

    fun readBytesExact(len: Int): ByteArray {
        val out = ba.copyOfRange(offset, offset + len)
        offset += len
        return out
    }
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

    fun getAllBytes() = ba.copyOfRange(start, end)
    fun getBackingArrayUnsafe() = ba
    //fun toByteArray(): ByteArray = ba.copyOfRange(start, end)
}

fun ByteArray.openFastStream(offset: Int = 0) = FastByteArrayInputStream(this, offset)
fun FastByteArrayInputStream.toSyncStream() = ReadonlySyncStreamBase(ba, start, end - start).open()
fun FastByteArrayInputStream.toAsyncStream() = toSyncStream().toAsync()