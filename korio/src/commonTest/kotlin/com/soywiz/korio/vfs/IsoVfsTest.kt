package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlin.test.*

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
