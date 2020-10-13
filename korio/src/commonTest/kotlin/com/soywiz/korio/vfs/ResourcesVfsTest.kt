package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.OS
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlin.test.*

class ResourcesVfsTest {
	@Test
	fun name() = suspendTest({ OS.isJvm }) {
		println("[A]")
		val listing = resourcesVfs["tresfolder"].list()
		println("[B]")

		for (v in resourcesVfs["tresfolder"].list().filter { it.extensionLC == "txt" }.toList()) {
			println(v)
		}

		assertEquals(
			"[a.txt, b.txt]",
			resourcesVfs["tresfolder"].list().filter { it.extensionLC == "txt" }.toList().map { it.baseName }.sorted().toString()
		)
	}
}
