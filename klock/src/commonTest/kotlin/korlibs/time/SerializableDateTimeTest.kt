package korlibs.time

import kotlin.test.*

class SerializableDateTimeTest {
	@Test
	fun testSerializableInstances() {
		@Suppress("USELESS_IS_CHECK")
		assertEquals(true, DateTime.nowLocal() is DateTimeTz)
	}
}
