package korlibs.korge.view

import korlibs.image.bitmap.*
import korlibs.image.font.*
import korlibs.image.paint.*
import korlibs.image.text.*
import korlibs.korge.render.*
import korlibs.korge.ui.*
import korlibs.korge.view.property.*
import korlibs.math.*
import korlibs.math.geom.*

inline fun Container.textBlock(
    text: RichTextData = RichTextData("", textSize = 16.0, font = DefaultTtfFontAsBitmap),
    align: TextAlignment = TextAlignment.TOP_LEFT,
    size: Size = Size(100, 100),
    block: @ViewDslMarker TextBlock.() -> Unit = {}
): TextBlock
    = TextBlock(text, align, size).addTo(this, block)

class TextBlock(
    text: RichTextData = RichTextData("", textSize = 16.0, font = DefaultTtfFontAsBitmap),
    align: TextAlignment = TextAlignment.TOP_LEFT,
    size: Size = Size(100, 100),
) : UIView(size), ViewLeaf {
    private var dirty = true

    @ViewProperty
    var text: RichTextData = text; set(value) { if (field != value) { field = value; invalidateText() } }

    @ViewProperty
    @ViewPropertyProvider(TextAlignmentProvider::class)
    var align: TextAlignment = align; set(value) { if (field != value) { field = value; invalidProps() } }

    @ViewProperty
    var includePartialLines: Boolean = false; set(value) { if (field != value) { field = value; invalidProps() } }
    @ViewProperty
    var includeFirstLineAlways: Boolean = true; set(value) { if (field != value) { field = value; invalidProps() } }
    @ViewProperty
    var fill: Paint? = colorMul; set(value) { if (field != value) { field = value; invalidProps() } }
    @ViewProperty
    var stroke: Stroke? = null; set(value) { if (field != value) { field = value; invalidProps() } }
    @ViewProperty
    var wordWrap: Boolean = true; set(value) { if (field != value) { field = value; invalidProps() } }
    @ViewProperty
    var ellipsis: String? = "..."; set(value) { if (field != value) { field = value; invalidProps() } }
    @ViewProperty
    var padding: Margin = Margin.ZERO; set(value) { if (field != value) { field = value; invalidProps() } }
    @ViewProperty
    var autoSize: Boolean = false; set(value) { if (field != value) { field = value; invalidateText() } }
    //@ViewProperty(min = 0.0, max = 10.0, clampMin = true)
    //var textRange: IntRange = ALL_TEXT_RANGE; set(value) { field = value; invalidateText() }
    @ViewProperty(min = 0.0, max = 10.0, clampMin = true)
    var textRangeStart: Int = 0; set(value) { if (field != value) { field = value; invalidateText() } }
    @ViewProperty(min = 0.0, max = 10.0, clampMin = true)
    var textRangeEnd: Int = Int.MAX_VALUE; set(value) { if (field != value) { field = value; invalidateText() } }
    var plainText: String
        get() = text.text
        set(value) {
            if (plainText != value) text = RichTextData(value, style = text.defaultStyle)
        }
    @ViewProperty
    var smoothing: Boolean = true
        set(value) {
            if (field != value) { field = value; invalidProps() }
        }
    private var image: Image? = null
    private var allBitmap: Boolean? = null
        get() {
            if (field == null) field = text.allFonts.all { it is BitmapFont }
            return field!!
        }

    private fun invalidateText() {
        invalidProps()
        if (autoSize) {
            size(text.width, text.height)
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
        image?.program = (text.defaultStyle.font as? BitmapFont?)?.agProgram
        image?.smoothing = smoothing
        bmp.context2d {
            drawRichText(
                text,
                bounds = Rectangle.fromBounds(padding.left, padding.top, width - padding.right, height - padding.bottom),
                includePartialLines = includePartialLines, wordWrap = wordWrap, ellipsis = ellipsis, align = align,
                fill = fill, stroke = stroke, includeFirstLineAlways = true,
                textRangeStart = textRangeStart, textRangeEnd = textRangeEnd,
            )
        }
    }

    private var placements: RichTextDataPlacements? = null

    override fun renderInternal(ctx: RenderContext) {
        if (allBitmap == true) {
            if (dirty || placements == null) {
                dirty = false
                placements = text.place(Rectangle(padding.left, padding.top, (width - padding.right), (height - padding.bottom)), wordWrap, includePartialLines, ellipsis, fill, stroke, align, includeFirstLineAlways = includeFirstLineAlways)
            }
            image?.removeFromParent()
            image = null
            //if (textRange != ALL_TEXT_RANGE) println("textRange=$textRange")
            renderCtx2d(ctx) {
                it.drawText(placements!!, textRangeStart = textRangeStart, textRangeEnd = textRangeEnd, filtering = smoothing)
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
