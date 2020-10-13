package com.soywiz.korge.intellij.module

import org.junit.Test
import kotlin.test.*

internal class KorgeProjectTemplateTest {
	@Test
	fun test() {
		val template = KorgeProjectTemplate.fromXml("<korge-templates><versions><version>1.0</version><version>2.0</version></versions></korge-templates>")
		assertEquals("1.0", template.versions.versions.first().text)
		assertEquals("2.0", template.versions.versions.last().text)
	}

	@Test
	fun testRead() {
		val template = KorgeProjectTemplate.fromEmbeddedResource()
		assertTrue(template.features.features.isNotEmpty())
	}
}
