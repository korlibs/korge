package com.soywiz.korio.vfs

import com.soywiz.korio.async.suspendTest
import kotlin.test.Test

class CommonResourcesVfsTest {
	@Test
	fun testCanReadResourceProperly() = suspendTest {
		//assertEquals("HELLO", resourcesVfs["resource.txt"].readString())
	}
}
