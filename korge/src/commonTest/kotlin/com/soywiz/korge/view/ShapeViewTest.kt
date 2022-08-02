package com.soywiz.korge.view

import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.vector.line
import kotlin.test.Test
import kotlin.test.assertEquals

class ShapeViewTest {
    @Test
    fun testShapeViewBounds() {
        for (renderer in GraphicsRenderer.values()) {
            val view = ShapeView(strokeThickness = 1.0)
            view.renderer = renderer
            view.updatePath {
                clear()
                line(256.0, 256.0, 400.0, 256.0)
            }
            view.boundsIncludeStrokes = false
            assertEquals(Rectangle(256, 256, 144, 0), view.getLocalBounds(), message = "bounds without strokes $renderer")
            view.boundsIncludeStrokes = true
            assertEquals(Rectangle(255.5, 255.5, 145.0, 1.0), view.getLocalBounds(), message = "bounds with strokes $renderer")
        }
    }
}
