package korlibs.datastructure.thread

import korlibs.concurrent.thread.*

@Deprecated("", ReplaceWith("korlibs.concurrent.thread.NativeThread"))
typealias NativeThread = korlibs.concurrent.thread.NativeThread

@Deprecated("", ReplaceWith("korlibs.concurrent.thread.nativeThread(start, isDaemon, name, priority, block)", "korlibs"))
public fun nativeThread(
    start: Boolean = true,
    isDaemon: Boolean = false,
    name: String? = null,
    priority: NativeThreadPriority = NativeThreadPriority.NORMAL,
    block: (NativeThread) -> Unit
): NativeThread = korlibs.concurrent.thread.nativeThread(isDaemon = isDaemon, name = name, priority = priority, start = start, block = block)
