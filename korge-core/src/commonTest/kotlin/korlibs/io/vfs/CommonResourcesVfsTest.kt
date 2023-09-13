package korlibs.io.vfs

import korlibs.io.async.suspendTest
import kotlin.test.Test

class CommonResourcesVfsTest {
	@Test
	fun testCanReadResourceProperly() = suspendTest {
		//assertEquals("HELLO", resourcesVfs["resource.txt"].readString())
	}
}
