package com.soywiz.korge.ui

import com.soywiz.korge.html.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*

@Deprecated("Use UILabel")
class Label(factory: UIFactory, skin: UISkin = factory.skin, initialText: String = "Label") : Widget(factory, skin), IText, IHtml {
	val textView = Text(initialText).apply { this@Label += this }

	override var text: String by textView::text.redirected()
	override var html: String by textView::html.redirected()
	var format: Html.Format by textView::format.redirected()

	init {
		format = Html.Format(size = 16, align = Html.Alignment.MIDDLE_CENTER, color = Colors.BLACK)
	}

	override fun updateSize() {
		textView.textBounds.setTo(0, 0, width, height)
	}

}

@Deprecated("Use UILabel")
fun UIFactory.label(text: String = "Label", skin: UISkin = this.skin) = Label(this, skin, text)
