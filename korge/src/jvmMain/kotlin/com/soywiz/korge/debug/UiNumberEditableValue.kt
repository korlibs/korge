package com.soywiz.korge.debug

import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korge.view.property.*
import com.soywiz.korge.view.property.ObservableProperty
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korte.*
import com.soywiz.korui.*
import com.soywiz.korui.layout.*
import kotlin.math.*

class UiNumberEditableValue(
    app: UiApplication,
    prop: ObservableProperty<Double>,
    var min: Double = -1.0,
    var max: Double = +1.0,
    var clampMin: Boolean = false,
    var clampMax: Boolean = false,
    var decimalPlaces: Int = 2
) : UiEditableValue<Double>(app, prop), ObservablePropertyHolder<Double> {
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
    var current: Double = Double.NaN

    val isEditorVisible get() = !contentText.visible

    override fun hideEditor() {
        if (!isEditorVisible) return
        contentText.visible = true
        contentTextField.visible = false
        if (contentTextField.text.isNotEmpty()) {
            val templateResult = runBlockingNoSuspensions { Template("{{ ${contentTextField.text} }}").invoke(evalContext()) }
            setValue(templateResult.toDoubleOrNull() ?: 0.0)
        }
        super.hideEditor()
    }

    override fun showEditor() {
        if (isEditorVisible) return
        contentTextField.text = contentText.text
        contentText.visible = false
        contentTextField.visible = true
        contentTextField.select()
        contentTextField.focus()
    }

    fun setValue(value: Double, setProperty: Boolean = true) {
        //println("setValue")
        var rvalue = value
        if (clampMin) rvalue = rvalue.coerceAtLeast(min)
        if (clampMax) rvalue = rvalue.coerceAtMost(max)
        val valueStr = rvalue.toStringDecimal(decimalPlaces)
        if (current != rvalue) {
            current = rvalue
            if (setProperty) {
                prop.value = rvalue
            }
            contentText.text = valueStr
            if (!isEditorVisible) {
                contentTextField.text = valueStr
            }
        }
    }

    init {
        layout = UiFillLayout
        //layout = HorizontalUiLayout
        visible = true
        contentText.onClick {
            showEditor()
        }
        contentTextField.onKeyEvent { e ->
            if (e.typeDown && e.key == Key.RETURN) {
                hideEditor()
            }
            //println(e)
        }
        contentTextField.onFocus { e ->
            if (e.typeBlur) {
                hideEditor()
            }
            //println(e)
        }
        var startX = 0
        var startY = 0
        var startValue = current
        contentText.onMouseEvent { e ->
            if (e.typeDown) {
                startX = e.x
                startY = e.y
                startValue = current
                e.requestLock()
            }
            if (e.typeUp) {
                app.views?.completedEditing(prop)
            }
            if (e.typeDrag) {
                val dx = (e.x - startX).toDouble()
                val dy = (e.y - startY).toDouble()
                //println("typeDrag: dx=$dx")
                val lenAbs = dx.absoluteValue.convertRange(0.0, MAX_WIDTH.toDouble(), 0.0, max - min)
                val len = lenAbs.withSign(dx)
                setValue(startValue + len)
                //println("//")
            }
        }
        setValue(initial)
        contentText.cursor = UiStandardCursor.RESIZE_EAST
        addChild(contentText)
        addChild(contentTextField)
    }
}
