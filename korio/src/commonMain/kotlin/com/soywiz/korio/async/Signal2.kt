package com.soywiz.korio.async

import com.soywiz.kds.iterators.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.*

abstract class BaseSignal2<T1, T2>(val onRegister: () -> Unit = {}) {
    inner class Node(val once: Boolean, val item: (T1, T2) -> Unit) : Closeable {
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

    protected fun _add(once: Boolean, handler: (T1, T2) -> Unit): Closeable {
        onRegister()
        val node = Node(once, handler)
        handlers.add(node)
        return node
    }
    protected var iterating: Int = 0
    protected inline fun iterateCallbacks(callback: ((T1, T2) -> Unit) -> Unit) {
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
    suspend fun listen(): ReceiveChannel<Pair<T1, T2>> = produce {
        while (true) send(waitOneBase())
    }
    abstract suspend fun waitOneBase(): Pair<T1, T2>
}

class Signal2<T1, T2>(onRegister: () -> Unit = {}) : BaseSignal2<T1, T2>(onRegister) {
    fun once(handler: (T1, T2) -> Unit): Closeable = _add(true, handler)
    fun add(handler: (T1, T2) -> Unit): Closeable = _add(false, handler)
    operator fun invoke(handler: (T1, T2) -> Unit): Closeable = add(handler)
    operator fun invoke(value1: T1, value2: T2) = iterateCallbacks { it(value1, value2) }
    override suspend fun waitOneBase(): Pair<T1, T2> = suspendCancellableCoroutine { c ->
        var close: Closeable? = null
        close = once { i1, i2 ->
            close?.close()
            c.resume(Pair(i1, i2))
        }
        c.invokeOnCancellation {
            close?.close()
        }
    }
}

suspend fun <T1, T2> Signal2<T1, T2>.waitOne() = waitOneBase()
