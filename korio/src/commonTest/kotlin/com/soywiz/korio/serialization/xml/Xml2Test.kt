package com.soywiz.korio.serialization.xml

import com.soywiz.korio.async.suspendTestNoBrowser
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals

class Xml2Test {
	@Test
	fun name2() = suspendTestNoBrowser {
		val xml = resourcesVfs["test.xml"].readXml()
		assertEquals("test", xml.name)
		assertEquals("hello", xml.text)
	}
}
