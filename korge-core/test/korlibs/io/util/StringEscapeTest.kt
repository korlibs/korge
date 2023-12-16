package korlibs.io.util

import kotlin.test.Test
import kotlin.test.assertEquals

class StringEscapeTest {
    @Test
    fun test() {
        assertEquals("\"hello\\nworld!\"", "hello\nworld!".quote())
        assertEquals("\\x1e", "\u001e".escape())
        assertEquals("\\u001e", "\u001e".escapeUnicode())
        assertEquals("\\uabcd", "\uabcd".escapeUnicode())
        assertEquals("\uabcd", "\\uabcd".unescape())
        assertEquals("\u00ab\u00cd", "\\xab\\xcd".unescape())
    }
}
