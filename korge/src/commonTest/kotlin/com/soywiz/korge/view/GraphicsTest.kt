package com.soywiz.korge.view

import com.soywiz.korag.log.*
import com.soywiz.korge.render.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class GraphicsTest {
	@Test
	@Ignore // @TODO: Enable with Korim is updated with its fixes for Node.JS
	fun test() = suspendTest {
		val g = Graphics().apply {
			fill(Colors.RED) {
				rect(-50, -50, 100, 100)
			}
		}
		val rc = TestRenderContext()
		g.render(rc)
		val bmp = g.bitmap.bmp.toBMP32()
		assertEquals(Size(100, 100), bmp.size)
		assertEquals("#ff0000ff", bmp[0, 0].hexString)
		assertEquals("#ff0000ff", bmp[99, 99].hexString)
		assertEquals(-50.0, g._sLeft)
		assertEquals(-50.0, g._sTop)

	}
}

fun TestRenderContext() = RenderContext(LogAG())
