package com.soywiz.korio.async

import com.soywiz.kds.iterators.fastForEach

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
}
