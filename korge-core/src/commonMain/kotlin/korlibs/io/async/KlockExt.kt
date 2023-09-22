package korlibs.io.async

import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

//suspend fun delay(time: TimeSpan): Unit = kotlinx.coroutines.delay(time.millisecondsLong)
suspend fun CoroutineContext.delay(time: TimeSpan) = kotlinx.coroutines.delay(time.millisecondsLong)

suspend fun <T> withTimeoutNullable(time: TimeSpan?, block: suspend CoroutineScope.() -> T): T {
	return if (time == null || time.isNil) {
		block(CoroutineScope(coroutineContext))
	} else {
		withTimeout(time.millisecondsLong, block)
	}
}
