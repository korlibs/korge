package com.soywiz.korio.serialization.xml

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class Xml2Test {
	@Test
	fun name2() = suspendTestNoBrowser {
		val xml = resourcesVfs["test.xml"].readXml()
		assertEquals("test", xml.name)
		assertEquals("hello", xml.text)
	}
}