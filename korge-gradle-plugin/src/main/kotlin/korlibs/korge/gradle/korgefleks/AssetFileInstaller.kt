package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.PIXEL_FONTS
import org.gradle.api.GradleException
import java.io.File


/**
 * Asset file loader for generic files (e.g. png, sound,...) in a KorgeFleks project.
 *
 * This class handles the installation of various asset types, such as pixel fonts,
 * by copying necessary files to the appropriate directories and updating the asset configuration.
 *
 * @param assetDir The directory where the original asset files are located.
 * @param exportTilesDir The directory where the pixel font png file will be stored, so that it is included in the texture atlas.
 * @param gameResourcesDir The directory where game resource files are located.
 * @param assetInfo The linked hash map containing asset configuration information.
 */
class AssetFileInstaller(
    private val assetDir: File,
    private val exportTilesDir: File,
    private val gameResourcesDir: File,
    private val assetInfo: LinkedHashMap<String, Any>
) {
    /**
     * Adds a pixel font to the asset configuration.
     * Copies the font file to the game resources directory and the associated image file to the export tiles directory.
     * Updates the asset info with an entry for the pixel font.
     *
     * @param filename The name of the pixel font file in the asset directory.
     * @throws GradleException if the image file referenced in the font file cannot be found.
     */
    fun addPixelFont(filename: String) {
        val fontFile = assetDir.resolve(filename)
        val fontName = fontFile.nameWithoutExtension
        val fontExtension = fontFile.extension
        val fontConfig = fontFile.readText()

        // Get file name for image "file=XXX"
        val imageFileName = fontConfig.lines().firstOrNull { it.startsWith("page id=") }
            ?.split("file=")?.getOrNull(1)?.trim()?.trim('"')
            ?: throw GradleException("Could not find image file in font file: $filename")
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
        } ?: error("AssetConfig - pixelFonts info not initialized!")
    }

    /**
     * Adds a sound file to the asset configuration.
     * Copies the sound file to the game resources' directory.
     *
     * @param filename The name of the sound file in the asset directory.
     */
    fun addSoundFile(filename: String) {
        TODO("Not yet implemented")
    }
}
