package korlibs.io.compression.zip

import korlibs.memory.ByteArrayBuilder
import korlibs.io.file.VfsFile
import korlibs.io.file.fullName
import korlibs.io.file.std.createZipFromTreeTo
import korlibs.io.lang.UTF8
import korlibs.io.lang.toByteArray
import korlibs.io.stream.AsyncStream
import korlibs.io.stream.MemorySyncStream
import korlibs.io.stream.toAsync
import korlibs.io.stream.write16LE
import korlibs.io.stream.write32LE
import korlibs.io.stream.writeBytes
import korlibs.io.stream.writeFile
import korlibs.io.stream.writeString
import korlibs.io.stream.writeSync
import korlibs.io.util.checksum.CRC32
import korlibs.io.util.checksum.checksum

class ZipBuilder {
    companion object {
        suspend fun createZipFromTree(file: VfsFile): ByteArray {
            val buf = ByteArrayBuilder()
            val mem = MemorySyncStream(buf)
            file.createZipFromTreeTo(mem.toAsync())
            return buf.toByteArray()
        }

        suspend fun createZipFromTreeTo(file: VfsFile, s: AsyncStream) {
            val entries = arrayListOf<ZipEntry>()
            ZipBuilder.addZipFileEntryTree(s, file, entries)
            val directoryStart = s.position

            for (entry in entries) {
                ZipBuilder.addDirEntry(s, entry)
            }
            val directoryEnd = s.position
            val comment = byteArrayOf()

            s.writeSync {
                writeString("PK\u0005\u0006")
                write16LE(0)
                write16LE(0)
                write16LE(entries.size)
                write16LE(entries.size)
                write32LE((directoryEnd - directoryStart).toInt())
                write32LE(directoryStart.toInt())
                write16LE(comment.size)
                writeBytes(comment)
            }
        }

        suspend fun addZipFileEntry(s: AsyncStream, entry: VfsFile): ZipEntry {
            val size = entry.size().toInt()
            val versionMadeBy = 0x314
            val extractVersion = 10
            val flags = 2048
            //val compressionMethod = 8 // Deflate
            val compressionMethod = 0 // Store
            val date = 0
            val time = 0
            val crc32 = entry.checksum(CRC32)
            val name = entry.fullName.trim('/')
            val nameBytes = name.toByteArray(UTF8)
            val extraBytes = byteArrayOf()
            val compressedSize = size
            val uncompressedSize = size

            val headerOffset = s.position
            s.writeSync {
                writeString("PK\u0003\u0004")
                write16LE(extractVersion)
                write16LE(flags)
                write16LE(compressionMethod)
                write16LE(date)
                write16LE(time)
                write32LE(crc32)
                write32LE(compressedSize)
                write32LE(uncompressedSize)
                write16LE(nameBytes.size)
                write16LE(extraBytes.size)
                writeBytes(nameBytes)
                writeBytes(extraBytes)
            }
            s.writeFile(entry)

            return ZipEntry(
                versionMadeBy = versionMadeBy,
                extractVersion = extractVersion,
                headerOffset = headerOffset,
                compressionMethod = compressionMethod,
                flags = flags,
                date = date,
                time = time,
                crc32 = crc32,
                compressedSize = compressedSize,
                uncompressedSize = uncompressedSize,
                nameBytes = nameBytes,
                extraBytes = extraBytes,
                commentBytes = byteArrayOf(),
                diskNumberStart = 0,
                internalAttributes = 0,
                externalAttributes = 0
            )
        }

        suspend fun addZipFileEntryTree(s: AsyncStream, entry: VfsFile, entries: MutableList<ZipEntry>) {
            if (entry.isDirectory()) {
                entry.list().collect { addZipFileEntryTree(s, it, entries) }
            } else {
                entries += addZipFileEntry(s, entry)
            }
        }

        suspend fun addDirEntry(s: AsyncStream, e: ZipEntry) {
            s.writeSync {
                writeString("PK\u0001\u0002")
                write16LE(e.versionMadeBy)
                write16LE(e.extractVersion)
                write16LE(e.flags)
                write16LE(e.compressionMethod)
                write16LE(e.date)
                write16LE(e.time)
                write32LE(e.crc32)
                write32LE(e.compressedSize)
                write32LE(e.uncompressedSize)
                write16LE(e.nameBytes.size)
                write16LE(e.extraBytes.size)
                write16LE(e.commentBytes.size)
                write16LE(e.diskNumberStart)
                write16LE(e.internalAttributes)
                write32LE(e.externalAttributes)
                write32LE(e.headerOffset.toInt())
                writeBytes(e.nameBytes)
                writeBytes(e.extraBytes)
                writeBytes(e.commentBytes)
            }
        }
    }
}
