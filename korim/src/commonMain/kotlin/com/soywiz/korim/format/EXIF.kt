package com.soywiz.korim.format

import com.soywiz.kds.mapDouble
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.stream.*

object EXIF {
    data class Info(
        val orientation: ImageOrientation = ImageOrientation(),
    )

    suspend fun readExifFromJpeg(s: VfsFile): Info = s.openUse { readExifFromJpeg(this) }

    suspend fun readExifFromJpeg(s: AsyncStream): Info {
        if (s.readU16BE() != 0xFFD8) error("Not a JPEG file")
        while (!s.eof()) {
            val sectionType = s.readU16BE()
            val sectionSize = s.readU16BE()
            //println("sectionType=${sectionType.hex}")
            if ((sectionType and 0xFF00) != 0xFF00) error("Probably an invalid JPEG file?")
            val ss = s.readStream(sectionSize - 2)
            when (sectionType) {
                0xFFE1 -> { // APP1
                    return readExif(ss.readAllAsFastStream())
                }
            }
        }
        error("Couldn't find EXIF information")
    }

    fun readExif(s: FastByteArrayInputStream): Info {
        if (s.readString(4, Charsets.LATIN1) != "Exif") error("Not an Exif section")
        s.skip(2)
        s.skip(2)
        val tagMark = s.readU16BE()
        val offsetFirstFID = s.readS32BE() // @TODO: do we need to use this somehow?
        var orientation = ImageOrientation.ORIGINAL

        val nDirEntry = s.readU16BE()
        for (n in 0 until nDirEntry) {
            val tagNumber = s.readU16BE()
            val dataFormat = s.readU16BE()
            val nComponent = s.readS32BE()
            //println("tagNumber=$tagNumber, dataFormat=$dataFormat, nComponent=$nComponent")
            val values = when (dataFormat) {
                DataFormat.UBYTE.id, DataFormat.SBYTE.id -> s.readBytes(nComponent).mapDouble { it.toDouble() }
                DataFormat.STRING.id -> s.readBytes(nComponent).mapDouble { it.toDouble() }
                DataFormat.UNDEFINED.id -> s.readBytes(nComponent).mapDouble { it.toDouble() }
                DataFormat.USHORT.id, DataFormat.SSHORT.id -> s.readShortArrayBE(nComponent).mapDouble { it.toDouble() }
                DataFormat.ULONG.id, DataFormat.SLONG.id -> s.readIntArrayBE(nComponent).mapDouble { it.toDouble() }
                DataFormat.SFLOAT.id -> s.readFloatArrayBE(nComponent).mapDouble { it.toDouble() }
                DataFormat.DFLOAT.id -> s.readIntArrayBE(nComponent).mapDouble { it.toDouble() } // These are offsets to the 8-byte structure (DWORD/DWORD)
                DataFormat.URATIO.id, DataFormat.SRATIO.id -> s.readIntArrayBE(nComponent).mapDouble { it.toDouble() } // These are offsets to the 8-byte structure (DWORD/DWORD)
                else -> error("Invalid data type")
            }
            when (tagNumber) {
                0x112 -> { // Orientation
                    orientation = when (values[0].toInt()) {
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

        return Info(
            orientation = orientation
        )
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
