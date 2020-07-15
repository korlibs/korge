package com.soywiz

import com.soywiz.kbignum.BigNum
import com.soywiz.kbignum.bi
import com.soywiz.kbignum.bn
import kotlin.test.*

class BigNumTest {
	@Test
	fun testToString() {
		assertEquals("0.019", BigNum(19.bi, 3).toString())
		assertEquals("0.19", BigNum(19.bi, 2).toString())
		assertEquals("1.9", BigNum(19.bi, 1).toString())
		assertEquals("19", BigNum(19.bi, 0).toString())
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
}
