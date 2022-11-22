package com.soywiz.korge.view

import com.soywiz.korim.font.DefaultTtfFont
import kotlin.test.Test
import kotlin.test.assertEquals

class TextTest {
    @Test
    fun testTextBounds() {
        val text = Text("", font = DefaultTtfFont)
        assertEquals(
            "Rectangle([0,0]-[0,18.4])",
            text.getLocalBoundsOptimized().roundDecimalPlaces(1).toStringBounds()
        )
        text.text = "hello"
        assertEquals(
            "Rectangle([-0.8,0]-[28.9,18.4])",
            text.getLocalBoundsOptimized().roundDecimalPlaces(1).toStringBounds()
        )
    }
}
