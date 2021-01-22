package com.soywiz.klogger.test

import com.soywiz.klogger.Logger
import com.soywiz.klogger.atomic.KloggerAtomicRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

}
