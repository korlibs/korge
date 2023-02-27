package com.soywiz.korge.view

import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.bitmapfont.*
import com.soywiz.korge.render.*
import com.soywiz.korge.text.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.text.*
import com.soywiz.korio.resources.*
import com.soywiz.korma.geom.*

inline fun Container.text(
    text: String,
    textSize: Double = 16.0,
    font: Font = DefaultTtfFontAsBitmap,
    align: TextAlignment = TextAlignment.TOP_LEFT,
    width: Double = 100.0,
    height: Double = 100.0,
    color: RGBA? = null,
    autoSize: Boolean = true,
    block: @ViewDslMarker Text.() -> Unit = {}
): Text
    = Text(text, textSize, font, align, width, height, color, autoSize).addTo(this, block)

inline fun Container.textBlock(
    text: RichTextData = RichTextData("", textSize = 16.0, font = DefaultTtfFontAsBitmap),
    align: TextAlignment = TextAlignment.TOP_LEFT,
    width: Double = 100.0,
    height: Double = 100.0,
    autoSize: Boolean = false,
    block: @ViewDslMarker TextBlock.() -> Unit = {}
): TextBlock
    = TextBlock(text, align, width, height, autoSize).addTo(this, block)

val RichTextData.Style.Companion.DEFAULT_AS_BITMAP by lazy { RichTextData.Style.DEFAULT.copy(font = DefaultTtfFontAsBitmap) }

open class Text(
    text: RichTextData = RichTextData("", RichTextData.Style.DEFAULT_AS_BITMAP),
    align: TextAlignment = TextAlignment.TOP_LEFT,
    width: Double = 100.0,
    height: Double = 100.0,
    autoSize: Boolean = true,
) : BaseText(text, align, width, height, autoSize), IText {
    override val asView: Container get() = this

    constructor(
        text: String,
        textSize: Double = 16.0,
        font: Font = DefaultTtfFontAsBitmap,
        align: TextAlignment = TextAlignment.TOP_LEFT,
        width: Double = 100.0,
        height: Double = 100.0,
        color: RGBA? = null,
        autoSize: Boolean = false,
    ) : this(RichTextData(text, font, textSize, color = color), align, width, height, autoSize)

    override var text: String
        get() = richText.text
        set(value) { richText = richText.withText(value) }

    override var textSize: Double
        get() = richText.defaultStyle.textSize
        set(value) { richText = richText.withStyle(richText.defaultStyle.copy(textSize = value)) }

    var textColor: RGBA?
        get() = richText.defaultStyle.color
        set(value) { richText = richText.withStyle(richText.defaultStyle.copy(color = value)) }

    var textFont: Font
        get() = richText.defaultStyle.font
        set(value) { richText = richText.withStyle(richText.defaultStyle.copy(font = value)) }

    override var color: RGBA by ::colorMul
    override var font: Resourceable<out Font>
        get() = textFont
        set(value) {
            textFont = value as Font
        }

    @Deprecated("")
    var graphicsRenderer: GraphicsRenderer = GraphicsRenderer.SYSTEM

    var verticalAlign: VerticalAlign
        get() = textAlignment.vertical
        set(value) { textAlignment = textAlignment.withVertical(value) }

    var horizontalAlign: HorizontalAlign
        get() = textAlignment.horizontal
        set(value) { textAlignment = textAlignment.withHorizontal(value) }

    var autoScaling: Boolean = true
    var smoothing: Boolean = true
    var useNativeRendering: Boolean = true

    fun setFormat(face: Font = this.textFont, size: Int = this.textSize.toInt(), color: RGBA? = this.textColor, align: TextAlignment = this.textAlignment) {
        richText = richText.withStyle(richText.defaultStyle.copy(
            font = face,
            textSize = size.toDouble(),
            color = color,
        ))
        this.textAlignment = align
    }

    fun setTextBounds(bounds: IRectangle) {
        setSize(bounds.width, bounds.height)
    }

    override fun getGlyphMetrics(): TextMetricsResult = getCachedPlacements().toTextMetricsResult()
}

open class TextBlock(
    text: RichTextData = RichTextData("", RichTextData.Style.DEFAULT_AS_BITMAP),
    align: TextAlignment = TextAlignment.TOP_LEFT,
    width: Double = 100.0,
    height: Double = 100.0,
    autoSize: Boolean = false,
) : BaseText(text, align, width, height, autoSize)

open class BaseText(
    text: RichTextData = RichTextData("", RichTextData.Style.DEFAULT_AS_BITMAP),
    align: TextAlignment = TextAlignment.TOP_LEFT,
    width: Double = 100.0,
    height: Double = 100.0,
    autoSize: Boolean = false,
) : UIView(width, height), ViewLeaf {
    private var dirty = true

    @ViewProperty
    var richText: RichTextData = text; set(value) { field = value; invalidateText() }

    @ViewProperty
    @ViewPropertyProvider(TextAlignment.Provider::class)
    var align: TextAlignment = align; set(value) { field = value; invalidProps() }

    @Deprecated("")
    var alignment: TextAlignment by ::align

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
    var padding: IMargin = IMargin.EMPTY; set(value) { field = value; invalidProps() }
    @ViewProperty
    var autoSize: Boolean = autoSize; set(value) { field = value; invalidateText() }
    //@ViewProperty(min = 0.0, max = 10.0, clampMin = true)
    //var textRange: IntRange = ALL_TEXT_RANGE; set(value) { field = value; invalidateText() }
    @ViewProperty(min = 0.0, max = 10.0, clampMin = true)
    var textRangeStart: Int = 0; set(value) { field = value; invalidateText() }
    @ViewProperty(min = 0.0, max = 10.0, clampMin = true)
    var textRangeEnd: Int = Int.MAX_VALUE; set(value) { field = value; invalidateText() }
    var plainText: String
        get() = richText.text
        set(value) {
            richText = RichTextData(value, style = richText.defaultStyle)
        }
    private var image: Image? = null
    private var allBitmap: Boolean? = null
        get() {
            if (field == null) field = richText.allFonts.all { it is BitmapFont }
            return field!!
        }

    private fun invalidateText() {
        invalidProps()
        if (autoSize) {
            setSize(richText.width, richText.height)
        }
        allBitmap = null
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
        image?.program = (richText.defaultStyle.font as? BitmapFont?)?.agProgram
        bmp.context2d {
            drawRichText(
                richText,
                bounds = MRectangle.fromBounds(padding.left, padding.top, width - padding.right, height - padding.bottom),
                includePartialLines = includePartialLines, wordWrap = wordWrap, ellipsis = ellipsis, align = align,
                fill = fill, stroke = stroke, includeFirstLineAlways = true,
                textRangeStart = textRangeStart, textRangeEnd = textRangeEnd
            )
        }
    }

    private var placements: RichTextDataPlacements? = null

    protected fun getCachedPlacements(): RichTextDataPlacements {
        if (dirty || placements == null) {
            dirty = false
            placements = richText.place(MRectangle(padding.left, padding.top, width - padding.right, height - padding.bottom), wordWrap, includePartialLines, ellipsis, fill, stroke, align, includeFirstLineAlways = includeFirstLineAlways)
        }
        return placements!!
    }

    override fun renderInternal(ctx: RenderContext) {
        if (allBitmap == true) {
            val placements = getCachedPlacements()
            image?.removeFromParent()
            image = null
            //if (textRange != ALL_TEXT_RANGE) println("textRange=$textRange")
            renderCtx2d(ctx) {
                it.drawText(placements, textRangeStart = textRangeStart, textRangeEnd = textRangeEnd)
            }
        } else {
            ensureTexture()
        }
        super.renderInternal(ctx)
    }

    companion object {
        val ALL_TEXT_RANGE = 0 until Int.MAX_VALUE
    }
}

fun <T : BaseText> T.autoSize(size: Boolean): T {
    this.autoSize = size
    return this
}
