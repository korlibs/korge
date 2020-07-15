package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import kotlin.test.*

class CommonResourcesVfsTest {
	@Test
	fun testCanReadResourceProperly() = suspendTest {
		//assertEquals("HELLO", resourcesVfs["resource.txt"].readString())
	}
}