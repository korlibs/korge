package com.soywiz.korge.ext.fla

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Test

class FlaTest {
	@Test
	fun name() = syncTest {
		val fla = Fla.read(ResourcesVfs["simple1.fla"])
		//val fla = Fla.read(ResourcesVfs["simple1"])
	}
}
