package com.soywiz.korio

import com.soywiz.korui.*
import com.soywiz.korui.layout.*

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

        UiApplication().window {
            //layout = UiLayout(this)
            title = "Hello"
            minimumSize = Size(600.pt)
            setBounds(16, 16, 600, 600)
            //vertical {
            scrollPanel {
                setBounds(0, 0, 200, 200)
                button("Hello World!") {
                    setBounds(16, 16, 320, 100)
                    height = 64.pt
                    onClick {
                        println("BUTTON")
                    }
                    //size = Size(width = 320.pt, height = 100.pt)
                }
            }
            //}
            label("Label") {
                onClick {
                    println("LABEL")
                }
                setBounds(16, 150, 320, 32)
            }
            textField("Hello") {
                setBounds(16, 300, 320, 32)
            }
            comboBox<String> {
                items = listOf("hello", "world")
                selectedItem = "world"
                setBounds(16, 350, 320, 32)
            }
        }
    }
}
