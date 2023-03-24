package korlibs.logger.test

import korlibs.logger.Logger
import korlibs.logger.atomic.KloggerAtomicRef
import kotlin.test.*

class LoggerTest {
	private val out = KloggerAtomicRef(listOf<String>())

	@Test
	fun simple() {
		//val out = arrayListOf<String>()
		//var out = listOf<String>()
		val logger = Logger("demo")
		logger.output = object : Logger.Output {
			override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
				out.update { it + "${logger.name}: $level: $msg" }
			}
		}
		logger.level = Logger.Level.INFO
		logger.warn { "mywarn" }
		logger.info { "myinfo" }
		logger.trace { "mytrace" }
		assertEquals(listOf("demo: WARN: mywarn", "demo: INFO: myinfo"), out.value)

		logger.level = Logger.Level.WARN
		logger.warn { "mywarn" }
		logger.info { "myinfo" }
		logger.trace { "mytrace" }
		assertEquals(listOf("demo: WARN: mywarn", "demo: INFO: myinfo", "demo: WARN: mywarn"), out.value)
	}

    @Test
    fun defaultLevel() {
        Logger.defaultLevel = Logger.Level.ERROR
        assertTrue { Logger.defaultLevel == Logger.Level.ERROR }

        Logger.defaultLevel = Logger.Level.DEBUG
        assertTrue { Logger.defaultLevel == Logger.Level.DEBUG }
    }

    @Test
    fun testSameInstance() {
        val hello1 = Logger("hello")
        val hello2 = Logger("hello")
        assertSame(hello1, hello2)
    }
}