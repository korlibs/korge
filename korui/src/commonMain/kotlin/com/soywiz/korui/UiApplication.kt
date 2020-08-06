package com.soywiz.korui

class UiApplication(val factory: KoruiFactory = defaultKoruiFactory) {
    fun window(block: UiWindow.() -> Unit): UiWindow {
        return factory.createWindow().also(block).also { it.visible = true }
    }
}
