package com.soywiz.korge.view

import com.soywiz.korag.shader.*
import com.soywiz.korge.debug.UiTextEditableValue
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.html.Html
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.TexturedVertexArray
import com.soywiz.korge.text.*
import com.soywiz.korge.view.filter.backdropFilter
import com.soywiz.korge.view.filter.filter
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.FontMetrics
import com.soywiz.korim.font.TextMetrics
import com.soywiz.korim.font.TextMetricsResult
import com.soywiz.korim.font.getTextBounds
import com.soywiz.korim.font.getTextBoundsWithGlyphs
import com.soywiz.korim.font.readFont
import com.soywiz.korim.paint.Paint
import com.soywiz.korim.paint.Stroke
import com.soywiz.korim.text.CurveTextRenderer
import com.soywiz.korim.text.DefaultStringTextRenderer
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.Text2TextRendererActions
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.text.TextRenderer
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korim.text.aroundPath
import com.soywiz.korim.text.invoke
import com.soywiz.korim.text.withSpacing
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.extensionLC
import com.soywiz.korio.resources.Resourceable
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korui.UiContainer

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
    fill: Paint? = null, stroke: Stroke? = null,
    block: @ViewDslMarker Text.() -> Unit = {}
): Text
    = Text(text, textSize, color, font, alignment, renderer, autoScaling, fill, stroke).addTo(this, block)

open class Text(
    text: String, textSize: Double = DEFAULT_TEXT_SIZE,
    color: RGBA = Colors.WHITE, font: Resourceable<out Font> = DefaultTtfFont,
    alignment: TextAlignment = TextAlignment.TOP_LEFT,
    renderer: TextRenderer<String> = DefaultStringTextRenderer,
    autoScaling: Boolean = DEFAULT_AUTO_SCALING,
    fill: Paint? = null, stroke: Stroke? = null,
) : Container(), IText, ViewLeaf {
    companion object {
        val DEFAULT_TEXT_SIZE = 16.0
        val DEFAULT_AUTO_SCALING = true
    }

    var smoothing: Boolean = true

    private var cachedVersion = -1
    private var cachedVersionRenderer = -1
    private var version = 0

    var lineCount: Int = 0; private set

    override var text: String = text; set(value) { if (field != value) {
        field = value;
        updateLineCount()
        version++
        invalidate()
    } }
    private fun updateLineCount() {
        lineCount = text.count { it == '\n' } + 1
    }
    init {
        updateLineCount()
    }
    var fillStyle: Paint? = fill ?: color; set(value) { if (field != value) { field = value; version++ } }
    var stroke: Stroke? = stroke; set(value) { if (field != value) { field = value; version++ } }

    var color: RGBA
        get() = (fillStyle as? RGBA?) ?: Colors.WHITE
        set(value) { fillStyle = value }

    var font: Resourceable<out Font> = font; set(value) {
        if (field != value) {
            field = value
            version++
            invalidate()
        }
        //printStackTrace("setfont=$field")
    }
    var textSize: Double = textSize; set(value) {
        if (field != value) {
            field = value
            version++
            invalidate()
        }
    }
    var fontSize: Double by ::textSize

    var renderer: TextRenderer<String> = renderer; set(value) {
        if (field != value) {
            field = value
            version++
            invalidate()
        }
    }

    //private var cachedRendererVersionInvalidated: Int = -1
    //init {
    //    addUpdater {
    //        val version = renderer.version
    //        if (version != cachedRendererVersionInvalidated) {
    //            cachedRendererVersionInvalidated = version
    //            //println("renderer.version: $version != $cachedVersionRenderer")
    //            invalidate()
    //        }
    //    }
    //}

    var alignment: TextAlignment = alignment
        set(value) {
            if (field == value) return
            field = value
            //println("Text.alignment=$field")
            version++
            invalidate()
        }
    var horizontalAlign: HorizontalAlign
        get() = alignment.horizontal
        set(value) { alignment = alignment.withHorizontal(value) }
    var verticalAlign: VerticalAlign
        get() = alignment.vertical
        set(value) { alignment = alignment.withVertical(value) }

    //private lateinit var textToBitmapResult: TextToBitmapResult
    private val container = container()
    private val bitmapFontActions = Text2TextRendererActions()
    private var fontLoaded: Boolean = false
    var autoScaling = autoScaling
        set(value) {
            field = value
            invalidate()
        }
    var preciseAutoscaling = false
        set(value) {
            field = value
            if (value) autoScaling = true
            invalidate()
        }
    var fontSource: String? = null
        set(value) {
            field = value
            fontLoaded = false
            invalidate()
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

    fun setFormat(face: Resourceable<out Font>? = this.font, size: Int = this.textSize.toInt(), color: RGBA = this.color, align: TextAlignment = this.alignment) {
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
        invalidate()
    }

    fun unsetTextBounds() {
        if (autoSize) return
        autoSize = true
        boundsVersion++
        version++
        invalidate()
    }

    override fun getLocalBoundsInternal(out: Rectangle) {
        _renderInternal(null)
        if (filter != null || backdropFilter != null) {
            super.getLocalBoundsInternal(out) // This is required for getting proper bounds when glyphs are transformed
        } else {
            out.copyFrom(_textBounds)
        }
    }

    private val tempMatrix = Matrix()

    //var newTvaRenderer = true
    var newTvaRenderer = false

    override fun renderInternal(ctx: RenderContext) {
        _renderInternal(ctx)
        //val tva: TexturedVertexArray? = null
        val tva = tva
        if (tva != null) {
            tempMatrix.copyFrom(globalMatrix)
            tempMatrix.pretranslate(container.x, container.y)
            ctx.useBatcher { batch ->
                val bmpfont = font as BitmapFont
                val tex = bmpfont.baseBmp
                batch.setStateFast(tex, smoothing, renderBlendMode, bmpfont.program, icount = tva.icount, vcount = tva.vcount)
                batch.drawVertices(tva, tempMatrix, premultiplied = tex.premultiplied, wrap = false)
            }
        } else {
            super.renderInternal(ctx)
        }
    }

    var cachedVersionGlyphMetrics = -1
    private var _textMetricsResult: TextMetricsResult? = null

    fun getGlyphMetrics(): TextMetricsResult {
        _renderInternal(null)
        if (cachedVersionGlyphMetrics != version) {
            cachedVersionGlyphMetrics = version
            _textMetricsResult = font.getOrNull()?.getTextBoundsWithGlyphs(fontSize, text, renderer)
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

    val BitmapFont.program: Program? get() = when (distanceField) {
        "msdf" -> MsdfRender.PROGRAM_MSDF
        "psdf" -> MsdfRender.PROGRAM_SDF_A
        "sdf" -> MsdfRender.PROGRAM_SDF_A
        else -> null
    }

    var graphicsRenderer: GraphicsRenderer = GraphicsRenderer.SYSTEM
        set(value) {
            field = value
            _staticGraphics?.renderer = value
        }

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
        //container.colorMul = color
        val font: Font? = this.font.getOrNull()

        //println("font=$font")

        if (autoSize && font is Font && boundsVersion != version) {
            boundsVersion = version
            val metrics = font.getTextBounds(textSize, text, out = textMetrics, renderer = renderer, align = alignment)
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

                    if (_staticGraphics != null) {
                        _staticGraphics = null
                        container.removeChildren()
                    }
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

                    val program = font.program
                    for (n in 0 until bitmapFontActions.size) {
                        val entry = bitmapFontActions.read(n, tempBmpEntry)
                        if (newTvaRenderer) {
                            tva?.quad(n * 4, entry.x + dx, entry.y, entry.tex.width * entry.sx, entry.tex.height * entry.sy, identityMat, entry.tex, renderColorMul, renderColorAdd)
                        } else {
                            val it = (container[n] as Image)
                            it.program = program
                            it.colorMul = color // @TODO: When doing black, all colors are lost even if the glyph is a colorized image
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
                if (lastSmoothing != smoothing || lastNativeRendering != useNativeRendering) {
                    version++
                    //println("UPDATED VERSION[$this] lastAutoScaling=$lastAutoScaling, autoScaling=$autoScaling, onRenderResult=$onRenderResult, lastAutoScalingResult=$lastAutoScalingResult")
                    lastNativeRendering = useNativeRendering
                    lastAutoScaling = autoScaling
                    lastSmoothing = smoothing
                }

                if (cachedVersion != version) {
                    //println("UPDATED VERSION: $cachedVersion!=$version")
                    cachedVersion = version
                    val realTextSize = textSize

                    if (_staticGraphics == null) {
                        container.removeChildren()
                        //_staticGraphics = container.gpuGraphics {  }
                        _staticGraphics = container.graphics(renderer = graphicsRenderer) { }
                        _staticGraphics?.autoScaling = true
                    }

                    //println("alignment=$alignment")
                    val metrics = _staticGraphics!!.updateShape {
                        //if (fill != null) {
                        //    fillStroke(fill, stroke) {
                        //        this.text(
                        //            text = this@Text.text,
                        //            x = 0.0, y = 0.0,
                        //            textSize = realTextSize,
                        //            font = font as VectorFont,
                        //            renderer = this@Text.renderer,
                        //            align = this@Text.alignment,
                        //        )
                        //    }
                        //}
                        drawText(
                            text = this@Text.text,
                            x = 0.0, y = 0.0,
                            size = realTextSize,
                            font = font,
                            paint = this@Text.color,
                            renderer = this@Text.renderer,
                            //align = TextAlignment.TOP_LEFT,
                            align = this@Text.alignment,
                            outMetrics = TextMetricsResult(),
                            //outMetrics = this@Text._textMetricsResult ?: TextMetricsResult(),
                            fillStyle = this@Text.fillStyle,
                            stroke = this@Text.stroke,
                        )
                    }
                    // Optimize since we already have the metrics to avoid recomputing them later
                    cachedVersionGlyphMetrics = version
                    _textMetricsResult = metrics

                    //val met = metrics!!.metrics
                    //val x = -horizontalAlign.getOffsetX(met.width)// + met.left
                    //val y = verticalAlign.getOffsetY(met.lineHeight, -(met.ascent))
                    //setContainerPosition(x * 1.0, y * 1.0, font.getFontMetrics(fontSize, fontMetrics).baseline)
                    //println("alignment=$alignment, horizontalAlign=$horizontalAlign, verticalAlign=$verticalAlign")
                    //setContainerPosition(x, y, font.getFontMetrics(fontSize, fontMetrics).baseline)
                    setContainerPosition(0.0, 0.0, font.getFontMetrics(fontSize, fontMetrics).baseline)

                }
                //_staticImage?.smoothing = smoothing
                _staticGraphics?.smoothing = smoothing
            }
        }
    }

    var useNativeRendering: Boolean = true

    private fun setContainerPosition(x: Double, y: Double, baseline: Double) {
        if (autoSize) {
            setRealContainerPosition(x, y)
        } else {
            //staticImage?.position(x + alignment.horizontal.getOffsetX(textBounds.width), y + alignment.vertical.getOffsetY(textBounds.height, font.getFontMetrics(fontSize).baseline))

            // @TODO: Fix this!
            setRealContainerPosition(x + alignment.horizontal.getOffsetX(_textBounds.width), y - alignment.vertical.getOffsetY(_textBounds.height, baseline))
        }
    }

    private fun setRealContainerPosition(x: Double, y: Double) {
        container.position(x, y)
    }

    internal var _staticGraphics: Graphics? = null

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection("Text") {
            uiEditableValue(::text)
            uiEditableValue(::textSize, min= 1.0, max = 300.0)
            uiEditableValue(::verticalAlign, VerticalAlign.ALL)
            uiEditableValue(::horizontalAlign, HorizontalAlign.ALL)
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

fun <T : Text> T.textSpacing(spacing: Double): T {
    renderer = renderer.withSpacing(spacing)
    return this
}

fun <T : Text> T.aroundPath(path: VectorPath?): T {
    aroundPath = path
    return this
}

var <T : Text> T.aroundPath: VectorPath?
    get() {
        val currentRenderer = renderer
        return if (currentRenderer is CurveTextRenderer<*>) currentRenderer.path else null
    }
    set(value) {
        val currentRenderer = renderer
        if (value == null) {
            if (currentRenderer is CurveTextRenderer<*>) {
                renderer = (currentRenderer as CurveTextRenderer<String>).original
            }
        } else {
            renderer = currentRenderer.aroundPath(value)
        }
    }
