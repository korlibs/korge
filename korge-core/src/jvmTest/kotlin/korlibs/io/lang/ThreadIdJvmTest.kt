package korlibs.io.lang

import korlibs.io.async.suspendTest
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ThreadIdJvmTest {
    @Test
    fun testDifferentThreadIdInDifferentThread() = suspendTest {
        val initialCurrentThreadId = currentThreadId
        val threadCurrentThreadIdDeferred = CompletableDeferred<Long>()
        Thread {
            threadCurrentThreadIdDeferred.complete(currentThreadId)
        }.also { it.start() }
        assertNotEquals(initialCurrentThreadId, threadCurrentThreadIdDeferred.await())
        val laterCurrentThreadId = currentThreadId
        assertEquals(initialCurrentThreadId, laterCurrentThreadId)
    }
}
