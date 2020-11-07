package com.soywiz.korge.ext.fla

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class FlaTest {
	@Test
	fun name() = suspendTestNoJs {
		val fla = Fla.read(resourcesVfs["simple1.fla"])
	}
}
