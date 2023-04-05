package korlibs.korge.view

import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.text.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.property.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.paint.*
import korlibs.image.text.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.resources.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

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
    color: RGBA = Colors.WHITE, font: Resourceable<out Font> = DefaultTtfFontAsBitmap,
    alignment: TextAlignment = TextAlignment.TOP_LEFT,
    renderer: TextRenderer<String> = DefaultStringTextRenderer,
    autoScaling: Boolean = Text.DEFAULT_AUTO_SCALING,
    fill: Paint? = null, stroke: Stroke? = null,
    block: @ViewDslMarker Text.() -> Unit = {}
): Text
    = Text(text, textSize, color, font, alignment, renderer, autoScaling, fill, stroke).addTo(this, block)

open class Text(
    text: String, textSize: Double = DEFAULT_TEXT_SIZE,
    color: RGBA = Colors.WHITE, font: Resourceable<out Font> = DefaultTtfFontAsBitmap,
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

    @ViewProperty
    override var text: String = text; set(value) { if (field != value) {
        field = value;
        updateLineCount()
        version++
        invalidate()
        invalidateRender()
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

    @ViewProperty(min = 1.0, max = 300.0)
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
    @ViewProperty
    @ViewPropertyProvider(provider = HorizontalAlign.Provider::class)
    var horizontalAlign: HorizontalAlign
        get() = alignment.horizontal
        set(value) { alignment = alignment.withHorizontal(value) }

    @ViewProperty
    @ViewPropertyProvider(provider = VerticalAlign.Provider::class)
    var verticalAlign: VerticalAlign
        get() = alignment.vertical
        set(value) { alignment = alignment.withVertical(value) }

    //private lateinit var textToBitmapResult: TextToBitmapResult
    private val container = container()
    private val bitmapFontActions = Text2TextRendererActions()
    private var fontLoaded: Boolean = false
    var autoScaling: Boolean = autoScaling
        set(value) {
            field = value
            invalidate()
        }
    var preciseAutoscaling: Boolean = false
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

    private var _textBounds = Rectangle(0, 0, 2048, 2048)
    var autoSize = true
    private var boundsVersion = -1
    val textBounds: Rectangle
        get() {
            _textBounds = getLocalBounds()
            return _textBounds
        }

    fun setFormat(face: Resourceable<out Font>? = this.font, size: Int = this.textSize.toInt(), color: RGBA = this.color, align: TextAlignment = this.alignment) {
        this.font = face ?: DefaultTtfFontAsBitmap
        this.textSize = size.toDouble()
        this.color = color
        this.alignment = align
    }

    fun setTextBounds(rect: Rectangle) {
        if (this._textBounds == rect && !autoSize) return
        this._textBounds = rect
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

    override fun getLocalBoundsInternal(): Rectangle {
        _renderInternal(null)
        if (filter != null || backdropFilter != null) {
            return super.getLocalBoundsInternal() // This is required for getting proper bounds when glyphs are transformed
        } else {
            return _textBounds
        }
    }

    private val tempMatrix = MMatrix()

    override fun renderInternal(ctx: RenderContext) {
        _renderInternal(ctx)
        //val tva: TexturedVertexArray? = null
        val tva = tva
        if (tva != null) {
            tempMatrix.copyFrom(globalMatrix)
            tempMatrix.pretranslate(container.xD, container.yD)
            ctx.useBatcher { batch ->
                val bmpfont = font as BitmapFont
                val tex = bmpfont.baseBmp
                batch.setStateFast(tex, smoothing, renderBlendMode, bmpfont.agProgram, icount = tva.icount, vcount = tva.vcount)
                batch.drawVertices(tva, tempMatrix.immutable)
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
            _textMetricsResult = font.getOrNull()?.getTextBoundsWithGlyphs(fontSize, text, renderer, alignment)
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
    private val identityMat = MMatrix()

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
            _textBounds = Rectangle(
                metrics.left,
                alignment.vertical.getOffsetY(metrics.height, -metrics.ascent),
                metrics.width,
                font.getFontMetrics(textSize, metrics = fontMetrics).lineHeight * lineCount
            )
            //println("autoSize: _textBounds=$_textBounds, ${alignment.horizontal}, ${alignment.horizontal.getOffsetX(metrics.width)} + ${metrics.left}")
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
                    bitmapFontActions.mreset()
                    bitmapFontActions.align = TextAlignment.BASELINE_LEFT
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

                    val bounds = bitmapFontActions.getBounds()
                    val firstBounds = bitmapFontActions.getGlyphBounds(0)
                    val lineInfos = bitmapFontActions.getLineInfos()
                    val firstLineInfos = lineInfos.firstOrNull() ?: Text2TextRendererActions.LineInfo()
                    val totalHeight = lineInfos.sumOf { it.maxLineHeight }
                    val textWidth = bounds.width
                    val textHeight = bounds.height

                    //println("lineInfos=$lineInfos")
                    //println("Text.BitmapFont: bounds=$bounds, firstBounds=$firstBounds, textWidth=$textWidth, textHeight=$textHeight, verticalAlign=$verticalAlign")

                    //val dx = (-_textBounds.width - textWidth) * horizontalAlign.ratio
                    val dx = _textBounds.x
                    val dy = when (verticalAlign) {
                        VerticalAlign.BASELINE -> 0.0
                        VerticalAlign.TOP -> firstLineInfos.maxTop
                        else -> firstLineInfos.maxTop - (totalHeight) * verticalAlign.ratioFake
                    }

                    val program = font.agProgram
                    //val program = null
                    for (n in 0 until bitmapFontActions.size) {
                        val entry = bitmapFontActions.read(n, tempBmpEntry)
                        val it = (container[n] as Image)
                        it.program = program
                        it.colorMul = color // @TODO: When doing black, all colors are lost even if the glyph is a colorized image
                        it.anchor(0, 0)
                        it.smoothing = smoothing
                        it.bitmap = entry.tex
                        it.xD = entry.x + dx
                        it.yD = entry.y + dy
                        it.scaleXD = entry.sx
                        it.scaleYD = entry.sy
                        it.rotation = entry.rot
                    }

                    //setContainerPosition(0.0, 0.0, font.base)
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
            setRealContainerPosition(x + alignment.horizontal.getOffsetX(_textBounds.widthD), y - alignment.vertical.getOffsetY(_textBounds.heightD, baseline))
        }
    }

    private fun setRealContainerPosition(x: Double, y: Double) {
        container.position(x, y)
    }

    internal var _staticGraphics: Graphics? = null

    @Suppress("unused")
    @ViewProperty
    @ViewPropertyFileRef(["ttf", "fnt", "otf"])
    private var fontSourceFile: String? by this::fontSource
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


val BitmapFont.agProgram: Program? get() = when (distanceField) {
    "msdf" -> MsdfRender.PROGRAM_MSDF
    "psdf" -> MsdfRender.PROGRAM_SDF_A
    "sdf" -> MsdfRender.PROGRAM_SDF_A
    else -> null
}
