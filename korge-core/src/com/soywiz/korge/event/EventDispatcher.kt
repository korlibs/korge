package com.soywiz.korge.event

import com.soywiz.korio.util.Cancellable

//interface Cancellable

interface EventDispatcher {
	class Mixin : EventDispatcher {
		val events = hashMapOf<Class<*>, ArrayList<(Any) -> Unit>>()

		override fun <T : Any> addEventListener(clazz: Class<T>, handler: (T) -> Unit): Cancellable {
			val handlers = events.getOrPut(clazz) { arrayListOf() }
			val chandler = handler as ((Any) -> Unit)
			handlers += chandler
			return Cancellable { handlers -= chandler }
		}

		override fun <T : Any> dispatch(event: T, clazz: Class<T>) {
			val handlers = events[clazz]
			if (handlers != null) {
				for (handler in handlers.toList()) {
					handler(event)
				}
			}
		}

	}

	fun <T : Any> addEventListener(clazz: Class<T>, handler: (T) -> Unit): Cancellable
	fun <T : Any> dispatch(event: T, clazz: Class<T> = event.javaClass)
}

interface Event

inline fun <reified T : Any> EventDispatcher.addEventListener(noinline handler: (T) -> Unit): Cancellable = this.addEventListener(T::class.java, handler)

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
