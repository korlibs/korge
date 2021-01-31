package com.soywiz.korio.compression.zip

import com.soywiz.klock.DateTime
import com.soywiz.kmem.extract
import com.soywiz.kmem.toIntClamp
import com.soywiz.kmem.unsigned
import com.soywiz.korio.file.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.internal.indexOf
import com.soywiz.korio.internal.max2
import com.soywiz.korio.stream.*
import com.soywiz.krypto.encoding.*
import kotlin.math.max

class ZipFile private constructor(
    val dummy: Boolean,
    val s: AsyncStream,
    val caseSensitive: Boolean = true
) {
    val files = LinkedHashMap<String, ZipEntry2>()
    val filesPerFolder = LinkedHashMap<String, MutableMap<String, ZipEntry2>>()

    companion object {
        suspend operator fun invoke(s: AsyncStream, caseSensitive: Boolean = true): ZipFile {
            return ZipFile(false, s, caseSensitive).also { it.read() }
        }

        internal val PK_END = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
    }

    fun normalizeName(name: String) = if (caseSensitive) name.trim('/') else name.trim('/').toLowerCase()

    private suspend fun read() {
        var endBytes = EMPTY_BYTE_ARRAY

        if (s.getLength() <= 8L) throw IllegalArgumentException("Zip file is too small length=${s.getLength()}")

        var pk_endIndex = -1
        val fileLength = s.getLength()

        for (chunkSize in listOf(0x16, 0x100, 0x1000, 0x10000)) {
            val pos = max2(0L, fileLength - chunkSize)
            s.setPosition(pos)
            val bytesLen = max2(chunkSize, s.getAvailable().toIntClamp())
            val ebytes = s.readBytesExact(bytesLen)
            endBytes = ebytes
            pk_endIndex = endBytes.indexOf(PK_END)
            if (pk_endIndex >= 0) break
        }

        if (pk_endIndex < 0) throw IllegalArgumentException("Not a zip file (pk_endIndex < 0) : pk_endIndex=$pk_endIndex : ${endBytes.sliceArray(endBytes.size - 32 until endBytes.size).hex} : ${s.getLength()}")

        val data = endBytes.copyOfRange(pk_endIndex, endBytes.size).openSync()

        @Suppress("UNUSED_VARIABLE")
        data.apply {
            //println(s)
            val magic = readS32BE()
            if (magic != 0x504B_0506) throw IllegalStateException("Not a zip file ${magic.hex} instead of ${0x504B_0102.hex}")
            val diskNumber = readU16LE()
            val startDiskNumber = readU16LE()
            val entriesOnDisk = readU16LE()
            val entriesInDirectory = readU16LE()
            val directorySize = readS32LE()
            val directoryOffset = readS32LE()
            val commentLength = readU16LE()

            //println("Zip: $entriesInDirectory")

            val ds = s.sliceWithSize(directoryOffset.toLong(), directorySize.toLong()).readAvailable().openSync()

            for (n in 0 until entriesInDirectory) {
                ds.apply {
                    val magic = readS32BE()
                    if (magic != 0x504B_0102) throw IllegalStateException("Not a zip file record ${magic.hex} instead of ${0x504B_0102.hex}")
                    val versionMade = readU16LE()
                    val versionExtract = readU16LE()
                    val flags = readU16LE()
                    val compressionMethod = readU16LE()
                    val fileTime = readU16LE()
                    val fileDate = readU16LE()
                    val crc = readS32LE()
                    val compressedSize = readS32LE()
                    val uncompressedSize = readS32LE()
                    val fileNameLength = readU16LE()
                    val extraLength = readU16LE()
                    val fileCommentLength = readU16LE()
                    val diskNumberStart = readU16LE()
                    val internalAttributes = readU16LE()
                    val externalAttributes = readS32LE()
                    val headerOffset = readU32LE()
                    val name = readString(fileNameLength)
                    val extra = readBytes(extraLength)

                    val isDirectory = name.endsWith("/")
                    val normalizedName = normalizeName(name)

                    val baseFolder = normalizedName.substringBeforeLast('/', "")
                    val baseName = normalizedName.substringAfterLast('/')

                    val folder = filesPerFolder.getOrPut(baseFolder) { LinkedHashMap() }

                    val headerEntry: AsyncStream = s.sliceStart(headerOffset)

                    val entry = ZipEntry2(
                        path = name,
                        compressionMethod = compressionMethod,
                        isDirectory = isDirectory,
                        time = DosFileDateTime(fileTime, fileDate),
                        inode = n.toLong(),
                        offset = headerOffset.toInt(),
                        headerEntry = headerEntry,
                        compressedSize = compressedSize.unsigned,
                        uncompressedSize = uncompressedSize.unsigned
                    )
                    val components = listOf("") + PathInfo(normalizedName).getPathFullComponents()
                    for (m in 1 until components.size) {
                        val f = components[m - 1]
                        val c = components[m]
                        if (c !in files) {
                            val folder2 = filesPerFolder.getOrPut(f) { LinkedHashMap() }
                            val entry2 = ZipEntry2(
                                path = c,
                                compressionMethod = 0,
                                isDirectory = true,
                                time = DosFileDateTime(0, 0),
                                inode = 0L,
                                offset = 0,
                                headerEntry = byteArrayOf().openAsync(),
                                compressedSize = 0L,
                                uncompressedSize = 0L
                            )
                            folder2[PathInfo(c).baseName] = entry2
                            files[c] = entry2
                        }
                    }
                    //println(components)
                    folder[baseName] = entry
                    files[normalizedName] = entry
                }
            }

            files[""] = ZipEntry2(
                path = "",
                compressionMethod = 0,
                isDirectory = true,
                time = DosFileDateTime(0, 0),
                inode = 0L,
                offset = 0,
                headerEntry = byteArrayOf().openAsync(),
                compressedSize = 0L,
                uncompressedSize = 0L
            )
            Unit
        }

    }
}

data class DosFileDateTime(var dosTime: Int, var dosDate: Int) {
    val seconds: Int get() = 2 * dosTime.extract(0, 5)
    val minutes: Int get() = dosTime.extract(5, 6)
    val hours: Int get() = dosTime.extract(11, 5)
    val day: Int get() = dosDate.extract(0, 5)
    val month1: Int get() = dosDate.extract(5, 4)
    val fullYear: Int get() = 1980 + dosDate.extract(9, 7)
    val utc: DateTime = DateTime.createAdjusted(fullYear, month1, day, hours, minutes, seconds)
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

