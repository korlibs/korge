package com.soywiz.korio.async

import com.soywiz.klock.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

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

fun CoroutineScope.launch(callback: suspend () -> Unit) = _launch(CoroutineStart.UNDISPATCHED, callback)
fun CoroutineScope.launchImmediately(callback: suspend () -> Unit) = _launch(CoroutineStart.UNDISPATCHED, callback)
fun CoroutineScope.launchAsap(callback: suspend () -> Unit) = _launch(CoroutineStart.DEFAULT, callback)

fun <T> CoroutineScope.async(callback: suspend () -> T) = _async(CoroutineStart.UNDISPATCHED, callback)
fun <T> CoroutineScope.asyncImmediately(callback: suspend () -> T) = _async(CoroutineStart.UNDISPATCHED, callback)
fun <T> CoroutineScope.asyncAsap(callback: suspend () -> T) = _async(CoroutineStart.DEFAULT, callback)


fun launch(context: CoroutineContext, callback: suspend () -> Unit) = CoroutineScope(context).launchImmediately(callback)
fun launchImmediately(context: CoroutineContext, callback: suspend () -> Unit) = CoroutineScope(context).launchImmediately(callback)
fun launchAsap(context: CoroutineContext, callback: suspend () -> Unit) = CoroutineScope(context).launchAsap(callback)

fun <T> async(context: CoroutineContext, callback: suspend () -> T) = CoroutineScope(context).asyncImmediately(callback)
fun <T> asyncImmediately(context: CoroutineContext, callback: suspend () -> T) = CoroutineScope(context).asyncImmediately(callback)
fun <T> asyncAsap(context: CoroutineContext, callback: suspend () -> T) = CoroutineScope(context).asyncAsap(callback)

expect fun asyncEntryPoint(callback: suspend () -> Unit)

val DEFAULT_SUSPEND_TEST_TIMEOUT = 20.seconds

fun suspendTest(timeout: TimeSpan?, callback: suspend CoroutineScope.() -> Unit) = asyncEntryPoint { if (timeout != null) withTimeout(timeout) { callback() } else coroutineScope { callback() } }
//fun suspendTest(timeout: TimeSpan?, callback: suspend CoroutineScope.() -> Unit) = asyncEntryPoint { coroutineScope { callback() } }
fun suspendTest(callback: suspend CoroutineScope.() -> Unit) = suspendTest(DEFAULT_SUSPEND_TEST_TIMEOUT, callback)
fun suspendTest(cond: () -> Boolean, timeout: TimeSpan? = DEFAULT_SUSPEND_TEST_TIMEOUT, callback: suspend CoroutineScope.() -> Unit) = suspendTest(timeout) { if (cond()) callback() }
fun suspendTestNoBrowser(callback: suspend CoroutineScope.() -> Unit) = suspendTest({ !OS.isJsBrowser }, callback = callback)
fun suspendTestNoJs(callback: suspend CoroutineScope.() -> Unit) = suspendTest({ !OS.isJs }, callback = callback)

private fun CoroutineScope._launch(start: CoroutineStart, callback: suspend () -> Unit): Job = launch(coroutineContext, start = start) {
	try {
		callback()
	} catch (e: CancellationException) {
		throw e
	} catch (e: Throwable) {
		e.printStackTrace()
		throw e
	}
}

private fun <T> CoroutineScope._async(start: CoroutineStart, callback: suspend () -> T): Deferred<T> = async(coroutineContext, start = start) {
	try {
		callback()
	} catch (e: CancellationException) {
		throw e
	} catch (e: Throwable) {
		e.printStackTrace()
		throw e
	}
}

// @TODO: Kotlin.JS bug!
//fun suspendTestExceptJs(callback: suspend () -> Unit) = suspendTest {
//	if (OS.isJs) return@suspendTest
//	callback()
//}

