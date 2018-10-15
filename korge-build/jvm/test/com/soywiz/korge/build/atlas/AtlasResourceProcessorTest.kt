package com.soywiz.korge.build.atlas

import com.soywiz.korge.build.ResourceVersion
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.async.toList
import com.soywiz.korio.vfs.MemoryVfs
import com.soywiz.korio.vfs.ResourcesVfs
import kotlin.test.assertEquals

class AtlasResourceProcessorTest {
	@Test
	fun name() = suspendTest {
		val memoryVfs = MemoryVfs()
		val processed1 = AtlasResourceProcessor.process(ResourcesVfs["simple.atlas"], memoryVfs)
		println(memoryVfs.listRecursive().toList())
		assertEquals(true, processed1)
		assertEquals(true, memoryVfs["simple.atlas.json"].exists())
		assertEquals(true, memoryVfs["simple.atlas.png"].exists())
		assertEquals(
			ResourceVersion(
				name = "simple.atlas",
				loaderVersion = 0,
				sha1 = "2001b3a895e3d9a8bee7267928a93941b6aee181",
				configSha1 = ""
			),
			ResourceVersion.readMeta(memoryVfs["simple.atlas.json.meta"])
		)
		val processed2 = AtlasResourceProcessor.process(ResourcesVfs["simple.atlas"], memoryVfs)
		assertEquals(false, processed2)
	}
}
