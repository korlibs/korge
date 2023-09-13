package korlibs.image.format

import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.Bitmap8
import korlibs.image.color.BGR
import korlibs.image.color.BGRA
import korlibs.image.color.RGBA
import korlibs.image.color.RgbaArray
import korlibs.image.color.decode
import korlibs.image.color.encode
import korlibs.io.stream.SyncStream
import korlibs.io.stream.readBytes
import korlibs.io.stream.readFastByteArrayInputStream
import korlibs.io.stream.readS16LE
import korlibs.io.stream.readS32LE
import korlibs.io.stream.readStringz
import korlibs.io.stream.write16LE
import korlibs.io.stream.write32LE
import korlibs.io.stream.write8
import korlibs.io.stream.writeBytes
import kotlin.math.abs

@Suppress("UNUSED_VARIABLE")
object BMP : ImageFormat("bmp") {
    class BmImageInfo : ImageInfo() {
        var flipX: Boolean = false
        var flipY: Boolean = false
        var compression: Int = 0
        var sizeImage: Int = 0
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
        val ss = s.readFastByteArrayInputStream(bsize - 4)
		val width = ss.readS32LE()
		val height = ss.readS32LE()
		val planes = ss.readS16LE()
		val bitcount = ss.readS16LE()
        val compression = ss.readS32LE()
        val sizeImage = ss.readS32LE()
        val pixelsPerMeterX = ss.readS32LE()
        val pixelsPerMeterY = ss.readS32LE()
        val clrUsed = ss.readS32LE()
        val clrImportant = ss.readS32LE()
		return BmImageInfo().apply {
            this.compression = compression
            this.sizeImage = sizeImage
            this.flipX = width < 0
            this.flipY = height >= 0
			this.width = abs(width)
			this.height = abs(height)
			this.bitsPerPixel = bitcount
		}
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val h = decodeHeader(s, props) ?: throw IllegalArgumentException("Not a BMP file")

        when (h.compression) {
            0, 3 -> Unit
            else -> error("Unsupported BMP compression ${h.compression}")
        }

        return when (h.bitsPerPixel) {
			8 -> {
				val out = Bitmap8(h.width, h.height)
				for (n in 0 until 256) out.palette[n] = RGBA(s.readS32LE(), 0xFF)
				for (n in 0 until h.height) out.setRow(h.height - n - 1, s.readBytes(h.width))
				ImageData(out)
			}
			24, 32 -> {
				val bytesPerRow = h.width * h.bitsPerPixel / 8
				val out = Bitmap32(h.width, h.height, premultiplied = false)
				val row = ByteArray(bytesPerRow)
				val format = if (h.bitsPerPixel == 24) BGR else BGRA
				val padding = 4 - (bytesPerRow % 4)
                val flipY = h.flipY
				for (n in 0 until h.height) {
					val y = if (h.flipY) h.height - n - 1 else n
					s.read(row)
					format.decode(row, 0, RgbaArray(out.ints), out.index(0, y), h.width)
					if (padding != 4) {
						s.skip(padding)
					}
				}
				ImageData(out)
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
            s.writeBytes(BGRA.encode(RgbaArray(bmp.ints), y * bmp.width, bmp.width, littleEndian = true))
        }
    }
}
