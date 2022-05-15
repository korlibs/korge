package com.soywiz.korio.file.std

import com.soywiz.korio.compression.deflate.Deflate
import com.soywiz.korio.compression.deflate.DeflatePortable
import com.soywiz.korio.compression.uncompressed
import com.soywiz.korio.compression.zip.ZipBuilder
import com.soywiz.korio.compression.zip.ZipEntry2
import com.soywiz.korio.compression.zip.ZipFile
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.VfsOpenMode
import com.soywiz.korio.file.VfsStat
import com.soywiz.korio.file.VfsV2
import com.soywiz.korio.file.fullName
import com.soywiz.korio.lang.FileNotFoundException
import com.soywiz.korio.lang.IOException
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readBytesExact
import com.soywiz.korio.stream.readS32BE
import com.soywiz.korio.stream.readS32LE
import com.soywiz.korio.stream.readSlice
import com.soywiz.korio.stream.readString
import com.soywiz.korio.stream.readU16LE
import com.soywiz.korio.stream.sliceStart
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.stream.withLength
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.collections.*

suspend fun ZipVfs(
    s: AsyncStream,
    vfsFile: VfsFile? = null,
    caseSensitive: Boolean = true,
    closeStream: Boolean = false,
    useNativeDecompression: Boolean = true,
    fullName: String? = null
): VfsFile {
    //val s = zipFile.open(VfsOpenMode.READ)
    val zipFile = ZipFile(s, caseSensitive, fullName ?: vfsFile?.fullName)

    class Impl : VfsV2() {
        val vfs = this

        override suspend fun close() {
            if (closeStream) {
                s.close()
            }
        }

        override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
            //println("[ZipVfs].open[1]")
            val entry = zipFile.files[zipFile.normalizeName(path)] ?: throw FileNotFoundException("Path: '$path'")
            if (entry.isDirectory) throw IOException("Can't open a zip directory for $mode")
            val base = entry.headerEntry.sliceStart()
            //println("[ZipVfs].open[2]")
            @Suppress("UNUSED_VARIABLE")
            return base.run {
                if (this.getAvailable() < 16) throw IllegalStateException("Chunk to small to be a ZIP chunk")
                if (readS32BE() != 0x504B_0304) throw IllegalStateException("Not a zip file (readS32BE() != 0x504B_0304)")
                //println("[ZipVfs].open[3]")
                val version = readU16LE()
                val flags = readU16LE()
                val compressionType = readU16LE()
                val fileTime = readU16LE()
                val fileDate = readU16LE()
                val crc = readS32LE()
                val compressedSize = readS32LE()
                val uncompressedSize = readS32LE()
                val fileNameLength = readU16LE()
                val extraLength = readU16LE()
                val name = readString(fileNameLength)
                val extra = readBytesExact(extraLength)
                val compressedData = readSlice(entry.compressedSize)

                //println("[ZipVfs].open[4]")

                when (entry.compressionMethod) {
                    0 -> compressedData
                    else -> {
                        //println("[ZipVfs].open[5]")
                        val method = when (entry.compressionMethod) {
                            8 -> if (useNativeDecompression) Deflate else DeflatePortable
                            else -> TODO("Not implemented zip method ${entry.compressionMethod}")
                        }
                        compressedData.uncompressed(method).withLength(uncompressedSize.toLong()).toAsyncStream()
                        //val compressed = compressedData.uncompressed(method).readAll()
                        //val compressed = compressedData.readAll().uncompress(method)
                        //if (crc != 0) {
                        //    val computedCrc = CRC32.compute(compressed)
                        //    if (computedCrc != crc) error("Uncompressed file crc doesn't match: expected=${crc.hex}, actual=${computedCrc.hex}")
                        //}
                        //compressed.openAsync()
                    }
                }
            }
        }

        override suspend fun stat(path: String): VfsStat {
            return zipFile.files[zipFile.normalizeName(path)].toStat(this@Impl[path])
        }

        override suspend fun listFlow(path: String): Flow<VfsFile> = flow {
            for ((_, entry) in zipFile.filesPerFolder[zipFile.normalizeName(path)] ?: LinkedHashMap()) {
                //yield(entry.toStat(this@Impl[entry.path]))
                emit(vfs[entry.path])
            }
        }

        override fun toString(): String = "ZipVfs($zipFile)"
    }

    return Impl().root
}

suspend fun VfsFile.openAsZip(caseSensitive: Boolean = true, useNativeDecompression: Boolean = true): VfsFile =
    ZipVfs(this.open(VfsOpenMode.READ), this, caseSensitive = caseSensitive, closeStream = true, useNativeDecompression = useNativeDecompression)

suspend fun AsyncStream.openAsZip(caseSensitive: Boolean = true, useNativeDecompression: Boolean = true) =
    ZipVfs(this, caseSensitive = caseSensitive, closeStream = false, useNativeDecompression = useNativeDecompression)

suspend fun <R> VfsFile.openAsZip(caseSensitive: Boolean = true, useNativeDecompression: Boolean = true, callback: suspend (VfsFile) -> R): R {
    val file = openAsZip(caseSensitive, useNativeDecompression = useNativeDecompression)
    try {
        return callback(file)
    } finally {
        file.vfs.close()
    }
}

suspend fun <R> AsyncStream.openAsZip(caseSensitive: Boolean = true, useNativeDecompression: Boolean = true, callback: suspend (VfsFile) -> R): R {
    val file = openAsZip(caseSensitive, useNativeDecompression = useNativeDecompression)
    try {
        return callback(file)
    } finally {
        //file.vfs.close()
    }
}

/**
 * @TODO: Kotlin.JS BUG:
 * ReferenceError: $this$ is not defined
 * at Coroutine$await$lambda.local$this$await (korio.js:38946:37)
 * at Coroutine$await$lambda.doResume (korio.js:626:34)
 * at file:///Users/soywiz/projects/korlibs/korio/build/node_modules/korio.js:603:25
 */
suspend fun VfsFile.createZipFromTree(): ByteArray = ZipBuilder.createZipFromTree(this)
suspend fun VfsFile.createZipFromTreeTo(s: AsyncStream) = ZipBuilder.createZipFromTreeTo(this, s)

private fun ZipEntry2?.toStat(file: VfsFile): VfsStat {
    val vfs = file.vfs
    return if (this != null) {
        vfs.createExistsStat(
            file.path,
            isDirectory = isDirectory,
            size = uncompressedSize,
            inode = inode,
            createTime = this.time.utc
        )
    } else {
        vfs.createNonExistsStat(file.path)
    }
}
