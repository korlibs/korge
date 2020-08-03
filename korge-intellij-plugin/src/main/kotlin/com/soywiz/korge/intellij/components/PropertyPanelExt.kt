package com.soywiz.korge.intellij.components

import com.intellij.openapi.ui.*
import com.intellij.ui.components.*
import com.soywiz.korge.awt.*
import java.awt.*
import java.util.*
import javax.swing.*

fun initializeIdeaComponentFactory() {
    myComponentFactory = IdeaMyComponentFactory
}

object IdeaMyComponentFactory : MyComponentFactory() {
    override fun <T> list(array: List<T>) = JBList(Vector(array))
    override fun scrollPane(view: Component): JScrollPane =
        JBScrollPane(view, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
    override fun <T> comboBox(array: Array<T>): JComboBox<T> = ComboBox<T>(array)

    override fun tabbedPane(tabPlacement: Int, tabLayoutPolicy: Int): JTabbedPane {
        return JBTabbedPane(tabPlacement, tabLayoutPolicy)
    }
}
