package com.soywiz.korio.i18n

import com.soywiz.klogger.*
import com.soywiz.korio.util.i18n.Language
import kotlin.test.*

class LanguageTest {
    val logger = Logger("LanguageTest")
	@Test
	fun testThatLanguageCurrentDoNotThrowExceptions() {
        val string = "Language.CURRENT: ${Language.CURRENT}" // Ensure Language.CURRENT is called by putting it outside logger
        logger.debug { string }
	}
}
