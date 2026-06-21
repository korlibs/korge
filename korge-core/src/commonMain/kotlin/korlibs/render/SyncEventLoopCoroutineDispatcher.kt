package korlibs.render

import korlibs.datastructure.event.*
import korlibs.io.lang.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

@OptIn(InternalCoroutinesApi::class)
class SyncEventLoopCoroutineDispatcher(val eventLoop: SyncEventLoop) : CoroutineDispatcher(), Delay, AutoCloseable {
    constructor(immediateRun: Boolean = false) : this(SyncEventLoop(immediateRun))

    override fun close() {
        eventLoop.close()
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        eventLoop.setImmediate {
            block.run()
        }
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        var cancelled: Throwable? = null
        continuation.invokeOnCancellation {
            cancelled = it
        }
        eventLoop.setTimeout(timeMillis.milliseconds) {
            if (cancelled != null) {
                continuation.resumeWithException(cancelled!!)
            } else {
                continuation.resume(Unit)
            }
        }
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        return eventLoop.setTimeout(timeMillis.milliseconds) {
            block.run()
        }.toDisposable()
    }

    fun loopForever() {
        eventLoop.runTasksForever()
    }

    fun loopUntilEmpty() {
        eventLoop.runTasksUntilEmpty()
    }

    fun executePending() {
        eventLoop.runAvailableNextTasks()
    }
}
