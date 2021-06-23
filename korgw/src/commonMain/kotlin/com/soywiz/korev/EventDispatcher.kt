package com.soywiz.korev

import com.soywiz.kds.*
import com.soywiz.korio.lang.*
import kotlin.reflect.*

interface EventDispatcher {
	fun <T : Event> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Closeable
	fun <T : Event> dispatch(clazz: KClass<T>, event: T)
	fun copyFrom(other: EventDispatcher) = Unit

	open class Mixin : EventDispatcher {
		private var handlers: LinkedHashMap<KClass<out Event>, FastArrayList<(Event) -> Unit>>? = null

		private fun <T : Event> getHandlersFor(clazz: KClass<T>): FastArrayList<(T) -> Unit>? {
            if (handlers == null) return null
			@Suppress("UNCHECKED_CAST")
			return handlers?.get(clazz) as? FastArrayList<(T) -> Unit>?
		}

        private fun <T : Event> getHandlersForCreate(clazz: KClass<T>): FastArrayList<(T) -> Unit> {
            if (handlers == null) handlers = LinkedHashMap()
            @Suppress("UNCHECKED_CAST")
            return handlers!!.getOrPut(clazz) { FastArrayList() } as FastArrayList<(T) -> Unit>
        }

        override fun <T : Event> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Closeable {
            val handlers = getHandlersForCreate(clazz)
			handlers += handler
			return Closeable { handlers -= handler }
		}

		final override fun copyFrom(other: EventDispatcher) {
			handlers?.clear()
			if (other is Mixin) {
                val otherHandlers = other.handlers
                if (otherHandlers != null) {
                    for ((clazz, events) in otherHandlers) {
                        for (event in events) {
                            addEventListener(clazz, event)
                        }
                    }
                }
			}
		}

        override fun <T : Event> dispatch(clazz: KClass<T>, event: T) {
            if (handlers == null) return
            getHandlersFor(clazz)?.fastForEach { handler ->
                handler(event)
            }
		}
	}

	companion object {
		operator fun invoke(): EventDispatcher = Mixin()
	}
}

object DummyEventDispatcher : EventDispatcher, Closeable {
	override fun close() {
	}

	override fun <T : Event> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Closeable {
		return this
	}

	override fun <T : Event> dispatch(clazz: KClass<T>, event: T) {
	}
}


inline fun <reified T : Event> EventDispatcher.addEventListener(noinline handler: (T) -> Unit) = addEventListener(T::class, handler)
inline fun <reified T : Event> EventDispatcher.dispatch(event: T) = dispatch(T::class, event)

inline operator fun <T : Event> T.invoke(callback: T.() -> Unit): T = this.apply(callback)

open class Event {
	var target: Any? = null
    var _stopPropagation = false
    fun stopPropagation() {
        _stopPropagation = true
    }
}

fun Event.preventDefault(reason: Any? = null): Nothing = throw PreventDefaultException(reason)
fun preventDefault(reason: Any? = null): Nothing = throw PreventDefaultException(reason)

class PreventDefaultException(val reason: Any? = null) : Exception()

/*

//interface Cancellable

interface EventDispatcher {
	class Mixin : EventDispatcher {
		val events = hashMapOf<KClass<*>, ArrayList<(Any) -> Unit>>()

		override fun <T : Any> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Cancellable {
			val handlers = events.getOrPut(clazz) { arrayListOf() }
			val chandler = handler as ((Any) -> Unit)
			handlers += chandler
			return Cancellable { handlers -= chandler }
		}

		override fun <T : Any> dispatch(event: T, clazz: KClass<out T>) {
			val handlers = events[clazz]
			if (handlers != null) {
				for (handler in handlers.toList()) {
					handler(event)
				}
			}
		}

	}

	fun <T : Any> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Cancellable
	fun <T : Any> dispatch(event: T, clazz: KClass<out T> = event::class): Unit
}

interface Event

inline fun <reified T : Any> EventDispatcher.addEventListener(noinline handler: (T) -> Unit): Cancellable =
	this.addEventListener(T::class, handler)

inline suspend fun <reified T : Any> EventDispatcher.addEventListenerSuspend(noinline handler: suspend (T) -> Unit): Cancellable {
	val context = getCoroutineContext()
	return this.addEventListener(T::class) { event ->
		context.go {
			handler(event)
		}
	}
}

/*
class ED : EventDispatcher by EventDispatcher.Mixin() {
	override fun dispatch(event: Any) {
		//super.dispatch(event) // ERROR!
		println("dispatched: $event!")
	}
}

open class ED1 : EventDispatcher by EventDispatcher.Mixin()
open class ED2 : ED1() {
	override fun dispatch(event: Any) {
		super.dispatch(event) // WORKS
		println("dispatched: $event!")
	}
}



class ED2(val ed: EventDispatcher = EventDispatcher.Mixin()) : EventDispatcher by ed {
	override fun dispatch(event: Any) {
		ed.dispatch(event)
		println("dispatched: $event!")
	}
}
*/

 */
