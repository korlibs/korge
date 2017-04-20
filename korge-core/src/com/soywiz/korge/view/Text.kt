package com.soywiz.korge.view

import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.html.Html
import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
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
		val color = globalColor
		if (document != null) {
			for (span in document!!.allSpans) {
				val font = views.fontRepository.getBitmapFont(span.format)
				val format = span.format
				font.drawText(ctx.batch, format.size.toDouble(), text, span.bounds.x.toInt(), span.bounds.y.toInt(), m, col1 = RGBA.multiply(color, format.color), blendMode = blendMode)
			}
		} else {
			val font = views.fontRepository.getBitmapFont(format)
			val anchor = format.align.anchor
			views.fontRepository.getBounds(text, format, out = tempRect)
			//println("tempRect=$tempRect, textBounds=$textBounds")
			//tempRect.setToAnchoredRectangle(tempRect, format.align.anchor, textBounds)
			//val x = (textBounds.width) * anchor.sx - tempRect.width
			val x = textBounds.x + (textBounds.width - tempRect.width) * anchor.sx
			//val x = textBounds.x + (textBounds.width) * anchor.sx
			val y = textBounds.y + (textBounds.height) * anchor.sy
			//println(" -> ($x, $y)")
			font.drawText(ctx.batch, format.size.toDouble(), text, x.toInt(), y.toInt(), m, col1 = RGBA.multiply(color, format.color), blendMode = blendMode)
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

fun Views.text(text: String, textSize: Double = 16.0, color: Int = Colors.WHITE, font: BitmapFont = this.defaultFont) = Text(this).apply {
	this.format.color = color
	this.format.face = Html.FontFace.Bitmap(font)
	this.format.size = textSize.toInt()
	this.text = text
}

fun Container.text(text: String, textSize: Double = 16.0, font: BitmapFont = this.views.defaultFont): Text = text(text, textSize, font) {
}

inline fun Container.text(text: String, textSize: Double = 16.0, font: BitmapFont = this.views.defaultFont, callback: Text.() -> Unit): Text {
	val child = views.text(text, textSize = textSize, font = font)
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
