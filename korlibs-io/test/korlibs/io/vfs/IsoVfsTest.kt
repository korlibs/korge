package korlibs.io.vfs

import korlibs.io.async.suspendTestNoBrowser
import korlibs.io.file.fullName
import korlibs.io.file.std.openAsIso
import korlibs.io.file.std.resourcesVfs
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoVfsTest {
	@Test
	fun testIso() = suspendTestNoBrowser {
		resourcesVfs["isotest.iso"].openAsIso { isotestIso ->
			assertEquals(
				listOf("/HELLO", "/HELLO/WORLD.TXT"),
				isotestIso.listRecursive().map { it.fullName }.toList()
			)

			// Case insensitive!
			assertEquals(
				"WORLD!",
				isotestIso["hello"]["world.txt"].readString()
			)
		}
	}
}
