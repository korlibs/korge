package com.soywiz.korui

import com.soywiz.korim.bitmap.*

data class UiMenu(val children: List<UiMenuItem>) {
    constructor(vararg children: UiMenuItem) : this(children.toList())
}
data class UiMenuItem(val text: String, val children: List<UiMenuItem>? = null, val icon: Bitmap? = null, val action: () -> Unit) {
    constructor(text: String, children: List<UiMenuItem>? = null, icon: Bitmap? = null) : this(text, children, icon, {})
}
