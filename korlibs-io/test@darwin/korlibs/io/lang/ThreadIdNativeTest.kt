package korlibs.io.lang

import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ThreadIdNativeTest {
    @Test
    fun testWorkerHaveDifferentThreadId() {
        val savedCurrentThreadId = currentThreadId
        val worker = Worker.start()
        val workerThreadId = worker.execute(TransferMode.SAFE, { Unit }, { currentThreadId })
        val workerCurrentThreadId = workerThreadId.result
        worker.requestTermination()
        assertEquals(savedCurrentThreadId, currentThreadId)
        assertNotEquals(workerCurrentThreadId, currentThreadId)
    }
}
