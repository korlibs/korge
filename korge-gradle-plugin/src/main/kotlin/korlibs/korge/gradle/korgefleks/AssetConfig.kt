package korlibs.korge.gradle.korgefleks

import com.android.build.gradle.internal.cxx.json.jsonStringOf
import org.gradle.api.GradleException
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
open class AssetConfig(
    private val asepriteExe: String,
    projectDir: File,
    assetPath: String,
    resourcePath: String
) {
    var assetInfoName: String = "assets"
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
        internal const val TILE_MAPS = "tileMaps"
    }

    // Directory where Aseprite files are located
    protected val assetDir = projectDir.resolve(assetPath)
    // Directory where exported tiles and tilesets will be stored
    protected val exportTilesDir = projectDir.resolve("build/assets/imageAtlasInput")
    // Directory where game resources are located
    protected val gameResourcesDir = projectDir.resolve("src/commonMain/resources/${resourcePath}")

    protected val assetInfo = linkedMapOf<String, Any>()

    init {
        // Make sure the export directories exist and that they are empty
        if (exportTilesDir.exists()) exportTilesDir.deleteRecursively()
        exportTilesDir.mkdirs()

        // Set version info
        val major = 1
        val minor = 0
        val build = 1
        assetInfo[VERSION] = arrayOf(major, minor, build)

        // Initialize maps and lists in asset info
        assetInfo[TEXTURES] = arrayListOf<String>()
        assetInfo[IMAGES] = linkedMapOf<String, Any>()
        assetInfo[NINE_PATCHES] = linkedMapOf<String, Any>()
        assetInfo[PIXEL_FONTS] = linkedMapOf<String, Any>()
        assetInfo[PARALLAX_LAYERS] = linkedMapOf<String, Any>()
        assetInfo[TILES] = linkedMapOf<String, Any>()
    }

    protected val assetImageAseExporter = AssetImageAseExporter(asepriteExe, assetDir, exportTilesDir, assetInfo)
    protected val assetFileInstaller = AssetFileInstaller(assetDir, exportTilesDir, gameResourcesDir, assetInfo)
    protected val assetImageAtlasBuilder = AssetImageAtlasBuilder(exportTilesDir, gameResourcesDir, assetInfo)

    /**
     * Export specific layers and tags from Aseprite file as independent png images.
     * Adds exported images to internal asset info list.
     */
    fun addImageAse(fileName: String, layers: List<String>, tags: List<String>, output: String) {
        assetImageAseExporter.addImageAse(fileName, layers, tags, output)
    }

    /** Export full image from Aseprite file
     */
    fun addImageAse(fileName: String, output: String) {
        assetImageAseExporter.addImageAse(fileName, emptyList(), emptyList(), output )
    }

    /** Export specific layer from Aseprite file
     */
    fun addImageAse(fileName: String, layer: String, output: String) {
        assetImageAseExporter.addImageAse(fileName, listOf(layer), emptyList(), output)
    }

    /** Export specific layer and tag from Aseprite file
     */
    fun addImageAse(fileName: String, layer: String, tag: String, output: String) {
        assetImageAseExporter.addImageAse(fileName, listOf(layer), listOf(tag), output)
    }

    /** Export specific layer and tags from Aseprite file
     */
    fun addImageAse(fileName: String, layer: String, tags: List<String>, output: String) {
        assetImageAseExporter.addImageAse(fileName, listOf(layer), tags, output)
    }

    /** Export specific layers from Aseprite file
     */
    fun addImageAse(fileName: String, layers: List<String>, output: String) {
        assetImageAseExporter.addImageAse(fileName, layers, emptyList(), output)
    }

    /** Export specific layers and tag from Aseprite file
     */
    fun addImageAse(fileName: String, layers: List<String>, tag: String, output: String) {
        assetImageAseExporter.addImageAse(fileName, layers, listOf(tag), output)
    }

    /**
     * Export nine-patch image from Aseprite file.
     * Adds exported nine-patch image to internal asset info list.
     */
    fun addNinePatchImageAse(fileName: String, output: String) {
        assetImageAseExporter.addNinePatchImageAse(fileName, emptyList(), emptyList(), output)
    }

    /**
     * Export pixel font file and associated pixel font image.
     * It copies font file to game resources and exports font image to assets folder for atlas packing.
     */
    fun addPixelFont(fileName: String) {
        assetFileInstaller.addPixelFont(fileName)
    }

    /**
     * Export parallax layer images from Aseprite file.
     * Adds exported parallax images to internal asset info list.
     */
    fun addParallaxImageAse(fileName: String, parallaxInfo: ParallaxInfo) {
        assetImageAseExporter.addParallaxImageAse(fileName, parallaxInfo)
    }

    /**
     * Add a generic file to the asset configuration.
     * Copies the file to the game resources' directory.
     */
    fun addFile(fileName: String) {
        assetFileInstaller.addFile(fileName)
    }

    /**
     * Build texture and tileset atlases from exported images.
     * Uses atlas names and sizes from the AssetConfig properties.
     *
     * This will always be called as last step after all assets have been added.
     */
    internal open fun buildAssetStore() {
        println()
        // First build the image and tileset atlases
        assetImageAtlasBuilder.buildAtlases(
            textureAtlasName,
            atlasWidth,
            atlasHeight,
            atlasPadding
        )
        // Finally, write out the asset info as JSON file
        writeAssetInfoJson()
    }

    protected fun writeAssetInfoJson() {
        val assetInfoJsonFile = gameResourcesDir.resolve("${assetInfoName}.json")
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

class WorldClusterAssetConfig(
    asepriteExe: String,
    projectDir: File,
    assetPath: String,
    resourcePath: String,
    private val clusterName: String
) : AssetConfig(asepriteExe, projectDir, assetPath, resourcePath) {

    private val tileSetFiles: MutableList<File> = mutableListOf()
    private val exportTilesetDir = projectDir.resolve("build/assets/tilesetAtlasInput")

    // Directory with cluster asset info files (e.g. world.json, intro.json, etc.)
    private val clusterAssetInfoDir = projectDir.resolve("gradle/worldClusterAssetInfo")

    private val assetTilesetExporter = AssetTilesetExporter(assetDir, exportTilesetDir, tileSetFiles)
    private val assetTilesetAtlasBuilder = AssetTilesetAtlasBuilder(exportTilesetDir, gameResourcesDir, assetInfo, tileSetFiles, clusterAssetInfoDir)
    private val assetLevelMapExporter = AssetLevelMapExporter(assetDir, gameResourcesDir, assetInfo)

    // Save input data for exporters
    private val listOfTileSetImageFiles = mutableListOf<String>()
    private val listOfTileMapLdtkFiles = mutableListOf<Pair<String, String>>()

    init {
        // Make sure the export directories exist and that they are empty
        if (exportTilesDir.exists()) exportTilesDir.deleteRecursively()
        if (exportTilesetDir.exists()) exportTilesetDir.deleteRecursively()
        exportTilesDir.mkdirs()
        exportTilesetDir.mkdirs()

        // Initialize maps and lists in asset info
        assetInfo[TILESETS] = arrayListOf<String>()
        assetInfo[TILE_MAPS] = linkedMapOf<String, Any>()
    }

    /**
     * Export single tiles from a png tileset file and stores them in a tileset atlas.
     * Adds exported tiles and tileset images to internal asset info list.
     *
     * @param fileName The png file containing the tileset image.
     */
    fun addTilesetImagePng(fileName: String) {
        listOfTileSetImageFiles.add(fileName)
    }

    /**
     * Export single level from LDtk file as an TileMap object.
     * Adds exported level map to internal asset info list.
     *
     * Note: The used tile set in the LDtk level map must be included in the same cluster's tileset assets!
     *
     * @param fileName The LDtk file containing the level data.
     * @param levelName The name of the level to export.
     */
    fun addTileMapLDtkFile(fileName: String, levelName: String) {
        listOfTileMapLdtkFiles.add(Pair(fileName, levelName))
    }

    /**
     * Build texture and tileset atlases from exported images.
     * Uses atlas names and sizes from the AssetConfig properties.
     *
     * This will always be called as last step after all assets have been added.
     *
     * @param assetResourcePath The relative path to the directory for game resources. The name of the last folder is the cluster name.
     */
    fun buildAssetStore(assetResourcePath: String) {
        // First run exporters for PNG and tile map files
        listOfTileSetImageFiles.forEach { tileSet ->
            assetTilesetExporter.addTilesetImagePng(tileSet)
        }
        listOfTileMapLdtkFiles.forEach { tileMapInfo ->
            val fileName = tileMapInfo.first
            val levelName = tileMapInfo.second
            val tileSetList = listOfTileSetImageFiles.map { File(it).nameWithoutExtension }
            println("tileset list: $tileSetList")
            assetLevelMapExporter.exportTileMapLDtk(fileName, levelName, clusterName, tileSetList)
        }
        println()

        // Now build the image and tileset atlases
        assetImageAtlasBuilder.buildAtlases(
            textureAtlasName,
            atlasWidth,
            atlasHeight,
            atlasPadding
        )
        assetTilesetAtlasBuilder.buildTilesetAtlas(
            assetResourcePath,
            tilesetAtlasName,
            tileWidth,
            tileHeight,
            atlasWidth,
            atlasHeight,
            atlasPadding
        )
        // Finally, write out the asset info as JSON file
        writeAssetInfoJson()
    }
}

class WorldLevelMapAssetConfig(
    projectDir: File,
    private val world: Int,
    private val assetPath: String = "",
    resourcePath: String
) {
    var simplifyJson: Boolean = true

    // Directory where game resources are located
    private val gameResourcesDir = projectDir.resolve("src/commonMain/resources/${resourcePath}")
    private val assetLevelMapExporter = AssetLevelMapExporter(projectDir, gameResourcesDir, linkedMapOf())

    // Save input data for exporters
    private var levelMapLdtkFile = ""
    private val mapOfClustersWithTileMaps: MutableMap<String, List<String>> = mutableMapOf()

    /**
     * Export level map from LDtk file as chunked level map.
     * This creates for each chunk of the world level map a separate JSON file which
     * contains the tile map data and entity configs for that chunk.
     */
    fun addLevelMapLdtkFile(fileName: String) {
        if (levelMapLdtkFile.isNotEmpty()) throw GradleException("ERROR: worldLevelMapAssets - Only one LDtk file can be added for exporting level maps as world chunks!")
        levelMapLdtkFile = if (assetPath.isNotEmpty()) "${assetPath}/${fileName}" else fileName
    }

    /**
     * Define tilesets per cluster for the level map export.
     *
     * @param clusterName The name of the cluster within the world.
     * @param tileSetNames The list of tileset names which are used in the level map and belong to the cluster.
     *                     The tileset names must match the ones defined in the cluster's "worldClusterAssets" block.
     */
    fun tileSetsPerCluster(clusterName : String, vararg tileSetNames: String) {
        mapOfClustersWithTileMaps[clusterName] = tileSetNames.toList()
    }

    /**
     * Export level map from LDtk file as chunked level map.
     * Adds exported level map to internal asset info list.
     */
    internal fun buildAssetStore() {
        // First run exporters for LDtk level map files
        mapOfClustersWithTileMaps.forEach { (cluster, tileSets) ->
            println("Cluster '$cluster' with tilesets: $tileSets")
        }
        if (levelMapLdtkFile.isEmpty()) throw GradleException("ERROR: worldLevelMapAssets - No LDtk file defined for exporting level maps as world chunks!")
        assetLevelMapExporter.exportLevelMapLDtk(levelMapLdtkFile, mapOfClustersWithTileMaps, simplifyJson)

            // TODO change to support world chunks
//            assetLevelMapExporter.exportLevelMapLDtk(levelMapFilePath, tileSetsPerClusterMap)
    }
}
