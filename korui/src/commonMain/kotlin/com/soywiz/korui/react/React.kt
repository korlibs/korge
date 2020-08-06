package com.soywiz.korui.react

import com.soywiz.kds.*
import com.soywiz.korio.async.*
import com.soywiz.korui.*
import kotlin.reflect.*

interface UiContainerWithReactState {
    fun <T> state(initial: () -> T): ReactState<T>
}

class ReactUiContainer(
    val container: UiContainer,
    val container2: UiContainer,
    val gen: UiContainerWithReactState.() -> Unit
) : UiContainer, Extra by Extra.Mixin(), UiContainerWithReactState {
    class ReactUiFactory : UiFactory {
    }

    override val factory: UiFactory = ReactUiFactory()

    init {
        gen(this)
    }

    override fun <T> state(initial: () -> T): ReactState<T> {
        return ReactState<T>(this, initial)
    }
}

class ReactState<T>(val container: ReactUiContainer, val initial: () -> T) {
    operator fun getValue(t: Any?, property: KProperty<*>): T {
        return initial()
    }

    operator fun setValue(t: Any?, property: KProperty<*>, any: T) {
    }

}

fun UiContainer.react(gen: UiContainerWithReactState.() -> Unit): UiContainer {
    return container {
        ReactUiContainer(this@react, this@container, gen)
    }
}
