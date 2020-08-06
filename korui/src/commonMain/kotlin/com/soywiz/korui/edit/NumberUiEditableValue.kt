package com.soywiz.korui.edit

import com.soywiz.kmem.convertRange
import com.soywiz.korev.*
import com.soywiz.korio.async.runBlockingNoSuspensions
import com.soywiz.korio.util.toStringDecimal
import com.soywiz.korte.Template
import com.soywiz.korui.*
import kotlin.math.absoluteValue
import kotlin.math.withSign

class NumberUiEditableValue(
    app: UiApplication,
    var initial: Double,
    var min: Double = -1.0,
    var max: Double = +1.0,
    var clampMin: Boolean = false,
    var clampMax: Boolean = false
) : UiEditableValue(app) {
    var evalContext: () -> Any? = { null }
    companion object {
        //val MAX_WIDTH = 300
        val MAX_WIDTH = 1000
    }

    val contentText = UiLabel(app).also { it.text = "" }.also { it.visible = true }
    val contentTextField = UiTextField(app).also { it.text = contentText.text }.also { it.visible = false }
    var current: Double = Double.NaN

    override fun hideEditor() {
        contentText.visible = true
        contentTextField.visible = false
        val templateResult = runBlockingNoSuspensions { Template("{{ ${contentTextField.text} }}").invoke(evalContext()) }
        setValue(templateResult.toDoubleOrNull() ?: 0.0)
    }

    override fun showEditor() {
        contentTextField.text = contentText.text
        contentText.visible = false
        contentTextField.visible = true
        contentTextField.select()
        contentTextField.focus()
    }

    fun setValue(value: Double) {
        var rvalue = value
        if (clampMin) rvalue = rvalue.coerceAtLeast(min)
        if (clampMax) rvalue = rvalue.coerceAtMost(max)
        val valueStr = rvalue.toStringDecimal(2)
        current = rvalue
        contentText.text = valueStr
        contentTextField.text = valueStr
    }

    init {
        layout = UiFillLayout
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
            if (e.typeDrag) {
                val dx = (e.x - startX).toDouble()
                val dy = (e.y - startY).toDouble()
                val lenAbs = dx.absoluteValue.convertRange(0.0, MAX_WIDTH.toDouble(), 0.0, max - min)
                val len = lenAbs.withSign(dx)
                setValue(startValue + len)
            }
        }
        setValue(initial)
        contentText.cursor = UiStandardCursor.RESIZE_EAST
        addChild(contentText)
        addChild(contentTextField)
    }
}
