package com.soywiz.korge.ui

import com.soywiz.korge.html.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun Container.textButton(
	width: Number,
	height: Number,
	text: String = "Button",
	skin: UISkin = defaultUISkin,
	textFont: Html.FontFace = defaultUIFont,
	block: @ViewsDslMarker TextButton.() -> Unit = {}
): TextButton = textButton(width.toDouble(), height.toDouble(), text, skin, textFont, block)

inline fun Container.textButton(
    width: Double = 128.0,
    height: Double = 64.0,
    text: String = "Button",
    skin: UISkin = defaultUISkin,
    textFont: Html.FontFace = defaultUIFont,
    block: @ViewsDslMarker TextButton.() -> Unit = {}
): TextButton = TextButton(width, height, text, skin, textFont).addTo(this).apply(block)

open class TextButton(
	width: Double = 128.0,
	height: Double = 64.0,
	text: String = "Button",
	skin: UISkin = DefaultUISkin,
	textFont: Html.FontFace = DefaultUIFont
) : UIButton(width, height, skin) {

	var text by uiObservable(text) { updateText(); updateShadow() }
	var textSize by uiObservable(16) { updateText() }
	var textColor by uiObservable(Colors.WHITE) { updateText() }
	var textAlignment by uiObservable(Html.Alignment.MIDDLE_CENTER) { updateText() }
	var textFont by uiObservable(textFont) { updateText(); updateShadow() }
	var shadowX by uiObservable(1) { updateShadow() }
	var shadowY by uiObservable(1) { updateShadow() }
	var shadowSize by uiObservable(16) { updateShadow() }
	var shadowColor by uiObservable(Colors.BLACK.withA(64)) { updateShadow() }
	var shadowVisible by uiObservable(true) { updateShadow() }

	private val textView = text(text)
	private val textShadow = text(text)

	init {
		updateText()
		updateShadow()
	}

	private fun updateText() {
		textView.format = Html.Format(face = textFont, size = textSize, color = textColor, align = textAlignment)
		textView.setTextBounds(Rectangle(0, 0, width, height))
		textView.setText(text)
	}

	private fun updateShadow() {
		textShadow.visible = shadowVisible
		textShadow.format = Html.Format(face = textFont, size = shadowSize, color = shadowColor, align = textAlignment)
		textShadow.setTextBounds(Rectangle(0, 0, width, height))
		textShadow.setText(text)
		textShadow.position(shadowX, shadowY)
	}

	override fun onSizeChanged() {
		super.onSizeChanged()
		textView.setTextBounds(Rectangle(0, 0, width, height))
		textShadow.setTextBounds(Rectangle(0, 0, width, height))
	}
}
