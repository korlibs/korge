package com.soywiz.korim.font

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*

expect val nativeSystemFontProvider: NativeSystemFontProvider

open class NativeSystemFontProvider {
    open fun getDefaultFontName(): String = listFontNames().maxByOrNull {
        when {
            it.equals("arial", ignoreCase = true) -> 1000
            it.contains("arial", ignoreCase = true) -> 1000 - it.length
            else -> 0
        }
    } ?: "default"

    open fun getEmojiFontName(): String {
        return listFontNames().firstOrNull { it.contains("emoji", ignoreCase = true) } ?: getDefaultFontName()
    }

    open fun listFontNames(): List<String> = listFontNamesWithFiles().keys.toList()

    open fun getTtfFromSystemFont(systemFont: SystemFont): TtfFont {
        return DefaultTtfFont
    }

    open fun listFontNamesWithFiles(): Map<String, VfsFile> {
        return mapOf()
    }

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

// Windows: C:\Windows\Fonts (%DRIVE%)
// Linux: /usr/share/fonts , /usr/local/share/fonts and ~/.fonts
// MacOS: /System/Library/Fonts, /Library/Fonts, ~/Library/Fonts

private val linuxFolders = listOf("/usr/share/fonts", "/usr/local/share/fonts", "~/.fonts")
private val windowsFolders = listOf("%WINDIR%\\Fonts")
private val macosFolders = listOf("/System/Library/Fonts/", "/Library/Fonts/", "~/Library/Fonts/", "/Network/Library/Fonts/")
private val iosFolders = listOf("/System/Library/Fonts/Cache", "/System/Library/Fonts")
private val androidFolders = listOf("/system/Fonts", "/system/font", "/data/fonts")

// @TODO: Maybe we can just filter fonts containing "emoji" (ignoring case)
private val emojiFontNames = listOf(
    "Segoe UI Emoji", // Windows
    "Apple Color Emoji", // Apple
    "Noto Color Emoji", // Google
)

open class FolderBasedNativeSystemFontProvider(val folders: List<String> = linuxFolders + windowsFolders + macosFolders + androidFolders + iosFolders, val fontCacheFile: String = "~/.korimFontCache") : TtfNativeSystemFontProvider() {
    fun listFontNamesMap(): Map<String, VfsFile> = runBlockingNoJs {
        val fontCacheVfsFile = localVfs(com.soywiz.korio.lang.Environment.expand(fontCacheFile))
        val out = LinkedHashMap<String, VfsFile>()
        val fileNamesToName = LinkedHashMap<String, String>()
        val oldFontCacheVfsFileText = try { fontCacheVfsFile.readString() } catch (e: Throwable) { "" }
        for (line in oldFontCacheVfsFileText.split("\n")) {
            val (file, name) = line.split("=", limit = 2) + listOf("", "")
            fileNamesToName[file] = name
        }
        for (folder in folders) {
            try {
                val file = localVfs(Environment.expand(folder))
                for (f in file.listSimple()) {
                    try {
                        val name = fileNamesToName.getOrPut(f.baseName) { com.soywiz.korim.font.TtfFont(f.readAll()).ttfCompleteName }
                        if (name != "") {
                            out[name] = f
                        }
                    } catch (e: Throwable) {
                        fileNamesToName.getOrPut(f.baseName) { "" }
                    }
                }
            } catch (e: Throwable) {
            }
        }
        val newFontCacheVfsFileText = fileNamesToName.map { "${it.key}=${it.value}" }.joinToString("\n")
        if (newFontCacheVfsFileText != oldFontCacheVfsFileText) {
            fontCacheVfsFile.writeString(newFontCacheVfsFileText)
        }
        //println("fileNamesToName: $fileNamesToName")
        out
    }

    fun listFontNamesMapLC(): Map<String, VfsFile> = listFontNamesMap().mapKeys { it.key.normalizeName() }

    override fun listFontNamesWithFiles(): Map<String, VfsFile> = listFontNamesMap()

    private val namesMapLC by lazy { listFontNamesMapLC() }

    override fun loadFontByName(name: String): TtfFont? =
        runBlockingNoJs { namesMapLC[name.normalizeName()]?.let { com.soywiz.korim.font.TtfFont(it.readAll()) } }
}

abstract class TtfNativeSystemFontProvider() : NativeSystemFontProvider() {
    abstract fun loadFontByName(name: String): TtfFont?

    fun String.normalizeName() = this.toLowerCase().trim()

    private val ttfCache by lazy { LinkedHashMap<String, TtfFont?>() }

    fun locateFontByName(name: String): TtfFont? {
        val normalizedName = name.normalizeName()
        return ttfCache.getOrPut(normalizedName) { loadFontByName(name) }
    }

    fun ttf(systemFont: SystemFont) = locateFontByName(systemFont.name) ?: DefaultTtfFont

    override fun getTtfFromSystemFont(systemFont: SystemFont): TtfFont = ttf(systemFont)

    override fun getSystemFontGlyph(systemFont: SystemFont, size: Double, codePoint: Int, path: GlyphPath): GlyphPath? =
        ttf(systemFont).getGlyphPath(size, codePoint, path)

    override fun getSystemFontMetrics(systemFont: SystemFont, size: Double, metrics: FontMetrics) {
        ttf(systemFont).getFontMetrics(size, metrics)
    }

    override fun getSystemFontGlyphMetrics(
        systemFont: SystemFont,
        size: Double,
        codePoint: Int,
        metrics: GlyphMetrics
    ) {
        ttf(systemFont).getGlyphMetrics(size, codePoint, metrics)
    }

    override fun getSystemFontKerning(
        systemFont: SystemFont,
        size: Double,
        leftCodePoint: Int,
        rightCodePoint: Int
    ): Double = ttf(systemFont).getKerning(size, leftCodePoint, rightCodePoint)
}

open class FallbackNativeSystemFontProvider(val ttf: TtfFont) : TtfNativeSystemFontProvider() {
    val vfs = VfsFileFromData(ttf.getAllBytesUnsafe(), "ttf")
    override fun getTtfFromSystemFont(systemFont: SystemFont): TtfFont = ttf
    override fun listFontNamesWithFiles(): Map<String, VfsFile> = mapOf(ttf.ttfCompleteName to vfs)
    override fun loadFontByName(name: String): TtfFont? = ttf
}
