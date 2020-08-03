@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.*


abstract class BaseSignal<T, THandler>(val onRegister: () -> Unit = {}) {
	inner class Node(val once: Boolean, val item: THandler) : Closeable {
		override fun close() {
			if (iterating > 0) {
				handlersToRemove.add(this)
			} else {
				handlers.remove(this)
			}
		}
	}

	protected var handlers = ArrayList<Node>()
	protected var handlersToRemove = ArrayList<Node>()
	val listenerCount: Int get() = handlers.size
	fun clear() = handlers.clear()

	// @TODO: This breaks binary compatibility
	//fun once(handler: THandler): Closeable = _add(true, handler)
	//fun add(handler: THandler): Closeable = _add(false, handler)
	//operator fun invoke(handler: THandler): Closeable = add(handler)

	protected fun _add(once: Boolean, handler: THandler): Closeable {
		onRegister()
		val node = Node(once, handler)
		handlers.add(node)
		return node
	}
	protected var iterating: Int = 0
	protected inline fun iterateCallbacks(callback: (THandler) -> Unit) {
		try {
			iterating++
			handlers.fastIterateRemove { node ->
				val remove = node.once
				callback(node.item)
				remove
			}
		} finally {
			iterating--
			if (handlersToRemove.isNotEmpty()) {
				handlersToRemove.fastIterateRemove {
					handlers.remove(it)
					true
				}
			}
		}
	}
	suspend fun listen(): ReceiveChannel<T> = produce {
		while (true) send(waitOneBase())
	}
	abstract suspend fun waitOneBase(): T
}

class AsyncSignal<T>(onRegister: () -> Unit = {}) : BaseSignal<T, suspend (T) -> Unit>(onRegister) {
	fun once(handler: suspend (T) -> Unit): Closeable = _add(true, handler)
	fun add(handler: suspend (T) -> Unit): Closeable = _add(false, handler)
	operator fun invoke(handler: suspend (T) -> Unit): Closeable = add(handler)

	suspend operator fun invoke(value: T) = iterateCallbacks{ it(value) }
	override suspend fun waitOneBase(): T = suspendCancellableCoroutine { c ->
		var close: Closeable? = null
		close = once {
			close?.close()
			c.resume(it)
		}
		c.invokeOnCancellation {
			close.close()
		}
	}
}

class Signal<T>(onRegister: () -> Unit = {}) : BaseSignal<T, (T) -> Unit>(onRegister) {
	fun once(handler: (T) -> Unit): Closeable = _add(true, handler)
	fun add(handler: (T) -> Unit): Closeable = _add(false, handler)
	operator fun invoke(handler: (T) -> Unit): Closeable = add(handler)
	operator fun invoke(value: T) = iterateCallbacks { it(value) }
	override suspend fun waitOneBase(): T= suspendCancellableCoroutine { c ->
		var close: Closeable? = null
		close = once {
			close?.close()
			c.resume(it)
		}
		c.invokeOnCancellation {
			close?.close()
		}
	}
}

suspend fun <T> AsyncSignal<T>.waitOne() = waitOneBase()
suspend fun <T> Signal<T>.waitOne() = waitOneBase()

fun <TI, TO> AsyncSignal<TI>.mapSignal(transform: (TI) -> TO): AsyncSignal<TO> {
	val out = AsyncSignal<TO>()
	this.add { out(transform(it)) }
	return out
}

suspend operator fun AsyncSignal<Unit>.invoke() = invoke(Unit)

//////////////////////////////////


//class AsyncSignal<T>(context: CoroutineContext) {

//}

fun <TI, TO> Signal<TI>.mapSignal(transform: (TI) -> TO): Signal<TO> {
	val out = Signal<TO>()
	this.add { out(transform(it)) }
	return out
}

operator fun Signal<Unit>.invoke() = invoke(Unit)

suspend fun Iterable<Signal<*>>.waitOne(): Any? = suspendCancellableCoroutine { c ->
	val closes = arrayListOf<Closeable>()
	for (signal in this) {
		closes += signal.once {
			closes.close()
			c.resume(it)
		}
	}

	c.invokeOnCancellation {
		closes.close()
	}
}

fun <T> Signal<T>.waitOneAsync(): Deferred<T> {
	val deferred = CompletableDeferred<T>(Job())
	var close: Closeable? = null
	close = once {
		close?.close()
		deferred.complete(it)
	}
	deferred.invokeOnCompletion {
		close.close()
	}
	return deferred
}

@Deprecated("", ReplaceWith("waitOneAsync()"))
fun <T> Signal<T>.waitOnePromise(): Deferred<T> = waitOneAsync()

suspend fun <T> Signal<T>.addSuspend(handler: suspend (T) -> Unit): Closeable {
	val cc = coroutineContext
	return this@addSuspend { value ->
		launchImmediately(cc) {
			handler(value)
		}
	}
}

fun <T> Signal<T>.addSuspend(context: CoroutineContext, handler: suspend (T) -> Unit): Closeable =
	this@addSuspend { value ->
		launchImmediately(context) {
			handler(value)
		}
	}


suspend fun <T> Signal<T>.waitOne(timeout: TimeSpan): T? = kotlinx.coroutines.suspendCancellableCoroutine { c ->
	var close: Closeable? = null
	var running = true

	fun closeAll() {
		running = false
		close?.close()
	}

	launchImmediately(c.context) {
		delay(timeout)
		if (running) {
			closeAll()
			c.resume(null)
		}
	}

	close = once {
		closeAll()
		c.resume(it)
	}

	c.invokeOnCancellation {
		closeAll()
	}
}

suspend fun <T> Signal<T>.waitOneOpt(timeout: TimeSpan?): T? = when {
	timeout != null -> waitOne(timeout)
	else -> waitOneBase()
}

suspend inline fun <T> Map<Signal<Unit>, T>.executeAndWaitAnySignal(callback: () -> Unit): T {
	val deferred = CompletableDeferred<T>()
	val closeables = this.map { pair -> pair.key.once { deferred.complete(pair.value) } }
	try {
		callback()
		return deferred.await()
	} finally {
		closeables.close()
	}
}

suspend inline fun <T> Iterable<Signal<T>>.executeAndWaitAnySignal(callback: () -> Unit): Pair<Signal<T>, T> {
	val deferred = CompletableDeferred<Pair<Signal<T>, T>>()
	val closeables = this.map { signal -> signal.once { deferred.complete(signal to it) } }
	try {
		callback()
		return deferred.await()
	} finally {
		closeables.close()
	}
}

suspend inline fun <T> Signal<T>.executeAndWaitSignal(callback: () -> Unit): T {
	val deferred = CompletableDeferred<T>()
	val closeable = this.once { deferred.complete(it) }
	try {
		callback()
		return deferred.await()
	} finally {
		closeable.close()
	}
}

fun <T> Signal<T>.addCallInit(initial: T, handler: (T) -> Unit): Closeable {
    handler(initial)
    return add(handler)
}

fun Signal<Unit>.addCallInit(handler: (Unit) -> Unit): Closeable = addCallInit(Unit, handler)
