package com.soywiz.korge.bus

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korinject.AsyncDestructor
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korio.async.launchUnscoped
import com.soywiz.korio.lang.Closeable
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

class Bus(
	private val globalBus: GlobalBus,
    val coroutineContext: CoroutineContext = globalBus.coroutineContext,
) : Closeable, AsyncDestructor {
	private val closeables = arrayListOf<Closeable>()

	suspend fun send(message: Any) {
		globalBus.send(message)
	}

    fun sendAsync(message: Any, coroutineContext: CoroutineContext = this.coroutineContext) {
        globalBus.sendAsync(message, coroutineContext)
    }

	fun <T : Any> register(clazz: KClass<out T>, handler: suspend (T) -> Unit): Closeable {
		val closeable = globalBus.register(clazz, handler)
		closeables += closeable
		return closeable
	}

    inline fun <reified T : Any> register(noinline handler: suspend (T) -> Unit): Closeable {
        return register(T::class, handler)
    }

	override fun close() {
		closeables.fastForEach { c ->
			c.close()
		}
	}

    override suspend fun deinit() {
        close()
    }
}

class GlobalBus(
    val coroutineContext: CoroutineContext
) {
	private val perClassHandlers = HashMap<KClass<*>, ArrayList<suspend (Any) -> Unit>>()

	suspend fun send(message: Any) {
		val clazz = message::class
		perClassHandlers[clazz]?.fastForEach { handler ->
			handler(message)
		}
	}

    fun sendAsync(message: Any, coroutineContext: CoroutineContext = this.coroutineContext) {
        coroutineContext.launchUnscoped { send(message) }
    }

	private fun forClass(clazz: KClass<*>) = perClassHandlers.getOrPut(clazz) { arrayListOf() }

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> register(clazz: KClass<out T>, handler: suspend (T) -> Unit): Closeable {
		val chandler = handler as (suspend (Any) -> Unit)
		forClass(clazz).add(chandler)
		return Closeable {
            unregister(clazz, chandler)
		}
	}

    fun <T : Any> unregister(clazz: KClass<out T>, handler: suspend (T) -> Unit) {
        forClass(clazz).remove(handler)
    }

    inline fun <reified T : Any> register(noinline handler: suspend (T) -> Unit): Closeable {
       return register(T::class, handler)
    }
}

fun AsyncInjector.mapBus() {
    mapSingleton { GlobalBus(get()) }
    mapPrototype { Bus(get(), get()) }
}
