package com.soywiz.korio.async

import com.soywiz.kds.iterators.fastForEach
import kotlin.reflect.*

class ObservableProperty<T>(initial: T) {
    private var observers = ArrayList<(T) -> Unit>()

    var value: T = initial; private set
    val observerCount: Int get() = observers.size
    fun clear() = observers.clear()

    fun observe(handler: (T) -> Unit) {
        observers.add(handler)
    }
    operator fun invoke(handler: (T) -> Unit) = observe(handler)

    fun update(value: T) {
        this.value = value
        observers.fastForEach { it(value) }
    }
    operator fun invoke(value: T) = update(value)

    operator fun getValue(any: Any?, property: KProperty<*>): T {
        return this.value
    }

    operator fun setValue(any: Any?, property: KProperty<*>, value: T) {
        this.update(value)
    }

    companion object {
        fun <T> synchronize(src: KMutableProperty0<T>, dst: KMutableProperty0<T>) {
            //src.isInitialized
            TODO()
        }
    }
}
