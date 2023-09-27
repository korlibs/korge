package korlibs.io.vfs

import korlibs.io.async.suspendTest
import korlibs.io.file.std.MemoryVfs
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryVfsTest {
	@Test
	fun name() = suspendTest {
		val log = ArrayList<String>()
		val mem = MemoryVfs()

		mem.watch {
			log += it.toString()
		}

		mem["item.txt"].writeString("test")
		mem["test"].mkdir()
		mem["test"].delete()
		delay(100)
		assertEquals(
			"[MODIFIED(NodeVfs[/item.txt]), CREATED(NodeVfs[/test]), DELETED(NodeVfs[/test])]",
			log.toString()
		)
	}
}
