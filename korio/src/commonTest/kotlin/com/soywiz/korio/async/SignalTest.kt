package com.soywiz.korio.async

import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.test.*

class SignalTest {
	@Test
	fun name() = suspendTest {
		var out = ""
		val s = Signal<Int>()
		assertEquals(0, s.listenerCount)
		val c1 = s.add { out += "[$it]" }
		assertEquals(1, s.listenerCount)
		s(1)
		val c2 = s.add { out += "{$it}" }
		assertEquals(2, s.listenerCount)
		s(2)
		s.once { out += "<$it>" }
		assertEquals(3, s.listenerCount)
		s(3)
		assertEquals(2, s.listenerCount)
		c2.close()
		assertEquals(1, s.listenerCount)
		s(4)
		c1.close()
		assertEquals(0, s.listenerCount)
		s(5)
		assertEquals(0, s.listenerCount)
		assertEquals("[1][2]{2}[3]{3}<3>[4]", out)
	}

	//@Test
    ////Flaky!
	//fun name2() = suspendTest {
	//	var out = ""
	//	val s = Signal<Int>()
	//	launchImmediately(coroutineContext) {
	//		try {
	//			withTimeout(200) {
	//				while (true) {
	//					out += "" + s.waitOneBase()
	//				}
	//			}
	//		} catch (e: CancellationException) {
	//			out += "<cancel>"
	//		}
	//	}
	//	s(1)
	//	delay(20)
	//	s(2)
	//	delay(220)
	//	s(3)
	//	delay(120)
	//	assertEquals("12<cancel>", out)
	//}

	@Test
	fun executeAndWait1() = suspendTest {
		val signal = Signal<String>()
		assertEquals("hello", signal.executeAndWaitSignal {
			signal("hello")
		})
	}

	@Test
	fun executeAndWait2() = suspendTest {
		val signal1 = Signal<Unit>()
		val signal2 = Signal<Unit>()
		assertEquals("1", mapOf(signal1 to "1", signal2 to "2").executeAndWaitAnySignal {
			signal1(Unit)
		})
		assertEquals("2", mapOf(signal1 to "1", signal2 to "2").executeAndWaitAnySignal {
			signal2(Unit)
		})
	}

	@Test
	fun executeAndWait3() = suspendTest {
		val signal1 = Signal<String>()
		val signal2 = Signal<String>()
		assertEquals(signal2 to "hello", listOf(signal1, signal2).executeAndWaitAnySignal {
			signal2("hello")
		})
	}
}
