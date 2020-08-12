package com.soywiz.korge.intellij.korui

import com.intellij.openapi.fileChooser.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korui.*
import com.soywiz.korui.native.*
import java.awt.*
import javax.swing.*

open class IdeaUiApplication(val project: Project, views: Views) : UiApplication(IdeaNativeUiFactory(project)) {
    init {
        this.views = views
    }
}

open class IdeaNativeUiFactory(val project: Project) : BaseAwtUiFactory() {
    override fun createJPopupMenu(): JPopupMenu = JBPopupMenu()
    override fun createJScrollPane(): JScrollPane = JBScrollPane()

    override fun awtOpenFileDialog(component: Component, file: VfsFile?, filter: (VfsFile) -> Boolean): VfsFile? {
        val file = FileChooser.chooseFile(
            FileChooserDescriptor(true, false, false, false, false, false).also { fcd ->
                fcd.withFileFilter { filter(it.toVfs()) }
            },
            project,
            null
        ) ?: return null
        return localVfs(file.canonicalPath!!)
    }

    override fun awtOpenColorPickerDialog(component: Component, color: RGBA, listener: ((RGBA) -> Unit)?): RGBA? {
        return ColorChooser.chooseColor(component, "Choose Color", color.toAwt(), true, mutableListOf(object : ColorPickerListener {
            override fun colorChanged(color: Color?) {
                if (color != null) {
                    listener?.invoke(color.toRgba())
                }
            }

            override fun closed(color: Color?) {
            }
        }), true)?.toRgba()
    }
}

