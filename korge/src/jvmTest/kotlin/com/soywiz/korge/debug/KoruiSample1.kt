package com.soywiz.korge.debug

import com.soywiz.korma.geom.*
import com.soywiz.korui.*

object KoruiSample2 {
    @JvmStatic
    fun main(args: Array<String>) {
        val window = DEFAULT_UI_FACTORY.createWindow()
        val scrollPanel = DEFAULT_UI_FACTORY.createScrollPanel()
        val button = DEFAULT_UI_FACTORY.createButton()
        button.bounds = RectangleInt(0, 0, 400, 250)
        scrollPanel.insertChildAt(-1, button)
        scrollPanel.bounds = RectangleInt(0, 0, 300, 300)
        window.insertChildAt(-1, scrollPanel)
        //window.insertChildAt(-1, button)
        window.bounds = RectangleInt(0, 0, 600, 600)
        window.visible = true
    }
}

/*
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

        UiApplication(DEFAULT_UI_FACTORY).window(600, 600) {
            val crossIcon = NativeImage(16, 16).context2d {
                stroke(Colors.RED, lineWidth = 3.0) {
                    line(0, 0, 16 - 1.5, 16 - 1.5)
                    line(16 - 1.5, 0, 0, 16 - 1.5)
                }
            }

            menu = UiMenu(listOf(UiMenuItem("hello", listOf(UiMenuItem("world", icon = crossIcon))), UiMenuItem("world")))

            layout = UiFillLayout
            scrollPanel(xbar = false, ybar = true) {
            //run {
                layout = VerticalUiLayout

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
                    //toolbar {
                    //    button("1") { }
                    //    button("2") { }
                    //    button("3") { }
                    //}
                    canvas(NativeImage(128, 128).context2d {
                        stroke(Colors.WHITE, lineWidth = 3.0) {
                            line(0, 0, 128 - 1.5, 128 - 1.5)
                            line(128 - 1.5, 0, 0, 128 - 1.5)
                        }
                    }) {

                    }
                    checkBox("hello", checked = checked) {
                        //enabled = false

                        bounds = RectangleInt(16, 16, 320, 32)
                        onClick {
                            checked = !checked
                            //checked = false
                            //checked = true
                        }
                    }

                    button("save", {
                        bounds = RectangleInt(16, 64, 320, 32)
                    }) {
                        this.showPopupMenu(listOf(UiMenuItem("HELLO", icon = crossIcon)))
                    }
                    lateinit var props: UiContainer
                    button("Properties") {
                        props.visible = !props.visible
                    }
                    props = vertical {
                        addChild(UiRowEditableValue(app, "position", UiTwoItemEditableValue(app, UiNumberEditableValue(app, 0.0, -1000.0, +1000.0, decimalPlaces = 0), UiNumberEditableValue(app, 0.0, -1000.0, +1000.0))))
                        addChild(UiRowEditableValue(app, "ratio", UiNumberEditableValue(app, 0.0, min = 0.0, max = 1.0, clampMin = true, clampMax = true)))
                        addChild(UiRowEditableValue(app, "y", UiListEditableValue(app, listOf("hello", "world"), "world")))
                    }
                    tree {
                        minimumSize = Size(32.pt, 128.pt)
                        nodeRoot = SimpleUiTreeNode("hello",
                            (0 until 40).map { SimpleUiTreeNode("world$it") }
                        )
                    }
                    lateinit var vert: UiContainer
                    label("DEMO") {
                        icon = crossIcon
                        onClick {
                            vert.visible = !vert.visible
                            icon = if (vert.visible) crossIcon else null
                        }
                    }
                    vert = vertical {
                        label("HELLO") {}
                        label("WORLD") {}
                        button("test")
                        label("LOL") {}
                    }
                    button("TEST") {

                    }
                }
            }
        }
    }
}
*/
