package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korim.color.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.native.*

open class UiComponent(val app: UiApplication, val component: NativeUiFactory.NativeComponent) : Extra by Extra.Mixin() {
    val factory get() = app.factory
    var _parent: UiContainer? = null
        internal set

    val root: UiContainer? get() {
        if (this.parent == null) return this as? UiContainer?
        return this.parent?.root
    }

    var parent: UiContainer?
        get() = _parent
        set(value) {
            parent?.removeChild(this)
            value?.addChild(this)
        }
    var visible: Boolean
        get() = component.visible
        set(value) {
            component.visible = value
            root?.relayout()
        }
    var enabled by redirect(component::enabled)
    open var bounds: RectangleInt
        get() = component.bounds
        set(value) {
            component.bounds = value
        }
    var cursor by redirect(component::cursor)
    var focusable by redirect(component::focusable)

    open fun copyFrom(that: UiComponent) {
        this.visible = that.visible
        this.enabled = that.enabled
        this.bounds = that.bounds
        this.cursor = that.cursor
        this.focusable = that.focusable
    }

    fun onMouseEvent(block: (MouseEvent) -> Unit) = component.onMouseEvent(block)
    fun onFocus(block: (FocusEvent) -> Unit) = component.onFocus(block)
    fun showPopupMenu(menu: List<UiMenuItem>, x: Int = Int.MIN_VALUE, y: Int = Int.MIN_VALUE) = component.showPopupMenu(menu, x, y)
    fun focus(focus: Boolean = true) = component.focus(focus)
    fun show() = run { visible = true }
    fun hide() = run { visible = false }
    fun onClick(block: (MouseEvent) -> Unit) = onMouseEvent { if (it.typeClick) block(it) }
}


