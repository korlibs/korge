package korlibs.korge.view

import korlibs.korge.testing.*
import korlibs.korge.tests.*
import korlibs.korge.view.filter.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.format.*
import korlibs.image.text.*
import korlibs.io.file.std.*
import korlibs.math.geom.*
import kotlin.test.*

class ViewsJvmTest : ViewsForTesting(log = true) {
	val tex = Bitmap32(10, 10, Colors.GREEN.premultiplied)

	@Test
	fun name() = korgeScreenshotTest(SizeInt(20, 20)) {
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
    fun testFilter() = korgeScreenshotTest(SizeInt(20, 20)) {
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
            val text = text("WTF is going on", 64.0, font = DefaultTtfFont.lazyBitmap)
            assertEquals(RectangleInt(2, 0, 416, 73), text.globalBounds.toInt())
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
            assertEquals(SizeInt(453, 121), textResult.bmp.size)
            //assertEquals(Size(450, 240), textResult.bmp.size)
        }
    }

    @Test
    fun testRunBlockingNoJs() = viewsTest {
        val bitmap = runBlockingNoJs {
            resourcesVfs["texture.png"].readBitmap()
        }
        assertEquals(SizeInt(64, 64), bitmap.size)
    }
}
