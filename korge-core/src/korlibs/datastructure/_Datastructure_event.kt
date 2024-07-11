@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.event

import korlibs.concurrent.lock.*
import korlibs.concurrent.thread.*
import korlibs.datastructure.*
import korlibs.datastructure.lock.*
import korlibs.datastructure.lock.Lock
import korlibs.datastructure.pauseable.*
import korlibs.datastructure.thread.*
import korlibs.datastructure.thread.NativeThread
import korlibs.datastructure.thread.nativeThread
import korlibs.logger.*
import korlibs.time.*
import kotlinx.atomicfu.locks.*
import kotlin.time.*

expect fun createPlatformEventLoop(precise: Boolean = true): SyncEventLoop

interface EventLoop : Pauseable, AutoCloseable {
    companion object

    fun setImmediate(task: () -> Unit)
    fun setTimeout(time: Duration, task: () -> Unit): AutoCloseable
    fun setInterval(time: Duration, task: () -> Unit): AutoCloseable
    fun setIntervalFrame(task: () -> Unit): AutoCloseable = setInterval(60.hz.timeSpan, task)
}

fun EventLoop.setInterval(time: Frequency, task: () -> Unit): AutoCloseable = setInterval(time.timeSpan, task)

abstract class BaseEventLoop : EventLoop, Pauseable {
    val runLock = Lock()
}

open class SyncEventLoop(
    /** precise=true will have better precision at the cost of more CPU-usage (busy waiting) */
    //var precise: Boolean = true,
    var precise: Boolean = false,
    /** Execute timers immediately instead of waiting. Useful for testing. */
    var immediateRun: Boolean = false,
) : BaseEventLoop(), Pauseable {
    private val pauseable = SyncPauseable()
    override var paused: Boolean by pauseable::paused
    private val lock = korlibs.concurrent.lock.Lock()
    private var running = true

    protected class TimedTask(val eventLoop: SyncEventLoop, var now: Duration, val time: Duration, var interval: Boolean, val callback: () -> Unit) :
        Comparable<TimedTask>, AutoCloseable {
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

    override fun setTimeout(time: Duration, task: () -> Unit): AutoCloseable {
        return _queueAfter(time, interval = false, task = task)
    }

    override fun setInterval(time: Duration, task: () -> Unit): AutoCloseable {
        return _queueAfter(time, interval = true, task = task)
    }

    private fun _queueAfter(time: Duration, interval: Boolean, task: () -> Unit): AutoCloseable {
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

    protected fun wait(waitTime: Duration) {
        if (immediateRun) return
        lock { lock.wait(waitTime, precise) }
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
            //runLock {
            run {
                block()
            }
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
            pauseable.checkPaused()
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

    fun runTasksForever(runWhile: () -> Boolean = { true }) {
        running = true
        while (running && runWhile()) {
            runTasksUntilEmpty()
            NativeThread.sleep(1.milliseconds)
        }
    }

    // START

    private var thread: NativeThread? = null
    open fun start(): Unit {
        if (thread != null) return
        thread = nativeThread {
            runTasksForever { thread?.threadSuggestRunning == true }
        }
    }

    open fun stop(): Unit {
        thread?.threadSuggestRunning = false
        thread = null
    }
}
