package com.soywiz.kbignum

import java.math.*
import kotlin.test.*

abstract class AbstractBigIntCompareWithJVMTest {
	val intItems = listOf(
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
    val stringItems = listOf<String>(
        //"11111111111",
        //"1234567890123456789",
        //"9191291821821972198723892731927412419757607241902412742141904810123913021931",
        //"121231246717581291824912849128509185124190310741841824712837131738172",
    )

    val allItems = intItems.map { it.toString() } + stringItems

    data class ResultEx(val result: Result, val jvm: String, val kbignum: String)
    data class Result(val op: String, val jvm: String, val kbignum: String)

	@Test
	fun testSub() = testBinary { jvmL, jvmR, kL, kR -> Result("-", "${jvmL - jvmR}", "${kL - kR}") }

	@Test
	fun testAdd() = testBinary { jvmL, jvmR, kL, kR -> Result("+", "${jvmL + jvmR}", "${kL + kR}") }

	@Test
	fun testMul() = testBinary { jvmL, jvmR, kL, kR -> Result("*", "${jvmL * jvmR}", "${kL * kR}") }

	@Test
	fun testDiv() =
		testBinary { jvmL, jvmR, kL, kR -> if (kR != 0.bi) Result("/", "${jvmL / jvmR}", "${kL / kR}") else null }

	@Test
	fun testDiv2() {
		assertEquals(
			"${BigInteger("-9999999") / BigInteger("-65536")}",
			"${"-9999999".bi / "-65536".bi}", "-9999999 / -65536"
		)
	}

	@Test
	fun testRem() =
		testBinary { jvmL, jvmR, kL, kR -> if (kR != 0.bi) Result("%", "${jvmL % jvmR}", "${kL % kR}") else null }

	@Test
	fun testLeftShift() =
		testBinary { jvmL, jvmR, kL, kR -> Result("<<", "${jvmL shl 1024}", "${kL shl 1024}") }

	@Test
	fun testLeftShift2() =
		testBinary { jvmL, jvmR, kL, kR -> Result("<<", "${jvmL shl 1030}", "${kL shl 1030}") }

	@Test
    open fun testRightShift() = testBinary { jvmL, jvmR, kL, kR ->
        //Result(">>", "${jvmL / (1 shl 16).toBigInteger()}", "${kL shr 16}")
        Result(">>", "${jvmL shr 16}", "${kL shr 16}")
	}

	@Test
	open fun testRightShift2() = testBinary { jvmL, jvmR, kL, kR ->
        //Result(">>", "${jvmL / (1 shl 27).toBigInteger()}", "${kL shr 27}")
        Result(">>", "${jvmL shr 27}", "${kL shr 27}")
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

	open fun testBinary(callback: (jvmL: BigInteger, jvmR: BigInteger, kL: BigInt, kR: BigInt) -> Result?) {
        val results = arrayListOf<ResultEx>()
		for (l in allItems) for (r in allItems) {
			val jvmL = BigInteger(l)
			val jvmR = BigInteger(r)
			val kL = l.bi
			val kR = r.bi
            val res = callback(jvmL, jvmR, kL, kR) ?: continue
            results += ResultEx(res, "$jvmL ${res.op} $jvmR", "$kL ${res.op} $kR")
		}
        assertEquals(
            results.joinToString("\n") { "${it.jvm} = ${it.result.jvm}" },
            results.joinToString("\n") { "${it.kbignum} = ${it.result.kbignum}" },
        )
	}

    @Test
    fun testMultComplexity() {
        //val num1Str = "1".repeat(1024 * 32 * 4)
        //val num1Str = "1".repeat(15)

        //val jnum = BigInteger(num1Str)
        //val num = num1Str.bi
        //val stats = BigInt.OpStats()
        //10.bi.powWithStats(1024 * 32 * 2, stats)
        //println(stats)

        BigInteger.valueOf(10).pow(1024 * 32 * 2)

        //println("result: " + BigInt.pow10(1024 * 32 * 2))
        //jnum.toString()
        //assertEquals(jnum.toString(), num.toString())
    }

    @Test
    fun testToString2() {
        println(BigInteger.valueOf(12345).toString(2))
        println(BigInteger("12345").toString(2))
        println(BigInteger("12345").toString(4))
        println(BigInteger("12345").toString(10))
        println(BigInteger("12345").toString(16))
    }

    // Big Integer
    abstract val Long.bi: BigInt
    abstract val Int.bi: BigInt
    abstract val String.bi: BigInt
    abstract fun String.bi(radix: Int): BigInt
}

class BigIntCompareWithJVMTestCommon : AbstractBigIntCompareWithJVMTest() {
    override val Long.bi: BigInt get() = CommonBigInt(this)
    override val Int.bi: BigInt get() = CommonBigInt(this)
    override val String.bi: BigInt get() = CommonBigInt(this)
    override fun String.bi(radix: Int): BigInt = CommonBigInt(this, radix)

    @Test
    override fun testRightShift() = testBinary { jvmL, jvmR, kL, kR ->
        Result(">>", "${jvmL / (1 shl 16).toBigInteger()}", "${kL shr 16}")
    }

    @Test
    override fun testRightShift2() = testBinary { jvmL, jvmR, kL, kR ->
        Result(">>", "${jvmL / (1 shl 27).toBigInteger()}", "${kL shr 27}")
    }
}

class BigIntCompareWithJVMTestJVM : AbstractBigIntCompareWithJVMTest() {
    override val Long.bi: BigInt get() = JvmBigInt.create(this)
    override val Int.bi: BigInt get() = JvmBigInt.create(this)
    override val String.bi: BigInt get() = JvmBigInt.create(this)
    override fun String.bi(radix: Int): BigInt = JvmBigInt.create(this, radix)
}
