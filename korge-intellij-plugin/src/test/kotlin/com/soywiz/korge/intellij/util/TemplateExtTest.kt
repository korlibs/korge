package com.soywiz.korge.intellij.util

import org.junit.Test
import kotlin.test.*

class TemplateExtTest {
	@Test
	fun test() {
		assertEquals("hello world", renderTemplate("hello \$hello", mapOf("hello" to "world")))
	}
}
