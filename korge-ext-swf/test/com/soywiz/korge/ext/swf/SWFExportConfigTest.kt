package com.soywiz.korge.ext.swf

import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.serialization.yaml.Yaml
import org.junit.Assert
import org.junit.Test

class SWFExportConfigTest {
	@Test
	fun name() {
		val config = Yaml.decodeToType<SWFExportConfig>(
			"""
				|mipmaps: false
				|rasterizerMethod: X2
			""".trimMargin()
		)

		Assert.assertEquals(
			SWFExportConfig(mipmaps = false, rasterizerMethod = Context2d.ShapeRasterizerMethod.X2),
			config
		)
	}
}
