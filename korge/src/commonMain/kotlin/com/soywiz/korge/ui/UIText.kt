package com.soywiz.korge.ui

import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*

inline fun Container.uiText(
    text: String,
    width: Double = 128.0,
    height: Double = 18.0,
    block: @ViewDslMarker UIText.() -> Unit = {}
): UIText = UIText(text, width, height).addTo(this).apply(block)

class UIText(
    var text: String,
    width: Double = 128.0,
    height: Double = 64.0,
) : UIView(width, height) {
    protected var bover by uiObservable(false) { updateState() }
    protected var bpressing by uiObservable(false) { updateState() }

    //private val background = solidRect(width, height, buttonBackColor)
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

    private val textBounds = Rectangle()

    override fun renderInternal(ctx: RenderContext) {
        textBounds.setTo(0.0, 0.0, width, height)
        textView.setFormat(face = textFont, size = textSize.toInt(), color = textColor, align = textAlignment)
        textView.setTextBounds(textBounds)
        //background.size(width, height)
        textView.text = text
        super.renderInternal(ctx)
    }

    override fun updateState() {
    }
}
