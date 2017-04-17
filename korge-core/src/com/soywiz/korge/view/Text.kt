package com.soywiz.korge.view

import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.html.Html
import com.soywiz.korge.render.RenderContext
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Rectangle

interface IText {
	var text: String
}

interface IHtml {
	var html: String
}

class Text(views: Views) : View(views), IText, IHtml {
	var _text: String = ""
	var _html: String = ""

	override var text: String
		get() = if (document != null) document?.xml?.text ?: "" else _text
		set(value) {
			_text = value
			_html = ""
			document = null
		}
	var document: Html.Document? = null
	var format: Html.Format = Html.Format()
	val textBounds = Rectangle(0, 0, 1024, 1024)
	private val tempRect = Rectangle()

	override var html: String
		get() = if (document != null) _html else _text
		set(value) {
			document = Html.parse(value)
			relayout()
			format = document!!.firstFormat.copy()
			_text = ""
			_html = value
		}

	fun relayout() {
		document?.doPositioning(views.fontRepository, textBounds)
	}

	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (document != null) {
			for (span in document!!.allSpans) {
				val font = views.fontRepository.getBitmapFont(span.format)
				font.drawText(ctx.batch, format.size.toDouble(), text, span.bounds.x.toInt(), span.bounds.y.toInt(), m, blendMode = blendMode)
			}
		} else {
			val font = views.fontRepository.getBitmapFont(format)
			views.fontRepository.getBounds(text, format, tempRect)
			tempRect.setToAnchoredRectangle(tempRect, format.align.anchor, textBounds)
			font.drawText(ctx.batch, format.size.toDouble(), text, tempRect.x.toInt(), tempRect.y.toInt(), m, blendMode = blendMode)
		}
	}

	override fun getLocalBounds(out: Rectangle) {
		if (document != null) {
			out.copyFrom(document!!.bounds)
		} else {
			views.fontRepository.getBounds(text, format, out)
			out.setToAnchoredRectangle(out, format.align.anchor, textBounds)
		}
	}
}

fun Views.text(font: BitmapFont, text: String, textSize: Double = 16.0) = Text(this).apply {
	this.format.face = Html.FontFace.Bitmap(font)
	this.format.size = textSize.toInt()
	this.text = text
}

fun Container.text(font: BitmapFont, text: String, textSize: Double = 16.0): Text = text(font, text, textSize) { }

inline fun Container.text(font: BitmapFont, text: String, textSize: Double = 16.0, callback: Text.() -> Unit): Text {
	val child = views.text(font, text, textSize)
	this += child
	callback(child)
	return child
}

fun View?.setText(text: String) {
	this.descendants {
		if (it is IText) it.text = text
	}
}

fun View?.setHtml(html: String) {
	this.descendants {
		if (it is IHtml) it.html = html
	}
}
