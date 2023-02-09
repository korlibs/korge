package com.soywiz.korio.async

import kotlinx.coroutines.*
import java.util.concurrent.*
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

suspend fun <T> executeInWorkerJVM(callback: suspend () -> T): T {
    try {
        return withContext(workerContext) {
            callback()
        }
    } catch (e: Throwable) {
        println("executeInWorkerJVM.workerContext=$workerContext")
        e.printStackTrace()
        throw e
    } finally {
        //println("executeInWorkerJVM.workerContext=$workerContext")
    }
}
