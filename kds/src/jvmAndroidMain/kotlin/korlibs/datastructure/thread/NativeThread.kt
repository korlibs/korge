package korlibs.datastructure.thread

actual class NativeThread actual constructor(val code: () -> Unit) {
    val thread = Thread(code)
    actual var isDaemon: Boolean
        get() = thread.isDaemon
        set(value) { thread.isDaemon = value }

    actual fun start() {
        thread.start()
    }

    actual fun interrupt() {
        // No operation
        thread.interrupt()
    }
}