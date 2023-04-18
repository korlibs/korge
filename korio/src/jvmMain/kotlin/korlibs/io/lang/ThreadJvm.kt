package korlibs.io.lang

actual val currentThreadId: Long get() = Thread.currentThread().id
actual val currentThreadName: String? get() = Thread.currentThread().name

actual fun Thread_sleep(ms: Double) {
    Thread.sleep(ms.toLong(), ((ms % 1.0) * 1_000_000).toInt())
}
