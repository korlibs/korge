package com.soywiz.korge.view.vector

import com.soywiz.korge.annotations.*
import com.soywiz.korge.render.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import kotlin.test.*

@OptIn(KorgeExperimental::class)
class GpuShapeViewTest {
    @Test
    fun testShapeIsUpdated() {
        val view = GpuShapeView(EmptyShape)
        assertIs<EmptyShape>(view.shape)
        view.updateShape {
            stroke(createLinearGradient(0, 0, 0, 100).add(0.0, Colors.WHITE).add(1.0, Colors.RED), lineWidth = 10.0) {
                rect(0, 0, 100, 100)
            }
        }
        assertIs<PolylineShape>(view.shape)
    }

    @Test
    @Ignore
    fun testShapeWithoutApplyScissorDisableScissoring() {
        val view = GpuShapeView(EmptyShape)
        view.applyScissor = false
        view.updateShape { stroke(Colors.RED, lineWidth = 2.0) { rect(0, 0, 100, 100) } }
        val ag = testRenderContext {
            view.render(it)
        }
        assertTrue { ag.log.contains("disable: SCISSOR") }
    }
}
