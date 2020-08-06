package com.soywiz.korui

interface UiWithText : UiContainer {
    var text: String
        get() = ""
        set(value) = Unit
    var checked: Boolean
        get() = false
        set(value) = Unit
}
