package com.soywiz.korui.react

import com.soywiz.kds.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import kotlin.reflect.*

fun UiContainer.react(gen: UiContainerWithReactState.() -> Unit): UiContainer {
    return container {
        ReactUiMyContainer(this@container, gen)
    }
}

interface UiContainerWithReactState : UiContainer {
    fun <T> state(initial: () -> T): ReactState<T>
}

class ReactUiMyContainer(
    val container: UiContainer,
    val gen: UiContainerWithReactState.() -> Unit
) {
    val changed = Signal<Unit>()
    val data = LinkedHashMap<String, Any?>()
    val holder = BaseUiContainerWithReactState(this, container)

    init {
        holder.removeChildren()
        gen(holder)
        changed.add {
            //println("CHANGED STATE!")
            holder.removeChildren()
            gen(holder)
            //holder.root?.repaintAll()
        }
        //gen(holder1)
    }
}

class BaseUiContainerWithReactState(val states: ReactUiMyContainer, val container: UiContainer) : UiContainerWithReactState, UiContainer by container {
    override fun <T> state(initial: () -> T): ReactState<T> = ReactState(states, initial)
}

class ReactState<T>(val states: ReactUiMyContainer, val initial: () -> T) {
    operator fun getValue(obj: Any?, property: KProperty<*>): T = (states.data as MutableMap<String, T>).getOrPut(property.name) { initial() }
    operator fun setValue(obj: Any?, property: KProperty<*>, value: T) = run {
        if (value != getValue(obj, property)) {
            states.data[property.name] = value
            states.changed()
        }
    }
}
