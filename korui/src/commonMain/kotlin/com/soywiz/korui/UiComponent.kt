package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.native.*

open class UiComponent(val app: UiApplication, val component: NativeUiFactory.NativeComponent) : Extra by Extra.Mixin() {
    val factory get() = app.factory
    var _parent: UiContainer? = null
        internal set

    var parent: UiContainer?
        get() = _parent
        set(value) {
            parent?.removeChild(this)
            value?.addChild(this)
        }
    var visible by RedirectMutableField(component::visible)
    var enabled by RedirectMutableField(component::enabled)
    var bounds by RedirectMutableField(component::bounds)

    open fun copyFrom(that: UiComponent) {
        this.visible = that.visible
        this.enabled = that.enabled
        this.bounds = that.bounds
    }

    fun onMouseEvent(block: (MouseEvent) -> Unit) {
        component.onMouseEvent(block)
    }
}

val UiComponent.root: UiComponent? get() {
    if (this.parent == null) return this as UiContainer
    return this.parent?.root
}

fun UiComponent.show() = run { visible = true }
fun UiComponent.hide() = run { visible = false }

fun UiComponent.onClick(block: (MouseEvent) -> Unit) {
    onMouseEvent {
        if (it.type == MouseEvent.Type.CLICK) {
            block(it)
        }
    }
}
