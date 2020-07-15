package com.soywiz.korio.async

import com.soywiz.klock.*
import kotlinx.coroutines.*
import kotlin.test.*

class AsyncQueueTest {
	@Test
	fun test() = suspendTest {
		val completed = CompletableDeferred<Unit>()
		val queue = AsyncQueue().withContext()
		var log = ""

		queue {
			delay(10.milliseconds)
			log += "a"
		}
		queue {
			delay(5.milliseconds)
			log += "b"
		}
		queue {
			delay(1.milliseconds)
			log += "c"
			completed.complete(Unit)
		}
		completed.await()
		assertEquals("abc", log)
	}
}