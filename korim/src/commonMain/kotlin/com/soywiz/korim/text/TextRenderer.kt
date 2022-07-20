package com.soywiz.korim.text

import com.soywiz.kds.doubleArrayListOf
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.FontMetrics
import com.soywiz.korim.font.GlyphMetrics
import com.soywiz.korim.font.GlyphPath
import com.soywiz.korim.font.VectorFont
import com.soywiz.korim.paint.Paint
import com.soywiz.korim.vector.CompoundShape
import com.soywiz.korim.vector.EmptyShape
import com.soywiz.korim.vector.FillShape
import com.soywiz.korim.vector.StyledShape
import com.soywiz.korio.lang.WString
import com.soywiz.korio.lang.WStringReader
import com.soywiz.korio.util.niceStr
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.angle
import com.soywiz.korma.geom.bezier.Curve
import com.soywiz.korma.geom.bezier.toVectorPath
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.minus
import com.soywiz.korma.geom.radians
import com.soywiz.korma.geom.vector.VectorBuilder
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.getCurves
import com.soywiz.korma.geom.vector.path
import kotlin.native.concurrent.SharedImmutable

interface ITextRendererActions {
    var x: Double
    var y: Double
    val lineHeight: Double
    val transform: Matrix

    fun getKerning(leftCodePoint: Int, rightCodePoint: Int): Double
    fun getGlyphMetrics(codePoint: Int): GlyphMetrics
    fun reset() {
        x = 0.0
        y = 0.0
    }
    fun setFont(font: Font, size: Double)
    fun put(reader: WStringReader, codePoint: Int): GlyphMetrics
    fun advance(x: Double) {
        this.x += x
    }
    fun newLine(y: Double) {
        this.x = 0.0
        this.y += y
    }
}

abstract class TextRendererActions : ITextRendererActions {
    protected val glyphPath = GlyphPath()
    protected val glyphMetrics = GlyphMetrics()
    val fontMetrics = FontMetrics()
    override val lineHeight: Double get() = fontMetrics.lineHeight
    lateinit var font: Font; private set
    var fontSize = 0.0; private set

    override fun setFont(font: Font, size: Double) {
        this.font = font
        this.fontSize = size
        font.getFontMetrics(size, fontMetrics)
    }

    override var x = 0.0
    override var y = 0.0

    override fun getGlyphMetrics(codePoint: Int): GlyphMetrics = font.getGlyphMetrics(fontSize, codePoint, glyphMetrics)

    //var transformAnchor: Anchor = Anchor.BOTTOM_CENTER
    override val transform: Matrix = Matrix()
    var paint: Paint? = null
    var tint: RGBA = Colors.WHITE // Ignored for now

    abstract override fun put(reader: WStringReader, codePoint: Int): GlyphMetrics

    override fun getKerning(leftCodePoint: Int, rightCodePoint: Int): Double =
        font.getKerning(fontSize, leftCodePoint, rightCodePoint)
}

class BoundBuilderTextRendererActions : TextRendererActions() {
    val flbb = BoundsBuilder()
    val bb = BoundsBuilder()
    var currentLine = 0
    val nlines get() = currentLine + 1

    private fun add(x: Double, y: Double) {
        //val itransform = transform.inverted()
        val rx = this.x + transform.transformX(x, y)
        val ry = this.y + transform.transformY(x, y)
        //println("P: $rx, $ry [$x, $y]")
        bb.add(rx, ry)
        if (currentLine == 0) {
            flbb.add(rx, ry)
        }
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
    }

    override fun put(reader: WStringReader, codePoint: Int): GlyphMetrics {
        val g = getGlyphMetrics(codePoint)
        // y = 0 is the baseline
        add(g.bounds)
        return g
    }

    override fun newLine(y: Double) {
        super.newLine(y)
        currentLine++
    }
}

class Text2TextRendererActions : TextRendererActions() {
    var verticalAlign: VerticalAlign = VerticalAlign.TOP
    var horizontalAlign: HorizontalAlign = HorizontalAlign.LEFT
    private val arrayTex = arrayListOf<BmpSlice>()
    private val arrayX = doubleArrayListOf()
    private val arrayY = doubleArrayListOf()
    private val arraySX = doubleArrayListOf()
    private val arraySY = doubleArrayListOf()
    private val arrayRot = doubleArrayListOf()
    private val tr = Matrix.Transform()
    val size get() = arrayX.size

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
        arrayTex.clear()
        arrayX.clear()
        arrayY.clear()
        arraySX.clear()
        arraySY.clear()
        arrayRot.clear()
    }

    override fun put(reader: WStringReader, codePoint: Int): GlyphMetrics {
        val bf = font as BitmapFont
        val m = getGlyphMetrics(codePoint)
        val g = bf[codePoint]
        val x = g.xoffset.toDouble()
        val y = g.yoffset.toDouble() - when (verticalAlign) {
            VerticalAlign.BASELINE -> bf.base
            else -> bf.lineHeight * verticalAlign.ratio
        }

        val fontScale = fontSize / bf.fontSize

        tr.setMatrixNoReturn(transform)
        //println("x: ${this.x}, y: ${this.y}")
        arrayTex.add(g.texture)
        arrayX.add(this.x + transform.transformX(x, y) * fontScale)
        arrayY.add(this.y + transform.transformY(x, y) * fontScale)
        arraySX.add(tr.scaleX * fontScale)
        arraySY.add(tr.scaleY * fontScale)
        arrayRot.add(tr.rotation.radians)
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
            this.transform.keepMatrix {
                try {
                    val ratio = curve.ratioFromLength(this.x)
                    val pos = curve.calc(ratio)
                    val normal = curve.normal(ratio)
                    val rpos = pos + normal * oldY
                    this.x = rpos.x
                    this.y = rpos.y
                    this.transform.rotate(normal.angle - 90.degrees)

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
                newLine(lineHeight)
            } else {
                val g = getGlyphMetrics(c)
                transform.identity()
                handler(this, reader, c, g, (g.xadvance + getKerning(c, c1)))
            }
            // No explicit movement, skip 1
            if (reader.position <= startPos) {
                reader.skip(1)
            }
        }
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
    renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
) {
    val transform = Matrix()
    //transform.pretranslate(x, y)

    val actions = object : TextRendererActions() {
        override fun put(reader: WStringReader, codePoint: Int): GlyphMetrics {
            val glyph = font.getGlyphPath(this.fontSize, codePoint, this.glyphPath)
            if (glyph != null) {
                transform.keepMatrix {
                    transform.premultiply(glyph.transform)
                    transform.translate(this.x + x, this.y + y)
                    transform.premultiply(this.transform)
                    //println("PUT $codePoint -> $transform : $x, $y, ${this.x}, ${this.y}")
                    val shape = glyph.colorShape
                    if (shape != null) {
                        path(shape.getPath(), transform)
                    } else {
                        path(glyph.path, transform)
                    }
                }
            }
            return glyphMetrics
        }
    }
    renderer.invoke(actions, text, textSize, font)
}
