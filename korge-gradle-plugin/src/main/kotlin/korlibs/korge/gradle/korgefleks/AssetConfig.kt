package korlibs.korge.gradle.korgefleks

import com.android.build.gradle.internal.cxx.json.jsonStringOf
import korlibs.korge.gradle.texpacker.NewTexturePacker
import java.io.File


/**
 * Configuration for managing assets in a KorgeFleks project.
 *
 * This class is a convenience wrapper around AssetImageLoader.
 *
 * @param asepriteExe The path to the Aseprite executable.
 * @param projectDir The root directory of the project.
 * @param assetPath The relative path to the directory containing asset files.
 * @param resourcePath The relative path to the directory for game resources.
 */
class AssetsConfig(
    private val asepriteExe: String,
    projectDir: File,
    assetPath: String,
    resourcePath: String
) {
    var textureAtlasName: String = "texture"
    var tilesetAtlasName: String = "tileset"
    var simplifyJson: Boolean = true
    var atlasWidth: Int = 2048
    var atlasHeight: Int = 2048


    // Directory where Aseprite files are located
    private val assetDir = projectDir.resolve(assetPath)
    // Directory where exported tiles and tilesets will be stored
    private val exportTilesDir = projectDir.resolve("build/assets/${assetPath.replace("/", "-").replace("\\", "-")}-tiles")
    private val exportTilesetDir = projectDir.resolve("build/assets/${assetPath.replace("/", "-").replace("\\", "-")}-tilesets2")
    // Directory where game resources are located
    private val gameResourcesDir = projectDir.resolve("src/commonMain/resources/${resourcePath}")

    private val assetInfoList = linkedMapOf<String, Any>()

    init {
        // Make sure the export directories exist and that they are empty
        if (exportTilesDir.exists()) exportTilesDir.deleteRecursively()
        if (exportTilesetDir.exists()) exportTilesetDir.deleteRecursively()
        exportTilesDir.mkdirs()
        exportTilesetDir.mkdirs()

        // Set version info
        val major = 1
        val minor = 0
        val build = 1
        assetInfoList["info"] = arrayOf(major, minor, build)
    }

    private val assetImageLoader = AssetImageLoader(
        asepriteExe,
        assetDir,
        exportTilesDir,
        assetInfoList
    )

    private val assetImageAtlasWriter = AssetImageAtlasWriter(
        exportTilesDir,
        exportTilesetDir,
        gameResourcesDir,
        assetInfoList
    )

    /**
     * Export specific layers and tags from Aseprite file as independent png images.
     * Adds exported images to internal asset info list.
     */
    fun addImageAse(filename: String, layers: List<String>, tags: List<String>, output: String) {
        assetImageLoader.addImageAse(filename, layers, tags, output)
    }

    /** Export full image from Aseprite file
     */
    fun addImageAse(filename: String, output: String) {
        assetImageLoader.addImageAse(filename, emptyList(), emptyList(), output )
    }

    /** Export specific layer from Aseprite file
     */
    fun addImageAse(filename: String, layer: String, output: String) {
        assetImageLoader.addImageAse(filename, listOf(layer), emptyList(), output)
    }

    /** Export specific layer and tag from Aseprite file
     */
    fun addImageAse(filename: String, layer: String, tag: String, output: String) {
        assetImageLoader.addImageAse(filename, listOf(layer), listOf(tag), output)
    }

    /** Export specific layer and tags from Aseprite file
     */
    fun addImageAse(filename: String, layer: String, tags: List<String>, output: String) {
        assetImageLoader.addImageAse(filename, listOf(layer), tags, output)
    }

    /** Export specific layers from Aseprite file
     */
    fun addImageAse(filename: String, layers: List<String>, output: String) {
        assetImageLoader.addImageAse(filename, layers, emptyList(), output)
    }

    /** Export specific layers and tag from Aseprite file
     */
    fun addImageAse(filename: String, layers: List<String>, tag: String, output: String) {
        assetImageLoader.addImageAse(filename, layers, listOf(tag), output)
    }

    /**
     * Export nine-patch image from Aseprite file.
     * Adds exported nine-patch image to internal asset info list.
     */
    fun addNinePatchImageAse(filename: String, output: String) {
        assetImageLoader.addNinePatchImageAse(filename, emptyList(), emptyList(), output)
    }

    /**
     * Export pixel font file and associated pixel font image.
     * It copies font file to game resources and exports font image to assets folder for atlas packing.
     */
    fun addPixelFont(filename: String) {
        val fontFile = assetDir.resolve(filename)
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
    }

    fun buildAtlases() {
        assetImageAtlasWriter.buildAtlases(
            textureAtlasName,
            tilesetAtlasName,
            atlasWidth,
            atlasHeight,
            simplifyJson
        )
    }

}
