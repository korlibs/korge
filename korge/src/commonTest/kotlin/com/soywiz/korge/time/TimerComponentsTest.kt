package com.soywiz.korge.time

import com.soywiz.klock.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import kotlin.test.*

class TimerComponentsTest {
	@Test
    @Ignore
	fun testTimerAcummulated() = suspendTest {
		var log = ""
		val view = DummyView()
		log += "a"
		view.timer(5.milliseconds) {
			log += "b"
			view.timer(5.milliseconds) {
				log += "c"
			}
		}
		assertEquals("a", log)
		view.updateSingleView(7.0)
		assertEquals("ab", log)
		view.updateSingleView(3.0)
		assertEquals("abc", log)
	}
}
