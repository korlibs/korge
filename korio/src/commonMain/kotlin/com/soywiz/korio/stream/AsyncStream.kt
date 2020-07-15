@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.stream

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlin.coroutines.*
import kotlin.math.*

//interface SmallTemp {
//	val smallTemp: ByteArray
//}

//interface AsyncBaseStream : AsyncCloseable, SmallTemp {
interface AsyncBaseStream : AsyncCloseable {
}

interface AsyncInputOpenable {
	suspend fun openRead(): AsyncInputStream
}

interface AsyncInputStream : AsyncBaseStream {
	suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int
	suspend fun read(): Int = smallBytesPool.alloc2 { if (read(it, 0, 1) > 0) it[0].unsigned else -1 }
	//suspend fun read(): Int
}

interface AsyncOutputStream : AsyncBaseStream {
	suspend fun write(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset)
	suspend fun write(byte: Int) = smallBytesPool.alloc2 { it[0] = byte.toByte(); write(it, 0, 1) }
	//suspend fun write(byte: Int)
}

interface AsyncGetPositionStream : AsyncBaseStream {
	suspend fun getPosition(): Long = throw UnsupportedOperationException()
}

interface AsyncPositionStream : AsyncGetPositionStream {
	suspend fun setPosition(value: Long): Unit = throw UnsupportedOperationException()
}

interface AsyncGetLengthStream : AsyncBaseStream {
	suspend fun getLength(): Long = throw UnsupportedOperationException()
}

interface AsyncLengthStream : AsyncGetLengthStream {
	suspend fun setLength(value: Long): Unit = throw UnsupportedOperationException()
}

interface AsyncPositionLengthStream : AsyncPositionStream, AsyncLengthStream {
}

interface AsyncInputStreamWithLength : AsyncInputStream, AsyncGetPositionStream, AsyncGetLengthStream {
}

fun List<AsyncInputStreamWithLength>.combine(): AsyncInputStreamWithLength {
	val list = this
	return object : AsyncInputStreamWithLength {
		override suspend fun getPosition(): Long = list.map { it.getPosition() }.sum()
		override suspend fun getLength(): Long = list.map { it.getLength() }.sum()

		override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
			list.fastForEach { i ->
				val read = i.read(buffer, offset, len)
				if (read > 0) return read
			}
			return -1
		}

		override suspend fun close() {
			list.fastForEach { i ->
				i.close()
			}
		}
	}
}

operator fun AsyncInputStreamWithLength.plus(other: AsyncInputStreamWithLength): AsyncInputStreamWithLength = listOf(this, other).combine()

suspend fun AsyncInputStreamWithLength.getAvailable() = this.getLength() - this.getPosition()
suspend fun AsyncInputStreamWithLength.hasAvailable() = getAvailable() > 0

interface AsyncRAInputStream {
	suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int
}

interface AsyncRAOutputStream {
	suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int)
}

fun AsyncBaseStream.toAsyncStream(): AsyncStream {
	val input = this as? AsyncInputStream
	val output = this as? AsyncOutputStream
	val rlen = this as? AsyncLengthStream
	val closeable = this

	return object : AsyncStreamBase() {
		var expectedPosition: Long = 0L
		//val events = arrayListOf<String>()

		override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
			if (input == null) throw UnsupportedOperationException()
			//events += "before_read:actualPosition=$position,position=$expectedPosition"
			checkPosition(position)
			val read = input.read(buffer, offset, len)
			//events += "read:$read"
			if (read > 0) expectedPosition += read
			return read
		}

		override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
			if (output == null) throw UnsupportedOperationException()
			checkPosition(position)
			output.write(buffer, offset, len)
			expectedPosition += len
		}

		private fun checkPosition(position: Long) {
			if (position != expectedPosition) {
				throw UnsupportedOperationException("Seeking not supported!")
			}
		}

		override suspend fun setLength(value: Long) = rlen?.setLength(value) ?: throw UnsupportedOperationException()
		override suspend fun getLength(): Long = rlen?.getLength() ?: throw UnsupportedOperationException()
		override suspend fun close() = closeable.close()
	}.toAsyncStream()
}

open class AsyncStreamBase : AsyncCloseable, AsyncRAInputStream, AsyncRAOutputStream, AsyncLengthStream {
	//var refCount = 0

	override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int =
		throw UnsupportedOperationException()

	override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit =
		throw UnsupportedOperationException()

	override suspend fun setLength(value: Long): Unit = throw UnsupportedOperationException()
	override suspend fun getLength(): Long = throw UnsupportedOperationException()

	override suspend fun close(): Unit = Unit
}

suspend fun AsyncStreamBase.readBytes(position: Long, count: Int): ByteArray {
	val out = ByteArray(count)
	val readLen = read(position, out, 0, out.size)
	return out.copyOf(readLen)
}

fun AsyncStreamBase.toAsyncStream(position: Long = 0L): AsyncStream = AsyncStream(this, position)

class AsyncStream(val base: AsyncStreamBase, var position: Long = 0L) : Extra by Extra.Mixin(), AsyncInputStream, AsyncInputStreamWithLength, AsyncOutputStream, AsyncPositionLengthStream,
	AsyncCloseable {
	private val readQueue = AsyncThread()
	private val writeQueue = AsyncThread()

	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = readQueue {
		val read = base.read(position, buffer, offset, len)
		if (read >= 0) position += read
		read
	}

	override suspend fun write(buffer: ByteArray, offset: Int, len: Int): Unit = writeQueue {
		base.write(position, buffer, offset, len)
		position += len
	}

	override suspend fun setPosition(value: Long): Unit = run { this.position = value }
	override suspend fun getPosition(): Long = this.position
	override suspend fun setLength(value: Long): Unit = base.setLength(value)
	override suspend fun getLength(): Long = base.getLength()
	suspend fun size(): Long = base.getLength()

	suspend fun getAvailable(): Long = getLength() - getPosition()
	suspend fun eof(): Boolean = this.getAvailable() <= 0L

	override suspend fun close(): Unit = base.close()

	fun duplicate(): AsyncStream = AsyncStream(base, position)
}

suspend fun AsyncStream.hasLength() = try {
	getLength(); true
} catch (t: Throwable) {
	false
}
suspend fun AsyncStream.hasAvailable() = try {
	getAvailable(); true
} catch (t: Throwable) {
	false
}

inline fun <T> AsyncStream.keepPosition(callback: () -> T): T {
	val old = this.position
	try {
		return callback()
	} finally {
		this.position = old
	}
}

suspend fun AsyncPositionLengthStream.getAvailable(): Long = getLength() - getPosition()
suspend fun AsyncPositionLengthStream.eof(): Boolean = this.getAvailable() <= 0L

class SliceAsyncStreamBase(
	internal val base: AsyncStreamBase,
	internal val baseStart: Long,
	internal val baseEnd: Long,
	internal val closeParent: Boolean
) : AsyncStreamBase() {
	//init {
	//	base.refCount++
	//}

	internal val baseLength = baseEnd - baseStart

	private fun clampPosition(position: Long) = position.clamp(baseStart, baseEnd)

	private fun clampPositionLen(position: Long, len: Int): Pair<Long, Int> {
		if (position < 0L) throw IllegalArgumentException("Invalid position")
		val targetStartPosition = clampPosition(this.baseStart + position)
		val targetEndPosition = clampPosition(targetStartPosition + len)
		val targetLen = (targetEndPosition - targetStartPosition).toInt()
		return Pair(targetStartPosition, targetLen)
	}

	override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		val (targetStartPosition, targetLen) = clampPositionLen(position, len)
		return base.read(targetStartPosition, buffer, offset, targetLen)
	}

	override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
		val (targetStartPosition, targetLen) = clampPositionLen(position, len)
		return base.write(targetStartPosition, buffer, offset, targetLen)
	}

	override suspend fun getLength(): Long = baseLength

	override suspend fun close() {
		if (closeParent) {
			base.close()
		}
	}

	override fun toString(): String = "SliceAsyncStreamBase($base, $baseStart, $baseEnd)"
}

fun AsyncStream.buffered(blockSize: Int = 2048, blocksToRead: Int = 0x10) = BufferedStreamBase(this.base, blockSize, blocksToRead).toAsyncStream(this.position)

class BufferedStreamBase(val base: AsyncStreamBase, val blockSize: Int = 2048, val blocksToRead: Int = 0x10) : AsyncStreamBase() {
	private val bsize = blockSize * blocksToRead

	override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = _read(position, buffer, offset, len)

	var cachedData = byteArrayOf()
	var cachedSector = -1L

	suspend fun _read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		if (position >= base.getLength()) return -1
		val sector = position / bsize
		if (cachedSector != sector) {
			cachedData = base.readBytes(sector * bsize, bsize)
			cachedSector = sector
		}
		val soffset = (position % bsize).toInt()
		val available = cachedData.size - soffset
		val toRead = min(available, len)
		arraycopy(cachedData, soffset, buffer, offset, toRead)
		return toRead
	}

	override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
		base.write(position, buffer, offset, len)
	}

	override suspend fun setLength(value: Long) = base.setLength(value)
	override suspend fun getLength(): Long = base.getLength()
	override suspend fun close() = base.close()
}

suspend fun AsyncBufferedInputStream.readBufferedLine(limit: Int = 0x1000, charset: Charset = UTF8) =
	readUntil('\n'.toByte(), including = false, limit = limit).toString(charset)

fun AsyncInputStream.bufferedInput(bufferSize: Int = 0x2000): AsyncBufferedInputStream =
	AsyncBufferedInputStream(this, bufferSize)

class AsyncBufferedInputStream(val base: AsyncInputStream, val bufferSize: Int = 0x2000) : AsyncInputStream {
	private val buf = ByteArrayDeque(bufferSize)

	private val queue = AsyncThread()
	private val temp = ByteArray(bufferSize)

	suspend fun require(len: Int = 1) = queue {
		while (buf.availableRead < len) {
			val read = base.read(temp, 0, temp.size)
			if (read <= 0) break
			buf.write(temp, 0, read)
		}
	}

	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		require()
		return buf.read(buffer, offset, len)
	}

	override suspend fun read(): Int {
		require()
		return buf.readByte()
	}

	suspend fun readUntil(end: Byte, including: Boolean = true, limit: Int = 0x1000): ByteArray {
		val out = ByteArrayBuilder()
		while (true) {
			require()
			val byteInt = buf.readByte()
			if (byteInt < 0) break
			val byte = byteInt.toByte()
			//println("chunk: $chunk, ${chunk.size}")
			if (including || byte != end) {
				out.append(byte)
			}
			if (byte == end || out.size >= limit) break
		}
		return out.toByteArray()
	}

	override suspend fun close() {
		base.close()
	}
}

suspend fun AsyncStream.sliceWithSize(start: Long, length: Long, closeParent: Boolean = false): AsyncStream = sliceWithBounds(start, start + length, closeParent)
suspend fun AsyncStream.sliceWithSize(start: Int, length: Int, closeParent: Boolean = false): AsyncStream =
	sliceWithBounds(start.toLong(), (start + length).toLong(), closeParent)

suspend fun AsyncStream.slice(range: IntRange, closeParent: Boolean = false): AsyncStream =
	sliceWithBounds(range.start.toLong(), (range.endInclusive.toLong() + 1), closeParent)

suspend fun AsyncStream.slice(range: LongRange, closeParent: Boolean = false): AsyncStream = sliceWithBounds(range.start, (range.endInclusive + 1), closeParent)

suspend fun AsyncStream.sliceWithBounds(start: Long, end: Long, closeParent: Boolean = false): AsyncStream {
	val len = this.getLength()
	val clampedStart = start.clamp(0, len)
	val clampedEnd = end.clamp(0, len)

	return if (this.base is SliceAsyncStreamBase) {
		SliceAsyncStreamBase(
			this.base.base,
			this.base.baseStart + clampedStart,
			this.base.baseStart + clampedEnd,
			closeParent
		).toAsyncStream()
	} else {
		SliceAsyncStreamBase(this.base, clampedStart, clampedEnd, closeParent).toAsyncStream()
	}
}

suspend fun AsyncStream.sliceStart(start: Long = 0L, closeParent: Boolean = false): AsyncStream = sliceWithBounds(start, this.getLength(), closeParent)
suspend fun AsyncStream.sliceHere(closeParent: Boolean = false): AsyncStream = this.sliceWithSize(position, this.getLength(), closeParent)

suspend fun AsyncStream.readSlice(length: Long): AsyncStream {
	val start = getPosition()
	val out = this.sliceWithSize(start, length)
	setPosition(start + length)
	return out
}

suspend fun AsyncStream.readStream(length: Int): AsyncStream = readSlice(length.toLong())
suspend fun AsyncStream.readStream(length: Long): AsyncStream = readSlice(length)

suspend fun AsyncInputStream.readStringz(charset: Charset = UTF8): String {
	val buf = ByteArrayBuilder()
	val temp = ByteArray(1)
	while (true) {
		val read = read(temp, 0, 1)
		if (read <= 0) break
		if (temp[0] == 0.toByte()) break
		buf.append(temp[0])
	}
	return buf.toByteArray().toString(charset)
}

suspend fun AsyncInputStream.readStringz(len: Int, charset: Charset = UTF8): String {
	val res = readBytesExact(len)
	val index = res.indexOf(0.toByte())
	return res.copyOf(if (index < 0) len else index).toString(charset)
}

suspend fun AsyncInputStream.readString(len: Int, charset: Charset = UTF8): String =
	readBytesExact(len).toString(charset)

suspend fun AsyncOutputStream.writeStringz(str: String, charset: Charset = UTF8) =
	this.writeBytes(str.toBytez(charset))

suspend fun AsyncOutputStream.writeStringz(str: String, len: Int, charset: Charset = UTF8) =
	this.writeBytes(str.toBytez(len, charset))

suspend fun AsyncOutputStream.writeString(string: String, charset: Charset = UTF8): Unit =
	writeBytes(string.toByteArray(charset))

suspend fun AsyncInputStream.readExact(buffer: ByteArray, offset: Int, len: Int) {
	var remaining = len
	var coffset = offset
	val reader = this
	while (remaining > 0) {
		val read = reader.read(buffer, coffset, remaining)
		if (read < 0) break
		if (read == 0) throw EOFException("Not enough data. Expected=$len, Read=${len - remaining}, Remaining=$remaining")
		coffset += read
		remaining -= read
	}
}

//val READ_SMALL_TEMP by threadLocal { ByteArray(8) }
//suspend private fun AsyncInputStream.readSmallTempExact(len: Int, temp: ByteArray): ByteArray = temp.apply { readExact(temp, 0, len) }
//suspend private fun AsyncInputStream.readSmallTempExact(len: Int): ByteArray = readSmallTempExact(len, READ_SMALL_TEMP)

@PublishedApi
internal suspend inline fun <R> AsyncInputStream.readSmallTempExact(size: Int, callback: ByteArray.() -> R): R = smallBytesPool.allocThis {
	val read = read(this, 0, size)
	if (read != size) error("Couldn't read exact size=$size but read=$read")
	callback()
}


private suspend fun AsyncInputStream.readTempExact(len: Int, temp: ByteArray): ByteArray =
	temp.apply { readExact(temp, 0, len) }
//suspend private fun AsyncInputStream.readTempExact(len: Int): ByteArray = readTempExact(len, BYTES_TEMP)

suspend fun AsyncInputStream.read(data: ByteArray): Int = read(data, 0, data.size)
suspend fun AsyncInputStream.read(data: UByteArray): Int = read(data.asByteArray(), 0, data.size)

val EMPTY_BYTE_ARRAY = ByteArray(0)

suspend fun AsyncInputStream.readBytesUpToFirst(len: Int): ByteArray {
	val out = ByteArray(len)
	val read = read(out, 0, len)
	if (read <= 0) return EMPTY_BYTE_ARRAY
	return out.copyOf(read)
}

suspend fun AsyncInputStream.readBytesUpTo(len: Int): ByteArray {
	val BYTES_TEMP_SIZE = 0x1000
	if (len > BYTES_TEMP_SIZE) {
		if (this is AsyncPositionLengthStream) {
			val ba = ByteArray(min(len, this.getAvailable().toIntClamp()))
			var available = ba.size
			var pos = 0
			while (true) {
				val alen = read(ba, pos, available)
				if (alen <= 0) break
				pos += alen
				available -= alen
			}
			return if (ba.size == pos) ba else ba.copyOf(pos)
		} else {
			var pending = len
			val temp = ByteArray(BYTES_TEMP_SIZE)
			val bout = ByteArrayBuilder()
			while (pending > 0) {
				val read = this.read(temp, 0, min(temp.size, pending))
				if (read <= 0) break
				bout.append(temp, 0, read)
				pending -= read
			}
			return bout.toByteArray()
		}
	} else {
		val ba = ByteArray(len)
		var available = len
		var pos = 0
		while (true) {
			val rlen = read(ba, pos, available)
			if (rlen <= 0) break
			pos += rlen
			available -= rlen
		}
		return if (ba.size == pos) ba else ba.copyOf(pos)
	}

}

suspend fun AsyncInputStream.readBytesExact(len: Int): ByteArray = ByteArray(len).apply { readExact(this, 0, len) }

//suspend fun AsyncInputStream.readU8(): Int = readBytesExact(1).readU8(0)
suspend fun AsyncInputStream.readU8(): Int = read()

suspend fun AsyncInputStream.readS8(): Int = read().toByte().toInt()
suspend fun AsyncInputStream.readU16LE(): Int = readSmallTempExact(2) { readU16LE(0) }
suspend fun AsyncInputStream.readU24LE(): Int = readSmallTempExact(3) { readU24LE(0) }
suspend fun AsyncInputStream.readU32LE(): Long = readSmallTempExact(4) { readU32LE(0) }
suspend fun AsyncInputStream.readS16LE(): Int = readSmallTempExact(2) { readS16LE(0) }
suspend fun AsyncInputStream.readS24LE(): Int = readSmallTempExact(3) { readS24LE(0) }
suspend fun AsyncInputStream.readS32LE(): Int = readSmallTempExact(4) { readS32LE(0) }
suspend fun AsyncInputStream.readS64LE(): Long = readSmallTempExact(8) { readS64LE(0) }
suspend fun AsyncInputStream.readF32LE(): Float = readSmallTempExact(4) { readF32LE(0) }
suspend fun AsyncInputStream.readF64LE(): Double = readSmallTempExact(8) { readF64LE(0) }
suspend fun AsyncInputStream.readU16BE(): Int = readSmallTempExact(2) { readU16BE(0) }
suspend fun AsyncInputStream.readU24BE(): Int = readSmallTempExact(3) { readU24BE(0) }
suspend fun AsyncInputStream.readU32BE(): Long = readSmallTempExact(4) { readU32BE(0) }
suspend fun AsyncInputStream.readS16BE(): Int = readSmallTempExact(2) { readS16BE(0) }
suspend fun AsyncInputStream.readS24BE(): Int = readSmallTempExact(3) { readS24BE(0) }
suspend fun AsyncInputStream.readS32BE(): Int = readSmallTempExact(4) { readS32BE(0) }
suspend fun AsyncInputStream.readS64BE(): Long = readSmallTempExact(8) { readS64BE(0) }
suspend fun AsyncInputStream.readF32BE(): Float = readSmallTempExact(4) { readF32BE(0) }
suspend fun AsyncInputStream.readF64BE(): Double = readSmallTempExact(8) { readF64BE(0) }

suspend fun AsyncInputStream.readAll(): ByteArray {
	return try {
		if (this is AsyncGetPositionStream && this is AsyncGetLengthStream) {
			val available = this.getLength() - this.getPosition()
			this.readBytesExact(available.toInt())
		} else if (this is AsyncStream && this.hasAvailable()) {
			val available = this.getAvailable().toInt()
			this.readBytesExact(available)
		} else {
			val out = ByteArrayBuilder()
			val temp = ByteArray(0x1000)
			while (true) {
				val r = this.read(temp, 0, temp.size)
				if (r <= 0) break
				out.append(temp, 0, r)
			}
			out.toByteArray()
		}
	} finally {
		this.close()
	}
}

// readAll alias
suspend fun AsyncInputStream.readAvailable(): ByteArray = readAll()

suspend fun AsyncInputStream.skip(count: Int) {
	if (this is AsyncPositionLengthStream) {
		this.setPosition(this.getPosition() + count)
	} else {
		val temp = ByteArray(0x1000)
		var remaining = count
		while (remaining > 0) {
			val toRead = min(remaining, count)
			readTempExact(toRead, temp)
			remaining -= toRead
		}
	}
}

suspend fun AsyncInputStream.readUByteArray(count: Int): UByteArray = readBytesExact(count).asUByteArray()
suspend fun AsyncInputStream.readShortArrayLE(count: Int): ShortArray = readBytesExact(count * 2).readShortArrayLE(0, count)
suspend fun AsyncInputStream.readShortArrayBE(count: Int): ShortArray = readBytesExact(count * 2).readShortArrayBE(0, count)
suspend fun AsyncInputStream.readCharArrayLE(count: Int): CharArray = readBytesExact(count * 2).readCharArrayLE(0, count)
suspend fun AsyncInputStream.readCharArrayBE(count: Int): CharArray = readBytesExact(count * 2).readCharArrayBE(0, count)
suspend fun AsyncInputStream.readIntArrayLE(count: Int): IntArray = readBytesExact(count * 4).readIntArrayLE(0, count)
suspend fun AsyncInputStream.readIntArrayBE(count: Int): IntArray = readBytesExact(count * 4).readIntArrayBE(0, count)
suspend fun AsyncInputStream.readLongArrayLE(count: Int): LongArray = readBytesExact(count * 8).readLongArrayLE(0, count)
suspend fun AsyncInputStream.readLongArrayBE(count: Int): LongArray = readBytesExact(count * 8).readLongArrayLE(0, count)
suspend fun AsyncInputStream.readFloatArrayLE(count: Int): FloatArray = readBytesExact(count * 4).readFloatArrayLE(0, count)
suspend fun AsyncInputStream.readFloatArrayBE(count: Int): FloatArray = readBytesExact(count * 4).readFloatArrayBE(0, count)
suspend fun AsyncInputStream.readDoubleArrayLE(count: Int): DoubleArray = readBytesExact(count * 8).readDoubleArrayLE(0, count)
suspend fun AsyncInputStream.readDoubleArrayBE(count: Int): DoubleArray = readBytesExact(count * 8).readDoubleArrayBE(0, count)

suspend fun AsyncOutputStream.writeBytes(data: ByteArray): Unit = write(data, 0, data.size)
suspend fun AsyncOutputStream.writeBytes(data: ByteArray, position: Int, length: Int): Unit = write(data, position, length)
suspend fun AsyncOutputStream.write8(v: Int): Unit = write(v)
suspend fun AsyncOutputStream.write16LE(v: Int): Unit = smallBytesPool.alloc2 { it.write16LE(0, v); write(it, 0, 2) }
suspend fun AsyncOutputStream.write24LE(v: Int): Unit = smallBytesPool.alloc2 { it.write24LE(0, v); write(it, 0, 3) }
suspend fun AsyncOutputStream.write32LE(v: Int): Unit = smallBytesPool.alloc2 { it.write32LE(0, v); write(it, 0, 4) }
suspend fun AsyncOutputStream.write32LE(v: Long): Unit = smallBytesPool.alloc2 { it.write32LE(0, v); write(it, 0, 4) }
suspend fun AsyncOutputStream.write64LE(v: Long): Unit = smallBytesPool.alloc2 { it.write64LE(0, v); write(it, 0, 8) }
suspend fun AsyncOutputStream.writeF32LE(v: Float): Unit = smallBytesPool.alloc2 { it.writeF32LE(0, v); write(it, 0, 4) }
suspend fun AsyncOutputStream.writeF64LE(v: Double): Unit = smallBytesPool.alloc2 { it.writeF64LE(0, v); write(it, 0, 8) }

suspend fun AsyncOutputStream.write16BE(v: Int): Unit = smallBytesPool.alloc2 { it.write16BE(0, v); write(it, 0, 2) }
suspend fun AsyncOutputStream.write24BE(v: Int): Unit = smallBytesPool.alloc2 { it.write24BE(0, v); write(it, 0, 3) }
suspend fun AsyncOutputStream.write32BE(v: Int): Unit = smallBytesPool.alloc2 { it.write32BE(0, v); write(it, 0, 4) }
suspend fun AsyncOutputStream.write32BE(v: Long): Unit = smallBytesPool.alloc2 { it.write32BE(0, v); write(it, 0, 4) }
suspend fun AsyncOutputStream.write64BE(v: Long): Unit = smallBytesPool.alloc2 { it.write64BE(0, v); write(it, 0, 8) }
suspend fun AsyncOutputStream.writeF32BE(v: Float): Unit = smallBytesPool.alloc2 { it.writeF32BE(0, v); write(it, 0, 4) }
suspend fun AsyncOutputStream.writeF64BE(v: Double): Unit = smallBytesPool.alloc2 { it.writeF64BE(0, v); write(it, 0, 8) }

fun SyncStream.toAsync(): AsyncStream = this.base.toAsync().toAsyncStream(this.position)
fun SyncStreamBase.toAsync(): AsyncStreamBase = when (this) {
	is MemorySyncStreamBase -> MemoryAsyncStreamBase(this.data)
	else -> SyncAsyncStreamBase(this)
}

fun SyncStream.toAsyncInWorker(): AsyncStream = this.base.toAsyncInWorker().toAsyncStream(this.position)
fun SyncStreamBase.toAsyncInWorker(): AsyncStreamBase = SyncAsyncStreamBaseInWorker(this)

class SyncAsyncStreamBase(val sync: SyncStreamBase) : AsyncStreamBase() {
	override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int =
		sync.read(position, buffer, offset, len)

	override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) =
		sync.write(position, buffer, offset, len)

	override suspend fun setLength(value: Long) = run { sync.length = value }
	override suspend fun getLength(): Long = sync.length
}

class SyncAsyncStreamBaseInWorker(val sync: SyncStreamBase) : AsyncStreamBase() {
	override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int =
		sync.read(position, buffer, offset, len)

	override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) =
		sync.write(position, buffer, offset, len)

	override suspend fun setLength(value: Long) = run { sync.length = value }
	override suspend fun getLength(): Long = sync.length
}

suspend fun AsyncOutputStream.writeStream(source: AsyncInputStream): Long = source.copyTo(this)
suspend fun AsyncOutputStream.writeFile(source: VfsFile): Long =
	source.openUse(VfsOpenMode.READ) { this@writeFile.writeStream(this) }

suspend fun AsyncInputStream.copyTo(target: AsyncOutputStream, chunkSize: Int = 0x10000): Long {
	// Optimization to reduce suspensions
	if (this is AsyncStream && base is MemoryAsyncStreamBase) {
		target.write(base.data.data, position.toInt(), base.ilength - position.toInt())
		return base.ilength.toLong()
	}

	val chunk = ByteArray(chunkSize)
	var totalCount = 0L
	while (true) {
		val count = this.read(chunk)
		if (count <= 0) break
		target.write(chunk, 0, count)
		totalCount += count
	}
	return totalCount
}

suspend fun AsyncStream.writeToAlign(alignment: Int, value: Int = 0) {
	val nextPosition = getPosition().nextAlignedTo(alignment.toLong())
	val data = ByteArray((nextPosition - getPosition()).toInt())
	data.fill(value.toByte())
	writeBytes(data)
}

suspend fun AsyncStream.skip(count: Int): AsyncStream = this.apply { position += count }
suspend fun AsyncStream.skipToAlign(alignment: Int) = run { position = position.nextAlignedTo(alignment.toLong()) }
suspend fun AsyncStream.truncate() = setLength(position)

suspend fun AsyncOutputStream.writeCharArrayLE(array: CharArray) =
	writeBytes(ByteArray(array.size * 2).apply { writeArrayLE(0, array) })

suspend fun AsyncOutputStream.writeShortArrayLE(array: ShortArray) =
	writeBytes(ByteArray(array.size * 2).apply { writeArrayLE(0, array) })

suspend fun AsyncOutputStream.writeIntArrayLE(array: IntArray) =
	writeBytes(ByteArray(array.size * 4).apply { writeArrayLE(0, array) })

suspend fun AsyncOutputStream.writeLongArrayLE(array: LongArray) =
	writeBytes(ByteArray(array.size * 8).apply { writeArrayLE(0, array) })

suspend fun AsyncOutputStream.writeFloatArrayLE(array: FloatArray) =
	writeBytes(ByteArray(array.size * 4).apply { writeArrayLE(0, array) })

suspend fun AsyncOutputStream.writeDoubleArrayLE(array: DoubleArray) =
	writeBytes(ByteArray(array.size * 8).apply { writeArrayLE(0, array) })

suspend fun AsyncOutputStream.writeCharArrayBE(array: CharArray) =
	writeBytes(ByteArray(array.size * 2).apply { writeArrayBE(0, array) })

suspend fun AsyncOutputStream.writeShortArrayBE(array: ShortArray) =
	writeBytes(ByteArray(array.size * 2).apply { writeArrayBE(0, array) })

suspend fun AsyncOutputStream.writeIntArrayBE(array: IntArray) =
	writeBytes(ByteArray(array.size * 4).apply { writeArrayBE(0, array) })

suspend fun AsyncOutputStream.writeLongArrayBE(array: LongArray) =
	writeBytes(ByteArray(array.size * 8).apply { writeArrayBE(0, array) })

suspend fun AsyncOutputStream.writeFloatArrayBE(array: FloatArray) =
	writeBytes(ByteArray(array.size * 4).apply { writeArrayBE(0, array) })

suspend fun AsyncOutputStream.writeDoubleArrayBE(array: DoubleArray) =
	writeBytes(ByteArray(array.size * 8).apply { writeArrayBE(0, array) })

suspend fun AsyncInputStream.readUntil(endByte: Byte, limit: Int = 0x1000): ByteArray {
	val temp = ByteArray(1)
	val out = ByteArrayBuilder()
	try {
		while (true) {
			val c = run { readExact(temp, 0, 1); temp[0] }
			//val c = readS8().toByte()
			if (c == endByte) break
			out.append(c)
			if (out.size >= limit) break
		}
	} catch (e: EOFException) {
	}
	//println("AsyncInputStream.readUntil: '${out.toString(UTF8).replace('\r', ';').replace('\n', '.')}'")
	return out.toByteArray()
}

suspend fun AsyncInputStream.readLine(eol: Char = '\n', charset: Charset = UTF8): String {
	val temp = ByteArray(1)
	val out = ByteArrayBuilder()
	try {
		while (true) {
			val c = run { readExact(temp, 0, 1); temp[0] }
			//val c = readS8().toByte()
			if (c.toChar() == eol) break
			out.append(c.toByte())
		}
	} catch (e: EOFException) {
	}
	return out.toByteArray().toString(charset)
}


fun SyncInputStream.toAsyncInputStream() = object : AsyncInputStreamWithLength {
	val sync = this@toAsyncInputStream

	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = sync.read(buffer, offset, len)
	override suspend fun close(): Unit = run { (sync as? Closeable)?.close() }
	override suspend fun getPosition(): Long = (sync as? SyncPositionStream)?.position ?: super.getPosition()
	override suspend fun getLength(): Long = (sync as? SyncLengthStream)?.length ?: super.getLength()
}

fun SyncOutputStream.toAsyncOutputStream() = object : AsyncOutputStream {
	override suspend fun write(buffer: ByteArray, offset: Int, len: Int): Unit =
		this@toAsyncOutputStream.write(buffer, offset, len)

	override suspend fun close(): Unit = run { (this@toAsyncOutputStream as? Closeable)?.close() }
}

fun AsyncStream.asVfsFile(name: String = "unknown.bin"): VfsFile = MemoryVfs(
	mapOf(name to this)
)[name]

suspend fun AsyncStream.readAllAsFastStream(offset: Int = 0) = this.readAll().openFastStream()

inline fun AsyncStream.getWrittenRange(callback: () -> Unit): LongRange {
	val start = position
	callback()
	val end = position
	return start until end
}

// Missing methods from Korio's AsyncStream
suspend fun AsyncStream.writeU_VL(value: Int) =
	this.apply { writeBytes(MemorySyncStreamToByteArray { writeU_VL(value) }) }

suspend fun AsyncStream.writeStringVL(str: String, charset: Charset = UTF8) =
	this.apply { writeBytes(MemorySyncStreamToByteArray { writeStringVL(str, charset) }) }

fun AsyncInputStream.withLength(length: Long): AsyncInputStream {
	val base = this
	var currentPos = 0L
	return object : AsyncInputStream by base, AsyncGetLengthStream, AsyncGetPositionStream {
		override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
			val read = base.read(buffer, offset, len)
			if (read >= 0) currentPos += read
			return read
		}

		override suspend fun getPosition(): Long = currentPos
		override suspend fun getLength(): Long = length
	}
}

class MemoryAsyncStreamBase(var data: com.soywiz.kmem.ByteArrayBuilder) : AsyncStreamBase() {
	constructor(initialCapacity: Int = 4096) : this(ByteArrayBuilder(initialCapacity))

	var ilength: Int
		get() = data.size
		set(value) = run { data.size = value }

	override suspend fun setLength(value: Long) = run { ilength = value.toInt() }
	override suspend fun getLength(): Long = ilength.toLong()

	fun checkPosition(position: Long) = run { if (position < 0) invalidOp("Invalid position $position") }

	override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		checkPosition(position)
		if (position !in 0 until ilength) return 0
		val end = min(this.ilength.toLong(), position + len)
		val actualLen = max((end - position).toInt(), 0)
		arraycopy(this.data.data, position.toInt(), buffer, offset, actualLen)
		return actualLen
	}

	override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
		checkPosition(position)
		data.size = max(data.size, (position + len).toInt())
		arraycopy(buffer, offset, this.data.data, position.toInt(), len)

	}

	override suspend fun close() = Unit

	override fun toString(): String = "MemoryAsyncStreamBase(${data.size})"
}

suspend fun asyncStreamWriter(bufferSize: Int = 1024, process: suspend (out: AsyncOutputStream) -> Unit): AsyncInputStream {
	val deque = AsyncByteArrayDeque(bufferSize)

	val job = launchImmediately(coroutineContext) {
		try {
			process(object : AsyncOutputStream {
				override suspend fun write(buffer: ByteArray, offset: Int, len: Int) = deque.write(buffer, offset, len)
				override suspend fun write(byte: Int) = deque.write(byte)
				override suspend fun close() = deque.close()
			})
		} finally {
			deque.close()
		}
	}

	return object : AsyncInputStream {
		override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = deque.read(buffer, offset, len)
		override suspend fun read(): Int = deque.read()
		override suspend fun close() = job.cancel()
	}
}

suspend inline fun AsyncOutputStream.writeSync(hintSize: Int = 4096, callback: SyncStream.() -> Unit) {
	writeBytes(MemorySyncStreamToByteArray(hintSize) {
		callback()
	})
}