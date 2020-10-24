package com.soywiz.korio.async

import com.soywiz.korio.concurrent.lock.*
import com.soywiz.korio.concurrent.lock.Lock
import kotlinx.coroutines.*
import kotlin.coroutines.*

interface AsyncInvokable {
	suspend operator fun <T> invoke(func: suspend () -> T): T
}

//class AsyncQueue(val context: CoroutineContext) {
class AsyncQueue {
	//constructor() : AsyncQueue(CoroutineContext())

	val thread = AsyncThread()

	//companion object {
	//	suspend operator fun invoke() = AsyncQueue(getCoroutineContext())
	//}

	suspend operator fun invoke(func: suspend () -> Unit): AsyncQueue = invoke(coroutineContext, func)

	operator fun invoke(context: CoroutineContext, func: suspend () -> Unit): AsyncQueue {
		thread.sync(context) { func() }
		return this
	}

	suspend fun await(func: suspend () -> Unit) {
		invoke(func)
		await()
	}

	suspend fun await() {
		thread.await()
	}
}

fun AsyncQueue.withContext(ctx: CoroutineContext) = AsyncQueueWithContext(this, ctx)
suspend fun AsyncQueue.withContext() = AsyncQueueWithContext(this, coroutineContext)

class AsyncQueueWithContext(val queue: AsyncQueue, val context: CoroutineContext) {
	operator fun invoke(func: suspend () -> Unit): AsyncQueue = queue.invoke(context, func)
	suspend fun await(func: suspend () -> Unit) = queue.await(func)
	suspend fun await() = queue.await()
}

class AsyncThread() : AsyncInvokable {
	private var lastPromise: Deferred<*>? = null

	suspend fun await() {
		while (true) {
			val cpromise = lastPromise
			lastPromise?.await()
			if (cpromise == lastPromise) break
		}
	}

	fun cancel(): AsyncThread {
		lastPromise?.cancel()
		lastPromise = CompletableDeferred(Unit)
		return this
	}

	suspend fun <T> cancelAndQueue(func: suspend () -> T): T {
		cancel()
		return queue(func)
	}

	suspend fun <T> queue(func: suspend () -> T): T = invoke(func)

	override suspend operator fun <T> invoke(func: suspend () -> T): T {
		val task = sync(coroutineContext, func)
		try {
			val res = task.await()
			return res
		} catch (e: Throwable) {
			throw e
		}
	}

	suspend fun <T> sync(func: suspend () -> T): Deferred<T> = sync(coroutineContext, func)

	fun <T> sync(context: CoroutineContext, func: suspend () -> T): Deferred<T> {
		val oldPromise = lastPromise
		val promise = asyncImmediately(context) {
			oldPromise?.await()
			func()
		}
		lastPromise = promise
		return promise

	}
}

/**
 * Creates a queue of processes that will be executed one after another by effectively preventing from executing
 * them at the same time.
 * This class is thread-safe.
 */
class AsyncThread2 : AsyncInvokable {
	private val lock = Lock()
	private var lastPromise: Deferred<*> = CompletableDeferred(Unit)

	suspend fun await() {
		while (true) {
			val cpromise = lock { lastPromise }
			cpromise.await()
			if (lock { cpromise == lastPromise }) break
		}
	}

	fun cancel() = apply {
		lock { lastPromise }.cancel()
		lock { lastPromise = CompletableDeferred(Unit) }
	}

	override suspend operator fun <T> invoke(func: suspend () -> T): T {
		val task = invoke(coroutineContext, func)
		try {
			val res = task.await()
			return res
		} catch (e: Throwable) {
			throw e
		}
	}

	private operator fun <T> invoke(context: CoroutineContext, func: suspend () -> T): Deferred<T> = lock {
		val oldPromise = lastPromise
		CoroutineScope(context).async {
			oldPromise.await()
			func()
		}.also { lastPromise = it }
	}
}

/**
 * Prevents a named invoke to happen at the same time (by effectively enqueuing by name).
 * This class is thread-safe.
 */
class NamedAsyncThreads(val threadFactory: () -> AsyncInvokable = { AsyncThread2() }) {
	class AsyncJob(val thread: AsyncInvokable) {
		var count = 0
	}
	private val lock = Lock()
	private val jobs = LinkedHashMap<String, AsyncJob>()

	internal fun threadsCount() = jobs.size

	suspend operator fun <T> invoke(name: String, func: suspend () -> T): T {
		val job = lock {
			jobs.getOrPut(name) { AsyncJob(threadFactory()) }.also { it.count++ }
		}
		try {
			return job.thread.invoke(func)
		} finally {
			// Synchronization to prevent another thread from being added in the mean time, or a process queued.
			lock {
				job.count--
				if (job.count == 0) {
					jobs.remove(name)
				}
			}
		}
	}
}
