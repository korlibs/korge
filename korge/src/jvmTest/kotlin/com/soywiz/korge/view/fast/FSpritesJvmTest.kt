package com.soywiz.korge.view.fast

import com.soywiz.korge.testing.*
import com.soywiz.korge.tests.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class FSpritesJvmTest : ViewsForTesting(log = true) {
    @Test
    fun test() = korgeScreenshotTest(512, 512) {
        val sprites = FSprites(64)
        val view = sprites.createView(Bitmaps.white.bmpBase)
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
}
