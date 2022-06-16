package com.soywiz.korge.ui

import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.View
import com.soywiz.korge.view.solidRect
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun testGrid() = viewsTest {
        val container = uiGridFill(300.0, 200.0, cols = 3, rows = 2) {
            for (n in 0 until 5) solidRect(1, 1) { name = "rect$n" }
        }
        assertEquals(
            """
                Rectangle(x=0, y=0, width=300, height=200): null
                Rectangle(x=0, y=0, width=100, height=100): rect0
                Rectangle(x=100, y=0, width=100, height=100): rect1
                Rectangle(x=200, y=0, width=100, height=100): rect2
                Rectangle(x=0, y=100, width=100, height=100): rect3
                Rectangle(x=100, y=100, width=100, height=100): rect4
            """.trimIndent(),
            (listOf(container) + container.children.toList()).joinToString("\n") { "${it.globalBounds}: ${it.name}" }
        )
    }
}
