package com.soywiz.korio.vfs

import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.json.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlin.test.*

class MapLikeStorageVfsTest {
    @Test
    fun testDeleteFile() = suspendTest {
        val storage = MySimpleStorage()
        val root = storage.toVfs(TimeProvider { DateTime.fromUnix(0L) })

        root["demo"].mkdir()
        root["demo/test.txt"].writeString("hello")
        assertEquals(
            """{"korio_stats_v1_\/":"{\"createdTime\":0.0,\"modifiedTime\":0.0,\"isFile\":false,\"size\":0.0,\"children\":[\"\\\/demo\"]}","korio_stats_v1_\/demo":"{\"createdTime\":0.0,\"modifiedTime\":0.0,\"isFile\":false,\"size\":0.0,\"children\":[\"\\\/demo\\\/test.txt\"]}","korio_chunk0_v1_\/demo\/test.txt":"68656c6c6f","korio_stats_v1_\/demo\/test.txt":"{\"createdTime\":0.0,\"modifiedTime\":0.0,\"isFile\":true,\"size\":5.0,\"children\":[]}"}""",
            storage.map.toJson()
        )
        assertEquals("hello", root["demo/test.txt"].readString())
        root["demo/test.txt"].delete()
        assertEquals(
            """{"korio_stats_v1_\/":"{\"createdTime\":0.0,\"modifiedTime\":0.0,\"isFile\":false,\"size\":0.0,\"children\":[\"\\\/demo\"]}","korio_stats_v1_\/demo":"{\"createdTime\":0.0,\"modifiedTime\":0.0,\"isFile\":false,\"size\":0.0,\"children\":[]}"}""",
            storage.map.toJson()
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
        override suspend fun set(key: String, value: String) = run { map[key] = value }
        override suspend fun remove(key: String): Unit = run { map.remove(key) }
        fun keysToString() = map.keys.sorted().joinToString(",")
    }
}
