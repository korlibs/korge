package com.soywiz.korio

import com.soywiz.korev.*
import com.soywiz.korma.geom.*
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

        UiApplication().window(600, 600) {
            layout = VerticalUiLayout

            menu = UiMenu(listOf(UiMenuItem("hello", listOf(UiMenuItem("world"))), UiMenuItem("world")))
            focusable = true
            layoutChildrenPadding = 8
            //var checked by state { false }
            var checked = true

            //val checked by ObservableProperty(false)
            //var checked: Boolean by ObservableProperty(false)
            //var checked2: Boolean by ObservableProperty(false)

            println("checked: $checked")

            onClick {
                focus()
            }

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
                addChild(MyCustomComponent(app, MyEditableNumber(app)).apply {
                })
                addChild(MyCustomComponent(app, MyEditableComboBox(app, listOf("hello", "world"))).apply {
                })
            }
        }
    }
}

open class MyEditableComponent(app: UiApplication) : UiContainer(app) {
    open fun hideEditor() {
    }

    open fun showEditor() {
    }
}

class MyEditableComboBox<T>(app: UiApplication, items: List<T>) : MyEditableComponent(app) {
    init {
        layout = UiFillLayout
        visible = true
    }

    val contentText = UiLabel(app).also { it.text = "world" }.also { it.visible = true }
    val contentComboBox = UiComboBox<T>(app).also { it.items =items }.also { it.visible = false }

    override fun hideEditor() {
        contentText.visible = true
        contentComboBox.visible = false
    }

    override fun showEditor() {
        contentText.visible = false
        contentComboBox.visible = true
        contentComboBox.focus()
    }

    init {
        contentText.onClick {
            showEditor()
        }

        contentComboBox.onChange {
            hideEditor()
        }
        contentComboBox.onFocus { e ->
            if (e.typeBlur) {
                hideEditor()
            } else {
                contentComboBox.open()
            }
            //println(e)
        }
        addChild(contentText)
        addChild(contentComboBox)
    }
}

class MyEditableNumber(app: UiApplication) : MyEditableComponent(app) {
    init {
        layout = UiFillLayout
        visible = true
    }

    val contentText = UiLabel(app).also { it.text = "world" }.also { it.visible = true }
    val contentTextField = UiTextField(app).also { it.text = contentText.text }.also { it.visible = false }

    override fun hideEditor() {
        contentText.visible = true
        contentTextField.visible = false
        contentText.text = contentTextField.text
    }

    override fun showEditor() {
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

class MyCustomComponent(app: UiApplication, val editor: MyEditableComponent) : UiContainer(app) {
    init {
        //this.bounds = RectangleInt(0, 0, 240, 32)
        layout = HorizontalUiLayout
    }
    val label = UiLabel(app).apply {
        text = "hello"
        width = 50.percent
    }
    init {
        editor.width = 50.percent
        //backgroundColor = Colors.RED
        addChild(label)
        addChild(editor)
        label.onClick {
            editor.hideEditor()
        }
        //addChild(UiLabel(app).also { it.text = "text" }.also { it.bounds = RectangleInt(120, 0, 120, 32) })
    }
}
