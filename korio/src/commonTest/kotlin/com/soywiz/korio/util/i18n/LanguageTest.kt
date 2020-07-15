package com.soywiz.korio.util.i18n

import kotlin.test.*

class LanguageTest {
	@Test
	fun test() {
		val initialSystem = Language.SYSTEM
		val initialSystemLangs = Language.SYSTEM_LANGS
		println("Language.CURRENT: ${Language.CURRENT}")
		println("Language.SYSTEM: ${Language.SYSTEM}")
		println("Language.SYSTEM: ${Language.SYSTEM_LANGS}")
		Language.CURRENT = Language.SPANISH
		println("Language.CURRENT: ${Language.CURRENT}")
		assertEquals(Language.SPANISH, Language.CURRENT)
		assertEquals(initialSystem, Language.SYSTEM)
		assertEquals(initialSystemLangs, Language.SYSTEM_LANGS)
	}
}