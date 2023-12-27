package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryStackTest {
    @Test
    fun test() {
        val stack = HistoryStack<String>()
        stack.push("a")
        assertEquals(null, stack.undo())
        assertEquals("a", stack.redo())

        stack.push("b")
        stack.push("c")
        stack.push("d")
        assertEquals("c", stack.undo())
        assertEquals("b", stack.undo())
        assertEquals("a", stack.undo())
        assertEquals(null, stack.undo())
        assertEquals("a", stack.redo())
        assertEquals("b", stack.redo())
        stack.push("C")
        assertEquals(null, stack.redo())
        assertEquals("C", stack.undo())
        assertEquals("b", stack.undo())
        assertEquals("a", stack.undo())
        assertEquals(null, stack.undo())
    }
}
