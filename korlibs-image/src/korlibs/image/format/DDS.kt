package korlibs.image.format

import korlibs.datastructure.*
import korlibs.io.lang.*
import korlibs.io.stream.*

object DDS : ImageFormat("dds") {
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		if (s.readString(4) != "DDS ") return null
		val size = s.readS32LE()
		val sh = s.readStream(size - 4)
		val flags = sh.readS32LE()
		val height = sh.readS32LE()
		val width = sh.readS32LE()
		val pitchOrLinearSize = sh.readS32LE()
		val depth = sh.readS32LE()
		val mipmapCount = sh.readS32LE()
		val reserved = sh.readIntArrayLE(11)

		val pf_size = sh.readS32LE()
		val pf_s = sh.readStream(pf_size - 4)
		val pf_flags = pf_s.readS32LE()
		val pf_fourcc = pf_s.readString(4)
		val pf_bitcount = pf_s.readS32LE()
		val pf_rbitmask = pf_s.readS32LE()
		val pf_gbitmask = pf_s.readS32LE()
		val pf_bbitmask = pf_s.readS32LE()
		val pf_abitmask = pf_s.readS32LE()

		val caps = sh.readS32LE()
		val caps2 = sh.readS32LE()
		val caps3 = sh.readS32LE()
		val caps4 = sh.readS32LE()

		val reserved2 = sh.readS32LE()

		return ImageInfo().apply {
			this.width = width
			this.height = height
			this.bitsPerPixel = 32
			this.fourcc = pf_fourcc
		}
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val h = decodeHeader(s, props) ?: invalidOp("Not a DDS file")
		val fourcc = h.fourcc.toUpperCase()
		val subimageFormat: DXT = when (fourcc) {
			"DXT1" -> DXT1
			"DXT3" -> DXT3
			"DXT4" -> DXT4
			"DXT5" -> DXT5
			else -> invalidOp("Unsupported DDS FourCC '$fourcc'")
		}
		val bytes = s.readAll()
		return subimageFormat.readImage(
			bytes.openSync(),
			ImageDecodingProps(filename = "image.$fourcc", width = h.width, height = h.height)
		)
	}
}

private var ImageInfo.fourcc by Extra.Property { "    " }
