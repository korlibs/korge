package korlibs.io.async

import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.test.*

class AsyncExtTest {
    @Test
    fun test() = suspendTest {
        delay(0.05.seconds)
        assertEquals(1, 1)
    }
}
