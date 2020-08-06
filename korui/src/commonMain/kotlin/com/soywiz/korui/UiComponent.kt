package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*

interface UiComponent : Extra {
    val factory: UiFactory
    var bounds: RectangleInt
        get() = RectangleInt(0, 0, 0, 0)
        set(value) = Unit
    fun setBounds(x: Int, y: Int, width: Int, height: Int) = Unit
    var parent: UiContainer?
        get() = null
        set(value) {
            parent?.removeChild(this)
            value?.addChild(this)
        }
    var index: Int
        get() = -1
        set(value) = Unit
    var visible: Boolean
        get() = true
        set(value) = Unit
    var enabled: Boolean
        get() = true
        set(value) = Unit
    fun onMouseEvent(handler: (MouseEvent) -> Unit): Disposable = Disposable { }

    fun showPopupMenu(menu: List<UiMenuItem>, x: Int = Int.MIN_VALUE, y: Int = Int.MIN_VALUE) = Unit

    fun repaintAll()
}

val UiComponent.root: UiContainer? get() {
    if (this.parent == null) return this as UiContainer
    return this.parent?.root
}

fun UiComponent.show() = run { visible = true }
fun UiComponent.hide() = run { visible = false }

fun UiComponent.onClick(block: (MouseEvent) -> Unit) {
    onMouseEvent(block)
}
