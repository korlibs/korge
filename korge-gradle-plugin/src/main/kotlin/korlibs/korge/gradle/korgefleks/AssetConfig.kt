package korlibs.korge.gradle.korgefleks

import com.android.build.gradle.internal.cxx.json.jsonStringOf
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
class AssetConfig(
    private val asepriteExe: String,
    projectDir: File,
    assetPath: String,
    resourcePath: String
) {
    var textureAtlasName: String = "texture"
    var tilesetAtlasName: String = "tileset"
    var simplifyJson: Boolean = true

    var tileWidth: Int = 16
    var tileHeight: Int = 16
    var atlasWidth: Int = 2048
    var atlasHeight: Int = 2048
    var atlasPadding: Int = 1

    companion object {
        internal const val VERSION = "version"
        internal const val TEXTURES = "textures"
        internal const val TILESETS = "tilesets"
        internal const val IMAGES = "images"
        internal const val NINE_PATCHES = "ninePatches"
        internal const val PIXEL_FONTS = "pixelFonts"
        internal const val PARALLAX_LAYERS = "parallaxLayers"
        internal const val TILES = "tiles"
    }

    // Directory where Aseprite files are located
    private val assetDir = projectDir.resolve(assetPath)
    // Directory where exported tiles and tilesets will be stored
    private val exportTilesDir = projectDir.resolve("build/assets/imageAtlasInput")
    private val exportTilesetDir = projectDir.resolve("build/assets/tilesetAtlasInput")
    // Directory where game resources are located
    private val gameResourcesDir = projectDir.resolve("src/commonMain/resources/${resourcePath}")

    private val assetInfo = linkedMapOf<String, Any>()

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
        assetInfo[VERSION] = arrayOf(major, minor, build)

        // Initialize maps and lists in asset info
        assetInfo[TEXTURES] = arrayListOf<String>()
        assetInfo[TILESETS] = arrayListOf<String>()
        assetInfo[IMAGES] = linkedMapOf<String, Any>()
        assetInfo[NINE_PATCHES] = linkedMapOf<String, Any>()
        assetInfo[PIXEL_FONTS] = linkedMapOf<String, Any>()
        assetInfo[PARALLAX_LAYERS] = linkedMapOf<String, Any>()
        assetInfo[TILES] = linkedMapOf<String, Any>()
    }

    private val assetImageAseExporter = AssetImageAseExporter(asepriteExe, assetDir, exportTilesDir, assetInfo)
    private val assetFileInstaller = AssetFileInstaller(assetDir, exportTilesDir, gameResourcesDir, assetInfo)
    private val assetTilesetExporter = AssetTilesetExporter(assetDir, exportTilesetDir)
    private val assetImageAtlasBuilder = AssetImageAtlasBuilder(exportTilesDir, gameResourcesDir, assetInfo)
    private val assetTilesetAtlasBuilder = AssetTilesetAtlasBuilder(exportTilesetDir, gameResourcesDir, assetInfo)

    /**
     * Export specific layers and tags from Aseprite file as independent png images.
     * Adds exported images to internal asset info list.
     */
    fun addImageAse(filename: String, layers: List<String>, tags: List<String>, output: String) {
        assetImageAseExporter.addImageAse(filename, layers, tags, output)
    }

    /** Export full image from Aseprite file
     */
    fun addImageAse(filename: String, output: String) {
        assetImageAseExporter.addImageAse(filename, emptyList(), emptyList(), output )
    }

    /** Export specific layer from Aseprite file
     */
    fun addImageAse(filename: String, layer: String, output: String) {
        assetImageAseExporter.addImageAse(filename, listOf(layer), emptyList(), output)
    }

    /** Export specific layer and tag from Aseprite file
     */
    fun addImageAse(filename: String, layer: String, tag: String, output: String) {
        assetImageAseExporter.addImageAse(filename, listOf(layer), listOf(tag), output)
    }

    /** Export specific layer and tags from Aseprite file
     */
    fun addImageAse(filename: String, layer: String, tags: List<String>, output: String) {
        assetImageAseExporter.addImageAse(filename, listOf(layer), tags, output)
    }

    /** Export specific layers from Aseprite file
     */
    fun addImageAse(filename: String, layers: List<String>, output: String) {
        assetImageAseExporter.addImageAse(filename, layers, emptyList(), output)
    }

    /** Export specific layers and tag from Aseprite file
     */
    fun addImageAse(filename: String, layers: List<String>, tag: String, output: String) {
        assetImageAseExporter.addImageAse(filename, layers, listOf(tag), output)
    }

    /**
     * Export nine-patch image from Aseprite file.
     * Adds exported nine-patch image to internal asset info list.
     */
    fun addNinePatchImageAse(filename: String, output: String) {
        assetImageAseExporter.addNinePatchImageAse(filename, emptyList(), emptyList(), output)
    }

    /**
     * Export pixel font file and associated pixel font image.
     * It copies font file to game resources and exports font image to assets folder for atlas packing.
     */
    fun addPixelFont(filename: String) {
        assetFileInstaller.addPixelFont(filename)
    }

    /**
     * Export parallax layer images from Aseprite file.
     * Adds exported parallax images to internal asset info list.
     */
    fun addParallaxImageAse(filename: String, parallaxInfo: ParallaxInfo) {
        assetImageAseExporter.addParallaxImageAse(filename, parallaxInfo)
    }

    /**
     * Export single tiles from a png tileset file and stores them in a tileset atlas.
     * Adds exported tiles and tileset images to internal asset info list.
     */
    fun addTilesetImagePng(filename: String) {
        assetTilesetExporter.addTilesetImagePng(filename)
    }

    /**
     * Add a generic file to the asset configuration.
     * Copies the file to the game resources' directory.
     */
    fun addFile(filename: String) {
        assetFileInstaller.addFile(filename)
    }

    /**
     * Build texture and tileset atlases from exported images.
     * Uses atlas names and sizes from the AssetConfig properties.
     *
     * This will always be called as last step after all assets have been added.
     */
    internal fun buildAssetStore() {
        // First build the image and tileset atlases
        assetImageAtlasBuilder.buildAtlases(
            textureAtlasName,
            atlasWidth,
            atlasHeight,
            atlasPadding
        )
        assetTilesetAtlasBuilder.buildTilesetAtlas(
            tilesetAtlasName,
            tileWidth,
            tileHeight,
            atlasWidth,
            atlasHeight,
            atlasPadding
        )

        // Finally, write out the asset info as JSON file
        val assetInfoJsonFile = gameResourcesDir.resolve("${textureAtlasName}.atlas.json")
        assetInfoJsonFile.parentFile?.let { parent ->
            if (!parent.exists() && !parent.mkdirs()) error("Failed to create directory: ${parent.path}")
            val jsonString = jsonStringOf(assetInfo)
            // Simplify JSON string by removing unnecessary spaces and line breaks
            val simplifiedJsonString = if (simplifyJson) jsonString.replace(Regex("\\s+"), "")
            else jsonString
            assetInfoJsonFile.writeText(simplifiedJsonString)
        }
    }
}
