package korlibs.io.lang

import korlibs.memory.*
import korlibs.io.*
import korlibs.platform.*
import kotlin.test.*

class JsEvalTest {
    @Test
    fun test() {
        assertEquals(true, Platform.isJs)
        assertEquals(true, JSEval.available)
        assertEquals(32, JSEval("return a ** b;", "a" to 2, "b" to 5))
        assertEquals(32, JSEval.expr("a ** b", "a" to 2, "b" to 5))
        assertEquals("world2", JSEval.expr("hello + 2", "hello" to "world"))
        assertEquals(jsGlobal, JSEval.globalThis)
    }
}
