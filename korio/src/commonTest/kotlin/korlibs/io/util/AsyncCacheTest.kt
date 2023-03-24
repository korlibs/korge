package korlibs.io.util

import korlibs.time.milliseconds
import korlibs.io.async.AsyncCache
import korlibs.io.async.AsyncCacheGen
import korlibs.io.async.delay
import korlibs.io.async.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncCacheTest {
	@Test
	fun test() = suspendTest {
		val cache = AsyncCache()
		assertEquals("a", cache("key1") { "a" })
		assertEquals("a", cache("key1") { "b" })
	}

	@Test
	fun test2() = suspendTest {
		var count = 0
		val cache = AsyncCacheGen { delay(1.milliseconds); "$it${count++}" }
		assertEquals("a0", cache("a"))
		assertEquals("a0", cache("a"))
		assertEquals("b1", cache("b"))
		assertEquals("b1", cache("b"))
	}
}