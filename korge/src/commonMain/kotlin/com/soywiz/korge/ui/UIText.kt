package com.soywiz.korge.ui

import com.soywiz.korge.html.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.text.*
import com.soywiz.korma.geom.*

inline fun Container.uiText(
    text: String,
    width: Double = 128.0,
    height: Double = 64.0,
    skin: TextSkin = defaultTextSkin,
    block: @ViewDslMarker UIText.() -> Unit = {}
): UIText = UIText(text, width, height, skin).addTo(this).apply(block)

class UIText(
    text: String,
    width: Double = 128.0,
    height: Double = 64.0,
    private val skin: TextSkin = DefaultTextSkin
) : UIView(width, height) {

    var text by uiObservable(text) { updateText() }
    var textColor by uiObservable(skin.normal.color) { updateText() }
    var textSize by uiObservable(skin.normal.size) { updateText() }
    var textFont by uiObservable(skin.normal.font) { updateText() }
    var textAlignment by uiObservable(TextAlignment.MIDDLE_CENTER) { updateText() }

    protected var bover by uiObservable(false) { updateState() }
    protected var bpressing by uiObservable(false) { updateState() }

    private val background = solidRect(width, height, skin.backColor)
    private val textView = text(text)

    init {
        mouse {
            onOver {
                simulateOver()
            }
            onOut {
                simulateOut()
            }
            onDown {
                simulateDown()
            }
            onUpAnywhere {
                simulateUp()
            }
        }
        updateText()
    }

    fun simulateOver() {
        bover = true
    }

    fun simulateOut() {
        bover = false
    }

    fun simulatePressing(value: Boolean) {
        bpressing = value
    }

    fun simulateDown() {
        bpressing = true
    }

    fun simulateUp() {
        bpressing = false
    }

    override fun updateState() {
        val (color, size, font) = when {
            !enabled -> skin.disabled
            bpressing -> skin.down
            bover -> skin.over
            else -> skin.normal
        }
        textColor = color
        textSize = size
        textFont = font
        updateText()
    }

    private fun updateText() {
        textView.setFormat(face = textFont, size = textSize, color = textColor, align = textAlignment)
        textView.setTextBounds(Rectangle(0.0, 0.0, width, height))
        textView.setText(text)
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        background.size(width, height)
        textView.setTextBounds(Rectangle(0.0, 0.0, width, height))
    }
}
