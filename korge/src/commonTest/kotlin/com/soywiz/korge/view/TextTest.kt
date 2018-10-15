package com.soywiz.korge.view

import com.soywiz.korag.log.*
import com.soywiz.korge.render.*
import com.soywiz.korio.async.*
import kotlin.test.*

class TextTest {
	@Test
	fun testRender() = suspendTestExceptJs {
		val text = Text()
		val ag = LogAG()
		text.render(RenderContext(ag))
	}
}