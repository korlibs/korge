package korlibs.memory

import korlibs.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class BitsTest {
	@kotlin.test.Test
	fun name() {
		val a = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
		assertEquals("0102030405060708", "%016X".format(a.getS64BE(0)))
		assertEquals("0807060504030201", "%016X".format(a.getS64LE(0)))

		assertEquals("01020304", "%08X".format(a.getS32BE(0)))
		assertEquals("04030201", "%08X".format(a.getS32LE(0)))

		assertEquals("010203", "%06X".format(a.getS24BE(0)))
		assertEquals("030201", "%06X".format(a.getS24LE(0)))

		assertEquals("0102", "%04X".format(a.getS16BE(0)))
		assertEquals("0201", "%04X".format(a.getS16LE(0)))

		assertEquals("01", "%02X".format(a.getS8(0)))

		val data = (0 until 128).map { ((it + 35363) * 104723).toByte() }.toByteArray()
        assertEquals(data.getS64BE(0), data.getS64LE(0).reverseBytes())
        assertEquals(data.getS32BE(0), data.getS32LE(0).reverseBytes())
		assertEquals(data.getU32BE(0).toInt(), data.getU32LE(0).toInt().reverseBytes())
		assertEquals(data.getS16BE(0).toShort(), data.getS16LE(0).toShort().reverseBytes())
		assertEquals(data.getU16BE(0).toShort(), data.getU16LE(0).toShort().reverseBytes())
	}

	@Test
	fun rotate() {
		val v = 0b10110111_01111011_11101111_11001000.toInt()
		assertEquals(0b0110111_01111011_11101111_11001000_1.toInt(), v.rotateLeft(1))
		assertEquals(0b0_10110111_01111011_11101111_1100100.toInt(), v.rotateRight(1))
		assertEquals(0b110111_01111011_11101111_11001000_10.toInt(), v.rotateLeft(2))
		assertEquals(0b00_10110111_01111011_11101111_110010.toInt(), v.rotateRight(2))
	}


	@Test
	fun clz() {
		assertEquals(32, 0.countLeadingZeros())
		for (n in 0 until 31) {
			assertEquals(31 - n, (1 shl n).countLeadingZeros())
		}
	}

	@Test
	fun testCountTrailingZeros() {
		assertEquals(32, (0b00000000000000000000000000000000).countTrailingZeros())
		assertEquals(0, (0b01111111111111111111111111111111).countTrailingZeros())
		assertEquals(1, (0b11111111111111111111111111111110).toInt().countTrailingZeros())
		for (n in 0 until 32) assertEquals(n, (1 shl n).countTrailingZeros())
		for (n in 0 until 32) assertEquals(n, (0x173F52B1 shl n).countTrailingZeros())
		for (n in 0 until 32) assertEquals(n, ((-1) shl n).countTrailingZeros())
	}

	@Test
	fun testCountTrailingOnes() {
		assertEquals(32, (0b11111111111111111111111111111111).toInt().countTrailingOnes())
		assertEquals(31, (0b01111111111111111111111111111111).toInt().countTrailingOnes())
		assertEquals(0, (0b11111111111111111111111111111110).toInt().countTrailingOnes())
		for (n in 0 until 32) assertEquals(n, (1 shl n).inv().countTrailingOnes())
		for (n in 0 until 32) assertEquals(n, (0x173F52B1 shl n).inv().countTrailingOnes())
		for (n in 0 until 32) assertEquals(n, ((-1) shl n).inv().countTrailingOnes())
	}

	@Test
	fun testCountLeadingZeros() {
		assertEquals(32, (0b00000000000000000000000000000000).countLeadingZeros())
		assertEquals(1, (0b01111111111111111111111111111111).countLeadingZeros())
		assertEquals(0, (0b11111111111111111111111111111110).toInt().countLeadingZeros())
		for (n in 0 until 32) assertEquals(n, (1 shl n).reverseBits().countLeadingZeros())
		for (n in 0 until 32) assertEquals(n, (0x173F52B1 shl n).reverseBits().countLeadingZeros())
		for (n in 0 until 32) assertEquals(n, ((-1) shl n).reverseBits().countLeadingZeros())
	}

	@Test
	fun reinterpret() {
		assertEquals(0x3ff0000000000000L, 1.0.reinterpretAsLong())
		assertEquals(1.0, 0x3ff0000000000000L.reinterpretAsDouble())

		assertEquals(0x3f800000, 1f.reinterpretAsInt())
		assertEquals(1f, 0x3f800000.reinterpretAsFloat())
	}

	@Test
	fun setUnset() {
		assertEquals(1, bit(0))
		assertEquals(2, bit(1))
		assertEquals(4, bit(2))
		assertEquals(8, bit(3))
		assertEquals(0b0101, 0b0111.unsetBits(0b0010))
		assertEquals(0b0110, 0b0100.setBits(0b0010))
	}
}
