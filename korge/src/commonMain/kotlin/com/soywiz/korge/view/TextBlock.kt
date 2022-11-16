package com.soywiz.korge.view

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korge.ui.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.font.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.text.*
import com.soywiz.korma.geom.*

inline fun Container.textBlock(
    text: RichTextData = RichTextData("", textSize = 16.0, font = DefaultTtfFontAsBitmap),
    align: TextAlignment = TextAlignment.TOP_LEFT,
    width: Double = 100.0,
    height: Double = 100.0,
    block: @ViewDslMarker TextBlock.() -> Unit = {}
): TextBlock
    = TextBlock(text, align, width, height).addTo(this, block)

class TextBlock(
    text: RichTextData = RichTextData("", textSize = 16.0, font = DefaultTtfFontAsBitmap),
    align: TextAlignment = TextAlignment.TOP_LEFT,
    width: Double = 100.0,
    height: Double = 100.0,
) : UIView(width, height) {
    private var dirty = true
    var text: RichTextData = text; set(value) { field = value; invalidateText() }
    var align: TextAlignment = align; set(value) { field = value; invalidProps() }
    var includePartialLines: Boolean = false; set(value) { field = value; invalidProps() }
    var fill: Paint? = colorMul; set(value) { field = value; invalidProps() }
    var stroke: Stroke? = null; set(value) { field = value; invalidProps() }
    var wordWrap: Boolean = true; set(value) { field = value; invalidProps() }
    var ellipsis: String? = "..."; set(value) { field = value; invalidProps() }
    var padding: Margin = Margin.EMPTY; set(value) { field = value; invalidProps() }
    var autoSize: Boolean = false; set(value) { field = value; invalidateText() }
    var plainText: String
        get() = text.text
        set(value) {
            text = RichTextData(value, style = text.defaultStyle)
        }
    private var image = image(Bitmaps.transparent)
    private var allBitmap: Boolean = true

    private fun invalidateText() {
        invalidProps()
        if (autoSize) {
            setSize(text.width, text.height)
        }
        allBitmap = text.allFonts.all { it is BitmapFont }
    }
    
    private fun invalidProps() {
        dirty = true
        invalidateRender()
    }

    override fun onSizeChanged() {
        invalidProps()
    }

    init {
        invalidateText()
    }

    private fun ensureTexture() {
        if (!dirty) return
        dirty = false
        val bmp = NativeImage(width.toIntCeil(), height.toIntCeil())
        //println("ensureTexture: bmp=$bmp")
        image.bitmap = bmp.slice()
        image.program = (text.defaultStyle.font as? BitmapFont?)?.agProgram
        bmp.context2d {
            drawRichText(
                text,
                bounds = Rectangle.fromBounds(padding.left, padding.top, width - padding.right, height - padding.bottom),
                includePartialLines = includePartialLines, wordWrap = wordWrap, ellipsis = ellipsis, align = align,
                fill = fill, stroke = stroke,
            )
        }
    }

    override fun renderInternal(ctx: RenderContext) {
        if (allBitmap) {
            renderCtx2d(ctx) {
                it.drawText(text, padding.left, padding.top, width - padding.right, height - padding.bottom, wordWrap, includePartialLines, ellipsis, fill, stroke, align)
            }
        } else {
            ensureTexture()
        }
        super.renderInternal(ctx)
    }
}
