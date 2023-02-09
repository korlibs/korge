package com.soywiz.korio.async

import kotlinx.coroutines.*
import java.util.concurrent.*
import java.util.logging.Logger
import kotlin.coroutines.*

fun <T> Deferred<T>.jvmSyncAwait(): T = runBlocking { await() }

operator fun ExecutorService.invoke(callback: () -> Unit) {
	this.execute(callback)
}

private val mainDispatcher: ExecutorCoroutineDispatcher by lazy { newSingleThreadContext("mainDispatcher") }
internal val workerContext: ExecutorCoroutineDispatcher by lazy { newFixedThreadPoolContext(4, "worker") }
//internal val workerContext by lazy { newSingleThreadContext("worker") }

actual fun asyncEntryPoint(callback: suspend () -> Unit) =
    //runBlocking { callback() }
	runBlocking(mainDispatcher) { callback() }
actual fun asyncTestEntryPoint(callback: suspend () -> Unit) =
    //runBlocking { callback() }
    runBlocking(mainDispatcher) { callback() }

val executeInWorkerJVMLogger = com.soywiz.klogger.Logger("ExecuteInWorkerJVM")

suspend fun <T> executeInWorkerJVM(callback: suspend () -> T): T {
    try {
        return withContext(workerContext) {
            callback()
        }
    } catch (e: Throwable) {
        if (e is TimeoutCancellationException) {
            System.err.println("executeInWorkerJVM.workerContext=$workerContext")
            e.printStackTrace()
        }
        executeInWorkerJVMLogger.info { "executeInWorkerJVM.workerContext=$workerContext, error=${e.message}" }
        //e.printStackTrace()
        throw e
    } finally {
        //println("executeInWorkerJVM.workerContext=$workerContext")
    }
}
