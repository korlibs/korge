package com.soywiz.korge.debug

import com.soywiz.korev.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korui.*
import com.soywiz.korui.layout.*

class UiTextEditableValue(
    app: UiApplication,
    override val prop: ObservableProperty<String>,
    val kind: Kind
) : UiEditableValue(app), ObservablePropertyHolder<String> {
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
        contentText.visible = true
        contentTextField.visible = false
        setValue(contentTextField.text)
    }

    override fun showEditor() {
        contentTextField.text = contentText.text
        contentText.visible = false
        contentTextField.visible = true
        contentTextField.select()
        contentTextField.focus()
    }

    fun setValue(value: String, setProperty: Boolean = true) {
        if (current != value) {
            current = value
            if (setProperty) prop.value = value
            contentText.text = value
            contentTextField.text = value
        }
    }

    init {
        visible = true
        layout = HorizontalUiLayout
        contentText.onClick { showEditor() }
        contentTextField.onKeyEvent { e -> if (e.typeDown && e.key == Key.RETURN) hideEditor() }
        contentTextField.onFocus { e -> if (e.typeBlur) hideEditor() }
        when (kind) {
            is Kind.FILE -> {
                button("...") {
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
                        }
                    }
                }
            }
            is Kind.COLOR -> {
                button("...") {
                    val color = Colors[prop.value]
                    prop.value = (openColorPickerDialog(color) {
                        prop.value = it.hexString
                    } ?: color).hexString
                }
            }
        }
        container {
            layout = UiFillLayout
            setValue(initial)
            addChild(contentText)
            addChild(contentTextField)
        }
    }
}
