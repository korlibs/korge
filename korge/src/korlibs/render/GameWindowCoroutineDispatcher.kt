package korlibs.render

import korlibs.datastructure.*
import korlibs.datastructure.closeable.*
import korlibs.datastructure.lock.*
import korlibs.io.experimental.*
import korlibs.logger.*
import korlibs.time.*
import korlibs.time.measureTime
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.time.*

@OptIn(InternalCoroutinesApi::class)
class GameWindowCoroutineDispatcher(
    var nowProvider: () -> Duration = { PerformanceCounter.reference },
    var fast: Boolean = false,
) : CoroutineDispatcher(), Delay, AutoCloseable {
    override fun dispatchYield(context: CoroutineContext, block: Runnable): Unit = dispatch(context, block)

    class TimedTask(val time: Duration, val continuation: CancellableContinuation<Unit>?, val callback: Runnable?) {
        var exception: Throwable? = null
    }

    @PublishedApi internal val tasks = Queue<Runnable>()
    @PublishedApi internal val timedTasks = TGenPriorityQueue<TimedTask> { a, b -> a.time.compareTo(b.time) }
    val lock = NonRecursiveLock()

    fun hasTasks() = tasks.isNotEmpty()

    fun queue(block: Runnable?) {
        if (block == null) return
        lock { tasks.enqueue(block) }
    }

    fun queue(block: () -> Unit) = queue(Runnable { block() })

    @KorioExperimentalApi
    fun <T> queueBlocking(block: () -> T): T {
        val result = CompletableDeferred<T>()
        queue(Runnable {
            result.complete(block())
        })
        return korlibs.io.async.runBlockingNoJs { result.await() }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) = queue(block) // @TODO: We are not using the context

    fun now(): Duration {
        return nowProvider()
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        scheduleResumeAfterDelay(timeMillis.toDouble().milliseconds, continuation)
    }

    fun scheduleResumeAfterDelay(time: Duration, continuation: CancellableContinuation<Unit>) {
        val task = TimedTask(now() + time, continuation, null)
        continuation.invokeOnCancellation {
            task.exception = it
        }
        lock { timedTasks.add(task) }
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val task = TimedTask(now() + timeMillis.toDouble().milliseconds, null, block)
        lock { timedTasks.add(task) }
        return DisposableHandle { lock { timedTasks.remove(task) } }
    }

    var timedTasksTime = 0.milliseconds
    var tasksTime = 0.milliseconds

    /**
     * Allows to configure how much time per frame is available to execute pending tasks,
     * despite time available in the frame.
     * When not set it uses the remaining available time in frame
     **/
    var maxAllocatedTimeForTasksPerFrame: Duration? = null

    fun executePending(availableTime: Duration) {
        try {
            val startTime = now()

            var processedTimedTasks = 0
            var processedTasks = 0

            timedTasksTime = measureTime {
                while (true) {
                    val item = lock {
                        if (timedTasks.isNotEmpty() && (fast || startTime >= timedTasks.head.time)) timedTasks.removeHead() else null
                    } ?: break
                    try {
                        if (item.exception != null) {
                            item.continuation?.resumeWithException(item.exception!!)
                            if (item.callback != null) {
                                item.exception?.printStackTrace()
                            }
                        } else {
                            item.continuation?.resume(Unit)
                            item.callback?.run()
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    } finally {
                        processedTimedTasks++
                    }
                    val elapsedTime = now() - startTime
                    if (elapsedTime >= availableTime) {
                        informTooManyCallbacksToHandleInThisFrame(elapsedTime, availableTime, processedTimedTasks, processedTasks)
                        break
                    }
                }
            }
            tasksTime = measureTime {
                while (true) {
                    val task = lock { (if (tasks.isNotEmpty()) tasks.dequeue() else null) } ?: break
                    val time = measureTime {
                        try {
                            task.run()
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        } finally {
                            processedTasks++
                        }
                    }
                    //println("task=$time, task=$task")
                    val elapsed = now() - startTime
                    if (elapsed >= availableTime) {
                        informTooManyCallbacksToHandleInThisFrame(elapsed, availableTime, processedTimedTasks, processedTasks)
                        break
                    }
                }
            }
        } catch (e: Throwable) {
            println("Error in GameWindowCoroutineDispatcher.executePending:")
            e.printStackTrace()
        }
    }

    val tooManyCallbacksLogger = Logger("Korgw.GameWindow.TooManyCallbacks")

    fun informTooManyCallbacksToHandleInThisFrame(elapsedTime: Duration, availableTime: Duration, processedTimedTasks: Int, processedTasks: Int) {
        tooManyCallbacksLogger.warn { "Too many callbacks to handle in this frame elapsedTime=${elapsedTime.roundMilliseconds()}, availableTime=${availableTime.roundMilliseconds()} pending timedTasks=${timedTasks.size}, tasks=${tasks.size}, processedTimedTasks=$processedTimedTasks, processedTasks=$processedTasks" }
    }

    override fun close() {
        executePending(2.seconds)
        logger.info { "GameWindowCoroutineDispatcher.close" }
        while (timedTasks.isNotEmpty()) {
            timedTasks.removeHead().continuation?.resume(Unit)
        }
        while (tasks.isNotEmpty()) {
            tasks.dequeue().run()
        }
    }

    val hasMore get() = timedTasks.isNotEmpty() || hasTasks()

    override fun toString(): String = "GameWindowCoroutineDispatcher(setNow=setNow, fast=$fast)"


    companion object {
        val logger = Logger("GameWindow")
    }
}
