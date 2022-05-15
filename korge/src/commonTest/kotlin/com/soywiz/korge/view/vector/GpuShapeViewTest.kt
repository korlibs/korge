package com.soywiz.korge.view.vector

import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korim.color.Colors
import com.soywiz.korim.vector.EmptyShape
import com.soywiz.korim.vector.PolylineShape
import com.soywiz.korma.geom.vector.rect
import kotlin.test.Test
import kotlin.test.assertIs

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
}
