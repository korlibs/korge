package com.soywiz.korui.native

import com.soywiz.korui.*
import javax.swing.*

fun UiMenuItem.toMenuItem(): JMenuItem {
    val item = JMenuItem(this.text)
    if (this.children != null) {
        for (child in this.children!!) {
            item.add(child.toMenuItem())
        }
    }
    return item
}

fun UiMenuItem.toMenu(): JMenu {
    val item = JMenu(this.text)
    if (this.children != null) {
        for (child in this.children!!) {
            item.add(child.toMenuItem())
        }
    }
    return item
}


fun UiMenu.toJMenuBar(): JMenuBar {
    val bar = JMenuBar()
    for (child in children) {
        bar.add(child.toMenu())
    }
    return bar
}
