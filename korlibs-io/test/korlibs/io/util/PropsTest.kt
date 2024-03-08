package korlibs.io.util

import korlibs.io.async.suspendTest
import korlibs.io.file.std.MemoryVfsMix
import kotlin.test.Test
import kotlin.test.assertEquals

class PropsTest {
	@Test
	fun test() = suspendTest {
		val vfs = MemoryVfsMix(
			"hello.properties" to """
    			hello=world
			""".trimIndent()
		)
		val props = vfs["hello.properties"].loadProperties()
		assertEquals("world", props["hello"])
		props["demo"] = "test"
		assertEquals("hello=world\ndemo=test", props.serialize())
		vfs["world.properties"].saveProperties(props)
		assertEquals("hello=world\ndemo=test", vfs["world.properties"].readString())
	}
}
