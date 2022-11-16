package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.layout.*
import com.soywiz.korui.native.*

internal var NativeUiFactory.NativeComponent.uiComponent by Extra.PropertyThis<NativeUiFactory.NativeComponent, UiComponent?> { null }

internal open class UiComponent(val app: UiApplication, val component: NativeUiFactory.NativeComponent) : Extra by Extra.Mixin(), LengthExtensions {
    init {
        component.uiComponent = this
    }
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
    var enabled by component::enabled
    open var bounds: RectangleInt
        get() = component.bounds
        set(value) {
            component.bounds = value
        }
    var cursor by component::cursor
    var focusable by component::focusable

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
    fun openFileDialog(file: VfsFile?, filter: (VfsFile) -> Boolean) = component.openFileDialog(file, filter)
    fun openColorPickerDialog(color: RGBA, listener: ((RGBA) -> Unit)?): RGBA? = component.openColorPickerDialog(color, listener)
    fun focus(focus: Boolean = true) = component.focus(focus)
    fun show() { visible = true }
    fun hide() { visible = false }
    fun onClick(block: (MouseEvent) -> Unit) = onMouseEvent { if (it.typeClick) block(it) }
    fun onResize(handler: (ReshapeEvent) -> Unit): Disposable = component.onResize(handler)

    fun repaintAll() = component.repaintAll()
    open fun updateUI() = component.updateUI()
}


