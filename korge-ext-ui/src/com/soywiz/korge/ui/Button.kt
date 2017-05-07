package com.soywiz.korge.ui

import com.soywiz.korge.html.Html
import com.soywiz.korge.input.*
import com.soywiz.korge.view.IHtml
import com.soywiz.korge.view.IText
import com.soywiz.korge.view.ninePatch
import com.soywiz.korge.view.text
import com.soywiz.korim.color.Colors
import com.soywiz.korio.util.redirectField

class Button(factory: UIFactory, skin: UISkin = factory.skin, initialText: String = "Label") : Widget(factory, skin), IText, IHtml {
	var over = false
	var down = false
	private val bgView = views.ninePatch(skin.buttonOut, width, height, 0.25, 0.25, 0.25, 0.25).apply { this@Button += this }
	private val textView = views.text(initialText).apply { this@Button += this }

	override var text: String by redirectField(textView::text)
	override var html: String by redirectField(textView::html)
	var format: Html.Format by redirectField(textView::format)

	init {
		format = Html.Format(size = 16, align = Html.Alignment.MIDDLE_CENTER, color = Colors.BLACK)

		onOver {
			over = true
			updateState()
		}
		onOut {
			over = false
			updateState()
		}
		onDown {
			down = true
			updateState()
		}
		onUp {
			down = false
			updateState()
		}
		onUpOutside {
			down = false
			updateState()
		}
		updateState()
	}

	private fun updateState() {
		bgView.tex = when {
			down -> skin.buttonDown
			over -> skin.buttonOver
			!over -> skin.buttonOut
			else -> skin.buttonOut
		}
		bgView.width = width
		bgView.height = height
		textView.textBounds.setTo(0, 0, width, height)
	}

	override fun updateSize() {
		updateState()
	}
}

fun UIFactory.button(text: String = "Button", skin: UISkin = this.skin) = Button(this, skin, text)
