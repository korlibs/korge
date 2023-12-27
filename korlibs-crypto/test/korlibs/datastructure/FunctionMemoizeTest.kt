package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class FunctionMemoizeTest {
    @Test
    fun test() {
        var called = 0
        val func = { called++; 10 }
        assertEquals(10, func())
        assertEquals(10, func())
        assertEquals(2, called)
        val funcMemo = func.memoize()
        for (n in 0 until 10) assertEquals(10, funcMemo())
        assertEquals(3, called)
    }
}
