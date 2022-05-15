package com.soywiz.korge.view

import com.soywiz.korag.log.LogBaseAG
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.SizeInt
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewFixedSizeContainerTest : ViewsForTesting(
    windowSize = SizeInt(1280, 720),
    virtualSize = SizeInt(640, 480)
) {
    @Test
    fun testClipContainerScissors() = viewsTest {
        clipContainer(100, 100) {
            xy(50, 70)
            solidRect(20, 20, Colors.RED).xy(-10, -10)
        }
        delayFrame()
        val log = arrayListOf<Any?>()
        testRenderContext(object : LogBaseAG() {
            override fun draw(batch: Batch) {
                log += batch.scissor?.rect
            }
        }) {
            stage.render(it)
        }

        assertEquals(listOf<Any?>(Rectangle(234, 105, 150, 150)), log)
    }
}
