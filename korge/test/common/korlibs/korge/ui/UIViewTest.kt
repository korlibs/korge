package korlibs.korge.ui

import korlibs.io.util.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.number.*
import kotlin.test.*

class UIViewTest {
    @Test
    fun test() {
        var sizeChangedCount = 0
        val view = object : UIView(Size(100f, 110f)) {
            override fun onSizeChanged() {
                sizeChangedCount++
            }
        }
        val lines = arrayListOf<String>()
        fun log() {
            lines += "${view.width.niceStr}, ${view.height.niceStr}, $sizeChangedCount"
        }

        log()
        view.size(230.0, 240.0)
        log()

        assertEquals(
            """
                100, 110, 0
                230, 240, 1
            """.trimIndent(),
            lines.joinToString("\n")
        )
    }
}
