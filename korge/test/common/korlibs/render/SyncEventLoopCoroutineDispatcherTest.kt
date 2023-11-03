package korlibs.render

import korlibs.datastructure.event.*
import korlibs.io.async.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.test.*

class SyncEventLoopCoroutineDispatcherTest {
    @Test
    fun test() {
        val eventLoop = SyncEventLoop(precise = true, immediateRun = true)
        val dispatcher = EventLoopCoroutineDispatcher(eventLoop)
        launchImmediately(dispatcher) {
            println("${DateTime.now()}: a")
            delay(1000.milliseconds)
            println("${DateTime.now()}: b")
        }
        eventLoop.runTasksUntilEmpty()
    }
}
