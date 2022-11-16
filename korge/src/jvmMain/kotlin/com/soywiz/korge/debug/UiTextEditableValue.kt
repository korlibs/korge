package com.soywiz.korge.debug

import com.soywiz.korev.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.util.*
import com.soywiz.korui.*
import com.soywiz.korui.layout.*

class UiTextEditableValue(
    app: UiApplication,
    prop: ObservableProperty<String>,
    val kind: Kind
) : UiEditableValue<String>(app, prop), ObservablePropertyHolder<String> {
    open class Kind {
        object STRING : Kind()
        object COLOR : Kind()
        class FILE(val currentVfs: VfsFile, val filter: (VfsFile) -> Boolean) : Kind()
    }

    var evalContext: () -> Any? = { null }
    val initial = prop.value
    companion object {
        //val MAX_WIDTH = 300
        val MAX_WIDTH = 1000
    }

    init {
        prop.onChange {
            //println("prop.onChange: $it")
            if (current != it) {
                setValue(it, setProperty = false)
            }
        }
    }

    val contentText = UiLabel(app).also { it.text = "" }.also { it.visible = true }
    val contentTextField = UiTextField(app).also { it.text = contentText.text }.also { it.visible = false }
    var current: String = ""

    override fun hideEditor() {
        if (!contentText.visible) {
            contentText.visible = true
            contentTextField.visible = false
            setValue(untransformed(contentTextField.text))
            super.hideEditor()
        }
    }

    override fun showEditor() {
        contentTextField.text = contentText.text
        contentText.visible = false
        contentTextField.visible = true
        contentTextField.select()
        contentTextField.focus()
    }

    fun transformed(text: String): String = text.uescape()
    fun untransformed(text: String): String = text.unescape()

    fun setValue(value: String, setProperty: Boolean = true) {
        if (current != value) {
            current = value
            if (setProperty) prop.value = value
            val transformed = transformed(value)
            contentText.text = transformed
            contentTextField.text = transformed
        }
    }

    init {
        visible = true
        layout = HorizontalUiLayout
        contentText.onClick { showEditor() }
        contentTextField.onKeyEvent { e -> if (e.typeDown && e.key == Key.RETURN) hideEditor() }
        contentTextField.onFocus { e -> if (e.typeBlur) hideEditor() }
        var childCount = 1
        when (kind) {
            is Kind.FILE -> {
                button("...") {
                    preferredWidth = 50.percent
                    childCount++
                    onClick {
                        val file = openFileDialog(null, kind.filter)
                        if (file != null) {
                            val filePathInfo = file.absolutePathInfo
                            val currentVfsPathInfo = kind.currentVfs.absolutePathInfo
                            val relativePath = filePathInfo.relativePathTo(currentVfsPathInfo)
                            println("filePathInfo: $filePathInfo")
                            println("currentVfsPathInfo: $currentVfsPathInfo")
                            println("relativePath: $relativePath")

                            //PathInfo("test").rela
                            if (relativePath != null) {
                                setValue(relativePath)
                                completedEditing()
                            }
                        }
                    }
                } 
            }
            is Kind.COLOR -> {
                button("...") {
                    preferredWidth = 50.percent
                    childCount++
                    onClick {
                        val color = Colors[prop.value]
                        prop.value = (openColorPickerDialog(color) { prop.value = it.hexString } ?: color).hexString
                        completedEditing()
                    }
                }
            }
        }
        container {
            layout = UiFillLayout
            preferredWidth = if (childCount == 2) 50.percent else 100.percent
            setValue(initial)
            addChild(contentText)
            addChild(contentTextField)
        }
    }
}
