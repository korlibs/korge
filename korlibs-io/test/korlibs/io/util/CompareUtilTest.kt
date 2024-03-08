package korlibs.io.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CompareUtilTest {
	@Test
	fun test() {
		class Comp(val x: Int, val y: Int)
		val ap = Comparator<Comp> { l, r -> l.x.compareTo(r.x).compareToChain { l.y.compareTo(r.y) } }
		assertEquals(0, ap.compare(Comp(0, 0), Comp(0, 0)))
		assertEquals(-1, ap.compare(Comp(-1, 0), Comp(0, 0)))
		assertEquals(+1, ap.compare(Comp(+1, 0), Comp(0, 0)))
		assertEquals(-1, ap.compare(Comp(0, -1), Comp(0, 0)))
		assertEquals(+1, ap.compare(Comp(0, +1), Comp(0, 0)))
	}
}
