package com.soywiz

import com.soywiz.kbignum.*
import com.soywiz.kbignum.internal.leadingZeros
import com.soywiz.kbignum.internal.trailingZeros
import kotlin.test.*

class BigIntTest {
	@Test
	fun testMultiplyPowerOfTwo() {
		assertEquals("1", (1.bi * 1.bi).toString2())
		assertEquals("10", (1.bi * 2.bi).toString2())
		assertEquals("100", (1.bi * 4.bi).toString2())
		assertEquals("1000", (1.bi * 8.bi).toString2())
		assertEquals("1000000000000000", (1.bi * (1 shl 15)).toString2())
		assertEquals("10000000000000000", (1.bi * (1 shl 16)).toString2())
		assertEquals("100000000000000000", (1.bi * (1 shl 17)).toString2())
		assertEquals(
			"100000000000000000000000000000000000000000000000000000000000000",
			(1.bi * (1L shl 62)).toString2()
		)
		assertEquals("1${"0".repeat(128)}", (1.bi * (1.bi shl 128)).toString2())
	}

	@Test
	fun testAddSmall() {
		assertEquals("10", (1.bi + 1.bi).toString2())
		assertEquals("11", (1.bi + 1.bi + 1.bi).toString2())
		assertEquals(108888887.bi, 99999999.bi + 8888888.bi)
		assertEquals("108888887", (99999999.bi + 8888888.bi).toString())
	}

	@Test
	fun testSub() {
		assertEquals("25", "${100.bi - 75.bi}")
		assertEquals("-25", "${75.bi - 100.bi}")
		assertEquals("0", "${100.bi - 100.bi}")
		assertEquals("0", "${(-100).bi - (-100).bi}")
		assertEquals("-50", "${(-100).bi - (-50).bi}")
		assertEquals("-150", "${(-100).bi - (50).bi}")
		assertEquals("150", "${(100).bi - (-50).bi}")
	}

	@Test
	fun testSubInt() {
		val res = (-9999999).bi - (-8888888).bi
		println("$res")

		val items = listOf(-9999999, -8888888, -100, -50, 0, +50, +100, +8888888, +9999999)
		for (l in items) for (r in items) {
			//println("$l - $r = ${l - r}")
			//println("${l.n} - ${r.n} = ${(l - r).n}")
			//println("${l.n} - ${r.n} = ${(l.n - r.n)}")
			assertEquals((l - r).bi, l.bi - r.bi)
		}
	}

	@Test
	fun testToString2() {
		assertEquals("0", "0".bi(2).toString2())
		assertEquals("101011", "101011".bi(2).toString2())
		assertEquals("1000000010000001", "1000000010000001".bi(2).toString2())
		assertEquals("1000000000000000", "1000000000000000".bi(2).toString2())
	}

	@Test
	fun testToString10() {
		assertEquals("0", "${0.bi}")
		assertEquals("1", "${1.bi}")
		assertEquals("10", "${10.bi}")
		assertEquals("100", "${100.bi}")
		assertEquals("999", "${999.bi}")
	}

	@Test
	fun testCompare() {
		assertTrue(1.bi == 1.bi)
		assertTrue(0.bi < 1.bi)
		assertTrue(1.bi > 0.bi)
		assertTrue(0.bi >= 0.bi)
		assertTrue(1.bi >= 0.bi)
		assertTrue(0.bi <= 0.bi)
		assertTrue(0.bi <= 1.bi)

		assertTrue((-1).bi < 1.bi)
		assertTrue((1).bi > (-1).bi)

		assertTrue((-2).bi < (-1).bi)
	}

	@Test
	fun testBitwise() {
		assertEquals("${0b101 xor 0b110}", "${0b101.bi xor 0b110.bi}")
		assertEquals("${0b101 and 0b110}", "${0b101.bi and 0b110.bi}")
		assertEquals("${0b101 or 0b110}", "${0b101.bi or 0b110.bi}")
	}

	@Test
	fun testTrailingZeros() {
        assertEquals(32, 0.trailingZeros())
        assertEquals(0, 1.trailingZeros())
        assertEquals(1, 2.trailingZeros())
        assertEquals(16, "000000000000000000000000000000".bi(2).trailingZeros())
		assertEquals(0, "000000000000000000000000000001".bi(2).trailingZeros())
		assertEquals(7, "100000000000000000000010000000".bi(2).trailingZeros())
		assertEquals(5, "100000000000000000000010100000".bi(2).trailingZeros())
		assertEquals(29, "100000000000000000000000000000".bi(2).trailingZeros())
		assertEquals(30, "1000000000000000000000000000000".bi(2).trailingZeros())
		assertEquals(31, "10000000000000000000000000000000".bi(2).trailingZeros())
		assertEquals(32, "100000000000000000000000000000000".bi(2).trailingZeros())
		assertEquals(33, "1000000000000000000000000000000000".bi(2).trailingZeros())
		assertEquals(40, "10000000000000000000000000000000000000000".bi(2).trailingZeros())
	}

    @Test
    fun testLeadingZeros() {
        assertEquals(32, 0.leadingZeros())
        assertEquals(31, 1.leadingZeros())
        assertEquals(30, 2.leadingZeros())
        assertEquals(15, "000000000000000000000000000001".bi(2).leadingZeros())
        assertEquals(2, "100000000000000000000010000000".bi(2).leadingZeros())
        assertEquals(2, "100000000000000000000010100000".bi(2).leadingZeros())
        assertEquals(2, "100000000000000000000000000000".bi(2).leadingZeros())
        assertEquals(1, "1000000000000000000000000000000".bi(2).leadingZeros())
        assertEquals(0, "10000000000000000000000000000000".bi(2).leadingZeros())
        assertEquals(15, "100000000000000000000000000000000".bi(2).leadingZeros())
        assertEquals(14, "1000000000000000000000000000000000".bi(2).leadingZeros())
        assertEquals(7, "10000000000000000000000000000000000000000".bi(2).leadingZeros())
    }

	@Test
	fun testBitCount() {
		assertEquals(0, "00000000000000000000000000000000000000000".bi(2).countBits())
		assertEquals(1, "00000000000000000000000000000000000000001".bi(2).countBits())
		assertEquals(1, "10000000000000000000000000000000000000000".bi(2).countBits())
		assertEquals(2, "10000000000000000000000000000000000000001".bi(2).countBits())
		assertEquals(7, "10000000001000001000000010000100001000001".bi(2).countBits())
	}

    @Test
    fun testSignificantBits() {
        assertEquals(0, 0.bi.significantBits)
        assertEquals(1, 1.bi.significantBits)
        assertEquals(2, 2.bi.significantBits)
        assertEquals(2, 3.bi.significantBits)
        assertEquals(16, 0xFFFF.bi.significantBits)
        assertEquals(24, 0xFFFFFF.bi.significantBits)
        assertEquals(32, 0xFFFFFFFFL.bi.significantBits)
        assertEquals(40, 0xFFFFFFFFFFL.bi.significantBits)
        assertEquals(48, 0xFFFFFFFFFFFFL.bi.significantBits)
    }

    @Test
    fun testRadixPrefix() {
        assertEquals("FF".toInt(16), "0xFF".bi.toInt())
        assertEquals("777".toInt(8), "0o777".bi.toInt())
        assertEquals("111".toInt(2), "0b111".bi.toInt())
    }

    @Test
    fun testInvalid() {
        assertFailsWith<BigIntInvalidFormatException> { "2".bi(2) }
        assertFailsWith<BigIntInvalidFormatException> { "a".bi(10) }
        assertFailsWith<BigIntInvalidFormatException> { "0xg".bi }
        assertFailsWith<BigIntInvalidFormatException> { "0o8".bi }
        assertFailsWith<BigIntInvalidFormatException> { "0b2".bi }
    }

    @Test
    fun testLongDiv() {
        //assertEquals("500000000".bi, "1000000000000000000".bi / "2000000000".bi)
        assertEquals("5000000000".bi, "100000000000000000000".bi / "20000000000".bi)
        assertEquals("-5000000000".bi, "-100000000000000000000".bi / "20000000000".bi)
        assertEquals("-5000000000".bi, "100000000000000000000".bi / "-20000000000".bi)
        assertEquals("5000000000".bi, "-100000000000000000000".bi / "-20000000000".bi)
    }

    @Test
    fun testInv() {
        assertEquals("0x0000".bi, "0xFFFF".bi.inv())
        assertEquals("0xFFFF".bi, "0x0000".bi.inv())
        assertEquals("0xedcb".bi, "0x1234".bi.inv())
    }
}
