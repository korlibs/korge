@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.event

import korlibs.datastructure.*
import korlibs.datastructure.closeable.*
import korlibs.datastructure.lock.*
import korlibs.datastructure.thread.*
import korlibs.logger.*
import korlibs.time.*
import kotlin.time.*

interface EventLoop : Closeable {
    fun setImmediate(task: () -> Unit)
    fun setTimeout(time: TimeSpan, task: () -> Unit): Closeable
    fun setInterval(time: TimeSpan, task: () -> Unit): Closeable
}
fun EventLoop.setInterval(time: Frequency, task: () -> Unit): Closeable = setInterval(time.timeSpan, task)

abstract class BaseEventLoop : EventLoop

class SyncEventLoop(
    /** precise=true will have better precision at the cost of more CPU-usage (busy waiting) */
    //var precise: Boolean = true,
    var precise: Boolean = false,
    /** Execute timers immediately instead of waiting. Useful for testing. */
    var immediateRun: Boolean = false,
) : BaseEventLoop() {
    private val lock = NonRecursiveLock()
    private var running = true
    protected class TimedTask(val eventLoop: SyncEventLoop, var now: Duration, val time: Duration, var interval: Boolean, val callback: () -> Unit) : Comparable<TimedTask>, Closeable {
        var timeMark: Duration
            get() = now + time
            set(value) {
                now = value - time
            }
        override fun compareTo(other: TimedTask): Int = this.timeMark.compareTo(other.timeMark)
        override fun close() {
            //println("CLOSE")
            interval = false
            eventLoop.timedTasks.remove(this)
        }
    }
    private val startTime = TimeSource.Monotonic.markNow()

    var nowProvider: () -> Duration = { startTime.elapsedNow() }

    private val now: Duration get() = nowProvider()
    private val tasks = ArrayDeque<() -> Unit>()
    private val timedTasks = TGenPriorityQueue<TimedTask> { a, b -> a.compareTo(b) }

    fun setImmediateFirst(task: () -> Unit) {
        lock {
            tasks.addFirst(task)
            lock.notify()
        }
    }

    override fun setImmediate(task: () -> Unit) {
        //println("setImmediate: task=$task")
        lock {
            tasks.addLast(task)
            lock.notify()
        }
    }

    override fun setTimeout(time: TimeSpan, task: () -> Unit): Closeable {
        return _queueAfter(time, interval = false, task = task)
    }

    override fun setInterval(time: TimeSpan, task: () -> Unit): Closeable {
        return _queueAfter(time, interval = true, task = task)
    }

    private fun _queueAfter(time: TimeSpan, interval: Boolean, task: () -> Unit): Closeable {
        //println("_queueAfter: time=$time, interval=$interval, task=$task")
        return lock {
            val task = TimedTask(this, now, time, interval, task)
            if (running) {
                timedTasks.add(task)
            } else {
                Console.warn("WARNING: QUEUED TASK time=$time interval=$interval without running")
            }
            //println("NOTIFIED!")
            lock.notify()
            task
        }
    }

    override fun close() {
        val oldImmediateRun = immediateRun
        try {
            // Run pending tasks including pending timers, but won't allow to add new tasks because running=false
            immediateRun = true
            runAvailableNextTasks()
            running = false
        } finally {
            immediateRun = oldImmediateRun
        }
    }

    protected fun shouldTimedTaskRun(task: TimedTask): Boolean {
        if (immediateRun) return true
        return now >= timedTasks.head.timeMark
    }

    protected fun wait(waitTime: TimeSpan) {
        if (immediateRun) return
        lock.wait(waitTime, precise)
    }

    fun runAvailableNextTasks(runTimers: Boolean = true): Int {
        var count = 0
        while (runAvailableNextTask(runTimers)) {
            count++
        }
        return count
    }

    var uncatchedExceptionHandler: (Throwable) -> Unit = { it.printStackTrace() }

    private inline fun runCatchingExceptions(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            uncatchedExceptionHandler(e)
        }
    }

    fun runAvailableNextTask(maxCount: Int): Boolean {
        for (n in 0 until maxCount) {
            if (!runAvailableNextTask()) return false
        }
        return true
    }

    fun runAvailableNextTask(runTimers: Boolean = true): Boolean {
        val timedTask = lock {
            if (runTimers) if (timedTasks.isNotEmpty() && shouldTimedTaskRun(timedTasks.head)) timedTasks.removeHead() else null else null
        }
        if (timedTask != null) {
            runCatchingExceptions { timedTask.callback() }
            if (timedTask.interval && !immediateRun) {
                timedTask.timeMark = maxOf(timedTask.timeMark + timedTask.time, now)
                //println("READDED: timedTask.now=${timedTask.now}")
                timedTasks.add(timedTask)
            }
        }
        val task = lock {
            if (tasks.isNotEmpty()) tasks.removeFirst() else null
        }
        runCatchingExceptions {
            //println("RUN TASK $task")
            task?.invoke()
        }

        return task != null || timedTask != null
    }

    fun waitAndRunNextTask(): Boolean {
        //println("tasks=$tasks, timedTasks=$timedTasks")
        lock {
            if (tasks.isEmpty() && timedTasks.isNotEmpty()) {
                val head = timedTasks.head
                //if ((head.timeMark - now) >= 16.milliseconds) {
                //    //println("GC")
                //    //NativeThread.gc(full = false)
                //}
                val waitTime = head.timeMark - now
                if (waitTime >= 0.seconds) {
                    wait(waitTime)
                }
            }
        }

        return runAvailableNextTask()
    }

    fun runTasksUntilEmpty() {
        //Thread.currentThread().priority = Thread.MAX_PRIORITY
        // Timed tasks
        val stopwatch = Stopwatch().start()
        while (running) {
            val somethingExecuted = waitAndRunNextTask()

            if (lock { !somethingExecuted && tasks.isEmpty() && timedTasks.isEmpty() }) {
                break
            }

            if (stopwatch.elapsed >= 0.1.seconds) {
                stopwatch.restart()
                NativeThread.sleep(10.milliseconds)
            }
        }
    }

    fun runTasksForever() {
        running = true
        while (running) {
            runTasksUntilEmpty()
            //NativeThread.gc(full = true)
            NativeThread.sleep(1.milliseconds)
        }
    }
}
