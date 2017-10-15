package com.soywiz.korge.bus

import com.soywiz.korio.inject.InjectedHandler
import com.soywiz.korio.inject.Prototype
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.lang.Closeable
import kotlin.reflect.KClass

@Prototype
class Bus(
	private val globalBus: GlobalBus
) : Closeable, InjectedHandler {
	private val closeables = arrayListOf<Closeable>()

	suspend fun send(message: Any) {
		globalBus.send(message)
	}

	fun <T : Any> register(clazz: KClass<out T>, handler: suspend (T) -> Unit): Closeable {
		val closeable = globalBus.register(clazz, handler)
		closeables += closeable
		return closeable
	}

	fun registerInstance(instance: Any) {
		TODO()
		//for (method in instance.javaClass.allDeclaredMethods) {
		//	if (method.getAnnotation(BusHandler::class.java) != null) {
		//		val param = method.parameterTypes.firstOrNull() ?: continue
		//		register(param) {
		//			method.invokeSuspend(instance, listOf(it))
		//		}
		//	}
		//}
	}

	suspend override fun injectedInto(instance: Any) {
		registerInstance(instance)
	}

	override fun close() {
		for (c in closeables) c.close()
	}
}

@Singleton
class GlobalBus {
	val perClassHandlers = HashMap<KClass<*>, ArrayList<suspend (Any) -> Unit>>()

	suspend fun send(message: Any) {
		val clazz = message::class
		val handlers = perClassHandlers[clazz]
		if (handlers != null) {
			for (handler in handlers) handler(message)
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
}

//@JTranscKeep
//@JTranscKeepName
annotation class BusHandler
