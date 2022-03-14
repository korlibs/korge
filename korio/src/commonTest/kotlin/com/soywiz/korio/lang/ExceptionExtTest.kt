package com.soywiz.korio.lang

import com.soywiz.klogger.*
import kotlin.test.*

class ExceptionExtTest {
	@Test
	fun test() {
        val logs = Console.capture {
            printStackTrace()
        }
        assertEquals(1, logs.size)
	}
}
