@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.thread

expect class NativeThread(code: () -> Unit) {
    var isDaemon: Boolean
    fun start(): Unit
    fun interrupt(): Unit
}

public fun nativeThread(
    start: Boolean = true,
    isDaemon: Boolean = false,
    //name: String? = null,
    //priority: Int = -1,
    block: () -> Unit
): NativeThread {
    val thread = NativeThread(block)
    if (isDaemon) thread.isDaemon = true
    // if (priority > 0) thread.priority = priority
    // if (name != null) thread.name = name
    // if (contextClassLoader != null) thread.contextClassLoader = contextClassLoader
    if (start) thread.start()
    return thread
}
