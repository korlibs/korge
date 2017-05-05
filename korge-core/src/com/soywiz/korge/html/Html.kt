package com.soywiz.korge.html

import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.NamedColors
import com.soywiz.korio.serialization.xml.Xml
import com.soywiz.korio.serialization.xml.isComment
import com.soywiz.korio.serialization.xml.isNode
import com.soywiz.korio.serialization.xml.isText
import com.soywiz.korio.util.Computed
import com.soywiz.korio.util.Extra
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.bounds

object Html {
	data class Alignment(val anchor: Anchor) {
		companion object {
			val LEFT = Alignment(Anchor.TOP_LEFT)
			val CENTER = Alignment(Anchor.TOP_CENTER)
			val RIGHT = Alignment(Anchor.TOP_RIGHT)
			val JUSTIFIED = Alignment(Anchor.TOP_LEFT)

			val MIDDLE_LEFT = Alignment(Anchor.MIDDLE_LEFT)
			val MIDDLE_CENTER = Alignment(Anchor.MIDDLE_CENTER)
			val MIDDLE_RIGHT = Alignment(Anchor.MIDDLE_RIGHT)

			val BOTTOM_LEFT = Alignment(Anchor.BOTTOM_LEFT)
			val BOTTOM_CENTER = Alignment(Anchor.BOTTOM_CENTER)
			val BOTTOM_RIGHT = Alignment(Anchor.BOTTOM_RIGHT)
		}
	}

	//enum class VerticalAlignment(val ratio: Double) {
	//	TOP(0.0),
	//	MIDDLE(0.5),
	//	BOTTOM(1.0);
	//}


	interface FontFace {
		data class Named(val name: String) : FontFace
		data class Bitmap(val font: BitmapFont) : FontFace
	}

	class Format(
		override var parent: Format? = null,
		var color: Int? = null,
		var face: FontFace? = null,
		var size: Int? = null,
		var letterSpacing: Double? = null,
		var kerning: Int? = null,
		var align: Alignment? = null
	) : Computed.WithParent<Format> {
		val computedColor by Computed(Format::color) { Colors.WHITE }
		val computedFace by Computed(Format::face) { FontFace.Named("Arial") }
		val computedSize by Computed(Format::size) { 16 }
		val computedLetterSpacing by Computed(Format::letterSpacing) { 0.0 }
		val computedKerning by Computed(Format::kerning) { 0 }
		val computedAlign by Computed(Format::align) { Alignment.LEFT }
	}

	interface MetricsProvider {
		fun getBounds(text: String, format: Format, out: Rectangle): Unit

		object Identity : MetricsProvider {
			override fun getBounds(text: String, format: Format, out: Rectangle): Unit = run { out.setTo(0, 0, text.length, 1) }
		}
	}

	data class PositionContext(
		val provider: MetricsProvider,
		val bounds: Rectangle,
		var x: Double = 0.0,
		var y: Double = 0.0
	)

	data class Span(val format: Format, var text: String) : Extra by Extra.Mixin() {
		val bounds = Rectangle()

		fun doPositioning(ctx: PositionContext) {
			ctx.provider.getBounds(text, format, bounds)
			bounds.x += ctx.x
			ctx.x += bounds.width
		}
	}

	data class Line(val spans: ArrayList<Span> = arrayListOf()) : Extra by Extra.Mixin() {
		var format: Format = Format()
		val firstNonEmptySpan get() = spans.firstOrNull { it.text.isNotEmpty() }
		val bounds = Rectangle()

		fun doPositioning(ctx: PositionContext) {
			ctx.x = ctx.bounds.x
			for (v in spans) {
				// @TODO: Reposition when overflowing
				v.doPositioning(ctx)
			}

			spans.map { it.bounds }.bounds(bounds) // calculate bounds

			// Alignment
			//println(bounds)
			val restoreY = bounds.y
			bounds.setToAnchoredRectangle(bounds, format.computedAlign.anchor, ctx.bounds)
			bounds.y = restoreY
			//println(bounds)
			var sx = bounds.x
			for (v in spans) {
				v.bounds.x = sx
				sx += v.bounds.width
			}

			ctx.x = ctx.bounds.x
			ctx.y += bounds.height
		}
	}

	data class Paragraph(val lines: ArrayList<Line> = arrayListOf()) : Extra by Extra.Mixin() {
		val firstNonEmptyLine get() = lines.firstOrNull { it.firstNonEmptySpan != null }
		val bounds = Rectangle()

		fun doPositioning(ctx: PositionContext) {
			for (v in lines) v.doPositioning(ctx)
			lines.map { it.bounds }.bounds(bounds) // calculate bounds
			ctx.x = bounds.left
			ctx.y = bounds.bottom
		}
	}

	data class Document(val paragraphs: ArrayList<Paragraph> = arrayListOf()) : Extra by Extra.Mixin() {
		val defaultFormat = Html.Format()
		var xml = Xml("")
		val text: String get() = xml.text.trim()
		val bounds = Rectangle()
		val firstNonEmptyParagraph get() = paragraphs.firstOrNull { it.firstNonEmptyLine != null }
		val firstNonEmptySpan get() = firstNonEmptyParagraph?.firstNonEmptyLine?.firstNonEmptySpan
		val firstFormat get() = firstNonEmptySpan?.format ?: Format()
		val allSpans get() = paragraphs.flatMap { it.lines }.flatMap { it.spans }

		fun doPositioning(gp: MetricsProvider, bounds: Rectangle) {
			val ctx = PositionContext(gp, bounds)
			for (v in paragraphs) v.doPositioning(ctx)
			paragraphs.map { it.bounds }.bounds(this.bounds) // calculate bounds
		}
	}

	class HtmlParser {
		val document = Document()
		var currentLine = Line()
		var currentParagraph = Paragraph()

		val Xml.isDisplayBlock get() = this.name == "p" || this.name == "div"

		fun emitText(format: Format, text: String) {
			//println(format)
			//println(text)
			if (currentLine.spans.isEmpty()) {
				currentLine.format = Format(format)
			}
			currentLine.spans += Span(Format(format), text)
		}

		fun emitEndOfLine(format: Format) {
			//println("endOfLine")
			if (currentLine.spans.isNotEmpty()) {
				//currentLine.format = format
				currentParagraph.lines += currentLine
				document.paragraphs += currentParagraph
				currentParagraph = Paragraph()
				currentLine = Line()
			}
		}

		fun parse(xml: Xml, format: Format): Format {
			when {
				xml.isText -> {
					emitText(format, xml.text)
				}
				xml.isComment -> Unit
				xml.isNode -> {
					val block = xml.isDisplayBlock
					format.align = when (xml.str("align").toLowerCase()) {
						"center" -> Alignment.CENTER
						"left" -> Alignment.LEFT
						"right" -> Alignment.RIGHT
						"jusitifed" -> Alignment.JUSTIFIED
						else -> format.align
					}
					val face = xml.strNull("face")
					format.face = if (face != null) FontFace.Named(face) else format.face
					format.size = xml.intNull("size") ?: format.size
					format.letterSpacing = xml.doubleNull("letterSpacing") ?: format.letterSpacing
					format.kerning = xml.intNull("kerning") ?: format.kerning
					format.color = NamedColors[xml.strNull("color") ?: "white"]
					for (child in xml.allChildrenNoComments) {
						// @TODO: Change .copy for an inline format.keep { parse(xml, format) } that doesn't allocate at all
						parse(child, Format(format))
					}
					if (block) {
						emitEndOfLine(format)
					}
				}
			}
			return format
		}

		fun parse(html: String) {
			val xml = Xml(html)
			document.xml = xml
			//println(html)
			val format = parse(xml, document.defaultFormat)
			emitEndOfLine(format)
			//println(document.firstFormat)
		}
	}

	fun parse(html: String): Document = HtmlParser().apply { parse(html) }.document
}
