package korlibs.io.async

import kotlinx.coroutines.*
import java.util.concurrent.*

fun <T> Deferred<T>.jvmSyncAwait(): T = runBlocking { await() }

operator fun ExecutorService.invoke(callback: () -> Unit) {
	this.execute(callback)
}

actual fun asyncEntryPoint(callback: suspend () -> Unit) = runBlocking { callback() }
actual fun asyncTestEntryPoint(callback: suspend () -> Unit) = runBlocking { callback() }