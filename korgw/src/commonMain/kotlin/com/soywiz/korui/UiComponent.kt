package com.soywiz.korui

import com.soywiz.kds.Extra
import com.soywiz.korev.FocusEvent
import com.soywiz.korev.MouseEvent
import com.soywiz.korev.ReshapeEvent
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.Disposable
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korui.layout.LengthExtensions
import com.soywiz.korui.native.NativeUiFactory

var NativeUiFactory.NativeComponent.uiComponent by Extra.PropertyThis<NativeUiFactory.NativeComponent, UiComponent?> { null }

open class UiComponent(val app: UiApplication, val component: NativeUiFactory.NativeComponent) : Extra by Extra.Mixin(), LengthExtensions {
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


