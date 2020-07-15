package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.stream.*
import kotlin.math.*

@Suppress("UNUSED_VARIABLE")
object BMP : ImageFormat("bmp") {
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
