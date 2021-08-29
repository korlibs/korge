package com.soywiz.korge.view

import com.soywiz.korge.debug.*
import com.soywiz.korge.html.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.internal.InternalViewAutoscaling
import com.soywiz.korge.view.ktree.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.text.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.resources.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

/*
// Example:
val font = BitmapFont(DefaultTtfFont, 64.0)

var offset = 0.degrees
addUpdater { offset += 10.degrees }
text2("Hello World!", color = Colors.RED, font = font, renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
    transform.identity()
    val sin = sin(offset + (n * 360 / text.length).degrees)
    transform.rotate(15.degrees)
    transform.translate(0.0, sin * 16)
    transform.scale(1.0, 1.0 + sin * 0.1)
    put(c)
    advance(advance)
}).position(100, 100)
*/
inline fun Container.text(
    text: String, textSize: Double = Text.DEFAULT_TEXT_SIZE,
    color: RGBA = Colors.WHITE, font: Resourceable<out Font> = DefaultTtfFont,
    alignment: TextAlignment = TextAlignment.TOP_LEFT,
    renderer: TextRenderer<String> = DefaultStringTextRenderer,
    autoScaling: Boolean = Text.DEFAULT_AUTO_SCALING,
    block: @ViewDslMarker Text.() -> Unit = {}
): Text
    = Text(text, textSize, color, font, alignment, renderer, autoScaling).addTo(this, block)

open class Text(
    text: String, textSize: Double = DEFAULT_TEXT_SIZE,
    color: RGBA = Colors.WHITE, font: Resourceable<out Font> = DefaultTtfFont,
    alignment: TextAlignment = TextAlignment.TOP_LEFT,
    renderer: TextRenderer<String> = DefaultStringTextRenderer,
    autoScaling: Boolean = DEFAULT_AUTO_SCALING
) : Container(), ViewLeaf, IText {
    companion object {
        val DEFAULT_TEXT_SIZE = 16.0
        val DEFAULT_AUTO_SCALING = true
    }

    var smoothing: Boolean = true

    object Serializer : KTreeSerializerExt<Text>("Text", Text::class, { Text("Text") }, {
        add(Text::text, "Text")
        add(Text::fontSource)
        add(Text::textSize, DEFAULT_TEXT_SIZE)
        add(Text::autoScaling, DEFAULT_AUTO_SCALING)
        add(Text::verticalAlign, { VerticalAlign(it) }, { it.toString() })
        add(Text::horizontalAlign, { HorizontalAlign(it) }, { it.toString() })
        //view.fontSource = xml.str("fontSource", "")
    }) {
        override suspend fun ktreeToViewTree(xml: Xml, currentVfs: VfsFile): Text {
            return super.ktreeToViewTree(xml, currentVfs).also { view ->
                if ((view.fontSource ?: "").isNotBlank()) {
                    try {
                        view.forceLoadFontSource(currentVfs, view.fontSource)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private var cachedVersion = -1
    private var cachedVersionRenderer = -1
    private var version = 0

    var lineCount: Int = 0; private set

    override var text: String = text; set(value) { if (field != value) {
        field = value;
        updateLineCount()
        version++
    } }
    private fun updateLineCount() {
        lineCount = text.count { it == '\n' } + 1
    }
    init {
        updateLineCount()
    }
    var color: RGBA = color; set(value) { if (field != value) { field = value; version++ } }
    var font: Resourceable<out Font> = font; set(value) { if (field != value) { field = value; version++ } }
    var textSize: Double = textSize; set(value) { if (field != value) { field = value; version++ } }
    var fontSize: Double
        get() = textSize
        set(value) { textSize = value }
    var renderer: TextRenderer<String> = renderer; set(value) { if (field != value) { field = value; version++ } }

    var alignment: TextAlignment = alignment; set(value) { if (field != value) { field = value; version++ } }
    var horizontalAlign: HorizontalAlign
        get() = alignment.horizontal
        set(value) { alignment = alignment.withHorizontal(value) }
    var verticalAlign: VerticalAlign
        get() = alignment.vertical
        set(value) { alignment = alignment.withVertical(value) }

    private lateinit var textToBitmapResult: TextToBitmapResult
    private val container = container()
    private val bitmapFontActions = Text2TextRendererActions()
    private var fontLoaded: Boolean = false
    var autoScaling = autoScaling
    var preciseAutoscaling = false
        set(value) {
            field = value
            if (value) autoScaling = true
        }
    var fontSource: String? = null
        set(value) {
            field = value
            fontLoaded = false
        }

    // @TODO: Use, font: Resourceable<out Font>
    suspend fun forceLoadFontSource(currentVfs: VfsFile, sourceFile: String?) {
        fontSource = sourceFile
        fontLoaded = true
        if (sourceFile != null) {
            font = currentVfs["$sourceFile"].readFont()
        }
    }

    private val _textBounds = Rectangle(0, 0, 2048, 2048)
    var autoSize = true
    private var boundsVersion = -1
    val textBounds: Rectangle
        get() {
            getLocalBounds(_textBounds)
            return _textBounds
        }

    fun setFormat(face: Resourceable<out Font>? = this.font, size: Int = this.size, color: RGBA = this.color, align: TextAlignment = this.alignment) {
        this.font = face ?: DefaultTtfFont
        this.textSize = size.toDouble()
        this.color = color
        this.alignment = align
    }

    fun setFormat(format: Html.Format) {
        setFormat(format.computedFace, format.computedSize, format.computedColor, format.computedAlign)
    }

    fun setTextBounds(rect: Rectangle) {
        if (this._textBounds == rect && !autoSize) return
        this._textBounds.copyFrom(rect)
        autoSize = false
        boundsVersion++
        version++
    }

    fun unsetTextBounds() {
        if (autoSize) return
        autoSize = true
        boundsVersion++
        version++
    }

    override fun getLocalBoundsInternal(out: Rectangle) {
        _renderInternal(null)
        out.copyFrom(_textBounds)
    }

    private val tempMatrix = Matrix()

    //var newTvaRenderer = true
    var newTvaRenderer = false

    override fun renderInternal(ctx: RenderContext) {
        _renderInternal(ctx)
        while (imagesToRemove.isNotEmpty()) {
            ctx.agBitmapTextureManager.removeBitmap(imagesToRemove.removeLast())
        }
        //val tva: TexturedVertexArray? = null
        if (tva != null) {
            tempMatrix.copyFrom(globalMatrix)
            tempMatrix.pretranslate(container.x, container.y)
            ctx.useBatcher { batch ->
                batch.setStateFast((font as BitmapFont).baseBmp, smoothing, renderBlendMode.factors, null)
                batch.drawVertices(tva!!, tempMatrix)
            }
        } else {
            super.renderInternal(ctx)
        }
    }

    var cachedVersionGlyphMetrics = -1
    private var _textMetricsResult: TextMetricsResult? = null

    fun getGlyphMetrics(): TextMetricsResult {
        if (cachedVersionGlyphMetrics != version) {
            cachedVersionGlyphMetrics = version
            _textMetricsResult = font.getOrNull()?.measureTextGlyphs(fontSize, text, renderer)
        }
        return _textMetricsResult ?: error("Must ensure font is resolved before calling getGlyphMetrics")
    }

    private val tempBmpEntry = Text2TextRendererActions.Entry()
    private val fontMetrics = FontMetrics()
    private val textMetrics = TextMetrics()
    private var lastAutoScaling: Boolean? = null
    private var lastSmoothing: Boolean? = null
    private var lastNativeRendering: Boolean? = null
    private var tva: TexturedVertexArray? = null
    private val identityMat = Matrix()

    fun _renderInternal(ctx: RenderContext?) {
        if (ctx != null) {
            val fontSource = fontSource
            if (!fontLoaded && fontSource != null) {
                fontLoaded = true
                launchImmediately(ctx.coroutineContext) {
                    forceLoadFontSource(ctx.views!!.currentVfs, fontSource)
                }
            }
        }
        container.colorMul = color
        val font = this.font.getOrNull()

        if (autoSize && font is Font && boundsVersion != version) {
            boundsVersion = version
            val metrics = font.getTextBounds(textSize, text, out = textMetrics, renderer = renderer)
            _textBounds.copyFrom(metrics.bounds)
            _textBounds.height = font.getFontMetrics(textSize, metrics = fontMetrics).lineHeight * lineCount
            _textBounds.x = -alignment.horizontal.getOffsetX(_textBounds.width) + metrics.left
            _textBounds.y = alignment.vertical.getOffsetY(_textBounds.height, -metrics.ascent)
        }

        when (font) {
            null -> Unit
            is BitmapFont -> {
                val rversion = renderer.version
                if (lastSmoothing != smoothing || cachedVersion != version || cachedVersionRenderer != rversion) {
                    lastSmoothing = smoothing
                    cachedVersionRenderer = rversion
                    cachedVersion = version

                    _staticImage = null
                    bitmapFontActions.x = 0.0
                    bitmapFontActions.y = 0.0

                    bitmapFontActions.mreset()
                    bitmapFontActions.verticalAlign = verticalAlign
                    bitmapFontActions.horizontalAlign = horizontalAlign
                    renderer.invoke(bitmapFontActions, text, textSize, font)
                    while (container.numChildren < bitmapFontActions.size) {
                        container.image(Bitmaps.transparent)
                    }
                    while (container.numChildren > bitmapFontActions.size) {
                        container[container.numChildren - 1].removeFromParent()
                    }
                    //println(font.glyphs['H'.toInt()])
                    //println(font.glyphs['a'.toInt()])
                    //println(font.glyphs['g'.toInt()])

                    val textWidth = bitmapFontActions.x

                    val dx = -textWidth * horizontalAlign.ratio

                    if (newTvaRenderer) {
                        this.tva = TexturedVertexArray.forQuads(bitmapFontActions.size)
                    }

                    for (n in 0 until bitmapFontActions.size) {
                        val entry = bitmapFontActions.read(n, tempBmpEntry)
                        if (newTvaRenderer) {
                            tva?.quad(n * 4, entry.x + dx, entry.y, entry.tex.width * entry.sx, entry.tex.height * entry.sy, identityMat, entry.tex, renderColorMul, renderColorAdd)
                        } else {
                            val it = (container[n] as Image)
                            it.anchor(0, 0)
                            it.smoothing = smoothing
                            it.bitmap = entry.tex
                            it.x = entry.x + dx
                            it.y = entry.y
                            it.scaleX = entry.sx
                            it.scaleY = entry.sy
                            it.rotation = entry.rot
                        }
                    }

                    setContainerPosition(0.0, 0.0, font.base)
                }
            }
            else -> {
                val onRenderResult = autoscaling.onRender(autoScaling, preciseAutoscaling, this.globalMatrix)
                val lastAutoScalingResult = lastAutoScaling != autoScaling
                if (onRenderResult || lastAutoScalingResult || lastSmoothing != smoothing || lastNativeRendering != useNativeRendering) {
                    version++
                    //println("UPDATED VERSION[$this] lastAutoScaling=$lastAutoScaling, autoScaling=$autoScaling, onRenderResult=$onRenderResult, lastAutoScalingResult=$lastAutoScalingResult")
                    lastNativeRendering = useNativeRendering
                    lastAutoScaling = autoScaling
                    lastSmoothing = smoothing
                }

                if (cachedVersion != version) {
                    cachedVersion = version
                    val realTextSize = textSize * autoscaling.renderedAtScaleXY
                    //println("realTextSize=$realTextSize")
                    textToBitmapResult = when {
                        text.isNotEmpty() -> {
                            font.renderTextToBitmap(
                                realTextSize, text,
                                paint = Colors.WHITE, fill = true, renderer = renderer,
                                //background = Colors.RED,
                                nativeRendering = useNativeRendering, drawBorder = true
                            )
                        }
                        else -> {
                            TextToBitmapResult(Bitmaps.transparent.bmp, FontMetrics(), TextMetrics(), emptyList())
                        }
                    }

                    //println("RENDER TEXT: '$text'")

                    val met = textToBitmapResult.metrics
                    val x = -horizontalAlign.getOffsetX(met.width) + met.left
                    val y = verticalAlign.getOffsetY(met.lineHeight, -(met.ascent))

                    if (_staticImage == null) {
                        container.removeChildren()
                        _staticImage = container.image(textToBitmapResult.bmp)
                    } else {
                        imagesToRemove.add(_staticImage!!.bitmap.bmpBase)
                        _staticImage!!.bitmap = textToBitmapResult.bmp.slice()
                    }
                    val mscale = 1.0 / autoscaling.renderedAtScaleXY
                    _staticImage!!.scale(mscale, mscale)
                    setContainerPosition(x * mscale, y * mscale, font.getFontMetrics(fontSize, fontMetrics).baseline)
                }
                _staticImage?.smoothing = smoothing
            }
        }
    }

    var useNativeRendering: Boolean = true

    private val autoscaling = InternalViewAutoscaling()

    private fun setContainerPosition(x: Double, y: Double, baseline: Double) {
        if (autoSize) {
            setRealContainerPosition(x, y)
        } else {
            //staticImage?.position(x + alignment.horizontal.getOffsetX(textBounds.width), y + alignment.vertical.getOffsetY(textBounds.height, font.getFontMetrics(fontSize).baseline))
            setRealContainerPosition(x + alignment.horizontal.getOffsetX(_textBounds.width), y - alignment.vertical.getOffsetY(_textBounds.height, baseline))
        }
    }

    private fun setRealContainerPosition(x: Double, y: Double) {
        container.position(x, y)
    }

    private val imagesToRemove = arrayListOf<Bitmap>()

    internal var _staticImage: Image? = null

    val staticImage: Image? get() {
        _renderInternal(null)
        return _staticImage
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection("Text") {
            uiEditableValue(::text)
            uiEditableValue(::textSize, min= 1.0, max = 300.0)
            uiEditableValue(::autoScaling)
            uiEditableValue(::verticalAlign, values = { listOf(VerticalAlign.TOP, VerticalAlign.MIDDLE, VerticalAlign.BASELINE, VerticalAlign.BOTTOM) })
            uiEditableValue(::horizontalAlign, values = { listOf(HorizontalAlign.LEFT, HorizontalAlign.CENTER, HorizontalAlign.RIGHT, HorizontalAlign.JUSTIFY) })
            uiEditableValue(::fontSource, UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.extensionLC == "ttf" || it.extensionLC == "fnt"
            })
        }
        super.buildDebugComponent(views, container)
    }
}

fun <T : Text> T.autoSize(value: Boolean): T {
    this.autoSize = value
    return this
}
