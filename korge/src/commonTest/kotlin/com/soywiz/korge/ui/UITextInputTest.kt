package com.soywiz.korge.ui

import com.soywiz.korev.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.tests.*
import kotlin.test.*

@OptIn(KorgeExperimental::class)
class UITextInputTest : ViewsForTesting() {
    @Test
    fun testBackspace() = viewsTest {
        val textInput = uiTextInput()
        assertEquals(0, textInput.selectionStart)
        textInput.focus()
        keyType("hello")
        assertEquals(5, textInput.selectionStart)
        keyDownThenUp(Key.BACKSPACE)
        assertEquals(4, textInput.selectionStart)
    }
}
