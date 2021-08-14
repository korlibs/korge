package com.soywiz.korge.view

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
        val log = arrayListOf<String>()
        testRenderContext(object : LogBaseAG() {
            override fun draw(batch: Batch) {
                log += batch.scissor.toString()
            }
        }) {
            stage.render(it)
        }
        assertEquals("Scissor(x=235, y=105, width=150, height=150)", log.joinToString(","))
    }
}
