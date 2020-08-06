package com.soywiz.korui.native.util

import com.soywiz.korui.*
import com.soywiz.korui.native.*
import javax.swing.*

fun UiMenuItem.toMenuItem(factory: BaseAwtUiFactory): JMenuItem {
    val item = factory.createJMenuItem()
    item.text = this.text
    item.icon = this.icon?.toAwtIcon()
    if (this.children != null) {
        for (child in this.children!!) {
            item.add(child.toMenuItem(factory))
        }
    }
    return item
}

fun UiMenuItem.toMenu(factory: BaseAwtUiFactory): JMenu {
    val item = factory.createJMenu()
    item.text = this.text
    if (this.children != null) {
        for (child in this.children!!) {
            item.add(child.toMenuItem(factory))
        }
    }
    return item
}

fun UiMenu.toJMenuBar(factory: BaseAwtUiFactory): JMenuBar {
    val bar = factory.createJMenuBar()
    for (child in children) {
        bar.add(child.toMenu(factory))
    }
    return bar
}
