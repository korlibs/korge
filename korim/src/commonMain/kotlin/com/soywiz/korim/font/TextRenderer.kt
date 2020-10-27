package com.soywiz.korim.font

import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.paint.Paint
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

    fun reset() {
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

typealias TextRenderer<T> = TextRendererActions.(text: T, size: Double, defaultFont: Font) -> Unit

inline fun <reified T> DefaultTextRenderer() = when (T::class) {
    String::class -> DefaultStringTextRenderer
    else -> error("No default DefaultTextRenderer for class ${T::class} only for String")
}

fun CreateStringTextRenderer(handler: TextRendererActions.(text: String, n: Int, c: Int, c1: Int, g: GlyphMetrics, advance: Double) -> Unit): TextRenderer<String> = { text, size, defaultFont ->
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
val DefaultStringTextRenderer: TextRenderer<String> = CreateStringTextRenderer { text, n, c, c1, g, advance ->
    put(c)
    advance(advance)
}
