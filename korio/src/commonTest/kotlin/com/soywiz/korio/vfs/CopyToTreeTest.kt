package com.soywiz.korio.vfs

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.fullName
import com.soywiz.korio.file.std.MemoryVfs
import com.soywiz.korio.lang.toByteArray
import com.soywiz.korio.stream.openAsync
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals

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
