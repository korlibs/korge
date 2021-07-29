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
        EnvironmentCustom(mutableMapOf(
            "HOMEDrive" to "C:",
            "Homepath" to "\\Users\\soywiz",
        )).also { env ->
            assertEquals("C:\\Users\\soywiz/.game", env.expand("~/.game"))
        }

        // Linux
        EnvironmentCustom(mutableMapOf(
            "hOme" to "/home/soywiz",
        )).also { env ->
            assertEquals("/home/soywiz/.game", env.expand("~/.game"))
        }
    }
}
