package com.soywiz.korge.animate

import org.junit.Assert
import org.junit.Test

class TimedTest {
	val tostr = fun(index: Int, left: String?, right: String?, ratio: Double) = "$index,$left,$right,$ratio"

	@Test
	fun name() {
		val timed = Timed<String>()
		timed.add(2, "a")
		timed.add(4, "b")
		timed.add(6, "c")
		Assert.assertEquals("0,null,a,0.0", timed.findAndHandle(-1, tostr))
		Assert.assertEquals("0,a,null,0.0", timed.findAndHandle(2, tostr))
		Assert.assertEquals("0,a,b,0.5", timed.findAndHandle(3, tostr))
		Assert.assertEquals("1,b,null,0.0", timed.findAndHandle(4, tostr))
		Assert.assertEquals("1,b,c,0.5", timed.findAndHandle(5, tostr))
		Assert.assertEquals("2,c,null,0.0", timed.findAndHandle(6, tostr))
		Assert.assertEquals("3,c,null,1.0", timed.findAndHandle(10, tostr))
	}

	@Test
	fun repeated() {
		val timed = Timed<String>()
		timed.add(1, "a")
		timed.add(1, "b")
		timed.add(1, "c")
		Assert.assertEquals(listOf("a", "b", "c"), timed.getRangeValues(1, 1))
	}

	@Test
	fun unsorted() {
		Timed<String>().apply {
			add(6, "c")
			add(4, "b")
			add(8, "d")
			add(2, "a")
			Assert.assertEquals(
				"[(2, a), (4, b), (6, c), (8, d)]",
				entries.toString()
			)
		}

		Timed<String>().apply {
			add(2, "a")
			add(4, "b")
			add(6, "c")
			add(8, "d")
			Assert.assertEquals(
				"[(2, a), (4, b), (6, c), (8, d)]",
				entries.toString()
			)
		}
	}
}
