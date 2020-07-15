package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.channels.*
import kotlin.test.*

/*
class MapLikeStorageVfsTest {
	@Test
	fun name() = suspendTest {
		val root = MySimpleStorage().toVfs()

		// @BUG: Circumvents https://github.com/JetBrains/kotlin-native/issues/2864
		//val root = (object : SimpleStorage {
		//	val map = LinkedHashMap<String, String>()
		//	override suspend fun get(key: String): String? = map[key]
		//	override suspend fun set(key: String, value: String) = run { map[key] = value }
		//	override suspend fun remove(key: String): Unit = run { map.remove(key) }
		//}).toVfs()

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
}

// @TODO: Circumvents https://github.com/JetBrains/kotlin-native/issues/2864
class MySimpleStorage : SimpleStorage {
	val map = LinkedHashMap<String, String>()
	override suspend fun get(key: String): String? = map[key]
	override suspend fun set(key: String, value: String) = run { map[key] = value }
	override suspend fun remove(key: String): Unit = run { map.remove(key) }
}
 */

