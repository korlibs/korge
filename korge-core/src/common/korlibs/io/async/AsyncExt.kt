package korlibs.io.async

import korlibs.io.lang.*
import korlibs.logger.*
import korlibs.platform.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

private val logger = Logger("AsyncExt")

suspend fun <T> CoroutineContext.launchUnscopedAndWait(block: suspend () -> T): T {
    val deferred = CompletableDeferred<T>()
    block.startCoroutine(object : Continuation<T> {
        override val context: CoroutineContext = this@launchUnscopedAndWait

        override fun resumeWith(result: Result<T>) {
            deferred.completeWith(result)
        }
    })
    return deferred.await()
}

fun CoroutineContext.onCancel(block: () -> Unit): Cancellable {
    var running = true
    launchUnscoped {
        try {
            while (running) kotlinx.coroutines.delay(1.seconds)
        } catch (e: CancellationException) {
            if (running) block()
        }
    }
    return Cancellable { running = false }
}

fun CoroutineContext.launchUnscoped(block: suspend () -> Unit) {
    block.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext = this@launchUnscoped

        override fun resumeWith(result: Result<Unit>) {
            if (result.isFailure) {
                result.exceptionOrNull()?.printStackTrace()
            }
        }
    })
}

fun CoroutineScope.launchUnscoped(block: suspend () -> Unit) = coroutineContext.launchUnscoped(block)

fun CoroutineScope.launch(callback: suspend () -> Unit): Job = _launch(CoroutineStart.UNDISPATCHED, callback)
fun CoroutineScope.launchImmediately(callback: suspend () -> Unit): Job = _launch(CoroutineStart.UNDISPATCHED, callback)
fun CoroutineScope.launchAsap(callback: suspend () -> Unit): Job = _launch(CoroutineStart.DEFAULT, callback)

fun <T> CoroutineScope.async(callback: suspend () -> T): Deferred<T> = _async(CoroutineStart.UNDISPATCHED, callback)
fun <T> CoroutineScope.asyncImmediately(callback: suspend () -> T): Deferred<T> = _async(CoroutineStart.UNDISPATCHED, callback)
fun <T> CoroutineScope.asyncAsap(callback: suspend () -> T): Deferred<T> = _async(CoroutineStart.DEFAULT, callback)

fun launch(context: CoroutineContext, callback: suspend () -> Unit) = CoroutineScope(context).launchImmediately(callback)
fun launchImmediately(context: CoroutineContext, callback: suspend () -> Unit) = CoroutineScope(context).launchImmediately(callback)
fun launchAsap(context: CoroutineContext, callback: suspend () -> Unit) = CoroutineScope(context).launchAsap(callback)

fun <T> async(context: CoroutineContext, callback: suspend () -> T) = CoroutineScope(context).asyncImmediately(callback)
fun <T> asyncImmediately(context: CoroutineContext, callback: suspend () -> T) = CoroutineScope(context).asyncImmediately(callback)
fun <T> asyncAsap(context: CoroutineContext, callback: suspend () -> T) = CoroutineScope(context).asyncAsap(callback)

public expect class AsyncEntryPointResult

expect fun asyncEntryPoint(callback: suspend () -> Unit): AsyncEntryPointResult
expect fun asyncTestEntryPoint(callback: suspend () -> Unit): AsyncEntryPointResult

val DEFAULT_SUSPEND_TEST_TIMEOUT = 20.seconds
//val DEFAULT_TEST_SYNC_IO: Boolean? = true
val DEFAULT_TEST_SYNC_IO: Boolean? = false

fun suspendTest(timeout: TimeSpan? = DEFAULT_SUSPEND_TEST_TIMEOUT, preferSyncIo: Boolean? = null, callback: suspend CoroutineScope.() -> Unit) = asyncTestEntryPoint {
    withContext(PreferSyncIo(preferSyncIo)) {
        if (timeout != null) withTimeoutNullable(timeout) { callback() } else coroutineScope { callback() }
    }
}
fun suspendTest(cond: () -> Boolean, timeout: TimeSpan? = DEFAULT_SUSPEND_TEST_TIMEOUT, preferSyncIo: Boolean? = DEFAULT_TEST_SYNC_IO, callback: suspend CoroutineScope.() -> Unit) = suspendTest(timeout, preferSyncIo = preferSyncIo) { if (cond()) callback() }
fun suspendTestNoBrowser(preferSyncIo: Boolean? = DEFAULT_TEST_SYNC_IO, callback: suspend CoroutineScope.() -> Unit) = suspendTest({ !Platform.isJsBrowser }, callback = callback, preferSyncIo = preferSyncIo)
fun suspendTestNoJs(preferSyncIo: Boolean? = DEFAULT_TEST_SYNC_IO, callback: suspend CoroutineScope.() -> Unit) = suspendTest({ !Platform.isJs && !Platform.isWasm }, callback = callback, preferSyncIo = preferSyncIo)

val DEBUG_ASYNC_LAUNCH_ERRORS by lazy { Environment["DEBUG_ASYNC_LAUNCH_ERRORS"] == "true" }

private fun CoroutineScope._launch(start: CoroutineStart, callback: suspend () -> Unit): Job = launch(coroutineContext, start = start) {
	try {
		callback()
	} catch (e: CancellationException) {
		throw e
	} catch (e: Throwable) {
        if (DEBUG_ASYNC_LAUNCH_ERRORS) {
            logger.error { "CoroutineScope._launch.catch:" }
            e.printStackTrace()
        }
		throw e
	}
}

private fun <T> CoroutineScope._async(start: CoroutineStart, callback: suspend () -> T): Deferred<T> = async(coroutineContext, start = start) {
	try {
		callback()
	} catch (e: Throwable) {
        if (e is CancellationException) throw e
        if (DEBUG_ASYNC_LAUNCH_ERRORS) {
            logger.error { "CoroutineScope._async.catch:" }
            e.printStackTrace()
        }
		throw e
	}
}

// @TODO: Kotlin.JS bug!
//fun suspendTestExceptJs(callback: suspend () -> Unit) = suspendTest {
//	if (OS.isJs) return@suspendTest
//	callback()
//}
