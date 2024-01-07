package korlibs.datastructure

import kotlin.test.assertEquals

class ComputedTest {
	class Format(override var parent: Format? = null) : Computed.WithParent<Format> {
		var size: Int? = null

		val computedSize by Computed(Format::size) { 10 }
	}

	@kotlin.test.Test
	fun name() {
		val f2 = Format()
		val f1 = Format(f2)
		assertEquals(10, f1.computedSize)
		f2.size = 12
		assertEquals(12, f1.computedSize)
		f1.size = 15
		assertEquals(15, f1.computedSize)
	}

	@kotlin.test.Test
	fun name2() {
		val f3 = Format()
		val f2 = Format(f3)
		val f1 = Format(f2)
		assertEquals(10, f1.computedSize)
		f3.size = 12
		assertEquals(12, f1.computedSize)
		f3.size = 15
		assertEquals(15, f1.computedSize)
		f2.size = 14
		assertEquals(14, f1.computedSize)
		f1.size = 13
		assertEquals(13, f1.computedSize)
	}
}
