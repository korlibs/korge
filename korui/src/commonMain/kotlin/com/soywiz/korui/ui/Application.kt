package com.soywiz.korui.ui

import com.soywiz.korui.factory.*

class Application(
    val factory: KoruiFactory = defaultKoruiFactory
) {
    inline fun window(block: Window.() -> Unit): Window {
        return Window(this).also {
            block(it)
            it.show()
        }
    }
}
