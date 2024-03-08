package korlibs.io.lang

import korlibs.encoding.hex
import kotlin.test.Test
import kotlin.test.assertEquals

class UTF8Test {
	@Test
	fun test() {
		assertEquals(byteArrayOf('h'.toByte(), 'e'.toByte(), 'l'.toByte(), 'l'.toByte(), 'o'.toByte()).hex, "hello".toByteArray(UTF8).hex)
		assertEquals("hello", byteArrayOf('h'.toByte(), 'e'.toByte(), 'l'.toByte(), 'l'.toByte(), 'o'.toByte()).toString(UTF8))
	}
}
