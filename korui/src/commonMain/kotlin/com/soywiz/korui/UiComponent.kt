package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korio.lang.*

interface UiComponent : Extra {
    val factory: KoruiFactory
    fun setBounds(x: Int, y: Int, width: Int, height: Int) = Unit
    fun setParent(p: UiContainer?) = Unit
    var index: Int
        get() = -1
        set(value) = Unit
    var visible: Boolean
        get() = false
        set(value) = Unit
    fun addMouseEventListener(handler: (MouseEvent) -> Unit): Disposable = Disposable { }
}

fun UiComponent.onClick(block: (MouseEvent) -> Unit) {
    addMouseEventListener(block)
}
