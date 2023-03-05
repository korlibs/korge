package com.soywiz.korge.bitmapfont

import com.soywiz.korge.testing.*
import com.soywiz.korge.view.*
import com.soywiz.korim.font.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class BitmapFontTest {
	@Test
	fun simple() = korgeScreenshotTest(SizeInt(64, 20)) {
		val font = resourcesVfs["font/font.fnt"].readBitmapFont()
		assertEquals(81, font.glyphs.size)
		val glyph = font[64]
		assertEquals(69, glyph.texture.width)
		assertEquals(70, glyph.texture.height)
		assertEquals(4, glyph.xoffset)
		assertEquals(4, glyph.yoffset)
		assertEquals(73, glyph.xadvance)

        renderableView(viewRenderer = ViewRenderer {
            font.drawText(ctx, 72.0 / 4.0, "ABF,", 0, 0)
            ctx.flush()
        })

        assertScreenshot()
	}

	@Test
	fun font2() = suspendTest {
		val font = resourcesVfs["font2/font1.fnt"].readBitmapFont()
		assertEquals(95, font.glyphs.size)
		val glyph = font[64]
		assertEquals(52, glyph.texture.width)
		assertEquals(52, glyph.texture.height)
		assertEquals(3, glyph.xoffset)
		assertEquals(8, glyph.yoffset)
		assertEquals(51, glyph.xadvance)
	}
}
