package korlibs.io.lang

import korlibs.datastructure.thread.*
import korlibs.time.*

@Deprecated("", ReplaceWith("NativeThread.currentThreadId", "korlibs.datastructure.thread.NativeThread"))
val currentThreadId: Long get() = NativeThread.currentThreadId
@Deprecated("", ReplaceWith("NativeThread.currentThreadName", "korlibs.datastructure.thread.NativeThread"))
val currentThreadName: String? get() = NativeThread.currentThreadName
@Deprecated("", ReplaceWith("NativeThread.sleep(time.milliseconds)", "korlibs.datastructure.thread.NativeThread", "korlibs.time.milliseconds"))
fun Thread_sleep(time: Long): Unit = NativeThread.sleep(time.milliseconds)
//inline fun spinWhile(cond: () -> Boolean): Unit = NativeThread.spinWhile(cond)

@Deprecated("", ReplaceWith("NativeThread.sleepUntil(dateTime, exact)", "korlibs.datastructure.thread.NativeThread", "korlibs.datastructure.thread.sleepUntil"))
fun Thread_sleepUntil(dateTime: DateTime, exact: Boolean = true) {
    NativeThread.sleepUntil(dateTime, exact)
}
