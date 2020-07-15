package com.soywiz.korio.util

import kotlin.test.*

class CompareUtilTest {
	@Test
	fun test() {
		class Point(val x: Int, val y: Int)
		val ap = Comparator<Point> { l, r -> l.x.compareTo(r.x).compareToChain { l.y.compareTo(r.y) } }
		assertEquals(0, ap.compare(Point(0, 0), Point(0, 0)))
		assertEquals(-1, ap.compare(Point(-1, 0), Point(0, 0)))
		assertEquals(+1, ap.compare(Point(+1, 0), Point(0, 0)))
		assertEquals(-1, ap.compare(Point(0, -1), Point(0, 0)))
		assertEquals(+1, ap.compare(Point(0, +1), Point(0, 0)))
	}
}