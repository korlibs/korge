package com.soywiz.korio.net.http

import com.soywiz.klock.*
import kotlin.test.*

class HttpDateTest {
	@Test
	fun name() {
		assertEquals("Mon, 18 Sep 2017 23:58:45 UTC", Http.Date.format(1505779125916L))
	}

	@Test
	fun reversible() {
		val STR = "Tue, 19 Sep 2017 00:58:45 UTC"

		assertEquals(STR, Http.Date.format(Http.Date.parse(STR)))
	}
}