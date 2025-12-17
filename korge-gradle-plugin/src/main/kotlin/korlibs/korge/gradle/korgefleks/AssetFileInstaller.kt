package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.PIXEL_FONTS
import java.io.File

class AssetFileInstaller(
    private val assetDir: File,
    private val exportTilesDir: File,
    private val gameResourcesDir: File,
    private val assetInfo: LinkedHashMap<String, Any>
) {


    fun addPixelFont(filename: String) {
        val fontFile = assetDir.resolve(filename)
        val fontName = fontFile.nameWithoutExtension
        val fontExtension = fontFile.extension
        val fontConfig = fontFile.readText()

        // Get file name for image "file=XXX"
        val imageFileName = fontConfig.lines().firstOrNull { it.startsWith("page id=") }
            ?.split("file=")?.getOrNull(1)?.trim()?.trim('"')
            ?: throw Exception("Could not find image file in font file: $filename")
        println("Export pixel font image file: $imageFileName")

        // Copy over font file to resources folder
        val resourceFontFile = gameResourcesDir.resolve(filename)
        resourceFontFile.parentFile.mkdirs()
        fontFile.copyTo(resourceFontFile, overwrite = true)

        // Copy over font image to assets folder for atlas packing
        val assetFontImageFile = assetDir.resolve(imageFileName)
        if (!assetFontImageFile.exists()) error("Font image file not found: $imageFileName")
        val exportFontImageFile = exportTilesDir.resolve(imageFileName)
        exportFontImageFile.parentFile.mkdirs()
        assetFontImageFile.copyTo(exportFontImageFile, overwrite = true)

        // Create empty pixel font info and store it - it will be filled later during atlas packing
        assetInfo[PIXEL_FONTS]?.let {
            (it as LinkedHashMap<String, Any>)[fontName] = linkedMapOf<String, Any>(
                "t" to fontExtension,
            )
        }
            ?: error("AssetConfig - pixelFonts info not initialized!")
    }
}
