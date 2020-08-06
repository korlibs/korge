package com.soywiz.korui

interface UiWindow : UiContainer {
    var title: String
        get() = ""
        set(value) = Unit
}
