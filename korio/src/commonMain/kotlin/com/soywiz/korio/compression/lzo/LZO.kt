package com.soywiz.korio.compression.lzo

import com.soywiz.kmem.*
import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.util.*
import com.soywiz.korio.experimental.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.krypto.encoding.*

// @TODO: We might want to support a raw version without headers?
open class LZO(val includeHeader: Boolean) : CompressionMethod {
    companion object : LZO(includeHeader = true) {
        val MAGIC = byteArrayOf(0x89.toByte(), 0x4c, 0x5a, 0x4f, 0x00, 0x0d, 0x0a, 0x1a, 0x0a)
    }

    data class Header(
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

        if (!includeHeader) error("Unsupported raw (without header) uncompression for now")

        val header = Header().apply { read(reader) }

        val compressedData = reader.readBytesExact(header.compressedSize)
        //println("$header")

        val uncompressed = ByteArray(header.uncompressedSize)
        val uncompressedWritten = LzoRawDecompressor.decompress(compressedData, 0, compressedData.size, uncompressed, 0, uncompressed.size)

        out.writeBytes(uncompressed, 0, uncompressedWritten)
    }

    @KorioExperimentalApi
    override suspend fun compress(i: BitReader, o: AsyncOutputStream, context: CompressionContext) {
        val uncompressedData = i.readAll()
        val compressedData = ByteArray(uncompressedData.size * 2)
        val compressedSize = LzoRawCompressor.compress(uncompressedData, 0, uncompressedData.size, compressedData, 0, compressedData.size)

        if (includeHeader) {
            val header = Header(compressedSize = compressedSize, uncompressedSize = uncompressedData.size)
            header.write(o)
        }
        o.writeBytes(compressedData)
    }
}


