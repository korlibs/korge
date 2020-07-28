package com.soywiz.korge.intellij.config

import kotlin.test.*

class KorgeGlobalSettingsTest {
	@Test
	fun test() {
		assertEquals(+1, KorgeGlobalSettings.compareKorgeTemplateVersion("<korge-templates version=\"1\">", "<korge-templates version=\"2\">"))
		assertEquals(-1, KorgeGlobalSettings.compareKorgeTemplateVersion("<korge-templates version=\"2\">", "<korge-templates version=\"1\">"))
		assertEquals(0, KorgeGlobalSettings.compareKorgeTemplateVersion("<korge-templates version=\"1\">", "<korge-templates version=\"1\">"))
		assertEquals(+1, KorgeGlobalSettings.compareKorgeTemplateVersion("<korge-templates>", "<korge-templates version=\"1\">"))
		assertEquals(-1, KorgeGlobalSettings.compareKorgeTemplateVersion("<korge-templates version=\"1\">", "<korge-templates>"))
	}
}