package korlibs.io.serialization.xml

import korlibs.io.async.suspendTestNoBrowser
import korlibs.io.file.std.resourcesVfs
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
