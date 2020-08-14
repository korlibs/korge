package com.soywiz.korge.intellij.editor

import com.intellij.ui.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.ui.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korge.intellij.util.ObservableProperty
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import java.awt.*
import java.awt.event.*
import javax.swing.event.*

data class KorgeBaseEditorSettings(
    val width: Int,
    val height: Int,
    val gridWidth: Int,
    val gridHeight: Int
)

fun showKorgeBaseEditorSettings(settings: KorgeBaseEditorSettings): KorgeBaseEditorSettings? {
    val width = ObservableProperty(settings.width)
    val height = ObservableProperty(settings.height)
    val gridWidth = ObservableProperty(settings.gridWidth)
    val gridHeight = ObservableProperty(settings.gridHeight)
    val initializedSignal = Signal<Unit>()

    fun Styled<out Container>.row(name: String, prop: ObservableProperty<Int>, focus: Boolean = false) {
        horizontalStack {
            this.height = 32.pt
            label("$name:") {
                this.width = 50.percentage
                this.height = 32.pt
            }
            textField("${prop.value}") {
                this.width = 50.percentage
                this.height = 32.pt
                component.addFocusListener(object : FocusAdapter() {
                    override fun focusGained(e: FocusEvent?) {
                        component.selectAll()
                    }
                })
                component.document.addDocumentListener(object : DocumentAdapter() {
                    override fun textChanged(e: DocumentEvent) {
                        runCatching {
                            prop.value = component.text.trim().toInt()
                        }
                    }
                })
                if (focus) {
                    initializedSignal {
                        component.grabFocus()
                    }
                }
            }
        }
    }

    val result = showDialog("Settings") {
        verticalStack {
            row("Width", width, focus = true)
            row("Height", height)
            row("Grid Width", gridWidth)
            row("Grid Height", gridHeight)
        }
        initializedSignal(Unit)
    }

    return when {
        result -> KorgeBaseEditorSettings(
            width = width.value,
            height = height.value,
            gridWidth = gridWidth.value,
            gridHeight = gridHeight.value,
        )
        else -> null
    }
}
