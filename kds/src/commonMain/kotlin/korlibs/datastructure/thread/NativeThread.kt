package korlibs.datastructure.thread

expect class NativeThread(code: () -> Unit) {
    var isDaemon: Boolean
    fun start(): Unit
    fun interrupt(): Unit
}