package com.soywiz.korge.ext.fla

import com.soywiz.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class FlaTest {
	@Test
	@Ignore
	fun name() = suspendTest {
		val fla = Fla.read(MyResourcesVfs["simple1.fla"])
		//val fla = Fla.read(ResourcesVfs["simple1"])
	}
}
