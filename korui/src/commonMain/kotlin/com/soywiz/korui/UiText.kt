package com.soywiz.korui

interface UiText : UiContainer {
    var text: String
        get() = ""
        set(value) = Unit
}
