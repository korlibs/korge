package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.korge.annotations.*
import com.soywiz.korio.async.*
import kotlin.reflect.*

@KorgeExperimental
class UIObservablePropertyList {
    val props = FastArrayList<UIObservableProperty<Any>>()

    fun register(list: List<UIObservableProperty<*>>) {
        props.addAll(list as List<UIObservableProperty<Any>>)
    }

    fun sync() {
        for (prop in props) {
            if (prop.getDisplayValue() != prop.value) {
                prop.onChanged(prop, prop.value)
            }
        }
    }
}

@KorgeExperimental
open class UIObservableProperty<T>(
    val prop: UIProperty<T>,
) {
    val onChanged = Signal2<UIObservableProperty<T>, T>()

    fun setValue(value: T, notify: Boolean = true) {
        if (value != prop.get()) {
            prop.set(value)
            if (notify) onChanged(this, value)
        }
    }

    val initialValue = prop.get()
    var value: T
        get() = prop.get()
        set(value) {
            setValue(value, notify = true)
        }

    var getDisplayValue: () -> T = { value }
}

@KorgeExperimental
class UIPropertyPlain<T>(var value: T) : UIProperty<T> {
    override fun set(value: T) { this.value = value }
    override fun get(): T = this.value
}

@KorgeExperimental
interface UIProperty<T> {
    fun set(value: T)
    fun get(): T
}

@KorgeExperimental
fun <T> UIProperty(set: (T) -> Unit, get: () -> T): UIProperty<T> = object : UIProperty<T> {
    override fun set(value: T) = set(value)
    override fun get(): T = get()
}

@KorgeExperimental
fun <T> KMutableProperty0<T>.toUI() = UIProperty(set = { this@toUI.set(it) }, get = { this@toUI.get() })
