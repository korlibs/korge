package korlibs.datastructure.thread

actual class NativeThread actual constructor(val code: () -> Unit) {
    actual var isDaemon: Boolean = false

    actual fun start() {
        TODO()
    }

    actual fun interrupt() {
        TODO()
    }
}