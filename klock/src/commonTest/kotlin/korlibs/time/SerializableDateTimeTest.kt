package korlibs.time

import korlibs.time.wrapped.WDateTime
import korlibs.time.wrapped.wrapped
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializableDateTimeTest {
	@Test
	fun testSerializableInstances() {
		@Suppress("USELESS_IS_CHECK")
		assertEquals(true, DateTime.now().wrapped is WDateTime)
		@Suppress("USELESS_IS_CHECK")
		assertEquals(true, DateTime.nowLocal() is DateTimeTz)
	}
}