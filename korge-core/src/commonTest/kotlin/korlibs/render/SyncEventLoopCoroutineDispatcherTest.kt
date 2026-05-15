package korlibs.render

import korlibs.concurrent.lock.Lock
import korlibs.concurrent.lock.isSupported
import korlibs.io.async.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.test.*

class SyncEventLoopCoroutineDispatcherTest {
    @Test
    fun test() {
        if (!Lock.isSupported) {
            println("Lock is not supported, test will be skipped.")
            return
        }

        val dispatcher = SyncEventLoopCoroutineDispatcher(immediateRun = true)
        launchImmediately(dispatcher) {
            println("${DateTime.now()}: a")
            delay(1000.milliseconds)
            println("${DateTime.now()}: b")
        }
        dispatcher.loopUntilEmpty()
    }
}
