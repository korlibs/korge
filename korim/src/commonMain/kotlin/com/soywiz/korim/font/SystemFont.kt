package com.soywiz.korim.font

import com.soywiz.korio.resources.*

inline class SystemFont(override val name: String) : VectorFont, Resourceable<Font> {
    companion object {
        fun listFontNames() = nativeSystemFontProvider.listFontNames()
        fun listFontNamesWithFiles() = nativeSystemFontProvider.listFontNamesWithFiles()
        fun getEmojiFont() = nativeSystemFontProvider.getEmojiFontName()?.let { SystemFont(it) }
    }

    override fun getFontMetrics(size: Double, metrics: FontMetrics): FontMetrics =
        metrics.also { nativeSystemFontProvider.getSystemFontMetrics(this, size, metrics) }

    override fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics): GlyphMetrics =
        metrics.also { nativeSystemFontProvider.getSystemFontGlyphMetrics(this, size, codePoint, metrics) }

    override fun getKerning(
        size: Double,
        leftCodePoint: Int,
        rightCodePoint: Int
    ): Double = nativeSystemFontProvider.getSystemFontKerning(this, size, leftCodePoint, rightCodePoint)

    override fun getGlyphPath(size: Double, codePoint: Int, path: GlyphPath): GlyphPath? {
        return nativeSystemFontProvider.getSystemFontGlyph(this, size, codePoint, path)
    }
}
