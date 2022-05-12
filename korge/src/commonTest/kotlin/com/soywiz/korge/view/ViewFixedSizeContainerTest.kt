package com.soywiz.korge.view

import com.soywiz.korag.AG
import com.soywiz.korag.log.*
import com.soywiz.korge.render.*
import com.soywiz.korge.tests.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.test.*

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
