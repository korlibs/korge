package korlibs.io.compression.lzo

import korlibs.memory.hasFlags
import korlibs.io.compression.CompressionContext
import korlibs.io.compression.CompressionMethod
import korlibs.io.compression.util.BitReader
import korlibs.io.experimental.KorioExperimentalApi
import korlibs.io.lang.UTF8
import korlibs.io.lang.toByteArray
import korlibs.io.stream.AsyncInputStream
import korlibs.io.stream.AsyncOutputStream
import korlibs.io.stream.readAll
import korlibs.io.stream.readBytesExact
import korlibs.io.stream.readS32BE
import korlibs.io.stream.readS32LE
import korlibs.io.stream.readString
import korlibs.io.stream.readU16BE
import korlibs.io.stream.readU8
import korlibs.io.stream.skip
import korlibs.io.stream.write16BE
import korlibs.io.stream.write32BE
import korlibs.io.stream.write32LE
import korlibs.io.stream.write8
import korlibs.io.stream.writeBytes
import korlibs.io.stream.writeString
import korlibs.encoding.hex

// @TODO: We might want to support a raw version without headers?
open class LZO(val headerType: HeaderType = HeaderType.SHORT) : CompressionMethod {
    override val name: String get() = "LZO"

    companion object : LZO(headerType = HeaderType.SHORT);

    enum class HeaderType { NONE, SHORT, LONG }

    data class HeaderLong(
        var version: Int = 4160,
        var libVersion: Int = 8352,
        var versionNeeded: Int = 2368,
        var method: Int = 3,
        var level: Int = 9,
        var flags: Int = 0,
        var filter: Int = 0,
        var mode: Int = 33188,
        var mtime: Int = 1635636518,
        var GMTdiff: Int = 0,
        var name: String = "",
        var checksum: Int = 0,
        var uncompressedSize: Int = 0,
        var compressedSize: Int = 0,
        var checksumUncompressed: Int = 0,
        var checksumCompressed: Int = 0,
    ) {
        companion object {
            val MAGIC = byteArrayOf(0x89.toByte(), 0x4c, 0x5a, 0x4f, 0x00, 0x0d, 0x0a, 0x1a, 0x0a)
        }

        suspend fun read(s: AsyncInputStream) {
            if (s.readBytesExact(MAGIC.size).hex != MAGIC.hex) error("INVALID LZO!")
            version = s.readU16BE()
            libVersion = s.readU16BE()
            versionNeeded = s.readU16BE()
            method = s.readU8()
            level = s.readU8()
            flags = s.readS32BE()
            filter = if (flags hasFlags LzoConstants.F_H_FILTER) s.readS32BE() else 0
            mode = s.readS32BE()
            mtime = s.readS32BE()
            GMTdiff = s.readS32BE()
            val fileNameLength = s.readU8()
            name = s.readString(fileNameLength)
            checksum = s.readS32BE()
            uncompressedSize = s.readS32BE()
            compressedSize = s.readS32BE()
            checksumUncompressed = s.readS32BE()
            checksumCompressed = if (flags hasFlags LzoConstants.F_ADLER32_C || flags hasFlags LzoConstants.F_CRC32_C) s.readS32BE() else 0
        }

        suspend fun write(o: AsyncOutputStream) {
            o.writeBytes(MAGIC)
            o.write16BE(version)
            o.write16BE(libVersion)
            o.write16BE(versionNeeded)
            o.write8(method)
            o.write8(level)
            o.write32BE(flags)
            if (flags hasFlags LzoConstants.F_H_FILTER) o.write32BE(filter)
            o.write32BE(mode)
            o.write32BE(mtime)
            o.write32BE(GMTdiff)
            val nameBytes = name.toByteArray(UTF8)
            o.write8(nameBytes.size)
            o.writeBytes(nameBytes)
            o.write32BE(checksum)
            o.write32BE(uncompressedSize)
            o.write32BE(compressedSize)
            o.write32BE(checksumUncompressed)
            if (flags hasFlags LzoConstants.F_ADLER32_C || flags hasFlags LzoConstants.F_CRC32_C) o.write32BE(checksumUncompressed)
        }
    }

    @KorioExperimentalApi
    override suspend fun uncompress(reader: BitReader, out: AsyncOutputStream) {
        reader.prepareBigChunkIfRequired()

        if (headerType == HeaderType.NONE) error("Unsupported raw (without header) uncompression for now")

        // Small header  version
        when (reader.internalPeekBytes(ByteArray(2)).hex) {
            // https://github.com/korlibs/korio/issues/151
            "4c5a" -> {
                reader.skip(2)
                val uncompressedSize = reader.readS32LE()
                val uncompressed = ByteArray(uncompressedSize)
                val compressedData = reader.readAll()
                val uncompressedWritten = LzoRawDecompressor.decompress(compressedData, 0, compressedData.size, uncompressed, 0, uncompressed.size)
                out.writeBytes(uncompressed, 0, uncompressedWritten)
            }
            else -> {
                val header = HeaderLong().apply { read(reader) }

                val compressedData = reader.readBytesExact(header.compressedSize)
                //println("$header")

                val uncompressed = ByteArray(header.uncompressedSize)
                val uncompressedWritten = LzoRawDecompressor.decompress(compressedData, 0, compressedData.size, uncompressed, 0, uncompressed.size)

                out.writeBytes(uncompressed, 0, uncompressedWritten)
            }
        }
    }

    @KorioExperimentalApi
    override suspend fun compress(i: BitReader, o: AsyncOutputStream, context: CompressionContext) {
        val uncompressedData = i.readAll()
        val compressedData = ByteArray(uncompressedData.size * 2)
        val compressedSize = LzoRawCompressor.compress(uncompressedData, 0, uncompressedData.size, compressedData, 0, compressedData.size)

        when (headerType) {
            HeaderType.NONE -> Unit
            HeaderType.LONG -> HeaderLong(compressedSize = compressedSize, uncompressedSize = uncompressedData.size).write(o)
            HeaderType.SHORT -> {
                o.writeString("LZ")
                o.write32LE(uncompressedData.size)
            }
        }
        o.writeBytes(compressedData, 0, compressedSize)
    }
}
