package com.soywiz.korio.i18n

import com.soywiz.korio.util.i18n.Language
import kotlin.test.Test

class LanguageTest {
	@Test
	fun testThatLanguageCurrentDoNotThrowExceptions() {
		println("Language.CURRENT: " + Language.CURRENT)
	}
}
