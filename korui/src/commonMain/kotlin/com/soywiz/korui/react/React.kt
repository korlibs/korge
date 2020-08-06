package com.soywiz.korui.react

import com.soywiz.kds.*
import com.soywiz.korio.async.*
import com.soywiz.korui.*
import com.soywiz.korui.layout.*
import kotlin.reflect.*

var UiComponent.reactUid by Extra.PropertyThis<UiComponent, Any?> { null }

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
    val holderTemp = BaseUiContainerWithReactState(this, container.factory.createContainer().also {
        it.layout = container.layout
        it.bounds = container.bounds
    })
    var currentUids = ReactUids()

    init {
        holder.removeChildren()
        gen(holder)
        currentUids.generate(holder.container)

        changed.add {
            //println("CHANGED STATE!")
            holderTemp.removeChildren()
            gen(holderTemp)
            val tempUids = ReactUids()
            tempUids.generate(holderTemp.container)
            sync(holder.container, currentUids, holderTemp.container, tempUids)
        }
    }

    private fun sync(current: UiContainer, cuids: ReactUids, next: UiContainer, nuids: ReactUids) {
        val minSize = kotlin.math.min(current.size, next.size)
        for (n in 0 until minSize) {
            val cchild = current[n]
            val nchild = next[n]
            if (cchild.reactUid == nchild.reactUid) {
                cchild.copyFrom(nchild)
                if (cchild is UiContainer) {
                    sync(cchild, cuids, nchild as UiContainer, nuids)
                }
            } else {
                current.replaceChildAt(n, nchild)
            }
        }
        for (n in minSize until next.size) {
            current.addChild(next.getChildAt(n))
        }
    }
}

class ReactUids {
    val uidToComponent = LinkedHashMap<Any?, UiComponent>()
    var lastUid = 0
    fun generate(component: UiComponent): ReactUids {
        //println("GENERATE")
        generateInternal(component)
        return this
    }
    fun generateInternal(component: UiComponent) {
        if (component.reactUid == null) {
            component.reactUid = component::class.simpleName + ":" + (lastUid++)
            //println(" - ${component.reactUid}")
        } else {
            lastUid++
        }
        uidToComponent[component.reactUid] = component
        if (component is UiContainer) {
            component.forEachChild {
                generateInternal(it)
            }
        }
    }
}

class BaseUiContainerWithReactState(val states: ReactUiMyContainer, val container: UiContainer) : UiContainerWithReactState, UiContainer by container {
    override fun <T> state(initial: () -> T): ReactState<T> = ReactState(states, initial)
}

class ReactState<T>(val states: ReactUiMyContainer, val initial: () -> T) {
    operator fun getValue(obj: Any?, property: KProperty<*>): T = (states.data as MutableMap<String, T>).getOrPut(property.name) { initial() }
    operator fun setValue(obj: Any?, property: KProperty<*>, value: T) = run {
        //if (value != getValue(obj, property)) {
        run {
            states.data[property.name] = value
            states.changed()
        }
    }
}
