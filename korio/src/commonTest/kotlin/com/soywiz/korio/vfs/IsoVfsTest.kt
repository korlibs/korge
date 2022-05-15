package com.soywiz.korio.vfs

import com.soywiz.korio.async.suspendTestNoBrowser
import com.soywiz.korio.file.fullName
import com.soywiz.korio.file.std.openAsIso
import com.soywiz.korio.file.std.resourcesVfs
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals

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
