package com.soywiz.korge.view

import com.soywiz.korag.log.LogBaseAG
import com.soywiz.korge.test.assertEqualsFileReference
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korim.color.Colors
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
        circle(64.0, fill = Colors.RED, stroke = Colors.BLUE, strokeThickness = 32.0).xy(50, 50).centered.rotation(30.degrees)
        delayFrame()
        assertEqualsFileReference("korge/render/Graphics.log", logAg.getLogAsString())
    }
}
