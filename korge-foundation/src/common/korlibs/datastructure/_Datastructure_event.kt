@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.event

import korlibs.datastructure.*
import korlibs.datastructure.closeable.*
import korlibs.datastructure.lock.*
import korlibs.datastructure.thread.*
import korlibs.time.*
import kotlin.time.*

interface EventLoop {
    fun setImmediate(task: () -> Unit)
    fun setTimeout(time: TimeSpan, task: () -> Unit): Closeable
    fun setInterval(time: TimeSpan, task: () -> Unit): Closeable
}

class SyncEventLoop(
    /** precise=true will have better precision at the cost of more CPU-usage (busy waiting) */
    var precise: Boolean = true
) : EventLoop {
    private val lock = NonRecursiveLock()
    private var running = true
    private class TimedTask(val eventLoop: SyncEventLoop, var now: Duration, val time: Duration, var interval: Boolean, val callback: () -> Unit) : Comparable<TimedTask>, Closeable {
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
    private val now: Duration get() = startTime.elapsedNow()
    private val tasks = ArrayDeque<() -> Unit>()
    private val timedTasks = TGenPriorityQueue<TimedTask> { a, b -> a.compareTo(b) }

    fun queueFirst(task: () -> Unit) {
        lock {
            tasks.addFirst(task)
            lock.notify()
        }
    }

    override fun setImmediate(task: () -> Unit) {
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
        return lock {
            val task = TimedTask(this, now, time, interval, task)
            timedTasks.add(task)
            //println("NOTIFIED!")
            lock.notify()
            task
        }
    }

    fun cancel() {
        running = false
    }

    fun runTasksForever() {
        running = true
        while (running) {
            runTasksUntilEmpty()
            NativeThread.gc(full = true)
            NativeThread.sleep(1.milliseconds)
        }
    }

    fun runTasksUntilEmpty() {
        //Thread.currentThread().priority = Thread.MAX_PRIORITY
        // Timed tasks
        while (running) {
            lock {
                if (tasks.isEmpty() && timedTasks.isNotEmpty()) {
                    val head = timedTasks.head
                    if ((head.timeMark - now) >= 16.milliseconds) {
                        //println("GC")
                        NativeThread.gc(full = false)
                    }
                    val waitTime = head.timeMark - now
                    if (waitTime >= 0.seconds) {
                        lock.wait(waitTime, precise)
                    }
                }
            }

            val timedTask = lock {
                if (timedTasks.isNotEmpty() && now >= timedTasks.head.timeMark) timedTasks.removeHead() else null
            }
            if (timedTask != null) {
                timedTask.callback()
                if (timedTask.interval) {
                    timedTask.timeMark = maxOf(timedTask.timeMark + timedTask.time, now)
                    //println("READDED: timedTask.now=${timedTask.now}")
                    timedTasks.add(timedTask)
                }
            }
            val task = lock {
                if (tasks.isNotEmpty()) tasks.removeFirst() else null
            }
            task?.invoke()

            if (lock { task == null && timedTask == null && tasks.isEmpty() && timedTasks.isEmpty() }) {
                break
            }
        }
    }
}
