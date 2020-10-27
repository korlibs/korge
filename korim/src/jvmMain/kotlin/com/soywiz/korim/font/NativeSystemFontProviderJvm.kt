package com.soywiz.korim.font

import com.soywiz.korim.awt.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import java.awt.*
import java.awt.font.*
import java.awt.geom.*
import java.awt.image.*

actual val nativeSystemFontProvider: NativeSystemFontProvider = AwtNativeSystemFontProvider

object AwtNativeSystemFontProvider : NativeSystemFontProvider() {
    val out = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    //println("BufferedImage.clone:${this.type} -> ${out.type}")
    val g = out.createGraphics(false)

    override fun listFontNames(): List<String> {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toList()
    }

    override fun getSystemFontGlyph(systemFont: SystemFont, size: Double, codePoint: Int, path: GlyphPath): GlyphPath? {
        val font = systemFont.toAwt(size)
        if (!font.canDisplay(codePoint)) return null
        val c = FontRenderContext(AffineTransform(), true, true)
        val vector = font.createGlyphVector(c, "${codePoint.toChar()}")
        val outline = vector.outline
        val pi = outline.getPathIterator(AffineTransform())
        val vp = GraphicsPath()
        val data = DoubleArray(6)
        while (!pi.isDone) {
            when (pi.currentSegment(data)) {
                PathIterator.SEG_MOVETO -> vp.moveTo(data[0], data[1])
                PathIterator.SEG_LINETO -> vp.lineTo(data[0], data[1])
                PathIterator.SEG_QUADTO -> vp.quadTo(data[0], data[1], data[2], data[3])
                PathIterator.SEG_CUBICTO -> vp.cubicTo(data[0], data[1], data[2], data[3], data[4], data[5])
                PathIterator.SEG_CLOSE -> vp.close()
            }
            pi.next()
        }
        path.path = vp
        path.transform.identity()
        return path
    }

    override fun getSystemFontMetrics(systemFont: SystemFont, size: Double, metrics: FontMetrics) {
        val font = systemFont.toAwt(size)
        g.font = font
        val fm = g.fontMetrics
        metrics.size = size
        metrics.top = fm.maxAscent.toDouble()
        metrics.ascent = fm.ascent.toDouble()
        metrics.baseline = 0.0
        metrics.descent = fm.descent.toDouble()
        metrics.bottom = fm.maxDescent.toDouble()
        metrics.leading = 0.0
    }

    override fun getSystemFontKerning(
        systemFont: SystemFont,
        size: Double,
        leftCodePoint: Int,
        rightCodePoint: Int
    ): Double {
        val font = systemFont.toAwt(size)
        return super.getSystemFontKerning(systemFont, size, leftCodePoint, rightCodePoint)
    }

    override fun getSystemFontGlyphMetrics(
        systemFont: SystemFont,
        size: Double,
        codePoint: Int,
        metrics: GlyphMetrics
    ) {
        super.getSystemFontGlyphMetrics(systemFont, size, codePoint, metrics)
        val font = systemFont.toAwt(size)
        g.font = font
        val fm = g.fontMetrics
        val c = FontRenderContext(AffineTransform(), true, true)
        val vector = font.createGlyphVector(c, "${codePoint.toChar()}")
        //val bounds = vector.logicalBounds
        val bounds = vector.visualBounds
        metrics.existing = font.canDisplay(codePoint)
        metrics.xadvance = fm.charWidth(codePoint.toChar()).toDouble()
        //metrics.bounds.setTo(0, 0, bounds.width, bounds.height)
        //println("BOUNDS: ${metrics.bounds}")
        //metrics.bounds.setTo(bounds.x, -(bounds.height - bounds.y), bounds.width, bounds.height)
    }

    fun SystemFont.toAwt(size: Double): java.awt.Font
        = java.awt.Font(this.name, java.awt.Font.PLAIN, size.toInt())
}
