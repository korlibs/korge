package com.soywiz.korio

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.async.ObservableProperty
import com.soywiz.korma.geom.*
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
            layout = null

            menu = UiMenu(listOf(UiMenuItem("hello", listOf(UiMenuItem("world"))), UiMenuItem("world")))
            focusable = true
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
            //react {
                //var checked by state { false }
                var checked = true

                //val checked by ObservableProperty(false)
                //var checked: Boolean by ObservableProperty(false)
                //var checked2: Boolean by ObservableProperty(false)

                println("checked: $checked")

                //vertical {
                run {
                    checkBox("hello", checked = checked) {
                        //enabled = false

                        bounds = RectangleInt(16, 16, 320, 32)
                        onClick {
                            checked = !checked
                            //checked = false
                            //checked = true
                        }
                    }

                    button("save") {
                        //enabled = false
                        bounds = RectangleInt(16, 64, 320, 32)
                        onClick {
                            this.showPopupMenu(listOf(UiMenuItem("HELLO")))
                        }
                    }
                    addChild(MyCustomComponent(app).apply {
                        bounds = RectangleInt(0, 100, 240, 32)
                    })
                }
            //}
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

class MyEditableNumber(app: UiApplication) : UiContainer(app) {
    init {
        layout = UiFillLayout
        visible = true
    }

    val contentText = UiLabel(app).also { it.text = "world" }.also { it.visible = true }
    val contentTextField = UiTextField(app).also { it.text = contentText.text }.also { it.visible = false }

    fun hideEditor() {
        contentText.visible = true
        contentTextField.visible = false
        contentText.text = contentTextField.text
    }

    fun showEditor() {
        contentTextField.text = contentText.text
        contentText.visible = false
        contentTextField.visible = true
        contentTextField.select()
        contentTextField.focus()
    }

    init {
        contentText.onClick {
            showEditor()
        }
        contentTextField.onKeyEvent { e ->
            if (e.typeDown && e.key == Key.RETURN) {
                hideEditor()
            }
            //println(e)
        }
        contentTextField.onFocus { e ->
            if (e.typeBlur) {
                hideEditor()
            }
            //println(e)
        }
        var startX = 0
        var startY = 0
        contentText.onMouseEvent { e ->
            if (e.typeDown) {
                startX = e.x
                startY = e.y
                e.requestLock()
            }
            if (e.typeDrag) {
                val dx = e.x - startX
                val dy = e.y - startY
                contentText.text = "$dx"
                contentTextField.text = "$dx"
            }
        }
        contentText.cursor = UiStandardCursor.RESIZE_EAST
        addChild(contentText)
        addChild(contentTextField)
    }
}

class MyCustomComponent(app: UiApplication) : UiContainer(app) {
    init {
        //this.bounds = RectangleInt(0, 0, 240, 32)
        layout = null
    }
    val label = UiLabel(app).also { it.text = "hello" }.also { it.bounds = RectangleInt(0, 0, 120, 32) }
    val editor = MyEditableNumber(app).also {  }.also { it.bounds = RectangleInt(120, 0, 120, 32) }
    init {
        //backgroundColor = Colors.RED
        addChild(label)
        addChild(editor)
        label.onClick {
            editor.hideEditor()
        }
        //addChild(UiLabel(app).also { it.text = "text" }.also { it.bounds = RectangleInt(120, 0, 120, 32) })
    }
}
