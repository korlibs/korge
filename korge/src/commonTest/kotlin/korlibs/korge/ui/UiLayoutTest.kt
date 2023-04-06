package korlibs.korge.ui

import korlibs.korge.tests.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.test.*

class UiLayoutTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        lateinit var rect1: View
        lateinit var rect2: View
        val stack = uiHorizontalStack(100f) {
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
        val container = uiGridFill(Size(300f, 200f), cols = 3, rows = 2) {
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
