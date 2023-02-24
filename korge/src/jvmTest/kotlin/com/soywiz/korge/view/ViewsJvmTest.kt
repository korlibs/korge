package com.soywiz.korge.view

import com.soywiz.korge.testing.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korim.text.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class ViewsJvmTest : ViewsForTesting(log = true) {
	val tex = Bitmap32(10, 10, Colors.GREEN.premultiplied)

	@Test
	fun name() = korgeScreenshotTest(20, 20) {
		this += Container().apply {
			this += Image(tex)
		}
		assertEquals(
			"""
				Stage
				 Container
				  Image:bitmap=RectSlice(null:Rectangle(x=0, y=0, width=10, height=10))
			""".trimIndent(),
			views.stage.dumpToString()
		)
        assertScreenshot()
	}

	@Test
	fun textGetBounds1() = viewsTest {
		val font = views.debugBmpFont
		assertEquals(MRectangle(0, 0, 77, 8), TextOld("Hello World", font = font, textSize = 8.0).globalBounds)
	}

    @Test
    fun textGetBounds2() = viewsTest {
        val font = views.debugBmpFont
        assertEquals(MRectangle(0, 0, 154, 16), TextOld("Hello World", font = font, textSize = 16.0).globalBounds)
    }

    @Test
    fun testFilter() = korgeScreenshotTest(20, 20) {
        this += Container().apply {
            this += Image(tex).also {
                it.addFilter(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
                it.addFilter(Convolute3Filter(Convolute3Filter.KERNEL_EDGE_DETECTION))
            }.xy(4, 4)
        }
        assertEquals(
            """
            Stage
             Container
              Image:pos=(4,4):bitmap=RectSlice(null:Rectangle(x=0, y=0, width=10, height=10))
            """.trimIndent(),
            views.stage.dumpToString()
        )
        assertScreenshot()
    }

    @Test
    fun testTextBounds() = viewsTest {
        val font = resourcesVfs["Pacifico.ttf"].readFont()
        run {
            val text = text("WTF is going on", 64.0)
            assertEquals(MRectangleInt(2, 0, 416, 73), text.globalBounds.toInt())
            //assertEquals(RectangleInt(0, 0, 420, 71), text.globalBounds.toInt())
            //assertEquals(RectangleInt(0, 15, 417, 56), text.globalBounds.toInt())
        }

        run {
            val realTextSize = 64.0
            //val text = "WTF is going on\nWTF is going on"
            val text = "WTF is going on"
            val renderer = DefaultStringTextRenderer
            val useNativeRendering = true
            val textResult = font.renderTextToBitmap(
                realTextSize, text,
                paint = Colors.WHITE, fill = true, renderer = renderer,
                //background = Colors.RED,
                nativeRendering = useNativeRendering,
                drawBorder = true
            )
            //textResult.bmp.showImageAndWait()
            assertEquals(MSize(453, 121), textResult.bmp.size)
            //assertEquals(Size(450, 240), textResult.bmp.size)
        }
    }

    @Test
    fun testRunBlockingNoJs() = viewsTest {
        val bitmap = runBlockingNoJs {
            resourcesVfs["texture.png"].readBitmap()
        }
        assertEquals(MSize(64, 64), bitmap.size)
    }
}
