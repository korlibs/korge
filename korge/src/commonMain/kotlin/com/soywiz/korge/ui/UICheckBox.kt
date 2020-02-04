package com.soywiz.korge.ui

import com.soywiz.korge.html.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*

inline fun Container.uiCheckBox(
    width: Number = 120.0,
    height: Number = 32.0,
    checked: Boolean = false,
    text: String = "CheckBox",
    textFont: Html.FontFace = defaultUIFont,
    skin: CheckBoxSkin = defaultCheckBoxSkin,
    block: @ViewsDslMarker UICheckBox.() -> Unit = {}
): UICheckBox = UICheckBox(width.toDouble(), height.toDouble(), checked, text, textFont, skin)
    .addTo(this).apply(block)

open class UICheckBox(
    width: Double = 120.0,
    height: Double = 32.0,
    checked: Boolean = false,
    text: String = "CheckBox",
    textFont: Html.FontFace = DefaultUIFont,
    private val skin: CheckBoxSkin = DefaultCheckBoxSkin
) : UIView(width, height) {

    var checked by uiObservable(checked) { updateState() }
    var text by uiObservable(text) { updateText() }
    var textFont by uiObservable(textFont) { updateText() }
    var textSize by uiObservable(16) { updateText() }
    var textColor by uiObservable(Colors.WHITE) { updateText() }

    val onChange = Signal<UICheckBox>()

    private val background = solidRect(width, height, Colors.TRANSPARENT_BLACK)
    private val box = uiButton(height, height, skin = skin.normal)
    private val textView = text(text)

    init {
        mouse {
            onOver {
                box.simulateOver()
            }
            onOut {
                box.simulateOut()
            }
            onDown {
                box.simulateDown()
            }
            onUpAnywhere {
                box.simulateUp()
            }
            onClick {
                this@UICheckBox.checked = !this@UICheckBox.checked
            }
        }
        updateState()
        updateText()
    }

    override fun updateState() {
        super.updateState()
        box.skin = if (checked) skin.checked else skin.normal
        onChange(this)
    }

    private fun updateText() {
        textView.format = Html.Format(
            face = textFont,
            size = textSize,
            color = textColor,
            align = Html.Alignment.MIDDLE_LEFT
        )
        textView.setTextBounds(Rectangle(0, 0, width - height, height))
        textView.setText(text)
        textView.position(height + 8.0, 0)
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        background.size(width, height)
        box.size(height, height)
        textView.position(height + 8.0, 0)
        textView.setTextBounds(Rectangle(0, 0, width - height - 8.0, height))
    }
}
