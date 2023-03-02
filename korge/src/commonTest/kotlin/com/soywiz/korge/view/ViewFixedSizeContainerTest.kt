package com.soywiz.korge.view

import com.soywiz.korag.*
import com.soywiz.korag.log.AGBaseLog
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.*
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
        testRenderContext(object : AGBaseLog() {
            override fun execute(command: AGCommand) {
                super.execute(command)
                when (command) {
                    is AGBatch -> {
                        log += command.scissor.toRectOrNull()
                    }
                    else -> Unit
                }
            }
        }) {
            stage.render(it)
        }

        assertEquals(listOf<Any?>(MRectangle(234, 105, 150, 150)), log)
    }
}
