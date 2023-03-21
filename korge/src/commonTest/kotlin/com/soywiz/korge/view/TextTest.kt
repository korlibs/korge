package com.soywiz.korge.view

import assertEqualsFloat
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korma.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TextTest {
    @Test
    fun testTextBounds() {
        val text = Text("", font = DefaultTtfFont)
        assertEqualsFloat(
            Rectangle.fromBounds(Point(0, 0), Point(0, 18.4)),
            text.getLocalBoundsOptimized().roundDecimalPlaces(1)
        )
        text.text = "hello"
        assertEqualsFloat(
            Rectangle.fromBounds(Point(-0.8, 0), Point(28.9, 18.4)),
            text.getLocalBoundsOptimized().roundDecimalPlaces(1)
        )
    }
}
