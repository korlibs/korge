package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class BitSetTest {
	fun BitSet.setCheckSeq(vararg bits: Boolean) {
		for (n in bits.indices) this[n] = bits[n]
		checkSeq(*bits)
	}

	fun BitSet.checkSeq(vararg bits: Boolean) {
		for (n in bits.indices) assertEquals(bits[n], this[n])
	}

	@Test
	fun justOneBit() {
		val v = BitSet(1)
		v[0] = false
		assertEquals(false, v[0])
		v[0] = true
		assertEquals(true, v[0])
	}

	@Test
	fun fewBits() {
		val v = BitSet(3)
		v.setCheckSeq(false, true, false)
		v.setCheckSeq(true, false, true)
		v.setCheckSeq(true, true, true)
		v.clear()
		v.checkSeq(false, false, false)
	}

	@Test
	fun empty() {
		BitSet(0)
	}

	@Test
	fun bits33() {
		val bs = BitSet(33)
		for (n in 0 until bs.size) {
			bs[n] = false
			assertEquals(false, bs[n])
			bs[n] = true
			assertEquals(true, bs[n])
		}
	}

	@Test
	fun unset() {
		val bs = BitSet(8)
		for (n in 0 until bs.size) bs[n] = true
		for (n in 0 until bs.size step 2) bs.unset(n)
		assertEquals(listOf(false, true, false, true, false, true, false, true), bs.toList())
	}
}
