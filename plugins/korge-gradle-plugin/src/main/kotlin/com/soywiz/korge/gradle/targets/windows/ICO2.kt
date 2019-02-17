package com.soywiz.korge.gradle.targets.windows

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.stream.*
import kotlin.math.*

@Suppress("UNUSED_VARIABLE")
object ICO2 : ImageFormat("ico") {
    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
        if (s.readU16LE() != 0) return null
        if (s.readU16LE() != 1) return null
        val count = s.readU16LE()
        if (count >= 1000) return null
        return ImageInfo()
    }

    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
        data class DirEntry(
            val width: Int, val height: Int,
            val colorCount: Int,
            val reserved: Int,
            val planes: Int,
            val bitCount: Int,
            val size: Int,
            val offset: Int
        )

        fun readDirEntry() = DirEntry(
            width = s.readU8(),
            height = s.readU8(),
            colorCount = s.readU8(),
            reserved = s.readU8(),
            planes = s.readU16LE(),
            bitCount = s.readU16LE(),
            size = s.readS32LE(),
            offset = s.readS32LE()
        )

        fun readBitmap(e: DirEntry, s: SyncStream): Bitmap {
            val tryPNGHead = s.sliceStart().readU32BE()
            if (tryPNGHead == 0x89_50_4E_47L) return PNG.decode(
                s.sliceStart(),
                props.copy(filename = "${props.filename}.png")
            )
            val headerSize = s.readS32LE()
            val width = s.readS32LE()
            val height = s.readS32LE()
            val planes = s.readS16LE()
            val bitCount = s.readS16LE()
            val compression = s.readS32LE()
            val imageSize = s.readS32LE()
            val pixelsXPerMeter = s.readS32LE()
            val pixelsYPerMeter = s.readS32LE()
            val clrUsed = s.readS32LE()
            val clrImportant = s.readS32LE()
            var palette = RgbaArray(0)
            if (compression != 0) throw UnsupportedOperationException("Not supported compressed .ico")
            if (bitCount <= 8) {
                val colors = if (clrUsed == 0) 1 shl bitCount else clrUsed
                palette = (0 until colors).map {
                    val b = s.readU8()
                    val g = s.readU8()
                    val r = s.readU8()
                    val reserved = s.readU8()
                    RGBA(r, g, b, 0xFF)
                }.toRgbaArray()
            }

            val stride = (e.width * bitCount) / 8
            val data = s.readBytes(stride * e.height)

            return when (bitCount) {
                4 -> Bitmap4(e.width, e.height, data, palette)
                8 -> Bitmap8(e.width, e.height, data, palette)
                32 -> Bitmap32(e.width, e.height).writeDecoded(BGRA, data)
                else -> throw UnsupportedOperationException("Unsupported bitCount: $bitCount")
            }
        }

        val reserved = s.readU16LE()
        val type = s.readU16LE()
        val count = s.readU16LE()
        val entries = (0 until count).map { readDirEntry() }
        val bitmaps = arrayListOf<Bitmap>()
        for (e in entries) {
            val bmp = readBitmap(e, s.sliceWithSize(e.offset.toLong(), e.size.toLong()))
            bmp.flipY()
            bitmaps += bmp
        }
        return ImageData(bitmaps.map { ImageFrame(it, main = false) })
    }

    // https://en.wikipedia.org/wiki/ICO_(file_format)
    override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
        // 6
        s.write16LE(0)
        s.write16LE(1) // ICO
        s.write16LE(image.frames.size)

        val payloadStart = 6 + 16 * image.frames.size
        val payloadData = MemorySyncStream()

        // 16 per entry
        for (frame in image.frames) {
            val bitmap = frame.bitmap
            val width = bitmap.width
            val height = bitmap.height
            if (width > 256 || height > 256) error("Size too big for ICO image: ${frame.bitmap.size}")

            s.write8(width)
            s.write8(height)
            s.write8(0) // Palette size
            s.write8(0) // Reserved
            s.write16LE(1) // Color planes
            s.write16LE(32) // Bits per pixel

            val start = payloadData.position.toInt()
            if (width == 32 && height == 32) {
                val bmp = BMP2.encode(bitmap.toBMP32())
                payloadData.writeBytes(bmp.sliceArray(14 until bmp.size))
                val data = Bitmap1(width, height)
                payloadData.writeBytes(data.data)
            } else {
                payloadData.writeBytes(PNG.encode(bitmap.toBMP32()))
            }
            val size = payloadData.position.toInt() - start

            s.write32LE(size)
            s.write32LE(payloadStart + start)
        }

        s.writeBytes(payloadData.toByteArray())
    }
}

@Suppress("UNUSED_VARIABLE")
object BMP2 : ImageFormat("bmp") {
    class BmImageInfo : ImageInfo() {
        var flipX: Boolean = false
        var flipY: Boolean = false
    }

    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): BmImageInfo? {
        if (s.readStringz(2) != "BM") return null
        // FILE HEADER
        val size = s.readS32LE()
        val reserved1 = s.readS16LE()
        val reserved2 = s.readS16LE()
        val offBits = s.readS32LE()
        // INFO HEADER
        val bsize = s.readS32LE()
        val width = s.readS32LE()
        val height = s.readS32LE()
        val planes = s.readS16LE()
        val bitcount = s.readS16LE()
        return BmImageInfo().apply {
            this.flipX = width < 0
            this.flipY = height >= 0
            this.width = abs(width)
            this.height = abs(height)
            this.bitsPerPixel = bitcount
        }
    }

    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
        val h = decodeHeader(s, props) ?: throw IllegalArgumentException("Not a BMP file")

        val compression = s.readS32LE()
        val sizeImage = s.readS32LE()
        val pixelsPerMeterX = s.readS32LE()
        val pixelsPerMeterY = s.readS32LE()
        val clrUsed = s.readS32LE()
        val clrImportant = s.readS32LE()

        return when (h.bitsPerPixel) {
            8 -> {
                val out = Bitmap8(h.width, h.height)
                for (n in 0 until 256) out.palette[n] = RGBA(s.readS32LE(), 0xFF)
                for (n in 0 until h.height) out.setRow(h.height - n - 1, s.readBytes(h.width))
                ImageData(listOf(ImageFrame(out)))
            }
            24, 32 -> {
                val bytesPerRow = h.width * h.bitsPerPixel / 8
                val out = Bitmap32(h.width, h.height)
                val row = ByteArray(bytesPerRow)
                val format = if (h.bitsPerPixel == 24) BGR else BGRA
                val padding = 4 - (bytesPerRow % 4)
                val flipY = h.flipY
                for (n in 0 until h.height) {
                    val y = if (h.flipY) h.height - n - 1 else n
                    s.read(row)
                    format.decode(row, 0, out.data, out.index(0, y), h.width)
                    if (padding != 0) {
                        s.skip(padding)
                    }
                }
                ImageData(listOf(ImageFrame(out)))
            }
            else -> TODO("Unsupported bitsPerPixel=${h.bitsPerPixel}")
        }
    }

    override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
        val bmp = image.mainBitmap.toBMP32()

        //
        s.write8('B'.toInt())
        s.write8('M'.toInt())
        s.write32LE(4 * bmp.area)
        s.write32LE(0) // Reserved
        s.write32LE(54) // Offset to data

        s.write32LE(40)
        s.write32LE(bmp.width)
        s.write32LE(bmp.height * 2)
        s.write16LE(1) // Planes
        s.write16LE(32) // Bit count
        s.write32LE(0) // Compression
        s.write32LE(4 * bmp.area) // Size
        s.write32LE(2834) // Pels per meter
        s.write32LE(2834) // Pels per meter
        s.write32LE(0) // Clr used
        s.write32LE(0) // Important
        //s.writeBytes(BGRA.encode(bmp.data))
        for (n in 0 until bmp.height) {
            val y = bmp.height - 1 - n
            s.writeBytes(BGRA.encode(bmp.data, y * bmp.width, bmp.width, littleEndian = true))
        }
    }
}
