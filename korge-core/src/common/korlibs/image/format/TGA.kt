package korlibs.image.format

import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.Bitmap8
import korlibs.image.color.BGR
import korlibs.image.color.BGRA
import korlibs.image.color.RGBA
import korlibs.io.stream.SyncStream
import korlibs.io.stream.readBytes
import korlibs.io.stream.readS16LE
import korlibs.io.stream.readU16LE
import korlibs.io.stream.readU8
import korlibs.io.stream.write16LE
import korlibs.io.stream.write8
import korlibs.io.stream.writeBytes

object TGA : ImageFormat("tga") {
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = kotlin.runCatching {
        ImageInfo().apply {
            val h = readHeader(s)
            width = h.width
            height = h.height
            bitsPerPixel = h.bitsPerPixel
        }
    }.getOrNull()

	class Info(
		val width: Int,
		val height: Int,
		val flipY: Boolean,
		val bitsPerPixel: Int
	) {
		val area = width * height
		val bytes = bitsPerPixel / 8
	}

	// http://www.paulbourke.net/dataformats/tga/
	@Suppress("UNUSED_VARIABLE")
	fun readHeader(s: SyncStream): Info {
		val idLength = s.readU8()
		val colorMapType = s.readU8()
        when (colorMapType) {
            0, 1 -> Unit //
            else -> error("Not a TGA. Unsupported colorMapType")
        }
		val imageType = s.readU8()
		when (imageType) {
			1 -> TODO("Unsupported indexed")
			2 -> Unit // RGBA
			9, 10 -> TODO("Unsupported RLE")
			else -> TODO("Unknown TGA")
		}
		val firstIndexEntry = s.readU16LE()
		val colorMapLength = s.readU16LE()
		val colorMapEntrySize = s.readU8()
		s.position += colorMapLength * colorMapEntrySize
		val xorig = s.readS16LE()
		val yorig = s.readS16LE()
		val width = s.readS16LE()
		val height = s.readS16LE()
		val pixelDepth = s.readU8()
		when (pixelDepth) {
			24, 32 -> Unit
			else -> TODO("Not a RGBA tga")
		}

        if (width < 0 || height < 0) error("Not a TGA. Invalid width/height")

		val imageDescriptor = s.readU8()
		val flipY = ((imageDescriptor ushr 5) and 1) == 0
		val storage = ((imageDescriptor ushr 6) and 3)
		s.readBytes(idLength)
		return Info(width = width, height = height, flipY = flipY, bitsPerPixel = pixelDepth)
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val info = readHeader(s)
		val format = when (info.bitsPerPixel) {
			24 -> BGR
            32 -> BGRA
			else -> TODO("Not a RGB/RGBA tga")
		}
		val out = Bitmap32(info.width, info.height, premultiplied = false).writeDecoded(format, s.readBytes(info.area * info.bytes))
		if (info.flipY) out.flipY()
		return ImageData(out)
	}

	override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
		val bitmap = image.mainBitmap
		when (bitmap) {
			is Bitmap8 -> {
				TODO("Not implemented encoding TGA Bitmap8")
			}
			else -> {
                val bmp = bitmap.toBMP32()
				val data = ByteArray(bmp.area * 4)
				var m = 0
				for (cc in bmp.ints) {
                    val c = RGBA(cc)
					data[m++] = c.b.toByte()
					data[m++] = c.g.toByte()
					data[m++] = c.r.toByte()
					data[m++] = c.a.toByte()
				}
				s.write8(0) // idLength
				s.write8(0) // colorMapType
				s.write8(2) // imageType=RGBA
				s.write16LE(0) // firstIndexEntry
				s.write16LE(0) // colorMapLength
				s.write8(0) // colorMapEntrySize
				s.write16LE(0) // xorig
				s.write16LE(0) // yorig
				s.write16LE(bmp.width) // width
				s.write16LE(bmp.height) // height
				s.write8(32) // pixelDepth
				s.write8(1 shl 5) // imageDescriptor
				//s.write8(0 shl 5) // imageDescriptor
				s.writeBytes(data)
			}
		}
	}
}
