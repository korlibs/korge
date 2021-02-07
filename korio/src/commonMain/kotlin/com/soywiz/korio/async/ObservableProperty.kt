package com.soywiz.korio.async

import com.soywiz.kds.iterators.fastForEach
import kotlin.reflect.*

class ObservableProperty<T>(initial: T) {
    private var observers = ArrayList<(T) -> Unit>()

    private var _value: T = initial

    var value: T
        get() = _value
        set(v) {
            update(v)
        }
    val observerCount: Int get() = observers.size
    fun clear() = observers.clear()

    fun observe(handler: (T) -> Unit): ObservableProperty<T> {
        observers.add(handler)
        return this
    }
    fun observeStart(handler: (T) -> Unit): ObservableProperty<T> {
        observe(handler)
        handler(value)
        return this
    }
    operator fun invoke(handler: (T) -> Unit) = observe(handler)

    fun bind(prop: KMutableProperty0<T>) {
        observe { prop.set(value) }
        prop.set(value)
    }

    fun update(value: T) {
        this._value = value
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

fun <T> ObservableProperty(prop: KMutableProperty0<T>): ObservableProperty<T> {
    return ObservableProperty(prop.get()).observeStart { prop.set(it) }
}
