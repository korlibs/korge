package com.soywiz.korio.stream

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.lang.*
import kotlin.math.*

interface SyncInputStream : OptionalCloseable {
	fun read(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset): Int
	fun read(): Int = smallBytesPool.alloc { if (read(it, 0, 1) > 0) it[0].unsigned else -1 }
}

interface SyncOutputStream : OptionalCloseable {
	fun write(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset): Unit
	fun write(byte: Int) = smallBytesPool.alloc { it[0] = byte.toByte(); write(it, 0, 1) }
	fun flush() = Unit
}

interface SyncPositionStream {
	var position: Long
}

interface SyncLengthStream {
	var length: Long
}

interface SyncRAInputStream {
	fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int
}

interface SyncRAOutputStream {
	fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit
	fun flush(): Unit = Unit
}

open class SyncStreamBase : Closeable, SyncRAInputStream, SyncRAOutputStream, SyncLengthStream {
	val smallTemp = ByteArray(16)
	fun read(position: Long): Int = if (read(position, smallTemp, 0, 1) >= 1) smallTemp[0].toInt() and 0xFF else -1
	override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = unsupported()
	override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit = unsupported()
	override var length: Long set(_) = unsupported(); get() = unsupported()
	override fun close() = Unit
}

class SyncStream(val base: SyncStreamBase, override var position: Long = 0L) : Extra by Extra.Mixin(), Closeable, SyncInputStream, SyncPositionStream, SyncOutputStream, SyncLengthStream {
	private val smallTemp = base.smallTemp

	override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		val read = base.read(position, buffer, offset, len)
		position += read
		return read
	}

	override fun read(): Int {
		val size = read(smallTemp, 0, 1)
		if (size <= 0) return -1
		return smallTemp[0].unsigned
	}

	override fun write(buffer: ByteArray, offset: Int, len: Int): Unit {
		base.write(position, buffer, offset, len)
		position += len
	}

	override fun write(byte: Int) {
		smallTemp[0] = byte.toByte()
		write(smallTemp, 0, 1)
	}

	override var length: Long
		set(value) = run { base.length = value }
		get() = base.length

	val available: Long get() = length - position

	override fun flush() {
		base.flush()
	}

	override fun close(): Unit = base.close()

	fun clone() = SyncStream(base, position)

	override fun toString(): String = "SyncStream($base, $position)"
}

inline fun <T> SyncStream.keepPosition(callback: () -> T): T {
	val old = this.position
	try {
		return callback()
	} finally {
		this.position = old
	}
}

class SliceSyncStreamBase(internal val base: SyncStreamBase, internal val baseStart: Long, internal val baseEnd: Long) :
	SyncStreamBase() {
	internal val baseLength: Long = baseEnd - baseStart

	override var length: Long
		set(value) = throw UnsupportedOperationException()
		get() = baseLength

	private fun clampPosition(position: Long) = position.clamp(baseStart, baseEnd)

	private fun clampPositionLen(position: Long, len: Int): Pair<Long, Int> {
		if (position < 0L) throw IllegalArgumentException("Invalid position")
		val targetStartPosition = clampPosition(this.baseStart + position)
		val targetEndPosition = clampPosition(targetStartPosition + len)
		val targetLen = (targetEndPosition - targetStartPosition).toInt()
		return Pair(targetStartPosition, targetLen)
	}

	override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		val (targetStartPosition, targetLen) = clampPositionLen(position, len)
		return base.read(targetStartPosition, buffer, offset, targetLen)
	}

	override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
		val (targetStartPosition, targetLen) = clampPositionLen(position, len)
		return base.write(targetStartPosition, buffer, offset, targetLen)
	}

	override fun close() = Unit

	override fun toString(): String = "SliceAsyncStreamBase($base, $baseStart, $baseEnd)"
}

class FillSyncStreamBase(val fill: Byte, override var length: Long) : SyncStreamBase() {
	override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		val end = min2(length, position + len)
		val actualLen = (end - position).toIntSafe()
		buffer.fill(fill, offset, offset + actualLen)
		return actualLen
	}

	override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) = Unit

	override fun close() = Unit
}

fun FillSyncStream(fillByte: Int = 0, length: Long = Long.MAX_VALUE) =
	FillSyncStreamBase(fillByte.toByte(), length).toSyncStream()

fun MemorySyncStream(data: ByteArray = EMPTY_BYTE_ARRAY) = MemorySyncStreamBase(ByteArrayBuilder(data)).toSyncStream()
fun MemorySyncStream(data: ByteArrayBuilder) = MemorySyncStreamBase(data).toSyncStream()

class DequeSyncStreamBase(val deque: ByteArrayDeque = ByteArrayDeque()) : SyncStreamBase() {
    override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
        //if (position != deque.read) error("Invalid DequeSyncStreamBase.position for reading $position != ${deque.read}")
        return deque.read(buffer, offset, len)
    }

    override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
        //if (position != deque.written) error("Invalid DequeSyncStreamBase.position for writting $position != ${deque.written}")
        deque.write(buffer, offset, len)
    }

    override var length: Long
        get() = deque.availableRead.toLong()
        set(value) {}

    override fun close() {
        deque.clear()
    }
}

fun DequeSyncStream() = DequeSyncStreamBase().toSyncStream()

inline fun MemorySyncStreamToByteArray(initialCapacity: Int = 4096, callback: SyncStream.() -> Unit): ByteArray {
	val buffer = ByteArrayBuilder(initialCapacity)
	val s = MemorySyncStream(buffer)
	callback(s)
	return buffer.toByteArray()
}

val SyncStream.hasLength: Boolean
	get() = try {
		length; true
	} catch (e: Throwable) {
		false
	}
val SyncStream.hasAvailable: Boolean
	get() = try {
		available; true
	} catch (e: Throwable) {
		false
	}

fun SyncStream.toByteArray(): ByteArray {
	if (hasLength) {
		return this.sliceWithBounds(0L, length).readAll()
	} else {
		return this.clone().readAll()
	}
}

class MemorySyncStreamBase(var data: ByteArrayBuilder) : SyncStreamBase() {
	constructor(initialCapacity: Int = 4096) : this(ByteArrayBuilder(initialCapacity))

	var ilength: Int
		get() = data.size
		set(value) = run { data.size = value }

	override var length: Long
		get() = data.size.toLong()
		set(value) = run { data.size = value.toInt() }

	fun checkPosition(position: Long) = run { if (position < 0) invalidOp("Invalid position $position") }

	override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		checkPosition(position)
		val ipos = position.toInt()
		//if (position !in 0 until ilength) return -1
		if (position !in 0 until ilength) return 0
		val end = min2(this.ilength, ipos + len)
		val actualLen = max2((end - ipos), 0)
		arraycopy(this.data.data, ipos, buffer, offset, actualLen)
		return actualLen
	}

	override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
		checkPosition(position)
		data.size = max2(data.size, (position + len).toInt())
		arraycopy(buffer, offset, this.data.data, position.toInt(), len)
	}

	override fun close() = Unit

	override fun toString(): String = "MemorySyncStreamBase(${data.size})"
}

fun SyncStream.sliceStart(start: Long = 0L): SyncStream = sliceWithBounds(start, this.length)
fun SyncStream.sliceHere(): SyncStream = SyncStream(SliceSyncStreamBase(this.base, position, length))

fun SyncStream.slice(range: IntRange): SyncStream =
	sliceWithBounds(range.start.toLong(), (range.endInclusive.toLong() + 1))

fun SyncStream.slice(range: LongRange): SyncStream = sliceWithBounds(range.start, (range.endInclusive + 1))

fun SyncStream.sliceWithBounds(start: Long, end: Long): SyncStream {
	val len = this.length
	val clampedStart = start.clamp(0, len)
	val clampedEnd = end.clamp(0, len)
	if (this.base is SliceSyncStreamBase) {
		return SliceSyncStreamBase(
			this.base.base,
			this.base.baseStart + clampedStart,
			this.base.baseStart + clampedEnd
		).toSyncStream()
	} else {
		return SliceSyncStreamBase(this.base, clampedStart, clampedEnd).toSyncStream()
	}
}

fun SyncStream.sliceWithSize(position: Long, length: Long): SyncStream = sliceWithBounds(position, position + length)
fun SyncStream.sliceWithSize(position: Int, length: Int): SyncStream =
	sliceWithBounds(position.toLong(), (position + length).toLong())

fun SyncStream.readSlice(length: Long): SyncStream = sliceWithSize(position, length).apply {
	this@readSlice.position += length
}

fun SyncStream.readStream(length: Int): SyncStream = readSlice(length.toLong())
fun SyncStream.readStream(length: Long): SyncStream = readSlice(length)

fun SyncInputStream.readStringz(charset: Charset = UTF8): String {
	val buf = ByteArrayBuilder()
	return bytesTempPool.alloc { temp ->
		while (true) {
			val read = read(temp, 0, 1)
			if (read <= 0) break
			if (temp[0] == 0.toByte()) break
			buf.append(temp[0].toByte())
		}
		buf.toByteArray().toString(charset)
	}
}

fun SyncInputStream.readStringz(len: Int, charset: Charset = UTF8): String {
	val res = readBytes(len)
	val index = res.indexOf(0.toByte())
	return res.copyOf(if (index < 0) len else index).toString(charset)
}

fun SyncInputStream.readString(len: Int, charset: Charset = UTF8): String = readBytes(len).toString(charset)
fun SyncOutputStream.writeString(string: String, charset: Charset = UTF8): Unit =
	writeBytes(string.toByteArray(charset))

fun SyncInputStream.readExact(out: ByteArray, offset: Int, len: Int): Unit {
	var ooffset = offset
	var remaining = len
	while (remaining > 0) {
		val read = read(out, ooffset, remaining)
		if (read <= 0) {
			throw RuntimeException("EOF")
		}
		remaining -= read
		ooffset += read
	}
}

fun SyncInputStream.read(data: ByteArray): Int = read(data, 0, data.size)
fun SyncInputStream.read(data: UByteArray): Int = read(data.asByteArray(), 0, data.size)

fun SyncInputStream.readBytesExact(len: Int): ByteArray = ByteArray(len).apply { readExact(this, 0, len) }

fun SyncOutputStream.writeStringz(str: String, charset: Charset = UTF8) =
	this.writeBytes(str.toBytez(charset))

fun SyncOutputStream.writeStringz(str: String, len: Int, charset: Charset = UTF8) =
	this.writeBytes(str.toBytez(len, charset))

fun SyncInputStream.readBytes(len: Int): ByteArray {
	val bytes = ByteArray(len)
	return bytes.copyOf(read(bytes, 0, len))
}

fun SyncOutputStream.writeBytes(data: ByteArray): Unit = write(data, 0, data.size)
fun SyncOutputStream.writeBytes(data: ByteArray, position: Int, length: Int): Unit = write(data, position, length)

val SyncStream.eof: Boolean get () = this.available <= 0L

fun SyncInputStream.readU8(): Int = read()
fun SyncInputStream.readS8(): Int = read().toByte().toInt()

fun SyncInputStream.readU16LE(): Int = (readU8()) or (readU8() shl 8)
fun SyncInputStream.readU24LE(): Int = (readU8()) or (readU8() shl 8) or (readU8() shl 16)
fun SyncInputStream.readU32LE(): Long = ((readU8()) or (readU8() shl 8) or (readU8() shl 16) or (readU8() shl 24)).toLong() and 0xFFFFFFFFL

fun SyncInputStream.readS16LE(): Int = readU16LE().signExtend(16)
fun SyncInputStream.readS24LE(): Int = readU24LE().signExtend(24)
fun SyncInputStream.readS32LE(): Int = (readU8()) or (readU8() shl 8) or (readU8() shl 16) or (readU8() shl 24)
fun SyncInputStream.readS64LE(): Long = readU32LE() or (readU32LE() shl 32)

fun SyncInputStream.readF32LE(): Float = readS32LE().reinterpretAsFloat()
fun SyncInputStream.readF64LE(): Double = readS64LE().reinterpretAsDouble()

fun SyncInputStream.readU16BE(): Int = (readU8() shl 8) or (readU8())
fun SyncInputStream.readU24BE(): Int = (readU8() shl 16) or (readU8() shl 8) or (readU8())
fun SyncInputStream.readU32BE(): Long = ((readU8() shl 24) or (readU8() shl 16) or (readU8() shl 8) or (readU8())).toLong() and 0xFFFFFFFFL

fun SyncInputStream.readS16BE(): Int = readU16BE().signExtend(16)
fun SyncInputStream.readS24BE(): Int = readU24BE().signExtend(24)
fun SyncInputStream.readS32BE(): Int = (readU8() shl 24) or (readU8() shl 16) or (readU8() shl 8) or (readU8())
fun SyncInputStream.readS64BE(): Long = (readU32BE() shl 32) or (readU32BE())

fun SyncInputStream.readF32BE(): Float = readS32BE().reinterpretAsFloat()
fun SyncInputStream.readF64BE(): Double = readS64BE().reinterpretAsDouble()

fun SyncStream.readAvailable(): ByteArray = readBytes(available.toInt())
fun SyncStream.readAll(): ByteArray = readBytes(available.toInt())

fun SyncInputStream.readUByteArray(count: Int): UByteArray = readBytesExact(count).asUByteArray()

fun SyncInputStream.readShortArrayLE(count: Int): ShortArray = readBytesExact(count * 2).readShortArrayLE(0, count)
fun SyncInputStream.readShortArrayBE(count: Int): ShortArray = readBytesExact(count * 2).readShortArrayBE(0, count)

fun SyncInputStream.readCharArrayLE(count: Int): CharArray = readBytesExact(count * 2).readCharArrayLE(0, count)
fun SyncInputStream.readCharArrayBE(count: Int): CharArray = readBytesExact(count * 2).readCharArrayBE(0, count)

fun SyncInputStream.readIntArrayLE(count: Int): IntArray = readBytesExact(count * 4).readIntArrayLE(0, count)
fun SyncInputStream.readIntArrayBE(count: Int): IntArray = readBytesExact(count * 4).readIntArrayBE(0, count)

fun SyncInputStream.readLongArrayLE(count: Int): LongArray = readBytesExact(count * 8).readLongArrayLE(0, count)
fun SyncInputStream.readLongArrayBE(count: Int): LongArray = readBytesExact(count * 8).readLongArrayBE(0, count)

fun SyncInputStream.readFloatArrayLE(count: Int): FloatArray = readBytesExact(count * 4).readFloatArrayLE(0, count)
fun SyncInputStream.readFloatArrayBE(count: Int): FloatArray = readBytesExact(count * 4).readFloatArrayBE(0, count)

fun SyncInputStream.readDoubleArrayLE(count: Int): DoubleArray = readBytesExact(count * 8).readDoubleArrayLE(0, count)
fun SyncInputStream.readDoubleArrayBE(count: Int): DoubleArray = readBytesExact(count * 8).readDoubleArrayBE(0, count)

fun SyncOutputStream.write8(v: Int): Unit = write(v)

fun SyncOutputStream.write16LE(v: Int): Unit = run { write8(v and 0xFF); write8((v ushr 8) and 0xFF) }
fun SyncOutputStream.write24LE(v: Int): Unit = run { write8(v and 0xFF); write8((v ushr 8) and 0xFF); write8((v ushr 16) and 0xFF) }
fun SyncOutputStream.write32LE(v: Int): Unit = run { write8(v and 0xFF); write8((v ushr 8) and 0xFF); write8((v ushr 16) and 0xFF); write8((v ushr 24) and 0xFF) }
fun SyncOutputStream.write32LE(v: Long): Unit = write32LE(v.toInt())
fun SyncOutputStream.write64LE(v: Long): Unit = run { write32LE(v.toInt()); write32LE((v ushr 32).toInt()) }
fun SyncOutputStream.writeF32LE(v: Float): Unit = write32LE(v.reinterpretAsInt())
fun SyncOutputStream.writeF64LE(v: Double): Unit = write64LE(v.reinterpretAsLong())

fun SyncOutputStream.write16BE(v: Int): Unit = run { write8((v ushr 8) and 0xFF); write8(v and 0xFF) }
fun SyncOutputStream.write24BE(v: Int): Unit = run { write8((v ushr 16) and 0xFF); write8((v ushr 8) and 0xFF); write8(v and 0xFF) }
fun SyncOutputStream.write32BE(v: Int): Unit = run { write8((v ushr 24) and 0xFF); write8((v ushr 16) and 0xFF); write8((v ushr 8) and 0xFF); write8(v and 0xFF) }
fun SyncOutputStream.write32BE(v: Long): Unit = write32BE(v.toInt())
fun SyncOutputStream.write64BE(v: Long): Unit = run { write32BE((v ushr 32).toInt()); write32BE(v.toInt()) }
fun SyncOutputStream.writeF32BE(v: Float): Unit = write32BE(v.reinterpretAsInt())
fun SyncOutputStream.writeF64BE(v: Double): Unit = write64BE(v.reinterpretAsLong())

fun SyncStreamBase.toSyncStream(position: Long = 0L) = SyncStream(this, position)

fun ByteArray.openSync(mode: String = "r"): SyncStream = MemorySyncStreamBase(ByteArrayBuilder(this)).toSyncStream(0L)
fun ByteArray.openAsync(mode: String = "r"): AsyncStream =
//MemoryAsyncStreamBase(ByteArrayBuffer(this, allowGrow = false)).toAsyncStream(0L)
	MemoryAsyncStreamBase(ByteArrayBuilder(this, allowGrow = true)).toAsyncStream(0L)

fun String.openSync(charset: Charset = UTF8): SyncStream = toByteArray(charset).openSync("r")
fun String.openAsync(charset: Charset = UTF8): AsyncStream = toByteArray(charset).openSync("r").toAsync()

fun SyncOutputStream.writeStream(source: SyncInputStream): Unit = source.copyTo(this)

fun SyncInputStream.copyTo(target: SyncOutputStream): Unit {
	bytesTempPool.alloc { chunk ->
		while (true) {
			val count = this.read(chunk)
			if (count <= 0) break
			target.write(chunk, 0, count)
		}
	}
}


fun SyncStream.writeToAlign(alignment: Int, value: Int = 0) {
	val nextPosition = position.nextAlignedTo(alignment.toLong())
	val data = ByteArray((nextPosition - position).toInt())
	data.fill(value.toByte())
	writeBytes(data)
}

fun SyncStream.skip(count: Int): SyncStream {
	position += count
	return this
}

fun SyncStream.skipToAlign(alignment: Int) {
	val nextPosition = position.nextAlignedTo(alignment.toLong())
	readBytes((nextPosition - position).toInt())
}

fun SyncStream.truncate() = run { length = position }

fun SyncOutputStream.writeCharArrayLE(array: CharArray) =
	writeBytes(ByteArray(array.size * 2).apply { writeArrayLE(0, array) })

fun SyncOutputStream.writeShortArrayLE(array: ShortArray) =
	writeBytes(ByteArray(array.size * 2).apply { writeArrayLE(0, array) })

fun SyncOutputStream.writeIntArrayLE(array: IntArray) =
	writeBytes(ByteArray(array.size * 4).apply { writeArrayLE(0, array) })

fun SyncOutputStream.writeLongArrayLE(array: LongArray) =
	writeBytes(ByteArray(array.size * 8).apply { writeArrayLE(0, array) })

fun SyncOutputStream.writeFloatArrayLE(array: FloatArray) =
	writeBytes(ByteArray(array.size * 4).apply { writeArrayLE(0, array) })

fun SyncOutputStream.writeDoubleArrayLE(array: DoubleArray) =
	writeBytes(ByteArray(array.size * 8).apply { writeArrayLE(0, array) })

fun SyncOutputStream.writeCharArrayBE(array: CharArray) =
	writeBytes(ByteArray(array.size * 2).apply { writeArrayBE(0, array) })

fun SyncOutputStream.writeShortArrayBE(array: ShortArray) =
	writeBytes(ByteArray(array.size * 2).apply { writeArrayBE(0, array) })

fun SyncOutputStream.writeIntArrayBE(array: IntArray) =
	writeBytes(ByteArray(array.size * 4).apply { writeArrayBE(0, array) })

fun SyncOutputStream.writeLongArrayBE(array: LongArray) =
	writeBytes(ByteArray(array.size * 8).apply { writeArrayBE(0, array) })

fun SyncOutputStream.writeFloatArrayBE(array: FloatArray) =
	writeBytes(ByteArray(array.size * 4).apply { writeArrayBE(0, array) })

fun SyncOutputStream.writeDoubleArrayBE(array: DoubleArray) =
	writeBytes(ByteArray(array.size * 8).apply { writeArrayBE(0, array) })

// Variable Length

fun SyncInputStream.readU_VL(): Int {
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

fun SyncInputStream.readS_VL(): Int {
	val v = readU_VL()
	val sign = ((v and 1) != 0)
	val uvalue = v ushr 1
	return if (sign) -uvalue - 1 else uvalue
}

fun SyncOutputStream.writeU_VL(v: Int): Unit {
	var value = v
	while (true) {
		val c = value and 0x7f
		value = value ushr 7
		if (value == 0) {
			write8(c)
			break
		}
		write8(c or 0x80)
	}
}

fun SyncOutputStream.writeS_VL(v: Int): Unit {
	val sign = if (v < 0) 1 else 0
	writeU_VL(sign or ((if (v < 0) -v - 1 else v) shl 1))
}

fun SyncOutputStream.writeStringVL(str: String, charset: Charset = UTF8): Unit {
	val bytes = str.toByteArray(charset)
	writeU_VL(bytes.size)
	writeBytes(bytes)
}

fun SyncStream.readStringVL(charset: Charset = UTF8): String {
	val bytes = ByteArray(readU_VL())
	readExact(bytes, 0, bytes.size)
	return bytes.toString(charset)
}

fun SyncInputStream.readExactTo(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size - offset): Int {
	val end = offset + length
	var pos = offset
	while (true) {
		val read = this.read(buffer, pos, end - pos)
		if (read <= 0) break
		pos += read
	}
	return pos - offset
}
