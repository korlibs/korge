package com.soywiz.korfl

import org.junit.Assert
import org.junit.Test

class TimedTest {
	@Test
	fun name() {
		val tostr = fun(index: Int, left: String?, right: String?, ratio: Double) = "$index,$left,$right,$ratio"
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
}
