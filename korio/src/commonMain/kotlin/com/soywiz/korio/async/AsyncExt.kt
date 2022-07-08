package com.soywiz.korio.async

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.klogger.Console
import com.soywiz.korio.lang.Environment
import com.soywiz.korio.util.OS
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.native.concurrent.ThreadLocal

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

expect fun asyncEntryPoint(callback: suspend () -> Unit)
expect fun asyncTestEntryPoint(callback: suspend () -> Unit)

val DEFAULT_SUSPEND_TEST_TIMEOUT = 20.seconds

fun suspendTest(timeout: TimeSpan?, callback: suspend CoroutineScope.() -> Unit) = asyncTestEntryPoint { if (timeout != null) withTimeout(timeout) { callback() } else coroutineScope { callback() } }
//fun suspendTest(timeout: TimeSpan?, callback: suspend CoroutineScope.() -> Unit) = asyncEntryPoint { coroutineScope { callback() } }
fun suspendTest(callback: suspend CoroutineScope.() -> Unit) = suspendTest(DEFAULT_SUSPEND_TEST_TIMEOUT, callback)
fun suspendTest(cond: () -> Boolean, timeout: TimeSpan? = DEFAULT_SUSPEND_TEST_TIMEOUT, callback: suspend CoroutineScope.() -> Unit) = suspendTest(timeout) { if (cond()) callback() }
fun suspendTestNoBrowser(callback: suspend CoroutineScope.() -> Unit) = suspendTest({ !OS.isJsBrowser }, callback = callback)
fun suspendTestNoJs(callback: suspend CoroutineScope.() -> Unit) = suspendTest({ !OS.isJs }, callback = callback)

@ThreadLocal
val DEBUG_ASYNC_LAUNCH_ERRORS by lazy { Environment["DEBUG_ASYNC_LAUNCH_ERRORS"] == "true" }

private fun CoroutineScope._launch(start: CoroutineStart, callback: suspend () -> Unit): Job = launch(coroutineContext, start = start) {
	try {
		callback()
	} catch (e: CancellationException) {
		throw e
	} catch (e: Throwable) {
        if (DEBUG_ASYNC_LAUNCH_ERRORS) {
            Console.error("CoroutineScope._launch.catch:")
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
            Console.error("CoroutineScope._async.catch:")
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

