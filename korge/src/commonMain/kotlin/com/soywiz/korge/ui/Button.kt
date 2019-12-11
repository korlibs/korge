package com.soywiz.korge.ui

import com.soywiz.korge.html.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*

@Deprecated("Use UIButton")
class Button(factory: UIFactory, skin: UISkin = factory.skin, initialText: String = "Label") : Widget(factory, skin),
	IText, IHtml {
	override fun createInstance(): View = Button(factory, skin, text)

	var over = false
	var down = false
	private val bgView =
		NinePatch(skin.buttonOut, width, height, 0.25, 0.25, 0.25, 0.25).apply { this@Button += this }
	private val textView = Text(initialText).apply { this@Button += this }

	override var text: String by textView::text.redirected()
	override var html: String by textView::html.redirected()
	var format: Html.Format by textView::format.redirected()

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

@Deprecated("Use UIButton")
fun UIFactory.button(text: String = "Button", skin: UISkin = this.skin) = Button(this, skin, text)
