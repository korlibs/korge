package com.soywiz.klock

import com.soywiz.klock.wrapped.WDateTime
import com.soywiz.klock.wrapped.wrapped
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SerializableDateTimeTest {
	@Test
	fun testSerializableInstances() {
		@Suppress("USELESS_IS_CHECK")
		assertEquals(true, DateTime.now().wrapped is WDateTime)
		@Suppress("USELESS_IS_CHECK")
		assertEquals(true, DateTime.nowLocal() is DateTimeTz)
	}
}
