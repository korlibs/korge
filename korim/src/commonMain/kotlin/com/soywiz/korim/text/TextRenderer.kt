package com.soywiz.korim.text

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.*
import com.soywiz.korim.paint.Paint
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*

abstract class TextRendererActions {
    protected val glyphMetrics = GlyphMetrics()
    val fontMetrics = FontMetrics()
    val lineHeight get() = fontMetrics.lineHeight
    lateinit var font: Font; private set
    var fontSize = 0.0; private set

    fun setFont(font: Font, size: Double) {
        this.font = font
        this.fontSize = size
        font.getFontMetrics(size, fontMetrics)
    }

    var x = 0.0
    var y = 0.0

    open fun reset() {
        x = 0.0
        y = 0.0
    }

    fun getGlyphMetrics(codePoint: Int): GlyphMetrics = font.getGlyphMetrics(fontSize, codePoint, glyphMetrics)

    //var transformAnchor: Anchor = Anchor.BOTTOM_CENTER
    val transform: Matrix = Matrix()
    var paint: Paint? = null
    var tint: RGBA = Colors.WHITE // Ignored for now

    abstract fun put(codePoint: Int): GlyphMetrics

    open fun getKerning(leftCodePoint: Int, rightCodePoint: Int): Double =
        font.getKerning(fontSize, leftCodePoint, rightCodePoint)

    open fun advance(x: Double) {
        this.x += x
    }

    open fun newLine(y: Double) {
        this.x = 0.0
        this.y += y
    }
}

class BoundBuilderTextRendererActions : TextRendererActions() {
    val flbb = BoundsBuilder()
    val bb = BoundsBuilder()
    var currentLine = 0

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

    override fun put(codePoint: Int): GlyphMetrics {
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
            if (sx != 1.0) append(", ${sx.niceStr}")
            if (sy != 1.0) append(", ${sy.niceStr}")
            if (rot != 0.radians) append(", ${rot.degrees.niceStr}")
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

    override fun put(codePoint: Int): GlyphMetrics {
        val bf = font as BitmapFont
        val m = getGlyphMetrics(codePoint)
        val g = bf[codePoint]
        val x = g.xoffset.toDouble()
        val y = g.yoffset.toDouble() - when (verticalAlign) {
            VerticalAlign.BASELINE -> bf.base
            else -> bf.lineHeight * verticalAlign.ratio
        }

        val fontScale = fontSize / bf.fontSize

        tr.setMatrix(transform)
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
    fun TextRendererActions.run(text: T, size: Double, defaultFont: Font): Unit
    fun invoke(actions: TextRendererActions, text: T, size: Double, defaultFont: Font) {
        actions.apply { run(text, size, defaultFont) }
    }
}

inline fun <reified T> DefaultTextRenderer() = when (T::class) {
    String::class -> DefaultStringTextRenderer
    else -> error("No default DefaultTextRenderer for class ${T::class} only for String")
}

fun CreateStringTextRenderer(
    getVersion: () -> Int = { 0 },
    handler: TextRendererActions.(text: String, n: Int, c: Int, c1: Int, g: GlyphMetrics, advance: Double) -> Unit
): TextRenderer<String> = object : TextRenderer<String> {
    override val version: Int get() = getVersion()

    override fun TextRendererActions.run(text: String, size: Double, defaultFont: Font) {
        reset()
        setFont(defaultFont, size)
        for (n in text.indices) {
            val c = text[n].toInt()
            val c1 = text.getOrElse(n + 1) { '\u0000' }.toInt()
            if (c == '\n'.toInt()) {
                newLine(lineHeight)
            } else {
                val g = getGlyphMetrics(c)
                transform.identity()
                handler(text, n, c, c1, g, (g.xadvance + getKerning(c, c1)))
            }
        }
    }
}

val DefaultStringTextRenderer: TextRenderer<String> = CreateStringTextRenderer { text, n, c, c1, g, advance ->
    put(c)
    advance(advance)
}
