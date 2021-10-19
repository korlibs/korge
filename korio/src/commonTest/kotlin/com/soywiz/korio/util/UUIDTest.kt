package com.soywiz.korio.util

import kotlin.test.*

class UUIDTest {
	@Test
	fun name() {
        if (OS.isAndroid) return // Do not test in android since Process is not mocked
		assertEquals("00000000-0000-0000-0000-000000000000", UUID("00000000-0000-0000-0000-000000000000").toString())
		assertEquals(4, UUID.randomUUID().version)
		assertEquals(1, UUID.randomUUID().variant)
		//assertEquals("00000000-0000-0000-0000-000000000000", UUID.randomUUID().toString())
	}
}
