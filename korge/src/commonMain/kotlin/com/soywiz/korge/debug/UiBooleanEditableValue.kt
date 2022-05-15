package com.soywiz.korge.debug

import com.soywiz.korui.UiApplication
import com.soywiz.korui.UiCheckBox
import com.soywiz.korui.layout.HorizontalUiLayout

class UiBooleanEditableValue(
    app: UiApplication,
    prop: ObservableProperty<Boolean>,
) : UiEditableValue<Boolean>(app, prop), ObservablePropertyHolder<Boolean> {
    val initial = prop.value

    init {
        prop.onChange {
            //println("prop.onChange: $it")
            setValue(it, setProperty = false)
        }
    }

    val contentCheckBox = UiCheckBox(app).also { it.text = "" }.also { it.checked = prop.value }

    override fun hideEditor() {
    }

    override fun showEditor() {
    }

    fun setValue(value: Boolean, setProperty: Boolean = true) {
        if (contentCheckBox.checked != value) {
            contentCheckBox.checked = value
        }
        if (setProperty) prop.value = value
    }

    init {
        layout = HorizontalUiLayout
        addChild(contentCheckBox)
        visible = true
        contentCheckBox.onChange {
            setValue(contentCheckBox.checked)
        }
    }
}
