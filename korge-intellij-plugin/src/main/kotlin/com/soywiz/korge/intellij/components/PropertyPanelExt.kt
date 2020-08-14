package com.soywiz.korge.intellij.components

import com.esotericsoftware.spine.korge.*
import com.intellij.openapi.fileChooser.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.soywiz.kds.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.korui.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korui.native.*
import java.awt.*
import java.util.*
import javax.swing.*

// com.soywiz.korge.intellij.components.initializeIdeaComponentFactory()

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

    override fun chooseFile(views: Views?, filter: (VfsFile) -> Boolean): VfsFile? {
        val file = FileChooser.chooseFile(
            FileChooserDescriptor(true, false, false, false, false, false).also { fcd ->
                fcd.withFileFilter { filter(it.toVfs()) }
            },
            views?.ideaProject,
            null
        ) ?: return null
        return localVfs(file.canonicalPath!!)
    }

    override fun chooseColor(value: RGBA, views: Views?): RGBA? {
        return ColorChooser.chooseColor(views!!.ideaComponent, "Choose Color", value.toAwt(), true, true)?.toRgba()
    }

    override fun createModule(block: suspend Scene.() -> Unit): Module {
        return com.soywiz.korge.intellij.editor.createModule(null) { block() }
    }

    override fun getViewFactories(views: Views): List<ViewFactory> {
        return super.getViewFactories(views) + listOf(
            ViewFactory("Dragonbones") { KorgeDbRef() },
            ViewFactory("Spine") { SpineViewRef() },
        )
    }

    override fun createPopupMenu() = JBPopupMenu()
    override fun createSeparator() = JSeparator()
    override fun createMenuItem(text: String, mnemonic: Int?, icon: Icon?) = when {
        icon != null -> JBMenuItem(text, icon)
        else -> JBMenuItem(text)
    }
}

var Views.ideaProject: Project? by Extra.PropertyThis<Views, Project?> { null }
var Views.ideaComponent: Component? by Extra.PropertyThis<Views, Component?> { null }
