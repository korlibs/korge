package com.soywiz.korio.async

import kotlin.native.concurrent.*
import kotlin.coroutines.*
import kotlinx.coroutines.*

suspend fun <T, R> executeInWorker(worker: kotlin.native.concurrent.Worker, value: T, func: (T) -> R): R {
	class Info(val value: T, val func: (T) -> R)
	val info = Info(value.freeze(), func.freeze())
	val future = worker.execute(kotlin.native.concurrent.TransferMode.UNSAFE, { info }, { it: Info -> it.func(it.value) })
	return future.await()
}

/*
@UseExperimental(InternalCoroutinesApi::class)
suspend fun <T, R> executeInWorker(worker: kotlin.native.concurrent.Worker, value: T, func: (T) -> R): R = kotlin.coroutines.suspendCoroutine { c ->
	class Info(val value: T, val func: (T) -> R, val c: kotlin.coroutines.Continuation<R>)
	val info = Info(value.freeze(), func.freeze(), c)
	worker.execute(kotlin.native.concurrent.TransferMode.UNSAFE, { info }, { it: Info ->
		try {
			val result = it.func(it.value)
			println("WORKER RESULT: $result")
			val c = it.c
			val ccontext = c.context
			(ccontext[ContinuationInterceptor]!! as Delay).invokeOnTimeout(0L, Runnable {
				try {
					println("DISPATCHED RUNNABLE: $result")
					c.resume(result)
				} catch (e: Throwable) {
					println(e)
				}
			})
			/*
			(ccontext[ContinuationInterceptor]!! as CoroutineDispatcher).dispatch(ccontext, Runnable {
				try {
					println("DISPATCHED RUNNABLE: $result")
					c.resume(result)
				} catch (e: Throwable) {
					println(e)
				}
			})
			*/
		} catch (e: Throwable) {
			println(e)
		}
	})

	//val it = info
	//val result = it.func(it.value)
	//val c = it.c
	//val ccontext = c.context
	//(ccontext[ContinuationInterceptor]!! as CoroutineDispatcher).dispatch(ccontext, Runnable { c.resume(result) })
}
*/