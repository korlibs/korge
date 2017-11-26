package com.soywiz.korge.view

import com.soywiz.korge.DebugBitmapFont
import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.bitmapfont.toKorge
import com.soywiz.korge.input.onOut
import com.soywiz.korge.input.onOver
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Rectangle
import org.junit.Test
import kotlin.test.assertEquals

class ViewsJvmTest : ViewsForTesting() {
	@Test
	fun textGetBounds() = viewsTest {
		val font = DebugBitmapFont.DEBUG_BMP_FONT.toKorge(views, mipmaps = false)
		val text = views.text("Hello World", font = font, textSize = 8.0)
		val text2 = views.text("Hello World", font = font, textSize = 16.0)
		assertEquals(Rectangle(0, 0, 77, 8), text.globalBounds)
		assertEquals(Rectangle(0, 0, 154, 16), text2.globalBounds)
	}
}
