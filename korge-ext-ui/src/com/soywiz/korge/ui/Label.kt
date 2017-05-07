package com.soywiz.korge.ui

import com.soywiz.korge.html.Html
import com.soywiz.korge.view.IHtml
import com.soywiz.korge.view.IText
import com.soywiz.korge.view.text
import com.soywiz.korim.color.Colors
import com.soywiz.korio.util.redirectField

class Label(factory: UIFactory, skin: UISkin = factory.skin, initialText: String = "Label") : Widget(factory, skin), IText, IHtml {
	val textView = views.text(initialText).apply { this@Label += this }

	override var text: String by redirectField(textView::text)
	override var html: String by redirectField(textView::html)
	var format: Html.Format by redirectField(textView::format)

	init {
		format = Html.Format(size = 16, align = Html.Alignment.MIDDLE_CENTER, color = Colors.BLACK)
	}

	override fun updateSize() {
		textView.textBounds.setTo(0, 0, width, height)
	}

}

fun UIFactory.label(text: String = "Label", skin: UISkin = this.skin) = Label(this, skin, text)
