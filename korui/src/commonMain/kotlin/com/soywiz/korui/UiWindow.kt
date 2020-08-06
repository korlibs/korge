package com.soywiz.korui

import com.soywiz.korev.*
import com.soywiz.korio.lang.*

interface UiWindow : UiContainer {
    var title: String
        get() = ""
        set(value) = Unit
    fun onResize(handler: (ReshapeEvent) -> Unit): Disposable = Disposable { }
}
