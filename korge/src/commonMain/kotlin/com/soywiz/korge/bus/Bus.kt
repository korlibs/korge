package com.soywiz.korge.bus

import com.soywiz.korge.internal.fastForEach
import com.soywiz.korio.lang.*
import kotlin.reflect.*

class Bus(
	private val globalBus: GlobalBus
) : Closeable {
	private val closeables = arrayListOf<Closeable>()

	suspend fun send(message: Any) {
		globalBus.send(message)
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
}

class GlobalBus {
	val perClassHandlers = HashMap<KClass<*>, ArrayList<suspend (Any) -> Unit>>()

	suspend fun send(message: Any) {
		val clazz = message::class
		perClassHandlers[clazz]?.fastForEach { handler ->
			handler(message)
		}
	}

	private fun forClass(clazz: KClass<*>) = perClassHandlers.getOrPut(clazz) { arrayListOf() }

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> register(clazz: KClass<out T>, handler: suspend (T) -> Unit): Closeable {
		val chandler = handler as (suspend (Any) -> Unit)
		forClass(clazz).add(chandler)
		return Closeable {
			forClass(clazz).remove(chandler)
		}
	}

    inline fun <reified T : Any> register(noinline handler: suspend (T) -> Unit): Closeable {
       return register(T::class, handler)
    }
}
