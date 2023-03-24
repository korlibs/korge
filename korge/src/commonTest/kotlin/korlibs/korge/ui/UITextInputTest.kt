package korlibs.korge.ui

import korlibs.event.*
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.tests.*
import korlibs.korge.view.*
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
        //val textInput = uiTextInput()
        //assertEquals(0, textInput.selectionStart)
        //textInput.focus()
        //keyType("aa")
        //assertEquals("aa", textInput.text)
        //keyDownThenUp(Key.BACKSPACE)
        //assertEquals("a", textInput.text)
        //keyType("b")
        //assertEquals("ab", textInput.text)
        assertEquals(
            """
                <START>
                STATE: '':0<..0
                TYPED<hello>
                STATE: 'ac':2<..2
                <BACKSPACE>
                STATE: 'a':1<..1
                TYPED<hello>
                STATE: 'ab':2<..2
            """.trimIndent(),
            inputTester {
                type("ac")
                down(Key.BACKSPACE)
                type("b")
            }
        )
    }

    @Test
    fun testBackspace3() = viewsTest {
        assertEquals(
            """
                <START>
                STATE: '':0<..0
                TYPED<hello>
                STATE: 'hello':5<..5
                <LEFT>
                STATE: 'hello':4<..4
                <BACKSPACE>
                STATE: 'helo':3<..3
                <BACKSPACE>
                STATE: 'heo':2<..2
            """.trimIndent(),
            inputTester {
                type("hello")
                down(Key.LEFT)
                down(Key.BACKSPACE)
                down(Key.BACKSPACE)
            }
        )
    }

    inline fun Stage.inputTester(block: TextInputTester.() -> Unit): String = TextInputTester(this).apply {
        log("<START>")
        focus()
        block()
    }.str

    inner class TextInputTester(val stage: Stage) {
        val log = arrayListOf<String>()
        val textInput = stage.uiTextInput()
        fun log(action: String) {
            log += action
            log += "STATE: '${textInput.text}':${textInput.selectionStart}<..${textInput.selectionEnd}"
        }
        val str: String get() = log.joinToString("\n")
        fun focus() {
            textInput.focus()
        }
        suspend fun type(str: String) {
            keyType(str)
            log("TYPED<hello>")
        }
        suspend fun down(key: Key) {
            keyDown(key)
            log("<$key>")
        }
    }
}