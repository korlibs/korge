package com.soywiz.korim.format

import com.soywiz.kds.mapDouble
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.stream.*
import com.soywiz.krypto.encoding.*

// https://zpl.fi/exif-orientation-in-different-formats/
object EXIF {
    suspend fun readExifFromJpeg(s: VfsFile, info: ImageInfo = ImageInfo()): ImageInfo = s.openUse(VfsOpenMode.READ) {
        readExifFromJpeg(this, info)
    }

    suspend fun readExifFromJpeg(s: AsyncStream, info: ImageInfo = ImageInfo()): ImageInfo {
        val jpegHeader = s.readU16BE()
        if (jpegHeader != 0xFFD8) error("Not a JPEG file ${jpegHeader.hex}")
        while (!s.eof()) {
            val sectionType = s.readU16BE()
            val sectionSize = s.readU16BE()
            //Console.error("sectionType=${sectionType.hex}, sectionSize=${sectionSize.hex}")
            if ((sectionType and 0xFF00) != 0xFF00) error("Probably an invalid JPEG file? ${sectionType.hex}")
            val ss = s.readStream(sectionSize - 2)
            when (sectionType) {
                0xFFE1 -> { // APP1
                    readExif(ss.readAllAsFastStream(), info)
                }
                0xFFC0 -> { // SOF0
                    val precision = ss.readU8()
                    info.width = ss.readU16BE()
                    info.height = ss.readU16BE()
                    info.bitsPerPixel = 24
                }
                0xFFDA -> { // SOS (starts data)
                    // END HERE, we don't want to read the data itself
                    return info
                }
            }
        }
        error("Couldn't find EXIF information")
    }

    fun readExif(s: FastByteArrayInputStream, info: ImageInfo = ImageInfo()): ImageInfo {
        return runBlockingNoSuspensions { readExif(s.toAsyncStream(), info) }
    }

    suspend fun readExif(s: AsyncStream, info: ImageInfo = ImageInfo()): ImageInfo {
        if (s.readString(4, Charsets.LATIN1) != "Exif") error("Not an Exif section")
        s.skip(2)
        return readExifBase(s, info)
    }

    suspend fun readExifBase(ss: AsyncStream, info: ImageInfo = ImageInfo()): ImageInfo {
        val endian = if (ss.readString(2, Charsets.LATIN1) == "MM") Endian.BIG_ENDIAN else Endian.LITTLE_ENDIAN
        val s = ss.sliceHere()
        val tagMark = s.readU16(endian)
        val offsetFirstFID = s.readS32(endian) // @TODO: do we need to use this somehow?

        val nDirEntry = s.readU16(endian)
        //Console.error("nDirEntry=$nDirEntry, tagMark=$tagMark, offsetFirstFID=$offsetFirstFID")
        for (n in 0 until nDirEntry) {
            val tagNumber = s.readU16(endian)
            val dataFormat = s.readU16(endian)
            val nComponent = s.readS32(endian)
            //Console.error("tagNumber=$tagNumber, dataFormat=$dataFormat, nComponent=$nComponent")
            val values = when (dataFormat) {
                DataFormat.UBYTE.id, DataFormat.SBYTE.id -> s.readBytesExact(nComponent).mapDouble { it.toDouble() }
                DataFormat.STRING.id -> s.readBytesExact(nComponent).mapDouble { it.toDouble() }
                DataFormat.UNDEFINED.id -> s.readBytesExact(nComponent).mapDouble { it.toDouble() }
                DataFormat.USHORT.id, DataFormat.SSHORT.id -> s.readShortArray(nComponent, endian).mapDouble { it.toDouble() }
                DataFormat.ULONG.id, DataFormat.SLONG.id -> s.readIntArray(nComponent, endian).mapDouble { it.toDouble() }
                DataFormat.SFLOAT.id -> s.readFloatArray(nComponent, endian).mapDouble { it.toDouble() }
                DataFormat.DFLOAT.id -> s.readIntArray(nComponent, endian).mapDouble { it.toDouble() } // These are offsets to the 8-byte structure (DWORD/DWORD)
                DataFormat.URATIO.id, DataFormat.SRATIO.id -> s.readIntArray(nComponent, endian).mapDouble { it.toDouble() } // These are offsets to the 8-byte structure (DWORD/DWORD)
                else -> error("Invalid data type: ${dataFormat.hex}")
            }
            when (tagNumber) {
                0x112 -> { // Orientation
                    info.orientation = when (values[0].toInt()) {
                        1 -> ImageOrientation.ORIGINAL
                        2 -> ImageOrientation.MIRROR_HORIZONTAL
                        3 -> ImageOrientation.ROTATE_180
                        4 -> ImageOrientation.MIRROR_VERTICAL
                        5 -> ImageOrientation.MIRROR_HORIZONTAL_ROTATE_270
                        6 -> ImageOrientation.ROTATE_90
                        7 -> ImageOrientation.MIRROR_HORIZONTAL_ROTATE_90
                        8 -> ImageOrientation.ROTATE_270
                        else -> ImageOrientation.ORIGINAL
                    }
                }
            }
            //AsyncStream().skipToAlign()
            s.skipToAlign(4)
        }
        return info
    }

    enum class DataFormat(
        val id: Int,
        val nBytes: Int,
        val rBytes: Int = nBytes
    ) {
        UBYTE(1, 1),
        STRING(2, 1),
        USHORT(3, 2),
        ULONG(4, 3),
        URATIO(5, 4, 8),
        SBYTE(6, 1),
        UNDEFINED(7, 1),
        SSHORT(8, 2),
        SLONG(9, 4),
        SRATIO(10, 4, 8),
        SFLOAT(11, 4),
        DFLOAT(12, 8);
    }
}
