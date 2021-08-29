package com.soywiz.kbignum

import java.math.*
import kotlin.test.*

class BigIntCompareWithJVMTest {
	val items = listOf(
		-9999999,
		-8888888,
		-0x10001,
		-0x10000,
		-0xFFFF,
		-0xFFFE,
		-1024,
		-100,
		-50,
		-16,
		-15,
		-2,
		-1,
		0,
		+1,
		+2,
		+15,
		+16,
		+50,
		+100,
		+1024,
		+0xFFFE,
		+0xFFFF,
		+0x10000,
		+0x10001,
		+8888888,
		+9999999
	)

	@Test
	fun testSub() = testBinary { jvmL, jvmR, kL, kR -> assertEquals("${jvmL - jvmR}", "${kL - kR}", "$kL - $kR") }

	@Test
	fun testAdd() = testBinary { jvmL, jvmR, kL, kR -> assertEquals("${jvmL + jvmR}", "${kL + kR}", "$kL + $kR") }

	@Test
	fun testMul() = testBinary { jvmL, jvmR, kL, kR -> assertEquals("${jvmL * jvmR}", "${kL * kR}", "$kL * $kR") }

	@Test
	fun testDiv() =
		testBinary { jvmL, jvmR, kL, kR -> if (!kR.isZero) assertEquals("${jvmL / jvmR}", "${kL / kR}", "$kL / $kR") }

	@Test
	fun testDiv2() {
		assertEquals(
			"${BigInteger("-9999999") / BigInteger("-65536")}",
			"${"-9999999".bi / "-65536".bi}", "-9999999 / -65536"
		)
	}

	@Test
	fun testRem() =
		testBinary { jvmL, jvmR, kL, kR -> if (!kR.isZero) assertEquals("${jvmL % jvmR}", "${kL % kR}", "$kL % $kR") }

	@Test
	fun testLeftShift() =
		testBinary { jvmL, jvmR, kL, kR -> assertEquals("${jvmL shl 1024}", "${kL shl 1024}", "$kL shl 1024") }

	@Test
	fun testLeftShift2() =
		testBinary { jvmL, jvmR, kL, kR -> assertEquals("${jvmL shl 1030}", "${kL shl 1030}", "$kL shl 1030") }

	@Test
	fun testRightShift() = testBinary { jvmL, jvmR, kL, kR ->
		assertEquals(
			"${jvmL / (1 shl 16).toBigInteger()}",
			"${kL shr 16}",
			"$kL shr 16"
		)
	}

	@Test
	fun testRightShift2() = testBinary { jvmL, jvmR, kL, kR ->
		assertEquals(
			"${jvmL / (1 shl 27).toBigInteger()}",
			"${kL shr 27}",
			"$kL shr 27"
		)
	}

	@Test
	fun testBigBig() {
		val a = "9191291821821972198723892731927412419757607241902412742141904810123913021931"
		val b = "121231246717581291824912849128509185124190310741841824712837131738172"
		assertEquals("${BigInteger(a) + BigInteger(b)}", "${a.bi + b.bi}")
		assertEquals("${BigInteger(a) + -BigInteger(b)}", "${a.bi + -b.bi}")
		assertEquals("${-BigInteger(a) + BigInteger(b)}", "${-a.bi + b.bi}")
		assertEquals("${-BigInteger(a) + -BigInteger(b)}", "${-a.bi + -b.bi}")

		assertEquals("${BigInteger(a) - BigInteger(b)}", "${a.bi - b.bi}")
		assertEquals("${BigInteger(a) - -BigInteger(b)}", "${a.bi - -b.bi}")
		assertEquals("${-BigInteger(a) - BigInteger(b)}", "${-a.bi - b.bi}")
		assertEquals("${-BigInteger(a) - -BigInteger(b)}", "${-a.bi - -b.bi}")

		assertEquals("${BigInteger(a) * BigInteger(b)}", "${a.bi * b.bi}")
		assertEquals("${BigInteger(a) * -BigInteger(b)}", "${a.bi * -b.bi}")
		assertEquals("${-BigInteger(a) * BigInteger(b)}", "${-a.bi * b.bi}")
		assertEquals("${-BigInteger(a) * -BigInteger(b)}", "${-a.bi * -b.bi}")
	}

	@Test
	fun testBigSmall() {
		val a = "123678"
		val b = "456965"
		assertEquals("${BigInteger(a) + BigInteger(b)}", "${a.bi + b.bi}")
		assertEquals("${BigInteger(a) - BigInteger(b)}", "${a.bi - b.bi}")
		assertEquals("${BigInteger(a) * BigInteger(b)}", "${a.bi * b.bi}")
		assertEquals("${BigInteger(a) * -BigInteger(b)}", "${a.bi * -b.bi}")
		assertEquals("${-BigInteger(a) * BigInteger(b)}", "${-a.bi * b.bi}")
		assertEquals("${-BigInteger(a) * -BigInteger(b)}", "${-a.bi * -b.bi}")
	}

	@Test
	fun testBigSmall2() {
		val a = "192318471586571265712651786924871293164197657612641412412410410"
		val b = "1234"
		assertEquals("${BigInteger(a) + BigInteger(b)}", "${a.bi + b.bi}")
		assertEquals("${BigInteger(a) - BigInteger(b)}", "${a.bi - b.bi}")
		assertEquals("${BigInteger(a) * BigInteger(b)}", "${a.bi * b.bi}")
		assertEquals("${BigInteger(a) * -BigInteger(b)}", "${a.bi * -b.bi}")
		assertEquals("${-BigInteger(a) * BigInteger(b)}", "${-a.bi * b.bi}")
		assertEquals("${-BigInteger(a) * -BigInteger(b)}", "${-a.bi * -b.bi}")
	}

	@Test
	fun testMultCarry() {
		var tempJvm = BigInteger.valueOf(0xFFFF)
		var temp = 0xFFFF.bi
		for (n in 0 until 10) {
			tempJvm *= tempJvm
			temp *= temp
			assertEquals("$tempJvm", "$temp")
		}
		//println("$tempJvm".length)
		//println(BigDecimal("2.0").pow(-1024, MathContext(1024)))
	}

	@Test
	fun testPow() {
		assertEquals("1", "${0.5.bn pow 0}")
		assertEquals("0.5", "${0.5.bn pow 1}")
		assertEquals("0.25", "${0.5.bn pow 2}")
		assertEquals("0.125", "${0.5.bn pow 3}")
		assertEquals("0.00000000000000088817841970012523233890533447265625", "${0.5.bn pow 50}")
		assertEquals("2", "${0.5.bn pow -1}")
		assertEquals("4", "${0.5.bn pow -2}")
		assertEquals("8", "${0.5.bn pow -3}")
		assertEquals("4294967296", "${2.bn pow 32}")
		assertEquals("1125899906842624", "${2.bn pow 50}")
		assertEquals(
			"5357543035931336604742125245300009052807024058527668037218751941851755255624680612465991894078479290637973364587765734125935726428461570217992288787349287401967283887412115492710537302531185570938977091076523237491790970633699383779582771973038531457285598238843271083830214915826312193418602834034688",
			"${2.bn pow 999}"
		)
	}

	private fun testBinary(callback: (jvmL: BigInteger, jvmR: BigInteger, kL: BigInt, kR: BigInt) -> Unit) {
		for (l in items) for (r in items) {
			val jvmL = BigInteger("$l")
			val jvmR = BigInteger("$r")
			val kL = l.bi
			val kR = r.bi
			callback(jvmL, jvmR, kL, kR)
		}
	}
}
