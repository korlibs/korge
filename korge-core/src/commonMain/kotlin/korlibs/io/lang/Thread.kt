package korlibs.io.lang

expect val currentThreadId: Long
expect val currentThreadName: String?

expect fun Thread_sleep(time: Long): Unit
