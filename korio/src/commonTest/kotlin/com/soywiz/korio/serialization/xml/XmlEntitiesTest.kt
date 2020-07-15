package com.soywiz.korio.serialization.xml

import kotlin.test.*

class XmlEntitiesTest {
	@Test
	fun testDecode() {
		assertEquals("hello", Xml.Entities.decode("hello"))
		assertEquals("\"", Xml.Entities.decode("&quot;"))
		assertEquals("hello\"world", Xml.Entities.decode("hello&quot;world"))
		assertEquals("hello\"world\"", Xml.Entities.decode("hello&quot;world&quot;"))
	}

	@Test
	fun testEncode() {
		assertEquals("hello", Xml.Entities.encode("hello"))
		assertEquals("&quot;", Xml.Entities.encode("\""))
		assertEquals("hello&quot;world", Xml.Entities.encode("hello\"world"))
		assertEquals("hello&quot;world&quot;", Xml.Entities.encode("hello\"world\""))
	}
}
