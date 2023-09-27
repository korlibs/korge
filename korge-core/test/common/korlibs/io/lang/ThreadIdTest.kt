package korlibs.io.lang

import kotlin.test.Test
import kotlin.test.assertEquals

class ThreadIdTest {
    @Test
    fun testCurrentThreadIdReturnsAlwaysTheSameValueOnTheSameThread() {
        assertEquals(currentThreadId, currentThreadId)
    }
}
