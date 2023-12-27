package korlibs.bignumber

import kotlin.test.*

class BigNumTest {
    @Test
    fun testBigNum() {
        assertEquals(1.bi, 1L.bi)
        assertEquals("1".bi, "1".bi)
        assertEquals("1".bi, "1".bi(16))
        assertEquals(1.bn, 1L.bn)
        assertEquals("1".bn, "1".bn)
    }

	@Test
	fun testToString() {
		assertEquals("0.019", BigNum(19.bi, 3).toString())
		assertEquals("0.19", BigNum(19.bi, 2).toString())
		assertEquals("1.9", BigNum(19.bi, 1).toString())
		assertEquals("19", BigNum(19.bi, 0).toString())
	}

    @Test
    fun testToStringE() {
        assertEquals("0.1", "1e-1".bn.toString())
        assertEquals("0.01", "1e-2".bn.toString())
        assertEquals("0.0001", "1e-4".bn.toString())
        assertEquals("1", "1e0".bn.toString())
        assertEquals("0.01", "0.1e-1".bn.toString())
        assertEquals("0.001", "0.1e-2".bn.toString())
        assertEquals("0.00001", "0.1e-4".bn.toString())
        assertEquals("0.1", "0.1e0".bn.toString())
    }

    @Test
    fun testToStringE2() {
        assertEquals("10", "1e+1".bn.toString())
        assertEquals("100", "1e+2".bn.toString())
        assertEquals("1000", "1e+3".bn.toString())
        assertEquals("10000", "1e+4".bn.toString())
        assertEquals("100000", "10e+4".bn.toString())
    }

    @Test
	fun testAddSameScale() {
		assertEquals("0.050", (BigNum(20.bi, 3) + BigNum(30.bi, 3)).toString())
	}

	@Test
	fun testPow() {
		assertEquals("1", "${10.bi pow 0}")
		assertEquals("10", "${10.bi pow 1}")
		assertEquals("10000000000", "${10.bi pow 10}")
		assertEquals(
			"10000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
			"${10.bi pow 100}"
		)

		assertEquals("1", "${10.bi pow 0.bi}")
		assertEquals("10", "${10.bi pow 1.bi}")
		assertEquals("10000000000", "${10.bi pow 10.bi}")
		assertEquals(
			"10000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
			"${10.bi pow 100.bi}"
		)
	}

	@Test
	fun testAddDifferentScale() {
		assertEquals("11.51", (BigNum("1.2") + BigNum("10.31")).toString())
	}

	@Test
	fun testMultiply() {
		assertEquals("2.4".bn, "1.2".bn * "2".bn)
		assertEquals("0.12".bn, "1.2".bn * "0.1".bn)
		assertEquals("0.012".bn, "1.2".bn * "0.01".bn)
		assertEquals("0.012".bn, "0.01".bn * "1.2".bn)
	}

	@Test
	fun testDivide() {
		//val rr = BigDecimal("1.000")
		assertEquals("0.333".bn, "1.000".bn / "3".bn)
		assertEquals("0.333".bn, "1.000".bn / "3.000".bn)
		assertEquals("0.3".bn, "1.0".bn / "3".bn)
		assertEquals("0".bn, "1".bn / "3".bn)
		assertEquals("0".bn, "1".bn / "3.000".bn)
	}

	@Test
	fun testFromString() {
		//assertEquals("-50", BigNum("-050").toString())
		assertEquals("50", BigNum("050").toString())
		assertEquals("0.00005000000", BigNum("0.00005000000").toString())
		assertEquals("0.050", BigNum("0.050").toString())
		assertEquals("0.050", BigNum(".050").toString())
	}

	@Test
	fun testCompare() {
		assertTrue("1.5".bn < "3.0".bn)
		assertTrue("1.5".bn <= "1.5".bn)
	}

    @Test
    fun testNegative() {
        assertEquals("-0.0001", "-0.0001".bn.toString())
    }

    @Test
    fun testConvertToScale() {
        assertEquals(3, "0.001".bn.scale)
        assertEquals(4, "0.001".bn.convertToScale(4).scale)
        assertEquals("0.001", "0.001".bn.convertToScale(3).toString())
        assertEquals("0.0010", "0.001".bn.convertToScale(4).toString())
        assertEquals("0.001", "0.001".bn.convertToScale(4).convertToScale(3).toString())
        //"0.001".bn.convertToScale()
        //assertEquals("-0.0001", "-0.0001".bn.toString())
    }

    @Test
    fun testConvert() {
        assertEquals(1.bi, "1.2".bn.toBigInt())
        assertEquals(1.bi, "1.9".bn.toBigInt())
    }

    @Test
    fun testConvertCeil() {
        assertEquals(1.bi, "1.0".bn.toBigIntCeil())
        assertEquals(2.bi, "1.1".bn.toBigIntCeil())
        assertEquals(2.bi, "1.9123".bn.toBigIntCeil())
    }

    @Test
    fun testConvertFloor() {
        assertEquals(1.bi, "1.0".bn.toBigIntFloor())
        assertEquals(1.bi, "1.1".bn.toBigIntFloor())
        assertEquals(1.bi, "1.9123".bn.toBigIntFloor())
    }

    @Test
    fun testConvertRound() {
        assertEquals(1.bi, "1.0".bn.toBigIntRound())
        assertEquals(1.bi, "1.4".bn.toBigIntRound())
        assertEquals(1.bi, "1.49".bn.toBigIntRound())
        assertEquals(1.bi, "1.4123456".bn.toBigIntRound())
        assertEquals(1.bi, "1.499999".bn.toBigIntRound())
        assertEquals(2.bi, "1.5".bn.toBigIntRound())
        assertEquals(2.bi, "1.500".bn.toBigIntRound())
        assertEquals(2.bi, "1.51".bn.toBigIntRound())
        assertEquals(2.bi, "1.512345".bn.toBigIntRound())
        assertEquals(2.bi, "1.6".bn.toBigIntRound())
        assertEquals(2.bi, "1.9123".bn.toBigIntRound())
    }

    @Test
    fun testDecimalPart() {
        assertEquals(9123.bi, "1.9123".bn.decimalPart)
        assertEquals(0.bi, "1.0".bn.decimalPart)
    }

    @Test
    fun testDiv() {
        val bi1 = BigNum("1.000000000")
        val bi2 = BigNum("2.000000000")
        assertEquals(
            expected = 0.5.bn,
            actual = bi1 / bi2
        )
    }

    @Test
    fun testDiv2() {
        val bi1 = BigNum("1.000000000000000000000000000000000000000")
        val bi2 = BigNum("2.000000000000000000000000000000000000000")
        assertEquals(
            expected = 0.5.bn,
            actual = bi1 / bi2
        )
    }

    @Test
    fun testHashCode() {
        assertEquals(BigNum("0.123").hashCode(), BigNum("0.123").hashCode())
        assertEquals(BigNum("123.123").hashCode(), BigNum("123.123").hashCode())
    }

}
