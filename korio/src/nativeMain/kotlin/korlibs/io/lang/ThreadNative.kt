package korlibs.io.lang

import kotlinx.cinterop.convert

actual fun Thread_sleep(ms: Double) {
	platform.posix.usleep((ms * 1000).toLong().convert())
}
