package com.soywiz.korge.ui

import com.soywiz.korge.debug.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.text.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

inline fun Container.uiTextButton(
    width: Double = 128.0,
    height: Double = 64.0,
    text: String = "Button",
    skin: UISkin = defaultUISkin,
    textFont: Font = defaultUIFont,
    textSize: Double = 16.0,
    block: @ViewDslMarker UITextButton.() -> Unit = {}
): UITextButton = UITextButton(width, height, text, skin, textFont, textSize).addTo(this).apply(block)

open class UITextButton(
    width: Double = 128.0,
    height: Double = 64.0,
    text: String = "Button",
    skin: UISkin = DefaultUISkin,
    textFont: Font = DefaultUIFont,
    textSize: Double = 16.0
) : UIButton(width, height, skin), ViewLeaf {

	var text by uiObservable(text) { updateText(); updateShadow() }
	var textSize by uiObservable(textSize) { updateText() }
	var textColor by uiObservable(Colors.WHITE) { updateText() }
	var textAlignment by uiObservable(TextAlignment.MIDDLE_CENTER) { updateText() }
	var textFont by uiObservable(textFont) { updateText(); updateShadow() }
	var shadowX by uiObservable(1) { updateShadow() }
	var shadowY by uiObservable(1) { updateShadow() }
	//var shadowSize by uiObservable(16) { updateShadow() }
	var shadowColor by uiObservable(Colors.BLACK.withA(64)) { updateShadow() }
	var shadowVisible by uiObservable(true) { updateShadow() }

	private val textView = text(text, textSize)
	private val textShadow = text(text, textSize)

	init {
		updateText()
		updateShadow()
	}

	private fun updateText() {
        textView.font = textFont
        textView.textSize = textSize
        textView.color = textColor
        textView.alignment = textAlignment
		textView.setTextBounds(Rectangle(0.0, 0.0, width, height))
		textView.setText(text)
	}

	private fun updateShadow() {
		textShadow.visible = shadowVisible
        textShadow.font = textFont
        textShadow.textSize = textSize
        textShadow.color = shadowColor
        textShadow.alignment = textAlignment
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
        container.uiCollapsableSection(UITextButton::class.simpleName!!) {
            uiEditableValue(::text)
            uiEditableValue(::textSize, min = 1.0, max = 300.0)
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

    object Serializer : KTreeSerializerExt<UITextButton>("UITextButton", UITextButton::class, { UITextButton().also { it.text = "Button" } }, {
        add(UITextButton::text)
        add(UITextButton::textSize)
        add(UITextButton::width)
        add(UITextButton::height)
    })
}
