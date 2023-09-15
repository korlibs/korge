package korlibs.io.ds

import korlibs.memory.ByteArrayBuilder
import korlibs.encoding.hex
import kotlin.test.Test
import kotlin.test.assertEquals

class ByteArrayBuilderTest {
	@Test
	fun name() {
		val bb = ByteArrayBuilder()
		bb.append(byteArrayOf(1))
		bb.append(byteArrayOf(2, 3))
		bb.append(4)
		bb.append(byteArrayOf(5))
		assertEquals("0102030405", bb.toByteArray().hex)
	}
}
