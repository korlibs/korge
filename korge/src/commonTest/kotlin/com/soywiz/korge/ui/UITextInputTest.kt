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

    @Test
    fun testBackspace2() = viewsTest {
        val textInput = uiTextInput()
        assertEquals(0, textInput.selectionStart)
        textInput.focus()
        keyType("aa")
        assertEquals("aa", textInput.text)
        keyDownThenUp(Key.BACKSPACE)
        assertEquals("a", textInput.text)
        keyType("b")
        assertEquals("ab", textInput.text)
    }
}
