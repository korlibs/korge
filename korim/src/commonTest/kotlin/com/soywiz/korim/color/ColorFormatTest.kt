package com.soywiz.korim.color

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.lang.splitInChunks
import com.soywiz.krypto.encoding.hex
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorFormatTest {
	//fun bmp() = Bitmap32(3, 1, arrayOf(Colors.RED, Colors.GREEN, Colors.BLUE).toRgbaArray())
	fun bmp() = Bitmap32(3, 1, RgbaArray(intArrayOf(Colors.RED.value, Colors.GREEN.value, Colors.BLUE.value)))
	fun ByteArray.toHexChunks(size: Int) = this.hex.splitInChunks(size).joinToString("-").toLowerCase()

	@Test
	fun name() {
		assertEquals("0000ff-00ff00-ff0000", RGB.encode(bmp().data, littleEndian = false).toHexChunks(6))
		assertEquals("ff0000ff-ff00ff00-ffff0000", RGBA.encode(bmp().data, littleEndian = false).toHexChunks(8))
		assertEquals("ffff004c-ff000095-ff00ff1d", YUVA.encode(bmp().data, littleEndian = false).toHexChunks(8))

		assertEquals("f00f-f0f0-ff00", RGBA_4444.encode(bmp().data, littleEndian = false).toHexChunks(4))
		assertEquals("801f-83e0-fc00", RGBA_5551.encode(bmp().data, littleEndian = false).toHexChunks(4))

		assertEquals("ff00-f0f0-f00f", BGRA_4444.encode(bmp().data, littleEndian = false).toHexChunks(4))
		assertEquals("fc00-83e0-801f", BGRA_5551.encode(bmp().data, littleEndian = false).toHexChunks(4))
	}

	@Test
	fun rgb565() {
		assertEquals("001f-07e0-f800", RGB_565.encode(bmp().data, littleEndian = false).toHexChunks(4))
		assertEquals("f800-07e0-001f", BGR_565.encode(bmp().data, littleEndian = false).toHexChunks(4))
	}
}
