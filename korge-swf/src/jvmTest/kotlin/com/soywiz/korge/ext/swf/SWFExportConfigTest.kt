package com.soywiz.korge.ext.swf

import com.soywiz.korim.vector.*
import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.dynamic.serialization.*
import com.soywiz.korio.serialization.*
import com.soywiz.korio.serialization.yaml.*
import com.soywiz.korio.util.*
import kotlin.test.*

class SWFExportConfigTest {
	@Test
	fun name() {
		val config = Yaml.decodeToType<SWFExportConfig>(
			"""
				|mipmaps: false
				|rasterizerMethod: X2
			""".trimMargin(),
			Mapper.jvmFallback()
		)

		assertEquals(
			SWFExportConfig(
				mipmaps = false,
				rasterizerMethod = ShapeRasterizerMethod.X2
			),
			config
		)
	}
}
