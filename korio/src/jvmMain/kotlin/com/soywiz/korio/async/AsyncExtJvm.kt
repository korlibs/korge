package com.soywiz.korio.async

import kotlinx.coroutines.*
import java.util.concurrent.*
import kotlin.coroutines.*

fun <T> Deferred<T>.jvmSyncAwait(): T = runBlocking { await() }

operator fun ExecutorService.invoke(callback: () -> Unit) {
	this.execute(callback)
}

private val mainDispatcher by lazy { newSingleThreadContext("mainDispatcher") }
internal val workerContext by lazy { newSingleThreadContext("worker") }

actual fun asyncEntryPoint(callback: suspend () -> Unit) =
    //runBlocking { callback() }
	runBlocking(mainDispatcher) { callback() }

suspend fun <T> executeInWorkerJVM(callback: suspend () -> T): T = withContext(workerContext) { callback() }
