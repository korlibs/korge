package com.soywiz.korge.ui

import com.soywiz.korev.Key
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.tests.ViewsForTesting
import kotlin.test.Test
import kotlin.test.assertEquals

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
