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
    override fun filterLogDraw(str: String, kind: LogBaseAG.Kind): Boolean = kind == LogBaseAG.Kind.DRAW

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
    fun testFSprites() = viewsTest {
        val bmp = Bitmap32(32, 32, Colors.RED).slice()
        val sprites = FSprites(16)
        sprites.apply {
            run {
                alloc().also { sprite ->
                    sprite.xy(100f, 100f)
                    sprite.setAnchor(.5f, .5f)
                    sprite.scale(2f, 1f)
                    sprite.colorMul = Colors.WHITE.withAf(.5f)
                    sprite.setTex(bmp)
                }
                alloc().also { sprite ->
                    sprite.xy(200f, 100f)
                    sprite.setAnchor(.5f, 1f)
                    sprite.scale(1f, 2f)
                    sprite.colorMul = Colors.WHITE.withAf(.5f)
                    sprite.setTex(bmp)
                }
            }
        }
        val fview = FSprites.FView(sprites, bmp.bmpBase)
        addChild(fview)

        delayFrame()
        assertEqualsFileReference(
            "korge/render/FSprites.log",
            listOf(
                logAg.getLogAsString(),
            ).joinToString("\n")
        )
    }

}
