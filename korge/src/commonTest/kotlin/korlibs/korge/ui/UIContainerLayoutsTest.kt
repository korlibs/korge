package korlibs.korge.ui

import korlibs.image.color.*
import korlibs.korge.view.*
import kotlin.test.*

class UIContainerLayoutsTest {
    @Test
    fun test() {
        val container = Container()
        lateinit var hs1: UIHorizontalStack
        lateinit var hs2: UIHorizontalStack
        val vs = container.uiVerticalStack {
            hs1 = uiHorizontalStack {
                solidRect(100, 100, Colors.BLUE)
                solidRect(100, 120, Colors.RED)
                solidRect(100, 100, Colors.GREEN)
                solidRect(100, 100, Colors.YELLOW)
            }
            hs2 = uiHorizontalStack {
                solidRect(100, 100, Colors.YELLOW)
                solidRect(100, 100, Colors.GREEN)
                solidRect(100, 100, Colors.RED)
                solidRect(100, 100, Colors.BLUE)
            }
        }

        assertEquals(
            """
                Rectangle(x=0, y=0, width=400, height=220)
                Rectangle(x=0, y=0, width=400, height=120)
                Rectangle(x=0, y=120, width=400, height=100)
            """.trimIndent(),
            """
                ${vs.getBounds(container)}
                ${hs1.getBounds(container)}
                ${hs2.getBounds(container)}
            """.trimIndent()
        )

        hs1.forcedHeight = 32f

        assertEquals(
            """
                Rectangle(x=0, y=0, width=400, height=132)
                Rectangle(x=0, y=0, width=400, height=32)
                Rectangle(x=0, y=32, width=400, height=100)
            """.trimIndent(),
            """
                ${vs.getBounds(container)}
                ${hs1.getBounds(container)}
                ${hs2.getBounds(container)}
            """.trimIndent()
        )
    }
}
