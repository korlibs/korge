package com.soywiz.korge.ui

import com.soywiz.korge.debug.*
import com.soywiz.korge.html.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun Container.textButton(
	width: Number,
	height: Number,
	text: String = "Button",
	skin: UISkin = defaultUISkin,
	textFont: Html.FontFace = defaultUIFont,
	block: @ViewDslMarker TextButton.() -> Unit = {}
): TextButton = textButton(width.toDouble(), height.toDouble(), text, skin, textFont, block)

inline fun Container.textButton(
    width: Double = 128.0,
    height: Double = 64.0,
    text: String = "Button",
    skin: UISkin = defaultUISkin,
    textFont: Html.FontFace = defaultUIFont,
    block: @ViewDslMarker TextButton.() -> Unit = {}
): TextButton = TextButton(width, height, text, skin, textFont).addTo(this).apply(block)

open class TextButton(
	width: Double = 128.0,
	height: Double = 64.0,
	text: String = "Button",
	skin: UISkin = DefaultUISkin,
	textFont: Html.FontFace = DefaultUIFont
) : UIButton(width, height, skin), ViewLeaf {

	var text by uiObservable(text) { updateText(); updateShadow() }
	var textSize by uiObservable(16) { updateText() }
	var textColor by uiObservable(Colors.WHITE) { updateText() }
	var textAlignment by uiObservable(Html.Alignment.MIDDLE_CENTER) { updateText() }
	var textFont by uiObservable(textFont) { updateText(); updateShadow() }
	var shadowX by uiObservable(1) { updateShadow() }
	var shadowY by uiObservable(1) { updateShadow() }
	//var shadowSize by uiObservable(16) { updateShadow() }
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
		textView.setTextBounds(Rectangle(0.0, 0.0, width, height))
		textView.setText(text)
	}

	private fun updateShadow() {
		textShadow.visible = shadowVisible
		textShadow.format = Html.Format(face = textFont, size = textSize, color = shadowColor, align = textAlignment)
		textShadow.setTextBounds(Rectangle(0.0, 0.0, width, height))
		textShadow.setText(text)
		textShadow.position(shadowX, shadowY)
	}

	override fun onSizeChanged() {
		super.onSizeChanged()
		textView.setTextBounds(Rectangle(0.0, 0.0, width, height))
		textShadow.setTextBounds(Rectangle(0.0, 0.0, width, height))
	}

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsableSection(TextButton::class.simpleName!!) {
            uiEditableValue(::text)
            uiEditableValue(::textSize, min = 1, max = 300)
            /*
            uiEditableValue(::verticalAlign, values = { listOf(VerticalAlign.TOP, VerticalAlign.MIDDLE, VerticalAlign.BASELINE, VerticalAlign.BOTTOM) })
            uiEditableValue(::horizontalAlign, values = { listOf(HorizontalAlign.LEFT, HorizontalAlign.CENTER, HorizontalAlign.RIGHT, HorizontalAlign.JUSTIFY) })
            uiEditableValue(::fontSource, UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.extensionLC == "ttf" || it.extensionLC == "fnt"
            })
             */
        }
        super.buildDebugComponent(views, container)
    }

    object Serializer : KTreeSerializerExt<TextButton>("UITextButton", TextButton::class, { TextButton().also { it.text = "Button" } }, {
        add(TextButton::text)
        add(TextButton::textSize)
        add(TextButton::width)
        add(TextButton::height)
    })
}
