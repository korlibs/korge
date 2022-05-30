package com.soywiz.korge.view

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korge.bitmapfont.drawText
import com.soywiz.korge.html.Html
import com.soywiz.korge.internal.KorgeDeprecated
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.scene.debugBmpFontSync
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.Font
import com.soywiz.korma.geom.Rectangle
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

@KorgeDeprecated
inline fun Container.textOld(
    text: String,
    textSize: Double = 16.0,
    color: RGBA = Colors.WHITE,
    font: BitmapFont = debugBmpFontSync,
    fontsCatalog: Html.FontsCatalog? = null,
    callback: @ViewDslMarker TextOld.() -> Unit = {}
) = TextOld(text, textSize = textSize, color = color, font = font, fontsCatalog = fontsCatalog).addTo(this, callback)

@KorgeDeprecated
class TextOld : View(), IText, IHtml {
	companion object {
		operator fun invoke(
            text: String,
            textSize: Double = 16.0,
            color: RGBA = Colors.WHITE,
            font: BitmapFont = debugBmpFontSync,
            fontsCatalog: Html.FontsCatalog?  =null,
		): TextOld = TextOld().apply {
			this.format = Html.Format(color = color, face = font, size = textSize.toInt())
			if (text != "") this.text = text
            this.fontsCatalog = fontsCatalog ?: Html.DefaultFontCatalogWithoutSystemFonts
		}
	}

	//var verticalAlign: Html.VerticalAlignment = Html.VerticalAlignment.TOP
    var fontsCatalog: Html.FontsCatalog = Html.DefaultFontCatalogWithoutSystemFonts
	val textBounds = Rectangle(0, 0, 1024, 1024)
	private val tempRect = Rectangle()
	var _text: String = ""
	var _html: String = ""
	var document: Html.Document? = null
	private var _format: Html.Format = Html.Format()
	var filtering = true
    var smoothing: Boolean
        get() = filtering
        set(value) {
            filtering = value
        }
	var autoSize = true
		set(value) {
			field = value
			recalculateBoundsWhenRequired()
		}
	var bgcolor = Colors.TRANSPARENT_BLACK

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

    var font: Font?
        get() = format.computedFace
        set(value) { format.face = value }

    var color: RGBA
        get() = format.computedColor
        set(value) { format.color = value }

    var textSize: Double
        get() = format.computedSize.toDouble()
        set(value) { format.size = value.toInt() }

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
			document = Html.parse(value, fontsCatalog)
			relayout()
			document!!.defaultFormat.parent = format
			_text = ""
			_html = value
			_format = document!!.firstFormat.consolidate()
		}

	fun relayout() {
		document?.doPositioning(fontsCatalog, textBounds)
	}

    //override fun hitTest(x: Double, y: Double): View? {
    //    val rect = when {
    //        autoSize -> tempRect.also { fonts.getBounds(text, format, out = it) }
    //        else -> textBounds
    //    }
    //    return if (rect.contains(globalToLocalX(x, y), globalToLocalY(x, y))) this else null
    //}

    override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
		val colorMul = renderColorMul
		val colorAdd = renderColorAdd
		val m = globalMatrix
		if (document != null) {
			document!!.allSpans.fastForEach { span ->
				val font = fontsCatalog.getBitmapFont(span.format.computedFace)
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
			val font = format.computedFace
			val anchor = format.computedAlign.anchor
            fontsCatalog.getBounds(text, format, out = tempRect)
			//println("tempRect=$tempRect, textBounds=$textBounds")
			//tempRect.setToAnchoredRectangle(tempRect, format.align.anchor, textBounds)
			//val x = (textBounds.width) * anchor.sx - tempRect.width
			val px = textBounds.x + (textBounds.width - tempRect.width) * anchor.sx
			//val x = textBounds.x + (textBounds.width) * anchor.sx
			val py = textBounds.y + (textBounds.height - tempRect.height) * anchor.sy

			if (bgcolor.a != 0) {
                ctx.useBatcher { batch ->
                    batch.drawQuad(
                        ctx.getTex(Bitmaps.white),
                        x = textBounds.x.toFloat(),
                        y = textBounds.y.toFloat(),
                        width = textBounds.width.toFloat(),
                        height = textBounds.height.toFloat(),
                        m = m,
                        filtering = false,
                        colorMul = RGBA.multiply(bgcolor, renderColorMul),
                        colorAdd = colorAdd,
                        blendFactors = renderBlendMode.factors
                    )
                }
			}

			//println(" -> ($x, $y)")
            fontsCatalog.getBitmapFont(font).drawText(
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
        fontsCatalog.getBounds(text, format, out = textBounds)
        //println("textBounds: $textBounds")
	}

	private fun recalculateBoundsWhenRequired() {
		if (autoSize) recalculateBounds()
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		if (document != null) {
			out.copyFrom(document!!.bounds)
		} else {
			if (autoSize) {
                fontsCatalog.getBounds(text, format, out)
				out.setToAnchoredRectangle(out, format.computedAlign.anchor, textBounds)
			} else {
				out.copyFrom(textBounds)
			}
		}
        //println("getLocalBoundsInternal=$out")
	}

	override fun createInstance(): View = TextOld()
	override fun copyPropsFrom(source: View) {
		super.copyPropsFrom(source)
		source as TextOld
		this.textBounds.copyFrom(source.textBounds)
		if (source._html.isNotEmpty()) {
			this.html = source.html
		} else {
			this.text = source.text
		}
	}
}
