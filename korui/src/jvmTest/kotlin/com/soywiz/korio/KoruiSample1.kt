package com.soywiz.korio

import com.soywiz.korui.ui.*

object KoruiSample1 {
    @JvmStatic
    fun main(args: Array<String>) {
        /*
        val window = defaultKoruiFactory.createWindow()
        val button = defaultKoruiFactory.createButton()
        defaultKoruiFactory.setBounds(window, 16, 16, 600, 600)
        defaultKoruiFactory.setBounds(button, 16, 16, 320, 100)
        defaultKoruiFactory.setText(button, "hello")
        defaultKoruiFactory.setParent(button, window)
        defaultKoruiFactory.setVisible(window, true)
        */

        val app = Application()
        app.window {
            title = "Hello"
            setBounds(16, 16, 600, 600)
            button("Hello World!") {
                setBounds(16, 16, 320, 100)
            }
        }
    }
}
