package com.soywiz.korge.ui

import com.soywiz.korio.util.*
import kotlin.test.*

class UIViewTest {
    @Test
    fun test() {
        var sizeChangedCount = 0
        val view = object : UIView(100.0, 110.0) {
            override fun onSizeChanged() {
                sizeChangedCount++
            }
        }
        val lines = arrayListOf<String>()
        fun log() {
            lines += "${view.width.niceStr}, ${view.height.niceStr}, $sizeChangedCount"
        }

        log()
        view.setSize(230.0, 240.0)
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
