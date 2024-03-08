package korlibs.io.vfs

import korlibs.time.DateTime
import korlibs.time.TimeProvider
import korlibs.io.async.suspendTest
import korlibs.io.file.SimpleStorage
import korlibs.io.file.fullName
import korlibs.io.file.std.toVfs
import korlibs.io.lang.toByteArray
import korlibs.io.serialization.json.toJson
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals

class MapLikeStorageVfsTest {
    @Test
    fun testDeleteFile() = suspendTest {
        val storage = MySimpleStorage()
        val root = storage.toVfs(TimeProvider { DateTime.fromUnixMillis(0L) })

        root["demo"].mkdir()
        root["demo/test.txt"].writeString("hello")
        assertEquals(
            """{"korio_stats_v1_\/":"{\"isFile\":false,\"size\":0,\"children\":[\"\\\/demo\"],\"createdTime\":0,\"modifiedTime\":0}","korio_stats_v1_\/demo":"{\"isFile\":false,\"size\":0,\"children\":[\"\\\/demo\\\/test.txt\"],\"createdTime\":0,\"modifiedTime\":0}","korio_chunk0_v1_\/demo\/test.txt":"68656c6c6f","korio_stats_v1_\/demo\/test.txt":"{\"isFile\":true,\"size\":5,\"children\":[],\"createdTime\":0,\"modifiedTime\":0}"}""",
            storage.map.toJson().replace(".0", "")
        )
        assertEquals("hello", root["demo/test.txt"].readString())
        root["demo/test.txt"].delete()
        assertEquals(
            """{"korio_stats_v1_\/":"{\"isFile\":false,\"size\":0,\"children\":[\"\\\/demo\"],\"createdTime\":0,\"modifiedTime\":0}","korio_stats_v1_\/demo":"{\"isFile\":false,\"size\":0,\"children\":[],\"createdTime\":0,\"modifiedTime\":0}"}""",
            storage.map.toJson().replace(".0", "")
        )
    }

    @Test
	fun name() = suspendTest {
		val root = MySimpleStorage().toVfs()

		assertEquals(listOf(), root.list().toList())
		root["demo.txt"].writeBytes("hello".toByteArray())
		assertEquals(listOf("/demo.txt"), root.list().toList().map { it.fullName })
		assertEquals("hello", root["demo.txt"].readString())
		root["demo"].mkdir()
		root["demo"].mkdir()
		assertEquals(listOf("/demo.txt", "/demo"), root.list().toList().map { it.fullName })
		root["demo/hello/world/yay"].mkdir()
		root["demo/hello/world/yay/file.txt"].writeString("DEMO")

		assertEquals(
			"[/demo.txt, /demo, /demo/hello, /demo/hello/world, /demo/hello/world/yay, /demo/hello/world/yay/file.txt]",
			root.listRecursive().toList().map { it.fullName }.toString()
		)

		assertEquals(true, root["demo.txt"].exists())
		assertEquals(5, root["demo.txt"].size())

		assertEquals(false, root["unexistant"].exists())
	}

    class MySimpleStorage : SimpleStorage {
        val map = LinkedHashMap<String, String>()
        override suspend fun get(key: String): String? = map[key]
        override suspend fun set(key: String, value: String) { map[key] = value }
        override suspend fun remove(key: String) { map.remove(key) }
        fun keysToString() = map.keys.sorted().joinToString(",")
    }
}
