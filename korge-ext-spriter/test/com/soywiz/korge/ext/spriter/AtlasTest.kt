package com.soywiz.korge.ext.spriter

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Test

class AtlasTest {
	@Test
	fun name() = syncTest {
		val atlas = KorgeAtlas.loadJsonSpriter(ResourcesVfs["demo.json"].readString())
	}
}
