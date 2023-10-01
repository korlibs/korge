---
permalink: /io/compression/
group: io
layout: default
title: Compression
title_prefix: KorIO
description: "Compression (zlib, deflate, lzma, zip) and checksum tools (adler32, crc32)"
fa-icon: fa-file-archive
priority: 6
---

KorIO has some compression and checksum tools.




## Checksum

```kotlin
fun SimpleChecksum.compute(data: ByteArray, offset: Int = 0, len: Int = data.size - offset): Int
fun ByteArray.checksum(checksum: SimpleChecksum): Int

fun SyncInputStream.checksum(checksum: SimpleChecksum): Int
suspend fun AsyncInputStream.checksum(checksum: SimpleChecksum): Int
suspend fun AsyncInputOpenable.checksum(checksum: SimpleChecksum): Int

interface SimpleChecksum {
	val initialValue: Int
	fun update(old: Int, data: ByteArray, offset: Int = 0, len: Int = data.size - offset): Int
}
```

### Standard (Adler32, CRC32)

```kotlin
com.soywiz.korio.util.checksum.Adler32
com.soywiz.korio.util.checksum.CRC32
```


{:#compression}
## Compression

Korio includes Deflate, GZIP, ZLib and LZMA compressions out of the box working on all the targets.

It defines a `CompressionMethod` interface:

```kotlin
// Asynchronous API
interface CompressionMethod {
    suspend fun uncompress(reader: BitReader, out: AsyncOutputStream): Unit
    suspend fun compress(i: BitReader, o: AsyncOutputStream, context: CompressionContext): Unit
}

// Synchronous API
fun CompressionMethod.uncompress(i: SyncInputStream, o: SyncOutputStream)
fun CompressionMethod.compress(i: SyncInputStream, o: SyncOutputStream, context: CompressionContext)
```

It also provides several extension methods for compressing/decompressing data:

```kotlin
fun ByteArray.uncompress(method: CompressionMethod, outputSizeHint: Int): ByteArray
fun ByteArray.compress(method: CompressionMethod, context: CompressionContext, outputSizeHint: Int): ByteArray

suspend fun AsyncInputStreamWithLength.uncompressed(method: CompressionMethod): AsyncInputStream
suspend fun AsyncInputStreamWithLength.compressed(method: CompressionMethod, context: CompressionContext): AsyncInputStream
```

{:#compression_standard}
### Standard (Deflate, GZIP, ZLib and LZMA)

And the CompressionMethod singletons/classes:

```kotlin
com.soywiz.korio.compression.deflate.Deflate
com.soywiz.korio.compression.deflate.GZIP
com.soywiz.korio.compression.deflate.GZIPNoCrc
com.soywiz.korio.compression.deflate.ZLib
com.soywiz.korio.compression.lzma.Lzma
```

## Zip

### ZipFile

```kotlin
class ZipFile {
    val s: AsyncStream,
    val caseSensitive: Boolean = true
    val files = LinkedHashMap<String, ZipEntry2>()
    val filesPerFolder = LinkedHashMap<String, MutableMap<String, ZipEntry2>>()

    companion object {
        suspend operator fun invoke(s: AsyncStream, caseSensitive: Boolean = true): ZipFile
    }

    fun normalizeName(name: String) = if (caseSensitive) name.trim('/') else name.trim('/').toLowerCase()
}

data class DosFileDateTime(var dosTime: Int, var dosDate: Int) {
    val seconds: Int
    val minutes: Int
    val hours: Int
    val day: Int
    val month1: Int
    val fullYear: Int
    val utc: DateTime
}

data class ZipEntry(
    val versionMadeBy: Int,
    val extractVersion: Int,
    val headerOffset: Long,
    val compressionMethod: Int,
    val flags: Int,
    val date: Int,
    val time: Int,
    val crc32: Int,
    val compressedSize: Int,
    val uncompressedSize: Int,
    val nameBytes: ByteArray,
    val extraBytes: ByteArray,
    val diskNumberStart: Int,
    val internalAttributes: Int,
    val externalAttributes: Int,
    val commentBytes: ByteArray
)

data class ZipEntry2(
    val path: String,
    val compressionMethod: Int,
    val isDirectory: Boolean,
    val time: DosFileDateTime,
    val offset: Int,
    val inode: Long,
    val headerEntry: AsyncStream,
    val compressedSize: Long,
    val uncompressedSize: Long
)
```

### ZipVfs

There is a ZipVfs (check the File System for more details).

```kotlin
suspend fun VfsFile.openAsZip(caseSensitive: Boolean = true, useNativeDecompression: Boolean = true): VfsFile
suspend fun AsyncStream.openAsZip(caseSensitive: Boolean = true, useNativeDecompression: Boolean = true): VfsFile
suspend fun <R> VfsFile.openAsZip(caseSensitive: Boolean = true, useNativeDecompression: Boolean = true, callback: suspend (VfsFile) -> R): R
suspend fun <R> AsyncStream.openAsZip(caseSensitive: Boolean = true, useNativeDecompression: Boolean = true, callback: suspend (VfsFile) -> R): R
```

## ZipBuilder

```kotlin
object ZipBuilder {
	suspend fun createZipFromTree(file: VfsFile): ByteArray
	suspend fun createZipFromTreeTo(file: VfsFile, s: AsyncStream)
	suspend fun addZipFileEntry(s: AsyncStream, entry: VfsFile): ZipEntry
	suspend fun addZipFileEntryTree(s: AsyncStream, entry: VfsFile, entries: MutableList<ZipEntry>)
	suspend fun addDirEntry(s: AsyncStream, e: ZipEntry)
}
```

### Creating a .zip file from a VfsFile

```kotlin
suspend fun VfsFile.createZipFromTree(): ByteArray
suspend fun VfsFile.createZipFromTreeTo(s: AsyncStream)
```
