package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlin.test.*

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
