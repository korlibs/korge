package com.soywiz.korge.ui

import com.soywiz.korge.tests.*
import com.soywiz.korge.view.*
import kotlin.test.*

class UiLayoutTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        lateinit var rect1: View
        lateinit var rect2: View
        val stack = uiHorizontalStack(100.0) {
            rect1 = solidRect(50, 50)
            rect2 = solidRect(50, 50)
        }
        assertEquals(
            """
                Rectangle(x=0, y=0, width=100, height=100)
                Rectangle(x=0, y=0, width=50, height=100)
                Rectangle(x=50, y=0, width=50, height=100)
            """.trimIndent(),
            """
                ${stack.globalBounds}
                ${rect1.globalBounds}
                ${rect2.globalBounds}
            """.trimIndent()
        )

    }
}
