package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlin.test.*

class CopyToTreeTest {
	@Test
	fun name() = suspendTest {
		val mem = MemoryVfs(
			linkedMapOf(
				"root.txt" to "hello".toByteArray().openAsync(),
				"hello/world.txt" to "hello".toByteArray().openAsync()
			)
		)
		val out = MemoryVfs()
		mem.copyToTree(out)
		assertEquals(
			"[/root.txt, /hello, /hello/world.txt]",
			out.listRecursive().map { it.fullName }.toList().toString()
		)
	}
}
