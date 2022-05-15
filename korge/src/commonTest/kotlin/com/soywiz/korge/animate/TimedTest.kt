package com.soywiz.korge.animate

import com.soywiz.klock.microseconds
import com.soywiz.korio.util.niceStr
import kotlin.test.Test
import kotlin.test.assertEquals

class TimedTest {
	// @TODO: kotlin-js kotlin.js inconsistency with double toString
	//val tostr = fun(index: Int, left: String?, right: String?, ratio: Double) = "$index,$left,$right,$ratio"
	//
	//@Test
	//fun name() {
	//	val timed = Timed<String>()
	//	timed.add(2, "a")
	//	timed.add(4, "b")
	//	timed.add(6, "c")
	//	assertEquals("0,null,a,0.0", timed.findAndHandle(-1, tostr))
	//	assertEquals("0,a,null,0.0", timed.findAndHandle(2, tostr))
	//	assertEquals("0,a,b,0.5", timed.findAndHandle(3, tostr))
	//	assertEquals("1,b,null,0.0", timed.findAndHandle(4, tostr))
	//	assertEquals("1,b,c,0.5", timed.findAndHandle(5, tostr))
	//	assertEquals("2,c,null,0.0", timed.findAndHandle(6, tostr))
	//	assertEquals("3,c,null,1.0", timed.findAndHandle(10, tostr))
	//}

	val tostr = fun(index: Int, left: String?, right: String?, ratio: Double) = "$index,$left,$right,${ratio.niceStr}"

    private val Int.t get() = this.microseconds

	@Test
	fun name() {
		val timed = Timed<String>()
		timed.add(2.t, "a")
		timed.add(4.t, "b")
		timed.add(6.t, "c")
		assertEquals("0,null,a,0", timed.findAndHandle((-1).t, tostr))
		assertEquals("0,a,null,0", timed.findAndHandle(2.t, tostr))
		assertEquals("0,a,b,0.5", timed.findAndHandle(3.t, tostr))
		assertEquals("1,b,null,0", timed.findAndHandle(4.t, tostr))
		assertEquals("1,b,c,0.5", timed.findAndHandle(5.t, tostr))
		assertEquals("2,c,null,0", timed.findAndHandle(6.t, tostr))
		assertEquals("3,c,null,1", timed.findAndHandle(10.t, tostr))
	}


	@Test
	fun repeated() {
		val timed = Timed<String>()
		timed.add(1.t, "a")
		timed.add(1.t, "b")
		timed.add(1.t, "c")
		assertEquals(listOf("a", "b", "c"), timed.getRangeValues(1.t, 1.t))
	}

	@Test
	fun unsorted() {
		Timed<String>().apply {
			add(6.t, "c")
			add(4.t, "b")
			add(8.t, "d")
			add(2.t, "a")
			assertEquals(
				"[(2, a), (4, b), (6, c), (8, d)]",
				entries.toString()
			)
		}

		Timed<String>().apply {
			add(2.t, "a")
			add(4.t, "b")
			add(6.t, "c")
			add(8.t, "d")
			assertEquals(
				"[(2, a), (4, b), (6, c), (8, d)]",
				entries.toString()
			)
		}
	}
}
