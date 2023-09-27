package korlibs.korge.ui

import korlibs.image.color.*
import korlibs.korge.view.*
import korlibs.math.geom.*
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

        hs1.forcedHeight = 32.0

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

    @Test
    fun testUIGridFill() {
        fun str(cols: Int = 3, rows: Int = 2, spacing: Spacing = Spacing.ZERO, direction: UIDirection = UIDirection.ROW): String {
            val grid = UIGridFill(cols = cols, rows = rows, spacing = spacing, direction = direction)
            val items = (0 until 6).map { SolidRect(Size(100, 100)).addTo(grid) }
            return "- COLS: $cols, ROWS: $rows, padding: $spacing, direction: $direction\n" + items.map { it.getBounds(grid).toIntRound() }.map { "(${it.x}, ${it.y})-(${it.width}, ${it.height})" }.joinToString("\n")
        }

        assertEquals(
            """
                - COLS: 3, ROWS: 2, padding: Spacing(vertical=0, horizontal=0), direction: ROW
                (0, 0)-(43, 64)
                (43, 0)-(43, 64)
                (85, 0)-(43, 64)
                (0, 64)-(43, 64)
                (43, 64)-(43, 64)
                (85, 64)-(43, 64)
                - COLS: 3, ROWS: 2, padding: Spacing(vertical=0, horizontal=0), direction: ROW_REVERSE
                (85, 0)-(43, 64)
                (43, 0)-(43, 64)
                (0, 0)-(43, 64)
                (85, 64)-(43, 64)
                (43, 64)-(43, 64)
                (0, 64)-(43, 64)
                - COLS: 3, ROWS: 2, padding: Spacing(vertical=0, horizontal=0), direction: COLUMN
                (0, 0)-(43, 64)
                (0, 64)-(43, 64)
                (43, 0)-(43, 64)
                (43, 64)-(43, 64)
                (85, 0)-(43, 64)
                (85, 64)-(43, 64)
                - COLS: 3, ROWS: 2, padding: Spacing(vertical=0, horizontal=0), direction: COLUMN_REVERSE
                (0, 64)-(43, 64)
                (0, 0)-(43, 64)
                (43, 64)-(43, 64)
                (43, 0)-(43, 64)
                (85, 64)-(43, 64)
                (85, 0)-(43, 64)
                - COLS: 3, ROWS: 2, padding: Spacing(vertical=4, horizontal=8), direction: ROW
                (0, 0)-(37, 62)
                (45, 0)-(37, 62)
                (91, 0)-(37, 62)
                (0, 66)-(37, 62)
                (45, 66)-(37, 62)
                (91, 66)-(37, 62)
            """.trimIndent(),
            listOf(
                str(cols = 3, rows = 2, spacing = Spacing.ZERO, direction = UIDirection.ROW),
                str(cols = 3, rows = 2, spacing = Spacing.ZERO, direction = UIDirection.ROW_REVERSE),
                str(cols = 3, rows = 2, spacing = Spacing.ZERO, direction = UIDirection.COLUMN),
                str(cols = 3, rows = 2, spacing = Spacing.ZERO, direction = UIDirection.COLUMN_REVERSE),

                str(cols = 3, rows = 2, spacing = Spacing(vertical = 4f, horizontal = 8f), direction = UIDirection.ROW),
            ).joinToString("\n")
        )
    }
}
