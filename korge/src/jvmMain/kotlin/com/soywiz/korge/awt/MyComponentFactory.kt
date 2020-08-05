package com.soywiz.korge.awt

import com.soywiz.korge.debug.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korinject.*
import com.soywiz.korio.file.*
import java.awt.*
import java.util.*
import javax.swing.*

open class MyComponentFactory {
    open fun <T> list(array: List<T>) = JList(Vector(array))

    open fun scrollPane(view: Component): JScrollPane =
            JScrollPane(view, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)

    open fun <T> comboBox(array: Array<T>): JComboBox<T> = JComboBox<T>()

    open fun tabbedPane(tabPlacement: Int, tabLayoutPolicy: Int): JTabbedPane = JTabbedPane(tabPlacement, tabLayoutPolicy)

    open fun chooseFile(views: Views?, filter: (VfsFile) -> Boolean): VfsFile? {
        TODO()
    }

    open fun chooseColor(value: RGBA, views: Views?): RGBA? {
        TODO()
    }

    open fun createModule(block: suspend Scene.() -> Unit): EditorModule {
        TODO()
    }
}
