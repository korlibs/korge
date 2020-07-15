package com.soywiz.klogger.test

import com.soywiz.klogger.*
import kotlin.test.*

class LoggerTest {
	var out = listOf<String>()

	@Test
	fun simple() {
		//val out = arrayListOf<String>()
		//var out = listOf<String>()
		val logger = Logger("demo")
		logger.output = object : Logger.Output {
			override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
				out += "${logger.name}: $level: $msg"
			}
		}
		logger.level = Logger.Level.INFO
		logger.warn { "mywarn" }
		logger.info { "myinfo" }
		logger.trace { "mytrace" }
		assertEquals(listOf("demo: WARN: mywarn", "demo: INFO: myinfo"), out)

		logger.level = Logger.Level.WARN
		logger.warn { "mywarn" }
		logger.info { "myinfo" }
		logger.trace { "mytrace" }
		assertEquals(listOf("demo: WARN: mywarn", "demo: INFO: myinfo", "demo: WARN: mywarn"), out)
	}
}