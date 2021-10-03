package com.soywiz.kbignum

import com.soywiz.kbignum.internal.leadingZeros
import com.soywiz.kbignum.internal.trailingZeros
import kotlin.test.*

abstract class AbstractBigIntTest {
	@Test
	fun testMultiplyPowerOfTwo() {
		assertEquals("1", (1.bi * 1.bi).toString(2))
		assertEquals("10", (1.bi * 2.bi).toString(2))
		assertEquals("100", (1.bi * 4.bi).toString(2))
		assertEquals("1000", (1.bi * 8.bi).toString(2))
		assertEquals("1000000000000000", (1.bi * (1 shl 15)).toString(2))
		assertEquals("10000000000000000", (1.bi * (1 shl 16)).toString(2))
		assertEquals("100000000000000000", (1.bi * (1 shl 17)).toString(2))
		assertEquals(
			"100000000000000000000000000000000000000000000000000000000000000",
			(1.bi * (1L shl 62)).toString(2)
		)
		assertEquals("1${"0".repeat(128)}", (1.bi * (1.bi shl 128)).toString(2))
	}

	@Test
	fun testAddSmall() {
		assertEquals("10", (1.bi + 1.bi).toString(2))
		assertEquals("11", (1.bi + 1.bi + 1.bi).toString(2))
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
		assertEquals("0", "0".bi(2).toString(2))
		assertEquals("101011", "101011".bi(2).toString(2))
		assertEquals("1000000010000001", "1000000010000001".bi(2).toString(2))
		assertEquals("1000000000000000", "1000000000000000".bi(2).toString(2))
	}

    @Test
    fun testParseRadix() {
        assertEquals("1", "1".bi(2).toString(2))
        assertEquals("-1", "-1".bi(2).toString(2))
        assertEquals("1", "1".bi(8).toString(8))
        assertEquals("-1", "-1".bi(8).toString(8))
        assertEquals("f7", "f7".bi(16).toString(16))
        assertEquals("-f7", "-f7".bi(16).toString(16))
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
        assertEquals(16, CommonBigInt("000000000000000000000000000000", 2).trailingZeros())
		assertEquals(0,  CommonBigInt("000000000000000000000000000001", 2).trailingZeros())
		assertEquals(7,  CommonBigInt("100000000000000000000010000000", 2).trailingZeros())
		assertEquals(5,  CommonBigInt("100000000000000000000010100000", 2).trailingZeros())
		assertEquals(29, CommonBigInt("100000000000000000000000000000", 2).trailingZeros())
		assertEquals(30, CommonBigInt("1000000000000000000000000000000", 2).trailingZeros())
		assertEquals(31, CommonBigInt("10000000000000000000000000000000", 2).trailingZeros())
		assertEquals(32, CommonBigInt("100000000000000000000000000000000", 2).trailingZeros())
		assertEquals(33, CommonBigInt("1000000000000000000000000000000000", 2).trailingZeros())
		assertEquals(40, CommonBigInt("10000000000000000000000000000000000000000", 2).trailingZeros())
	}

    @Test
    fun testLeadingZeros() {
        assertEquals(32, 0.leadingZeros())
        assertEquals(31, 1.leadingZeros())
        assertEquals(30, 2.leadingZeros())
        assertEquals(15, CommonBigInt("000000000000000000000000000001", 2).leadingZeros())
        assertEquals(2,  CommonBigInt("100000000000000000000010000000", 2).leadingZeros())
        assertEquals(2,  CommonBigInt("100000000000000000000010100000", 2).leadingZeros())
        assertEquals(2,  CommonBigInt("100000000000000000000000000000", 2).leadingZeros())
        assertEquals(1,  CommonBigInt("1000000000000000000000000000000", 2).leadingZeros())
        assertEquals(0,  CommonBigInt("10000000000000000000000000000000", 2).leadingZeros())
        assertEquals(15, CommonBigInt("100000000000000000000000000000000", 2).leadingZeros())
        assertEquals(14, CommonBigInt("1000000000000000000000000000000000", 2).leadingZeros())
        assertEquals(7,  CommonBigInt("10000000000000000000000000000000000000000", 2).leadingZeros())
    }

	@Test
	fun testBitCount() {
		assertEquals(0, CommonBigInt("00000000000000000000000000000000000000000", 2).countBits())
		assertEquals(1, CommonBigInt("00000000000000000000000000000000000000001", 2).countBits())
		assertEquals(1, CommonBigInt("10000000000000000000000000000000000000000", 2).countBits())
		assertEquals(2, CommonBigInt("10000000000000000000000000000000000000001", 2).countBits())
		assertEquals(7, CommonBigInt("10000000001000001000000010000100001000001", 2).countBits())
	}

    @Test
    fun testSignificantBits() {
        assertEquals(0, CommonBigInt(0).significantBits)
        assertEquals(1, CommonBigInt(1).significantBits)
        assertEquals(2, CommonBigInt(2).significantBits)
        assertEquals(2, CommonBigInt(3).significantBits)
        assertEquals(16, CommonBigInt(0xFFFF).significantBits)
        assertEquals(24, CommonBigInt(0xFFFFFF).significantBits)
        assertEquals(32, CommonBigInt(0xFFFFFFFFL).significantBits)
        assertEquals(40, CommonBigInt(0xFFFFFFFFFFL).significantBits)
        assertEquals(48, CommonBigInt(0xFFFFFFFFFFFFL).significantBits)
    }

    @Test
    fun testRadixPrefix() {
        assertEquals("FF".toInt(16), "0xFF".bi.toInt())
        assertEquals("777".toInt(8), "0o777".bi.toInt())
        assertEquals("111".toInt(2), "0b111".bi.toInt())
        assertEquals("-FF".toInt(16), "-0xFF".bi.toInt())
        assertEquals("-777".toInt(8), "-0o777".bi.toInt())
        assertEquals("-111".toInt(2), "-0b111".bi.toInt())
    }

    @Test
    fun testInvalid() {
        assertFailsWith<BigIntInvalidFormatException> { "2".bi(2) }
        assertFailsWith<BigIntInvalidFormatException> { "-2".bi(2) }
        assertFailsWith<BigIntInvalidFormatException> { "a".bi(10) }
        assertFailsWith<BigIntInvalidFormatException> { "-a".bi(10) }
        assertFailsWith<BigIntInvalidFormatException> { "0xg".bi }
        assertFailsWith<BigIntInvalidFormatException> { "0o8".bi }
        assertFailsWith<BigIntInvalidFormatException> { "0b2".bi }
        assertFailsWith<BigIntInvalidFormatException> { "-0b2".bi }
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
    open fun testInv() {
        assertEquals(
            listOf("0x0000".bi, "0xFFFF".bi, "0xedcb".bi),
            listOf("0xFFFF".bi.inv(), "0x0000".bi.inv(), "0x1234".bi.inv())
        )
    }

    @Test
    fun testMultComplexity() {
        val num1 = CommonBigInt("1".repeat(1024))
        val stats = CommonBigInt.OpStats()
        val res = num1.mulWithStats(num1, stats)
        assertEquals(2047, res.toString().length)
        //assertEquals(1024, stats.iterations)
    }

    @Test
    fun testOtherToString2() {
        println(12345.bi.toString(2))
        println(12345.bi.toString(4))
        println(12345.bi.toString(10))
        println(12345.bi.toString(16))
        //assertEquals(1024, stats.iterations)
    }

    @Test
    fun testMixed() {
        val int1 = "9191291821821972198723892731927412419757607241902412742141904810123913021931".bi
        val int2 = "121231246717581291824912849128509185124190310741841824712837131738172".bi
        assertEquals(
            """
                9191291943053218916305184556840261548266792366092723483983729522961044760103
                9191291700590725481142600907014563291248422117712102000300080097286781283759
                1114271766504586738871424632299032567834176034059871342978560190227960452332298253865995506036055166329263498074211029232849112689623243185850132
                75816194
                101819965766955686129307629109600080549778649042399450539859107464563
            """.trimIndent(),
            """
                ${int1 + int2}
                ${int1 - int2}
                ${int1 * int2}
                ${int1 / int2}
                ${int1 % int2}
            """.trimIndent()
        )
    }

    @Test
    fun testUsesNativeImplementationDoNotThrow() {
        BigInt.usesNativeImplementation
    }

    // Big Integer
    abstract val Long.bi: BigInt
    abstract val Int.bi: BigInt
    abstract val String.bi: BigInt
    abstract fun String.bi(radix: Int): BigInt
}

class BigIntTestCommon : AbstractBigIntTest() {
    override val Long.bi: BigInt get() = CommonBigInt(this)
    override val Int.bi: BigInt get() = CommonBigInt(this)
    override val String.bi: BigInt get() = CommonBigInt(this)
    override fun String.bi(radix: Int): BigInt = CommonBigInt(this, radix)
}

class BigIntTestPlatform : AbstractBigIntTest() {
    override val Long.bi: BigInt get() = BigInt(this)
    override val Int.bi: BigInt get() = BigInt(this)
    override val String.bi: BigInt get() = BigInt(this)
    override fun String.bi(radix: Int): BigInt = BigInt(this, radix)

    @Test
    @Ignore // Disabled because in the JVM BigInteger.inv() works differently than in common
    override fun testInv() {
        super.testInv()
    }
}
