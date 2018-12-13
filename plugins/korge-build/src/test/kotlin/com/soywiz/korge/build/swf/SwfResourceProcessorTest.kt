package com.soywiz.korge.build.swf

import com.soywiz.korge.animate.serialization.*
import com.soywiz.korge.build.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class SwfResourceProcessorTest {
	@Test
	fun name() = suspendTest {
		val memoryVfs = MemoryVfs()
		val processed1 = SwfResourceProcessor.process(ResourcesVfs["shapes.swf"], memoryVfs)
		assertEquals(true, processed1)
		assertEquals(true, memoryVfs["shapes.ani"].exists())
		assertEquals(true, memoryVfs["shapes.ani.meta"].exists())
		assertEquals(
			ResourceVersion(
				name = "shapes.swf",
				loaderVersion = AniFile.VERSION,
				sha1 = "7b8d9db612be09d0fc6f92e9eb2278bf00a62da3",
				configSha1 = "c012fd0b2067923af7d69c7bb690534c0ec7d246"
			),
			ResourceVersion.readMeta(memoryVfs["shapes.ani.meta"])
		)
		val processed2 = SwfResourceProcessor.process(ResourcesVfs["shapes.swf"], memoryVfs)
		assertEquals(false, processed2)
	}
}
