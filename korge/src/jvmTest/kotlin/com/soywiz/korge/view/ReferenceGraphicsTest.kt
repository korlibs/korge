package com.soywiz.korge.view

import com.soywiz.korag.log.LogBaseAG
import com.soywiz.korge.test.assertEqualsFileReference
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.fast.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.SizeInt
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.vector.rect
import kotlin.test.Test

class ReferenceGraphicsTest : ViewsForTesting(
    windowSize = SizeInt(200, 200),
    virtualSize = SizeInt(100, 100),
    log = true,
) {
    override fun filterLogDraw(str: String, kind: LogBaseAG.Kind): Boolean = kind == LogBaseAG.Kind.DRAW || kind == LogBaseAG.Kind.SHADER

    @Test
    fun test() = viewsTest {
        graphics {
            fill(Colors.RED) {
                rect(-60, -60, 70, 70)
            }
        }

        // Circle Graphics
        circle(64.0, fill = Colors.RED, stroke = Colors.BLUE, strokeThickness = 32.0).xy(50, 50).centered.rotation(30.degrees)

        val bmp = BitmapSlice(
            Bitmap32(64, 64) { x, y -> Colors.PURPLE },
            RectangleInt(0, 0, 64, 64),
            virtFrame = RectangleInt(64, 64, 196, 196)
        )
        val image = image(bmp).anchor(0.5, 1.0).xy(200, 200).rotation(30.degrees)

        delayFrame()
        assertEqualsFileReference(
            "korge/render/Graphics.log",
            listOf(
                logAg.getLogAsString(),
                image.getGlobalBounds().toString(),
                image.getLocalBounds().toString()
            ).joinToString("\n")
        )
    }

    @Test
    fun testFSprites1() = testFSpritesN(1)

    @Test
    fun testFSprites2() = testFSpritesN(2)

    @Test
    fun testFSprites3() = testFSpritesN(3)

    @Test
    fun testFSprites4() = testFSpritesN(4)

    fun testFSpritesN(N: Int) = viewsTest {
        val bmp = Bitmap32(32, 32, Colors.RED).slice()
        val sprites = FSprites(16)
        val anchorsX = listOf(.5f, .5f, .5f, .0f)
        val anchorsY = listOf(.5f, 1f, .0f, 1f)
        sprites.apply {
            run {
                for (n in 0 until 4) {
                    alloc().also { sprite ->
                        sprite.xy(100f + 100f * n, 100f)
                        sprite.setAnchor(anchorsX[n], anchorsY[n])
                        sprite.scale(2f, 1f)
                        sprite.colorMul = Colors.WHITE.withAf(.5f)
                        sprite.setTex(bmp)
                        sprite.setTexIndex(n % N)
                    }
                }
            }
        }
        val fview = FSprites.FView(sprites, Array(N) { bmp.bmpBase })
        addChild(fview)

        delayFrame()
        "korge/render/FSprites.log"
        "korge/render/FSprites1.log"
        "korge/render/FSprites2.log"
        "korge/render/FSprites3.log"
        "korge/render/FSprites4.log"
        assertEqualsFileReference(
            "korge/render/FSprites$N.log",
            listOf(
                logAg.getLogAsString(),
            ).joinToString("\n")
        )
    }

}
