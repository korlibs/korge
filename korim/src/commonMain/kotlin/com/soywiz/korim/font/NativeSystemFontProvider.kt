package com.soywiz.korim.font

import com.soywiz.korim.vector.*

expect val nativeSystemFontProvider: NativeSystemFontProvider

open class NativeSystemFontProvider {
    open fun getSystemFontGlyph(systemFont: SystemFont, size: Double, codePoint: Int, path: GlyphPath = GlyphPath()): GlyphPath? {
        return null
    }

    open fun getSystemFontMetrics(systemFont: SystemFont, size: Double, metrics: FontMetrics) {
        val ascentRatio = 0.8
        metrics.size = size
        metrics.top = size * ascentRatio
        metrics.ascent = metrics.top
        metrics.baseline = 0.0
        metrics.descent = -size * (1.0 - ascentRatio)
        metrics.bottom = metrics.descent
        metrics.maxWidth = size
    }

    open fun getSystemFontGlyphMetrics(systemFont: SystemFont, size: Double, codePoint: Int, metrics: GlyphMetrics) {
        metrics.existing = false
        metrics.bounds.setTo(0.0, 0.0, size, size)
        metrics.xadvance = size
    }

    open fun getSystemFontKerning(systemFont: SystemFont, size: Double, leftCodePoint: Int, rightCodePoint: Int) : Double
        = 0.0
}

open class FallbackNativeSystemFontProvider(val ttf: TtfFont) : NativeSystemFontProvider() {
    override fun getSystemFontGlyph(systemFont: SystemFont, size: Double, codePoint: Int, path: GlyphPath): GlyphPath? =
        ttf.getGlyphPath(size, codePoint, path)

    override fun getSystemFontMetrics(systemFont: SystemFont, size: Double, metrics: FontMetrics) {
        ttf.getFontMetrics(size, metrics)
    }

    override fun getSystemFontGlyphMetrics(
        systemFont: SystemFont,
        size: Double,
        codePoint: Int,
        metrics: GlyphMetrics
    ) {
        ttf.getGlyphMetrics(size, codePoint, metrics)
    }

    override fun getSystemFontKerning(
        systemFont: SystemFont,
        size: Double,
        leftCodePoint: Int,
        rightCodePoint: Int
    ): Double = ttf.getKerning(size, leftCodePoint, rightCodePoint)
}

// @TODO: We can check the system's font directory with a VfsFile folder,
// @TODO: then construct a catalog parsing the TTF files as a way of getting system fonts

// Windows: C:\Windows\Fonts (%DRIVE%)
// Linux: /usr/share/fonts , /usr/local/share/fonts and ~/.fonts
// MacOS: /System/Library/Fonts, /Library/Fonts, ~/Library/Fonts

