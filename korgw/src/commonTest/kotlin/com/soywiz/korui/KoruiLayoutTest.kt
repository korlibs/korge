package com.soywiz.korui

import com.soywiz.korui.layout.*
import com.soywiz.korui.native.*
import kotlin.test.*

class KoruiLayoutTest {
    @Test
    fun test() {
        lateinit var button0: UiButton
        lateinit var button1: UiButton
        lateinit var button2: UiButton
        lateinit var horizontal: UiContainer
        val app = UiApplication(DummyUiFactory)
        app.window(300, 300) {
            button0 = button("hello")
            horizontal = horizontal {
                button1 = button("hello") {
                    preferredWidth = 50.percent
                }
                button2 = button("world") {
                    preferredWidth = 50.percent
                    preferredHeight = 32.pt
                }
            }
        }
        assertEquals("Rectangle(x=0, y=0, width=300, height=32)", "${button0.bounds}")
        assertEquals("Rectangle(x=0, y=0, width=150, height=32), Rectangle(x=150, y=0, width=150, height=32)", "${button1.bounds}, ${button2.bounds}")
        assertEquals("Rectangle(x=0, y=32, width=300, height=32)", "${horizontal.bounds}")
    }
}
