package com.soywiz.korui

data class UiMenu(val children: List<UiMenuItem>) {
    constructor(vararg children: UiMenuItem) : this(children.toList())
}
data class UiMenuItem(val text: String, val children: List<UiMenuItem>? = null, val action: () -> Unit) {
    constructor(text: String, children: List<UiMenuItem>? = null) : this(text, children, {})
}
