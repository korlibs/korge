package korlibs.image.color

import korlibs.image.bitmap.Bitmap32
import korlibs.io.lang.splitInChunks
import korlibs.encoding.hex
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorFormatTest {
	//fun bmp() = Bitmap32(3, 1, arrayOf(Colors.RED, Colors.GREEN, Colors.BLUE).toRgbaArray())
	fun bmp(): Bitmap32 = Bitmap32(3, 1, RgbaArray(intArrayOf(Colors.RED.value, Colors.GREEN.value, Colors.BLUE.value)))
    fun bmpData(): RgbaArray = RgbaArray(bmp().ints)
	fun ByteArray.toHexChunks(size: Int): String = this.hex.splitInChunks(size).joinToString("-").lowercase()

	@Test
	fun name() {
		assertEquals("0000ff-00ff00-ff0000", RGB.encode(bmpData(), littleEndian = false).toHexChunks(6))
		assertEquals("ff0000ff-ff00ff00-ffff0000", RGBA.encode(bmpData(), littleEndian = false).toHexChunks(8))
		assertEquals("ffff004c-ff000095-ff00ff1d", YUVA.encode(bmpData(), littleEndian = false).toHexChunks(8))

		assertEquals("f00f-f0f0-ff00", RGBA_4444.encode(bmpData(), littleEndian = false).toHexChunks(4))
		assertEquals("801f-83e0-fc00", RGBA_5551.encode(bmpData(), littleEndian = false).toHexChunks(4))

		assertEquals("ff00-f0f0-f00f", BGRA_4444.encode(bmpData(), littleEndian = false).toHexChunks(4))
		assertEquals("fc00-83e0-801f", BGRA_5551.encode(bmpData(), littleEndian = false).toHexChunks(4))
	}

	@Test
	fun rgb565() {
		assertEquals("001f-07e0-f800", RGB_565.encode(bmpData(), littleEndian = false).toHexChunks(4))
		assertEquals("f800-07e0-001f", BGR_565.encode(bmpData(), littleEndian = false).toHexChunks(4))
	}
}
