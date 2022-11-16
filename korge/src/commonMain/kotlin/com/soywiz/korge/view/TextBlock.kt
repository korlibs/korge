package com.soywiz.korge.view

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.property.*
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
) : UIView(width, height), ViewLeaf {
    private var dirty = true

    @ViewProperty
    var text: RichTextData = text; set(value) { field = value; invalidateText() }

    @ViewProperty
    @ViewPropertyProvider(TextAlignment.Provider::class)
    var align: TextAlignment = align; set(value) { field = value; invalidProps() }

    @ViewProperty
    var includePartialLines: Boolean = false; set(value) { field = value; invalidProps() }
    @ViewProperty
    var includeFirstLineAlways: Boolean = true; set(value) { field = value; invalidProps() }
    @ViewProperty
    var fill: Paint? = colorMul; set(value) { field = value; invalidProps() }
    @ViewProperty
    var stroke: Stroke? = null; set(value) { field = value; invalidProps() }
    @ViewProperty
    var wordWrap: Boolean = true; set(value) { field = value; invalidProps() }
    @ViewProperty
    var ellipsis: String? = "..."; set(value) { field = value; invalidProps() }
    @ViewProperty
    var padding: Margin = Margin.EMPTY; set(value) { field = value; invalidProps() }
    @ViewProperty
    var autoSize: Boolean = false; set(value) { field = value; invalidateText() }
    @ViewProperty
    var plainText: String
        get() = text.text
        set(value) {
            text = RichTextData(value, style = text.defaultStyle)
        }
    private var image: Image? = null
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
        if (image == null) {
            image = image(Bitmaps.transparent)
        }
        image?.bitmap = bmp.slice()
        image?.program = (text.defaultStyle.font as? BitmapFont?)?.agProgram
        bmp.context2d {
            drawRichText(
                text,
                bounds = Rectangle.fromBounds(padding.left, padding.top, width - padding.right, height - padding.bottom),
                includePartialLines = includePartialLines, wordWrap = wordWrap, ellipsis = ellipsis, align = align,
                fill = fill, stroke = stroke, includeFirstLineAlways = true
            )
        }
    }

    override fun renderInternal(ctx: RenderContext) {
        if (allBitmap) {
            image?.removeFromParent()
            image = null
            renderCtx2d(ctx) {
                it.drawText(text, padding.left, padding.top, width - padding.right, height - padding.bottom, wordWrap, includePartialLines, ellipsis, fill, stroke, align)
            }
        } else {
            ensureTexture()
        }
        super.renderInternal(ctx)
    }
}
