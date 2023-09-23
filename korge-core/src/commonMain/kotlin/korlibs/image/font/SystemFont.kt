package korlibs.image.font

import korlibs.datastructure.*
import korlibs.io.lang.*
import korlibs.io.resources.*
import kotlin.coroutines.*

suspend fun SystemFont(name: String): SystemFont = SystemFont(name, coroutineContext)

class SystemFont constructor(override val name: String, val coroutineContext: CoroutineContext) : VectorFont, Resourceable<Font>, Extra by Extra.Mixin() {
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = other is SystemFont && this.name == other.name
    override fun toString(): String = "SystemFont(name=$name)"

    companion object {
        suspend fun listFontNames() = nativeSystemFontProvider().listFontNames()
        suspend fun listFontNamesWithFiles() = nativeSystemFontProvider().listFontNamesWithFiles()
        suspend fun getDefaultFont() = SystemFont(nativeSystemFontProvider().getDefaultFontName())
        suspend fun getEmojiFont() = SystemFont(nativeSystemFontProvider().getEmojiFontName())
    }

    val nativeSystemFontProvider get() = nativeSystemFontProvider(coroutineContext)

    override fun getFontMetrics(size: Double, metrics: FontMetrics): FontMetrics =
        metrics.also { nativeSystemFontProvider.getSystemFontMetrics(this, size, metrics) }

    override fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics, reader: WStringReader?): GlyphMetrics =
        metrics.also { nativeSystemFontProvider.getSystemFontGlyphMetrics(this, size, codePoint, metrics, reader) }

    override fun getKerning(
        size: Double,
        leftCodePoint: Int,
        rightCodePoint: Int
    ): Double = nativeSystemFontProvider.getSystemFontKerning(this, size, leftCodePoint, rightCodePoint)

    override fun getGlyphPath(size: Double, codePoint: Int, path: GlyphPath, reader: WStringReader?): GlyphPath? {
        return nativeSystemFontProvider.getSystemFontGlyph(this, size, codePoint, path, reader)
    }

    val ttf get() = nativeSystemFontProvider.getTtfFromSystemFont(this)
}
