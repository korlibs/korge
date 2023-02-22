package com.soywiz.korge.bus

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korinject.AsyncDestructor
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korio.lang.Closeable
import kotlin.reflect.KClass

class SyncBus(
	private val globalBus: SyncGlobalBus
) : Closeable, AsyncDestructor {
	private val closeables = arrayListOf<Closeable>()

	fun send(message: Any) {
		globalBus.send(message)
	}

	fun <T : Any> register(clazz: KClass<out T>, handler: (T) -> Unit): Closeable {
		val closeable = globalBus.register(clazz, handler)
		closeables += closeable
		return closeable
	}

    inline fun <reified T : Any> register(noinline handler: (T) -> Unit): Closeable {
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

class SyncGlobalBus {
	val perClassHandlers = HashMap<KClass<*>, ArrayList<(Any) -> Unit>>()

	fun send(message: Any) {
		val clazz = message::class
		perClassHandlers[clazz]?.fastForEach { handler ->
			handler(message)
		}
	}

	private fun forClass(clazz: KClass<*>) = perClassHandlers.getOrPut(clazz) { arrayListOf() }

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> register(clazz: KClass<out T>, handler: (T) -> Unit): Closeable {
		val chandler = handler as ((Any) -> Unit)
		forClass(clazz).add(chandler)
		return Closeable {
			forClass(clazz).remove(chandler)
		}
	}

    inline fun <reified T : Any> register(noinline handler: (T) -> Unit): Closeable {
       return register(T::class, handler)
    }
}

fun AsyncInjector.mapSyncBus() {
    mapSingleton { SyncGlobalBus() }
    mapPrototype { SyncBus(get()) }
}
