package korlibs.image.font

import korlibs.datastructure.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.concurrent.atomic.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.platform.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.coroutines.*

internal fun createNativeSystemFontProvider(coroutineContext: CoroutineContext, platform: Platform = Platform): NativeSystemFontProvider = when {
    platform.runtime.isJs -> FallbackNativeSystemFontProvider(DefaultTtfFont)
    else -> {
        val folders = when (platform.os) {
            Os.WINDOWS -> listOf("%WINDIR%\\Fonts", "%LOCALAPPDATA%\\Microsoft\\Windows\\Fonts")
            Os.LINUX -> listOf("/usr/share/fonts", "/usr/local/share/fonts", "~/.fonts")
            Os.MACOSX -> listOf("/System/Library/Fonts/", "/Library/Fonts/", "~/Library/Fonts/", "/Network/Library/Fonts/")
            Os.IOS, Os.TVOS -> listOf("/System/Library/Fonts")
            Os.ANDROID -> listOf("/system/fonts", "/system/font", "/data/fonts")
            else -> listOf("/usr/share/fonts", "/usr/local/share/fonts", "~/.fonts")
        }
        FolderBasedNativeSystemFontProvider(coroutineContext, folders)
    }
}

var _nativeSystemFontProvider: NativeSystemFontProvider? = null
fun nativeSystemFontProvider(coroutineContext: CoroutineContext): NativeSystemFontProvider =
    cacheLazyNullable(::_nativeSystemFontProvider) { createNativeSystemFontProvider(coroutineContext) }

suspend fun nativeSystemFontProvider(): NativeSystemFontProvider = nativeSystemFontProvider(coroutineContext)

open class NativeSystemFontProvider {
    companion object {
        val logger = Logger("NativeSystemFontProvider")
    }

    open fun getDefaultFontName(): String = listFontNames().maxByOrNull {
        when {
            it.equals("arial unicode ms", ignoreCase = true) -> 2000
            it.equals("arialuni", ignoreCase = true) -> 2000
            it.equals("arial", ignoreCase = true) -> 1000
            it.contains("arial", ignoreCase = true) -> 1000 - it.length
            it.contains("roboto", ignoreCase = true) -> 1000 - it.length // In android we don't have Arial
            else -> 0
        }
    } ?: "default"

    open fun getEmojiFontName(): String = listFontNames().maxByOrNull {
        when {
            it.equals("segoe ui emoji", ignoreCase = true) -> 2000
            it.contains("emoji", ignoreCase = true) -> 1000 - it.length // Shortest should have a higher score (Emoji vs Emoji Flags)
            else -> 0
        }
    } ?: getDefaultFontName()

    open fun listFontNames(): List<String> = listFontNamesWithFiles().keys.toList()

    open fun getTtfFromSystemFont(systemFont: SystemFont): TtfFont {
        return DefaultTtfFont
    }

    open fun listFontNamesWithFiles(): Map<String, VfsFile> {
        return mapOf()
    }

    open fun getSystemFontGlyph(systemFont: SystemFont, size: Double, codePoint: Int, path: GlyphPath = GlyphPath(), reader: WStringReader? = null): GlyphPath? {
        return null
    }

    open fun getSystemFontMetrics(systemFont: SystemFont, size: Double, metrics: FontMetrics) {
        val ascentRatio = 0.8f
        metrics.size = size
        metrics.top = size * ascentRatio
        metrics.ascent = metrics.top
        metrics.baseline = 0.0
        metrics.descent = -size * (1f - ascentRatio)
        metrics.bottom = metrics.descent
        metrics.maxWidth = size
    }

    open fun getSystemFontGlyphMetrics(
        systemFont: SystemFont,
        size: Double,
        codePoint: Int,
        metrics: GlyphMetrics,
        reader: WStringReader? = null
    ) {
        metrics.existing = false
        metrics.bounds = Rectangle(0.0, 0.0, size, size)
        metrics.xadvance = size
    }

    open fun getSystemFontKerning(systemFont: SystemFont, size: Double, leftCodePoint: Int, rightCodePoint: Int) : Double = 0.0
}

// Windows: C:\Windows\Fonts (%DRIVE%)
// Linux: /usr/share/fonts , /usr/local/share/fonts and ~/.fonts
// MacOS: /System/Library/Fonts, /Library/Fonts, ~/Library/Fonts

private val linuxFolders get() = listOf("/usr/share/fonts", "/usr/local/share/fonts", "~/.fonts")
private val windowsFolders get() = listOf("%WINDIR%\\Fonts", "%LOCALAPPDATA%\\Microsoft\\Windows\\Fonts")
private val macosFolders get() = listOf("/System/Library/Fonts/", "/Library/Fonts/", "~/Library/Fonts/", "/Network/Library/Fonts/")
private val iosFolders get() = listOf("/System/Library/Fonts")
private val androidFolders get() = listOf("/system/fonts", "/system/font", "/data/fonts")

// @TODO: Maybe we can just filter fonts containing "emoji" (ignoring case)
private val emojiFontNames get() = listOf(
    "Segoe UI Emoji", // Windows
    "Apple Color Emoji", // Apple
    "Noto Color Emoji", // Google
)

open class FolderBasedNativeSystemFontProvider(
    val context: CoroutineContext,
    val folders: List<String> = (linuxFolders + windowsFolders + macosFolders + androidFolders + iosFolders).distinct(),
    val fontCacheFile: VfsFile = standardVfs.userSharedCacheFile("korimFontCache"), // Typically ~/.korimFontCache
) : TtfNativeSystemFontProvider() {
    companion object {
        val logger = Logger("FolderBasedNativeSystemFontProvider")
    }
    fun listFontNamesMap(): Map<String, VfsFile> = runBlockingNoJs(context) {
        val out = LinkedHashMap<String, VfsFile>()
        val time = measureTime {
            logger.info { "FolderBasedNativeSystemFontProvider.listFontNamesMap: $folders" }
            //fontCacheFile.delete()
            val fontCacheVfsFile = fontCacheFile
            val fileNamesToName = LinkedHashMap<String, String>()
            val oldFontCacheVfsFileText = try {
                fontCacheVfsFile.readString()
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                ""
            }
            for (line in oldFontCacheVfsFileText.split("\n")) {
                val (file, name) = line.split("=", limit = 2) + listOf("", "")
                fileNamesToName[file] = name
            }
            for (folder in folders) {
                try {
                    val file = localVfs(Environment.expand(folder))
                    for (f in file.listRecursiveSimple()) {
                        try {
                            val name = fileNamesToName.getOrPut(f.baseName) {
                                val (ttf, totalTime) = measureTimeWithResult { TtfFont.readNames(f) }
                                //if (totalTime >= 1.milliseconds) println("Compute name size[${f.size()}] '${ttf.ttfCompleteName}' $totalTime")
                                ttf.ttfCompleteName
                            }
                            //println("name=$name, f=$f")
                            if (name != "") {
                                out[name] = f
                            }
                        } catch (e: Throwable) {
                            fileNamesToName.getOrPut(f.baseName) { "" }
                        }
                    }
                } catch (e: Throwable) {
                    imageLoadingLogger.info { e }
                }
            }
            val newFontCacheVfsFileText = fileNamesToName.map { "${it.key}=${it.value}" }.joinToString("\n")
            if (newFontCacheVfsFileText != oldFontCacheVfsFileText) {
                try {
                    fontCacheVfsFile.writeString(newFontCacheVfsFileText)
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    imageLoadingLogger.info { e }
                }
            }
        }
        if (time >= 100.milliseconds) {
            logger.info {  "Load System font names in $time" }
        }
        //println("fileNamesToName: $out")
        out
    }

    fun listFontNamesMapLC(): Map<String, VfsFile> = listFontNamesMap().mapKeys { it.key.normalizeName() }
    override fun defaultFont(): TtfFont = DefaultTtfFont

    override fun listFontNamesWithFiles(): Map<String, VfsFile> = listFontNamesMap()

    private val _namesMapLC = KorAtomicRef<Map<String, VfsFile>?>(null)
    private val namesMapLC: Map<String, VfsFile> get() {
        if (_namesMapLC.value == null) {
            _namesMapLC.value = (listFontNamesMapLC())
        }
        return _namesMapLC.value!!
    }

    override fun loadFontByName(name: String): TtfFont? =
        runBlockingNoJs { namesMapLC[name.normalizeName()]?.let { TtfFont(it.readAll()) } }
}

abstract class TtfNativeSystemFontProvider() : NativeSystemFontProvider() {
    abstract fun loadFontByName(name: String): TtfFont?
    abstract fun defaultFont(): TtfFont

    fun String.normalizeName() = this.toLowerCase().trim()

    //private val ttfCache = CopyOnWriteFrozenMap<String, TtfFont?>()
    private val ttfCache = LinkedHashMap<String, TtfFont?>()

    fun locateFontByName(name: String): TtfFont? {
        val normalizedName = name.normalizeName()
        return ttfCache.getOrPut(normalizedName) { loadFontByName(name) }
    }

    fun ttf(systemFont: SystemFont) = locateFontByName(systemFont.name) ?: defaultFont()

    override fun getTtfFromSystemFont(systemFont: SystemFont): TtfFont = ttf(systemFont)

    override fun getSystemFontGlyph(
        systemFont: SystemFont,
        size: Double,
        codePoint: Int,
        path: GlyphPath,
        reader: WStringReader?
    ): GlyphPath? =
        ttf(systemFont).getGlyphPath(size, codePoint, path, reader)

    override fun getSystemFontMetrics(systemFont: SystemFont, size: Double, metrics: FontMetrics) {
        ttf(systemFont).getFontMetrics(size, metrics)
    }

    override fun getSystemFontGlyphMetrics(
        systemFont: SystemFont,
        size: Double,
        codePoint: Int,
        metrics: GlyphMetrics,
        reader: WStringReader?,
    ) {
        ttf(systemFont).getGlyphMetrics(size, codePoint, metrics, reader)
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
    override fun defaultFont(): TtfFont = ttf
}
