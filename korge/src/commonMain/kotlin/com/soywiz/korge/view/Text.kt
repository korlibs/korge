package com.soywiz.korge.view

import com.soywiz.korge.bitmapfont.*
import com.soywiz.korge.html.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korma.geom.*

inline fun Container.text(
	text: String, textSize: Double = 16.0, font: BitmapFont = Fonts.defaultFont,
	callback: @ViewsDslMarker Text.() -> Unit = {}
) = Text(text, textSize = textSize, font = font).addTo(this).apply(callback)

class Text : View(), IText, IHtml {
	companion object {
		operator fun invoke(
			text: String,
			textSize: Double = 16.0,
			color: RGBA = Colors.WHITE,
			font: BitmapFont = Fonts.defaultFont
		): Text = Text().apply {
			this.format = Html.Format(color = color, face = Html.FontFace.Bitmap(font), size = textSize.toInt())
			if (text != "") this.text = text
		}
	}

	//var verticalAlign: Html.VerticalAlignment = Html.VerticalAlignment.TOP
	val textBounds = Rectangle(0, 0, 1024, 1024)
	private val tempRect = Rectangle()
	var _text: String = ""
	var _html: String = ""
	var document: Html.Document? = null
	private var _format: Html.Format = Html.Format()
	var filtering = true
	var autoSize = true
		set(value) {
			field = value
			recalculateBoundsWhenRequired()
		}
	var bgcolor = Colors.TRANSPARENT_BLACK
	val fonts = Fonts.fonts

	fun setTextBounds(rect: Rectangle) {
		this.textBounds.copyFrom(rect)
		autoSize = false
	}


	fun unsetTextBounds() {
		autoSize = true
	}

	var format: Html.Format
		get() = _format
		set(value) {
			_format = value
			if (value != document?.defaultFormat) {
				document?.defaultFormat?.parent = value
			}
			recalculateBoundsWhenRequired()
		}

	override var text: String
		get() = if (document != null) document?.xml?.text ?: "" else _text
		set(value) {
			_text = value
			_html = ""
			document = null
			recalculateBoundsWhenRequired()
		}
	override var html: String
		get() = if (document != null) _html else _text
		set(value) {
			document = Html.parse(value)
			relayout()
			document!!.defaultFormat.parent = format
			_text = ""
			_html = value
			_format = document!!.firstFormat.consolidate()
		}

	fun relayout() {
		document?.doPositioning(fonts, textBounds)
	}

	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
		val colorMul = renderColorMul
		val colorAdd = renderColorAdd
		val m = globalMatrix
		if (document != null) {
			for (span in document!!.allSpans) {
				val font = fonts.getBitmapFont(span.format)
				val format = span.format
				font.drawText(
					ctx, format.computedSize.toDouble(), text,
					span.bounds.x.toInt(), span.bounds.y.toInt(),
					m,
					colMul = RGBA.multiply(colorMul, format.computedColor),
					colAdd = colorAdd,
					blendMode = renderBlendMode,
					filtering = filtering
				)
			}
		} else {
			val font = fonts.getBitmapFont(format)
			val anchor = format.computedAlign.anchor
			fonts.getBounds(text, format, out = tempRect)
			//println("tempRect=$tempRect, textBounds=$textBounds")
			//tempRect.setToAnchoredRectangle(tempRect, format.align.anchor, textBounds)
			//val x = (textBounds.width) * anchor.sx - tempRect.width
			val px = textBounds.x + (textBounds.width - tempRect.width) * anchor.sx
			//val x = textBounds.x + (textBounds.width) * anchor.sx
			val py = textBounds.y + (textBounds.height - tempRect.height) * anchor.sy

			if (bgcolor.a != 0) {
				ctx.batch.drawQuad(
					ctx.getTex(Bitmaps.white),
					x = textBounds.x.toFloat(),
					y = textBounds.y.toFloat(),
					width = textBounds.width.toFloat(),
					height = textBounds.height.toFloat(),
					m = m,
					filtering = false,
					colorMulInt = RGBA.multiplyInt(bgcolor.rgba, renderColorMulInt),
					colorAdd = colorAdd,
					blendFactors = renderBlendMode.factors
				)
			}

			//println(" -> ($x, $y)")
			font.drawText(
				ctx, format.computedSize.toDouble(), text, px.toInt(), py.toInt(),
				m,
				colMul = RGBA.multiply(colorMul, format.computedColor),
				colAdd = colorAdd,
				blendMode = renderBlendMode,
				filtering = filtering
			)
		}
	}

	private fun recalculateBounds() {
		fonts.getBounds(text, format, out = textBounds)
	}

	private fun recalculateBoundsWhenRequired() {
		if (autoSize) recalculateBounds()
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		if (document != null) {
			out.copyFrom(document!!.bounds)
		} else {
			if (autoSize) {
				fonts.getBounds(text, format, out)
				out.setToAnchoredRectangle(out, format.computedAlign.anchor, textBounds)
			} else {
				out.copyFrom(textBounds)
			}
			//println(textBounds)
		}
	}

	override fun createInstance(): View = Text()
	override fun copyPropsFrom(source: View) {
		super.copyPropsFrom(source)
		source as Text
		this.textBounds.copyFrom(source.textBounds)
		if (source._html.isNotEmpty()) {
			this.html = source.html
		} else {
			this.text = source.text
		}
	}
}

interface IText {
	var text: String
}

interface IHtml {
	var html: String
}

fun View?.setText(text: String) = run { this.foreachDescendant { if (it is IText) it.text = text } }
fun View?.setHtml(html: String) = run { this.foreachDescendant { if (it is IHtml) it.html = html } }
