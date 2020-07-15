package com.soywiz.klock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SerializableDateTimeTest {
	@Test
	fun testSerializableInstances() {
		@Suppress("USELESS_IS_CHECK")
		assertEquals(true, DateTime.now().serializable() is SerializableDateTime)
		@Suppress("USELESS_IS_CHECK")
		assertEquals(true, DateTime.nowLocal() is DateTimeTz)
	}
}