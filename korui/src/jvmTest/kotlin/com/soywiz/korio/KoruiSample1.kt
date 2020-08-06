package com.soywiz.korio

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korio.async.*
import com.soywiz.korio.async.ObservableProperty
import com.soywiz.korui.*
import com.soywiz.korui.layout.*
import com.soywiz.korui.react.*
import javax.swing.*
import kotlin.properties.*
import kotlin.reflect.*

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

        UiApplication().window(600, 600) {
            layout = UiLayout(this)

            menu = UiMenu(listOf(UiMenuItem("hello", listOf(UiMenuItem("world"))), UiMenuItem("world")))
            /*
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
             */
            react {
                var checked by state { false }
                //var checked = true

                //val checked by ObservableProperty(false)
                //var checked: Boolean by ObservableProperty(false)
                //var checked2: Boolean by ObservableProperty(false)

                println("checked: $checked")

                //vertical {
                run {
                    checkBox("hello", checked = checked) {
                        //enabled = false

                        setBounds(16, 16, 320, 32)
                        onClick {
                            checked = !checked
                            //checked = false
                            //checked = true
                        }
                    }

                    button("save") {
                        //enabled = false
                        setBounds(16, 64, 320, 32)
                        onClick { }
                    }
                }
            }
            /*
            tree {
                setBounds(0, 0, 300, 300)
                lateinit var select: SimpleUiTreeNode
                root = SimpleUiTreeNode("hello", listOf(
                    SimpleUiTreeNode("world", listOf(
                        SimpleUiTreeNode("this"),
                        SimpleUiTreeNode("is").also { select = it },
                        SimpleUiTreeNode("a"),
                        SimpleUiTreeNode("test"),
                    ))
                ))
                //hide()
                onSelect {
                    println(it)
                }
                onClick {
                    if (it.button == MouseButton.RIGHT) {
                        showPopupMenu(listOf(UiMenuItem("hello") { }))
                    }
                }
                select(select)
            }

             */
        }
    }
}
