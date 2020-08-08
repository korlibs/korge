package com.soywiz.korge.intellij.editor

import com.intellij.codeInsight.editorActions.*
import com.intellij.ide.actions.*
import com.intellij.openapi.ide.*
import com.intellij.openapi.ui.*
import com.soywiz.korgw.awt.*
import javax.swing.*

class GLCanvasGameWindowIJ(canvas: GLCanvas) : GLCanvasGameWindow(canvas) {
    override fun showContextMenu(items: List<MenuItem?>) {
        val popupMenu = JBPopupMenu()
        for (item in items) {
            if (item?.text == null) {
                popupMenu.add(JSeparator())
            } else {
                popupMenu.add(JBMenuItem(item.text).also {
                    it.isEnabled = item.enabled
                    it.addActionListener {
                        item.action()
                    }
                })
            }
        }
        popupMenu.show(contentComponent, mouseX, mouseY)
    }
}
