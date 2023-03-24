package korlibs.io.vfs

import korlibs.io.async.suspendTest
import korlibs.io.file.fullName
import korlibs.io.file.std.MemoryVfsMix
import korlibs.io.lang.FileNotFoundException
import korlibs.io.util.expectException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals

class JailVfsTest {
	@Test
	fun name() = suspendTest {
		val mem = MemoryVfsMix(
			"hello/secret.txt" to "SECRET!",
			"hello/world/test.txt" to "HELLO WORLD!"
		)

		assertEquals(
			"[/hello, /hello/secret.txt, /hello/world, /hello/world/test.txt]",
			mem.listRecursive().map { it.fullName }.toList().toString()
		)

		val worldFolder = mem["hello/world"]
		val worldFolderJail = mem["hello/world"].jail()

		assertEquals(
			"SECRET!",
			worldFolder["../secret.txt"].readString()
		)

		expectException<FileNotFoundException> {
			worldFolderJail["../secret.txt"].readString()
		}
	}
}
