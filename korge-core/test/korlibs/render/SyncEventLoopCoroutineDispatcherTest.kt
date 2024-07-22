package korlibs.render

import korlibs.io.async.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.test.*

class SyncEventLoopCoroutineDispatcherTest {
    @Test
    fun test() {
        val dispatcher = SyncEventLoopCoroutineDispatcher(immediateRun = true)
        launchImmediately(dispatcher) {
            println("${DateTime.now()}: a")
            delay(1000.milliseconds)
            println("${DateTime.now()}: b")
        }
        dispatcher.loopUntilEmpty()
    }
}
