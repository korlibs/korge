package korlibs.render

import korlibs.datastructure.closeable.*
import korlibs.datastructure.event.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

@OptIn(InternalCoroutinesApi::class)
class SyncEventLoopCoroutineDispatcher(val eventLoop: SyncEventLoop) : CoroutineDispatcher(), Delay, Closeable {
    constructor(precise: Boolean = true, immediateRun: Boolean = false) : this(SyncEventLoop(precise, immediateRun))

    override fun close() {
        eventLoop.close()
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        eventLoop.setImmediate {
            block.run()
        }
        //TODO("Not yet implemented")
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        eventLoop.setTimeout(timeMillis.milliseconds) {
            continuation.resume(Unit)
        }
    }

    fun loopForever() {
        eventLoop.runTasksForever()
    }

    fun loopUntilEmpty() {
        eventLoop.runTasksUntilEmpty()
    }
}
