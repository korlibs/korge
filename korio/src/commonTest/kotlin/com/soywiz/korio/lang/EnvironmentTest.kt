package com.soywiz.korio.lang

import kotlin.test.*

class EnvironmentTest {
	@Test
	fun test() {
		println(Environment["path"])
		println(Environment["PATH"])
		println(Environment.getAll())
	}

    @Test
    fun testExpand() {
        // Windows
        Environment(
            "HOMEDrive" to "C:",
            "Homepath" to "\\Users\\soywiz",
        ).also { env ->
            assertEquals("C:\\Users\\soywiz/.game", env.expand("~/.game"))
        }

        // Linux
        Environment(
            "hOme" to "/home/soywiz",
        ).also { env ->
            assertEquals("/home/soywiz/.game", env.expand("~/.game"))
        }
    }
}
