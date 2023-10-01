---
permalink: /io/astreams/
group: io
layout: default
title: Async Streams
title_prefix: KorIO
description: "AsyncInputStream, AsyncOutputStream, AsyncStream..."
fa-icon: fa-memory
priority: 2
---

KorIO has functionality to manipulate asynchronous streams of data.



## AsyncBaseStream

```kotlin
interface AsyncBaseStream : AsyncCloseable
```

## AsyncInputStream

```kotlin
interface AsyncInputStream : AsyncBaseStream {
	suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int
	suspend fun read(): Int = default
}

interface AsyncRAInputStream {
	suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int
}

suspend fun AsyncInputStream.readStringz(charset: Charset = UTF8): String
suspend fun AsyncInputStream.readStringz(len: Int, charset: Charset = UTF8): String
suspend fun AsyncInputStream.readString(len: Int, charset: Charset = UTF8): String
suspend fun AsyncInputStream.readExact(buffer: ByteArray, offset: Int, len: Int)
suspend fun AsyncInputStream.read(data: ByteArray): Int
suspend fun AsyncInputStream.read(data: UByteArray): Int
suspend fun AsyncInputStream.readBytesUpToFirst(len: Int): ByteArray
suspend fun AsyncInputStream.readBytesUpTo(len: Int): ByteArray
suspend fun AsyncInputStream.readBytesExact(len: Int): ByteArray
suspend fun AsyncInputStream.readU8(): Int

suspend fun AsyncInputStream.readS8(): Int
suspend fun AsyncInputStream.readU16LE(): Int
suspend fun AsyncInputStream.readU24LE(): Int
suspend fun AsyncInputStream.readU32LE(): Long
suspend fun AsyncInputStream.readS16LE(): Int
suspend fun AsyncInputStream.readS24LE(): Int
suspend fun AsyncInputStream.readS32LE(): Int
suspend fun AsyncInputStream.readS64LE(): Long
suspend fun AsyncInputStream.readF32LE(): Float
suspend fun AsyncInputStream.readF64LE(): Double
suspend fun AsyncInputStream.readU16BE(): Int
suspend fun AsyncInputStream.readU24BE(): Int
suspend fun AsyncInputStream.readU32BE(): Long
suspend fun AsyncInputStream.readS16BE(): Int
suspend fun AsyncInputStream.readS24BE(): Int
suspend fun AsyncInputStream.readS32BE(): Int
suspend fun AsyncInputStream.readS64BE(): Long
suspend fun AsyncInputStream.readF32BE(): Float
suspend fun AsyncInputStream.readF64BE(): Double
suspend fun AsyncInputStream.readAll(): ByteArray
suspend fun AsyncInputStream.readAvailable(): ByteArray

suspend fun AsyncInputStream.skip(count: Int)

suspend fun AsyncInputStream.readUByteArray(count: Int): UByteArray
suspend fun AsyncInputStream.readShortArrayLE(count: Int): ShortArray
suspend fun AsyncInputStream.readShortArrayBE(count: Int): ShortArray
suspend fun AsyncInputStream.readCharArrayLE(count: Int): CharArray
suspend fun AsyncInputStream.readCharArrayBE(count: Int): CharArray
suspend fun AsyncInputStream.readIntArrayLE(count: Int): IntArray
suspend fun AsyncInputStream.readIntArrayBE(count: Int): IntArray
suspend fun AsyncInputStream.readLongArrayLE(count: Int): LongArray
suspend fun AsyncInputStream.readLongArrayBE(count: Int): LongArray
suspend fun AsyncInputStream.readFloatArrayLE(count: Int): FloatArray
suspend fun AsyncInputStream.readFloatArrayBE(count: Int): FloatArray
suspend fun AsyncInputStream.readDoubleArrayLE(count: Int): DoubleArray
suspend fun AsyncInputStream.readDoubleArrayBE(count: Int): DoubleArray
suspend fun AsyncInputStream.copyTo(target: AsyncOutputStream, chunkSize: Int = 0x10000): Long
suspend fun AsyncInputStream.readUntil(endByte: Byte, limit: Int = 0x1000): ByteArray
suspend fun AsyncInputStream.readLine(eol: Char = '\n', charset: Charset = UTF8): String
fun AsyncInputStream.withLength(length: Long): AsyncInputStream

fun AsyncInputStream.bufferedInput(bufferSize: Int = 0x2000): AsyncBufferedInputStream

```

## AsyncInputStreamWithLength

```kotlin
interface AsyncInputStreamWithLength : AsyncInputStream, AsyncGetPositionStream, AsyncGetLengthStream
interface AsyncPositionLengthStream : AsyncPositionStream, AsyncLengthStream

interface AsyncPositionStream : AsyncGetPositionStream {
	suspend fun setPosition(value: Long): Unit = throw UnsupportedOperationException()
}

interface AsyncGetLengthStream : AsyncBaseStream {
	suspend fun getLength(): Long = throw UnsupportedOperationException()
}

interface AsyncGetPositionStream : AsyncBaseStream {
	suspend fun getPosition(): Long = throw UnsupportedOperationException()
}

interface AsyncLengthStream : AsyncGetLengthStream {
	suspend fun setLength(value: Long): Unit = throw UnsupportedOperationException()
}

fun List<AsyncInputStreamWithLength>.combine(): AsyncInputStreamWithLength

operator fun AsyncInputStreamWithLength.plus(other: AsyncInputStreamWithLength): AsyncInputStreamWithLength

suspend fun AsyncInputStreamWithLength.getAvailable(): Long
suspend fun AsyncInputStreamWithLength.hasAvailable(): Boolean

suspend fun AsyncPositionLengthStream.getAvailable(): Long
suspend fun AsyncPositionLengthStream.eof(): Boolean

```

## AsyncOutputStream

```kotlin
interface AsyncOutputStream : AsyncBaseStream {
	suspend fun write(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset)
	suspend fun write(byte: Int) = default
}

interface AsyncRAOutputStream {
	suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int)
}

suspend fun AsyncOutputStream.writeStringz(str: String, charset: Charset = UTF8)
suspend fun AsyncOutputStream.writeStringz(str: String, len: Int, charset: Charset = UTF8)
suspend fun AsyncOutputStream.writeString(string: String, charset: Charset = UTF8): Unit

suspend fun AsyncOutputStream.writeBytes(data: ByteArray): Unit
suspend fun AsyncOutputStream.writeBytes(data: ByteArray, position: Int, length: Int): Unit
suspend fun AsyncOutputStream.write8(v: Int): Unit
suspend fun AsyncOutputStream.write16LE(v: Int): Unit
suspend fun AsyncOutputStream.write24LE(v: Int): Unit
suspend fun AsyncOutputStream.write32LE(v: Int): Unit
suspend fun AsyncOutputStream.write32LE(v: Long): Unit
suspend fun AsyncOutputStream.write64LE(v: Long): Unit
suspend fun AsyncOutputStream.writeF32LE(v: Float): Unit
suspend fun AsyncOutputStream.writeF64LE(v: Double): Unit

suspend fun AsyncOutputStream.write16BE(v: Int): Unit
suspend fun AsyncOutputStream.write24BE(v: Int): Unit
suspend fun AsyncOutputStream.write32BE(v: Int): Unit
suspend fun AsyncOutputStream.write32BE(v: Long): Unit
suspend fun AsyncOutputStream.write64BE(v: Long): Unit
suspend fun AsyncOutputStream.writeF32BE(v: Float): Unit
suspend fun AsyncOutputStream.writeF64BE(v: Double): Unit

suspend fun AsyncOutputStream.writeStream(source: AsyncInputStream): Long
suspend fun AsyncOutputStream.writeFile(source: VfsFile): Long

suspend fun AsyncOutputStream.writeCharArrayLE(array: CharArray)
suspend fun AsyncOutputStream.writeShortArrayLE(array: ShortArray)
suspend fun AsyncOutputStream.writeIntArrayLE(array: IntArray)
suspend fun AsyncOutputStream.writeLongArrayLE(array: LongArray)
suspend fun AsyncOutputStream.writeFloatArrayLE(array: FloatArray)
suspend fun AsyncOutputStream.writeDoubleArrayLE(array: DoubleArray)
suspend fun AsyncOutputStream.writeCharArrayBE(array: CharArray)
suspend fun AsyncOutputStream.writeShortArrayBE(array: ShortArray)
suspend fun AsyncOutputStream.writeIntArrayBE(array: IntArray)
suspend fun AsyncOutputStream.writeLongArrayBE(array: LongArray)
suspend fun AsyncOutputStream.writeFloatArrayBE(array: FloatArray)
suspend fun AsyncOutputStream.writeDoubleArrayBE(array: DoubleArray)

suspend fun asyncStreamWriter(bufferSize: Int = 1024, process: suspend (out: AsyncOutputStream) -> Unit): AsyncInputStream
suspend inline fun AsyncOutputStream.writeSync(hintSize: Int = 4096, callback: SyncStream.() -> Unit)

```

## AsyncBufferedInputStream

```kotlin
fun AsyncStream.buffered(blockSize: Int = 2048, blocksToRead: Int = 0x10): AsyncStream

class BufferedStreamBase(val base: AsyncStreamBase, val blockSize: Int = 2048, val blocksToRead: Int = 0x10) : AsyncStreamBase()

suspend fun AsyncBufferedInputStream.readBufferedLine(limit: Int = 0x1000, charset: Charset = UTF8)

class AsyncBufferedInputStream(val base: AsyncInputStream, val bufferSize: Int = 0x2000) : AsyncInputStream {
	suspend fun require(len: Int = 1)
	suspend fun readUntil(end: Byte, including: Boolean = true, limit: Int = 0x1000): ByteArray
}
```

## AsyncInputOpenable

```kotlin
interface AsyncInputOpenable { suspend fun openRead(): AsyncInputStream }
```

## AsyncBaseStream

```kotlin
fun AsyncBaseStream.toAsyncStream(): AsyncStream

open class AsyncStreamBase : AsyncCloseable, AsyncRAInputStream, AsyncRAOutputStream, AsyncLengthStream

suspend fun AsyncStreamBase.readBytes(position: Long, count: Int): ByteArray
fun AsyncStreamBase.toAsyncStream(position: Long = 0L): AsyncStream


class MemoryAsyncStreamBase(var data: com.soywiz.kmem.ByteArrayBuilder) : AsyncStreamBase() {
	constructor(initialCapacity: Int = 4096)
	var ilength: Int
	fun checkPosition(position: Long)
}

```

## AsyncStream

```kotlin
class AsyncStream(val base: AsyncStreamBase, var position: Long = 0L) : Extra by Extra.Mixin(), AsyncInputStream, AsyncInputStreamWithLength, AsyncOutputStream, AsyncPositionLengthStream,
	AsyncCloseable {
	fun duplicate(): AsyncStream = AsyncStream(base, position)
}

suspend fun AsyncStream.hasLength(): Boolean
suspend fun AsyncStream.hasAvailable(): Boolean

inline fun <T> AsyncStream.keepPosition(callback: () -> T): T

suspend fun AsyncStream.readStream(length: Int): AsyncStream
suspend fun AsyncStream.readStream(length: Long): AsyncStream

suspend fun AsyncStream.writeToAlign(alignment: Int, value: Int = 0)

suspend fun AsyncStream.skip(count: Int): AsyncStream
suspend fun AsyncStream.skipToAlign(alignment: Int)
suspend fun AsyncStream.truncate()

suspend fun AsyncStream.readAllAsFastStream(offset: Int = 0): FastByteArrayInputStream
inline fun AsyncStream.getWrittenRange(callback: () -> Unit): LongRange
suspend fun AsyncStream.writeU_VL(value: Int)
suspend fun AsyncStream.writeStringVL(str: String, charset: Charset = UTF8)
```

### Slicing an AsyncStream

```kotlin
suspend fun AsyncStream.readSlice(length: Long): AsyncStream
suspend fun AsyncStream.sliceWithSize(start: Long, length: Long, closeParent: Boolean = false): AsyncStream
suspend fun AsyncStream.sliceWithSize(start: Int, length: Int, closeParent: Boolean = false): AsyncStream
suspend fun AsyncStream.slice(range: IntRange, closeParent: Boolean = false): AsyncStream
suspend fun AsyncStream.slice(range: LongRange, closeParent: Boolean = false): AsyncStream
suspend fun AsyncStream.sliceWithBounds(start: Long, end: Long, closeParent: Boolean = false): AsyncStream
suspend fun AsyncStream.sliceStart(start: Long = 0L, closeParent: Boolean = false): AsyncStream
suspend fun AsyncStream.sliceHere(closeParent: Boolean = false): AsyncStream

class SliceAsyncStreamBase(
	base: AsyncStreamBase,
	baseStart: Long,
	baseEnd: Long,
	closeParent: Boolean
) : AsyncStreamBase()
```

### Creating an Asynchronous Stream from a ByteArray or String

```kotlin
fun ByteArray.openAsync(mode: String = "r"): AsyncStream
fun String.openAsync(charset: Charset = UTF8): AsyncStream
```


### Convert an Asynchronous Stream into a VfsFile

```kotlin
fun AsyncStream.asVfsFile(name: String = "unknown.bin"): VfsFile
```

### Create an Asynchronous Stream from a Synchronous one

Convert a synchronous stream, into an asynchronous stream.

```kotlin
fun SyncInputStream.toAsyncInputStream(): AsyncInputStreamWithLength
fun SyncOutputStream.toAsyncOutputStream(): AsyncOutputStream

fun SyncStream.toAsync(): AsyncStream
fun SyncStreamBase.toAsync(): AsyncStreamBase

fun SyncStream.toAsyncInWorker(): AsyncStream
fun SyncStreamBase.toAsyncInWorker(): AsyncStreamBase

class SyncAsyncStreamBase(val sync: SyncStreamBase) : AsyncStreamBase()
class SyncAsyncStreamBaseInWorker(val sync: SyncStreamBase) : AsyncStreamBase()
```
