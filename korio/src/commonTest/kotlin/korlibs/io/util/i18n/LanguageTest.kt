package korlibs.io.util.i18n

import korlibs.logger.*
import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageTest {
    val logger = Logger("LanguageTest")
	@Test
	fun test() {
        val currentLanguage = Language.CURRENT
		val initialSystem = Language.SYSTEM
		val initialSystemLangs = Language.SYSTEM_LANGS
		logger.debug { "Language.CURRENT: ${Language.CURRENT}" }
		logger.debug { "Language.SYSTEM: ${Language.SYSTEM}" }
		logger.debug { "Language.SYSTEM: ${Language.SYSTEM_LANGS}" }
		Language.CURRENT = Language.SPANISH
        logger.debug { "Language.CURRENT: ${Language.CURRENT}" }
		assertEquals(Language.SPANISH, Language.CURRENT)
		assertEquals(initialSystem, Language.SYSTEM)
		assertEquals(initialSystemLangs, Language.SYSTEM_LANGS)
	}
}
