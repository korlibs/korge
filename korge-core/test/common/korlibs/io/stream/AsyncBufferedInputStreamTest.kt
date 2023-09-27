package korlibs.io.stream

import korlibs.io.async.suspendTest
import korlibs.io.lang.UTF8
import korlibs.io.lang.toString
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncBufferedInputStreamTest {
	@Test
	fun test() = suspendTest {
		val buffered = "hello\nworld\ndemo".openAsync().bufferedInput()
		assertEquals("hello", buffered.readUntil('\n'.toByte(), including = false).toString(UTF8))
		assertEquals("world\n", buffered.readUntil('\n'.toByte(), including = true).toString(UTF8))
		assertEquals("demo", buffered.readUntil('\n'.toByte(), including = true).toString(UTF8))
	}
}
