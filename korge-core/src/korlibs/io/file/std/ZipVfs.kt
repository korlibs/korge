package korlibs.io.file.std

import korlibs.io.compression.*
import korlibs.io.compression.deflate.Deflate
import korlibs.io.compression.deflate.DeflatePortable
import korlibs.io.compression.zip.ZipBuilder
import korlibs.io.compression.zip.ZipEntry2
import korlibs.io.compression.zip.ZipFile
import korlibs.io.file.Vfs
import korlibs.io.file.VfsFile
import korlibs.io.file.VfsOpenMode
import korlibs.io.file.VfsStat
import korlibs.io.file.fullName
import korlibs.io.lang.FileNotFoundException
import korlibs.io.lang.IOException
import korlibs.io.stream.AsyncStream
import korlibs.io.stream.readBytesExact
import korlibs.io.stream.readS32BE
import korlibs.io.stream.readS32LE
import korlibs.io.stream.readSlice
import korlibs.io.stream.readString
import korlibs.io.stream.readU16LE
import korlibs.io.stream.sliceStart
import korlibs.io.stream.toAsyncStream
import korlibs.io.stream.withLength
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.collections.*

suspend fun ZipVfs(
    s: AsyncStream,
    vfsFile: VfsFile? = null,
    caseSensitive: Boolean = true,
    closeStream: Boolean = false,
    useNativeDecompression: Boolean = true,
    fullName: String? = null,
    compressionMethods: List<CompressionMethod>? = null,
): VfsFile {
    val compressionMethods = (compressionMethods ?: emptyList()) + listOf(CompressionMethod.Uncompressed, if (useNativeDecompression) Deflate else DeflatePortable)
    val algosByName = compressionMethods.associateBy { it.name }

    val algoIdToName = mapOf(0 to "STORE", 8 to "DEFLATE", 14 to "LZMA")

    //val s = zipFile.open(VfsOpenMode.READ)
    val zipFile = ZipFile(s, caseSensitive, fullName ?: vfsFile?.fullName)

    class Impl : Vfs() {
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
                        val algoName = algoIdToName[entry.compressionMethod]
                        val method = algosByName[algoName] ?: TODO("Not implemented zip method ${entry.compressionMethod} with name '$algoName' not provided as compressionMethods")
                        compressedData.uncompressed(method).withLength(entry.uncompressedSize).toAsyncStream()
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

suspend fun VfsFile.openAsZip(caseSensitive: Boolean = true, useNativeDecompression: Boolean = true, compressionMethods: List<CompressionMethod>? = null): VfsFile =
    ZipVfs(this.open(VfsOpenMode.READ), this, caseSensitive = caseSensitive, closeStream = true, useNativeDecompression = useNativeDecompression, compressionMethods = compressionMethods)

suspend fun AsyncStream.openAsZip(caseSensitive: Boolean = true, useNativeDecompression: Boolean = true, compressionMethods: List<CompressionMethod>? = null) =
    ZipVfs(this, caseSensitive = caseSensitive, closeStream = false, useNativeDecompression = useNativeDecompression, compressionMethods = compressionMethods)

suspend fun <R> VfsFile.openAsZip(caseSensitive: Boolean = true, useNativeDecompression: Boolean = true, compressionMethods: List<CompressionMethod>? = null, callback: suspend (VfsFile) -> R): R {
    val file = openAsZip(caseSensitive, useNativeDecompression = useNativeDecompression, compressionMethods = compressionMethods)
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
suspend fun VfsFile.createZipFromTree(
    useFolderAsRoot: Boolean = false, compression: CompressionMethod = CompressionMethod.Uncompressed): ByteArray = ZipBuilder.createZipFromTree(
    this,
    compression,
    useFolderAsRoot
)
suspend fun VfsFile.createZipFromTreeTo(s: AsyncStream, compression: CompressionMethod = CompressionMethod.Uncompressed, useFolderAsRoot: Boolean = false) = ZipBuilder.createZipFromTreeTo(
    this,
    s,
    compression,
    useFolderAsRoot
)
suspend fun VfsFile.createZipFromTreeTo(zipFile: VfsFile, compression: CompressionMethod = CompressionMethod.Uncompressed, useFolderAsRoot: Boolean = true): VfsFile = ZipBuilder.createZipFromTreeTo(
    this,
    zipFile,
    compression,
    useFolderAsRoot
)

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
