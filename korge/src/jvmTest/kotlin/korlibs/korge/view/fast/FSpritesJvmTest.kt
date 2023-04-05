package korlibs.korge.view.fast

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.testing.*
import korlibs.korge.tests.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.test.*

class FSpritesJvmTest : ViewsForTesting(log = true) {
    @Test
    fun test() = korgeScreenshotTest(SizeInt(512, 512)) {
        val sprites = FSprites(64)
        val view = sprites.createView(Bitmaps.white.bmp)
        addChild(view)
        sprites.apply {
            for (n in 0 until 10) {
                alloc().also {
                    it.xy(n * 20f + 1f, n * 20f + 2f)
                    it.scale(n + 3f, n + 4f)
                    it.setAnchor(.5f, .5f)
                    it.angle = (n * 30).degrees
                    it.setTex(Bitmaps.white)
                }
            }
        }
        assertScreenshot(this, includeBackground = true)
    }

    @Test
    fun testAlpha() = korgeScreenshotTest(SizeInt(20, 20), bgcolor = Colors.GREEN) {
        val bmp = Bitmap32(10, 10, Colors.RED).premultiplied()
        image(bmp).xy(0, 0)
        image(bmp) {
            xy(0, 10)
            alpha = .5f
        }

        val fSprites = FSprites(2)
        addChild(fSprites.createView(bmp))
        with(fSprites) {
            val left = fSprites.alloc()
            left.xy(10f, 0f)
            left.colorMul = Colors.WHITE
            left.setTexIndex(0)
            left.setTex(bmp.slice())

            val right = fSprites.alloc()
            right.xy(10f, 10f)
            right.colorMul = Colors.WHITE.withAf(.5f)
            right.setTexIndex(0)
            right.setTex(bmp.slice())
        }
        assertScreenshot(this, includeBackground = true)
    }
}
