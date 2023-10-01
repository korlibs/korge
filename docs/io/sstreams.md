---
permalink: /io/sstreams/
group: io
layout: default
title: Sync Streams
title_prefix: KorIO
description: "SyncInputStream, SyncOutputStream, SyncStream, FastByteArrayInputStream..."
fa-icon: fa-memory
priority: 1
---

KorIO has functionality to manipulate synchronous streams of data.



## SyncPositionStream

```kotlin
interface SyncPositionStream { var position: Long }
```

## SyncLengthStream

```kotlin
interface SyncLengthStream { var length: Long }
```

## SyncInputStream

```kotlin
interface SyncInputStream : OptionalCloseable {
	fun read(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset): Int
	fun read(): Int = smallBytesPool.alloc2 { if (read(it, 0, 1) > 0) it[0].unsigned else -1 }
}

interface SyncRAInputStream { fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int }

fun SyncInputStream.readStringz(charset: Charset = UTF8): String
fun SyncInputStream.readStringz(len: Int, charset: Charset = UTF8): String
fun SyncInputStream.readString(len: Int, charset: Charset = UTF8): String
fun SyncInputStream.readExact(out: ByteArray, offset: Int, len: Int): Unit
fun SyncInputStream.readExactTo(buffer: ByteArray, offset: Int = 0, length: Int
fun SyncInputStream.read(data: ByteArray): Int
fun SyncInputStream.read(data: UByteArray): Int
fun SyncInputStream.readBytesExact(len: Int): ByteArray
fun SyncInputStream.readBytes(len: Int): ByteArray
fun SyncInputStream.readU8(): Int
fun SyncInputStream.readS8(): Int
fun SyncInputStream.readU16LE(): Int
fun SyncInputStream.readU24LE(): Int
fun SyncInputStream.readU32LE(): Long
fun SyncInputStream.readS16LE(): Int
fun SyncInputStream.readS24LE(): Int
fun SyncInputStream.readS32LE(): Int
fun SyncInputStream.readS64LE(): Long
fun SyncInputStream.readF32LE(): Float
fun SyncInputStream.readF64LE(): Double
fun SyncInputStream.readU16BE(): Int
fun SyncInputStream.readU24BE(): Int
fun SyncInputStream.readU32BE(): Long
fun SyncInputStream.readS16BE(): Int
fun SyncInputStream.readS24BE(): Int
fun SyncInputStream.readS32BE(): Int
fun SyncInputStream.readS64BE(): Long
fun SyncInputStream.readF32BE(): Float
fun SyncInputStream.readF64BE(): Double
fun SyncInputStream.readUByteArray(count: Int): UByteArray
fun SyncInputStream.readShortArrayLE(count: Int): ShortArray
fun SyncInputStream.readShortArrayBE(count: Int): ShortArray
fun SyncInputStream.readCharArrayLE(count: Int): CharArray
fun SyncInputStream.readCharArrayBE(count: Int): CharArray
fun SyncInputStream.readIntArrayLE(count: Int): IntArray
fun SyncInputStream.readIntArrayBE(count: Int): IntArray
fun SyncInputStream.readLongArrayLE(count: Int): LongArray
fun SyncInputStream.readLongArrayBE(count: Int): LongArray
fun SyncInputStream.readFloatArrayLE(count: Int): FloatArray
fun SyncInputStream.readFloatArrayBE(count: Int): FloatArray
fun SyncInputStream.readDoubleArrayLE(count: Int): DoubleArray
fun SyncInputStream.readDoubleArrayBE(count: Int): DoubleArray

// Variable Length

fun SyncInputStream.readU_VL(): Int
fun SyncInputStream.readS_VL(): Int

// Copy

fun SyncInputStream.copyTo(target: SyncOutputStream): Unit

```

## SyncOutputStream

```kotlin
interface SyncOutputStream : OptionalCloseable {
	fun write(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset): Unit
	fun write(byte: Int) = smallBytesPool.alloc2 { it[0] = byte.toByte(); write(it, 0, 1) }
	fun flush() = Unit
}

interface SyncRAOutputStream {
	fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit
	fun flush(): Unit = Unit
}

fun SyncOutputStream.writeString(string: String, charset: Charset = UTF8): Unit
fun SyncOutputStream.writeStringz(str: String, charset: Charset = UTF8)
fun SyncOutputStream.writeStringz(str: String, len: Int, charset: Charset = UTF8)
fun SyncOutputStream.writeBytes(data: ByteArray): Unit
fun SyncOutputStream.writeBytes(data: ByteArray, position: Int, length: Int)

fun SyncOutputStream.write8(v: Int): Unit
fun SyncOutputStream.write16LE(v: Int): Unit
fun SyncOutputStream.write24LE(v: Int): Unit
fun SyncOutputStream.write32LE(v: Int): Unit
fun SyncOutputStream.write32LE(v: Long): Unit
fun SyncOutputStream.write64LE(v: Long): Unit
fun SyncOutputStream.writeF32LE(v: Float): Unit
fun SyncOutputStream.writeF64LE(v: Double): Unit

fun SyncOutputStream.write16BE(v: Int): Unit
fun SyncOutputStream.write24BE(v: Int): Unit
fun SyncOutputStream.write32BE(v: Int): Unit
fun SyncOutputStream.write32BE(v: Long): Unit
fun SyncOutputStream.write64BE(v: Long): Unit
fun SyncOutputStream.writeF32BE(v: Float): Unit
fun SyncOutputStream.writeF64BE(v: Double): Unit
fun SyncOutputStream.writeStream(source: SyncInputStream): Unit
fun SyncOutputStream.writeCharArrayLE(array: CharArray)
fun SyncOutputStream.writeShortArrayLE(array: ShortArray)
fun SyncOutputStream.writeIntArrayLE(array: IntArray)
fun SyncOutputStream.writeLongArrayLE(array: LongArray)
fun SyncOutputStream.writeFloatArrayLE(array: FloatArray)
fun SyncOutputStream.writeDoubleArrayLE(array: DoubleArray)
fun SyncOutputStream.writeCharArrayBE(array: CharArray)
fun SyncOutputStream.writeShortArrayBE(array: ShortArray)
fun SyncOutputStream.writeIntArrayBE(array: IntArray)
fun SyncOutputStream.writeLongArrayBE(array: LongArray)
fun SyncOutputStream.writeFloatArrayBE(array: FloatArray)
fun SyncOutputStream.writeDoubleArrayBE(array: DoubleArray)

// Variable Length

fun SyncOutputStream.writeU_VL(v: Int): Unit
fun SyncOutputStream.writeS_VL(v: Int): Unit
fun SyncOutputStream.writeStringVL(str: String, charset: Charset = UTF8): Unit

```

## SyncStream

```kotlin
class SyncStream(val base: SyncStreamBase, override var position: Long = 0L) : Extra by Extra.Mixin(), Closeable, SyncInputStream, SyncPositionStream, SyncOutputStream, SyncLengthStream {
	val available: Long
	fun clone() = SyncStream(base, position)
}

open class SyncStreamBase : Closeable, SyncRAInputStream, SyncRAOutputStream, SyncLengthStream {
	val smallTemp = ByteArray(16)
	fun read(position: Long): Int
}

inline fun <T> SyncStream.keepPosition(callback: () -> T): T

val SyncStream.hasLength: Boolean
val SyncStream.hasAvailable: Boolean
fun SyncStream.toByteArray(): ByteArray

val SyncStream.eof: Boolean

fun SyncStream.readAvailable(): ByteArray

fun SyncStream.writeToAlign(alignment: Int, value: Int = 0)
fun SyncStream.skip(count: Int): SyncStream
fun SyncStream.skipToAlign(alignment: Int)
fun SyncStream.truncate()
fun SyncStream.readStringVL(charset: Charset = UTF8): String
```

## SyncStream Slicing

```kotlin
fun SyncStream.readStream(length: Int): SyncStream
fun SyncStream.readStream(length: Long): SyncStream

fun SyncStream.readSlice(length: Long): SyncStream
fun SyncStream.sliceStart(start: Long = 0L): SyncStream
fun SyncStream.sliceHere(): SyncStream
fun SyncStream.slice(range: IntRange): SyncStream
fun SyncStream.slice(range: LongRange): SyncStream
fun SyncStream.sliceWithBounds(start: Long, end: Long): SyncStream
fun SyncStream.sliceWithSize(position: Long, length: Long): SyncStream
fun SyncStream.sliceWithSize(position: Int, length: Int): SyncStream

class SliceSyncStreamBase(internal val base: SyncStreamBase, internal val baseStart: Long, internal val baseEnd: Long) :
	SyncStreamBase()
	
fun SyncStreamBase.toSyncStream(position: Long = 0L): SyncStream

```

### Creating a SyncStream from a ByteArray or a String

```String
fun ByteArray.openSync(mode: String = "r"): SyncStream
fun String.openSync(charset: Charset = UTF8): SyncStream
```

## FillSyncStream

```kotlin
class FillSyncStreamBase(val fill: Byte, override var length: Long) : SyncStreamBase()
fun FillSyncStream(fillByte: Int = 0, length: Long = Long.MAX_VALUE)
```

## MemorySyncStream

```kotlin
fun MemorySyncStream(data: ByteArray = EMPTY_BYTE_ARRAY)
fun MemorySyncStream(data: ByteArrayBuilder)
inline fun MemorySyncStreamToByteArray(initialCapacity: Int = 4096, callback: SyncStream.() -> Unit): ByteArray

class MemorySyncStreamBase(var data: ByteArrayBuilder) : SyncStreamBase() {
	constructor(initialCapacity: Int = 4096)
	var ilength: Int
	fun checkPosition(position: Long)
}
```


## FastByteArrayInputStream

```kotlin
fun ByteArray.openFastStream(offset: Int = 0): FastByteArrayInputStream

class FastByteArrayInputStream(val ba: ByteArray, var offset: Int = 0) {
	val length: Int
	val available: Int
	val hasMore: Boolean
	val eof: Boolean

	fun skip(count: Int)
	fun skipToAlign(count: Int)

	fun readS8(): Int
	fun readU8(): Int
	fun readS16LE(): Int
	fun readS16BE(): Int
	fun readU16LE(): Int
	fun readU16BE(): Int
	fun readS24LE(): Int
	fun readS24BE(): Int
	fun readU24LE(): Int
	fun readU24BE(): Int
	fun readS32LE(): Int
	fun readS32BE(): Int
	fun readU32LE(): Int
	fun readU32BE(): Int
	fun readF32LE(): Float
	fun readF32BE(): Float
	fun readF64LE(): Double
	fun readF64BE(): Double
	fun readBytes(count: Int): ByteArray
	fun readShortArrayLE(count: Int): ShortArray
	fun readShortArrayBE(count: Int): ShortArray
	fun readCharArrayLE(count: Int): CharArray
	fun readCharArrayBE(count: Int): CharArray
	fun readIntArrayLE(count: Int): IntArray
	fun readIntArrayBE(count: Int): IntArray
	fun readLongArrayLE(count: Int): LongArray
	fun readLongArrayBE(count: Int): LongArray
	fun readFloatArrayLE(count: Int): FloatArray
	fun readFloatArrayBE(count: Int): FloatArray
	fun readDoubleArrayLE(count: Int): DoubleArray
	fun readDoubleArrayBE(count: Int): DoubleArray
	fun readU_VL(): Int
	fun readS_VL(): Int
	fun readString(len: Int, charset: Charset = UTF8)
	fun readStringz(len: Int, charset: Charset = UTF8): String
	fun readStringz(charset: Charset = UTF8): String
	fun readStringVL(charset: Charset = UTF8): String
}
```
