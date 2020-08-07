package com.soywiz.korge.debug

import com.soywiz.korio.async.*
import com.soywiz.korui.*

class ObservableProperty<T>(
    val name: String,
    val internalSet: (T) -> Unit,
    val internalGet: () -> T,
) {
    val onChange = Signal<T>()

    fun forceUpdate(value: T) {
        internalSet(value)
        onChange(value)
    }

    fun forceRefresh() {
        //println("forceRefresh: $value")
        //forceUpdate(value)
        onChange(value)
    }

    var value: T
        get() = internalGet()
        set(value) {
            if (this.value != value) {
                forceUpdate(value)
            }
        }

    override fun toString(): String = "ObservableProperty($name, $value)"
}

interface ObservablePropertyHolder<T> {
    val prop: ObservableProperty<T>
}

fun UiComponent.findObservableProperties(out: ArrayList<ObservableProperty<*>> = arrayListOf()): List<ObservableProperty<*>> {
    if (this is ObservablePropertyHolder<*>) {
        out.add(prop)
    }
    if (this is UiContainer) {
        forEachChild {
            it.findObservableProperties(out)
        }
    }
    return out
}
