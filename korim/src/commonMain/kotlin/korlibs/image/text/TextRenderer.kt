package korlibs.image.text

import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.paint.*
import korlibs.io.lang.*
import korlibs.io.util.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.vector.*
import kotlin.math.*
import kotlin.native.concurrent.*

interface ITextRendererActions {
    var x: Double
    var y: Double
    val lineHeight: Double
    var currentLineNum: Int
    var transform: Matrix

    fun getKerning(leftCodePoint: Int, rightCodePoint: Int): Double
    fun getGlyphMetrics(reader: WStringReader?, codePoint: Int): GlyphMetrics
    fun reset() {
        x = 0.0
        y = 0.0
    }
    fun setFont(font: Font, size: Double)
    fun put(reader: WStringReader, codePoint: Int): GlyphMetrics
    fun advance(x: Double) {
        this.x += x
    }
    fun newLine(y: Double, end: Boolean) {
        this.x = 0.0
        this.y += y
        currentLineNum++
    }
}

abstract class TextRendererActions : ITextRendererActions {
    protected val glyphPath = GlyphPath()
    protected val glyphMetrics = GlyphMetrics()
    val fontMetrics = FontMetrics()
    override val lineHeight: Double get() = fontMetrics.lineHeight
    override var currentLineNum: Int = 0
    lateinit var font: Font; private set
    var fontSize = 0.0; private set

    override fun setFont(font: Font, size: Double) {
        this.font = font
        this.fontSize = size
        font.getFontMetrics(size, fontMetrics)
    }

    override var x = 0.0
    override var y = 0.0

    override fun getGlyphMetrics(reader: WStringReader?, codePoint: Int): GlyphMetrics =
        font.getGlyphMetrics(fontSize, codePoint, glyphMetrics, reader)

    //var transformAnchor: Anchor = Anchor.BOTTOM_CENTER
    override var transform: Matrix = Matrix.IDENTITY
    var paint: Paint? = null
    var tint: RGBA = Colors.WHITE // Ignored for now

    abstract override fun put(reader: WStringReader, codePoint: Int): GlyphMetrics

    override fun getKerning(leftCodePoint: Int, rightCodePoint: Int): Double =
        font.getKerning(fontSize, leftCodePoint, rightCodePoint)
}

fun <T> TextRenderer<T>.measure(text: T, size: Double, defaultFont: Font): BoundBuilderTextRendererActions {
    val actions = BoundBuilderTextRendererActions()
    invoke(actions, text, size, defaultFont)
    return actions
}

class BoundBuilderTextRendererActions : TextRendererActions() {
    val flbb = MBoundsBuilder()
    val bb = MBoundsBuilder()
    var currentLine = 0
    //val nlines get() = currentLine + 1
    val nlines get() = currentLine

    val lines = arrayListOf<LineStats>()
    var current = LineStats()

    val totalMaxLineHeight get() = lines.sumOf { it.maxLineHeight }

    fun getAlignX(align: HorizontalAlign, line: Int): Double {
        val line = lines.getOrNull(line) ?: return align.getOffsetX(0.0)
        return align.getOffsetX(line.maxX)
    }
    fun getAlignY(align: VerticalAlign, fontMetrics: FontMetrics): Double =
        align.getOffsetYRespectBaseline(fontMetrics, totalMaxLineHeight)

    data class LineStats(
        var maxLineHeight: Double = 0.0,
        var maxX: Double = 0.0,
        val bounds: MBoundsBuilder = MBoundsBuilder(),
    ) {
        fun getAlignX(align: HorizontalAlign): Double = align.getOffsetX(maxX) + bounds.xminOr(0.0)

        fun advance(dx: Double) {
            maxX += dx
        }

        fun metrics(fontMetrics: FontMetrics) {
            maxLineHeight = max(maxLineHeight, fontMetrics.lineHeight)
        }

        fun end() {
        }

        fun reset() {
            maxX = 0.0
            bounds.reset()
            maxLineHeight = 0.0
        }
    }

    private fun add(x: Float, y: Float) {
        //val itransform = transform.inverted()
        val rx = this.x + transform.transformX(x, y)
        val ry = this.y + transform.transformY(x, y)
        //println("P: $rx, $ry [$x, $y]")
        bb.add(rx, ry)
        if (currentLine == 0) {
            flbb.add(rx, ry)
        }
        current.bounds.add(rx, ry)
    }

    private fun add(rect: Rectangle) {
        val fx = rect.left
        val fy = rect.top
        val w = rect.width
        val h = rect.height

        //println("------: [$x,$y] -- ($fx, $fy)-($w, $h)")
        add(fx, fy)
        add(fx + w, fy)
        add(fx + w, fy + h)
        add(fx, fy + h)
    }

    override fun reset() {
        super.reset()
        currentLine = 0
        bb.reset()
        flbb.reset()
        current.reset()
        lines.clear()
    }

    override fun put(reader: WStringReader, codePoint: Int): GlyphMetrics {
        val g = getGlyphMetrics(reader, codePoint)
        // y = 0 is the baseline
        add(g.bounds)
        current.metrics(fontMetrics)
        return g
    }

    override fun advance(x: Double) {
        super.advance(x)
        current.advance(x)
    }

    override fun newLine(y: Double, end: Boolean) {
        super.newLine(y, end)
        currentLine++
        current.metrics(fontMetrics)
        current.end()
        lines.add(current)
        current = LineStats()
    }
}

class Text2TextRendererActions : TextRendererActions() {
    var align: TextAlignment = TextAlignment.TOP_LEFT

    val verticalAlign: VerticalAlign get() = align.vertical
    val horizontalAlign: HorizontalAlign get() = align.horizontal
    private val arrayTex = arrayListOf<BmpSlice>()
    private val arrayX = doubleArrayListOf()
    private val arrayY = doubleArrayListOf()
    private val arraySX = doubleArrayListOf()
    private val arraySY = doubleArrayListOf()
    private val arrayRot = doubleArrayListOf()
    val arrayMetrics = VectorArrayList(dimensions = 4)
    private var tr = MatrixTransform()
    val size get() = arrayX.size

    data class LineInfo(var maxTop: Double = 0.0, var minBottom: Double = 0.0, var maxLineHeight: Double = 0.0)

    fun getLineInfos(): List<LineInfo> {
        val out = arrayListOf<LineInfo>(LineInfo())
        arrayMetrics.fastForEachGeneric {
            val line = this[it, 0].toInt()
            while (out.size <= line) out.add(LineInfo())
            val lineInfo = out[line]
            val top = this[it, 1].toDouble()
            val bottom = this[it, 2].toDouble()
            val lineHeight = this[it, 3].toDouble()
            lineInfo.maxTop = max(lineInfo.maxTop, top)
            lineInfo.minBottom = min(lineInfo.minBottom, bottom)
            lineInfo.maxLineHeight = max(lineInfo.maxLineHeight, lineHeight)
        }
        return out
    }

    fun getGlyphBounds(n: Int): Rectangle = when {
        n >= size -> Rectangle.ZERO
        else -> Rectangle.fromBounds(
            arrayX[n], arrayY[n],
            arrayX[n] + arrayTex[n].width * arraySX[n], arrayY[n] + arrayTex[n].height * arraySY[n]
        )
    }

    fun getBounds(): Rectangle {
        if (size == 0) {
            return Rectangle.ZERO
        }
        var bb = BoundsBuilder()
        for (n in 0 until size) bb += getGlyphBounds(n)
        return bb.bounds
    }

    data class Entry(
        var tex: BmpSlice = Bitmaps.transparent,
        var x: Double = 0.0,
        var y: Double = 0.0,
        var sx: Double = 1.0,
        var sy: Double = 1.0,
        var rot: Angle = 0.radians
    ) {
        override fun toString(): String = buildString {
            append("Entry(")
            append("'${tex.name}', ${x.toInt()}, ${y.toInt()}, ${tex.width}, ${tex.height}")
            if (sx != 1.0) append(", sx=${sx.niceStr}")
            if (sy != 1.0) append(", sy=${sy.niceStr}")
            if (rot != 0.radians) append(", rot=${rot.degrees.niceStr}")
            append(")")
        }
    }

    fun readAll(): List<Entry> = (0 until size).map { read(it) }

    fun read(n: Int, out: Entry = Entry()): Entry {
        out.tex = arrayTex[n]
        out.x = arrayX[n]
        out.y = arrayY[n]
        out.sx = arraySX[n]
        out.sy = arraySY[n]
        out.rot = arrayRot[n].radians
        return out
    }

    fun mreset() {
        x = 0.0
        y = 0.0
        arrayTex.clear()
        arrayX.clear()
        arrayY.clear()
        arraySX.clear()
        arraySY.clear()
        arrayRot.clear()
        arrayMetrics.clear()
        currentLineNum = 0
    }

    override fun put(reader: WStringReader, codePoint: Int): GlyphMetrics {
        val bf = font as BitmapFont
        val m = reader.keep { getGlyphMetrics(reader, codePoint) }
        val g = bf[codePoint]
        val x = g.xoffset.toDouble()
        val y = g.yoffset.toDouble() - when (verticalAlign) {
            VerticalAlign.BASELINE -> bf.base
            else -> bf.lineHeight * verticalAlign.ratio
        }

        val fontScale = fontSize / bf.fontSize

        tr = transform.immutable.toTransform()
        //println("x: ${this.x}, y: ${this.y}")
        arrayTex.add(g.texture)
        arrayX.add(this.x + transform.transformX(x, y) * fontScale)
        arrayY.add(this.y + transform.transformY(x, y) * fontScale)
        arraySX.add(tr.scaleX * fontScale)
        arraySY.add(tr.scaleY * fontScale)
        arrayRot.add(tr.rotation.radians)
        arrayMetrics.add(
            currentLineNum.toDouble(),
            fontMetrics.top,
            fontMetrics.bottom,
            fontMetrics.lineHeight,
        )
        return m
    }
}

interface TextRenderer<T> {
    val version: Int get() = 0
    fun ITextRendererActions.run(text: T, size: Double, defaultFont: Font): Unit
}

operator fun <T> TextRenderer<T>.invoke(actions: ITextRendererActions, text: T, size: Double, defaultFont: Font) {
    actions.apply { run(text, size, defaultFont) }
}

fun ITextRendererActions.aroundPath(curve: Curve): ITextRendererActions {
    val original = this
    return object : ITextRendererActions by original {
        override fun put(reader: WStringReader, codePoint: Int): GlyphMetrics {
            val oldX = this.x
            val oldY = this.y
            keep(this::transform) {
                try {
                    val ratio = curve.ratioFromLength(this.x)
                    val pos = curve.calc(ratio)
                    val normal = curve.normal(ratio)
                    val rpos = pos + normal * oldY
                    this.x = rpos.xD
                    this.y = rpos.yD
                    this.transform = this.transform.rotated(normal.angle - 90.degrees)

                    //println("PUT: oldX=$oldX, oldY=$oldY, x=$x, y=$y, codePoint=$codePoint")
                    return original.put(reader, codePoint)
                } finally {
                    this.x = oldX
                    this.y = oldY
                }
            }
        }
    }
}

fun <T> TextRenderer<T>.transformed(transformation: (ITextRendererActions) -> ITextRendererActions): TransformedTextRenderer<T> =
    TransformedTextRenderer(this, transformation)

fun <T> TextRenderer<T>.withSpacing(spacing: Double): TransformedTextRenderer<T> =
    transformed { original -> object : ITextRendererActions by original {
        override fun advance(x: Double) {
            super.advance(x + spacing)
        }
    } }

open class TransformedTextRenderer<T>(
    val original: TextRenderer<T>,
    val transformation: (ITextRendererActions) -> ITextRendererActions
) : TextRenderer<T> {
    override val version: Int get() = original.version
    override fun ITextRendererActions.run(text: T, size: Double, defaultFont: Font) =
        original.invoke(transformation(this), text, size, defaultFont)
}

inline fun <T> TextRenderer<T>.aroundPath(out: VectorPath = VectorPath(), block: VectorPath.() -> Unit): CurveTextRenderer<T> = aroundPath(out.apply(block))
fun <T> TextRenderer<T>.aroundPath(path: VectorPath): CurveTextRenderer<T> = CurveTextRenderer(this, path, path.getCurves())
fun <T> TextRenderer<T>.aroundPath(curve: Curve): CurveTextRenderer<T> = CurveTextRenderer(this, curve.toVectorPath(), curve)

class CurveTextRenderer<T>(
    original: TextRenderer<T>,
    val path: VectorPath,
    val curve: Curve,
) : TransformedTextRenderer<T>(original, { it.aroundPath(curve) })

inline fun <reified T> DefaultTextRenderer() = when (T::class) {
    String::class -> DefaultStringTextRenderer
    WString::class -> DefaultWStringTextRenderer
    else -> error("No default DefaultTextRenderer for class ${T::class} only for String")
}

fun CreateWStringTextRenderer(
    getVersion: () -> Int = { 0 },
    handler: ITextRendererActions.(reader: WStringReader, c: Int, g: GlyphMetrics, advance: Double) -> Unit
): TextRenderer<WString> = object : TextRenderer<WString> {
    override val version: Int get() = getVersion()

    override fun ITextRendererActions.run(text: WString, size: Double, defaultFont: Font) {
        reset()
        setFont(defaultFont, size)
        val reader = WStringReader(text)
        while (reader.hasMore) {
            val c = reader.peek().codePoint
            val c1 = reader.peek(+1).codePoint
            val startPos = reader.position
            if (c == '\n'.code) {
                newLine(lineHeight, end = false)
            } else {
                val g = reader.keep { getGlyphMetrics(reader, c) }
                transform = Matrix.IDENTITY
                //println("READER: c='${c.toChar()}', pos=${reader.position}")
                handler(this, reader, c, g, (g.xadvance + getKerning(c, c1)))
            }
            // No explicit movement, skip 1
            if (reader.position <= startPos) {
                reader.skip(1)
            }
        }
        newLine(lineHeight, end = true)
    }
}

fun CreateStringTextRenderer(
    getVersion: () -> Int = { 0 },
    handler: ITextRendererActions.(reader: WStringReader, c: Int, g: GlyphMetrics, advance: Double) -> Unit
): TextRenderer<String> = object : TextRenderer<String> {
    val wstring = CreateWStringTextRenderer(getVersion) { text, c, g, advance ->
        handler(this, text, c, g, advance)
    }
    override val version: Int get() = wstring.version

    override fun ITextRendererActions.run(text: String, size: Double, defaultFont: Font) {
        wstring.apply {
            run(WString(text), size, defaultFont)
        }
    }
}

@SharedImmutable
val DefaultWStringTextRenderer: TextRenderer<WString> = CreateWStringTextRenderer { text, c, g, advance ->
    put(text, c)
    advance(advance)
}

@SharedImmutable
val DefaultStringTextRenderer: TextRenderer<String> = CreateStringTextRenderer { text, c, g, advance ->
    put(text, c)
    advance(advance)
}

fun <T> VectorBuilder.text(
    text: T, font: VectorFont, textSize: Double = 16.0,
    x: Double = 0.0, y: Double = 0.0,
    align: TextAlignment = TextAlignment.BASELINE_LEFT,
    renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
) {
    val vectorBuilder = this
    var transform = Matrix()

    val actions = object : TextRendererActions() {
        val metrics = renderer.measure(text, textSize, font)
        override fun put(reader: WStringReader, codePoint: Int): GlyphMetrics {
            val glyph = font.getGlyphPath(this.fontSize, codePoint, this.glyphPath, reader) ?: return glyphMetrics
            keep(::transform) {
                val dx = metrics.getAlignX(align.horizontal, currentLineNum)
                val dy = metrics.getAlignY(align.vertical, fontMetrics)
                transform = Matrix.IDENTITY
                    .premultiplied(glyph.transform)
                    .translated(this.x + x - dx, this.y + y + dy)
                    .premultiplied(this.transform)
                //println("PUT $codePoint -> $transform : $x, $y, ${this.x}, ${this.y}")
                val shape = glyph.colorShape
                vectorBuilder.path(shape?.getPath() ?: glyph.path, transform)
            }
            return glyphMetrics
        }
    }
    renderer.invoke(actions, text, textSize, font)
}
